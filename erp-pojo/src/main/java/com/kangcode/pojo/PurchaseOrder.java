package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrder {
    private Integer id;
    private Integer goodsId;
    private Integer supplierId;
    private Integer buyNum;
    private Double buyPrice;     // 本次进货单价
    private Double totalMoney;
    private Integer userId;
    private Integer status;
    private String goodsName;
    private String supplierName;
    private String userName;

    private LocalDateTime buyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
