package com.lab.rain.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dubbo.config.annotation.Service;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.demo.rain.service.ProcessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Calendar;

/**
 * @author Alex
 * @version 1.0
 * @date 2021/11/2 19:06
 */
@Service
@Component
@Slf4j
public class ProcessServiceImpl implements ProcessService {

    @Value("${spring.servlet.multipart.location}")
    private String basePath;

    @Value("${python.location}")
    private String pythonBasePath;

    @Override
    public String getCurrentGpsRain(String year, String doy, String tempMin, String tempMax, String pressure, String humidity, String windSpeed, String pwvGps) {
        String rainGps = "0";
        if (!"0".equals(pwvGps)){
            log.info("GPS============");
            rainGps = getRainGNSS(year, doy, tempMin, tempMax, pressure, humidity, windSpeed, pwvGps, true);
        }
        return rainGps;
    }

    @Override
    public String getCurrentBdRain(String year, String doy, String tempMin, String tempMax, String pressure, String humidity, String windSpeed, String pwvBd) {
        String rainBd = "0";
        if (!"0".equals(pwvBd)){
            log.info("BD============");
            rainBd = getRainGNSS(year, doy, tempMin, tempMax, pressure, humidity, windSpeed, pwvBd, false);
        }
        return rainBd;
    }

    @Override
    public String getGpsPwv(String year, String doy, String hour, String city) {
        String gpsBasePath = basePath + "/GPS";
        //initialization
        String cmd="rm -r archive brdc control figs gfiles glbf gsoln igs ionex met mkrinex raw rinex tables " + String.format("%03d",Integer.parseInt(doy)) + "G";
        executeCmd(cmd, null, new File(gpsBasePath));

        //buildRinexProject
        cmd="sh_setup -yr " + year + " -doy " + doy;
        executeCmd(cmd, null, new File(gpsBasePath));

        //updateTable
        updateTable(gpsBasePath);

        //mkdir mv files
        mkdirAndMv(gpsBasePath);

        //link
        linkVmf1(gpsBasePath, year);

        //gamitProcess
        gamitProcessGps(gpsBasePath, year, doy);

        //getPwv
        String pwv = null;
        try {
            pwv = getGPwv(gpsBasePath, year, doy, hour, city);
        }
        catch (Exception e){
            log.error(e.getMessage());
        }
        return pwv;
    }

    @Override
    public String getBdPwv(String year, String doy, String hour, String city){
        String bdBasePath = basePath + "/BD";
        //initialization
        String cmd="rm -r archive brdc control figs gfiles glbf gsoln igs ionex met mkrinex raw rinex tables " + String.format("%03d",Integer.parseInt(doy)) + "C";
        executeCmd(cmd, null, new File(bdBasePath));

        //buildRinexProject
        cmd="sh_setup -yr " + year + " -doy " + doy;
        executeCmd(cmd, null, new File(bdBasePath));

        //updateTable
        updateTable(bdBasePath);

        //mkdir mv files
        mkdirAndMv(bdBasePath);

        //link
        linkVmf1(bdBasePath, year);

        //gamitProcess
        gamitProcessBd(bdBasePath, year, doy);

        //getPwv
        String pwv = null;
        try {
            pwv = getCPwv(bdBasePath, year, doy, hour, city);
        }
        catch (Exception e){
            log.error(e.getMessage());
        }
        return pwv;
    }

