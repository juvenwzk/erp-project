package com.kangcode.storage;

import com.kangcode.config.AliyunOssProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliyunOSSOperatorTest {

    @Test
    void uploadWhenConfigured() throws Exception {
        AliyunOssProperties properties = new AliyunOssProperties();
        properties.setAccessKeyId(System.getenv("OSS_ACCESS_KEY_ID"));
        properties.setAccessKeySecret(System.getenv("OSS_ACCESS_KEY_SECRET"));

        AliyunOSSOperator operator = new AliyunOSSOperator(properties);
        Assumptions.assumeTrue(operator.isConfigured(), "OSS credentials not configured");

        String url = operator.upload("erp-oss-test".getBytes(), "test.txt");
        assertNotNull(url);
        assertTrue(url.startsWith("https://"));
        assertTrue(url.contains("kangcode-erp"));
    }
}
