package com.conference.mic.service;

import com.conference.mic.entity.Microphone;
import com.conference.mic.entity.Seat;
import com.conference.mic.vo.TrackingResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class TrackingAlgorithmService {

    @Value("${mic.default-height:300}")
    private BigDecimal defaultHeight;

    @Value("${mic.animation-duration:2000}")
    private Integer animationDuration;

    private static final BigDecimal TARGET_MIC_HEIGHT = new BigDecimal("50");

    public TrackingResultVO calculateOptimalTracking(Seat seat, List<Microphone> mics) {
        log.info("开始计算座位[{}]的最佳吊麦追踪方案, 坐标: ({}, {}, {})",
                seat.getSeatNo(), seat.getXCoord(), seat.getYCoord(), seat.getZCoord());

        Microphone nearestMic = findNearestMicrophone(seat, mics);
        log.info("选中最近吊麦: {}, 坐标: ({}, {})", nearestMic.getMicCode(),
                nearestMic.getXCoord(), nearestMic.getYCoord());

        BigDecimal dropDistance = calculateDropDistance(nearestMic, seat);
        BigDecimal rotateAngle = calculateRotateAngle(nearestMic, seat);

        TrackingResultVO vo = new TrackingResultVO();
        vo.setSeatNo(seat.getSeatNo());
        vo.setMicId(nearestMic.getId());
        vo.setMicCode(nearestMic.getMicCode());
        vo.setMicX(nearestMic.getXCoord());
        vo.setMicY(nearestMic.getYCoord());
        vo.setSeatX(seat.getXCoord());
        vo.setSeatY(seat.getYCoord());
        vo.setDropDistance(dropDistance);
        vo.setRotateAngle(rotateAngle);
        vo.setAnimationDuration(animationDuration);
        vo.setTimestamp(System.currentTimeMillis());

        log.info("追踪方案计算完成: 吊麦[{}] 下降{}cm, 旋转{}度",
                nearestMic.getMicCode(), dropDistance, rotateAngle);

        return vo;
    }

    private Microphone findNearestMicrophone(Seat seat, List<Microphone> mics) {
        return mics.stream()
                .filter(m -> m.getStatus() == 1)
                .min(Comparator.comparingDouble(m -> calculateHorizontalDistance(m, seat)))
                .orElseThrow(() -> new RuntimeException("没有可用的在线吊麦"));
    }

    private double calculateHorizontalDistance(Microphone mic, Seat seat) {
        double dx = mic.getXCoord().subtract(seat.getXCoord()).doubleValue();
        double dy = mic.getYCoord().subtract(seat.getYCoord()).doubleValue();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private BigDecimal calculateDropDistance(Microphone mic, Seat seat) {
        BigDecimal currentHeight = mic.getZCoord() != null ? mic.getZCoord() : defaultHeight;
        BigDecimal targetHeight = TARGET_MIC_HEIGHT;

        BigDecimal drop = currentHeight.subtract(targetHeight);

        if (drop.compareTo(BigDecimal.ZERO) < 0) {
            drop = BigDecimal.ZERO;
        }

        return drop.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRotateAngle(Microphone mic, Seat seat) {
        double dx = seat.getXCoord().subtract(mic.getXCoord()).doubleValue();
        double dy = seat.getYCoord().subtract(mic.getYCoord()).doubleValue();

        double angleRad = Math.atan2(dx, dy);
        double angleDeg = Math.toDegrees(angleRad);

        double currentAngle = mic.getRotateAngle() != null ? mic.getRotateAngle().doubleValue() : 0.0;
        double deltaAngle = angleDeg - currentAngle;

        while (deltaAngle > 180) deltaAngle -= 360;
        while (deltaAngle < -180) deltaAngle += 360;

        return BigDecimal.valueOf(deltaAngle).setScale(2, RoundingMode.HALF_UP);
    }
}
