package com.kangcode.storage;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.kangcode.config.AliyunOssProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class AliyunOSSOperator {

    private final AliyunOssProperties properties;
    private final String accessKeyId;
    private final String accessKeySecret;

    public AliyunOSSOperator(AliyunOssProperties properties) {
        this.properties = properties;
        this.accessKeyId = properties.resolvedAccessKeyId();
        this.accessKeySecret = properties.resolvedAccessKeySecret();
    }

    @PostConstruct
    void logStatus() {
        if (isConfigured()) {
            log.info("阿里云 OSS 已启用: bucket={}, endpoint={}, region={}",
                    properties.getBucket(), properties.getEndpoint(), properties.getRegion());
        } else {
            log.warn("阿里云 OSS 未配置 AccessKey，商品图片将使用本地上传目录");
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(accessKeyId) && StringUtils.hasText(accessKeySecret);
    }

    public String upload(byte[] content, String originalFilename) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("OSS 未配置 AccessKey，请在环境变量或 .env 中设置 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET");
        }

        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String ext = resolveExtension(originalFilename);
        String objectName = dir + "/" + UUID.randomUUID() + ext;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.setContentType(resolveContentType(ext));
        metadata.setObjectAcl(CannedAccessControlList.PublicRead);

        PutObjectRequest putRequest = new PutObjectRequest(
                properties.getBucket(),
                objectName,
                new ByteArrayInputStream(content),
                metadata);

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(properties.getEndpoint())
                .credentialsProvider(CredentialsProviderFactory.newDefaultCredentialProvider(
                        accessKeyId, accessKeySecret))
                .clientConfiguration(clientBuilderConfiguration)
                .region(properties.getRegion())
                .build();

        try {
            ossClient.putObject(putRequest);
        } finally {
            ossClient.shutdown();
        }

        return buildPublicUrl(objectName);
    }

    private String buildPublicUrl(String objectName) {
        String host = properties.getEndpoint()
                .replace("https://", "")
                .replace("http://", "");
        return "https://" + properties.getBucket() + "." + host + "/" + objectName;
    }

    private static String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    private static String resolveContentType(String ext) {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            case ".svg" -> "image/svg+xml";
            default -> "image/jpeg";
        };
    }
}
