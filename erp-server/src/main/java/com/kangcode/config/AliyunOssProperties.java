package com.kangcode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    private String accessKeyId = "";
    private String accessKeySecret = "";
    private String endpoint = "https://oss-cn-beijing.aliyuncs.com";
    private String bucket = "kangcode-erp";
    private String region = "cn-beijing";

    public String resolvedAccessKeyId() {
        return firstNonBlank(System.getenv("OSS_ACCESS_KEY_ID"), accessKeyId);
    }

    public String resolvedAccessKeySecret() {
        return firstNonBlank(System.getenv("OSS_ACCESS_KEY_SECRET"), accessKeySecret);
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : "";
    }
}
