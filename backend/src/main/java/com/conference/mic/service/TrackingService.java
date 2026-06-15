package com.conference.mic.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.conference.mic.dto.SpeakRequestDTO;
import com.conference.mic.entity.Microphone;
import com.conference.mic.entity.Seat;
import com.conference.mic.entity.TrackingTask;
import com.conference.mic.mapper.MicrophoneMapper;
import com.conference.mic.mapper.SeatMapper;
import com.conference.mic.mapper.TrackingTaskMapper;
import com.conference.mic.vo.TrackingResultVO;
import com.conference.mic.websocket.TrackingWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final SeatMapper seatMapper;
    private final MicrophoneMapper microphoneMapper;
    private final TrackingTaskMapper trackingTaskMapper;
    private final TrackingAlgorithmService algorithmService;
    private final MotorControlService motorControlService;

    @Transactional(rollbackFor = Exception.class)
    public TrackingResultVO handleSpeakRequest(SpeakRequestDTO request) {
        log.info("收到发言请求, 座位号: {}", request.getSeatNo());

        Seat seat = seatMapper.selectOne(
                new QueryWrapper<Seat>().eq("seat_no", request.getSeatNo())
        );
        if (seat == null) {
            throw new RuntimeException("座位不存在: " + request.getSeatNo());
        }
        if (seat.getStatus() != 1) {
            throw new RuntimeException("座位不可用: " + request.getSeatNo());
        }

        List<Microphone> mics = microphoneMapper.selectList(
                new QueryWrapper<Microphone>().eq("status", 1)
        );
        if (mics.isEmpty()) {
            throw new RuntimeException("没有可用的吊麦设备");
        }

        TrackingResultVO result = algorithmService.calculateOptimalTracking(seat, mics);

        String taskNo = "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        result.setTaskNo(taskNo);

        TrackingTask task = new TrackingTask();
        task.setTaskNo(taskNo);
        task.setSeatNo(seat.getSeatNo());
        task.setMicId(result.getMicId());
        task.setTargetX(seat.getXCoord());
        task.setTargetY(seat.getYCoord());
        task.setTargetZ(seat.getZCoord() != null ? seat.getZCoord() : BigDecimal.ZERO);
        task.setDropDistance(result.getDropDistance());
        task.setRotateAngle(result.getRotateAngle());
        task.setTaskStatus(0);
        trackingTaskMapper.insert(task);

        TrackingWebSocket.broadcast(result);

        Microphone mic = microphoneMapper.selectById(result.getMicId());
        executeTrackingAsync(task, mic, result);

        log.info("追踪任务已创建: taskNo={}, mic={}, seat={}", taskNo, result.getMicCode(), seat.getSeatNo());
        return result;
    }

    @Async
    public void executeTrackingAsync(TrackingTask task, Microphone mic, TrackingResultVO result) {
        try {
            task.setTaskStatus(1);
            task.setStartTime(LocalDateTime.now());
            trackingTaskMapper.updateById(task);

            Thread.sleep(result.getAnimationDuration());

            boolean success = motorControlService.moveMicrophone(
                    mic.getDeviceAddr(),
                    result.getDropDistance(),
                    result.getRotateAngle()
            );

            if (success) {
                mic.setZCoord(mic.getBaseHeight().subtract(result.getDropDistance()));
                BigDecimal newAngle = mic.getRotateAngle().add(result.getRotateAngle());
                mic.setRotateAngle(normalizeAngle(newAngle));
                microphoneMapper.updateById(mic);

                task.setTaskStatus(2);
                task.setFinishTime(LocalDateTime.now());
                log.info("吊麦运动执行成功: mic={}", mic.getMicCode());
            } else {
                task.setTaskStatus(3);
                task.setErrorMsg("电机控制指令执行失败");
                log.error("吊麦运动执行失败: mic={}", mic.getMicCode());
            }
            trackingTaskMapper.updateById(task);
        } catch (Exception e) {
            log.error("吊麦运动执行异常: {}", e.getMessage(), e);
            task.setTaskStatus(3);
            task.setErrorMsg(e.getMessage());
            trackingTaskMapper.updateById(task);
        }
    }

    private BigDecimal normalizeAngle(BigDecimal angle) {
        double a = angle.doubleValue();
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return BigDecimal.valueOf(a);
    }

    public List<Seat> getAllSeats() {
        return seatMapper.selectList(new QueryWrapper<Seat>().orderByAsc("row_no", "col_no"));
    }

    public List<Microphone> getAllMicrophones() {
        return microphoneMapper.selectList(new QueryWrapper<>());
    }

    public List<TrackingTask> getRecentTasks(int limit) {
        return trackingTaskMapper.selectList(
                new QueryWrapper<TrackingTask>().orderByDesc("create_time").last("LIMIT " + limit)
        );
    }
}
