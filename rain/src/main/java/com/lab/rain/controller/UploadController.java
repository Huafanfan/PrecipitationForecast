package com.lab.rain.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.lab.rain.service.BuildProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Alex
 * @version 1.0
 * @date 2020/12/3 15:59
 */

@RestController
@Slf4j
@RequestMapping("/upload")
public class UploadController {
    @Value("${spring.servlet.multipart.location}")
    private String fileTempPath;

    @PostMapping(value = "/baseFiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dict local(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Dict.create().set("code", 400).set("message", "empty");
        }
        String fileName = file.getOriginalFilename();
        String rawFileName = StrUtil.subBefore(fileName, ".", true);
        String fileType = StrUtil.subAfter(fileName, ".", true);
        String localFilePath = StrUtil.appendIfMissing(fileTempPath, "/") + rawFileName + "." + fileType;
        try {
            file.transferTo(new File(localFilePath));
            //file.transferTo(new File(localFilePathBd));
        } catch (IOException e) {
            log.error("【文件上传至本地】失败，绝对路径：{}", localFilePath);
            //log.error("【文件上传至本地】失败，绝对路径：{}", localFilePathBd);
            return Dict.create().set("code", 500).set("message", "fail");
        }
        log.info("【文件上传至本地】绝对路径：{}", localFilePath);
        //log.info("【文件上传至本地】绝对路径：{}", localFilePathBd);
        return Dict.create().set("code", 200).set("message", "success").set("data", Dict.create().set("fileName", fileName).set("filePath", localFilePath));
    }

    //@GetMapping(value = "/download")
    //public Dict download(@RequestParam(required = false, name = "year") String year, @RequestParam(required = false, name = "doyStart") String doyStart, @RequestParam(required = false, name = "doyEnd") String doyEnd){
    //    buildProjectService.downloadRinexBatch(year, doyStart, doyEnd);
    //    return Dict.create().set("code", 200).set("message", "success");
    //}
}
