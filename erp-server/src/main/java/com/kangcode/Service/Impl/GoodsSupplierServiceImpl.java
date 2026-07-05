package com.kangcode.Service.Impl;

import com.kangcode.Mapper.GoodsSupplierMapper;
import com.kangcode.Service.GoodsSupplierService;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.GoodsSupplier;
import com.kangcode.pojo.GoodsSupplierVO;
import com.kangcode.pojo.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoodsSupplierServiceImpl implements GoodsSupplierService {

    @Autowired
    private GoodsSupplierMapper goodsSupplierMapper;

    @Override
    public List<GoodsSupplierVO> listByGoodsId(Integer goodsId) {
        if (goodsId == null) {
            return List.of();
        }
        return goodsSupplierMapper.listByGoodsId(goodsId);
    }

    @Override
    public List<GoodsSupplierVO> listBySupplierId(Integer supplierId) {
        if (supplierId == null) {
            return List.of();
        }
        return goodsSupplierMapper.listBySupplierId(supplierId);
    }

    @Override
    public List<GoodsSupplierVO> listAll() {
        return goodsSupplierMapper.listAll();
    }

    @Override
    @Transactional
    public void bindSuppliersForGoods(Integer goodsId, List<Integer> supplierIds) {
        if (goodsId == null) {
            throw new IllegalStateException("商品ID不能为空");
        }
        goodsSupplierMapper.deleteByGoodsId(goodsId);
        if (supplierIds == null || supplierIds.isEmpty()) {
            return;
        }
        Set<Integer> unique = new LinkedHashSet<>(supplierIds);
        for (Integer supplierId : unique) {
            if (supplierId == null) {
                continue;
            }
            GoodsSupplier relation = new GoodsSupplier();
            relation.setGoodsId(goodsId);
            relation.setSupplierId(supplierId);
            goodsSupplierMapper.insert(relation);
        }
    }

    @Override
    @Transactional
    public void bindGoodsForSupplier(Integer supplierId, List<Integer> goodsIds) {
        if (supplierId == null) {
            throw new IllegalStateException("供应商ID不能为空");
        }
        goodsSupplierMapper.deleteBySupplierId(supplierId);
        if (goodsIds == null || goodsIds.isEmpty()) {
            return;
        }
        Set<Integer> unique = new LinkedHashSet<>(goodsIds);
        for (Integer goodsId : unique) {
            if (goodsId == null) {
                continue;
            }
            GoodsSupplier relation = new GoodsSupplier();
            relation.setGoodsId(goodsId);
            relation.setSupplierId(supplierId);
            goodsSupplierMapper.insert(relation);
        }
    }

    @Override
    @Transactional
    public void deleteByGoodsIds(List<Integer> goodsIds) {
        if (goodsIds == null || goodsIds.isEmpty()) {
            return;
        }
        goodsSupplierMapper.deleteByGoodsIds(goodsIds);
    }

    @Override
    @Transactional
    public void deleteBySupplierId(Integer supplierId) {
        if (supplierId == null) {
            return;
        }
        goodsSupplierMapper.deleteBySupplierId(supplierId);
    }

    @Override
    public void fillGoodsListRelations(List<Good> goodsList) {
        if (goodsList == null || goodsList.isEmpty()) {
            return;
        }
        Map<Integer, List<GoodsSupplierVO>> grouped = groupByGoodsId(listAll());
        for (Good good : goodsList) {
            List<GoodsSupplierVO> relations = grouped.getOrDefault(good.getId(), List.of());
            good.setSupplierIds(relations.stream().map(GoodsSupplierVO::getSupplierId).collect(Collectors.toList()));
            String names = relations.stream()
                    .map(GoodsSupplierVO::getSupplierName)
                    .collect(Collectors.joining("、"));
            good.setSupplierNames(names.isEmpty() ? null : names);
        }
    }

    @Override
    public void fillSupplierListRelations(List<Supplier> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return;
        }
        Map<Integer, List<GoodsSupplierVO>> grouped = groupBySupplierId(listAll());
        for (Supplier supplier : suppliers) {
            List<GoodsSupplierVO> relations = grouped.getOrDefault(supplier.getId(), List.of());
            supplier.setGoodsIds(relations.stream().map(GoodsSupplierVO::getGoodsId).collect(Collectors.toList()));
            String names = relations.stream()
                    .map(GoodsSupplierVO::getGoodsName)
                    .collect(Collectors.joining("、"));
            supplier.setGoodsNames(names.isEmpty() ? null : names);
        }
    }

    private Map<Integer, List<GoodsSupplierVO>> groupByGoodsId(List<GoodsSupplierVO> all) {
        Map<Integer, List<GoodsSupplierVO>> map = new LinkedHashMap<>();
        if (all == null) {
            return map;
        }
        for (GoodsSupplierVO vo : all) {
            map.computeIfAbsent(vo.getGoodsId(), k -> new ArrayList<>()).add(vo);
        }
        return map;
    }

    private Map<Integer, List<GoodsSupplierVO>> groupBySupplierId(List<GoodsSupplierVO> all) {
        Map<Integer, List<GoodsSupplierVO>> map = new LinkedHashMap<>();
        if (all == null) {
            return map;
        }
        for (GoodsSupplierVO vo : all) {
            map.computeIfAbsent(vo.getSupplierId(), k -> new ArrayList<>()).add(vo);
        }
        return map;
    }
}
