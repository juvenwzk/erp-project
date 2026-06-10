package com.kangcode.Service.Impl;

import com.kangcode.Mapper.GoodsTypeMapper;
import com.kangcode.Service.GoodsTypeService;
import com.kangcode.pojo.GoodsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
public class GoodsTypeServiceImpl implements GoodsTypeService {
    @Autowired
    private GoodsTypeMapper goodsTypeMapper;
    @Override
    public void add(GoodsType goodsType) {
        goodsTypeMapper.insertById(goodsType);

    }

    @Override
    public List<GoodsType> list() {
        List<GoodsType> list = goodsTypeMapper.list();
        return list;
    }

    @Override
    public void update(GoodsType goodsType) {
        goodsTypeMapper.updateById(goodsType);
    }

    @Override
    public void deleteById(Integer id) {
        goodsTypeMapper.deleteById(id);
    }


}
