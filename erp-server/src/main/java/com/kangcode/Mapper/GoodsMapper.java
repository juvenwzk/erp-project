package com.kangcode.Mapper;

import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoodsMapper {


    List<Good> listAll();

    void insert(Good good);

    Good getById(Integer id);

    List<Good> list(GoodsQueryParam goodsQueryParam);

    void delete(List<Integer> ids);

    void update(Good good);


    List<Good> getLowStock();
}
