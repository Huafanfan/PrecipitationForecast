package com.lab.rain;

import com.lab.rain.utils.FTPUtil;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

@SpringBootTest
class RainApplicationTests {

	@Autowired
	private FTPUtil ftpUtil;

	@Test
	void contextLoads() {
		//ftpUtil.downloadFiles("2020/020/", "ABMF00GLP_R_20200200000_01D_30S_MO.crx.gz", "/Users/alex/IdeaProjects/PrecipitationForecast/tmpFile");
		String cmd = "gzip -d " + "/Users/alex/IdeaProjects/PrecipitationForecast/tmpFile/*.crx.gz";
		Runtime run = Runtime.getRuntime();
		try {
			System.out.println(cmd);
			Process p = run.exec(cmd);
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1){
					System.out.println("命令执行失败!");
				}
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}


}
