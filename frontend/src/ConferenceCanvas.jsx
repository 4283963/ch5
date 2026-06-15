import React, { useEffect, useRef, useCallback } from 'react'

const ROOM_WIDTH_CM = 1000
const ROOM_HEIGHT_CM = 800
const PADDING = 60

function ConferenceCanvas({ seats, microphones, activeTracking, filteredMicId }) {
  const canvasRef = useRef(null)
  const animFrameRef = useRef(null)
  const animStateRef = useRef({
    startTime: 0,
    duration: 2000,
    tracking: null,
    animating: false
  })

  const worldToScreen = useCallback((canvas, x, y) => {
    const scaleX = (canvas.width - PADDING * 2) / ROOM_WIDTH_CM
    const scaleY = (canvas.height - PADDING * 2) / ROOM_HEIGHT_CM
    const scale = Math.min(scaleX, scaleY)
    const offsetX = (canvas.width - ROOM_WIDTH_CM * scale) / 2
    const offsetY = (canvas.height - ROOM_HEIGHT_CM * scale) / 2
    return {
      x: offsetX + x * scale,
      y: offsetY + y * scale,
      scale
    }
  }, [])

  const drawStage = useCallback((ctx, canvas) => {
    const { x: sx, y: sy } = worldToScreen(canvas, 350, 100)
    const { x: ex, y: ey } = worldToScreen(canvas, 650, 200)

    const grad = ctx.createLinearGradient(sx, sy, sx, ey)
    grad.addColorStop(0, 'rgba(14, 165, 233, 0.3)')
    grad.addColorStop(1, 'rgba(14, 165, 233, 0.05)')

    ctx.fillStyle = grad
    ctx.strokeStyle = 'rgba(56, 189, 248, 0.5)'
    ctx.lineWidth = 2

    const radius = 6
    ctx.beginPath()
    ctx.moveTo(sx + radius, sy)
    ctx.lineTo(ex - radius, sy)
    ctx.quadraticCurveTo(ex, sy, ex, sy + radius)
    ctx.lineTo(ex, ey - radius)
    ctx.quadraticCurveTo(ex, ey, ex - radius, ey)
    ctx.lineTo(sx + radius, ey)
    ctx.quadraticCurveTo(sx, ey, sx, ey - radius)
    ctx.lineTo(sx, sy + radius)
    ctx.quadraticCurveTo(sx, sy, sx + radius, sy)
    ctx.closePath()
    ctx.fill()
    ctx.stroke()

    ctx.fillStyle = '#38bdf8'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('主 席 台', (sx + ex) / 2, (sy + ey) / 2)
  }, [worldToScreen])

  const drawSeat = useCallback((ctx, canvas, seat, isActive) => {
    const { x, y, scale } = worldToScreen(canvas, seat.xCoord, seat.yCoord)
    const size = 28
    const r = size / 2

    if (isActive) {
      const glow = ctx.createRadialGradient(x, y, 0, x, y, r * 2.5)
      glow.addColorStop(0, 'rgba(34, 197, 94, 0.6)')
      glow.addColorStop(1, 'rgba(34, 197, 94, 0)')
      ctx.fillStyle = glow
      ctx.fillRect(x - r * 3, y - r * 3, r * 6, r * 6)
    }

    ctx.beginPath()
    ctx.roundRect(x - r, y - r, size, size, 4)

    let fillColor = 'rgba(51, 65, 85, 0.8)'
    let strokeColor = 'rgba(100, 116, 139, 0.6)'
    let textColor = '#94a3b8'

    if (seat.zone === 'VIP') {
      fillColor = 'rgba(245, 158, 11, 0.2)'
      strokeColor = 'rgba(245, 158, 11, 0.5)'
      textColor = '#fbbf24'
    }
    if (isActive) {
      fillColor = 'rgba(34, 197, 94, 0.3)'
      strokeColor = '#22c55e'
      textColor = '#4ade80'
    }

    ctx.fillStyle = fillColor
    ctx.fill()
    ctx.strokeStyle = strokeColor
    ctx.lineWidth = 1.5
    ctx.stroke()

    ctx.fillStyle = textColor
    ctx.font = `${Math.max(9, scale * 8)}px sans-serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(seat.seatNo, x, y)
  }, [worldToScreen])

  const drawMicrophone = useCallback((ctx, canvas, mic, animState, isFiltered) => {
    const { x, y } = worldToScreen(canvas, mic.xCoord, mic.yCoord)
    const r = 16

    let currentX = x
    let currentY = y
    let dropProgress = 0

    if (animState.animating && animState.tracking && animState.tracking.micId === mic.id) {
      const t = animState.tracking
      const elapsed = Date.now() - animState.startTime
      const progress = Math.min(elapsed / animState.duration, 1)
      dropProgress = easeInOutCubic(progress)

      const targetScreen = worldToScreen(canvas, t.seatX, t.seatY)
      currentX = x + (targetScreen.x - x) * dropProgress * 0.5
      currentY = y + (targetScreen.y - y) * dropProgress * 0.5

      if (progress >= 1) {
        animState.animating = false
      }
    }

    let glowColor1 = 'rgba(239, 68, 68, 0.5)'
    let glowColor2 = 'rgba(239, 68, 68, 0)'
    let micGrad1 = '#f87171'
    let micGrad2 = '#b91c1c'
    let strokeColor = '#fecaca'

    if (isFiltered) {
      glowColor1 = 'rgba(245, 158, 11, 0.7)'
      glowColor2 = 'rgba(245, 158, 11, 0)'
      micGrad1 = '#fcd34d'
      micGrad2 = '#d97706'
      strokeColor = '#fef3c7'
    }

    const glow = ctx.createRadialGradient(currentX, currentY, 0, currentX, currentY, r * 3)
    glow.addColorStop(0, glowColor1)
    glow.addColorStop(1, glowColor2)
    ctx.fillStyle = glow
    ctx.fillRect(currentX - r * 4, currentY - r * 4, r * 8, r * 8)

    if (isFiltered) {
      const pulseR = r + 10 + Math.sin(Date.now() / 150) * 5
      const filterGlow = ctx.createRadialGradient(currentX, currentY, 0, currentX, currentY, pulseR * 2)
      filterGlow.addColorStop(0, 'rgba(245, 158, 11, 0.4)')
      filterGlow.addColorStop(1, 'rgba(245, 158, 11, 0)')
      ctx.fillStyle = filterGlow
      ctx.fillRect(currentX - pulseR * 2, currentY - pulseR * 2, pulseR * 4, pulseR * 4)

      ctx.strokeStyle = `rgba(245, 158, 11, ${0.5 + Math.sin(Date.now() / 200) * 0.3})`
      ctx.lineWidth = 3
      ctx.setLineDash([6, 6])
      ctx.beginPath()
      ctx.arc(currentX, currentY, pulseR, 0, Math.PI * 2)
      ctx.stroke()
      ctx.setLineDash([])
    }

    ctx.beginPath()
    ctx.arc(currentX, currentY, r, 0, Math.PI * 2)
    const micGrad = ctx.createRadialGradient(currentX - 3, currentY - 3, 0, currentX, currentY, r)
    micGrad.addColorStop(0, micGrad1)
    micGrad.addColorStop(1, micGrad2)
    ctx.fillStyle = micGrad
    ctx.fill()
    ctx.strokeStyle = strokeColor
    ctx.lineWidth = 2
    ctx.stroke()

    ctx.fillStyle = '#ffffff'
    ctx.font = 'bold 11px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(mic.micCode.split('-')[1], currentX, currentY)

    if (isFiltered) {
      ctx.fillStyle = '#fbbf24'
      ctx.font = 'bold 10px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText('静止过滤', currentX, currentY + r + 14)
    }

    if (animState.animating && animState.tracking && animState.tracking.micId === mic.id && !isFiltered) {
      ctx.strokeStyle = `rgba(34, 197, 94, ${0.3 + dropProgress * 0.4})`
      ctx.lineWidth = 2
      ctx.setLineDash([5, 5])
      ctx.beginPath()
      ctx.arc(currentX, currentY, r + 8 + dropProgress * 4, 0, Math.PI * 2)
      ctx.stroke()
      ctx.setLineDash([])
    }
  }, [worldToScreen])

  const drawTrackingBeam = useCallback((ctx, canvas, animState) => {
    if (!animState.animating || !animState.tracking || animState.tracking.filtered) return

    const t = animState.tracking
    const micScreen = worldToScreen(canvas, t.micX, t.micY)
    const seatScreen = worldToScreen(canvas, t.seatX, t.seatY)

    const elapsed = Date.now() - animState.startTime
    const progress = Math.min(elapsed / animState.duration, 1)
    const eased = easeInOutCubic(progress)

    const beamStartX = micScreen.x + (seatScreen.x - micScreen.x) * eased * 0.3
    const beamStartY = micScreen.y + (seatScreen.y - micScreen.y) * eased * 0.3
    const beamEndX = micScreen.x + (seatScreen.x - micScreen.x) * eased
    const beamEndY = micScreen.y + (seatScreen.y - micScreen.y) * eased

    const beamGrad = ctx.createLinearGradient(beamStartX, beamStartY, beamEndX, beamEndY)
    beamGrad.addColorStop(0, `rgba(248, 113, 113, 0.8)`)
    beamGrad.addColorStop(1, `rgba(34, 197, 94, 0.9)`)

    ctx.strokeStyle = beamGrad
    ctx.lineWidth = 3
    ctx.setLineDash([10, 6])
    ctx.lineDashOffset = -(Date.now() / 30) % 16
    ctx.beginPath()
    ctx.moveTo(beamStartX, beamStartY)
    ctx.lineTo(beamEndX, beamEndY)
    ctx.stroke()
    ctx.setLineDash([])

    if (eased > 0.5) {
      const pulseR = 8 + Math.sin(Date.now() / 100) * 4
      const pulseGrad = ctx.createRadialGradient(beamEndX, beamEndY, 0, beamEndX, beamEndY, pulseR * 2)
      pulseGrad.addColorStop(0, 'rgba(34, 197, 94, 0.7)')
      pulseGrad.addColorStop(1, 'rgba(34, 197, 94, 0)')
      ctx.fillStyle = pulseGrad
      ctx.beginPath()
      ctx.arc(beamEndX, beamEndY, pulseR * 2, 0, Math.PI * 2)
      ctx.fill()

      ctx.beginPath()
      ctx.arc(beamEndX, beamEndY, pulseR, 0, Math.PI * 2)
      ctx.fillStyle = '#22c55e'
      ctx.fill()
    }
  }, [worldToScreen])

  const drawGrid = useCallback((ctx, canvas) => {
    const { x: ox, y: oy, scale } = worldToScreen(canvas, 0, 0)
    const w = ROOM_WIDTH_CM * scale
    const h = ROOM_HEIGHT_CM * scale

    ctx.strokeStyle = 'rgba(56, 189, 248, 0.06)'
    ctx.lineWidth = 1
    for (let i = 0; i <= 10; i++) {
      const x = ox + (w / 10) * i
      ctx.beginPath()
      ctx.moveTo(x, oy)
      ctx.lineTo(x, oy + h)
      ctx.stroke()
    }
    for (let i = 0; i <= 8; i++) {
      const y = oy + (h / 8) * i
      ctx.beginPath()
      ctx.moveTo(ox, y)
      ctx.lineTo(ox + w, y)
      ctx.stroke()
    }

    ctx.strokeStyle = 'rgba(56, 189, 248, 0.25)'
    ctx.lineWidth = 2
    ctx.strokeRect(ox, oy, w, h)

    ctx.fillStyle = 'rgba(148, 163, 184, 0.5)'
    ctx.font = '10px sans-serif'
    ctx.textAlign = 'right'
    ctx.textBaseline = 'bottom'
    ctx.fillText('N (0°)', ox + w / 2, oy - 8)
  }, [worldToScreen])

  const draw = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')

    const rect = canvas.getBoundingClientRect()
    const dpr = window.devicePixelRatio || 1
    canvas.width = rect.width * dpr
    canvas.height = rect.height * dpr
    ctx.scale(dpr, dpr)
    canvas.style.width = rect.width + 'px'
    canvas.style.height = rect.height + 'px'

    ctx.clearRect(0, 0, rect.width, rect.height)

    const bgGrad = ctx.createRadialGradient(rect.width / 2, rect.height / 2, 0, rect.width / 2, rect.height / 2, rect.width)
    bgGrad.addColorStop(0, 'rgba(15, 23, 42, 1)')
    bgGrad.addColorStop(1, 'rgba(2, 6, 23, 1)')
    ctx.fillStyle = bgGrad
    ctx.fillRect(0, 0, rect.width, rect.height)

    const virtualCanvas = { width: rect.width, height: rect.height }

    drawGrid(ctx, virtualCanvas)
    drawStage(ctx, virtualCanvas)

    const activeSeatNo = animStateRef.current.tracking?.seatNo
    seats.forEach(seat => {
      drawSeat(ctx, virtualCanvas, seat, seat.seatNo === activeSeatNo)
    })

    microphones.forEach(mic => {
      drawMicrophone(ctx, virtualCanvas, mic, animStateRef.current, mic.id === filteredMicId)
    })

    drawTrackingBeam(ctx, virtualCanvas, animStateRef.current)

    animFrameRef.current = requestAnimationFrame(draw)
  }, [seats, microphones, filteredMicId, drawGrid, drawStage, drawSeat, drawMicrophone, drawTrackingBeam])

  useEffect(() => {
    if (activeTracking) {
      animStateRef.current = {
        startTime: Date.now(),
        duration: activeTracking.animationDuration || 2000,
        tracking: activeTracking,
        animating: !activeTracking.filtered
      }
    }
  }, [activeTracking])

  useEffect(() => {
    animFrameRef.current = requestAnimationFrame(draw)
    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current)
    }
  }, [draw])

  return (
    <div className="canvas-wrapper">
      <canvas ref={canvasRef} className="conference-canvas" />
      <div className="status-indicator">
        <span className="status-dot"></span>
        <span>系统运行中</span>
      </div>
    </div>
  )
}

function easeInOutCubic(t) {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}

export default ConferenceCanvas
