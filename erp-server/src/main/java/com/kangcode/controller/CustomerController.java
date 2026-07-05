package com.kangcode.controller;

import com.kangcode.Service.CustomerService;
import com.kangcode.pojo.Customer;
import com.kangcode.pojo.CustomerQueryParam;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customers")
public class CustomerController{
    @Autowired
    private CustomerService customerService;


    @PostMapping
    public Result add (@RequestBody Customer customer){
        log.info("添加客户: {}", customer);
        customerService.add(customer);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id){
        log.info("删除客户: {}", id);
        customerService.deleteById(id);
        return Result.success();
    }

    @PutMapping
    public Result update (@RequestBody Customer customer){
        log.info("更新客户: {}", customer);
        customerService.updateById(customer);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id){
        log.info("查询客户: {}", id);
        Customer customer = customerService.getById(id);
        return Result.success(customer);
    }

    @GetMapping("/list")
    public Result list(CustomerQueryParam customerQueryParam){
        log.info("分页条件查询：{}", customerQueryParam);
        PageResult pageResult=customerService.page(customerQueryParam);
        return Result.success(pageResult);
    }












}
