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

## 项目状态 (2026-04-22)
- **UPDATE.md 代码审查完成**: 33项优化任务（5 P0安全 + 8 P1功能 + 9 P2性能UX + 11 P3代码质量）
- **已修复 22/33 项**: 第一批10项(SEC/PERF/FUNC) + BUG00030错误透传 + 第三批4项(P2配置/质量) + 第四批2项(UX分页+布局) + R8三项(BUG00032滚动+BUG00036-A/D) + R9三项(BUG00037音画错位三重修复)
- **FFmpeg路径策略更新**: application.yml 留空走系统PATH，Windows路径移到 application-dev.yml
- **静态资源安全**: 新增 PathTraversalProtectionFilter 防路径遍历攻击
- **前端日志清理**: 9处 console.log 全部改为 `import.meta.env.DEV` 条件限定
- **定时器加固**: MediaStudio/Workbench onMounted 加防重复挂载保护
- **✅ 视频合成增强完成 (FEAT-002~004)**: AudioSyncEngine(4种对齐策略) + SubtitleRenderer(ASS字幕) + concatWithTransition(xfade转场)
  - 新增: AudioSyncEngine.java, SubtitleRenderer.java
  - 改造: VideoComposeService(probeAudio+xfade), ComposeService(智能合成集成), MediaStudio(对齐详情UI), application.yml(subtitle.*配置)
- JWT 依赖已引入但未使用（无认证过滤器）
- **前端UI框架**: 已切换到 **Ant Design Vue 4.x**（全局注册，暗色主题通过 ConfigProvider 配置）
  - App.vue 使用 a-config-provider + a-app + 全局按钮icon对齐CSS
  - 所有页面组件已全面替换：a-input/a-modal/a-button/a-select/a-tag/a-popconfirm 等
  - 图标库: @ant-design/icons-vue（PlusOutlined/SaveOutlined/DeleteOutlined/EditOutlined 等）
  - 暗色主题 token: colorPrimary=#6366f1, colorBgContainer=#1a1a1a, colorBgLayout=#0f0f0f
  - 前端端口: 5173 / 后端端口: 8080
- **环境要求**: JDK 17+（系统当前只有JDK8，需升级）
- 前端响应式布局已完成，4个页面全面自适应
- **Workbench 新增第4个Tab「视频生成」** (2026-04-20)：图片URL输入+模型选择+视频列表+在线播放预览+5秒轮询
- **后端新增合成API** (2026-04-20)：`ComposeController.java`(5端点) + `ComposeService.java`(业务编排)，路径 `/api/v1/compose/*`
- **MediaStudio 合成Tab已对接真实API**：FFmpeg状态检测→集数选择→整集拼接→历史记录
- **DramaDetail 分镜区新增「媒体工作室」按钮**：直达 /media/:dramaId
- **新增前端API文件**：`frontend/src/utils/compose.ts`（composeApi 5方法）

## 已完成的 BUG 修复 (截至 2026-04-20, 共17个)
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

## 新增BUG修复 (2026-04-20)
| BUG00023 | TTS试听路由冲突(/preview被/{id}吞) + 分镜选择器混乱 | ✅ /tts-preview + 音色16个 + 联动 |
| BUG00024 | TTS音频未归档OSS(24h临时URL过期) | ✅ saveAudioFile()改造: URL下载→uploadBytes归档 |
| BUG00025 | FFmpeg路径找不到 + 视频结果未存OSS | ✅ FfmpegConfig可配置 + VideoService/Compose归档OSS |

