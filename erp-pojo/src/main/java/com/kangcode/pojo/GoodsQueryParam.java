package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsQueryParam {
    private Integer page;
    private Integer pageSize;
    private String goodsName;
    private Integer typeId;

}
