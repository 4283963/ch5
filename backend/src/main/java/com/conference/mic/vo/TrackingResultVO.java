package com.conference.mic.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrackingResultVO {

    private String taskNo;

    private String seatNo;

    private Long micId;

    private String micCode;

    private BigDecimal micX;

    private BigDecimal micY;

    private BigDecimal seatX;

    private BigDecimal seatY;

    private BigDecimal dropDistance;

    private BigDecimal rotateAngle;

    private Integer animationDuration;

    private Long timestamp;
}
