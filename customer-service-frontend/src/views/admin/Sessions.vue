<template>
  <div class="sessions-page">
    <div class="list-panel">
      <div class="panel-head">
        <el-tabs v-model="activeTab" class="tabs" @tab-change="switchTab">
          <el-tab-pane label="排队中" name="WAITING" />
          <el-tab-pane label="处理中" name="ACTIVE" />
          <el-tab-pane label="已结束" name="CLOSED" />
        </el-tabs>
        <el-button :icon="Refresh" circle size="small" @click="load()" />
      </div>
      <div class="search-row">
        <el-input v-model="keyword" placeholder="搜索访客 / 会话编号" clearable :prefix-icon="Search" @keyup.enter="load()" @clear="load()" />
      </div>

      <div class="session-list" v-loading="loading">
        <div
          v-for="s in list"
          :key="s.id"
          class="session-item"
          :class="{ active: current && current.id === s.id }"
          @click="selectSession(s)"
        >
          <div class="s-top">
            <span class="s-name">{{ s.visitorName || '访客' }}</span>
            <el-tag size="small" :type="statusType(s.status)">{{ statusText(s.status) }}</el-tag>
          </div>
          <div class="s-no">{{ s.sessionNo }}</div>
          <div class="s-last">{{ s.lastMessage || '（暂无消息）' }}</div>
          <div class="s-bottom">
            <span class="s-time">{{ s.lastMessageTime || s.createdAt }}</span>
            <span v-if="s.messageCount" class="s-count">{{ s.messageCount }} 条</span>
          </div>
        </div>
        <el-empty v-if="!loading && list.length === 0" description="暂无会话" :image-size="80" />
      </div>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPage"
        small
      />
    </div>

    <div class="chat-panel">
      <template v-if="current">
        <div class="chat-head">
          <el-avatar :size="34" class="user-avatar">🧑</el-avatar>
          <div class="chat-head-info">
            <div class="chat-head-name">{{ current.visitorName || '访客' }}</div>
            <div class="chat-head-no">{{ current.sessionNo }}</div>
          </div>
          <div class="spacer"></div>
          <el-button
            v-if="current.status === 'WAITING'"
            type="success"
            :icon="Service"
            @click="takeCurrent"
          >
            接入会话
          </el-button>
          <el-button
            v-else-if="current.status === 'ACTIVE'"
            type="danger"
            plain
            @click="closeCurrent"
          >
            结束会话
          </el-button>
        </div>

        <div ref="msgRef" class="chat-body">
          <div v-for="(m, i) in messages" :key="m.messageId || i">
            <div v-if="m.senderType === 'SYSTEM'" class="sys-msg"><span>{{ m.content }}</span></div>
            <div v-else class="msg-row" :class="m.senderType === 'AGENT' ? 'from-agent' : 'from-visitor'">
              <div class="bubble" :class="m.senderType === 'AGENT' ? 'bubble-agent' : 'bubble-visitor'">
                <div v-if="m.senderType !== 'AGENT'" class="b-name">{{ m.senderName || '访客' }}</div>
                <div class="b-content">{{ m.content }}</div>
                <div class="b-time">{{ m.time }}</div>
              </div>
            </div>
          </div>
          <div v-if="current.status === 'WAITING'" class="waiting-tip">
            ⏳ 该访客正在排队等待接入，点击右上角「接入会话」开始服务
          </div>
          <div v-if="current.status === 'CLOSED'" class="closed-tip">
            会话已结束{{ current.rating ? `，满意度评分：${current.rating} 星` : '' }}
          </div>
        </div>

        <div class="chat-foot">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="输入回复内容…"
            :disabled="current.status !== 'ACTIVE' || !connected"
            @keydown.enter.exact.prevent="sendMsg"
          />
          <el-button
            type="primary"
            class="send-btn"
            :disabled="current.status !== 'ACTIVE' || !connected || !inputText.trim()"
            @click="sendMsg"
          >
            发送
          </el-button>
        </div>
      </template>
      <el-empty v-else description="请选择左侧会话" class="empty-chat" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Service } from '@element-plus/icons-vue'
import { getSessions, getSessionMessages, takeSession, closeSession } from '@/api'
import { createChatSocket } from '@/utils/ws'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const activeTab = ref('WAITING')
const keyword = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(8)
const loading = ref(false)

const current = ref(null)
const messages = ref([])
const inputText = ref('')
const connected = ref(false)
const msgRef = ref(null)
let ws = null
let timer = null

function statusType(s) {
  return { ACTIVE: 'primary', WAITING: 'warning', CLOSED: 'info' }[s] || 'info'
}
function statusText(s) {
  return { ACTIVE: '处理中', WAITING: '排队中', CLOSED: '已结束' }[s] || s
}