    public String getRainGNSS(String year, String doy, String tempMin, String tempMax, String pressure, String humidity, String windSpeed, String pwv, boolean isGps) {
        String yearModel = null;
        String yearMonthModel = null;
        String rainYearModel = null;
        String rainYearMonthModel = null;

        yearModel = getYearModel(year, isGps);
        yearMonthModel = getYearMonthModel(year, doy, isGps);
        log.info("yearModel :" + yearModel);
        if (yearModel != null){
            String cmd = "python3 " + pythonBasePath + yearModel + "test.py " + tempMin + " " + tempMax + " " + pressure + " " + humidity + " " + windSpeed + " " + pwv;
            if (!executeCmd(cmd, null, new File(pythonBasePath + yearModel))){
                return null;
            }
            rainYearModel = readFileContent(pythonBasePath + yearModel + "result.txt");
        }
        log.info("yearMonthModel :" + yearMonthModel);
        if (yearMonthModel != null){
            String cmd = "python3 " + pythonBasePath + yearMonthModel + "test.py " + tempMin + " " + tempMax + " " + pressure + " " + humidity + " " + windSpeed + " " + pwv;
            if (!executeCmd(cmd, null, new File(pythonBasePath + yearMonthModel))){
                return null;
            }
            rainYearMonthModel = readFileContent(pythonBasePath + yearMonthModel + "result.txt");
        }
        log.info("rainYearModel :" + rainYearModel);
        log.info("rainYearMonthModel :" + rainYearMonthModel);
        if (rainYearMonthModel != null){
            //if (rainYearModel != null){
            //    double rainYearModelNum = Double.parseDouble(rainYearModel);
            //    double rainYearMonthModelNum = Double.parseDouble(rainYearMonthModel);
            //    if (rainYearMonthModelNum < 0){
            //        return rainYearModel;
            //    }
            //    else if(rainYearModelNum < 0){
            //        return rainYearMonthModel;
            //    }
            //    else {
            //        return String.valueOf(rainYearMonthModelNum * 0.8 + rainYearModelNum * 0.2);
            //    }
            //}
            return rainYearMonthModel;
        }
        else {
            return rainYearModel;
        }
    }

    public String getYearModel(String year, boolean isGps){
        if (Integer.parseInt(year) < 2017 || Integer.parseInt(year) > 2020){
            return null;
        }
        if (isGps){
            //return "GPS-all";
            return null;
        }
        else {
            return "BD2018-2019/";
        }
    }

    public String getYearMonthModel(String year, String doy, boolean isGps){
        //if (Integer.parseInt(year) < 2017 || Integer.parseInt(year) > 2020){
        //    return null;
        //}
        //if (Integer.parseInt(doy) < 152 || Integer.parseInt(doy) > 243){
        //    return null;
        //}
        if (isGps){
            if (Integer.parseInt(doy) >=61 && Integer.parseInt(doy) <= 152){
                return "GPS3-5/";
            }
            else if (Integer.parseInt(doy) >=153 && Integer.parseInt(doy) <= 243){
                return "GPS6-8/";
            }
            else if (Integer.parseInt(doy) >=244 && Integer.parseInt(doy) <= 335){
                return "GPS9-11/";
            }
            else {
                return "GPS12-2/";
            }
        }
        else {
            return null;
            //return "BD2018-2019-6-8/";
        }

    }

