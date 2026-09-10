<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" :style="{ '--c': card.color }">
          <div class="stat-icon">{{ card.icon }}</div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="trend-card">
      <div class="trend-title">近 7 日会话趋势</div>
      <div class="trend-chart">
        <div v-for="(v, i) in trend.values" :key="i" class="trend-col">
          <div class="trend-bar-wrap">
            <div class="trend-bar" :style="{ height: barHeight(v) }"></div>
            <span class="trend-num">{{ v }}</span>
          </div>
          <div class="trend-day">{{ shortDay(trend.days[i]) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getTrend } from '@/api'

const overview = ref({})
const trend = ref({ days: [], values: [] })

const cards = computed(() => [
  { label: '总会话数', value: overview.value.totalSessions ?? '-', icon: '💬', color: '#4f7cff' },
  { label: '今日会话', value: overview.value.todaySessions ?? '-', icon: '📅', color: '#67c23a' },
  { label: '排队中', value: overview.value.waitingSessions ?? '-', icon: '⏳', color: '#e6a23c' },
  { label: '进行中', value: overview.value.activeSessions ?? '-', icon: '🟢', color: '#409eff' },
  { label: '消息总数', value: overview.value.totalMessages ?? '-', icon: '✉️', color: '#7b5cff' },
  { label: '机器人回复', value: overview.value.robotMessages ?? '-', icon: '🤖', color: '#13c2c2' },
  { label: '知识条目', value: overview.value.knowledgeCount ?? '-', icon: '📚', color: '#f56c6c' },
  { label: '知识命中', value: overview.value.knowledgeHits ?? '-', icon: '🎯', color: '#ff8c42' },
  { label: '客服人数', value: overview.value.agentCount ?? '-', icon: '👩‍💼', color: '#909399' }
])

function barHeight(v) {
  const max = Math.max(...trend.value.values, 1)
  return Math.max(v > 0 ? 8 : 2, Math.round((v / max) * 140)) + 'px'
}
function shortDay(d) {
  return d ? d.slice(5) : ''
}

onMounted(async () => {
  overview.value = await getOverview()
  trend.value = await getTrend()
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 16px;
  border-left: 4px solid var(--c);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.stat-icon {
  font-size: 22px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  margin: 6px 0 2px;
}
.stat-label {
  font-size: 13px;
  color: #909399;
}

.trend-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.trend-title {
  font-weight: 600;
  margin-bottom: 20px;
}
.trend-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 220px;
}
.trend-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.trend-bar-wrap {
  height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}
.trend-bar {
  width: 34px;
  background: linear-gradient(180deg, #7b5cff, #4f7cff);
  border-radius: 6px 6px 0 0;
  transition: height 0.3s;
}
.trend-num {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.trend-day {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}
</style>