## 关键架构决策 (2026-04-20)
- **AI文件归档模式**: 所有AI生成的临时URL(图片/音频/视频)必须通过 FileStorageService.uploadBytes() 归档到用户配置的存储，避免24h过期
- **FFmpeg配置化**: application.yml → ffmpeg.path 可指定完整路径(如 C:/tools/ffmpeg/bin/ffmpeg.exe)，留空则走系统PATH
- **MediaStudio是路由组件**: dramaId 从 route.params.dramaId 获取，不用 defineProps
- **MiniMax视频返回file_id而非URL**: 视频生成任务完成后，查询接口返回的是 `file_id`（纯数字ID如 "389685784670407"），不是直接的 download_url。必须调用 `GET /v1/files/retrieve?file_id=xxx` 解析为真实下载链接。MiniMaxAdapter.fetchFileDownloadUrl() + pollVideoStatus() 已实现此链路
- **整集合成排序铁律**: composeEpisode() 必须按 Storyboard.shotNumber 排序拼接镜头，不能按 Video.storyboardId(UUID字符串)。流程：查分镜(shotNumber升序) → 逐镜头composeShot(视频+音频+字幕合并) → concatVideos拼接整集
- **FFmpeg远程URL模式**: composeShot()/getVideoInfo() 不直接传HTTPS URL给FFmpeg（会触发协议白名单限制），而是先 downloadVideoToTemp() 下载到本地临时文件，FFmpeg操作完成后清理临时文件
- **合成文件预览路径**: ComposeController /compose/download/** 端点提供本地合成文件的HTTP访问；composeEpisode() 归档失败时降级为此路径
- **视频prompt铁律**: 前端选择分镜时必须智能组合角色+场景+动作/台词+情绪构建丰富提示词，避免空prompt导致MiniMax生成随机画面
- **分镜提示词构建优先级**: action > description > dialogue(加角色名前缀)，同时拼接 characterName + sceneName + mood
- **checkQueryParams action覆盖漏洞**: URL参数 ?action=xxx 不得直接覆盖 videoPrompt（会跳过智能构建），仅作无分镜ID时的降级值
- **后端prompt传递链完整**: MediaStudio→VideoController(RequestBody)→VideoService→AiServiceFactory→MiniMaxAdapter，全程透传无丢参
- **MiniMax视频API规范铁律(2026-04-20对齐)**:
  - **resolution 必填**: `"1080P"`，缺省会导致API报错或低分辨率
  - **图生视频参数名**: `first_frame_image`（不是 reference_image_url！后者不存在）
  - **主体参考结构**: `subject_reference: [{type:"character", image:["url"]}]`（不是字符串！）
  - **模型按模式自动选型**: 文生/图生=Hailuo-2.3, 首尾帧=Hailuo-02, 主体参考=S2V-01
  - **prompt支持运镜指令**: 可加 `[镜头缓慢推进]` 等指令控制镜头运动
- **音频下载校验铁律(2026-04-22 R5)**: downloadVideoToTemp() 下载远程音频后，必须用 isValidAudioFile() 校验魔数(MP3 ID3/MPEG-sync/AAC ftyp/OGG/WAV/FLAC/WebM)，无效则降级 VIDEO_ONLY 策略；否则 FFmpeg mp3float 解码器报 Header missing 崩溃（退出码-22）
- **FFmpeg字幕终极方案(R6)**: Windows下永远不传绝对路径给subtitles滤镜！改为复制ASS到输出目录→传纯文件名(sub_xxx.ass)→设置ProcessBuilder.directory(输出目录)。R4的冒号转义方案被证明无效（FFmpeg解析后吃掉盘符C）
- **FFmpeg致命错误静默吞掉(BUG00037-Fix1)**: executeFf() 必须检测 stderr 中的 Header missing/Invalid data/Error submitting packet 关键词，即使 exitCode=0 也要抛异常！否则产出空音频文件→拼接后该镜头静音→听起来像"上一镜声音跑到下一镜"
- **DIRECT_BLEND -shortest截断陷阱(BUG00037-Fix2)**: 策略选择时，**audio>video 绝对不能走 DIRECT_BLEND**（-shortest 取较短者=视频长度，音频尾部被无声截断）。正确做法：audio>video → EXTEND_FREEZE(冻结帧延展)；video≥audio → DIRECT_BLEND(安全)
- **mp3float Header missing真相当WARNING(BUG00037-R10)**: MiniMax TTS生成的MP3缺少ID3头→FFmpeg mp3float报Header missing但实际能解码出有效音频(如audio:57KiB time=4.08s)！**不能当fatal error处理**。根治：Phase0.5预转码为AAC(44.1kHz/stereo/128k)，和createSilencePad格式一致，后续所有步骤用AAC输入彻底消灭兼容性问题
- **音频中间格式铁律(R10-Fix1b)**: **所有中间音频文件必须统一格式！** transcodeToAac和createSilencePad都输出AAC(44.1kHz/stereo/128k)，确保concatTwoAudios的-f concat拼接不会出错。WAV(24kHz/mono)+AAC(44.1kHz/stereo)混搭会导致输出时长异常(4669s vs 5.9s)
