const API_BASE = '/api'

export async function fetchSeats() {
  const res = await fetch(`${API_BASE}/tracking/seats`)
  const data = await res.json()
  return data.code === 200 ? data.data : []
}

export async function fetchMicrophones() {
  const res = await fetch(`${API_BASE}/tracking/microphones`)
  const data = await res.json()
  return data.code === 200 ? data.data : []
}

export async function fetchOverview() {
  const res = await fetch(`${API_BASE}/tracking/overview`)
  const data = await res.json()
  return data.code === 200 ? data.data : {}
}

export async function triggerSpeak(seatNo) {
  const res = await fetch(`${API_BASE}/tracking/speak`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ seatNo })
  })
  const data = await res.json()
  if (data.code !== 200) {
    throw new Error(data.message || '请求失败')
  }
  return data.data
}

export async function fetchNoiseConfig() {
  const res = await fetch(`${API_BASE}/tracking/noise-config`)
  const data = await res.json()
  return data.code === 200 ? data.data : {}
}

export async function updateNoiseConfig(config) {
  const res = await fetch(`${API_BASE}/tracking/noise-config`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config)
  })
  const data = await res.json()
  if (data.code !== 200) {
    throw new Error(data.message || '配置更新失败')
  }
  return data.data
}
