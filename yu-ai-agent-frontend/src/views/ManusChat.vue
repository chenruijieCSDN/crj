<template>
  <div class="chat-page chat-page-manus">
    <header class="chat-header">
      <router-link to="/" class="back">← 返回</router-link>
      <h1>AI 超级智能体</h1>
    </header>
    <div class="messages" ref="messagesRef">
      <div
        v-for="(msg, i) in messages"
        :key="i"
        :class="['msg', msg.role === 'user' ? 'msg-user' : 'msg-ai']"
      >
        <div v-if="msg.role === 'ai'" class="msg-avatar" aria-hidden="true">🤖</div>
        <div class="msg-bubble">
          <div class="msg-content">
            <template v-for="(seg, segIdx) in (msg.role === 'ai' ? contentSegments(msg.content) : [{ type: 'text', value: msg.content }])" :key="segIdx">
              <span v-if="seg.type === 'text'" class="msg-text">{{ seg.value }}</span>
              <img v-else-if="seg.type === 'image'" :src="seg.value" class="msg-inline-img" alt="图片" loading="lazy" />
            </template>
          </div>
          <template v-if="msg.role === 'ai' && getPdfFilename(msg.content)">
            <a :href="pdfDownloadUrl(getPdfFilename(msg.content))" class="pdf-link" target="_blank" rel="noopener">📄 下载 PDF</a>
          </template>
          <span v-if="msg.role === 'ai' && msg.streaming" class="cursor">▌</span>
        </div>
      </div>
    </div>
    <div class="input-area">
      <textarea
        v-model="input"
        placeholder="描述你的任务..."
        rows="2"
        :disabled="loading"
        @keydown.enter.exact.prevent="send"
      />
      <button class="send-btn" :disabled="loading || !input.trim()" @click="send">
        {{ loading ? `执行中… ${loadingElapsed}s` : '发送' }}
      </button>
    </div>
    <p v-if="loading" class="loading-hint">步骤较多时可能需 1～2 分钟，请耐心等待</p>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { getApiBase } from '../api'

const API_BASE = getApiBase()
const BASE_URL = (API_BASE || '') + '/api'

const messages = ref([])
const input = ref('')
const loading = ref(false)
const messagesRef = ref(null)
/** 本次请求开始时间（用于显示已等待秒数） */
const loadingStartAt = ref(0)
/** 已等待秒数（每秒更新，便于用户知道未卡死） */
const loadingElapsed = ref(0)
let loadingTimer = null
/** 用于在离开页面时取消进行中的请求 */
let abortController = null

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

function parseSSELine(line) {
  const s = line.trim()
  if (s.startsWith('data:')) return s.slice(5).trim()
  return s
}

/** 从 AI 回复中解析「PDF generated successfully to: ...」得到文件名，供下载用 */
function getPdfFilename(content) {
  if (!content || typeof content !== 'string') return null
  const prefix = 'PDF generated successfully to:'
  const idx = content.indexOf(prefix)
  if (idx === -1) return null
  const path = content.slice(idx + prefix.length).trim()
  const name = path.split(/[/\\]/).pop()
  return name && name.endsWith('.pdf') ? name : null
}

function pdfDownloadUrl(filename) {
  return `${BASE_URL}/files/pdf?name=${encodeURIComponent(filename)}`
}

/** 判断 URL 是否为可展示的图片（高德静态图、常见图片扩展名等） */
function isImageUrl(url) {
  if (!url || typeof url !== 'string') return false
  const u = url.trim()
  if (u.includes('restapi.amap.com/v3/staticmap')) return true
  if (/\.(jpg|jpeg|png|gif|webp)(\?|$)/i.test(u)) return true
  return false
}

