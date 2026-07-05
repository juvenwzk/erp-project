package com.kangcode.controller;

import com.kangcode.Service.GoodsSupplierService;
import com.kangcode.anno.Log;
import com.kangcode.pojo.GoodsSupplierBindDTO;
import com.kangcode.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/goods-supplier")
public class GoodsSupplierController {

    @Autowired
    private GoodsSupplierService goodsSupplierService;

    @GetMapping("/by-goods/{goodsId}")
    public Result listByGoods(@PathVariable Integer goodsId) {
        log.info("查询商品关联供应商: goodsId={}", goodsId);
        return Result.success(goodsSupplierService.listByGoodsId(goodsId));
    }

    @GetMapping("/by-supplier/{supplierId}")
    public Result listBySupplier(@PathVariable Integer supplierId) {
        log.info("查询供应商关联商品: supplierId={}", supplierId);
        return Result.success(goodsSupplierService.listBySupplierId(supplierId));
    }

    @Log
    @PutMapping("/goods/{goodsId}")
    public Result bindSuppliers(@PathVariable Integer goodsId, @RequestBody GoodsSupplierBindDTO dto) {
        log.info("绑定商品供应商: goodsId={}, supplierIds={}", goodsId, dto != null ? dto.getSupplierIds() : null);
        goodsSupplierService.bindSuppliersForGoods(goodsId, dto != null ? dto.getSupplierIds() : null);
        return Result.success();
    }

    @Log
    @PutMapping("/supplier/{supplierId}")
    public Result bindGoods(@PathVariable Integer supplierId, @RequestBody GoodsSupplierBindDTO dto) {
        log.info("绑定供应商商品: supplierId={}, goodsIds={}", supplierId, dto != null ? dto.getGoodsIds() : null);
        goodsSupplierService.bindGoodsForSupplier(supplierId, dto != null ? dto.getGoodsIds() : null);
        return Result.success();
    }
}
