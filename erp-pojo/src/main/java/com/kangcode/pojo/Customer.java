package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    private Integer id;

    private String custName;
    private String contactPreson;
    private String phone;
    private String address;
    private String remark;
    private Integer type;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
