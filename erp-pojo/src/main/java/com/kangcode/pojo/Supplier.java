package com.kangcode.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Supplier {
    private Integer id;
    private String supplierName;
    private String contactPerson;
    private String phone ;
    private String address;
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
