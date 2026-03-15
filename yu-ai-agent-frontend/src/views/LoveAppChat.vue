<template>
  <div class="chat-page chat-page-love">
    <header class="chat-header">
      <router-link to="/" class="back">← 返回</router-link>
      <h1>AI 恋爱大师</h1>
      <span class="chat-id">会话 ID: {{ shortChatId }}</span>
    </header>
    <div class="messages" ref="messagesRef">
      <div
        v-for="(msg, i) in messages"
        :key="i"
        :class="['msg', msg.role === 'user' ? 'msg-user' : 'msg-ai']"
      >
        <div v-if="msg.role === 'ai'" class="msg-avatar" aria-hidden="true">💕</div>
        <div class="msg-bubble">
          <div class="msg-content">{{ msg.content }}</div>
          <span v-if="msg.role === 'ai' && msg.streaming" class="cursor">▌</span>
        </div>
      </div>
    </div>
    <div class="input-area">
      <textarea
        v-model="input"
        placeholder="输入消息..."
        rows="2"
        :disabled="loading"
        @keydown.enter.exact.prevent="send"
      />
      <button class="send-btn" :disabled="loading || !input.trim()" @click="send">
        {{ loading ? '回复中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { getApiBase } from '../api'

const API_BASE = getApiBase()
const BASE_URL = (API_BASE || '') + '/api'

const messages = ref([])
const input = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const chatId = ref('')

const shortChatId = computed(() => {
  const id = chatId.value
  return id ? id.slice(0, 8) : ''
})

function ensureChatId() {
  if (!chatId.value) {
    chatId.value = crypto.randomUUID ? crypto.randomUUID() : `chat-${Date.now()}`
  }
  return chatId.value
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

let eventSource = null

function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  ensureChatId()
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  scrollToBottom()

  const aiIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '', streaming: true })
  loading.value = true

  const url = `${BASE_URL}/ai/love_app/chat/sse?message=${encodeURIComponent(text)}&chatId=${encodeURIComponent(chatId.value)}`
  eventSource = new EventSource(url)

  eventSource.onmessage = (e) => {
    const chunk = e.data
    if (typeof chunk === 'string' && messages.value[aiIndex]) {
      messages.value[aiIndex].content += chunk
      scrollToBottom()
    }
  }

  eventSource.onerror = () => {
    eventSource.close()
    eventSource = null
    if (messages.value[aiIndex]) {
      messages.value[aiIndex].streaming = false
      if (!messages.value[aiIndex].content) {
        messages.value[aiIndex].content = '连接中断或服务异常，请重试。'
      }
    }
    loading.value = false
  }
}

onMounted(() => {
  ensureChatId()
})

onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
})
</script>

<style scoped>
.chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fefbfa;
}
.chat-header {
  flex-shrink: 0;
  min-height: var(--header-height);
  padding: 0.5rem 0.75rem;
  background: var(--love-bg);
  border-bottom: 1px solid var(--love-border);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.back {
  color: var(--love-accent);
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
.chat-id {
  font-size: 0.7rem;
  color: var(--text-secondary);
  width: 100%;
  order: 3;
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
  background: var(--love-bubble-ai-border);
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
  background: var(--love-bubble-user);
  color: #2d1517;
}
.msg-ai .msg-bubble {
  background: var(--love-bubble-ai-bg);
  border: 1px solid var(--love-bubble-ai-border);
  color: #333;
}
.msg-ai .msg-content {
  text-align: left;
}
.msg-content { display: inline; }
.cursor { animation: blink 0.8s step-end infinite; color: var(--love-accent); }
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
  border-color: var(--love-accent);
}
.send-btn {
  padding: 0.65rem 1rem;
  background: var(--love-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}
.send-btn:hover:not(:disabled) { filter: brightness(0.95); }
.send-btn:disabled { opacity: 0.6; cursor: not-allowed; }

@media (min-width: 769px) {
  .chat-header {
    padding: 0.75rem 1rem;
    gap: 1rem;
    flex-wrap: nowrap;
  }
  .chat-header h1 { font-size: 1.1rem; }
  .chat-id { width: auto; order: 0; font-size: 0.75rem; }
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
