<template>
  <div class="agents-page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索登录名 / 昵称"
        clearable
        style="width: 240px"
        :prefix-icon="Search"
        @keyup.enter="load()"
        @clear="load()"
      />
      <el-button type="primary" :icon="Search" @click="load()">查询</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增客服</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="登录名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="toggle(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPage"
    />

    <el-dialog v-model="dialog" :title="form.id ? '编辑客服' : '新增客服'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="登录名" required>
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '密码'" :required="!form.id">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="form.id ? '留空则不修改' : '初始密码'"
          />
        </el-form-item>
        <el-form-item v-if="form.id" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search } from '@element-plus/icons-vue'
import { getAgents, createAgent, updateAgent, toggleAgent, deleteAgent } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)

const dialog = ref(false)
const form = reactive({ id: null, username: '', nickname: '', password: '', status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await getAgents({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  page.value = p
  load()
}

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, nickname: row.nickname, password: '', status: row.status })
  } else {
    Object.assign(form, { id: null, username: '', nickname: '', password: '', status: 1 })
  }
  dialog.value = true
}

async function save() {
  if (!form.username) {
    ElMessage.warning('请输入登录名')
    return
  }
  if (!form.id && !form.password) {
    ElMessage.warning('请输入初始密码')
    return
  }
  if (form.id) {
    await updateAgent(form.id, form)
  } else {
    await createAgent(form)
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function toggle(row) {
  await ElMessageBox.confirm(`确定${row.status === 1 ? '禁用' : '启用'}客服「${row.nickname || row.username}」吗？`, '提示', {
    type: 'warning'
  })
  await toggleAgent(row.id)
  ElMessage.success('操作成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除客服「${row.nickname || row.username}」吗？`, '提示', { type: 'warning' })
  await deleteAgent(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.agents-page {
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