async function load() {
  loading.value = true
  try {
    const data = await getSessions({
      page: page.value,
      size: size.value,
      status: activeTab.value,
      keyword: keyword.value || undefined
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function switchTab() {
  page.value = 1
  load()
}
function onPage(p) {
  page.value = p
  load()
}

function closeWs() {
  if (ws) {
    ws.close()
    ws = null
  }
  connected.value = false
}

function connectAgentWs(sessionId) {
  closeWs()
  ws = createChatSocket({ sessionId, role: 'AGENT', token: userStore.token })
  ws.onopen = () => {
    connected.value = true
  }
  ws.onmessage = (evt) => {
    try {
      const data = JSON.parse(evt.data)
      pushMessage(data)
    } catch (e) {
      /* ignore */
    }
  }
  ws.onclose = () => {
    connected.value = false
  }
}

function pushMessage(m) {
  const item = {
    messageId: m.messageId != null ? m.messageId : m.id,
    senderType: m.senderType,
    senderName: m.senderName,
    content: m.content,
    time: m.timestamp || m.createdAt
  }
  if (item.messageId != null && messages.value.some((x) => x.messageId === item.messageId)) {
    return
  }
  messages.value.push(item)
  nextTick(() => {
    if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
  })
}

async function selectSession(s) {
  current.value = s
  messages.value = []
  try {
    const history = await getSessionMessages(s.id)
    history.forEach(pushMessage)
  } catch (e) {
    /* ignore */
  }
  if (s.status === 'ACTIVE') {
    connectAgentWs(s.id)
  } else {
    closeWs()
  }
}

async function takeCurrent() {
  await takeSession(current.value.id)
  ElMessage.success('已接入，开始服务')
  current.value.status = 'ACTIVE'
  await load()
  connectAgentWs(current.value.id)
}

async function closeCurrent() {
  await ElMessageBox.confirm('确定结束该会话吗？', '提示', { type: 'warning' })
  await closeSession(current.value.id)
  current.value.status = 'CLOSED'
  closeWs()
  ElMessage.success('会话已结束')
  load()
}

function sendMsg() {
  const content = inputText.value.trim()
  if (!content || !connected.value) return
  ws.send(JSON.stringify({ type: 'CHAT', content }))
  inputText.value = ''
}

onMounted(() => {
  load()
  // 每 15 秒刷新队列
  timer = setInterval(() => {
    if (activeTab.value === 'WAITING') load()
  }, 15000)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  closeWs()
})
</script>

<style scoped>
.sessions-page {
  display: flex;
  gap: 16px;
  height: calc(100vh - 100px);
  min-height: 520px;
}

.list-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-head {
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-bottom: 1px solid #f0f2f5;
}
.tabs {
  flex: 1;
}
.search-row {
  padding: 10px 12px;
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px;
}
.session-item {
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 8px;
  border: 1px solid #f0f2f5;
  transition: all 0.2s;
}
.session-item:hover {
  border-color: #4f7cff;
}
.session-item.active {
  border-color: #4f7cff;
  background: #eef2ff;
}
.s-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.s-name {
  font-weight: 600;
  font-size: 14px;
}
.s-no {
  font-size: 12px;
  color: #c0c4cc;
  margin: 3px 0;
}
.s-last {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.s-bottom {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
}
.s-time {
  font-size: 12px;
  color: #c0c4cc;
}
.s-count {
  font-size: 12px;
  color: #909399;
}
.pager {
  padding: 10px;
  justify-content: center;
}

.chat-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}
.user-avatar {
  background: #e8f7ee;
  font-size: 16px;
}
.chat-head-name {
  font-weight: 600;
  font-size: 15px;
}
.chat-head-no {
  font-size: 12px;
  color: #c0c4cc;
}
.spacer {
  flex: 1;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #fafbfe;
}
.sys-msg {
  text-align: center;
  margin: 10px 0;
}
.sys-msg span {
  display: inline-block;
  background: #eef0f5;
  color: #909399;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 12px;
}
.msg-row {
  display: flex;
  margin-bottom: 12px;
}
.msg-row.from-agent {
  justify-content: flex-end;
}
.msg-row.from-visitor {
  justify-content: flex-start;
}
.bubble {
  max-width: 68%;
  padding: 9px 13px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble-visitor {
  background: #fff;
  border: 1px solid #eef0f5;
  border-top-left-radius: 4px;
}
.bubble-agent {
  background: #4f7cff;
  color: #fff;
  border-top-right-radius: 4px;
}
.b-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 3px;
}
.b-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 3px;
  text-align: right;
}
.waiting-tip,
.closed-tip {
  text-align: center;
  color: #e6a23c;
  font-size: 13px;
  padding: 16px;
}

.chat-foot {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 16px;
  border-top: 1px solid #f0f2f5;
}
.send-btn {
  height: 54px;
}
.empty-chat {
  margin: auto;
}
</style>
