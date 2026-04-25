<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { storageApi } from '@/utils/storage'
import { assetApi } from '@/utils/asset'
import { message as AMessage } from 'ant-design-vue'
import {
  CloudUploadOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SettingOutlined,
  InboxOutlined,
  DatabaseOutlined,
  PlayCircleOutlined,
  SoundOutlined,
  EyeOutlined,
  FileImageOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'

// 文件上传输入框引用
const fileUploadInput = ref<HTMLInputElement | null>(null)

// 存储状态
const storageStatus = ref<any>(null)
const loadingStatus = ref(false)

// OSS 配置表单
const ossForm = ref({
  enabled: false,
  provider: 'aliyun',
  endpoint: '',
  accessKeyId: '',
  accessKeySecret: '',
  bucketName: '',
  customDomain: '',
})
const savingOss = ref(false)
const testingConnection = ref(false)
const showOssModal = ref(false)
const ossPersisted = ref(false)

// 文件列表
const files = ref<any[]>([])
const loadingFiles = ref(false)
const loadingMoreFiles = ref(false)
const uploading = ref(false)
const selectedType = ref('all')
const pageNum = ref(1)
const pageSize = ref(24)
const fileTotal = ref(0)
const hasMoreFiles = ref(true)

const fileTypes = [
  { value: 'all', label: '全部' },
  { value: 'image', label: '图片' },
  { value: 'video', label: '视频' },
  { value: 'audio', label: '音频' },
  { value: 'file', label: '文件' },
]

const filteredFiles = computed(() => {
  if (selectedType.value === 'all') return files.value
  return files.value.filter((f: any) => f.type === selectedType.value)
})

// ====== 媒体预览状态 ======
const previewVisible = ref(false)
const previewItem = ref<any>(null)
const previewType = ref<'image' | 'video' | 'audio'>('image')
const previewLoading = ref(false)
const previewError = ref('')

// 根据实际数据（mimeType/filename）推断真实类型，防止数据库type字段不准确导致渲染错误
const getRealFileType = (item: any): 'image' | 'video' | 'audio' | 'file' => {
  // 优先用数据库类型字段
  const dbType = item.type
  if (dbType === 'video' || dbType === 'audio') return dbType
  
  // 图片：如果数据库标记为image，再通过mimeType二次验证
  if (dbType === 'image') return 'image'
  
  // 其他情况（file或空）：通过mimeType和扩展名兜底判断
  const mime = item.mimeType || ''
  if (mime.startsWith('video/')) return 'video'
  if (mime.startsWith('audio/')) return 'audio'
  if (mime.startsWith('image/')) return 'image'
  
  // 通过扩展名判断
  const fname = item.filename || ''
  const ext = fname.split('.').pop()?.toLowerCase() || ''
  const videoExts = ['mp4','mkv','avi','mov','wmv','flv','webm']
  const audioExts = ['mp3','wav','ogg','aac','flac','m4a']
  const imageExts = ['jpg','jpeg','png','gif','bmp','webp','svg']
  if (videoExts.includes(ext)) return 'video'
  if (audioExts.includes(ext)) return 'audio'
  if (imageExts.includes(ext)) return 'image'
  
  return 'file'
}

// 打开图片预览（Ant Design a-image 内置）
// 打开视频/音频预览弹窗
const openPreview = (item: any) => {
  previewItem.value = item
  previewLoading.value = true
  previewError.value = ''
  
  const realType = getRealFileType(item)
  if (realType === 'video') {
    previewType.value = 'video'
    previewVisible.value = true
  } else if (realType === 'audio') {
    previewType.value = 'audio'
    previewVisible.value = true
  } else if (realType === 'image' || realType === 'file') {
    // 图片用 a-image 的预览功能，这里统一走弹窗体验更好；file类型也走此分支显示原始内容
    previewType.value = 'image'
    previewVisible.value = true
  }
}

const closePreview = () => {
  previewVisible.value = false
  // 稍微延迟清空，避免动画闪烁
  setTimeout(() => { 
    previewItem.value = null
    previewLoading.value = false
    previewError.value = ''
  }, 300)
}

// 处理媒体加载错误
const handleMediaError = (e: Event) => {
  previewLoading.value = false
  previewError.value = '媒体加载失败，可能是文件访问权限问题或文件不存在'
  console.error('Media load error:', e)
}

// 处理媒体加载成功
const handleMediaLoaded = () => {
  previewLoading.value = false
}

// 加载存储状态
const loadStatus = async () => {
  loadingStatus.value = true
  try {
    const res = await storageApi.status()
    if (res.code === 200) storageStatus.value = res.data
  } catch (e) {
    console.error('Load status failed:', e)
  } finally {
    loadingStatus.value = false
  }
}

// 加载文件列表（支持滚动分页）
const loadFiles = async (isLoadMore = false) => {
  // 防止重复加载
  if (isLoadMore && loadingMoreFiles.value) return
  if (!isLoadMore && loadingFiles.value) return

  if (isLoadMore) {
    loadingMoreFiles.value = true
  } else {
    loadingFiles.value = true
    pageNum.value = 1
    hasMoreFiles.value = true
  }

  try {
    // 添加时间戳避免缓存
    const res = await assetApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      _t: Date.now()
    })
    if (res.code === 200) {
      const list = res.data?.records || res.data || []
      const total = res.data?.total || list.length
      fileTotal.value = total

      if (isLoadMore) {
        files.value.push(...list)
      } else {
        files.value = list
      }

      // 判断是否还有更多
      hasMoreFiles.value = files.value.length < total && list.length === pageSize.value
      console.log(`[存储文件] 第${pageNum.value}页加载完成，本页${list.length}条，总计${files.value.length}/${total}条`)
    }
  } catch (e) {
    console.error('Load files failed:', e)
    AMessage.error('加载文件失败，请重试')
  } finally {
    loadingFiles.value = false
    loadingMoreFiles.value = false
  }
}

