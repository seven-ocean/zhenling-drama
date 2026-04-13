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



| 编号       |功能路径| 预期结果     | 实测结果                                                                                                                                                                    |是否修复| AI给出的修复方案 |
|----------|------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|----------|
| BUG00001 |/drama/new (POST /api/v1/dramas)| 可以创建剧集   | 创建剧集失败：POST "/dramas": 404 Not Found                                                                                                                                    | **已修复** | ① 执行 `backend/sql/init-full.sql` 初始化数据库表结构 + 示例数据；② 确保后端服务启动（端口8080）；③ 前端 vite.config.ts 已配置 proxy `/api → localhost:8080`。**根因**：数据库 `dramas` 表不存在或缺少 `deleted` 列（实体类有但原 init.sql 漏掉），导致后端报错返回404 |
| BUG00002 |/settings/ai| 可以添加配置   | 不知道怎么配置，需要初始化参数SQL参考数据                                                                                                                                                  | **已修复** | 已创建 `backend/sql/init-full.sql`，内含 **4条AI配置示例**（OpenAI图片/文本/MiniMax视频/TTS）+ **2条示例剧集** + **3个示例角色** + **3个示例场景**。用户只需将 SQL 中的 `sk-your-openai-key-here` 替换为真实 API Key 即可使用。详见 init-full.sql 文件 |
| BUG00003 | /dramas | 可以正常添加剧集 | 添加失败：POST http://localhost:5173/dramas 返回 404 Not Found。入参：{"title":"111","description":"111","totalEpisodes":1} | **已修复** | **根因**：`request.ts` 中 `ofetch.create({ baseURL })` 创建了带前缀的实例，但 `export default ofetch` 导出的仍是原始 ofetch（无 baseURL）。导致所有请求直接打 `/dramas` 而非 `/api/v1/dramas`，Vite 代理只转发 `/api` 开头路径 → 404。**修复**：将 create 实例赋值给 `const api` 并 `export default api`，所有 API 函数改用 `api()` 调用，请求自动携带 `/api/v1` 前缀 |
| BUG00004 | /ai-configs | 可以正常添加AI服务配置 | 添加失败：POST http://localhost:5173/ai-configs 返回 404 Not Found。入参：{"provider":"openai","apiType":"image","priority":0,"enabled":true,"baseUrl":"1111111111111","apiKey":"1111111111","model":"11111111111"} | **已修复** | **根因**：同 BUG00003，`request.ts` 导出原始 ofetch 导致请求缺少 `/api/v1` 前缀。**修复**：同上，统一使用带 baseURL 的 api 实例，`aiConfig.ts` 通过 `import ofetch from './request'` 已自动获得正确实例 |
| BUG00005 |    |          |                                                                                                                                                                         |    |           |
