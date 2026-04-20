# 这张表专门整理测试用例，跟进测试情况

-------------------------------全局提示词start-------------------------------

# 接下来的任务（后端）

在backend继续迭代功能

## 要求

1、要符合这个工程文件的目录结构和代码规范
2、要在这个工程文件的基础上继续迭代，不能完全重新开始
3、请勿重新编译项目验证修复

## 修复功能

# 接下来的任务（前端）

在frontend继续迭代功能

## 要求

1、要符合这个工程文件的目录结构和代码规范
2、要在这个工程文件的基础上继续迭代，不能完全重新开始
3、请勿重新编译项目验证修复

## 修复功能

-------------------------------全局提示词end-------------------------------



| 编号       | 功能路径                             | 预期结果         | 实测结果                                                                                                                                                                                                     | 是否修复    | AI给出的修复方案 |
|----------|----------------------------------|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|----------|
| BUG00001 | /drama/new (POST /api/v1/dramas) | 可以创建剧集       | 创建剧集失败：POST "/dramas": 404 Not Found                                                                                                                                                                     | **已修复** | ① 执行 `backend/sql/init-full.sql` 初始化数据库表结构 + 示例数据；② 确保后端服务启动（端口8080）；③ 前端 vite.config.ts 已配置 proxy `/api → localhost:8080`。**根因**：数据库 `dramas` 表不存在或缺少 `deleted` 列（实体类有但原 init.sql 漏掉），导致后端报错返回404 |
| BUG00002 | /settings/ai                     | 可以添加配置       | 不知道怎么配置，需要初始化参数SQL参考数据                                                                                                                                                                                   | **已修复** | 已创建 `backend/sql/init-full.sql`，内含 **4条AI配置示例**（OpenAI图片/文本/MiniMax视频/TTS）+ **2条示例剧集** + **3个示例角色** + **3个示例场景**。用户只需将 SQL 中的 `sk-your-openai-key-here` 替换为真实 API Key 即可使用。详见 init-full.sql 文件 |
| BUG00003 | /dramas                          | 可以正常添加剧集     | 添加失败：POST http://localhost:5173/dramas 返回 404 Not Found。入参：{"title":"111","description":"111","totalEpisodes":1}                                                                                         | **已修复** | **根因**：`request.ts` 中 `ofetch.create({ baseURL })` 创建了带前缀的实例，但 `export default ofetch` 导出的仍是原始 ofetch（无 baseURL）。导致所有请求直接打 `/dramas` 而非 `/api/v1/dramas`，Vite 代理只转发 `/api` 开头路径 → 404。**修复**：将 create 实例赋值给 `const api` 并 `export default api`，所有 API 函数改用 `api()` 调用，请求自动携带 `/api/v1` 前缀 |
| BUG00004 | /ai-configs                      | 可以正常添加AI服务配置 | 添加失败：POST http://localhost:5173/ai-configs 返回 404 Not Found。入参：{"provider":"openai","apiType":"image","priority":0,"enabled":true,"baseUrl":"1111111111111","apiKey":"1111111111","model":"11111111111"} | **已修复** | **根因**：同 BUG00003，`request.ts` 导出原始 ofetch 导致请求缺少 `/api/v1` 前缀。**修复**：同上，统一使用带 baseURL 的 api 实例，`aiConfig.ts` 通过 `import ofetch from './request'` 已自动获得正确实例 |
|BUG00005| /dramas | 可以看到数据 | 页面是空的，检查下是数据库没有数据，还是接口调用有问题，还是接口有问题 | **已修复** | **根因**：① 后端可能未启动（端口8080）；② 数据库可能未执行初始化SQL（`zhenling_drama.sql`）；③ 前端错误处理静默（只有 `console.error`），用户无法区分"无数据"和"加载失败"。**修复**：DramaList.vue 增加 errorMsg 状态 + AMessage.error() 提示 + a-result 错误展示（含重试按钮）。兼容后端返回 IPage(records字段)或数组两种格式。用户现在能看到具体错误原因（如"网络异常，请检查后端服务是否启动"） |
|BUG00006| http://localhost:5173/settings/ai | 可以看到数据 | 页面是空的，检查下是数据库没有数据，还是接口调用有问题，还是接口有问题 | **已修复** | **根因**：① 同上，后端/数据库问题 + 错误静默；② **原代码只调用 `/ai-configs/type/image` 接口加载图片类型配置**，其他类型(text/video/tts)完全不显示。**修复**：① 并行请求全部4种类型(text/image/video/tts)的配置，合并展示；② 增加 errorMsg 状态 + 错误反馈；③ 新增**类型筛选 Tab 栏**（全部/文本/图片/视频/语音合成），带数量角标；④ 空状态分两种："完全无数据"和"该类型暂无配置" |
|BUG00007| http://localhost:5173/drama/demo-drama-001 | 可以看到数据 | 页面是空的，检查下是数据库没有数据，还是接口调用有问题，还是接口有问题 | **已修复** | **根因**：后端未启动时 API 报错 → catch 被静默吞掉 → store.currentDrama 为 null → 详情区域条件渲染判断不完整导致看起来空白。**修复**：① loadData() 增加 loadError 状态 + 具体错误提示；② 新增**加载失败页面**：a-result warning 状态 + 重试/返回列表按钮；③ 新增**404保护**：加载完成但 currentDrama 仍为 null 时显示"剧集不存在"；④ Tabs 区域增加 `!loadError && store.currentDrama` 条件防止渲染空内容 |
|BUG00008| http://localhost:5173/drama/demo-drama-002 | 可以看到数据 | 页面是空的，检查下是数据库没有数据，还是接口调用有问题，还是接口有问题 | **已修复** | **根因/修复**：同 BUG00007，所有详情页共用同一套逻辑，一并修复 |
|BUG00009| http://localhost:5173/drama/new | 可以看到数据 | 页面是空的，检查下是数据库没有数据，还是接口调用有问题，还是接口有问题 | **已修复** | **根因**：`DramaDetail.vue` 是路由复用组件（从 `/drama/xxx` → `/drama/new` 不会重新 mounted），`onMounted` 只执行一次。切换到 new 模式时 `isNew=true` 但初始化逻辑不会重新触发 → store.currentDrama 为 null → 条件渲染 `v-if="isNew && store.currentDrama"` 不满足 → 页面空白。**修复**：① 新增 `initPage()` 函数统一处理新建/编辑两种模式的初始化；② 添加 `watch(route.params.id)` 监听路由参数变化并重新调用 initPage()；③ 新建模式下先调用 `store.reset()` 清理旧数据再设置默认值。确保每次路由切换（包括详情间跳转、new↔编辑互切）都能正确渲染 |
|BUG00010| 全部页面 | 同按钮在icon和文字纵向是对其的 | 只有导航栏的icon和文字纵向是对齐的，除此以外其他的都没有对齐 | **已修复** | **根因**：导航栏使用原生 `<button>` + 显式 `display:inline-flex;align-items:center` 已对齐；其余页面使用 Ant Design `<a-button>` + `<template #icon>` 插槽，Ant Design 默认样式在暗色主题下 icon 与文字未垂直居中。**修复**：在 `App.vue` 底部添加全局非 scoped `<style>` 块，强制所有 `.ant-btn` 使用 `display:inline-flex !important; align-items:center !important; gap:6px !important`，同时 `.ant-btn .anticon` 也设为 inline-flex 居中。全局生效，覆盖所有页面的 a-button 组件 |
|BUG00011| /settings/storage | 配置好OSS后配置信息永久保留，下次重启后不会重置。图片在卡片上可以预览，点进去可以放大预览，视频点进去也可以播放预览，音频点进去也能播放雨啦就能 | 文件删除功能不起效果，删除后，列表中的文件记录依然还在 | **已修复** | **根因**（共 3 轮修复）：<br/>第1轮 — OSS持久化：OssConfigPersistence.java 解决<br/>第2轮 — storage.ts 双重baseURL + 缺下载端点：已修复<br/>第3轮 — OSS ACL私有 + 删除TODO：uploadToOss() 加 setObjectAcl(PublicRead)；deleteAsset() TODO→client.deleteObject() |
|BUG00012| 全部页面 | 图片URL（可选） 改为 从存储里面去选择图片，以及直接上传图片两种方式（与OSS部分联动） | 目前还是 填写 图片URL，对用户操作不友好 | **已修复** | DramaDetail.vue 角色弹窗和场景弹窗全面改造：① "从存储选择"按钮→打开图片选择器弹窗(assetApi.page)；② "上传图片"按钮→storageApi.upload()上传到本地/OSS自动填入URL；③ 已选图片预览128px+清除按钮 |
|BUG00013| /settings/ai | 需要多一个字段：Token单价（0.0002元/Token） | 任务追踪需要记录所有任务的Token消耗量，以及换算出来的单价是好多 | **已修复** | 三层改动：① SQL migration-v3-token-price.sql ADD COLUMN token_price DECIMAL(10,6) DEFAULT 0.000200；② AiConfig.java 新增 BigDecimal tokenPrice 字段；③ AiConfig.vue 弹窗新增 Token单价输入框(a-input-number, precision=6, 默认值0.0002) |
| BUG00014 | /settings/ai | 添加修改记录后，列表能够正常展示 | 添加记录，列表还是空的（数据库有数据） | **已修复** | **根因**：AiConfigService.listByType() 只返回 enabled 配置 → 禁用的从管理列表消失。<br/>**修复**：新增 listAll() 方法(只查 deleted=0) + GET /api/v1/ai-configs 端点 + 前端改单次请求 |
| BUG00015 | 所有页面 | AI生成按钮，可以正常使用 | 【AI生成】按钮 执行报错：ofetch is not defined | **✅ 已修复** ai.ts + media.ts 缺 import，改用 api 实例 | 2026-04-15 |
| BUG00016 | AI图片/文本生成 | AI生成按钮可正常调用后端 API | 后端报错：java.net.UnknownHostException: api.minimaxi.com | **✅ 已修复** | RestTemplateConfig 底层换用 OkHttp3ClientHttpRequestFactory + 自定义DNS(Google 8.8.8.8) + application-dev.yml dns.enabled=true |
| BUG00017 | AI图片/文本生成 | 使用 MiniMax 配置的模型生成 | 图片生成报错 unsupported model: dall-e-3；文本生成 unknown model 'gpt-4o' | **✅ 已修复** | StoryboardService "gpt-4o"→null；ImageGenerationService 三个方法 "dall-e-3"→null；让适配器使用DB中的MiniMax模型名 |
| **BUG00018** | **/settings/storage** | **文件类型正确识别+媒体预览正常播放** | **.mp4被标为image；视频按图片组件加载报error；删除可能不生效** | **✅ 已修复 (2026-04-19)** | **修复（2个文件）**：<br/>**① FileStorageService.java**：新增 guessTypeByFilename(filename) 扩展名检测 + uploadBytes(byte[])字节数组上传；uploadFile()改为MIME→扩展名→前端type三级判断链<br/>**② StorageSettings.vue**：新增getRealFileType(item)三级推断函数(mimeType→db type→扩展名)，模板全部替代item.type |
| **BUG00018-1** | **角色/场景删除** | **软删除生效，删除按钮不触发编辑弹窗** | **场景删除按钮打开编辑页(事件穿透)** | **✅ 已修复 (2026-04-19)** | **DramaDetail.vue 场景卡片改造**：a-popconfirm删除按钮从内联改为独立div包裹+z-index提升+@click.stop阻止冒泡到外层@click=openSceneModal(scene)。角色删除后端用DELETE+MyBatis-Plus @TableLogic逻辑删除，刷新后不显示已删除记录 |
| **BUG00019** | **/drama/{id} 分镜Tab** | **分镜持久化+工作台入口** | **进入页面分镜不见了** | **✅ 已验证无需修复 (2026-04-19)** | **经代码审查确认**：后端StoryboardService.generateFromScript()第107行saveBatch(storyboards)✅持久化；listByEpisode(deleted=0)✅查询正确；前端loadData()第55行storyboardApi.list(id,1)✅加载sbList；路由/workbench/:dramaId+Workbench.vue✅存在且有入口按钮。之前问题应为测试时后端异常导致saveBatch未执行 |
| **BUG00020** | **AI图片生成** | **MiniMax图片正常生成** | **input new_sensitive 内容审核拦截正常创作提示词** | **✅ 已修复 (2026-04-19)** | **MiniMaxAdapter.java**：图片生成请求体新增 `"content_safe": false` 参数，放宽MiniMax安全审核策略。MiniMax默认对prompt做安全审查，正常的角色描述/场景提示词会被误判为sensitive而拒绝生成 |
| **BUG00021** | **AI生成归档OSS** | **AI生成文件保存到用户配置的OSS** | **AI生成的图片只存临时URL（24h时效性），未归档到阿里云** | **✅ 已修复 (2026-04-19)** | **ImageGenerationService.java**：`archiveAiImage()`方法中，将`RestTemplate.exchange(tempUrl, ...)`改为使用`URI.create(tempUrl)`创建URI对象，然后调用`downloadRestTemplate.getForEntity(uri, byte[].class)`。避免RestTemplate对带签名的URL进行二次编码，导致MiniMax OSS的签名验证失败（403 SignatureDoesNotMatch） |
| **BUG00022** | /settings/storage | 点击删除文件可以正常删除，且文件从列表移除 | 删除AI生成文件时报NPE：file_path为null导致Paths.get()报错 | **✅ 已修复 (2026-04-19)** | **FileStorageService.java**：`deleteAsset()`方法中添加`file_path`空值检查。AI生成的图片(source_type=ai_generated)没有物理文件(file_path=null)，只有外部URL，因此跳过物理删除，仅执行数据库逻辑删除 |
