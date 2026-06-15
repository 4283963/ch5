package com.conference.mic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.conference.mic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_microphone")
public class Microphone extends BaseEntity {

    private String micCode;

    private Integer deviceAddr;

    private BigDecimal xCoord;

    private BigDecimal yCoord;

    private BigDecimal zCoord;

    private BigDecimal baseHeight;

    private BigDecimal rotateAngle;

    private Integer status;

    private LocalDateTime lastHeartbeat;
}
