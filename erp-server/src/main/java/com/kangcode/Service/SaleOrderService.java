package com.kangcode.Service;

import com.kangcode.pojo.PageResult;
import java.util.List;
import com.kangcode.pojo.SaleOrder;
import com.kangcode.pojo.SaleOrderQueryParam;

public interface SaleOrderService {
    List<SaleOrder> listAll(Integer status);

    // 订单状态
    int STATUS_NOT_OUT = 1;    // 未出库（创建默认）
    int STATUS_OUTED = 2;      // 已出库（扣库存）
    int STATUS_FINISHED = 3;    // 已完成
    int STATUS_CANCELED = 4;    // 已取消
    void addOrder(SaleOrder saleOrder);

    void updateOrder(SaleOrder saleOrder);

    SaleOrder getById(Integer id);

    void deleteById(Integer id);

    void outStock(Integer id);

    void cancel(Integer id);

    PageResult page(SaleOrderQueryParam saleOrderQueryParam);
}
