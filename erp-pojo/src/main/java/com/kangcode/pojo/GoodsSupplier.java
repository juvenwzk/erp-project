package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsSupplier {
    private Integer id;
    private Integer goodsId;
    private Integer supplierId;
    private Double supplyPrice;
    private LocalDateTime createTime;
}
