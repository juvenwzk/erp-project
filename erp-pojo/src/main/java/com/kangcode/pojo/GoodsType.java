package com.kangcode.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsType {
    private Integer id;
    private String typeName;
    private String remark;
}
