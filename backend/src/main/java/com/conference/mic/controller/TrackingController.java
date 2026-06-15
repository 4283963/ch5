package com.conference.mic.controller;

import com.conference.mic.common.Result;
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
        return Result.success(data);
    }
}
