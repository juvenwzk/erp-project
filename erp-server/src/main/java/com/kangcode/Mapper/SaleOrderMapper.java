package com.kangcode.Mapper;

import com.kangcode.pojo.SaleOrder;
import com.kangcode.pojo.SaleOrderQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SaleOrderMapper {
    List<SaleOrder> listByStatus(Integer status);

    void addOrder(SaleOrder saleOrder);

    SaleOrder getById(Integer id);

    void updateById(SaleOrder saleOrder);

    void deleteById(Integer id);

    List<SaleOrder> list(SaleOrderQueryParam saleOrderQueryParam);
}
