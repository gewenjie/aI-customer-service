<template>
  <div class="chat-page">
    <div class="chat-card">
      <!-- 侧边栏 -->
      <aside class="chat-side">
        <div class="side-logo">
          <div class="logo-icon">🤖</div>
          <div>
            <div class="logo-title">智能客服</div>
            <div class="logo-sub">7×24 小时在线</div>
          </div>
        </div>
        <div class="side-section">
          <div class="side-title">常见问题</div>
          <div
            v-for="q in quickQuestions"
            :key="q"
            class="quick-item"
            @click="send(q)"
          >
            {{ q }}
          </div>
        </div>
        <div class="side-footer">
          <div class="side-tip">💡 输入「转人工」或点击下方按钮，可转接人工客服</div>
        </div>
      </aside>

      <!-- 主聊天区 -->
      <section class="chat-main">
        <header class="chat-header">
          <el-avatar :size="38" class="robot-avatar">🤖</el-avatar>
          <div class="header-info">
            <div class="header-name">小智 · 智能客服</div>
            <div class="header-status">
              <span class="dot" :class="statusClass"></span>
              {{ statusText }}
            </div>
          </div>
          <el-button
            v-if="!transferRequested && !closed"
            type="warning"
            plain
            size="small"
            :icon="Service"
            @click="requestHuman"
          >
            转人工
          </el-button>
        </header>

        <div ref="listRef" class="chat-list">
          <div v-if="loading" class="chat-loading">
            <el-icon class="is-loading"><Loading /></el-icon> 加载中…
          </div>

          <template v-for="(m, i) in messages" :key="m.messageId || i">
            <!-- 系统消息 -->
            <div v-if="m.senderType === 'SYSTEM'" class="sys-msg">
              <span>{{ m.content }}</span>
            </div>
            <!-- 普通气泡 -->
            <div
              v-else
              class="msg-row"
              :class="m.senderType === 'CUSTOMER' ? 'from-customer' : 'from-bot'"
            >
              <el-avatar
                v-if="m.senderType !== 'CUSTOMER'"
                :size="34"
                class="msg-avatar"
                :class="m.senderType === 'AGENT' ? 'agent-avatar' : 'robot-avatar'"
              >
                {{ m.senderType === 'AGENT' ? '👩‍💼' : '🤖' }}
              </el-avatar>
              <div class="msg-body">
                <div v-if="m.senderType !== 'CUSTOMER'" class="msg-name">
                  {{ m.senderName || '智能客服' }}
                </div>
                <div class="bubble" :class="m.senderType === 'CUSTOMER' ? 'bubble-customer' : 'bubble-bot'">
                  {{ m.content }}
                </div>
                <div class="msg-time">{{ m.time }}</div>
              </div>
              <el-avatar v-if="m.senderType === 'CUSTOMER'" :size="34" class="msg-avatar user-avatar">
                🧑
              </el-avatar>
            </div>
          </template>

          <!-- 满意度评价 -->
          <div v-if="closed && !rated" class="rate-box">
            <div class="rate-title">本次服务还满意吗？</div>
            <el-rate v-model="rating" size="large" @change="submitRate" />
          </div>
          <div v-if="rated" class="rate-done">感谢您的评价！❤️</div>
        </div>

        <footer class="chat-input">
          <div v-if="!connected" class="reconnect-bar">
            连接已断开
            <el-button size="small" type="primary" @click="reconnect">重新连接</el-button>
          </div>
          <div class="input-row">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              resize="none"
              placeholder="请输入您的问题…"
              :disabled="closed"
              @keydown.enter.exact.prevent="send()"
            />
            <el-button
              class="send-btn"
              type="primary"
              :disabled="closed || !inputText.trim()"
              @click="send()"
            >
              发送
            </el-button>
          </div>
          <div class="input-tip">Enter 发送 · Shift+Enter 换行</div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Service, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createSession, getChatMessages, rateSession } from '@/api'
import { createChatSocket } from '@/utils/ws'

const quickQuestions = [
  '如何申请退货？',
  '退款多久到账？',
  '支持哪些支付方式？',
  '忘记密码怎么办？'
]

const sessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const loading = ref(true)
const connected = ref(false)
const transferRequested = ref(false)
const closed = ref(false)
const rated = ref(false)
const rating = ref(0)
const listRef = ref(null)
let ws = null

const statusClass = computed(() => {
  if (closed.value) return 'off'
  if (transferRequested.value) return 'waiting'
  return 'on'
})
const statusText = computed(() => {
  if (closed.value) return '会话已结束'
  if (transferRequested.value) return '等待人工客服接入…'
  return '在线（智能客服）'
})

function normalize(m) {
  return {
    messageId: m.messageId != null ? m.messageId : m.id,
    senderType: m.senderType,
    senderName: m.senderName,
    content: m.content,
    time: m.timestamp || m.createdAt
  }
}

function pushMessage(m) {
  const item = normalize(m)
  // 按 messageId 去重
  if (item.messageId != null && messages.value.some((x) => x.messageId === item.messageId)) {
    return
  }
  messages.value.push(item)
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}

