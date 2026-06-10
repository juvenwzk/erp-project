package com.kangcode.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
@Data
public class UserQueryParam {
    private Integer page;//页码
    private Integer pageSize;//每页记录数
    private String name;// 姓名
    private Integer gender;//性别
    private String phone;//电话
    private Integer role;//

    //入职日期的区间
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate begin;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;
}
