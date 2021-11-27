package org.jeecg.modules.demo.rain.service;

import com.alibaba.dubbo.config.annotation.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Alex
 * @version 1.0
 * @date 2021/11/2 19:04
 */
public interface ProcessService {
    String getCurrentGpsRain(String year, String doy, String tempMin, String tempMax, String pressure, String humidity, String windSpeed, String pwvGps);

    String getCurrentBdRain(String year, String doy, String tempMin, String tempMax, String pressure, String humidity, String windSpeed, String pwvBd);

    String getGpsPwv(String year, String doy, String hour, String city);

    String getBdPwv(String year, String doy, String hour, String city);
}
