package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.GoodsMapper;
import com.kangcode.Service.GoodsService;
import com.kangcode.Service.GoodsSupplierService;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsQueryParam;
import com.kangcode.pojo.GoodsSupplierVO;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl implements GoodsService {
    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsSupplierService goodsSupplierService;


    @Override
    public List<Good> listAll() {
        return goodsMapper.listAll();
    }

    @Override
    public void add(Good good) {
        good.setCreateTime(LocalDateTime.now());
        good.setUpdateTime(LocalDateTime.now());

        goodsMapper.insert(good);
        if (good.getSupplierIds() != null && !good.getSupplierIds().isEmpty()) {
            goodsSupplierService.bindSuppliersForGoods(good.getId(), good.getSupplierIds());
        }
    }

    @Override
    public Good getById(Integer id) {
        Good good = goodsMapper.getById(id);
        if (good == null) {
            return null;
        }
        fillSuppliers(good);
        return good;
    }

    private void fillSuppliers(Good good) {
        List<GoodsSupplierVO> relations = goodsSupplierService.listByGoodsId(good.getId());
        if (relations == null || relations.isEmpty()) {
            good.setSupplierIds(List.of());
            good.setSuppliers(List.of());
            return;
        }
        good.setSupplierIds(relations.stream().map(GoodsSupplierVO::getSupplierId).collect(Collectors.toList()));
        List<Supplier> suppliers = new ArrayList<>();
        for (GoodsSupplierVO relation : relations) {
            Supplier supplier = new Supplier();
            supplier.setId(relation.getSupplierId());
            supplier.setSupplierName(relation.getSupplierName());
            suppliers.add(supplier);
        }
        good.setSuppliers(suppliers);
        String names = relations.stream()
                .map(GoodsSupplierVO::getSupplierName)
                .collect(Collectors.joining("、"));
        good.setSupplierNames(names.isEmpty() ? null : names);
    }

    @Override
    public PageResult page(GoodsQueryParam goodsQueryParam) {
        //1.设置分页参数
        PageHelper.startPage(goodsQueryParam.getPage(), goodsQueryParam.getPageSize());
        //2.执行查询
        List<Good> empList = goodsMapper.list(goodsQueryParam);
        goodsSupplierService.fillGoodsListRelations(empList);
        //3，解析查询结合，并封装
        Page< Good> p=(Page< Good>) empList;
        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public void delete(List<Integer> ids) {
        goodsSupplierService.deleteByGoodsIds(ids);
        goodsMapper.delete(ids);
    }

    @Override
    public void update(Good good) {
        goodsMapper.update(good);
        if (good.getSupplierIds() != null) {
            goodsSupplierService.bindSuppliersForGoods(good.getId(), good.getSupplierIds());
        }
    }

    @Override
    public Object getLowStock() {
        return goodsMapper.getLowStock();
    }

    @Override
    public List<Good> searchByName(String keyword) {
        GoodsQueryParam param = new GoodsQueryParam();
        param.setGoodsName(keyword);
        param.setPage(1);
        param.setPageSize(50);
        PageResult result = page(param);
        return (List<Good>) result.getRows();
    }

}
