package com.kangcode.Mapper;

import com.kangcode.pojo.GoodsSupplier;
import com.kangcode.pojo.GoodsSupplierVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsSupplierMapper {

    List<GoodsSupplierVO> listByGoodsId(@Param("goodsId") Integer goodsId);

    List<GoodsSupplierVO> listBySupplierId(@Param("supplierId") Integer supplierId);

    void deleteByGoodsId(@Param("goodsId") Integer goodsId);

    void deleteBySupplierId(@Param("supplierId") Integer supplierId);

    void deleteByGoodsIds(@Param("goodsIds") List<Integer> goodsIds);

    void deleteBySupplierIds(@Param("supplierIds") List<Integer> supplierIds);

    void insert(GoodsSupplier relation);

    List<GoodsSupplierVO> listAll();
}
