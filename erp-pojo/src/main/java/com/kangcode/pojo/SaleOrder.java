package com.kangcode.pojo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kangcode.pojo.jackson.LenientLocalDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleOrder {
    private Integer id;
    private Integer goodsId;
    private Integer custId;
    private Integer saleNum;
    private Double salePrice;    // 本次销售单价
    private Double saleMoney;
    private Integer userId;
    private Integer status;
    private String goodsName;
    private String custName;
    private String userName;

    @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class)
    private LocalDateTime saleTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;


}
