package com.kangcode.controller;

import com.kangcode.Service.UserService;
import com.kangcode.pojo.PageResult;
import com.kangcode.pojo.Result;
import com.kangcode.pojo.User;
import com.kangcode.pojo.UserQueryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    //请求体为json格式
    @PostMapping
    public Result add(@RequestBody User user){
        log.info("添加用户:{}", user);
        userService.add(user);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody User user){
        log.info("更新用户:{}", user);
        userService.update(user);
        return Result.success();
    }
    @GetMapping
    public Result list(UserQueryParam userQueryParam){
        log.info("分页查询用户: {}", userQueryParam);
        PageResult PageResult=userService.page(userQueryParam);
        return Result.success(PageResult);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id){
        log.info("查询用户: {}",id);
        User user=userService.getById(id);
        return Result.success(user);
    }
    @DeleteMapping
    public Result delete(@RequestBody List<Integer> ids){
        log.info("删除用户:{}", ids);
        userService.delete(ids);
        return Result.success();
    }
}
