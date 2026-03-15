import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE || ''

export const request = axios.create({
  baseURL: API_BASE || '/api',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
})

/** 用于 SSE 的 base URL（相对路径走 Vite 代理） */
export function getApiBase() {
  return API_BASE || ''
}
