package com.conference.mic.service;

import com.conference.mic.config.NoiseThresholdConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioSamplingService {

    private final NoiseThresholdConfig config;

    private final Random random = ThreadLocalRandom.current();

    private static final double BASE_NOISE = 30.0;

    private static final String[] SEAT_NOISE_PROFILES = {
            "QUIET",
            "NORMAL_TALK",
            "LOUD_TALK",
            "COUGH",
            "BOOK_FLIP"
    };

    public int sampleCurrentDb(String seatNo) {
        String profile = getNoiseProfile(seatNo);
        int db = generateDbByProfile(profile);
        log.trace("座位[{}] 噪声采样: {}dB (模式:{})", seatNo, db, profile);
        return db;
    }

    public boolean checkContinuousSpeech(String seatNo) {
        if (!config.isEnabled()) {
            log.debug("噪声滞后门槛功能未开启, 直接通过检测");
            return true;
        }

        int minDb = config.getMinDb();
        int durationMs = config.getDurationMs();
        int intervalMs = config.getSampleIntervalMs();
        int requiredSamples = (int) Math.ceil((double) durationMs / intervalMs);

        log.info("开始检测座位[{}] 连续语音: 阈值>={}dB, 持续={}ms, 采样间隔={}ms, 需连续{}个样本",
                seatNo, minDb, durationMs, intervalMs, requiredSamples);

        int consecutiveCount = 0;
        int maxConsecutive = 0;
        int totalSamples = 0;
        int sampleCountNeeded = (durationMs / intervalMs) + 2;

        for (int i = 0; i < sampleCountNeeded; i++) {
            int db = sampleCurrentDb(seatNo);
            totalSamples++;

            if (db >= minDb) {
                consecutiveCount++;
                maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
            } else {
                consecutiveCount = 0;
            }

            if (consecutiveCount >= requiredSamples) {
                log.info("座位[{}] 语音检测通过: 连续{}个样本≥{}dB, 最大连续={}, 总采样={}",
                        seatNo, consecutiveCount, minDb, maxConsecutive, totalSamples);
                return true;
            }

            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("座位[{}] 语音检测被中断", seatNo);
                return false;
            }
        }

        log.warn("座位[{}] 语音检测未通过: 最大连续={}个样本≥{}dB, 需要={}, 总采样={}",
                seatNo, maxConsecutive, minDb, requiredSamples, totalSamples);
        return false;
    }

    public String getFilterReason(String seatNo) {
        StringBuilder sb = new StringBuilder();
        sb.append("噪声检测未通过: ");
        int samples = 5;
        int overThreshold = 0;
        for (int i = 0; i < samples; i++) {
            int db = sampleCurrentDb(seatNo);
            if (db >= config.getMinDb()) overThreshold++;
            sb.append(db).append("dB");
            if (i < samples - 1) sb.append("/");
        }
        sb.append(String.format(" (阈值≥%ddB, 需连续%ds)",
                config.getMinDb(), config.getDurationMs() / 1000));
        return sb.toString();
    }

    private String getNoiseProfile(String seatNo) {
        int hash = Math.abs(seatNo.hashCode());
        int idx = hash % SEAT_NOISE_PROFILES.length;

        double roll = random.nextDouble();
        if (roll < 0.55) {
            return "NORMAL_TALK";
        } else if (roll < 0.75) {
            return "LOUD_TALK";
        } else if (roll < 0.85) {
            return "COUGH";
        } else if (roll < 0.95) {
            return "BOOK_FLIP";
        } else {
            return "QUIET";
        }
    }

    private int generateDbByProfile(String profile) {
        return switch (profile) {
            case "QUIET" -> (int) (BASE_NOISE + random.nextDouble() * 10);
            case "NORMAL_TALK" -> (int) (50 + random.nextDouble() * 15);
            case "LOUD_TALK" -> (int) (65 + random.nextDouble() * 20);
            case "COUGH" -> {
                double val = random.nextDouble();
                if (val < 0.3) yield (int) (55 + random.nextDouble() * 10);
                else yield (int) (BASE_NOISE + random.nextDouble() * 8);
            }
            case "BOOK_FLIP" -> {
                double val = random.nextDouble();
                if (val < 0.2) yield (int) (50 + random.nextDouble() * 8);
                else yield (int) (BASE_NOISE + random.nextDouble() * 8);
            }
            default -> (int) (BASE_NOISE + random.nextDouble() * 20);
        };
    }
}
