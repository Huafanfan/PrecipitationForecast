package com.lab.rain;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubboConfiguration
@SpringBootApplication
public class RainApplication {

	public static void main(String[] args) {
		SpringApplication.run(RainApplication.class, args);
	}

}
