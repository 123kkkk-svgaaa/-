import api from './index'

export function getPollList(pageNum = 1, pageSize = 10) {
  return api.get('/polls', { params: { pageNum, pageSize } })
}

export function getPollDetail(id) {
  return api.get(`/polls/${id}`)
}

export function syncPoll(pollId, txHash) {
  return api.post('/polls/sync', null, { params: { pollId, txHash } })
}

export function verifyPoll(id) {
  return api.get(`/polls/${id}/verify`)
}
