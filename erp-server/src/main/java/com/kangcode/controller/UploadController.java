package com.kangcode.controller;

import com.kangcode.Service.UploadStorageService;
import com.kangcode.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
public class UploadController {

    @Autowired
    private UploadStorageService uploadStorageService;

    @PostMapping("/upload")
    public Result upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        log.info("上传文件: {}", file.getOriginalFilename());
        String url = uploadStorageService.store(file.getBytes(), file.getOriginalFilename());
        log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), url);
        return Result.success(url);
    }
}
