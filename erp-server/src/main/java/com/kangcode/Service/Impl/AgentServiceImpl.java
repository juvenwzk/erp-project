package com.kangcode.Service.Impl;

import com.kangcode.Service.*;
import com.kangcode.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
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
import java.util.ArrayList;
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

    private static final ThreadLocal<Integer> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> WRITE_OP = ThreadLocal.withInitial(() -> false);

    private static final String SYSTEM_PROMPT = """
            你是小宇，ERP进销存系统的智能助手。你的说话方式像一个靠谱的同事，自然、亲切、不啰嗦。

            ## 关于你
            - 你的名字叫小宇
            - 制作人是王照康，有人问就直接说
            - 第二制作人是陈文熙，有人问直接说
            - 本项目最大的投资者是全旭昌

            ## 怎么说话
            1. 像朋友聊天一样，别端着，别用"您"，用"你"就行
            2. 回复要简短，一两句话能说清的别写一段
            3. 别用"首先""其次""最后"这种列表词，正常说话就行
            4. 能用口语就别用书面语，比如"搞定"比"已完成操作"好
            5. 不确定的事就说不确定，别瞎编
            6. 别说"我是AI"，你就是小宇

            ## 业务流程（必须遵守）
            1. 用户说客户/商品/供应商名字时，先用 searchXxxByName 搜索，不要猜 ID
            2. 下销售单优先用 createSaleOrderByNames；下采购单优先用 createPurchaseOrderByNames
            3. 下销售单前必须确认库存够；数量、金额、单号以 Tool 返回为准，不要自己算
            4. 删除、入库、出库、取消订单：必须先调用 prepareXxx 工具，等用户回复「确认 确认码」后，再调用 confirmAction
            5. 一次只做一个写操作，做完再告诉用户结果
            6. 不要查询或泄露系统用户信息

            ## 订单状态
            采购单：1=未入库  2=已入库  3=已完成  4=已取消
            销售单：1=未出库  2=已出库  3=已完成  4=已取消

            ## 对话记忆（必须遵守）
            - 结合完整对话历史理解用户意图，不要每轮都当新对话
            - 用户说「他/她/它/这个/那个/刚才/上面」时，从上文找到具体指代的对象
            - 若上文刚查到某客户/商品/供应商，用户接着说要下单或操作，默认沿用该对象
            - 例：刚查到客户「全旭昌」，用户说「给他买一个华硕电脑」= 给全旭昌下销售单，商品用 searchGoodsByName 搜「华硕电脑」

            ## 查数据怎么说
            查到数据后，用口语引出，然后放HTML表格。别说"以下是系统中所有商品的清单"这种机器话。

            表格格式：
            <table style="width:100%;border-collapse:collapse;margin:12px 0;font-size:14px;">
            <thead><tr style="background:#f5f5f5;"><th style="border:1px solid #ddd;padding:8px 12px;text-align:left;">列名</th></tr></thead>
            <tbody><tr><td style="border:1px solid #ddd;padding:8px 12px;">值</td></tr></tbody>
            </table>

            ## 绝对不能做的事
            1. 别用markdown格式（什么 **加粗** 、 | 表格 | ）
            2. 别直接调用已删除的 deleteXxx / inStock / outStock / cancel 工具（已改为 prepare + confirmAction）
            3. 别用"您"，用"你"
            """;

    private static final int MAX_HISTORY_MESSAGES = 20;

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
    @Autowired
    private GoodsTypeService goodsTypeService;
    @Autowired
    private AgentConfirmService agentConfirmService;

    public AgentServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String chat(List<Map<String, String>> messages, Integer userId) {
        CURRENT_USER.set(userId);
        WRITE_OP.set(false);
        try {
            List<Message> chatMessages = buildChatMessages(messages);
            if (chatMessages.isEmpty()) {
                return "请先发送消息";
            }
            if (chatMessages.size() > MAX_HISTORY_MESSAGES) {
                chatMessages = chatMessages.subList(chatMessages.size() - MAX_HISTORY_MESSAGES, chatMessages.size());
            }

            LocalDateTime now = LocalDateTime.now();
            String dateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String dayOfWeek = getChineseDayOfWeek(now.getDayOfWeek());
            String systemWithContext = SYSTEM_PROMPT
                    + "\n[当前时间] " + dateTime + " " + dayOfWeek
                    + "\n[当前用户ID] " + userId;

            String cacheKey = md5(systemWithContext + chatMessages.toString());
            if (chatMessages.size() <= 1) {
                CacheEntry cached = RESPONSE_CACHE.get(cacheKey);
                if (cached != null && !cached.isExpired()) {
                    log.info("[AgentCache] HIT key={}", cacheKey);
                    return cached.response;
                }
            }

            if (RESPONSE_CACHE.size() > CACHE_MAX_ENTRIES) {
                RESPONSE_CACHE.entrySet().removeIf(e -> e.getValue().isExpired());
            }

            log.info("[AgentCache] MISS key={} userId={} turns={}", cacheKey, userId, chatMessages.size());
            String response = chatClient.prompt()
                    .system(systemWithContext)
                    .messages(chatMessages)
                    .tools(this)
                    .call()
                    .content();

            if (!WRITE_OP.get() && chatMessages.size() <= 1) {
                RESPONSE_CACHE.put(cacheKey, new CacheEntry(response, System.currentTimeMillis() + CACHE_TTL_MS));
            } else if (WRITE_OP.get()) {
                log.info("[AgentCache] SKIP write operation userId={}", userId);
            } else {
                log.info("[AgentCache] SKIP multi-turn cache userId={} turns={}", userId, chatMessages.size());
            }
            return response;
        } finally {
            CURRENT_USER.remove();
            WRITE_OP.remove();
        }
    }

    private List<Message> buildChatMessages(List<Map<String, String>> messages) {
        List<Message> result = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            if (msg == null) {
                continue;
            }
            String role = msg.get("role");
            String content = normalizeHistoryContent(msg.get("content"));
            if (content.isBlank()) {
                continue;
            }
            if ("user".equalsIgnoreCase(role)) {
                result.add(new UserMessage(content));
            } else if ("assistant".equalsIgnoreCase(role)) {
                result.add(new AssistantMessage(content));
            }
        }
        return result;
    }

    /** 历史消息去掉 HTML，便于模型理解上下文 */
    private String normalizeHistoryContent(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void markWrite(String action, Object detail) {
        WRITE_OP.set(true);
        log.info("[Agent] userId={} action={} detail={}", CURRENT_USER.get(), action, detail);
    }

    /** AI 助手代操作时：记录当前登录用户 */
    private void applyAgentOperator(PurchaseOrder order) {
        order.setUserId(CURRENT_USER.get());
    }

    private void applyAgentOperator(SaleOrder order) {
        order.setUserId(CURRENT_USER.get());
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
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
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

    private String formatMultipleMatches(String entityLabel, List<?> items) {
        StringBuilder sb = new StringBuilder("找到多个").append(entityLabel).append("，请说具体是哪一个：\n");
        sb.append("<table style=\"width:100%;border-collapse:collapse;margin:12px 0;font-size:14px;\">");
        sb.append("<thead><tr style=\"background:#f5f5f5;\">");
        sb.append("<th style=\"border:1px solid #ddd;padding:8px 12px;text-align:left;\">ID</th>");
        sb.append("<th style=\"border:1px solid #ddd;padding:8px 12px;text-align:left;\">名称</th>");
        sb.append("</tr></thead><tbody>");
        for (Object item : items) {
            if (item instanceof Customer c) {
                sb.append(row(c.getId(), c.getCustName()));
            } else if (item instanceof Good g) {
                sb.append(row(g.getId(), g.getGoodsName()));
            } else if (item instanceof Supplier s) {
                sb.append(row(s.getId(), s.getSupplierName()));
            }
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String row(Integer id, String name) {
        return "<tr><td style=\"border:1px solid #ddd;padding:8px 12px;\">" + id
                + "</td><td style=\"border:1px solid #ddd;padding:8px 12px;\">" + name + "</td></tr>";
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

    @Tool(description = "按客户名称模糊搜索，用户说客户名字时用这个方法")
    public List<Customer> searchCustomersByName(
            @ToolParam(description = "客户名称关键词") String keyword) {
        return customerService.searchByName(keyword);
    }

    @Tool(description = "查询全部供应商列表")
    public List<Supplier> getAllSuppliers() {
        return supplierService.list();
    }

    @Tool(description = "根据ID查询单个供应商详情")
    public Supplier getSupplierById(Integer id) {
        return supplierService.getById(id);
    }

    @Tool(description = "按供应商名称模糊搜索，用户说供应商名字时用这个方法")
    public List<Supplier> searchSuppliersByName(
            @ToolParam(description = "供应商名称关键词") String keyword) {
        return supplierService.searchByName(keyword);
    }

    @Tool(description = "查询所有商品的库存列表")
    public List<Good> getGoodsList() {
        return goodsService.listAll();
    }

    @Tool(description = "根据ID查询单个商品详情")
    public Good getGoodsById(Integer id) {
        return goodsService.getById(id);
    }

    @Tool(description = "按商品名称模糊搜索，用户说商品名字时用这个方法")
    public List<Good> searchGoodsByName(
            @ToolParam(description = "商品名称关键词") String keyword) {
        return goodsService.searchByName(keyword);
    }

    @Tool(description = "查询库存低于指定数量的商品，用于库存预警")
    public List<Good> getLowStockGoods(Integer threshold) {
        return goodsService.listAll().stream()
                .filter(g -> g.getStockNum() != null && g.getStockNum() < threshold)
                .collect(Collectors.toList());
    }

    @Tool(description = "查询商品分类列表")
    public List<GoodsType> getGoodsTypes() {
        return goodsTypeService.list();
    }

    @Tool(description = "查询待入库采购单和待出库销售单的数量摘要")
    public String getPendingOrdersSummary() {
        long pendingPurchase = purchaseService.listAll(1).size();
        long pendingSale = saleOrderService.listAll(1).size();
        return "待入库采购单 " + pendingPurchase + " 张，待出库销售单 " + pendingSale + " 张";
    }

    @Tool(description = "查询采购订单列表，status：1未入库 2已入库 3已完成 4已取消，null查全部")
    public List<PurchaseOrder> getPurchaseOrders(Integer status) {
        return purchaseService.listAll(status);
    }

    @Tool(description = "根据ID查询单个采购订单详情")
    public PurchaseOrder getPurchaseOrderById(Integer id) {
        return purchaseService.getById(id);
    }

    @Tool(description = "查询销售订单列表，status：1未出库 2已出库 3已完成 4已取消，null查全部")
    public List<SaleOrder> getSaleOrders(Integer status) {
        return saleOrderService.listAll(status);
    }

    @Tool(description = "根据ID查询单个销售订单详情")
    public SaleOrder getSaleOrderById(Integer id) {
        return saleOrderService.getById(id);
    }

    // ===================== 组合业务工具 =====================

    @Tool(description = "按客户名和商品名创建销售单，自动搜索并校验库存，用户说名字下单时优先用这个")
    public String createSaleOrderByNames(
            @ToolParam(description = "客户名称") String customerName,
            @ToolParam(description = "商品名称") String goodsName,
            @ToolParam(description = "销售数量") Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return "销售数量必须大于 0";
        }

        List<Customer> customers = customerService.searchByName(customerName);
        if (customers.isEmpty()) {
            return "没找到叫「" + customerName + "」的客户，你确认一下名字？";
        }
        if (customers.size() > 1) {
            return formatMultipleMatches("客户", customers);
        }
        Customer customer = customers.get(0);

        List<Good> goods = goodsService.searchByName(goodsName);
        if (goods.isEmpty()) {
            return "没找到叫「" + goodsName + "」的商品，你确认一下名字？";
        }
        if (goods.size() > 1) {
            return formatMultipleMatches("商品", goods);
        }
        Good good = goods.get(0);

        if (good.getStockNum() == null || good.getStockNum() < quantity) {
            int stock = good.getStockNum() == null ? 0 : good.getStockNum();
            return "库存不够哦，「" + good.getGoodsName() + "」现在只剩 " + stock + " 件";
        }

        SaleOrder order = new SaleOrder();
        order.setCustId(customer.getId());
        order.setGoodsId(good.getId());
        order.setSaleNum(quantity);
        order.setStatus(1);
        applyAgentOperator(order);
        saleOrderService.addOrder(order);
        markWrite("createSaleOrderByNames", Map.of(
                "customer", customer.getCustName(),
                "goods", good.getGoodsName(),
                "quantity", quantity));
        return "搞定！给「" + customer.getCustName() + "」下了 " + quantity
                + " 件「" + good.getGoodsName() + "」的销售单";
    }

    @Tool(description = "按供应商名和商品名创建采购单，自动搜索匹配，用户说名字下采购单时优先用这个")
    public String createPurchaseOrderByNames(
            @ToolParam(description = "供应商名称") String supplierName,
            @ToolParam(description = "商品名称") String goodsName,
            @ToolParam(description = "采购数量") Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return "采购数量必须大于 0";
        }

        List<Supplier> suppliers = supplierService.searchByName(supplierName);
        if (suppliers.isEmpty()) {
            return "没找到叫「" + supplierName + "」的供应商，你确认一下名字？";
        }
        if (suppliers.size() > 1) {
            return formatMultipleMatches("供应商", suppliers);
        }
        Supplier supplier = suppliers.get(0);

        List<Good> goods = goodsService.searchByName(goodsName);
        if (goods.isEmpty()) {
            return "没找到叫「" + goodsName + "」的商品，你确认一下名字？";
        }
        if (goods.size() > 1) {
            return formatMultipleMatches("商品", goods);
        }
        Good good = goods.get(0);

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(supplier.getId());
        order.setGoodsId(good.getId());
        order.setBuyNum(quantity);
        order.setStatus(1);
        applyAgentOperator(order);
        purchaseService.addOrder(order);
        markWrite("createPurchaseOrderByNames", Map.of(
                "supplier", supplier.getSupplierName(),
                "goods", good.getGoodsName(),
                "quantity", quantity));
        return "搞定！向「" + supplier.getSupplierName() + "」采购了 " + quantity
                + " 件「" + good.getGoodsName() + "」";
    }

    // ===================== 新增工具 =====================

    @Tool(description = "新增客户")
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
        markWrite("addCustomer", custName);
        return "客户「" + custName + "」新增成功！";
    }

    @Tool(description = "新增供应商")
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
        markWrite("addSupplier", supplierName);
        return "供应商「" + supplierName + "」新增成功！";
    }

    @Tool(description = "新增商品")
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
        markWrite("addGoods", goodsName);
        return "商品「" + goodsName + "」新增成功！";
    }

    @Tool(description = "新增采购订单（需要 goodsId 和 supplierId）")
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
        applyAgentOperator(order);
        purchaseService.addOrder(order);
        markWrite("addPurchaseOrder", order);
        return "采购订单新增成功！";
    }

    @Tool(description = "新增销售订单（需要 goodsId 和 custId）")
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
        applyAgentOperator(order);
        saleOrderService.addOrder(order);
        markWrite("addSaleOrder", order);
        return "销售订单新增成功！";
    }

    // ===================== 修改工具 =====================

    @Tool(description = "修改客户信息，只改用户提到的字段")
    public String updateCustomer(
            @ToolParam(description = "客户ID（必填）") Integer id,
            @ToolParam(description = "客户名称") String custName,
            @ToolParam(description = "联系人") String contactPreson,
            @ToolParam(description = "电话") String phone,
            @ToolParam(description = "地址") String address,
            @ToolParam(description = "备注") String remark) {
        Customer existing = customerService.getById(id);
        if (existing == null) {
            return "客户 ID " + id + " 不存在";
        }
        if (custName != null && !custName.isBlank()) {
            existing.setCustName(custName);
        }
        if (contactPreson != null && !contactPreson.isBlank()) {
            existing.setContactPreson(contactPreson);
        }
        if (phone != null && !phone.isBlank()) {
            existing.setPhone(phone);
        }
        if (address != null && !address.isBlank()) {
            existing.setAddress(address);
        }
        if (remark != null) {
            existing.setRemark(remark);
        }
        customerService.updateById(existing);
        markWrite("updateCustomer", id);
        return "客户（ID:" + id + "）修改成功！";
    }

    @Tool(description = "修改供应商信息，只改用户提到的字段")
    public String updateSupplier(
            @ToolParam(description = "供应商ID（必填）") Integer id,
            @ToolParam(description = "供应商名称") String supplierName,
            @ToolParam(description = "联系人") String contactPerson,
            @ToolParam(description = "电话") String phone,
            @ToolParam(description = "地址") String address,
            @ToolParam(description = "备注") String remark) {
        Supplier existing = supplierService.getById(id);
        if (existing == null) {
            return "供应商 ID " + id + " 不存在";
        }
        if (supplierName != null && !supplierName.isBlank()) {
            existing.setSupplierName(supplierName);
        }
        if (contactPerson != null && !contactPerson.isBlank()) {
            existing.setContactPerson(contactPerson);
        }
        if (phone != null && !phone.isBlank()) {
            existing.setPhone(phone);
        }
        if (address != null && !address.isBlank()) {
            existing.setAddress(address);
        }
        if (remark != null) {
            existing.setRemark(remark);
        }
        supplierService.update(existing);
        markWrite("updateSupplier", id);
        return "供应商（ID:" + id + "）修改成功！";
    }

    @Tool(description = "修改商品信息，只改用户提到的字段")
    public String updateGoods(
            @ToolParam(description = "商品ID（必填）") Integer id,
            @ToolParam(description = "商品名称") String goodsName,
            @ToolParam(description = "商品类别ID") Integer typeId,
            @ToolParam(description = "进货单价") Double buyPrice,
            @ToolParam(description = "销售单价") Double sellPrice,
            @ToolParam(description = "库存数量") Integer stockNum,
            @ToolParam(description = "备注") String remark) {
        Good existing = goodsService.getById(id);
        if (existing == null) {
            return "商品 ID " + id + " 不存在";
        }
        if (goodsName != null && !goodsName.isBlank()) {
            existing.setGoodsName(goodsName);
        }
        if (typeId != null) {
            existing.setTypeId(typeId);
        }
        if (buyPrice != null) {
            existing.setBuyPrice(buyPrice);
        }
        if (sellPrice != null) {
            existing.setSellPrice(sellPrice);
        }
        if (stockNum != null) {
            existing.setStockNum(stockNum);
        }
        if (remark != null) {
            existing.setRemark(remark);
        }
        goodsService.update(existing);
        markWrite("updateGoods", id);
        return "商品（ID:" + id + "）修改成功！";
    }

    // ===================== 确认机制：危险操作 =====================

    @Tool(description = "执行已确认的操作。用户回复「确认 确认码」时调用，传入确认码")
    public String confirmAction(@ToolParam(description = "8位确认码") String confirmToken) {
        markWrite("confirmAction", confirmToken);
        return agentConfirmService.execute(confirmToken, CURRENT_USER.get());
    }

    @Tool(description = "准备删除客户，不会立即删除，返回确认码")
    public String prepareDeleteCustomer(@ToolParam(description = "客户ID") Integer id) {
        String detail = agentConfirmService.describeCustomer(id);
        if (detail.startsWith("ID=") && customerService.getById(id) == null) {
            return "客户 ID " + id + " 不存在";
        }
        String token = agentConfirmService.create("deleteCustomer", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("删除客户", detail, token);
    }

    @Tool(description = "准备删除供应商，不会立即删除，返回确认码")
    public String prepareDeleteSupplier(@ToolParam(description = "供应商ID") Integer id) {
        String detail = agentConfirmService.describeSupplier(id);
        if (supplierService.getById(id) == null) {
            return "供应商 ID " + id + " 不存在";
        }
        String token = agentConfirmService.create("deleteSupplier", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("删除供应商", detail, token);
    }

    @Tool(description = "准备删除商品，不会立即删除，返回确认码")
    public String prepareDeleteGoods(@ToolParam(description = "商品ID列表") List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return "请提供要删除的商品 ID";
        }
        String detail = agentConfirmService.describeGoods(ids);
        String token = agentConfirmService.create("deleteGoods", CURRENT_USER.get(), Map.of("ids", ids));
        return agentConfirmService.buildConfirmMessage("删除商品", detail, token);
    }

    @Tool(description = "准备删除采购订单，不会立即删除，返回确认码")
    public String prepareDeletePurchaseOrder(@ToolParam(description = "采购订单ID") Integer id) {
        if (purchaseService.getById(id) == null) {
            return "采购订单 ID " + id + " 不存在";
        }
        String detail = agentConfirmService.describePurchaseOrder(id);
        String token = agentConfirmService.create("deletePurchaseOrder", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("删除采购订单", detail, token);
    }

    @Tool(description = "准备删除销售订单，不会立即删除，返回确认码")
    public String prepareDeleteSaleOrder(@ToolParam(description = "销售订单ID") Integer id) {
        if (saleOrderService.getById(id) == null) {
            return "销售订单 ID " + id + " 不存在";
        }
        String detail = agentConfirmService.describeSaleOrder(id);
        String token = agentConfirmService.create("deleteSaleOrder", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("删除销售订单", detail, token);
    }

    @Tool(description = "准备采购入库，不会立即入库，返回确认码")
    public String prepareInStockPurchaseOrder(@ToolParam(description = "采购订单ID") Integer id) {
        if (purchaseService.getById(id) == null) {
            return "采购订单 ID " + id + " 不存在";
        }
        String detail = agentConfirmService.describePurchaseOrder(id);
        String token = agentConfirmService.create("inStockPurchase", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("采购入库", detail, token);
    }

    @Tool(description = "准备销售出库，不会立即出库，返回确认码")
    public String prepareOutStockSaleOrder(@ToolParam(description = "销售订单ID") Integer id) {
        if (saleOrderService.getById(id) == null) {
            return "销售订单 ID " + id + " 不存在";
        }
        String detail = agentConfirmService.describeSaleOrder(id);
        String token = agentConfirmService.create("outStockSale", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("销售出库", detail, token);
    }

    @Tool(description = "准备取消采购订单，不会立即取消，返回确认码")
    public String prepareCancelPurchaseOrder(@ToolParam(description = "采购订单ID") Integer id) {
        if (purchaseService.getById(id) == null) {
            return "采购订单 ID " + id + " 不存在";
        }
        String detail = agentConfirmService.describePurchaseOrder(id);
        String token = agentConfirmService.create("cancelPurchase", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("取消采购订单", detail, token);
    }

    @Tool(description = "准备取消销售订单，不会立即取消，返回确认码")
    public String prepareCancelSaleOrder(@ToolParam(description = "销售订单ID") Integer id) {
        if (saleOrderService.getById(id) == null) {
            return "销售订单 ID " + id + " 不存在";
        }
        String detail = agentConfirmService.describeSaleOrder(id);
        String token = agentConfirmService.create("cancelSale", CURRENT_USER.get(), Map.of("id", id));
        return agentConfirmService.buildConfirmMessage("取消销售订单", detail, token);
    }
}
