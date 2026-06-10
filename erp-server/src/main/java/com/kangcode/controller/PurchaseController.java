package com.kangcode.Controller;

import com.kangcode.Service.PurchaseService;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.PurchaseOrder;
import com.kangcode.pojo.PurchaseQueryParam;
import com.kangcode.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/purchase-order")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    // 1. 新增采购订单
    @PostMapping
    public Result add(@RequestBody PurchaseOrder purchaseOrder) {
        log.info("添加采购订单: {}", purchaseOrder);
        purchaseService.addOrder(purchaseOrder);
        return Result.success();
    }

    // 2. 修改采购订单
    @PutMapping
    public Result update(@RequestBody PurchaseOrder purchaseOrder) {
        log.info("修改采购订单: {}", purchaseOrder);
        purchaseService.updateOrder(purchaseOrder);
        return Result.success();
    }

    // 3. 获取单个采购订单
    @GetMapping("/{id}")
    public Result get(@PathVariable Integer id) {
        log.info("查询采购订单: {}", id);
        PurchaseOrder purchaseOrder = purchaseService.getById(id);
        return Result.success(purchaseOrder);
    }

    // 4. 删除采购订单
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("删除采购订单: {}", id);
        purchaseService.deleteById(id);
        return Result.success();
    }

    // 5. 采购入库
    @PostMapping("/inStock/{id}")
    public Result inStock(@PathVariable Integer id) {
        log.info("采购入库: {}", id);
        purchaseService.inStock(id);
        return Result.success();
    }

    // 6. 取消采购订单
    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable Integer id) {
        log.info("取消采购订单: {}", id);
        purchaseService.cancel(id);
        return Result.success();
    }

    // 7. 条件分页查询
    @GetMapping("/list")
    public Result list(PurchaseQueryParam param) {
        log.info("分页条件查询采购订单: {}", param);
        PageResult pageResult = purchaseService.page(param);
        return Result.success(pageResult);
    }
}
