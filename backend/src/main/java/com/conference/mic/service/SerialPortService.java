package com.conference.mic.service;

import com.conference.mic.config.SerialPortConfig;
import com.fazecast.jSerialComm.SerialPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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

    private volatile SerialPort serialPort;

    private volatile OutputStream outputStream;

    private volatile InputStream inputStream;

    private final ReentrantLock commandLock = new ReentrantLock(true);

    private volatile boolean portOpen = false;

    private static final long LOCK_TIMEOUT_MS = 5000;

    private static final long CMD_INTERVAL_MS = 80;

    private volatile long lastSendTime = 0;

    @PostConstruct
    public void init() {
        tryOpenPort();
        if (!portOpen) {
            log.warn("串口 {} 初始化失败, 将以模拟模式运行, 后续自动重连", config.getPort());
        }
    }

    @PreDestroy
    public void destroy() {
        closePortQuietly();
    }

    private synchronized void tryOpenPort() {
        if (portOpen && serialPort != null && serialPort.isOpen()) {
            return;
        }

        closePortQuietly();

        try {
            SerialPort port = SerialPort.getCommPort(config.getPort());
            if (port == null) {
                log.warn("未找到串口设备: {}", config.getPort());
                return;
            }

            port.setComPortParameters(
                    config.getBaudRate(),
                    config.getDataBits(),
                    config.getStopBits(),
                    getParityCode(config.getParity())
            );
            port.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    config.getTimeout(),
                    config.getTimeout()
            );

            if (port.openPort()) {
                this.serialPort = port;
                this.outputStream = port.getOutputStream();
                this.inputStream = port.getInputStream();
                this.portOpen = true;
                log.info("串口 {} 打开成功(长连接), 波特率: {}", config.getPort(), config.getBaudRate());
            } else {
                log.warn("串口 {} openPort()返回false", config.getPort());
            }
        } catch (Exception e) {
            log.error("串口 {} 打开异常: {}", config.getPort(), e.getMessage());
            closePortQuietly();
        }
    }

    private void closePortQuietly() {
        portOpen = false;
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception ignored) {
        }
        outputStream = null;
        inputStream = null;

        try {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                log.info("串口 {} 已关闭", config.getPort());
            }
        } catch (Exception ignored) {
        }
        serialPort = null;
    }

    private boolean ensurePortOpen() {
        if (portOpen && serialPort != null && serialPort.isOpen()) {
            return true;
        }
        log.warn("串口连接断开, 尝试重连...");
        tryOpenPort();
        return portOpen;
    }

    public boolean sendCommand(byte[] command) throws IOException {
        if (!portOpen && !ensurePortOpen()) {
            log.warn("[模拟模式] 发送指令: {}", bytesToHex(command));
            return true;
        }

        boolean locked = false;
        try {
            locked = commandLock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("获取串口锁被中断");
        }

        if (!locked) {
            log.error("串口指令互斥锁等待超时({}ms), 丢弃指令: {}", LOCK_TIMEOUT_MS, bytesToHex(command));
            throw new IOException("串口繁忙, 指令排队超时");
        }

        try {
            enforceCommandInterval();

            if (!ensurePortOpen()) {
                log.warn("重连失败, 降级为模拟模式: {}", bytesToHex(command));
                return true;
            }

            OutputStream out = this.outputStream;
            if (out == null) {
                throw new IOException("串口输出流不可用");
            }

            out.write(command);
            out.flush();
            lastSendTime = System.currentTimeMillis();
            log.debug("串口指令已发送: {}", bytesToHex(command));

            return readAck();
        } catch (IOException e) {
            log.error("串口发送失败, 标记断开等待重连: {}", e.getMessage());
            portOpen = false;
            throw e;
        } finally {
            commandLock.unlock();
        }
    }

    private void enforceCommandInterval() {
        long elapsed = System.currentTimeMillis() - lastSendTime;
        if (elapsed < CMD_INTERVAL_MS) {
            try {
                TimeUnit.MILLISECONDS.sleep(CMD_INTERVAL_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean readAck() {
        try {
            InputStream in = this.inputStream;
            if (in == null) {
                return true;
            }

            long deadline = System.currentTimeMillis() + Math.min(config.getTimeout(), 500);
            while (System.currentTimeMillis() < deadline) {
                if (in.available() > 0) {
                    byte[] buffer = new byte[32];
                    int len = in.read(buffer);
                    if (len > 0) {
                        log.debug("收到串口应答: {}", bytesToHex(buffer, len));
                        return true;
                    }
                }
                TimeUnit.MILLISECONDS.sleep(10);
            }
            return true;
        } catch (Exception e) {
            log.error("读取串口应答失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean isPortOpen() {
        return portOpen && serialPort != null && serialPort.isOpen();
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
