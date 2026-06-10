package com.kangcode.Service;

import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsQueryParam;
import com.kangcode.pojo.PageResult;

import java.util.List;

public interface GoodsService {


    List<Good> listAll();

    void add(Good good);

    Good getById(Integer id);

    PageResult page(GoodsQueryParam goodsQueryParam);

    void delete(List<Integer> ids);


    void update(Good good);

    Object getLowStock();
}
