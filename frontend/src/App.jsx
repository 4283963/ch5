import React, { useState, useEffect, useRef } from 'react'
import ConferenceCanvas from './ConferenceCanvas.jsx'
import { fetchOverview, triggerSpeak, fetchNoiseConfig, updateNoiseConfig } from './api.js'

export default function App() {
  const [seats, setSeats] = useState([])
  const [microphones, setMicrophones] = useState([])
  const [tasks, setTasks] = useState([])
  const [activeTracking, setActiveTracking] = useState(null)
  const [noiseConfig, setNoiseConfig] = useState({ enabled: true, minDb: 45, durationMs: 2000 })
  const [filteredMicId, setFilteredMicId] = useState(null)
  const [seatInput, setSeatInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [wsConnected, setWsConnected] = useState(false)
  const [error, setError] = useState('')
  const wsRef = useRef(null)

  useEffect(() => {
    loadData()
    connectWebSocket()
    return () => {
      if (wsRef.current) wsRef.current.close()
    }
  }, [])

  const loadData = async () => {
    try {
      const [overview, noise] = await Promise.all([fetchOverview(), fetchNoiseConfig()])
      if (overview.seats) setSeats(overview.seats)
      if (overview.microphones) setMicrophones(overview.microphones)
      if (overview.tasks) setTasks(overview.tasks)
      if (noise) setNoiseConfig(noise)
    } catch (e) {
      console.error('加载数据失败:', e)
    }
  }

  const connectWebSocket = () => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/api/ws/tracking`

    try {
      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        setWsConnected(true)
        console.log('WebSocket 连接成功')
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('收到追踪数据:', data)
          setActiveTracking(data)

          if (data.filtered) {
            setFilteredMicId(data.micId)
            setTimeout(() => setFilteredMicId(null), 5000)
          } else {
            setFilteredMicId(null)
          }

          loadData()
        } catch (e) {
          console.error('解析WS消息失败:', e)
        }
      }

      ws.onclose = () => {
        setWsConnected(false)
        console.log('WebSocket 已断开, 3秒后重连...')
        setTimeout(connectWebSocket, 3000)
      }

      ws.onerror = (e) => {
        console.error('WebSocket 错误:', e)
      }
    } catch (e) {
      console.error('WebSocket 初始化失败:', e)
      setTimeout(connectWebSocket, 3000)
    }
  }

  const handleTrigger = async () => {
    const seatNo = seatInput.trim().toUpperCase()
    if (!seatNo) {
      setError('请输入座位号')
      return
    }
    setError('')
    setLoading(true)
    try {
      const result = await triggerSpeak(seatNo)
      setActiveTracking(result)
      if (result.filtered) {
        setFilteredMicId(result.micId)
        setTimeout(() => setFilteredMicId(null), 5000)
      }
      loadData()
    } catch (e) {
      setError(e.message || '触发失败')
    } finally {
      setLoading(false)
    }
  }

  const toggleNoiseThreshold = async () => {
    try {
      const newConfig = await updateNoiseConfig({ enabled: !noiseConfig.enabled })
      setNoiseConfig(newConfig)
    } catch (e) {
      setError(e.message || '配置更新失败')
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') handleTrigger()
  }

  const formatAngle = (deg) => {
    const d = Number(deg || 0)
    return `${d > 0 ? '+' : ''}${d.toFixed(1)}°`
  }

  return (
    <div className="app-container">
      <header className="header">
        <h1>多功能会议室多维度数字吊麦与光电对焦追踪系统</h1>
        <div className="subtitle">Multi-dimensional Digital Ceiling Mic & Photoelectric Focusing Tracking System</div>
      </header>

      <div className="main-content">
        <div className="canvas-panel">
          <div className="panel-title">报告厅实时状态监控</div>
          <ConferenceCanvas
            seats={seats}
            microphones={microphones}
            activeTracking={activeTracking}
            filteredMicId={filteredMicId}
          />
          <div className="legend">
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#334155' }}></span>
              <span>普通座位</span>
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#f59e0b', opacity: 0.3 }}></span>
              <span>VIP座位</span>
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#22c55e' }}></span>
              <span>发言人座位</span>
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#ef4444', borderRadius: '50%' }}></span>
              <span>数字吊麦</span>
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#f59e0b', borderRadius: '50%' }}></span>
              <span>静止微调过滤</span>
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: 'linear-gradient(90deg, #f87171, #22c55e)' }}></span>
              <span>对焦追踪光束</span>
            </div>
          </div>
        </div>

        <div className="side-panel">
          <div className="control-card">
            <div className="panel-title">模拟发言按钮</div>
            <div className="control-form">
              <input
                type="text"
                className="control-input"
                placeholder="输入座位号 (如 B07)"
                value={seatInput}
                onChange={(e) => setSeatInput(e.target.value)}
                onKeyPress={handleKeyPress}
                disabled={loading}
              />
              <button
                className="btn-primary"
                onClick={handleTrigger}
                disabled={loading}
              >
                {loading ? '处理中' : '触发'}
              </button>
            </div>
            {error && <div style={{ color: '#ef4444', fontSize: 12, marginTop: 8 }}>{error}</div>}
            <div style={{ marginTop: 10, fontSize: 12, color: '#64748b' }}>
              WebSocket 状态: {wsConnected ? (
                <span style={{ color: '#22c55e' }}>已连接</span>
              ) : (
                <span style={{ color: '#ef4444' }}>未连接</span>
              )}
            </div>
          </div>

          <div className="info-card">
            <div className="panel-title">噪声滞后门槛</div>
            <div className="info-row">
              <span className="info-label">功能状态</span>
              <span className={`info-value ${noiseConfig.enabled ? 'active' : ''}`} style={{ color: noiseConfig.enabled ? '#22c55e' : '#94a3b8' }}>
                {noiseConfig.enabled ? '已启用' : '已禁用'}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">分贝阈值</span>
              <span className="info-value">≥{noiseConfig.minDb}dB</span>
            </div>
            <div className="info-row">
              <span className="info-label">持续时间</span>
              <span className="info-value">{noiseConfig.durationMs / 1000}秒</span>
            </div>
            <button
              className="btn-primary"
              onClick={toggleNoiseThreshold}
              style={{
                width: '100%',
                marginTop: 12,
                background: noiseConfig.enabled
                  ? 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)'
                  : 'linear-gradient(135deg, #22c55e 0%, #16a34a 100%)'
              }}
            >
              {noiseConfig.enabled ? '关闭噪声过滤' : '开启噪声过滤'}
            </button>
            <div style={{ marginTop: 10, fontSize: 11, color: '#64748b', lineHeight: 1.5 }}>
              开启后，只有当座位麦克风连续{noiseConfig.durationMs / 1000}秒采样超过{noiseConfig.minDb}dB，才会驱动吊麦转动，防止咳嗽、翻书等噪声误触发。
            </div>
          </div>

          <div className="info-card">
            <div className="panel-title">吊麦设备状态</div>
            <div className="mic-status-grid">
              {microphones.map(mic => (
                <div
                  key={mic.id}
                  className="mic-status-item"
                  style={{
                    borderColor: filteredMicId === mic.id ? 'rgba(245, 158, 11, 0.6)' : undefined,
                    background: filteredMicId === mic.id ? 'rgba(245, 158, 11, 0.15)' : undefined
                  }}
                >
                  <div className="mic-code" style={{ color: filteredMicId === mic.id ? '#fbbf24' : undefined }}>
                    {mic.micCode}
                    {filteredMicId === mic.id && (
                      <span style={{ fontSize: 10, marginLeft: 4, color: '#f59e0b' }}>过滤中</span>
                    )}
                  </div>
                  <div className="mic-detail">高度: {Number(mic.zCoord || 0).toFixed(0)}cm</div>
                  <div className="mic-detail">角度: {formatAngle(mic.rotateAngle)}</div>
                  <div className="mic-detail">
                    状态: {mic.status === 1 ? (
                      <span style={{ color: '#22c55e' }}>在线</span>
                    ) : (
                      <span style={{ color: '#ef4444' }}>离线</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="info-card">
            <div className="panel-title">当前追踪任务</div>
            {activeTracking ? (
              <div
                className="tracking-highlight"
                style={{
                  background: activeTracking.filtered
                    ? 'rgba(245, 158, 11, 0.15)'
                    : 'rgba(34, 197, 94, 0.15)',
                  borderColor: activeTracking.filtered
                    ? 'rgba(245, 158, 11, 0.4)'
                    : 'rgba(34, 197, 94, 0.4)'
                }}
              >
                <div
                  className="tracking-title"
                  style={{ color: activeTracking.filtered ? '#f59e0b' : '#22c55e' }}
                >
                  {activeTracking.filtered ? '⚠ 噪声过滤 - 吊麦静止' : '✓ 正在追踪'}
                </div>
                <div className="info-row">
                  <span className="info-label">任务编号</span>
                  <span className="info-value">{activeTracking.taskNo || '-'}</span>
                </div>
                <div className="info-row">
                  <span className="info-label">发言人座位</span>
                  <span className="info-value active">{activeTracking.seatNo}</span>
                </div>
                <div className="info-row">
                  <span className="info-label">分配吊麦</span>
                  <span className="info-value">{activeTracking.micCode}</span>
                </div>
                {activeTracking.filtered ? (
                  <div className="info-row">
                    <span className="info-label">过滤原因</span>
                    <span className="info-value" style={{ color: '#fbbf24', fontSize: 11 }}>
                      {activeTracking.filterReason}
                    </span>
                  </div>
                ) : (
                  <>
                    <div className="info-row">
                      <span className="info-label">下降距离</span>
                      <span className="info-value">{Number(activeTracking.dropDistance || 0).toFixed(1)} cm</span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">旋转角度</span>
                      <span className="info-value">{formatAngle(activeTracking.rotateAngle)}</span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">动画时长</span>
                      <span className="info-value">{activeTracking.animationDuration || 2000} ms</span>
                    </div>
                  </>
                )}
              </div>
            ) : (
              <div style={{ color: '#64748b', fontSize: 13, textAlign: 'center', padding: 20 }}>
                暂无追踪任务，请触发发言按钮
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