/** 将 AI 回复内容拆成文本段与图片段，便于渲染文字+图片 */
function contentSegments(content) {
  if (!content || typeof content !== 'string') return [{ type: 'text', value: '' }]
  // 匹配可能是图片的 URL（高德 staticmap 或常见图片后缀）
  const urlRe = /https?:\/\/[^\s\n\)\]"]+/g
  const segments = []
  let lastEnd = 0
  let match
  const re = new RegExp(urlRe.source, 'g')
  while ((match = re.exec(content)) !== null) {
    const url = match[0]
    if (!isImageUrl(url)) continue
    if (match.index > lastEnd) {
      segments.push({ type: 'text', value: content.slice(lastEnd, match.index) })
    }
    segments.push({ type: 'image', value: url })
    lastEnd = re.lastIndex
  }
  if (lastEnd < content.length) {
    segments.push({ type: 'text', value: content.slice(lastEnd) })
  }
  return segments.length ? segments : [{ type: 'text', value: content }]
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  scrollToBottom()

  const aiIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '', streaming: true })
  loading.value = true
  loadingStartAt.value = Date.now()
  loadingElapsed.value = 0
  loadingTimer = setInterval(() => {
    loadingElapsed.value = Math.floor((Date.now() - loadingStartAt.value) / 1000)
  }, 1000)

  const url = `${BASE_URL}/ai/manus/chat`
  abortController = new AbortController()
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text }),
      signal: abortController.signal,
    })
    if (!res.ok) {
      const errText = await res.text()
      throw new Error(errText || `请求失败 ${res.status}`)
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        const data = parseSSELine(line)
        if (data && messages.value[aiIndex]) {
          /* 后端每完成一步才响应一次，每条 SSE 消息后多加一个换行 */
          messages.value[aiIndex].content += data + '\n\n'
          scrollToBottom()
        }
      }
    }
    if (buffer.trim() && messages.value[aiIndex]) {
      const data = parseSSELine(buffer)
      if (data) messages.value[aiIndex].content += data + '\n\n'
    }
  } catch (e) {
    if (e.name === 'AbortError') {
      if (messages.value[aiIndex]) messages.value[aiIndex].content += '\n\n[已取消]'
    } else if (messages.value[aiIndex]) {
      messages.value[aiIndex].content = '请求失败：' + (e.message || String(e))
    }
  } finally {
    if (loadingTimer) {
      clearInterval(loadingTimer)
      loadingTimer = null
    }
    abortController = null
    if (messages.value[aiIndex]) messages.value[aiIndex].streaming = false
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {})

onUnmounted(() => {
  if (loadingTimer) {
    clearInterval(loadingTimer)
    loadingTimer = null
  }
  if (abortController) {
    abortController.abort()
    abortController = null
  }
})
</script>

<style scoped>
.chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}
.chat-header {
  flex-shrink: 0;
  min-height: var(--header-height);
  padding: 0.5rem 0.75rem;
  background: var(--manus-bg);
  border-bottom: 1px solid var(--manus-border);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.back {
  color: var(--manus-accent);
  text-decoration: none;
  font-size: 0.9rem;
}
.back:hover { text-decoration: underline; }
.chat-header h1 {
  font-size: 1rem;
  margin: 0;
  color: var(--text-primary);
  flex: 1;
  min-width: 0;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 0;
}
.msg {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
}
.msg-user {
  justify-content: flex-end;
  flex-direction: row-reverse;
}
.msg-ai {
  justify-content: flex-start;
}
.msg-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  background: var(--manus-bubble-ai-border);
}
.msg-bubble {
  max-width: 85%;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-user .msg-bubble {
  margin-left: auto;
  background: var(--manus-bubble-user);
  color: #fff;
}
.msg-ai .msg-bubble {
  background: var(--manus-bubble-ai-bg);
  border: 1px solid var(--manus-bubble-ai-border);
  color: #333;
}
.msg-ai .msg-content {
  text-align: left;
}
.msg-content { display: block; }
.msg-text { white-space: pre-wrap; word-break: break-word; }
.msg-inline-img {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-sm);
  margin-top: 0.5rem;
  border: 1px solid var(--border);
}
.pdf-link {
  display: inline-block;
  margin-top: 0.5rem;
  padding: 0.35rem 0.75rem;
  background: var(--manus-accent);
  color: #fff;
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-size: 0.875rem;
}
.pdf-link:hover { filter: brightness(1.1); }
.cursor { animation: blink 0.8s step-end infinite; color: var(--manus-accent); }
@keyframes blink { 50% { opacity: 0; } }
.input-area {
  flex-shrink: 0;
  padding: 0.75rem;
  background: var(--surface);
  border-top: 1px solid var(--border);
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
}
.input-area textarea {
  flex: 1;
  min-width: 0;
  padding: 0.65rem 0.85rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-family: inherit;
  font-size: 0.95rem;
  resize: none;
}
.input-area textarea:focus {
  outline: none;
  border-color: var(--manus-accent);
}
.send-btn {
  padding: 0.65rem 1rem;
  background: var(--manus-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}
.send-btn:hover:not(:disabled) { filter: brightness(0.95); }
.send-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.loading-hint {
  margin: 0;
  padding: 0.5rem 1rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

@media (min-width: 769px) {
  .chat-header { padding: 0.75rem 1rem; gap: 1rem; }
  .chat-header h1 { font-size: 1.1rem; }
  .messages { padding: 1rem; gap: 1.25rem; }
  .msg-avatar { width: 40px; height: 40px; font-size: 1.25rem; }
  .msg-bubble { max-width: 70%; padding: 0.85rem 1.1rem; }
  .input-area { padding: 1rem; gap: 0.75rem; }
  .input-area textarea { padding: 0.75rem 1rem; }
  .send-btn { padding: 0.75rem 1.25rem; }
}

@media (min-width: 1024px) {
  .msg-bubble { max-width: 55%; }
}
</style>
