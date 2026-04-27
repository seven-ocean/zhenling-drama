import ofetch from './request'

const baseURL = '/api/v1'

export const taskApi = {
  // 分页查询任务
  list: (params?: any) => ofetch(`${baseURL}/task-logs`, { baseURL, params }),

  // 查询剧集任务列表
  listByDramaId: (dramaId: string) => ofetch(`${baseURL}/task-logs/drama/${dramaId}`, { baseURL }),

  // 获取正在运行的任务（轮询用）
  running: () => ofetch(`${baseURL}/task-logs/running`, { baseURL }),

  // 获取单个任务详情
  get: (id: string) => ofetch(`${baseURL}/task-logs/${id}`, { baseURL }),

  // 删除任务记录
  delete: (id: string) => ofetch(`${baseURL}/task-logs/${id}/delete`, { baseURL, method: 'POST' }),
}
