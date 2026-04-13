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

## 项目状态 (2026-04-09)
- 7批移植完成，骨架代码全部完善
- AI适配器: OpenAiAdapter(完整) / MiniMaxAdapter(完整含轮询)
- TaskLog 实体+Mapper+Service 已创建
- TtsService 音频已持久化到文件系统
- VideoService 支持异步任务轮询(pollPendingTasks)
- AssetService 图片尺寸获取已实现(ImageIO)
- VideoComposeService getVideoInfo() ffprobe解析已完成
- JWT 依赖已引入但未使用（无认证过滤器）
- **前端UI框架**: 已切换到 **Ant Design Vue 4.x**（全局注册，暗色主题通过 ConfigProvider 配置）
  - App.vue 使用 a-config-provider + a-app
  - 所有页面组件已全面替换：a-input/a-modal/a-button/a-select/a-tag/a-popconfirm 等
  - 图标库: @ant-design/icons-vue（PlusOutlined/SaveOutlined/DeleteOutlined/EditOutlined 等）
  - 暗色主题 token: colorPrimary=#6366f1, colorBgContainer=#1a1a1a, colorBgLayout=#0f0f0f
  - 前端端口: 5173 / 后端端口: 8080
- **环境要求**: JDK 17+（系统当前只有JDK8，需升级）
- 前端响应式布局已完成，4个页面全面自适应
