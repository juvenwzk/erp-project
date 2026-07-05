package com.kangcode.Service;

import com.kangcode.pojo.GoodsSupplierBindDTO;
import com.kangcode.pojo.GoodsSupplierVO;
import com.kangcode.pojo.Good;
import com.kangcode.pojo.Supplier;

import java.util.List;

public interface GoodsSupplierService {

    List<GoodsSupplierVO> listByGoodsId(Integer goodsId);

    List<GoodsSupplierVO> listBySupplierId(Integer supplierId);

    List<GoodsSupplierVO> listAll();

    void bindSuppliersForGoods(Integer goodsId, List<Integer> supplierIds);

    void bindGoodsForSupplier(Integer supplierId, List<Integer> goodsIds);

    void deleteByGoodsIds(List<Integer> goodsIds);

    void deleteBySupplierId(Integer supplierId);

    void fillGoodsListRelations(List<Good> goodsList);

    void fillSupplierListRelations(List<Supplier> suppliers);
}
