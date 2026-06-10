package com.kangcode.Mapper;

import com.kangcode.pojo.OperateLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperateLogMapper {

    @Insert("INSERT INTO operate_log (" +
            "operate_user_id, class_name, method_name, " +
            "method_params, return_value, cost_time, operate_time) " +
            "VALUES (#{userId}, #{className}, #{methodName}, " +
            "#{methodParams}, #{returnValue}, #{costTime}, #{operateTime})")
    public void insert(OperateLog log);
}
