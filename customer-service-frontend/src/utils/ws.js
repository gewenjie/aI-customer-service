/**
 * 建立聊天 WebSocket 连接
 * @param {Object} opts
 * @param {number|string} opts.sessionId 会话ID
 * @param {string} [opts.role='CUSTOMER'] CUSTOMER | AGENT
 * @param {string} [opts.token=''] 客服令牌（AGENT 必填）
 */
export function createChatSocket({ sessionId, role = 'CUSTOMER', token = '' }) {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  let url = `${proto}://${location.host}/ws/chat?sessionId=${sessionId}&role=${role}`
  if (token) {
    url += `&token=${encodeURIComponent(token)}`
  }
  return new WebSocket(url)
}
