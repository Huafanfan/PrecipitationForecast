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

    @Autowired
    private BuildProjectService buildProjectService;

    @PostMapping(value = "/rinex", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
        } catch (IOException e) {
            log.error("【文件上传至本地】失败，绝对路径：{}", localFilePath);
            return Dict.create().set("code", 500).set("message", "fail");
        }
        log.info("【文件上传至本地】绝对路径：{}", localFilePath);

        //buildProjectService.downloadRinex(rawFileName);
        if (buildProjectService.buildRinexProject(rawFileName)){
            if (buildProjectService.updateTable()){
                if (buildProjectService.downloadRinex(rawFileName)){

                }
                else {
                    return Dict.create().set("code", 500).set("message", "download rinex failed");
                }
            }
            else {
                return Dict.create().set("code", 500).set("message", "update table failed");
            }
        }
        else {
            return Dict.create().set("code", 500).set("message", "build project failed");
        }

        return Dict.create().set("code", 200).set("message", "success").set("data", Dict.create().set("fileName", fileName).set("filePath", localFilePath));
    }

    @GetMapping(value = "/download")
    public Dict download(@RequestParam(required = false, name = "year") String year){
        buildProjectService.downloadRinexBatch(year);
        return Dict.create().set("code", 200).set("message", "success");
    }
}
