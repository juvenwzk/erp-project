package com.kangcode.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperateLog {
    private Integer id;
    private Integer userId;
    private String className;
    private String methodName;
    private String methodParams;
    private String returnValue;
    private Long costTime;

    private LocalDateTime operateTime;


}
