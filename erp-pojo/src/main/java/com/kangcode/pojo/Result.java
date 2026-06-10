package com.kangcode.pojo;

import lombok.Data;

@Data
public class Result {
    private Integer code; // 200成功, 500失败, 401未登录, 403无权限
    private String message;//返回的信息
    private Object data;//响应数据

    public static Result success(){
        Result result=new Result();
        result.code=200;
        result.message="成功";
        return result;
    }


    public static Result success(Object data){
        Result result=new Result();
        result.data=data;
        result.code=200;
        result.message="成功";
        return result;
    }


    public static Result error(Object data) {
        return of(500, "失败", data);
    }

    public static Result of(int code, String message) {
        return of(code, message, null);
    }

    public static Result of(int code, String message, Object data) {
        Result result = new Result();
        result.code = code;
        result.message = message;
        result.data = data;
        return result;
    }
}
