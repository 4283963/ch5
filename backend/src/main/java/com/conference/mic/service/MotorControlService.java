package com.conference.mic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class MotorControlService {

    private final SerialPortService serialPortService;

    private final ReentrantLock motionLock = new ReentrantLock(true);

    private static final byte CMD_HEADER = 0xAA;
    private static final byte CMD_TAIL = 0x55;

    private static final byte CMD_SET_HEIGHT = 0x01;
    private static final byte CMD_SET_ROTATE = 0x02;
    private static final byte CMD_RESET = 0x03;
    private static final byte CMD_QUERY_STATUS = 0x04;

    public boolean moveMicrophone(int deviceAddr, BigDecimal dropDistanceCm, BigDecimal rotateAngleDeg) {
        motionLock.lock();
        try {
            log.info("开始驱动吊麦[{}]: 下降{}cm, 旋转{}度", deviceAddr, dropDistanceCm, rotateAngleDeg);

            boolean heightOk = true;
            if (dropDistanceCm != null && dropDistanceCm.compareTo(BigDecimal.ZERO) > 0) {
                int pulses = cmToPulses(dropDistanceCm);
                heightOk = sendHeightCommand(deviceAddr, pulses);
                log.info("吊麦[{}] 高度指令执行结果: {}", deviceAddr, heightOk);
                if (!heightOk) {
                    log.error("吊麦[{}] 高度指令失败, 终止旋转指令", deviceAddr);
                    return false;
                }
            }

            boolean rotateOk = true;
            if (rotateAngleDeg != null && rotateAngleDeg.abs().compareTo(BigDecimal.ZERO) > 0) {
                int pulses = degreeToPulses(rotateAngleDeg);
                rotateOk = sendRotateCommand(deviceAddr, pulses);
                log.info("吊麦[{}] 旋转指令执行结果: {}", deviceAddr, rotateOk);
            }

            return heightOk && rotateOk;
        } finally {
            motionLock.unlock();
        }
    }

    public boolean resetMicrophone(int deviceAddr) {
        motionLock.lock();
        try {
            log.info("复位吊麦[{}] 到初始位置", deviceAddr);
            byte[] frame = buildFrame(deviceAddr, CMD_RESET, new byte[0]);
            return sendAndCheck(frame);
        } finally {
            motionLock.unlock();
        }
    }

    private boolean sendHeightCommand(int deviceAddr, int pulses) {
        byte[] data = intToBytes(pulses);
        byte[] frame = buildFrame(deviceAddr, CMD_SET_HEIGHT, data);
        return sendAndCheck(frame);
    }

    private boolean sendRotateCommand(int deviceAddr, int pulses) {
        byte[] data = intToBytes(pulses);
        byte[] frame = buildFrame(deviceAddr, CMD_SET_ROTATE, data);
        return sendAndCheck(frame);
    }

    private byte[] buildFrame(int deviceAddr, byte cmd, byte[] data) {
        int frameLen = 5 + data.length + 1;
        byte[] frame = new byte[frameLen];

        frame[0] = CMD_HEADER;
        frame[1] = (byte) (deviceAddr & 0xFF);
        frame[2] = cmd;
        frame[3] = (byte) (data.length & 0xFF);

        System.arraycopy(data, 0, frame, 4, data.length);

        byte checksum = calculateChecksum(frame, 0, 4 + data.length);
        frame[4 + data.length] = checksum;
        frame[5 + data.length - 1] = CMD_TAIL;

        return frame;
    }

    private byte calculateChecksum(byte[] data, int offset, int length) {
        byte checksum = 0;
        for (int i = offset; i < offset + length; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }

    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    private int cmToPulses(BigDecimal cm) {
        double pulsesPerCm = 100.0;
        return cm.multiply(BigDecimal.valueOf(pulsesPerCm)).intValue();
    }

    private int degreeToPulses(BigDecimal degree) {
        double pulsesPerDegree = 50.0;
        return degree.multiply(BigDecimal.valueOf(pulsesPerDegree)).intValue();
    }

    private boolean sendAndCheck(byte[] frame) {
        try {
            return serialPortService.sendCommand(frame);
        } catch (Exception e) {
            log.error("发送串口指令失败: {}", e.getMessage());
            return false;
        }
    }
}
