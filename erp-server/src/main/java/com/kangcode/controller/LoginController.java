package com.kangcode.Controller;

import com.kangcode.Service.UserService;
import com.kangcode.pojo.LoginInfo;
import com.kangcode.pojo.Result;
import com.kangcode.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@Slf4j
public class LoginController {

    @Autowired
    private UserService userservice;
    @PostMapping
    public Result login(@RequestBody User user) {
        log.info("登录：{}",user);
        LoginInfo info =userservice.login(user);

        if(info==null)return Result.error("用户名不存在，或者密码错误");

        return Result.success(info);

    }
}
