# 长期记忆 - 臻灵短剧项目

## 项目基本信息
- **名称**: 臻灵短剧 (Zhenling Drama) - AI 短剧自动化生产平台
- **路径**: c:/Data/Projects/drama-project
- **技术栈**: Vue3 + Vite + ofetch + Tailwind CSS | Java 17 + SpringBoot 3.2.0 + MyBatis Plus 3.5.5 | MySQL 8.0 | Redis | FFmpeg
- **端口**: 前端 5173 / 后端 8080
- **数据库**: zhenling_drama, 9张表 (dramas/characters/scenes/storyboards/videos/audios/ai_configs/assets/task_logs)

## 用户偏好
- 沟通语言: 中文
- UI风格: 极简、高级灰、低饱和、卡片悬浮、大圆角、轻阴影、柔和动画（暗色系 #0f0f0f 背景）
- 工作模式: Vibe Coding - 用户只负责验收效果与 vibe，龙虾全权负责一切
- 文档要求: 5份固定文档（operation-manual/product-manual/update-plan/test-report/daily-record）
- Git规范: feat/fix/docs/refactor 前缀
- 核心铁律: 不反问、不质疑、不拖延、不废话；永远输出完整可运行代码

## 项目状态 (2026-04-16)
- 7批移植完成，骨架代码全部完善
- AI适配器: OpenAiAdapter(完整) / MiniMaxAdapter(完整含轮询)
- TaskLog 实体+Mapper+Service 已创建
- TtsService 音频已持久化到文件系统
- VideoService 支持异步任务轮询(pollPendingTasks)
- AssetService 图片尺寸获取已实现(ImageIO)
- VideoComposeService getVideoInfo() ffprobe解析已完成
- JWT 依赖已引入但未使用（无认证过滤器）
- **前端UI框架**: 已切换到 **Ant Design Vue 4.x**（全局注册，暗色主题通过 ConfigProvider 配置）
  - App.vue 使用 a-config-provider + a-app + 全局按钮icon对齐CSS
  - 所有页面组件已全面替换：a-input/a-modal/a-button/a-select/a-tag/a-popconfirm 等
  - 图标库: @ant-design/icons-vue（PlusOutlined/SaveOutlined/DeleteOutlined/EditOutlined 等）
  - 暗色主题 token: colorPrimary=#6366f1, colorBgContainer=#1a1a1a, colorBgLayout=#0f0f0f
  - 前端端口: 5173 / 后端端口: 8080
- **环境要求**: JDK 17+（系统当前只有JDK8，需升级）
- 前端响应式布局已完成，4个页面全面自适应

## 已完成的 BUG 修复 (截至 2026-04-14, 共13个)
| 编号 | 问题 | 状态 |
|------|------|------|
| BUG00001~08 | 基础功能(404/空页面/静默错误) | ✅ |
| BUG00009 | /drama/new 路由复用空白 | ✅ watch(route.params.id) |
| BUG00010 | 按钮icon+文字纵向对齐 | ✅ 全局 .ant-btn flex CSS |
| **BUG00011** | OSS配置不持久化 + 无媒体预览 | ✅ OssConfigPersistence + 预览弹窗 |
| **BUG00012** | 图片URL输入不友好 | ✅ 选择器弹窗 + 上传自动填入 |
| **BUG00013** | AI配置缺Token单价字段 | ✅ DB DECIMAL(10,6) + BigDecimal |
| **BUG00014** | AI配置列表只显示enabled记录 | ✅ 新增listAll() + 前端改单次请求 |
| **BUG00011(2)** | storage.ts双重baseURL + 缺下载端点 | ✅ 移除冗余前缀 + AssetController新增download |
| **BUG00011(3)** | OSS上传ACL私有+删除TODO | ✅ setObjectAcl(PublicRead) + client.deleteObject() |
| **BUG00015** | ai.ts/media.ts 中 ofetch is not defined | ✅ 改用 import api from './request' |
| **BUG00016** | RestTemplate 不走系统代理 → UnknownHostException | ✅ 新增 RestTemplateConfig + application-dev.yml 代理配置(127.0.0.1:8888) + Adapter 注入 Bean |

## 架构要点
- **OSS持久化方案**: `OssConfigPersistence.java` → `./data/oss-config.json`，@PostConstruct 启动恢复
- **图片选择器模式**: DramaDetail.vue 角色/场景共用 imagePicker（assetApi.page 加载素材库 + storageApi.upload 上传）
- **SQL迁移文件**: migration-v2.sql (4张表) / migration-v3-token-price.sql (token_price字段) / migration-v4-minimax-configs.sql (MiniMax 4条配置)
- **MiniMax API**: BaseURL=https://api.minimaxi.com/v1，文本OpenAI兼容/chat/completions，图片/image_generation，视频/video_generation+查询，TTS/t2a_v2
- **RestTemplate 代理配置**: `RestTemplateConfig.java` → `OkHttp3ClientHttpRequestFactory` + 自定义 Dns（Google 8.8.8.8）；dev 环境 `http.proxy.enabled=false / dns.enabled=true`
- **MiniMax 模型名铁律**: 只支持 `MiniMax-M2.5`（文本）/ `image-01`（图片）/ `MiniMax-Hailuo-2.3`（视频）/ `speech-02-hd`（TTS），**禁止传 OpenAI 模型名**（gpt-4o/dall-e-3）—— `StoryboardService` 和 `ImageGenerationService` 均已改为传 `null` 让适配器用 DB 配置
