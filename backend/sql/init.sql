CREATE DATABASE IF NOT EXISTS conference_mic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE conference_mic;

DROP TABLE IF EXISTS t_seat;
CREATE TABLE t_seat (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    seat_no         VARCHAR(20) NOT NULL UNIQUE COMMENT '座位编号，如 A01、B12',
    row_no          VARCHAR(5)  NOT NULL COMMENT '排号',
    col_no          INT         NOT NULL COMMENT '列号',
    x_coord         DECIMAL(8,2) NOT NULL COMMENT 'X坐标(cm)，报告厅平面坐标系',
    y_coord         DECIMAL(8,2) NOT NULL COMMENT 'Y坐标(cm)，报告厅平面坐标系',
    z_coord         DECIMAL(8,2) DEFAULT 0 COMMENT 'Z坐标(cm)，通常为0（桌面高度）',
    zone            VARCHAR(20) DEFAULT '' COMMENT '座位区域：VIP/NORMAL/BACKSTAGE',
    status          TINYINT     DEFAULT 1 COMMENT '状态：1可用 0禁用',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT     DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告厅座位表';

DROP TABLE IF EXISTS t_microphone;
CREATE TABLE t_microphone (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    mic_code        VARCHAR(20) NOT NULL UNIQUE COMMENT '吊麦编号：MIC-01 ~ MIC-04',
    device_addr     INT         NOT NULL COMMENT 'RS485设备地址 1~4',
    x_coord         DECIMAL(8,2) NOT NULL COMMENT 'X坐标(cm)，天花板投影位置',
    y_coord         DECIMAL(8,2) NOT NULL COMMENT 'Y坐标(cm)，天花板投影位置',
    z_coord         DECIMAL(8,2) NOT NULL DEFAULT 300 COMMENT 'Z坐标(cm)，当前悬挂高度（距桌面）',
    base_height     DECIMAL(8,2) NOT NULL DEFAULT 300 COMMENT '基准高度(cm)，天花板默认高度',
    rotate_angle    DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '当前水平旋转角(度)，0度指正北',
    status          TINYINT     DEFAULT 1 COMMENT '状态：1在线 0离线 2故障',
    last_heartbeat  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT     DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字吊麦设备表';

DROP TABLE IF EXISTS t_tracking_task;
CREATE TABLE t_tracking_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_no         VARCHAR(32) NOT NULL UNIQUE COMMENT '任务编号',
    seat_no         VARCHAR(20) NOT NULL COMMENT '触发座位号',
    mic_id          BIGINT      NOT NULL COMMENT '分配的吊麦ID',
    target_x        DECIMAL(8,2) NOT NULL COMMENT '目标X坐标',
    target_y        DECIMAL(8,2) NOT NULL COMMENT '目标Y坐标',
    target_z        DECIMAL(8,2) NOT NULL COMMENT '目标Z坐标',
    drop_distance   DECIMAL(8,2) NOT NULL COMMENT '需要下降的距离(cm)',
    rotate_angle    DECIMAL(8,2) NOT NULL COMMENT '需要旋转的角度(度，带正负号)',
    task_status     TINYINT     DEFAULT 0 COMMENT '状态：0待执行 1执行中 2完成 3失败',
    start_time      DATETIME    DEFAULT NULL,
    finish_time     DATETIME    DEFAULT NULL,
    error_msg       VARCHAR(500) DEFAULT '',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='吊麦对焦追踪任务表';

DROP TABLE IF EXISTS t_operation_log;
CREATE TABLE t_operation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    op_type         VARCHAR(20) NOT NULL COMMENT '操作类型：BUTTON_TRIGGER/MIC_MOVE/STATUS_CHANGE',
    op_detail       VARCHAR(500) DEFAULT '' COMMENT '操作详情JSON',
    operator        VARCHAR(50) DEFAULT 'SYSTEM',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

INSERT INTO t_microphone (mic_code, device_addr, x_coord, y_coord, z_coord, base_height, rotate_angle, status) VALUES
('MIC-01', 1, 250.00, 200.00, 300.00, 300.00, 0, 1),
('MIC-02', 2, 750.00, 200.00, 300.00, 300.00, 0, 1),
('MIC-03', 3, 250.00, 600.00, 300.00, 300.00, 0, 1),
('MIC-04', 4, 750.00, 600.00, 300.00, 300.00, 0, 1);

INSERT INTO t_seat (seat_no, row_no, col_no, x_coord, y_coord, z_coord, zone, status) VALUES
('A01','A',1,150.00,350.00,0,'NORMAL',1),('A02','A',2,200.00,350.00,0,'NORMAL',1),
('A03','A',3,250.00,350.00,0,'NORMAL',1),('A04','A',4,300.00,350.00,0,'NORMAL',1),
('A05','A',5,350.00,350.00,0,'NORMAL',1),('A06','A',6,400.00,350.00,0,'NORMAL',1),
('A07','A',7,450.00,350.00,0,'NORMAL',1),('A08','A',8,500.00,350.00,0,'NORMAL',1),
('A09','A',9,550.00,350.00,0,'NORMAL',1),('A10','A',10,600.00,350.00,0,'NORMAL',1),
('A11','A',11,650.00,350.00,0,'NORMAL',1),('A12','A',12,700.00,350.00,0,'NORMAL',1),
('A13','A',13,750.00,350.00,0,'NORMAL',1),('A14','A',14,800.00,350.00,0,'NORMAL',1),
('A15','A',15,850.00,350.00,0,'NORMAL',1),
('B01','B',1,150.00,420.00,0,'NORMAL',1),('B02','B',2,200.00,420.00,0,'NORMAL',1),
('B03','B',3,250.00,420.00,0,'NORMAL',1),('B04','B',4,300.00,420.00,0,'NORMAL',1),
('B05','B',5,350.00,420.00,0,'NORMAL',1),('B06','B',6,400.00,420.00,0,'VIP',1),
('B07','B',7,450.00,420.00,0,'VIP',1),('B08','B',8,500.00,420.00,0,'VIP',1),
('B09','B',9,550.00,420.00,0,'VIP',1),('B10','B',10,600.00,420.00,0,'NORMAL',1),
('B11','B',11,650.00,420.00,0,'NORMAL',1),('B12','B',12,700.00,420.00,0,'NORMAL',1),
('B13','B',13,750.00,420.00,0,'NORMAL',1),('B14','B',14,800.00,420.00,0,'NORMAL',1),
('B15','B',15,850.00,420.00,0,'NORMAL',1),
('C01','C',1,150.00,490.00,0,'NORMAL',1),('C02','C',2,200.00,490.00,0,'NORMAL',1),
('C03','C',3,250.00,490.00,0,'NORMAL',1),('C04','C',4,300.00,490.00,0,'NORMAL',1),
('C05','C',5,350.00,490.00,0,'NORMAL',1),('C06','C',6,400.00,490.00,0,'NORMAL',1),
('C07','C',7,450.00,490.00,0,'NORMAL',1),('C08','C',8,500.00,490.00,0,'NORMAL',1),
('C09','C',9,550.00,490.00,0,'NORMAL',1),('C10','C',10,600.00,490.00,0,'NORMAL',1),
('C11','C',11,650.00,490.00,0,'NORMAL',1),('C12','C',12,700.00,490.00,0,'NORMAL',1),
('C13','C',13,750.00,490.00,0,'NORMAL',1),('C14','C',14,800.00,490.00,0,'NORMAL',1),
('C15','C',15,850.00,490.00,0,'NORMAL',1),
('D01','D',1,150.00,560.00,0,'NORMAL',1),('D02','D',2,200.00,560.00,0,'NORMAL',1),
('D03','D',3,250.00,560.00,0,'NORMAL',1),('D04','D',4,300.00,560.00,0,'NORMAL',1),
('D05','D',5,350.00,560.00,0,'NORMAL',1),('D06','D',6,400.00,560.00,0,'NORMAL',1),
('D07','D',7,450.00,560.00,0,'NORMAL',1),('D08','D',8,500.00,560.00,0,'NORMAL',1),
('D09','D',9,550.00,560.00,0,'NORMAL',1),('D10','D',10,600.00,560.00,0,'NORMAL',1),
('D11','D',11,650.00,560.00,0,'NORMAL',1),('D12','D',12,700.00,560.00,0,'NORMAL',1),
('D13','D',13,750.00,560.00,0,'NORMAL',1),('D14','D',14,800.00,560.00,0,'NORMAL',1),
('D15','D',15,850.00,560.00,0,'NORMAL',1);
