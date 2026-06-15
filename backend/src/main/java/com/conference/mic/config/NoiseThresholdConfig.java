package com.conference.mic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "noise-threshold")
public class NoiseThresholdConfig {

    private boolean enabled = true;

    private Integer minDb = 45;

    private Integer durationMs = 2000;

    private Integer sampleIntervalMs = 200;
}
