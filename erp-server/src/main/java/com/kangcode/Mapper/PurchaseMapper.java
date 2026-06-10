package com.kangcode.Mapper;

import com.kangcode.pojo.PurchaseOrder;
import com.kangcode.pojo.PurchaseQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PurchaseMapper {
    List<PurchaseOrder> listByStatus(Integer status);

    void addOrder(PurchaseOrder purchaseOrder);

    PurchaseOrder getById(Integer id);

    void updateById(PurchaseOrder purchaseOrder);

    void deleteById(Integer id);

    List<PurchaseOrder> list(PurchaseQueryParam param);
}
