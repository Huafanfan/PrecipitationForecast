package com.lab.rain.utils;

/**
 * @author Alex
 * @version 1.0
 * @date 2020/12/5 14:58
 */

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;

@Slf4j
@Component
public class FTPUtil {
    /**
     * 日志对象
     **/
    private static final Logger logger = LoggerFactory.getLogger(FTPUtil.class);

    /**
     * 该目录不存在
     */
    public static final String DIR_NOT_EXIST = "该目录不存在";

    /**
     * "该目录下没有文件
     */
    public static final String DIR_CONTAINS_NO_FILE = "该目录下没有文件";

    /**
     * FTP地址
     **/
    @Value("${ftp.host}")
    private String ftpAddress;
    /**
     * FTP端口
     **/
    @Value("${ftp.port}")
    private int ftpPort;
    /**
     * FTP用户名
     **/
    @Value("${ftp.username}")
    private String ftpUsername;
    /**
     * FTP密码
     **/
    private final String ftpPassword = null;
    /**
     * FTP基础目录
     **/
    @Value("${ftp.basepath}")
    private String basePath;

    /**
     * 本地字符编码
     **/
    private static String localCharset = "GBK";

    /**
     * FTP协议里面，规定文件名编码为iso-8859-1
     **/
    private static String serverCharset = "ISO-8859-1";

    /**
     * UTF-8字符编码
     **/
    private static final String CHARSET_UTF8 = "UTF-8";

    /**
     * OPTS UTF8字符串常量
     **/
    private static final String OPTS_UTF8 = "OPTS UTF8";

    /**
     * 设置缓冲区大小4M
     **/
    private static final int BUFFER_SIZE = 1024 * 1024 * 4;

    /**
     * FTPClient对象
     **/
    private static FTPClient ftpClient = null;


    /**
     * 下载该目录下所有文件到本地
     *
     * @param ftpPath  FTP服务器上的相对路径，例如：test/123
     * @param savePath 保存文件到本地的路径，例如：D:/test
     * @return 成功返回true，否则返回false
     */
    public boolean downloadFiles(String ftpPath, String fileName, String savePath) {
        // 登录
        login(ftpAddress, ftpPort, ftpUsername, ftpPassword);
        if (ftpClient != null) {
            try {
                String path = changeEncoding(basePath + ftpPath);
                // 判断是否存在该目录
                if (!ftpClient.changeWorkingDirectory(path)) {
                    logger.error(basePath + ftpPath + DIR_NOT_EXIST);
                    return Boolean.FALSE;
                }
                ftpClient.enterLocalPassiveMode();  // 设置被动模式，开通一个端口来传输数据
                String[] fs = ftpClient.listNames();
                // 判断该目录下是否有文件
                if (fs == null || fs.length == 0) {
                    logger.error(basePath + ftpPath + DIR_CONTAINS_NO_FILE);
                    return Boolean.FALSE;
                }
                for (String ff : fs) {
                    String ftpName = new String(ff.getBytes(serverCharset), localCharset);
                    if (ftpName.equals(fileName)){
                        File file = new File(savePath + '/' + ftpName);
                        try (OutputStream os = new FileOutputStream(file)) {
                            ftpClient.retrieveFile(ff, os);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("下载文件失败:{}", fileName);
                //logger.error("下载文件失败", e);
            } finally {
                closeConnect();
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 连接FTP服务器
     *
     * @param address  地址，如：127.0.0.1
     * @param port     端口，如：21
     * @param username 用户名，如：root
     * @param password 密码，如：root
     */
    private void login(String address, int port, String username, String password) {
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(address, port);
            ftpClient.login(username, password);
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            //限制缓冲区大小
            ftpClient.setBufferSize(BUFFER_SIZE);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                closeConnect();
                logger.error("FTP服务器连接失败");
            }
        } catch (Exception e) {
            logger.error("FTP登录失败", e);
        }
    }


    /**
     * FTP服务器路径编码转换
     *
     * @param ftpPath FTP服务器路径
     * @return String
     */
    private static String changeEncoding(String ftpPath) {
        String directory = null;
        try {
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand(OPTS_UTF8, "ON"))) {
                localCharset = CHARSET_UTF8;
            }
            directory = new String(ftpPath.getBytes(localCharset), serverCharset);
        } catch (Exception e) {
            logger.error("路径编码转换失败", e);
        }
        return directory;
    }

    /**
     * 关闭FTP连接
     */
    private void closeConnect() {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                logger.error("关闭FTP连接失败", e);
            }
        }
    }
}
