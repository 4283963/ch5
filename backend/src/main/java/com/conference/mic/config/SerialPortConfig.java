package com.conference.mic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "serial")
public class SerialPortConfig {

    private String port = "/dev/tty.usbserial-0";

    private Integer baudRate = 9600;

    private Integer dataBits = 8;

    private Integer stopBits = 1;

    private String parity = "NONE";

    private Integer timeout = 2000;
}
