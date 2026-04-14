-- ============================================================
-- MiniMax AI 配置初始化脚本
-- ============================================================
-- 官方文档: https://platform.minimaxi.com/docs/guides/
--
-- Base URL: https://api.minimaxi.com/v1
-- 认证方式: Bearer Token (Authorization: Bearer <api-key>)
--
-- 使用说明:
--   1. 将 <YOUR_MINIMAX_API_KEY> 替换为你的真实 API Key
--   2. 执行本 SQL 插入 ai_configs 表
--   3. 前端 /settings/ai 页面即可看到这些配置并可启用/禁用
-- ============================================================

-- 清理已有的 minimax 配置（避免重复）
DELETE FROM ai_configs WHERE provider = 'minimax';

-- ============================================================
-- 1️⃣ 文本生成 — MiniMax-M2.5（性价比之选）
--    用于: 角色描述生成、场景描述生成、分镜拆解
--    OpenAI 兼容接口: POST /v1/chat/completions
--    上下文窗口: 204,800 tokens
INSERT INTO ai_configs
(id, provider, api_type, base_url, api_key, model, priority, enabled, token_price, description, created_at, updated_at, deleted)
VALUES (
    'mm-text-m25',
    'minimax',
    'text',
    'https://api.minimaxi.com/v1',
    'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8',
    'MiniMax-M2.5',
    10,
    true,
    0.000100,
    'MiniMax 文本模型 M2.5，用于角色/场景描述生成、分镜AI拆解。支持 OpenAI 兼容格式。',
    NOW(),
    NOW(),
    0
);

-- ============================================================
-- 2️⃣ 图片生成 — image-01
--    用于: 角色形象图、场景背景图、分镜画面生成
--    接口: POST /v1/image_generation
--    支持: 文生图、图生图，aspect_ratio 可选 1:1/16:9/4:3 等
INSERT INTO ai_configs
(id, provider, api_type, base_url, api_key, model, priority, enabled, token_price, description, created_at, updated_at, deleted)
VALUES (
    'mm-image-01',
    'minimax',
    'image',
    'https://api.minimaxi.com/v1',
    'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8',
    'image-01',
    10,
    true,
    0.000500,
    'MiniMax 图片生成模型 image-01。支持文生图和图生图，输出 URL 有效期24小时。可设置宽高比(16:9/1:1等)和尺寸(512~2048px)。',
    NOW(),
    NOW(),
    0
);

-- ============================================================
-- 3️⃣ 视频生成 — Hailuo 2.3（最新旗舰）
--    用于: 分镜→视频片段合成
--    接口: POST /v1/video_generation （异步任务）
--    支持: 6秒/10秒视频，720P/768P/1080P，运镜指令[推进][左摇]等
INSERT INTO ai_configs
(id, provider, api_type, base_url, api_key, model, priority, enabled, token_price, description, created_at, updated_at, deleted)
VALUES (
    'mm-video-hailuo23',
    'minimax',
    'video',
    'https://api.minimaxi.com/v1',
    'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8',
    'MiniMax-Hailuo-2.3',
    10,
    true,
    0.010000,
    'MiniMax 海螺视频模型 Hailuo 2.3（最新旗舰）。异步任务模式：创建→轮询状态→下载。支持 6s/10s 视频、1080P 分辨率、15种运镜指令([推进][拉远][左移]等)。',
    NOW(),
    NOW(),
    0
);

-- ============================================================
-- 4️⃣ TTS 语音合成 — speech-02-hd
--    用于: 角色配音、旁白生成
--    接口: POST /v1/t2a_v2
--    输出: mp3/wav/flac，支持 voice_id 选择音色
INSERT INTO ai_configs
(id, provider, api_type, base_url, api_key, model, priority, enabled, token_price, description, created_at, updated_at, deleted)
VALUES (
    'mm-tts-speech02hd',
    'minimax',
    'tts',
    'https://api.minimaxi.com/v1',
    'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8',
    'speech-02-hd',
    10,
    true,
    0.000200,
    'MiniMax 语音合成 speech-02-hd 高清版。同步接口，返回音频URL或Hex数据。支持语速/音量/音高调节，默认音色 female-tianmei。单次最长10000字符。',
    NOW(),
    NOW(),
    0
);

-- ============================================================
-- 验证插入结果
SELECT id, provider, api_type, model, enabled, priority
FROM ai_configs
WHERE provider = 'minimax'
ORDER BY api_type;
