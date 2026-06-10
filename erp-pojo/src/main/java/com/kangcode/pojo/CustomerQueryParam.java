package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerQueryParam {

    private Integer page;
    private Integer pageSize;
    private String custName;
    private String phone;

}
