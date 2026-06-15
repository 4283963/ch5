package com.conference.mic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.conference.mic.mapper")
@EnableAsync
public class MicTrackingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicTrackingApplication.class, args);
    }
}
