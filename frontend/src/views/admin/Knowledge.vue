<template>
  <div class="knowledge-page">
    <div class="left-panel">
      <div class="panel-head">
        <span>知识分类</span>
        <el-button size="small" type="primary" :icon="Plus" @click="openCategory()">新增</el-button>
      </div>
      <div class="cat-list">
        <div
          class="cat-item"
          :class="{ active: filter.categoryId === null }"
          @click="selectCategory(null)"
        >
          全部分类
        </div>
        <div
          v-for="c in categories"
          :key="c.id"
          class="cat-item"
          :class="{ active: filter.categoryId === c.id }"
          @click="selectCategory(c.id)"
        >
          <span class="cat-name">{{ c.name }}</span>
          <span class="cat-ops">
            <el-icon @click.stop="openCategory(c)"><Edit /></el-icon>
            <el-icon @click.stop="removeCategory(c)"><Delete /></el-icon>
          </span>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="toolbar">
        <el-input
          v-model="filter.keyword"
          placeholder="搜索问题 / 关键词 / 答案"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @keyup.enter="load()"
          @clear="load()"
        />
        <el-select v-model="filter.status" placeholder="状态" clearable style="width: 120px" @change="load()">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load()">查询</el-button>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Plus" @click="openKnowledge()">新增知识</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="question" label="标准问题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="110">
          <template #default="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="keywords" label="关键词" min-width="140" show-overflow-tooltip />
        <el-table-column prop="hits" label="命中" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openKnowledge(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeKnowledge(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="filter.size"
        :current-page="filter.page"
        @current-change="onPage"
      />
    </div>

    <!-- 知识编辑弹窗 -->
    <el-dialog v-model="kDialog" :title="kForm.id ? '编辑知识' : '新增知识'" width="560px">
      <el-form :model="kForm" label-width="80px">
        <el-form-item label="标准问题" required>
          <el-input v-model="kForm.question" placeholder="例如：如何申请退货？" />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="kForm.answer" type="textarea" :rows="4" placeholder="机器人将返回该答案" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="kForm.keywords" placeholder="多个关键词用逗号分隔，如：退货,退款,申请" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="kForm.categoryId" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="kForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="kDialog = false">取消</el-button>
        <el-button type="primary" @click="saveKnowledge">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类编辑弹窗 -->
    <el-dialog v-model="cDialog" :title="cForm.id ? '编辑分类' : '新增分类'" width="400px">
      <el-form :model="cForm" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="cForm.name" placeholder="例如：订单与物流" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="cForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search } from '@element-plus/icons-vue'
import {
  getKnowledge,
  createKnowledge,
  updateKnowledge,
  deleteKnowledge,
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory
} from '@/api'

const list = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const filter = reactive({ page: 1, size: 10, keyword: '', categoryId: null, status: null })

const kDialog = ref(false)
const kForm = reactive({ id: null, question: '', answer: '', keywords: '', categoryId: null, status: 1 })

const cDialog = ref(false)
const cForm = reactive({ id: null, name: '', sort: 0 })

async function loadCategories() {
  categories.value = await getCategories()
}

async function load() {
  loading.value = true
  try {
    const data = await getKnowledge({
      page: filter.page,
      size: filter.size,
      keyword: filter.keyword || undefined,
      categoryId: filter.categoryId ?? undefined,
      status: filter.status ?? undefined
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function selectCategory(id) {
  filter.categoryId = id
  filter.page = 1
  load()
}
function onPage(p) {
  filter.page = p
  load()
}

function openKnowledge(row) {
  if (row) {
    Object.assign(kForm, {
      id: row.id,
      question: row.question,
      answer: row.answer,
      keywords: row.keywords,
      categoryId: row.categoryId,
      status: row.status
    })
  } else {
    Object.assign(kForm, { id: null, question: '', answer: '', keywords: '', categoryId: null, status: 1 })
  }
  kDialog.value = true
}

async function saveKnowledge() {
  if (!kForm.question || !kForm.answer) {
    ElMessage.warning('请填写标准问题和答案')
    return
  }
  if (kForm.id) {
    await updateKnowledge(kForm.id, kForm)
  } else {
    await createKnowledge(kForm)
  }
  ElMessage.success('保存成功')
  kDialog.value = false
  load()
}

async function removeKnowledge(row) {
  await ElMessageBox.confirm(`确定删除「${row.question}」吗？`, '提示', { type: 'warning' })
  await deleteKnowledge(row.id)
  ElMessage.success('删除成功')
  load()
}

function openCategory(row) {
  if (row) {
    Object.assign(cForm, { id: row.id, name: row.name, sort: row.sort })
  } else {
    Object.assign(cForm, { id: null, name: '', sort: 0 })
  }
  cDialog.value = true
}

async function saveCategory() {
  if (!cForm.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (cForm.id) {
    await updateCategory(cForm.id, cForm)
  } else {
    await createCategory(cForm)
  }
  ElMessage.success('保存成功')
  cDialog.value = false
  loadCategories()
}

async function removeCategory(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  if (filter.categoryId === row.id) {
    filter.categoryId = null
  }
  loadCategories()
  load()
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<style scoped>
.knowledge-page {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.left-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  margin-bottom: 10px;
}
.cat-list {
  max-height: 60vh;
  overflow-y: auto;
}
.cat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}
.cat-item:hover {
  background: #f5f7fa;
}
.cat-item.active {
  background: #eef2ff;
  color: #4f7cff;
}
.cat-ops {
  display: none;
  gap: 6px;
  color: #909399;
}
.cat-item:hover .cat-ops {
  display: inline-flex;
}
.right-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}
.spacer {
  flex: 1;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
