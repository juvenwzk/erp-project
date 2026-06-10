package com.kangcode.Service;

import com.kangcode.pojo.GoodsType;

import java.util.List;

public interface GoodsTypeService {
    void add(GoodsType goodsType);

    List<GoodsType> list();

    void update(GoodsType goodsType);

    void deleteById(Integer id);
}
