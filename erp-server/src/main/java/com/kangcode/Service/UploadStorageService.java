package com.kangcode.Service;

import com.kangcode.storage.AliyunOSSOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class UploadStorageService {

    private final AliyunOSSOperator ossOperator;
    private final Path localRoot;
    private final String urlPrefix;

    public UploadStorageService(
            AliyunOSSOperator ossOperator,
            @Value("${app.upload.local-dir:uploads}") String localDir,
            @Value("${app.upload.local-url-prefix:/uploads}") String urlPrefix) throws IOException {
        this.ossOperator = ossOperator;
        this.localRoot = Paths.get(localDir).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        Files.createDirectories(this.localRoot);
        log.info("本地上传目录: {}", this.localRoot);
    }

    /**
     * OSS 已配置时优先走 OSS；未配置时才写入本地目录。
     */
    public String store(byte[] content, String originalFilename) throws Exception {
        if (ossOperator.isConfigured()) {
            return ossOperator.upload(content, originalFilename);
        }
        log.info("OSS 未配置，使用本地上传: {}", originalFilename);
        return storeLocally(content, originalFilename);
    }

    public Path getLocalRoot() {
        return localRoot;
    }

    private String storeLocally(byte[] content, String originalFilename) throws IOException {
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String ext = resolveExtension(originalFilename);
        String newFileName = UUID.randomUUID() + ext;

        Path targetDir = localRoot.resolve(dir);
        Files.createDirectories(targetDir);
        Files.write(targetDir.resolve(newFileName), content);

        return urlPrefix + "/" + dir + "/" + newFileName;
    }

    private static String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
