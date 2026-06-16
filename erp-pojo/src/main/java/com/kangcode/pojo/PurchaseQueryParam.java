package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseQueryParam {
    private Integer page = 1;
    private Integer pageSize = 10;

    private Integer status;
    private Integer goodsId;
    private Integer supplierId;
    private Integer userId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime buyTimeStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime buyTimeEnd;

    /** 与前端 purchaseTimeStart/End 对齐 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchaseTimeStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchaseTimeEnd;

    public LocalDateTime getBuyTimeStart() {
        return buyTimeStart != null ? buyTimeStart : purchaseTimeStart;
    }

    public LocalDateTime getBuyTimeEnd() {
        return buyTimeEnd != null ? buyTimeEnd : purchaseTimeEnd;
    }
}
