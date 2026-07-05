package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsSupplierBindDTO {
    private List<Integer> supplierIds;
    private List<Integer> goodsIds;
}
