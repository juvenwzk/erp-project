package com.kangcode.Controller;

import com.kangcode.Service.SaleOrderService;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.Result;
import com.kangcode.pojo.SaleOrder;
import com.kangcode.pojo.SaleOrderQueryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/sale-order")
public class SaleOrderController {
    @Autowired
    private SaleOrderService saleOrderService;

    // 新增订单
    @PostMapping
    public Result add(@RequestBody SaleOrder saleOrder){
        log.info("添加销售订单: {}", saleOrder);
        saleOrderService.addOrder(saleOrder);
        return Result.success();
    }

    //2.修改订单
    @PutMapping
    public Result update(@RequestBody SaleOrder saleOrder){
        log.info("修改销售订单: {}", saleOrder);
        saleOrderService.updateOrder(saleOrder);
        return Result.success();
    }

    //3.获取单个订单
    @GetMapping("/{id}")
    public Result get(@PathVariable Integer id){
        log.info("查询订单: {}", id);
        SaleOrder saleOrder = saleOrderService.getById(id);
        return Result.success(saleOrder);
    }
    //4.删除订单
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id){
        log.info("删除订单: {}", id);
        saleOrderService.deleteById(id);
        return Result.success();
    }


    //5.订单出库
    @PostMapping("/outStock/{id}")
    public Result outStock(@PathVariable Integer id){
        log.info("订单出库: {}", id);
        saleOrderService.outStock(id);
        return Result.success();
    }


    //6.取消订单
    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable Integer id){
        log.info("取消订单: {}", id);
        saleOrderService.cancel(id);
        return Result.success();
    }


    //7.条件分页查询 Get
    @GetMapping("/list")
    public Result list(SaleOrderQueryParam saleOrderQueryParam){
        log.info("分页条件查询：{}", saleOrderQueryParam);
        PageResult pageResult=saleOrderService.page(saleOrderQueryParam);
        return Result.success(pageResult);
    }



















}
