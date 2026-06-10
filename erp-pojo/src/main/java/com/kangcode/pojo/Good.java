package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Good {
    private Integer id;
    private String goodsName;
    private Integer typeId;            // 商品类别ID → goods_type.id
    private Double buyPrice;           // 进货单价
    private Double sellPrice;          // 销售单价
    private Integer stockNum;          // 当前库存数量
    private String imageUrl;           // 商品图片URL
    private String remark;             // 备注
    private String typeName;           // 商品类别名称（联表查询）
    private Integer minStock;      //最低库存数量

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
