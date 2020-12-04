package com.lab.rain.service.impl;

import cn.hutool.core.util.StrUtil;
import com.lab.rain.service.BuildProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * @author Alex
 * @version 1.0
 * @date 2020/12/3 17:21
 */
@Slf4j
@Service
public class BuildProjectServiceImpl implements BuildProjectService {

    @Value("${table.location}")
    private String tablePath;

    @Override
    public boolean buildRinexProject(String rinexFile) {
        String[] rinexFileNames = rinexFile.split("_");
        String time = rinexFileNames[2];
        String year = time.substring(0,4);
        String doy = time.substring(4,7);
        String[] cmd={"sh_setup -yr " + year + " -doy " + doy,
                "cp rinexDepository/jfng"+doy+"* rinex/",
                "cp rinexDepository/lhaz"+doy+"* rinex/",
                "cp rinexDepository/ncku"+doy+"* rinex/",
                "cp rinexDepository/twtf"+doy+"* rinex/",
                "cp rinexDepository/urum"+doy+"* rinex/"};
        Runtime run = Runtime.getRuntime();
        //返回与当前 Java 应用程序相关的运行时对象
        try {
            Process p = run.exec(cmd);
            if (p.waitFor() != 0) {
                if (p.exitValue() == 1){
                    log.error("命令执行失败!");
                    return false;
                }
            }
            return true;
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateTable() {
        try {
            File sourceFile = new File(StrUtil.appendIfMissing(tablePath, "/") + "sestbl.");
            BufferedReader in = new BufferedReader(new FileReader(sourceFile));

            File targetFile = new File(StrUtil.appendIfMissing(tablePath, "/") + "sestbl.update");
            BufferedWriter out = new BufferedWriter(new FileWriter(targetFile));

            String line = null;
            String newLine = null;
            while ((line=in.readLine())!=null) {
                if (line.startsWith("Met obs source = UFL GPT 50")) {
                    newLine = line.substring(0, 17) + "RNX " + line.substring(17);
                    out.write(newLine);
                }
                else if (line.startsWith("Output met = N")) {
                    newLine = line.substring(0, 13) + "Y" + line.substring(14);
                    out.write(newLine);
                }
                else if (line.startsWith("DMap = GMF") || line.startsWith("WMap = GMF")) {
                    newLine = line.substring(0, 7) + "VMF1" + line.substring(11);
                    out.write(newLine);
                }
                else if (line.startsWith("Tides applied = 31")) {
                    newLine = line.substring(0, 16) + "23" + line.substring(18);
                    out.write(newLine);
                }
                else if (line.startsWith("Use otl.grid = Y")) {
                    newLine = line.substring(0, 15) + "N" + line.substring(16);
                    out.write(newLine);
                }
                else {
                    out.write(line);
                }
                out.newLine();
                out.flush();
            }

            in.close();
            out.close();

            if (sourceFile.delete()){
                if (targetFile.renameTo(sourceFile)){
                    return true;
                }
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return false;
    }
}
