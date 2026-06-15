package com.conference.mic.controller;

import com.conference.mic.common.Result;
import com.conference.mic.config.NoiseThresholdConfig;
import com.conference.mic.dto.SpeakRequestDTO;
import com.conference.mic.entity.Microphone;
import com.conference.mic.entity.Seat;
import com.conference.mic.entity.TrackingTask;
import com.conference.mic.service.TrackingService;
import com.conference.mic.vo.TrackingResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final NoiseThresholdConfig noiseThresholdConfig;

    @PostMapping("/speak")
    public Result<TrackingResultVO> speak(@Valid @RequestBody SpeakRequestDTO request) {
        log.info("收到发言按钮触发: seatNo={}", request.getSeatNo());
        TrackingResultVO result = trackingService.handleSpeakRequest(request);
        return Result.success(result);
    }

    @GetMapping("/seats")
    public Result<List<Seat>> listSeats() {
        return Result.success(trackingService.getAllSeats());
    }

    @GetMapping("/microphones")
    public Result<List<Microphone>> listMicrophones() {
        return Result.success(trackingService.getAllMicrophones());
    }

    @GetMapping("/tasks")
    public Result<List<TrackingTask>> listTasks(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(trackingService.getRecentTasks(limit));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("seats", trackingService.getAllSeats());
        data.put("microphones", trackingService.getAllMicrophones());
        data.put("tasks", trackingService.getRecentTasks(10));
        data.put("noiseThreshold", getNoiseThresholdConfig());
        return Result.success(data);
    }

    @GetMapping("/noise-config")
    public Result<Map<String, Object>> getNoiseThresholdConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", noiseThresholdConfig.isEnabled());
        config.put("minDb", noiseThresholdConfig.getMinDb());
        config.put("durationMs", noiseThresholdConfig.getDurationMs());
        config.put("sampleIntervalMs", noiseThresholdConfig.getSampleIntervalMs());
        return Result.success(config);
    }

    @PostMapping("/noise-config")
    public Result<Map<String, Object>> updateNoiseThresholdConfig(@RequestBody Map<String, Object> params) {
        if (params.containsKey("enabled")) {
            noiseThresholdConfig.setEnabled((Boolean) params.get("enabled"));
        }
        if (params.containsKey("minDb")) {
            noiseThresholdConfig.setMinDb(((Number) params.get("minDb")).intValue());
        }
        if (params.containsKey("durationMs")) {
            noiseThresholdConfig.setDurationMs(((Number) params.get("durationMs")).intValue());
        }
        if (params.containsKey("sampleIntervalMs")) {
            noiseThresholdConfig.setSampleIntervalMs(((Number) params.get("sampleIntervalMs")).intValue());
        }
        log.info("噪声滞后门槛配置已更新: enabled={}, minDb={}, durationMs={}",
                noiseThresholdConfig.isEnabled(),
                noiseThresholdConfig.getMinDb(),
                noiseThresholdConfig.getDurationMs());
        return getNoiseThresholdConfig();
    }
}
