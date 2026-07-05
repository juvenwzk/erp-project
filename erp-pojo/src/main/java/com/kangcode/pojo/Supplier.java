package com.kangcode.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 关联商品ID（查询/保存时使用） */
    private List<Integer> goodsIds;
    /** 关联商品详情（查询时使用） */
    private List<Good> goods;
    /** 关联商品名称（列表展示） */
    private String goodsNames;
}
