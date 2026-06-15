package com.conference.mic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.conference.mic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_seat")
public class Seat extends BaseEntity {

    private String seatNo;

    private String rowNo;

    private Integer colNo;

    private BigDecimal xCoord;

    private BigDecimal yCoord;

    private BigDecimal zCoord;

    private String zone;

    private Integer status;
}
