package com.kangcode.Mapper;

import com.kangcode.pojo.GoodsType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoodsTypeMapper {

    void insertById(GoodsType goodsType);

    List<GoodsType> list();

    void updateById(GoodsType goodsType);

    void deleteById(Integer id);
}
