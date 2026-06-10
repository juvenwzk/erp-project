package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Integer id;           // 主键
    private String username;      // 登录账号（对应 login_name）
    private String password;      // 登录密码
    private String name;          // 真实姓名（对应 user_name）
    private Integer gender;       // 性别 1.男 2.女（对应 sex）
    private String phone;         // 电话号码
    private String email;         // 邮箱地址
    private Integer role;         // 职位 1.管理员 2.员工
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
