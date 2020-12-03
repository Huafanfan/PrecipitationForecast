package com.lab.rain.service.impl;

import com.lab.rain.service.BuildProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Alex
 * @version 1.0
 * @date 2020/12/3 17:21
 */
@Slf4j
@Service
public class BuildProjectServiceImpl implements BuildProjectService {
    @Override
    public void buildRinexProject(String rinexFile) {
        String[] rinexFileNames = rinexFile.split("_");
        String time = rinexFileNames[2];
        String year = time.substring(0,4);
        String doy = time.substring(4,7);
        String cmd="sh_setup -yr " + year + " -doy " + doy;
        Runtime run = Runtime.getRuntime();
        //返回与当前 Java 应用程序相关的运行时对象
        try {
            Process p = run.exec(cmd);
            if (p.waitFor() != 0) {
                if (p.exitValue() == 1){
                    log.error("命令执行失败!");
                }
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
