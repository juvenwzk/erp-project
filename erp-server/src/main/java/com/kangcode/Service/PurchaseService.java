package com.kangcode.Service;

import com.kangcode.pojo.PageResult;
import java.util.List;
import com.kangcode.pojo.PurchaseOrder;
import com.kangcode.pojo.PurchaseQueryParam;

public interface PurchaseService {
    // 采购订单状态
    int STATUS_NOT_IN = 1;    // 未入库（创建默认）
    int STATUS_INED = 2;      // 已入库（加库存）
    int STATUS_FINISHED = 3;  // 已完成
    int STATUS_CANCELED = 4;  // 已取消

    List<PurchaseOrder> listAll(Integer status);

    void addOrder(PurchaseOrder purchaseOrder);

    void updateOrder(PurchaseOrder purchaseOrder);

    PurchaseOrder getById(Integer id);

    void deleteById(Integer id);

    void inStock(Integer id);

    void cancel(Integer id);

    PageResult page(PurchaseQueryParam param);
}
