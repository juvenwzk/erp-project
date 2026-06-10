package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.GoodsMapper;
import com.kangcode.Service.GoodsService;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsQueryParam;
import com.kangcode.pojo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GoodsServiceImpl implements GoodsService {
    @Autowired
    private GoodsMapper goodsMapper;


    @Override
    public List<Good> listAll() {
        return goodsMapper.listAll();
    }

    @Override
    public void add(Good good) {
        good.setCreateTime(LocalDateTime.now());
        good.setUpdateTime(LocalDateTime.now());

        goodsMapper.insert(good);

    }

    @Override
    public Good getById(Integer id) {
        Good good = goodsMapper.getById(id);
        return good;
    }

    @Override
    public PageResult page(GoodsQueryParam goodsQueryParam) {
        //1.设置分页参数
        PageHelper.startPage(goodsQueryParam.getPage(), goodsQueryParam.getPageSize());
        //2.执行查询
        List<Good> empList = goodsMapper.list(goodsQueryParam);
        //3，解析查询结合，并封装
        Page< Good> p=(Page< Good>) empList;
        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public void delete(List<Integer> ids) {
        goodsMapper.delete(ids);
    }

    @Override
    public void update(Good good) {
        goodsMapper.update(good);
    }

    @Override
    public Object getLowStock() {
        return goodsMapper.getLowStock();
    }


}
