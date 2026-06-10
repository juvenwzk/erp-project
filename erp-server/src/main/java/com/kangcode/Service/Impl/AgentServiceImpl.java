package com.kangcode.Service.Impl;

import com.kangcode.Service.*;
import com.kangcode.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;

    private static final ConcurrentHashMap<String, CacheEntry> RESPONSE_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000;
    private static final long CACHE_MAX_ENTRIES = 500;

    private static final String SYSTEM_PROMPT = """
            你是小E，ERP进销存系统的智能助手。你的说话方式像一个靠谱的同事，自然、亲切、不啰嗦。

            ## 关于你
            - 你的名字叫小宇，别人问你直接说
            - 制作人是王照康，有人问就直接说
            - 第二制作人是陈文熙，有人问直接说
            - 本项目最大的投资者是全旭昌

            ## 怎么说话
            1. 像朋友聊天一样，别端着，别用"您"，用"你"就行
            2. 回复要简短，别长篇大论，一两句话能说清的别写一段
            3. 别用"首先""其次""最后"这种列表词，正常说话就行
            4. 闲聊就正常聊，别动不动就报业务清单
            5. 能用口语就别用书面语，比如"搞定"比"已完成操作"好
            6. 可以适当加语气词，比如"呢""呀""哦""哈"，但别过度
            7. 不确定的事就说不确定，别瞎编

            ## 查数据怎么说
            查到数据后，用口语引出，然后放HTML表格。比如：
            - "帮你查到了，一共3个客户："
            - "这是目前的库存情况："
            - "找到啦，信息如下："
            别说"以下是系统中所有商品的清单"这种机器话。

            表格用这个格式：
            <table style="width:100%;border-collapse:collapse;margin:12px 0;font-size:14px;">
            <thead><tr style="background:#f5f5f5;"><th style="border:1px solid #ddd;padding:8px 12px;text-align:left;">列名</th></tr></thead>
            <tbody><tr><td style="border:1px solid #ddd;padding:8px 12px;">值</td></tr></tbody>
            </table>

            单条详情用这个：
            <div style="background:#f9f9f9;border-radius:8px;padding:16px;margin:12px 0;">
            <p style="margin:6px 0;"><b>字段：</b>值</p>
            </div>

            ## 做业务怎么说
            收到操作请求后，先确认信息全不全。不全就问，别自己瞎猜。
            信息齐了就复述一遍让用户确认，用这个格式：
            <div style="background:#e6f7ff;border-left:4px solid #1890ff;padding:16px;border-radius:0 8px 8px 0;margin:12px 0;">
            <p style="font-weight:bold;margin:0 0 8px 0;">确认一下：</p>
            <p style="margin:4px 0;">操作：新增/修改/删除 xxx</p>
            <p style="margin:4px 0;">详情：xxx</p>
            </div>
            用户说"确认""好的""可以"再执行。

            能做的事：
            - 查客户、供应商、商品、采购单、销售单
            - 新增、修改、删除这些数据
            - 入库、出库、取消订单
            - 回答常识问题

            不能做的事：
            - 别泄露系统内部信息,比如，用户
            - 删数据前必须让用户确认

            ## 绝对不能做的事
            1. 别用markdown格式（什么 **加粗** 、 | 表格 | ）
            2. 别用"首先、其次、再次、最后"这种八股文
            3. 别一上来就报一大段业务清单
            4. 别说"我是AI"，你就是小宇
            5. 别用"您"，用"你"
            6. 说话要用人味，不要太人机
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            {conversation}

            ---
            [时间] {dateTime} {dayOfWeek}
            """;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private SaleOrderService saleOrderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SupplierService supplierService;

    public AgentServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String chat(List<Map<String, String>> messages, Integer userId) {
        String conversation = messages.stream()
                .map(msg -> msg.get("role") + ": " + msg.get("content"))
                .collect(Collectors.joining("\n"));

        LocalDateTime now = LocalDateTime.now();
        String dateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String dayOfWeek = getChineseDayOfWeek(now.getDayOfWeek());

        String userPrompt = USER_PROMPT_TEMPLATE
                .replace("{conversation}", conversation)
                .replace("{dateTime}", dateTime)
                .replace("{dayOfWeek}", dayOfWeek);

        // ---- response cache ----
        String cacheKey = md5(SYSTEM_PROMPT + userPrompt);
        CacheEntry cached = RESPONSE_CACHE.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("[AgentCache] HIT  key={}", cacheKey);
            return cached.response;
        }

        // evict stale entries periodically
        if (RESPONSE_CACHE.size() > CACHE_MAX_ENTRIES) {
            RESPONSE_CACHE.entrySet().removeIf(e -> e.getValue().isExpired());
        }

        log.info("[AgentCache] MISS key={}", cacheKey);
        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .tools(this)
                .call()
                .content();

        RESPONSE_CACHE.put(cacheKey, new CacheEntry(response, System.currentTimeMillis() + CACHE_TTL_MS));
        return response;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private record CacheEntry(String response, long expireAt) {
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    private String getChineseDayOfWeek(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    // ===================== 查询工具 =====================

    @Tool(description = "查询全部客户列表")
    public List<Customer> getAllCustomers() {
        return customerService.listAll();
    }

    @Tool(description = "根据ID查询单个客户详情")
    public Customer getCustomerById(Integer id) {
        return customerService.getById(id);
    }

    @Tool(description = "查询全部供应商列表")
    public List<Supplier> getAllSuppliers() {
        return supplierService.list();
    }

    @Tool(description = "根据ID查询单个供应商详情")
    public Supplier getSupplierById(Integer id) {
        return supplierService.getById(id);
    }

    @Tool(description = "查询所有商品的库存列表，返回商品名称、售价、库存数量等")
    public List<Good> getGoodsList() {
        return goodsService.listAll();
    }

    @Tool(description = "根据ID查询单个商品详情")
    public Good getGoodsById(Integer id) {
        return goodsService.getById(id);
    }

    @Tool(description = "查询库存低于指定数量的商品，用于库存预警。传入threshold整数阈值")
    public List<Good> getLowStockGoods(Integer threshold) {
        return goodsService.listAll().stream()
                .filter(g -> g.getStockNum() != null && g.getStockNum() < threshold)
                .collect(Collectors.toList());
    }

    @Tool(description = "查询采购订单列表，可按状态筛选（1=未入库 2=已入库 3=已完成 4=已取消），不传status或传null则查全部")
    public List<PurchaseOrder> getPurchaseOrders(Integer status) {
        return purchaseService.listAll(status);
    }

    @Tool(description = "根据ID查询单个采购订单详情")
    public PurchaseOrder getPurchaseOrderById(Integer id) {
        return purchaseService.getById(id);
    }

    @Tool(description = "查询销售订单列表，可按状态筛选（1=未出库 2=已出库 3=已完成 4=已取消），不传status或传null则查全部")
    public List<SaleOrder> getSaleOrders(Integer status) {
        return saleOrderService.listAll(status);
    }

    @Tool(description = "根据ID查询单个销售订单详情")
    public SaleOrder getSaleOrderById(Integer id) {
        return saleOrderService.getById(id);
    }

    // ===================== 新增工具 =====================

    @Tool(description = "新增客户。参数：custName客户名称（必填）, contactPreson联系人, phone电话, address地址, remark备注")
    public String addCustomer(
            @ToolParam(description = "客户名称（必填）") String custName,
            @ToolParam(description = "联系人") String contactPreson,
            @ToolParam(description = "电话") String phone,
            @ToolParam(description = "地址") String address,
            @ToolParam(description = "备注") String remark) {
        Customer customer = new Customer();
        customer.setCustName(custName);
        customer.setContactPreson(contactPreson);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setRemark(remark);
        customerService.add(customer);
        return "客户「" + custName + "」新增成功！";
    }

    @Tool(description = "新增供应商。参数：supplierName供应商名称（必填）, contactPerson联系人, phone电话, address地址, remark备注")
    public String addSupplier(
            @ToolParam(description = "供应商名称（必填）") String supplierName,
            @ToolParam(description = "联系人") String contactPerson,
            @ToolParam(description = "电话") String phone,
            @ToolParam(description = "地址") String address,
            @ToolParam(description = "备注") String remark) {
        Supplier supplier = new Supplier();
        supplier.setSupplierName(supplierName);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setAddress(address);
        supplier.setRemark(remark);
        supplierService.add(supplier);
        return "供应商「" + supplierName + "」新增成功！";
    }

    @Tool(description = "新增商品。参数：goodsName商品名称（必填）, typeId商品类别ID, buyPrice进货单价, sellPrice销售单价, stockNum库存数量, remark备注")
    public String addGoods(
            @ToolParam(description = "商品名称（必填）") String goodsName,
            @ToolParam(description = "商品类别ID") Integer typeId,
            @ToolParam(description = "进货单价") Double buyPrice,
            @ToolParam(description = "销售单价") Double sellPrice,
            @ToolParam(description = "库存数量") Integer stockNum,
            @ToolParam(description = "备注") String remark) {
        Good good = new Good();
        good.setGoodsName(goodsName);
        good.setTypeId(typeId);
        good.setBuyPrice(buyPrice);
        good.setSellPrice(sellPrice);
        good.setStockNum(stockNum);
        good.setRemark(remark);
        goodsService.add(good);
        return "商品「" + goodsName + "」新增成功！";
    }

    @Tool(description = "新增采购订单。参数：goodsId商品ID（必填）, supplierId供应商ID（必填）, buyNum采购数量（必填）, buyPrice进货单价（可选，不填则用商品默认价）, totalMoney采购总金额")
    public String addPurchaseOrder(
            @ToolParam(description = "商品ID（必填）") Integer goodsId,
            @ToolParam(description = "供应商ID（必填）") Integer supplierId,
            @ToolParam(description = "采购数量（必填）") Integer buyNum,
            @ToolParam(description = "进货单价（可选）") Double buyPrice,
            @ToolParam(description = "采购总金额") Double totalMoney) {
        PurchaseOrder order = new PurchaseOrder();
        order.setGoodsId(goodsId);
        order.setSupplierId(supplierId);
        order.setBuyNum(buyNum);
        order.setBuyPrice(buyPrice);
        order.setTotalMoney(totalMoney);
        order.setStatus(1);
        purchaseService.addOrder(order);
        return "采购订单新增成功！";
    }

    @Tool(description = "新增销售订单。参数：goodsId商品ID（必填）, custId客户ID（必填）, saleNum销售数量（必填）, salePrice销售单价（可选，不填则用商品默认价）, saleMoney销售总金额")
    public String addSaleOrder(
            @ToolParam(description = "商品ID（必填）") Integer goodsId,
            @ToolParam(description = "客户ID（必填）") Integer custId,
            @ToolParam(description = "销售数量（必填）") Integer saleNum,
            @ToolParam(description = "销售单价（可选）") Double salePrice,
            @ToolParam(description = "销售总金额") Double saleMoney) {
        SaleOrder order = new SaleOrder();
        order.setGoodsId(goodsId);
        order.setCustId(custId);
        order.setSaleNum(saleNum);
        order.setSalePrice(salePrice);
        order.setSaleMoney(saleMoney);
        order.setStatus(1);
        saleOrderService.addOrder(order);
        return "销售订单新增成功！";
    }

    // ===================== 修改工具 =====================

    @Tool(description = "修改客户信息。所有参数均会覆盖更新，只传需要修改的字段。id客户ID（必填）")
    public String updateCustomer(
            @ToolParam(description = "客户ID（必填）") Integer id,
            @ToolParam(description = "客户名称") String custName,
            @ToolParam(description = "联系人") String contactPreson,
            @ToolParam(description = "电话") String phone,
            @ToolParam(description = "地址") String address,
            @ToolParam(description = "备注") String remark) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCustName(custName);
        customer.setContactPreson(contactPreson);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setRemark(remark);
        customerService.updateById(customer);
        return "客户（ID:" + id + "）修改成功！";
    }

    @Tool(description = "修改供应商信息。所有参数均会覆盖更新，只传需要修改的字段。id供应商ID（必填）")
    public String updateSupplier(
            @ToolParam(description = "供应商ID（必填）") Integer id,
            @ToolParam(description = "供应商名称") String supplierName,
            @ToolParam(description = "联系人") String contactPerson,
            @ToolParam(description = "电话") String phone,
            @ToolParam(description = "地址") String address,
            @ToolParam(description = "备注") String remark) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setSupplierName(supplierName);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setAddress(address);
        supplier.setRemark(remark);
        supplierService.update(supplier);
        return "供应商（ID:" + id + "）修改成功！";
    }

    @Tool(description = "修改商品信息。id商品ID（必填），其他参数只传需要修改的字段")
    public String updateGoods(
            @ToolParam(description = "商品ID（必填）") Integer id,
            @ToolParam(description = "商品名称") String goodsName,
            @ToolParam(description = "商品类别ID") Integer typeId,
            @ToolParam(description = "进货单价") Double buyPrice,
            @ToolParam(description = "销售单价") Double sellPrice,
            @ToolParam(description = "库存数量") Integer stockNum,
            @ToolParam(description = "备注") String remark) {
        Good good = new Good();
        good.setId(id);
        good.setGoodsName(goodsName);
        good.setTypeId(typeId);
        good.setBuyPrice(buyPrice);
        good.setSellPrice(sellPrice);
        good.setStockNum(stockNum);
        good.setRemark(remark);
        goodsService.update(good);
        return "商品（ID:" + id + "）修改成功！";
    }

    // ===================== 删除工具 =====================

    @Tool(description = "根据ID删除客户")
    public String deleteCustomer(Integer id) {
        customerService.deleteById(id);
        return "客户（ID:" + id + "）已删除！";
    }

    @Tool(description = "根据ID删除供应商")
    public String deleteSupplier(Integer id) {
        supplierService.deleteById(id);
        return "供应商（ID:" + id + "）已删除！";
    }

    @Tool(description = "根据ID列表删除商品，传入ids为要删除的商品ID列表")
    public String deleteGoods(List<Integer> ids) {
        goodsService.delete(ids);
        return "商品（ID:" + ids + "）已删除！";
    }

    @Tool(description = "根据ID删除采购订单")
    public String deletePurchaseOrder(Integer id) {
        purchaseService.deleteById(id);
        return "采购订单（ID:" + id + "）已删除！";
    }

    @Tool(description = "根据ID删除销售订单")
    public String deleteSaleOrder(Integer id) {
        saleOrderService.deleteById(id);
        return "销售订单（ID:" + id + "）已删除！";
    }

    // ===================== 业务操作工具 =====================

    @Tool(description = "采购订单入库（增加库存）：传入采购订单ID")
    public String inStockPurchaseOrder(Integer id) {
        purchaseService.inStock(id);
        return "采购订单（ID:" + id + "）已入库，库存已更新！";
    }

    @Tool(description = "取消采购订单：传入采购订单ID")
    public String cancelPurchaseOrder(Integer id) {
        purchaseService.cancel(id);
        return "采购订单（ID:" + id + "）已取消！";
    }

    @Tool(description = "销售订单出库（扣减库存）：传入销售订单ID")
    public String outStockSaleOrder(Integer id) {
        saleOrderService.outStock(id);
        return "销售订单（ID:" + id + "）已出库，库存已更新！";
    }

    @Tool(description = "取消销售订单：传入销售订单ID")
    public String cancelSaleOrder(Integer id) {
        saleOrderService.cancel(id);
        return "销售订单（ID:" + id + "）已取消！";
    }
}