function connectWs() {
  ws = createChatSocket({ sessionId: sessionId.value, role: 'CUSTOMER' })
  ws.onopen = () => {
    connected.value = true
  }
  ws.onmessage = (evt) => {
    try {
      const data = JSON.parse(evt.data)
      if (data.type === 'SYSTEM' || data.senderType === 'SYSTEM') {
        pushMessage(data)
        if (data.content && data.content.includes('客服') && data.content.includes('已接入')) {
          transferRequested.value = false
        }
        if (data.content && data.content.includes('会话已结束')) {
          closed.value = true
          transferRequested.value = false
        }
        return
      }
      if (data.senderType === 'AGENT') {
        transferRequested.value = false
      }
      pushMessage(data)
    } catch (e) {
      /* ignore */
    }
  }
  ws.onclose = () => {
    connected.value = false
  }
  ws.onerror = () => {
    connected.value = false
  }
}

function send(text) {
  const content = (text || inputText.value || '').trim()
  if (!content) return
  if (closed.value) {
    ElMessage.info('会话已结束，请刷新页面开启新会话')
    return
  }
  if (!connected.value) {
    ElMessage.warning('连接已断开，请先重新连接')
    return
  }
  ws.send(JSON.stringify({ type: 'CHAT', content }))
  inputText.value = ''
}

function requestHuman() {
  if (!connected.value) {
    ElMessage.warning('连接已断开，请先重新连接')
    return
  }
  transferRequested.value = true
  ws.send(JSON.stringify({ type: 'TRANSFER' }))
}

async function submitRate(val) {
  try {
    await rateSession(sessionId.value, val)
    rated.value = true
    ElMessage.success('感谢您的评价')
  } catch (e) {
    /* handled by interceptor */
  }
}

async function reconnect() {
  if (ws) {
    ws.close()
  }
  connectWs()
  // 重连后拉取一次历史，补齐断线期间的消息
  try {
    const history = await getChatMessages(sessionId.value)
    history.forEach(pushMessage)
  } catch (e) {
    /* ignore */
  }
}

onMounted(async () => {
  try {
    const session = await createSession()
    sessionId.value = session.id
    const history = await getChatMessages(session.id)
    history.forEach(pushMessage)
    connectWs()
  } catch (e) {
    ElMessage.error('初始化会话失败，请刷新重试')
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (ws) {
    ws.close()
  }
})
</script>

<style scoped>
.chat-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4f7cff 0%, #7b5cff 100%);
  padding: 24px;
}

.chat-card {
  width: 860px;
  max-width: 96vw;
  height: 640px;
  max-height: 92vh;
  display: flex;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.chat-side {
  width: 220px;
  flex-shrink: 0;
  background: #f7f8fc;
  border-right: 1px solid #eef0f5;
  display: flex;
  flex-direction: column;
}

.side-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid #eef0f5;
}
.logo-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f7cff, #7b5cff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.logo-title {
  font-weight: 600;
  font-size: 15px;
}
.logo-sub {
  font-size: 12px;
  color: #909399;
}

.side-section {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}
.side-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}
.quick-item {
  font-size: 13px;
  color: #4f7cff;
  background: #eef2ff;
  padding: 9px 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-item:hover {
  background: #4f7cff;
  color: #fff;
}

.side-footer {
  padding: 14px 16px;
  border-top: 1px solid #eef0f5;
}
.side-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f2f5;
}
.robot-avatar {
  background: linear-gradient(135deg, #4f7cff, #7b5cff);
}
.header-info {
  flex: 1;
}
.header-name {
  font-weight: 600;
  font-size: 15px;
}
.header-status {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 5px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.dot.on {
  background: #67c23a;
}
.dot.waiting {
  background: #e6a23c;
}
.dot.off {
  background: #c0c4cc;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafbfe;
}
.chat-loading {
  text-align: center;
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 20px;
}

.sys-msg {
  text-align: center;
  margin: 12px 0;
}
.sys-msg span {
  display: inline-block;
  background: #eef0f5;
  color: #909399;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 12px;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;
}
.msg-row.from-customer {
  justify-content: flex-end;
}
.msg-avatar {
  flex-shrink: 0;
  font-size: 16px;
}
.robot-avatar {
  background: #eef2ff;
}
.agent-avatar {
  background: #fdf0e6;
}
.user-avatar {
  background: #e8f7ee;
}
.msg-body {
  max-width: 65%;
  display: flex;
  flex-direction: column;
}
.msg-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble-bot {
  background: #fff;
  border: 1px solid #eef0f5;
  border-top-left-radius: 4px;
}
.bubble-customer {
  background: #4f7cff;
  color: #fff;
  border-top-right-radius: 4px;
}
.msg-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}

.rate-box {
  text-align: center;
  padding: 16px;
  background: #fff;
  border: 1px dashed #e6a23c;
  border-radius: 12px;
  margin: 8px 0;
}
.rate-title {
  font-size: 14px;
  margin-bottom: 8px;
}
.rate-done {
  text-align: center;
  color: #67c23a;
  padding: 12px;
}

.chat-input {
  border-top: 1px solid #f0f2f5;
  padding: 12px 16px;
  background: #fff;
}
.reconnect-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e6a23c;
  font-size: 13px;
  margin-bottom: 8px;
}
.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.send-btn {
  height: 54px;
}
.input-tip {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
