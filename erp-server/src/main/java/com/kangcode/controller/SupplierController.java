package com.kangcode.controller;

import com.kangcode.Service.SupplierService;
import com.kangcode.pojo.Result;
import com.kangcode.pojo.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/supplier")
public class SupplierController {
    @Autowired
    private SupplierService supplierService;

    @PostMapping
    public Result add(@RequestBody  Supplier supplier){
        log.info("添加供应商:{}",supplier);
        supplierService.add(supplier);
        return Result.success(supplier.getId());
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id){
        log.info("删除供应商: {}", id);
        supplierService.deleteById(id);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Supplier supplier){
        log.info("修改供应商: {}", supplier);
        supplierService.update(supplier);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer  id){
        log.info("查询供应商: {}");
        Supplier supplier=supplierService.getById(id);
        return Result.success(supplier);
    }

    @GetMapping("/list")
    public Result list(){
      log.info("查询所有供应商");
      List<Supplier> list=supplierService.list();

      return Result.success(list);
    }





}