    public boolean updateTable(String GNSSBasePath) {
        log.info("updateTable");
        try {
            File sourceFile = new File(GNSSBasePath + "/tables/"+ "sestbl.");
            BufferedReader in = new BufferedReader(new FileReader(sourceFile));

            File targetFile = new File(GNSSBasePath + "/tables/"+ "sestbl.update");
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

    public void mkdirAndMv(String GNSSBasePath){
        String cmd="mkdir rinex igs brdc ionex";
        executeCmd(cmd, null, new File(GNSSBasePath));
        String[] shCommand = {"/bin/sh","-c",""};

        cmd = "cp " + basePath +"/*o " + GNSSBasePath+ "/rinex/";
        shCommand[2] = cmd;
        executeCmd(shCommand, null, new File(GNSSBasePath));

        cmd = "cp " + basePath +"/*sp3 " + GNSSBasePath+ "/igs/";
        shCommand[2] = cmd;
        executeCmd(shCommand, null, new File(GNSSBasePath));

        cmd = "cp " + basePath +"/*n " + GNSSBasePath+ "/brdc/";
        shCommand[2] = cmd;
        executeCmd(shCommand, null, new File(GNSSBasePath));

        cmd = "cp " + basePath +"/*i " + GNSSBasePath+ "/ionex/";
        shCommand[2] = cmd;
        executeCmd(shCommand, null, new File(GNSSBasePath));
    }

    public void linkVmf1(String GNSSBasePath, String year){
        String cmd="rm -f tables/map.grid";
        executeCmd(cmd, null, new File(GNSSBasePath));

        cmd="ln -s /opt/gamit10.71/tables/vmf1grd." + year + "_365 tables/map.grid";
        executeCmd(cmd, null, new File(GNSSBasePath));
    }

    public void gamitProcessGps(String GNSSBasePath, String year, String doy){
        String cmd="sh_gamit -expt demo -d " + year+ " " + doy + " -orbit codm -met -metutil Z -gnss G";
        executeCmd(cmd, null, new File(GNSSBasePath));
    }

    public void gamitProcessBd(String GNSSBasePath, String year, String doy){
        String cmd="sh_gamit -expt demo -d " + year+ " " + doy + " -orbit codm -met -metutil Z -gnss C";
        executeCmd(cmd, null, new File(GNSSBasePath));
    }

    public String getCPwv(String GNSSBasePath, String year, String doy, String hour, String city) throws IOException {
        String fileName = GNSSBasePath + "/" + String.format("%03d",Integer.parseInt(doy)) + "C/met_" + city + "." + year.substring(2) + String.format("%03d",Integer.parseInt(doy));
        File file = new File(fileName);
        if (!file.exists()){
            if ("wuh2".equals(city)){
                fileName = GNSSBasePath + "/" + String.format("%03d",Integer.parseInt(doy)) + "C/met_" + "jfng." + year.substring(2) + String.format("%03d",Integer.parseInt(doy));
            }
            else {
                fileName = GNSSBasePath + "/" + String.format("%03d",Integer.parseInt(doy)) + "C/met_" + "wuh2." + year.substring(2) + String.format("%03d",Integer.parseInt(doy));
            }
        }
        file = new File(fileName);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine();
        reader.readLine();
        reader.readLine();
        reader.readLine();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] item = line.replaceAll("\\s+", " ").split(" ");
            if (item[3].equals(hour)){
                return item[9];
            }
        }
        return null;
    }

    public String getGPwv(String GNSSBasePath, String year, String doy, String hour, String city) throws IOException {
        String fileName = GNSSBasePath + "/" + String.format("%03d",Integer.parseInt(doy)) + "G/met_" + city + "." + year.substring(2) + String.format("%03d",Integer.parseInt(doy));
        File file = new File(fileName);
        if (!file.exists()){
            if ("wuh2".equals(city)){
                fileName = GNSSBasePath + "/" + String.format("%03d",Integer.parseInt(doy)) + "G/met_" + "jfng." + year.substring(2) + String.format("%03d",Integer.parseInt(doy));
            }
            else {
                fileName = GNSSBasePath + "/" + String.format("%03d",Integer.parseInt(doy)) + "G/met_" + "wuh2." + year.substring(2) + String.format("%03d",Integer.parseInt(doy));
            }
        }
        file = new File(fileName);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine();
        reader.readLine();
        reader.readLine();
        reader.readLine();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] item = line.replaceAll("\\s+", " ").split(" ");
            if (item[3].equals(hour)){
                return item[9];
            }
        }
        return null;
    }

    public boolean executeCmd(String[] cmd, String[] envp, File dir) {
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

    public boolean executeCmd(String cmd, String[] envp, File dir) {
        log.info("execute {} {} {}", cmd, envp, dir);
        Runtime run = Runtime.getRuntime();
        try {
            Process p = run.exec(cmd, envp, dir);
            final InputStream stream = p.getInputStream();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }).start();
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

    public String readFileContent(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }
}
