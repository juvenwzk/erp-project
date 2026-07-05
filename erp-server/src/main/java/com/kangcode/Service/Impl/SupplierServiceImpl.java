package com.kangcode.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.kangcode.Mapper.SupplierMapper;
import com.kangcode.Service.GoodsSupplierService;
import com.kangcode.Service.SupplierService;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsSupplierVO;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl implements SupplierService {
    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private GoodsSupplierService goodsSupplierService;


    @Override
    public void add(Supplier supplier) {
        if (supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            throw new RuntimeException("供应商名称不能为空");
        }
        supplierMapper.insert(supplier);
        if (supplier.getGoodsIds() != null && !supplier.getGoodsIds().isEmpty()) {
            goodsSupplierService.bindGoodsForSupplier(supplier.getId(), supplier.getGoodsIds());
        }
    }

    @Override
    public void deleteById(Integer id) {
        goodsSupplierService.deleteBySupplierId(id);
        supplierMapper.deleteById(id);
    }

    @Override
    public void update(Supplier supplier) {
        supplierMapper.updateById(supplier);
        if (supplier.getGoodsIds() != null) {
            goodsSupplierService.bindGoodsForSupplier(supplier.getId(), supplier.getGoodsIds());
        }
    }

    @Override
    public Supplier getById(Integer id) {
        Supplier supplier = supplierMapper.getById(id);
        if (supplier == null) {
            return null;
        }
        fillGoods(supplier);
        return supplier;
    }

    private void fillGoods(Supplier supplier) {
        List<GoodsSupplierVO> relations = goodsSupplierService.listBySupplierId(supplier.getId());
        if (relations == null || relations.isEmpty()) {
            supplier.setGoodsIds(List.of());
            supplier.setGoods(List.of());
            return;
        }
        supplier.setGoodsIds(relations.stream().map(GoodsSupplierVO::getGoodsId).collect(Collectors.toList()));
        List<Good> goods = new ArrayList<>();
        for (GoodsSupplierVO relation : relations) {
            Good good = new Good();
            good.setId(relation.getGoodsId());
            good.setGoodsName(relation.getGoodsName());
            goods.add(good);
        }
        supplier.setGoods(goods);
        String names = relations.stream()
                .map(GoodsSupplierVO::getGoodsName)
                .collect(Collectors.joining("、"));
        supplier.setGoodsNames(names.isEmpty() ? null : names);
    }

    @Override
    public List<Supplier> list() {
       List<Supplier> suppliers = supplierMapper.list();
       goodsSupplierService.fillSupplierListRelations(suppliers);
       return suppliers;
    }

    @Override
    public List<Supplier> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return supplierMapper.searchByName(keyword.trim());
    }
}
