package com.lab.rain.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.demo.rain.service.HelloService;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Hello服务实现
 * </p>
 * @author alex
 */
@Service
@Component
@Slf4j
public class HelloServiceImpl implements HelloService {
    /**
     * 问好
     *
     * @param name 姓名
     * @return 问好
     */
    @Override
    public String sayHello(String name) {
        return "say hello to: " + name;
    }
}
