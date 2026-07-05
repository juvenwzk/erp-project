package com.kangcode.Service;

import com.kangcode.pojo.Customer;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.PurchaseOrder;
import com.kangcode.pojo.SaleOrder;
import com.kangcode.pojo.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AgentConfirmService {

    private static final long TOKEN_TTL_MS = 300_000;

    private final ConcurrentHashMap<String, PendingAction> pending = new ConcurrentHashMap<>();

    @Autowired
    private CustomerService customerService;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private PurchaseService purchaseService;
    @Autowired
    private SaleOrderService saleOrderService;

    public record PendingAction(String type, Integer userId, Map<String, Object> params, long expireAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    public String create(String type, Integer userId, Map<String, Object> params) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        pending.put(token, new PendingAction(type, userId, params, System.currentTimeMillis() + TOKEN_TTL_MS));
        return token;
    }

    public String execute(String token, Integer userId) {
        PendingAction action = pending.remove(token);
        if (action == null) {
            return "确认码无效，可能已过期或已使用";
        }
        if (action.isExpired()) {
            return "确认码已过期，请重新发起操作";
        }
        if (userId == null || !userId.equals(action.userId())) {
            pending.put(token, action);
            return "确认码与当前登录用户不匹配";
        }

        try {
            return switch (action.type()) {
                case "deleteCustomer" -> {
                    Integer id = (Integer) action.params().get("id");
                    customerService.deleteById(id);
                    yield "客户（ID:" + id + "）已删除";
                }
                case "deleteSupplier" -> {
                    Integer id = (Integer) action.params().get("id");
                    supplierService.deleteById(id);
                    yield "供应商（ID:" + id + "）已删除";
                }
                case "deleteGoods" -> {
                    @SuppressWarnings("unchecked")
                    List<Integer> ids = (List<Integer>) action.params().get("ids");
                    goodsService.delete(ids);
                    yield "商品（ID:" + ids + "）已删除";
                }
                case "deletePurchaseOrder" -> {
                    Integer id = (Integer) action.params().get("id");
                    purchaseService.deleteById(id);
                    yield "采购订单（ID:" + id + "）已删除";
                }
                case "deleteSaleOrder" -> {
                    Integer id = (Integer) action.params().get("id");
                    saleOrderService.deleteById(id);
                    yield "销售订单（ID:" + id + "）已删除";
                }
                case "inStockPurchase" -> {
                    Integer id = (Integer) action.params().get("id");
                    purchaseService.inStock(id);
                    yield "采购订单（ID:" + id + "）已入库，库存已更新";
                }
                case "outStockSale" -> {
                    Integer id = (Integer) action.params().get("id");
                    saleOrderService.outStock(id);
                    yield "销售订单（ID:" + id + "）已出库，库存已更新";
                }
                case "cancelPurchase" -> {
                    Integer id = (Integer) action.params().get("id");
                    purchaseService.cancel(id);
                    yield "采购订单（ID:" + id + "）已取消";
                }
                case "cancelSale" -> {
                    Integer id = (Integer) action.params().get("id");
                    saleOrderService.cancel(id);
                    yield "销售订单（ID:" + id + "）已取消";
                }
                default -> "未知操作类型";
            };
        } catch (Exception e) {
            log.warn("[AgentConfirm] execute failed type={} params={}", action.type(), action.params(), e);
            return "操作失败：" + e.getMessage();
        }
    }

    public String buildConfirmMessage(String actionLabel, String detail, String token) {
        return """
                <div style="background:#e6f7ff;border-left:4px solid #1890ff;padding:16px;border-radius:0 8px 8px 0;margin:12px 0;">
                <p style="font-weight:bold;margin:0 0 8px 0;">确认一下：</p>
                <p style="margin:4px 0;">操作：%s</p>
                <p style="margin:4px 0;">详情：%s</p>
                <p style="margin:8px 0 0 0;">确认请回复：<b>确认 %s</b>（5分钟内有效）</p>
                </div>
                """.formatted(actionLabel, detail, token);
    }

    public String describeCustomer(Integer id) {
        Customer c = customerService.getById(id);
        return c == null ? "ID=" + id : "「" + c.getCustName() + "」(ID:" + id + ")";
    }

    public String describeSupplier(Integer id) {
        Supplier s = supplierService.getById(id);
        return s == null ? "ID=" + id : "「" + s.getSupplierName() + "」(ID:" + id + ")";
    }

    public String describeGoods(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return "无";
        }
        if (ids.size() == 1) {
            Good g = goodsService.getById(ids.get(0));
            return g == null ? "ID=" + ids.get(0) : "「" + g.getGoodsName() + "」(ID:" + ids.get(0) + ")";
        }
        return "商品ID " + ids;
    }

    public String describePurchaseOrder(Integer id) {
        PurchaseOrder o = purchaseService.getById(id);
        return o == null ? "ID=" + id : "采购单 #" + id + "（商品ID:" + o.getGoodsId() + "，数量:" + o.getBuyNum() + "）";
    }

    public String describeSaleOrder(Integer id) {
        SaleOrder o = saleOrderService.getById(id);
        return o == null ? "ID=" + id : "销售单 #" + id + "（商品ID:" + o.getGoodsId() + "，数量:" + o.getSaleNum() + "）";
    }
}
