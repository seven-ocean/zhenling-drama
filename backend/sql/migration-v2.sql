-- ============================================================
-- 臻灵短剧 - 增量迁移脚本 v2
-- 日期: 2026-04-13
-- 说明: 确保 assets / videos / audios / task_logs 四张表完整可用
-- 用法: 在已有 zhenling_drama 库上直接执行即可（幂等，支持重复执行）
-- ============================================================

SET NAMES utf8mb4;

-- ==================== 1. 素材表 assets ====================
CREATE TABLE IF NOT EXISTS `assets` (
  `id` varchar(36) NOT NULL COMMENT 'UUID',
  `drama_id` varchar(36) DEFAULT NULL COMMENT '剧集ID',
  `type` varchar(30) NOT NULL COMMENT '类型: image/video/audio/file',
  `filename` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_path` varchar(512) DEFAULT NULL COMMENT '存储路径',
  `file_url` varchar(512) DEFAULT NULL COMMENT '访问URL',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `mime_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `width` int DEFAULT NULL COMMENT '图片宽度',
  `height` int DEFAULT NULL COMMENT '图片高度',
  `duration` float DEFAULT NULL COMMENT '音视频时长(秒)',
  `source_type` varchar(30) DEFAULT 'uploaded' COMMENT '来源: generated/uploaded/reference',
  `extra_data` text COMMENT '扩展数据JSON',
  `deleted` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_drama_id` (`drama_id`),
  INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='素材表';

-- ==================== 2. 视频表 videos ====================
CREATE TABLE IF NOT EXISTS `videos` (
  `id` varchar(36) NOT NULL COMMENT 'UUID',
  `drama_id` varchar(36) NOT NULL,
  `episode_number` int NOT NULL,
  `storyboard_id` varchar(36) DEFAULT NULL,
  `video_url` varchar(512) DEFAULT NULL COMMENT '视频URL',
  `duration` float DEFAULT NULL COMMENT '时长(秒)',
  `provider` varchar(50) DEFAULT NULL COMMENT '生成厂商(minimax/openai等)',
  `model` varchar(100) DEFAULT NULL COMMENT '模型名(video-01等)',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending/generating/completed/failed',
  `task_id` varchar(100) DEFAULT NULL COMMENT '厂商任务ID',
  `error_message` text COMMENT '错误信息',
  `extra_data` text COMMENT '扩展数据JSON',
  `deleted` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_drama_episode` (`drama_id`, `episode_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频表';

-- ==================== 3. 音频表 audios ====================
CREATE TABLE IF NOT EXISTS `audios` (
  `id` varchar(36) NOT NULL COMMENT 'UUID',
  `drama_id` varchar(36) NOT NULL,
  `episode_number` int NOT NULL,
  `storyboard_id` varchar(36) DEFAULT NULL,
  `character_id` varchar(36) DEFAULT NULL,
  `audio_url` varchar(512) DEFAULT NULL COMMENT '音频URL',
  `text` text COMMENT '配音文本',
  `provider` varchar(50) DEFAULT NULL COMMENT 'TTS厂商',
  `voice_id` varchar(100) DEFAULT NULL COMMENT '音色ID',
  `duration` float DEFAULT NULL COMMENT '时长(秒)',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending/generating/completed/failed',
  `task_id` varchar(100) DEFAULT NULL COMMENT '厂商任务ID',
  `error_message` text COMMENT '错误信息',
  `extra_data` text COMMENT '扩展数据JSON',
  `deleted` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_drama_episode` (`drama_id`, `episode_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='音频表';

-- ==================== 4. 任务日志表 task_logs ====================
CREATE TABLE IF NOT EXISTS `task_logs` (
  `id` varchar(36) NOT NULL COMMENT 'UUID',
  `drama_id` varchar(36) DEFAULT NULL,
  `task_type` varchar(50) NOT NULL COMMENT '任务类型: video_generate/audio_generate/image_generate/tts/compose',
  `task_id` varchar(100) DEFAULT NULL COMMENT '外部厂商任务ID',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending/running/success/failed',
  `progress` int DEFAULT 0 COMMENT '进度 0-100',
  `message` text COMMENT '进度描述',
  `result` text COMMENT '结果数据(URL或JSON)',
  `deleted` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_drama_id` (`drama_id`),
  INDEX `idx_task_id` (`task_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务日志表';

-- ============================================================
-- 验证
-- ============================================================
SELECT '✅ assets 表就绪' AS info, COUNT(*) AS count FROM assets;
SELECT '✅ videos 表就绪' AS info, COUNT(*) AS count FROM videos;
SELECT '✅ audios 表就绪' AS info, COUNT(*) AS count FROM audios;
SELECT '✅ task_logs 表就绪' AS info, COUNT(*) AS count FROM task_logs;
