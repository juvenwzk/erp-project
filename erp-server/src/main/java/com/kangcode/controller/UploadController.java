package com.kangcode.Controller;

import com.kangcode.pojo.Result;
import com.kangcode.utils.AliyunOSSOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
public class UploadController {


    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;



    @PostMapping("/upload")
    public Result upload(MultipartFile  file) throws Exception {
        log.info("上传文件:{}",file.getOriginalFilename());
        String url = aliyunOSSOperator.upload(file.getBytes(),file.getOriginalFilename());
        log.info("文件上传成功：{}",file.getOriginalFilename());

        return Result.success(url);
    }
}
