package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsSupplierVO {
    private Integer id;
    private Integer goodsId;
    private String goodsName;
    private Integer supplierId;
    private String supplierName;
    private Double supplyPrice;
    private LocalDateTime createTime;
}
