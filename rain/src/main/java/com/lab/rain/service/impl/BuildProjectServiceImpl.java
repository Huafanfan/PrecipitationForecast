package com.lab.rain.service.impl;

import cn.hutool.core.util.StrUtil;
import com.lab.rain.service.BuildProjectService;
import com.lab.rain.utils.FTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Calendar;
import java.util.List;

/**
 * @author Alex
 * @version 1.0
 * @date 2020/12/3 17:21
 */
@Slf4j
@Service
@ConfigurationProperties(prefix = "city")
public class BuildProjectServiceImpl implements BuildProjectService {

    @Value("${table.location}")
    private String tablePath;

    @Value("${spring.servlet.multipart.location}")
    private String rinexPath;

    @Value("${vmf1.data}")
    private String vmf1;

    @Autowired
    private FTPUtil ftpUtil;

    private List<String> cityList;

    @Override
    public boolean buildRinexProject(String rinexFile) {
        log.info("buildRinexProject rinexFile:{}", rinexFile);
        String year;
        String doy;
        if (rinexFile.endsWith("o")) {
            year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            doy = rinexFile.substring(4, 7);
        }
        else {
            String[] rinexFileNames = rinexFile.split("_");
            String time = rinexFileNames[2];
            year = time.substring(0, 4);
            doy = time.substring(4, 7);
        }
        String cmd="sh_setup -yr " + year + " -doy " + doy;
        return executeCmd(cmd);
    }

    @Override
    public boolean downloadRinex(String rinexFile) {
        String year;
        String doy;
        if (rinexFile.endsWith("o")) {
            year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            doy = rinexFile.substring(4, 7);
        }
        else {
            String[] rinexFileNames = rinexFile.split("_");
            String time = rinexFileNames[2];
            year = time.substring(0, 4);
            doy = time.substring(4, 7);
        }
        String sourcePosition = rinexFile.substring(0,4).toLowerCase();
        String ftpPath;
        String fileName;
        String savePath;
        for (String position : cityList) {
            if (!position.equals(sourcePosition)) {
                if ("ncku".equals(position) || "twtf".equals(position)){
                    ftpPath = year + "/" + doy + "/";
                    fileName = position.toUpperCase()+"00TWN_R_"+year+doy+"0000_01D_30S_MO.crx.gz";
                    savePath = rinexPath;
                    ftpUtil.downloadFiles(ftpPath, fileName, savePath);
                }
                else {
                    ftpPath = year + "/" + doy + "/";
                    fileName = position.toUpperCase()+"00CHN_R_"+year+doy+"0000_01D_30S_MO.crx.gz";
                    savePath = rinexPath;
                    ftpUtil.downloadFiles(ftpPath, fileName, savePath);
                }
            }
        }
        String cmd;
        File rinexDic = new File(rinexPath);
        String[] files = rinexDic.list();
        for (String file : files) {
            File tempFile = new File(rinexPath + "/" + file);
            if (tempFile.getName().endsWith(".gz")) {
                cmd = "gzip -d " + tempFile.getAbsolutePath();
                if (!executeCmd(cmd)) {
                    log.error("gzip fail");
                    return false;
                }
                cmd = rinexPath + "/crx2rnx " + tempFile.getAbsolutePath().replace(".gz","");
                if (!executeCmd(cmd)) {
                    log.error("crx2rnx fail");
                    return false;
                }
                cmd = "rm -f " + tempFile.getAbsolutePath().replace(".gz","");
                if (!executeCmd(cmd)) {
                    log.error("rm fail");
                    return false;
                }
            }
        }

        cmd = "sh_rename_rinex3 -f *.rnx -r";
        if (!executeCmd(cmd,null,new File("rinex/"))) {
            log.error("sh_rename_rinex3 fail");
            return false;
        }

        cmd = "rm -f tables/map.grid";
        if (!executeCmd(cmd)) {
            log.error("rm -f tables/map.grid fail");
            return false;
        }

        cmd = "ln -s ~/gg/tables/vmf1grd." + vmf1 + " tables/map.grid";
        if (!executeCmd(cmd)) {
            log.error("ln -s fail");
            return false;
        }

        cmd = "sh_gamit -expt demo -d " + year + " " + doy + " -orbit codm -met -metutil Z -gnss G";
        if (!executeCmd(cmd)) {
            log.error("sh_gamit G fail");
            return false;
        }

        cmd = "sh_gamit -expt demo -d " + year + " " + doy + " -orbit codm -met -metutil Z -gnss C";
        if (!executeCmd(cmd)) {
            log.error("sh_gamit C fail");
            return false;
        }

        return true;
    }

    @Override
    public boolean updateTable() {
        log.info("updateTable");
        try {
            File sourceFile = new File(StrUtil.appendIfMissing(tablePath, "/") + "sestbl.");
            BufferedReader in = new BufferedReader(new FileReader(sourceFile));

            File targetFile = new File(StrUtil.appendIfMissing(tablePath, "/") + "sestbl.update");
            BufferedWriter out = new BufferedWriter(new FileWriter(targetFile));

            String line;
            String newLine;
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

    @Override
    public boolean downloadRinexBatch(String year, String doyStart, String doyEnd) {
        String ftpPath;
        String fileName;
        String savePath;
        for (int i=Integer.parseInt(doyStart); i<=Integer.parseInt(doyEnd); i++){
            String doy = String.valueOf(i);
            for (String position : cityList) {
                if ("ncku".equals(position) || "twtf".equals(position)){
                    ftpPath = year + "/" + doy + "/";
                    fileName = position.toUpperCase()+"00TWN_R_"+year+doy+"0000_01D_30S_MO.crx.gz";
                    savePath = rinexPath;
                    ftpUtil.downloadFiles(ftpPath, fileName, savePath);
                }
                else {
                    ftpPath = year + "/" + doy + "/";
                    fileName = position.toUpperCase()+"00CHN_R_"+year+doy+"0000_01D_30S_MO.crx.gz";
                    savePath = rinexPath;
                    ftpUtil.downloadFiles(ftpPath, fileName, savePath);
                }
                log.info("{} success", fileName);
            }
        }
        return true;
    }

    public void setCityList(List<String> cityList) {
        this.cityList = cityList;
    }

    public boolean executeCmd(String cmd) {
        return executeCmd(cmd, null, null);
    }
    public boolean executeCmd(String cmd, String[] envp, File dir) {
        log.info("execute {} {} {}", cmd, envp, dir);
        Runtime run = Runtime.getRuntime();
        try {
            Process p = run.exec(cmd, envp, dir);
            if (p.waitFor() != 0) {
                if (p.exitValue() == 0){
                    log.error("命令执行失败!");
                    return false;
                }
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }
}