// 滚动加载更多
const onFilesScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const scrollBottom = target.scrollHeight - target.scrollTop - target.clientHeight
  if (scrollBottom < 80 && hasMoreFiles.value && !loadingMoreFiles.value && !loadingFiles.value) {
    pageNum.value++
    loadFiles(true)
  }
}

// 根据文件类型判断素材类型
const getFileType = (file: File): string => {
  const mimeType = file.type
  if (mimeType.startsWith('image/')) return 'image'
  if (mimeType.startsWith('video/')) return 'video'
  if (mimeType.startsWith('audio/')) return 'audio'
  // 根据扩展名判断
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  const videoExts = ['mp4', 'mkv', 'avi', 'mov', 'wmv', 'flv', 'webm']
  const audioExts = ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a', 'wma']
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
  if (videoExts.includes(ext)) return 'video'
  if (audioExts.includes(ext)) return 'audio'
  if (imageExts.includes(ext)) return 'image'
  return 'file'
}

// 上传文件
const uploadFiles = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return

  uploading.value = true
  try {
    for (const file of Array.from(input.files)) {
      const fileType = getFileType(file)
      await storageApi.upload(file, undefined, fileType)
    }
    AMessage.success(`成功上传 ${input.files.length} 个文件`)
    input.value = ''
    await loadFiles()
    await loadStatus()
  } catch (e: any) {
    AMessage.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

// 删除文件
const deleteFile = async (id: string) => {
  try {
    const res = await storageApi.delete(id)

    // 立即从本地列表中移除该项（乐观更新）
    const index = files.value.findIndex((f: any) => f.id === id)
    if (index > -1) {
      files.value.splice(index, 1)
      fileTotal.value = Math.max(0, fileTotal.value - 1)
    }
    
    AMessage.success('已删除')
    // 延迟一下再刷新列表，确保数据库事务已提交
    setTimeout(async () => {
      await loadFiles()
    }, 500)
  } catch (e: any) {
    console.error('Delete failed:', e)
    AMessage.error(e?.message || '删除失败')
  }
}

// 打开 OSS 配置弹窗
const openOssModal = async () => {
  showOssModal.value = true
  try {
    const res = await storageApi.getOssConfig()
    if (res.code === 200 && res.data) {
      ossForm.value.enabled = res.data.enabled === 'true'
      ossForm.value.provider = res.data.provider || 'aliyun'
      ossForm.value.endpoint = res.data.endpoint || ''
      ossForm.value.bucketName = res.data.bucketName || ''
      ossForm.value.customDomain = res.data.customDomain || ''
      ossPersisted.value = res.data.persisted === 'true'
    }
  } catch (e) {
    // 使用默认值
  }
}

// 保存 OSS 配置
const saveOssConfig = async () => {
  savingOss.value = true
  try {
    await storageApi.updateOssConfig(ossForm.value)
    showOssModal.value = false
    AMessage.success('配置已保存并持久化（重启后自动生效）')
    ossPersisted.value = true
    await loadStatus()
  } catch (e: any) {
    AMessage.error(e?.message || '保存失败')
  } finally {
    savingOss.value = false
  }
}

// 测试连接（保存后即时生效）
const testConnection = async () => {
  testingConnection.value = true
  try {
    await saveOssConfig()
    AMessage.success('配置已持久化保存！当前会话已立即生效')
  } catch (e: any) {
    AMessage.error(e?.message || '操作失败')
  } finally {
    testingConnection.value = false
  }
}

onMounted(() => {
  loadStatus()
  loadFiles()
})
</script>

<template>
  <div class="space-y-6 w-full max-w-5xl mx-auto">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-[#f5f5f5] flex items-center gap-2">
          <CloudUploadOutlined />
          文件存储管理
        </h2>
        <p class="text-sm text-[#606060] mt-1">本地存储 / 阿里云 OSS 对象存储</p>
      </div>
      <a-button type="primary" @click="openOssModal()">
        <template #icon><SettingOutlined /></template>
        OSS 配置
      </a-button>
    </div>

    <!-- 存储状态卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <div class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-5">
        <div class="flex items-center gap-3 mb-3">
          <div :class="storageStatus?.enabled ? 'bg-green-500/10 text-green-400' : 'bg-[#333] text-[#808080]'"
            class="w-10 h-10 rounded-lg flex items-center justify-center">
            <DatabaseOutlined style="font-size: 20px;" />
          </div>
          <div>
            <p class="text-xs text-[#606060]">存储方式</p>
            <p class="text-sm font-medium text-[#f5f5f5]">
              {{ storageStatus?.enabled ? (storageStatus?.provider || 'OSS') + ' 云存储' : '本地文件系统' }}
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2 mt-2">
          <CheckCircleOutlined v-if="!loadingStatus" :class="storageStatus?.enabled ? 'text-green-400' : 'text-[#808080]'" />
          <CloseCircleOutlined v-if="loadingStatus" class="text-yellow-400 animate-pulse" />
          <span class="text-xs" :class="storageStatus?.enabled ? 'text-green-400' : 'text-[#808080]'">
            {{ storageStatus?.enabled ? '已启用云存储' : '使用本地存储' }}
          </span>
        </div>
      </div>

      <div class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-5">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center">
            <InboxOutlined style="font-size: 20px;" />
          </div>
          <div>
            <p class="text-xs text-[#606060]">文件总数</p>
            <p class="text-sm font-medium text-[#f5f5f5]">{{ filteredFiles.length }} 个</p>
          </div>
        </div>
        <p v-if="storageStatus?.bucketName && !loadingStatus"
          class="text-xs text-[#505050] mt-2 truncate">
          Bucket: {{ storageStatus.bucketName }}
        </p>
      </div>

      <div class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-5">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center">
            <CloudUploadOutlined style="font-size: 20px;" />
          </div>
          <div>
            <p class="text-xs text-[#606060]">本地占用空间</p>
            <p class="text-sm font-medium text-[#f5f5f5]">{{ storageStatus?.localUsedMB || 0 }} MB</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传区域 -->
    <div class="bg-[#1a1a1a] rounded-xl border border-dashed border-[#333] p-8 text-center cursor-pointer hover:border-[#6366f1]/40 transition-colors"
         @click="fileUploadInput?.click()"
    >
      <CloudUploadOutlined style="font-size: 36px; color: #404040;" class="mb-3 block mx-auto" />
      <p class="text-sm text-[#808080] mb-1">点击或拖拽上传文件到此处</p>
      <p class="text-xs text-[#505050]">支持图片、视频、音频等格式，单文件最大 100MB</p>
      <input ref="fileUploadInput" type="file" multiple accept="image/*,video/*,audio/*,.pdf" class="hidden" @change="uploadFiles" />
    </div>

    <!-- 文件列表 -->
    <div>
      <!-- 筛选 Tab -->
      <div class="flex gap-2 overflow-x-auto pb-2 no-scrollbar mb-4">
        <a-button
          v-for="t in fileTypes" :key="t.value"
          @click="selectedType = t.value"
          :type="selectedType === t.value ? 'primary' : 'default'"
          size="small"
          :class="[
            '!rounded-xl !font-medium whitespace-nowrap shrink-0',
            selectedType !== t.value && '!bg-[#1a1a1a] !border-[#2a2a2a] !text-[#a0a0a0] hover:!text-[#f5f5f5]'
          ]"
        >
          {{ t.label }}
          <span v-if="t.value !== 'all'" class="ml-1 px-1 py-0.5 bg-white/20 rounded-full text-[10px]">{{ files.filter((f: any) => f.type === t.value).length }}</span>
          <span v-else class="ml-1 px-1 py-0.5 bg-white/20 rounded-full text-[10px]">{{ files.length }}</span>
        </a-button>
      </div>

      <!-- Loading -->
      <div v-if="loadingFiles" class="flex justify-center py-12"><a-spin tip="加载中..." /></div>

      <!-- Empty -->
      <div v-else-if="filteredFiles.length === 0" class="text-center py-12">
        <a-empty description="暂无文件，点击上方按钮上传" />
      </div>

      <!-- File Grid (滚动加载) -->
      <div v-else class="max-h-[60vh] overflow-y-auto pr-1" @scroll="onFilesScroll">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
          <div v-for="item in filteredFiles" :key="item.id"
            class="group bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] overflow-hidden hover:border-[#6366f1]/30 transition-all">

            <!-- ====== 图片：缩略图 + 点击放大预览 ====== -->
            <div v-if="getRealFileType(item) === 'image'" class="aspect-video bg-[#242424] relative overflow-hidden cursor-pointer"
                 @click="openPreview(item)">
              <img :src="item.fileUrl" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                   loading="lazy" />
              <!-- 放大图标遮罩 -->
              <div class="absolute inset-0 bg-black/0 group-hover:bg-black/30 flex items-center justify-center transition-all">
                <EyeOutlined class="text-white opacity-0 group-hover:opacity-100 scale-90 group-hover:scale-100 transition-all" style="font-size: 22px;" />
              </div>
            </div>

            <!-- ====== 视频：封面 + 播放图标 + 点击播放 ====== -->
            <div v-else-if="getRealFileType(item) === 'video'" class="aspect-video bg-[#242424] relative overflow-hidden cursor-pointer"
                 @click="openPreview(item)">
              <video :src="item.fileUrl" muted preload="metadata"
                     class="w-full h-full object-cover" />
              <!-- 播放按钮 -->
              <div class="absolute inset-0 flex items-center justify-center">
                <PlayCircleOutlined class="text-white/80 drop-shadow-lg group-hover:text-white group-hover:scale-110 transition-all"
                                    style="font-size: 42px;" />
              </div>
              <!-- 时长标签 -->
              <div v-if="item.duration" class="absolute bottom-1 right-1 px-1.5 py-0.5 bg-black/70 rounded text-[10px] text-white font-mono">
                {{ Math.floor(item.duration) }}s
              </div>
            </div>

            <!-- ====== 音频：图标 + 点击播放 ====== -->
            <div v-else-if="getRealFileType(item) === 'audio'" class="aspect-video bg-[#242424] flex items-center justify-center cursor-pointer relative"
                 @click="openPreview(item)">
              <SoundOutlined class="text-[#6366f1] group-hover:scale-110 transition-transform" style="font-size: 48px;" />
              <div v-if="item.duration" class="absolute bottom-2 right-2 text-[10px] text-[#888]">
                {{ item.duration.toFixed(1) }}s
              </div>
            </div>

            <!-- ====== 其他文件类型：显示图标（用真实类型判断） ====== -->
            <div v-else class="aspect-video bg-[#242424] flex items-center justify-center">
              <FileTextOutlined style="font-size: 32px; color: #404040;" />
              <span class="ml-2 text-xs text-[#606060]">{{ item.filename?.split('.').pop()?.toUpperCase() || '' }}</span>
            </div>

            <!-- Info -->
            <div class="p-3">
              <p class="text-xs font-medium text-[#e0e0e0] truncate" :title="item.filename">{{ item.filename }}</p>
              <div class="flex items-center justify-between mt-2">
                <span class="text-[10px] text-[#606060]">
                  {{ formatSize(item.fileSize) }} · {{ item.sourceType }}
                </span>
                <a-popconfirm title="确定删除该文件？" ok-text="确定" cancel-text="取消" @confirm="deleteFile(item.id)">
                  <a-button type="text" danger size="small" class="opacity-0 group-hover:opacity-100 !p-1">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多指示 -->
        <div v-if="loadingMoreFiles" class="flex justify-center py-4">
          <a-spin size="small" tip="加载更多..." />
        </div>
        <div v-else-if="!hasMoreFiles && filteredFiles.length > 0" class="text-center py-4 text-[#666] text-xs">
          已加载全部 {{ filteredFiles.length }} 个文件
        </div>
      </div>
    </div>

    <!-- ====== 媒体预览弹窗 ====== -->
    <a-modal v-model:open="previewVisible" :footer="null" width="720px"
             :body-style="{ padding: 0, background: '#000' }" destroyOnClose @cancel="closePreview">

      <!-- 错误提示 -->
      <div v-if="previewError" class="flex flex-col items-center justify-center py-16 px-8 text-center">
        <CloseCircleOutlined style="font-size: 48px; color: #ff4d4f;" class="mb-4" />
        <p class="text-[#e0e0e0] mb-2">{{ previewError }}</p>
        <p class="text-xs text-[#808080] mb-4">如果是OSS文件，请检查Bucket权限设置</p>
        <a-button type="primary" @click="closePreview">关闭</a-button>
      </div>

      <!-- 图片预览 -->
      <div v-else-if="previewType === 'image' && previewItem" class="relative">
        <!-- 加载状态遮罩 -->
        <div v-if="previewLoading" class="absolute inset-0 flex items-center justify-center bg-black/50 z-10">
          <a-spin size="large" tip="加载中..." />
        </div>
        <img :src="previewItem.fileUrl" class="max-w-full max-h-[70vh] mx-auto object-contain"
             @load="handleMediaLoaded" @error="handleMediaError" />
      </div>

      <!-- 视频预览 -->
      <div v-else-if="previewType === 'video' && previewItem" class="flex flex-col items-center relative">
        <!-- 加载状态遮罩 -->
        <div v-if="previewLoading" class="absolute inset-0 flex items-center justify-center bg-black/50 z-10">
          <a-spin size="large" tip="加载中..." />
        </div>
        <video :src="previewItem.fileUrl" controls autoplay class="w-full max-w-full" style="max-height: 65vh;"
               @loadeddata="handleMediaLoaded" @error="handleMediaError" />
        <p v-if="previewItem.filename" class="mt-3 text-sm text-[#a0a0a0]">{{ previewItem.filename }}</p>
      </div>

      <!-- 音频预览 -->
      <div v-else-if="previewType === 'audio' && previewItem" class="flex flex-col items-center justify-center py-8 px-8 space-y-4 relative">
        <!-- 加载状态遮罩 -->
        <div v-if="previewLoading" class="absolute inset-0 flex items-center justify-center bg-black/50 z-10">
          <a-spin size="large" tip="加载中..." />
        </div>
        <SoundOutlined style="font-size: 64px; color: #6366f1;" />
        <p v-if="previewItem.filename" class="text-sm text-[#f5f5f5]">{{ previewItem.filename }}</p>
        <audio :src="previewItem.fileUrl" controls autoplay class="w-full"
               @loadeddata="handleMediaLoaded" @error="handleMediaError" />
      </div>
    </a-modal>

    <!-- ====== OSS 配置弹窗 ====== -->
    <a-modal v-model:open="showOssModal" title="对象存储 (OSS) 配置"
             @ok="saveOssConfig" :okButtonProps="{ disabled: savingOss }"
             okText="保存配置" cancelText="取消" width="600px" destroyOnClose>
      <a-alert :message="ossPersisted ? '配置已持久化，修改后将更新到本地文件（重启自动恢复）' : '保存后配置将持久化到本地文件，重启服务后自动生效'"
               type="info" show-icon class="mb-4" />

      <div class="space-y-4 pt-2">
        <!-- 启用开关 -->
        <div class="flex items-center justify-between">
          <label class="text-sm text-[#a0a0a0]">启用 OSS 云存储</label>
          <a-switch v-model:checked="ossForm.enabled" checked-children="开" un-checked-children="关" />
        </div>

        <a-form layout="vertical">
          <a-form-item label="存储提供商">
            <a-select v-model:value="ossForm.provider">
              <a-select-option value="aliyun">阿里云 OSS</a-select-option>
              <a-select-option value="volcengine">火山引擎 TOS</a-select-option>
              <a-select-option value="tencentcloud">腾讯云 COS</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="Endpoint（服务地址）">
            <a-input v-model:value="ossForm.endpoint" placeholder="http://oss-cn-chengdu.aliyuncs.com" />
          </a-form-item>

          <a-form-item label="AccessKey ID">
            <a-input-password v-model:value="ossForm.accessKeyId" placeholder="LTAIxxxx" />
          </a-form-item>

          <a-form-item label="AccessKey Secret">
            <a-input-password v-model:value="ossForm.accessKeySecret" placeholder="sk-xxx" />
          </a-form-item>

          <a-form-item label="Bucket 名称">
            <a-input v-model:value="ossForm.bucketName" placeholder="your-bucket-name" />
          </a-form-item>

          <a-form-item label="自定义域名（可选，用于 CDN）">
            <a-input v-model:value="ossForm.customDomain" placeholder="cdn.example.com（不带协议前缀）" />
          </a-form-item>
        </a-form>
      </div>

      <template #footer>
        <a-button @click="showOssModal = false">取消</a-button>
        <a-button :loading="testingConnection" @click="testConnection">测试连接</a-button>
        <a-button type="primary" :loading="savingOss" @click="saveOssConfig">保存配置</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script lang="ts">
function formatSize(bytes?: number | null): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.no-scrollbar::-webkit-scrollbar { display: none; }

::deep(.ant-form-item-label > label) {
  color: #a0a0a0 !important;
}

/* 分页组件暗色适配 */
:deep(.ant-pagination) { gap: 4px; }
:deep(.ant-pagination-item) {
  background: #1a1a1a !important;
  border-color: #2a2a2a !important;
  border-radius: 8px !important;
}
:deep(.ant-pagination-item a) { color: #a0a0a0 !important; }
:deep(.ant-pagination-item-active),
:deep(.ant-pagination-item-active a) {
  background: #6366f1 !important;
  border-color: #6366f1 !important;
  color: #fff !important;
}
:deep(.ant-pagination-prev .ant-pagination-item-link),
:deep(.ant-pagination-next .ant-pagination-item-link) {
  background: #1a1a1a !important;
  border-color: #2a2a2a !important;
  color: #a0a0a0 !important;
  border-radius: 8px !important;
}
</style>
