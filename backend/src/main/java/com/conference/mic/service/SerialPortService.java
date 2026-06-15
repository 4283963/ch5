package com.conference.mic.service;

import com.conference.mic.config.SerialPortConfig;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortIOException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerialPortService {

    private final SerialPortConfig config;

    private SerialPort serialPort;

    private final ReentrantLock writeLock = new ReentrantLock();

    private volatile boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            serialPort = SerialPort.getCommPort(config.getPort());
            if (serialPort == null) {
                log.warn("未找到串口设备: {}, 将以模拟模式运行", config.getPort());
                return;
            }

            serialPort.setComPortParameters(
                    config.getBaudRate(),
                    config.getDataBits(),
                    config.getStopBits(),
                    getParityCode(config.getParity())
            );
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    config.getTimeout(),
                    config.getTimeout()
            );

            if (serialPort.openPort()) {
                initialized = true;
                log.info("串口 {} 打开成功, 波特率: {}", config.getPort(), config.getBaudRate());
            } else {
                log.warn("串口 {} 打开失败, 将以模拟模式运行", config.getPort());
            }
        } catch (Exception e) {
            log.warn("串口初始化异常, 将以模拟模式运行: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            log.info("串口已关闭");
        }
    }

    public boolean sendCommand(byte[] command) throws IOException {
        if (!initialized) {
            log.warn("[模拟模式] 发送指令: {}", bytesToHex(command));
            return true;
        }

        if (writeLock.tryLock()) {
            try {
                OutputStream out = serialPort.getOutputStream();
                out.write(command);
                out.flush();
                log.debug("串口指令已发送: {}", bytesToHex(command));

                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                return readAck();
            } finally {
                writeLock.unlock();
            }
        } else {
            throw new SerialPortIOException("串口写入锁获取超时");
        }
    }

    private boolean readAck() {
        try {
            InputStream in = serialPort.getInputStream();
            if (in.available() > 0) {
                byte[] buffer = new byte[16];
                int len = in.read(buffer);
                if (len > 0) {
                    log.debug("收到串口应答: {}", bytesToHex(buffer, len));
                    return true;
                }
            }
            return true;
        } catch (IOException e) {
            log.error("读取串口应答失败: {}", e.getMessage());
            return false;
        }
    }

    private int getParityCode(String parity) {
        return switch (parity.toUpperCase()) {
            case "ODD" -> SerialPort.ODD_PARITY;
            case "EVEN" -> SerialPort.EVEN_PARITY;
            case "MARK" -> SerialPort.MARK_PARITY;
            case "SPACE" -> SerialPort.SPACE_PARITY;
            default -> SerialPort.NO_PARITY;
        };
    }

    private String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, bytes.length);
    }

    private String bytesToHex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }
}
