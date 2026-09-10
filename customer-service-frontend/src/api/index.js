import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')

// 访客聊天（公开）
export const createSession = (visitorName) => request.post('/chat/session', { visitorName })
export const getChatMessages = (id) => request.get(`/chat/sessions/${id}/messages`)
export const rateSession = (id, rating) => request.post(`/sessions/${id}/rate`, { rating })

// 知识库
export const getKnowledge = (params) => request.get('/knowledge', { params })
export const createKnowledge = (data) => request.post('/knowledge', data)
export const updateKnowledge = (id, data) => request.put(`/knowledge/${id}`, data)
export const deleteKnowledge = (id) => request.delete(`/knowledge/${id}`)

// 分类
export const getCategories = () => request.get('/categories')
export const createCategory = (data) => request.post('/categories', data)
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/categories/${id}`)

// 客服
export const getAgents = (params) => request.get('/agents', { params })
export const createAgent = (data) => request.post('/agents', data)
export const updateAgent = (id, data) => request.put(`/agents/${id}`, data)
export const toggleAgent = (id) => request.put(`/agents/${id}/status`)
export const deleteAgent = (id) => request.delete(`/agents/${id}`)

// 会话
export const getSessions = (params) => request.get('/sessions', { params })
export const getSessionMessages = (id) => request.get(`/sessions/${id}/messages`)
export const takeSession = (id) => request.post(`/sessions/${id}/take`)
export const closeSession = (id) => request.post(`/sessions/${id}/close`)

// 统计
export const getOverview = () => request.get('/stats/overview')
export const getTrend = () => request.get('/stats/trend')
