package com.conference.mic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.conference.mic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tracking_task")
public class TrackingTask extends BaseEntity {

    private String taskNo;

    private String seatNo;

    private Long micId;

    private BigDecimal targetX;

    private BigDecimal targetY;

    private BigDecimal targetZ;

    private BigDecimal dropDistance;

    private BigDecimal rotateAngle;

    private Integer taskStatus;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private String errorMsg;
}
