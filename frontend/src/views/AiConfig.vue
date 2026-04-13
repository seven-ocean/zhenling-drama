<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { aiConfigApi } from '@/utils/aiConfig'
import { message as AMessage, Modal as AModal } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue'

const configs = ref<any[]>([])
const loading = ref(false)
const showModal = ref(false)
const editing = ref<any>(null)

const providers = [
  { value: 'openai', label: 'OpenAI' },
  { value: 'minimax', label: 'MiniMax' },
  { value: 'gemini', label: 'Google Gemini' },
  { value: 'ali', label: '阿里云' },
  { value: 'volcengine', label: '火山引擎' },
]

const apiTypes = [
  { value: 'text', label: '文本生成' },
  { value: 'image', label: '图片生成' },
  { value: 'video', label: '视频生成' },
  { value: 'tts', label: '语音合成' },
]

const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await aiConfigApi.listByType('image')
    if (res.code === 200) {
      configs.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openModal = (item?: any) => {
  editing.value = item ? { ...item } : { provider: 'openai', apiType: 'image', priority: 0, enabled: true }
  showModal.value = true
}

const save = async () => {
  loading.value = true
  try {
    if (editing.value.id) {
      await aiConfigApi.update(editing.value.id, editing.value)
    } else {
      await aiConfigApi.create(editing.value)
    }
    showModal.value = false
    AMessage.success('保存成功')
    await loadConfigs()
  } catch (e) {
    AMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

const toggle = async (item: any) => {
  try {
    await aiConfigApi.toggle(item.id, !item.enabled)
    AMessage.success(item.enabled ? '已禁用' : '已启用')
    await loadConfigs()
  } catch (e) {
    AMessage.error('操作失败')
  }
}

const remove = (id: string) => {
  AModal.confirm({
    title: '确定删除？',
    content: '删除后不可恢复',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await aiConfigApi.delete(id)
        AMessage.success('已删除')
        await loadConfigs()
      } catch (e) {
        AMessage.error('删除失败')
      }
    },
  })
}

onMounted(() => {
  loadConfigs()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <h3 class="text-lg font-medium text-[#f5f5f5]">AI 服务配置</h3>
      <a-button type="primary" @click="openModal()">
        <template #icon><PlusOutlined /></template>
        添加配置
      </a-button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <a-spin tip="加载中..." />
    </div>

    <!-- Empty -->
    <div v-else-if="configs.length === 0" class="text-center py-12 text-[#606060]">
      <a-empty description="暂无 AI 配置，点击上方添加">
        <template #extra>
          <a-button type="primary" @click="openModal()">
            <template #icon><PlusOutlined /></template>
            添加配置
          </a-button>
        </template>
      </a-empty>
    </div>

    <!-- Config List -->
    <div v-else class="space-y-3">
      <div v-for="item in configs" :key="item.id"
        class="flex items-center justify-between p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] hover:border-[#444] transition-colors">
        <div class="flex items-center gap-4 flex-1 min-w-0">
          <a-tag :color="item.enabled ? '#16a34a15' : '#52525b'" class="!font-medium shrink-0">
            <CheckCircleOutlined v-if="item.enabled" style="margin-right: 4px;" />
            <CloseCircleOutlined v-else style="margin-right: 4px;" />
            {{ item.enabled ? '启用' : '禁用' }}
          </a-tag>
          <span class="text-[#f5f5f5] font-medium">{{ providers.find(p => p.value === item.provider)?.label }}</span>
          <span class="text-[#606060]">|</span>
          <span class="text-[#a0a0a0]">{{ apiTypes.find(t => t.value === item.apiType)?.label }}</span>
          <span class="text-[#606060]">|</span>
          <span class="text-[#808080] text-sm truncate">{{ item.model }}</span>
        </div>
        <div class="flex items-center gap-1 ml-3 shrink-0">
          <a-tooltip :title="item.enabled ? '禁用' : '启用'">
            <a-button type="text" size="small" @click="toggle(item)">
              <template #icon>
                <CloseCircleOutlined v-if="item.enabled" />
                <CheckCircleOutlined v-else />
              </template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="编辑">
            <a-button type="text" size="small" @click="openModal(item)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-popconfirm title="确定删除该配置？" ok-text="确定" cancel-text="取消" @confirm="remove(item.id)">
            <a-button type="text" danger size="small">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-popconfirm>
        </div>
      </div>
    </div>

    <!-- ====== 添加/编辑 Modal ====== -->
    <a-modal
      v-model:open="showModal"
      :title="editing?.id ? '编辑配置' : '添加配置'"
      @ok="save"
      :confirmLoading="loading"
      okText="保存"
      cancelText="取消"
      width="520px"
      destroyOnClose
    >
      <div class="space-y-4 pt-2">
        <a-form layout="vertical">
          <a-form-item label="厂商">
            <a-select v-model:value="editing.provider">
              <a-select-option v-for="p in providers" :key="p.value" :value="p.value">{{ p.label }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="API类型">
            <a-select v-model:value="editing.apiType">
              <a-select-option v-for="t in apiTypes" :key="t.value" :value="t.value">{{ t.label }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="Base URL">
            <a-input v-model:value="editing.baseUrl" placeholder="https://api.openai.com/v1" />
          </a-form-item>
          <a-form-item label="API Key">
            <a-input-password v-model:value="editing.apiKey" placeholder="sk-xxx" />
          </a-form-item>
          <a-form-item label="模型名称">
            <a-input v-model:value="editing.model" placeholder="gpt-4o" />
          </a-form-item>
          <a-form-item label="优先级">
            <a-input-number v-model:value="editing.priority" :min="0" class="w-full" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
/* 暗色主题下覆盖 Ant Design 表单标签颜色 */
:deep(.ant-form-item-label > label) {
  color: #a0a0a0 !important;
}
</style>
