package com.kangcode.exception;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.kangcode.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.NestedServletException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result handleBusiness(BusinessException e) {
        log.warn("业务异常 [{}]: {}", e.getCode(), e.getMessage());
        return Result.of(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result handleIllegalState(IllegalStateException e) {
        log.warn("业务校验失败: {}", e.getMessage());
        return Result.of(400, e.getMessage());
    }

    @ExceptionHandler(OSSException.class)
    public Result handleOss(OSSException e) {
        log.error("OSS 上传失败: {}", e.getErrorMessage(), e);
        return Result.of(500, "图片上传失败：" + formatOssMessage(e.getErrorMessage()));
    }

    @ExceptionHandler(ClientException.class)
    public Result handleOssClient(ClientException e) {
        log.error("OSS 客户端错误: {}", e.getMessage(), e);
        return Result.of(500, "图片上传失败：" + formatOssMessage(e.getMessage()));
    }

    private static String formatOssMessage(String msg) {
        if (msg == null) {
            return "请检查 OSS AccessKey、Bucket 与 Endpoint 配置";
        }
        if (msg.contains("Access Key Id") || msg.contains("InvalidAccessKeyId")) {
            return "OSS AccessKey 无效，请检查 .env 中 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET";
        }
        if (msg.contains("SignatureDoesNotMatch")) {
            return "OSS AccessKey Secret 错误，请检查 OSS_ACCESS_KEY_SECRET";
        }
        if (msg.contains("NoSuchBucket")) {
            return "OSS Bucket 不存在，请检查 ALIYUN_OSS_BUCKET 配置";
        }
        return msg;
    }

    @ExceptionHandler(NonTransientAiException.class)
    public Result handleAi(NonTransientAiException e) {
        log.error("AI 调用失败: {}", e.getMessage(), e);
        String msg = e.getMessage();
        if (msg != null && (msg.contains("401") || msg.contains("Authentication") || msg.contains("api key"))) {
            return Result.of(500, "AI 助手不可用：DeepSeek API Key 无效或未配置。请在 .env 中设置 DEEPSEEK_API_KEY 后执行 docker compose up -d --force-recreate erp-app");
        }
        return Result.of(500, "AI 助手调用失败：" + (msg != null ? msg : "未知错误"));
    }

    @ExceptionHandler(DataAccessException.class)
    public Result handleDataAccess(DataAccessException e) {
        log.warn("数据库操作失败: {}", e.getMessage());
        return Result.of(500, resolveMessage(e));
    }

    @ExceptionHandler(NestedServletException.class)
    public Result handleNestedServlet(NestedServletException e) {
        log.warn("请求处理失败: {}", e.getMessage());
        return Result.of(500, resolveMessage(e));
    }

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntime(RuntimeException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.of(500, e.getMessage() != null ? e.getMessage() : "操作失败");
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("程序出错了", e);
        return Result.of(500, resolveMessage(e));
    }

    private static String resolveMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        return "服务器出错了,请联系开发者";
    }
}
