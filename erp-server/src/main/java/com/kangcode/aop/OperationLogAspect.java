package com.kangcode.aop;

import com.kangcode.Mapper.OperateLogMapper;
import com.kangcode.pojo.OperateLog;
import com.kangcode.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
@Slf4j
@Aspect
@Component

public class OperationLogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;
    @Autowired
    private JwtUtils jwtUtils;

    @Around("@annotation(com.kangcode.anno.Log)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 开始时间
        long startTime = System.currentTimeMillis();

        //执行目标方法
        Object result = joinPoint .proceed();

        //计算耗时
        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;

        //获取操作日志注解
        OperateLog olog = new OperateLog();
        olog.setUserId(getCurrentUserId());
        olog.setOperateTime(LocalDateTime.now());
        olog.setClassName(joinPoint.getTarget().getClass().getName());
        olog.setMethodName(joinPoint.getSignature().getName());
        olog.setMethodParams(truncate(Arrays.toString(joinPoint.getArgs()), 2000));
        olog.setReturnValue(truncate(result != null ? result.toString() : "void", 2000));
        olog.setCostTime(costTime);

        try {
            operateLogMapper.insert(olog);
        } catch (Exception e) {
            log.warn("操作日志写入失败（不影响业务）: {}.{}", olog.getClassName(), olog.getMethodName(), e);
        }
        return result;

    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen) + "...(truncated)";
    }

    private Integer getCurrentUserId() {
        ServletRequestAttributes attrs=
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if(attrs == null){
            return null;
        }
        String auth = attrs.getRequest().getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtils.getUserId(auth.substring(7));
        } catch (Exception e) {
            return null;
        }


    }
}
