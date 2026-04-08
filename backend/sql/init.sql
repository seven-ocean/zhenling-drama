-- =============================================
-- Huobao Drama 数据库表结构
-- 迁移自 SQLite Drizzle ORM
-- =============================================

-- ----------------------------
-- 剧集表 (dramas)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dramas` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'UUID',
    `title` VARCHAR(255) NOT NULL COMMENT '剧集标题',
    `description` TEXT COMMENT '剧集描述',
    `cover_image` VARCHAR(512) COMMENT '封面图URL',
    `status` VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft/editing/producing/completed',
    `total_episodes` INT DEFAULT 0 COMMENT '总集数',
    `created_episodes` INT DEFAULT 0 COMMENT '已生成集数',
    `settings` TEXT COMMENT 'JSON配置',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧集表';

-- ----------------------------
-- 角色表 (characters)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `characters` (
    `id` VARCHAR(36) PRIMARY KEY,
    `drama_id` VARCHAR(36) NOT NULL COMMENT '所属剧集ID',
    `name` VARCHAR(100) NOT NULL COMMENT '角色名',
    `description` TEXT COMMENT '角色描述',
    `image_url` VARCHAR(512) COMMENT '角色形象图',
    `voice_id` VARCHAR(36) COMMENT '关联音色ID',
    `voice_provider` VARCHAR(50) COMMENT '音色厂商',
    `appearance_prompt` TEXT COMMENT '形象提示词',
    `dialogue_style` TEXT COMMENT '对话风格',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `extra_data` TEXT COMMENT '扩展数据JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_drama_id` (`drama_id`),
    FOREIGN KEY (`drama_id`) REFERENCES `dramas`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 场景表 (scenes)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `scenes` (
    `id` VARCHAR(36) PRIMARY KEY,
    `drama_id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(100) NOT NULL COMMENT '场景名',
    `description` TEXT COMMENT '场景描述',
    `image_url` VARCHAR(512) COMMENT '场景图',
    `location` VARCHAR(100) COMMENT '地点',
    `time_of_day` VARCHAR(20) COMMENT '时间段',
    `prompt` TEXT COMMENT 'AI绘图提示词',
    `extra_data` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_drama_id` (`drama_id`),
    FOREIGN KEY (`drama_id`) REFERENCES `dramas`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景表';

-- ----------------------------
-- 分镜表 (storyboards)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `storyboards` (
    `id` VARCHAR(36) PRIMARY KEY,
    `drama_id` VARCHAR(36) NOT NULL,
    `episode_number` INT NOT NULL COMMENT '集数',
    `scene_number` INT NOT NULL COMMENT '场景序号',
    `shot_number` INT NOT NULL COMMENT '镜头序号',
    `shot_type` VARCHAR(20) DEFAULT 'medium' COMMENT '镜头类型',
    `shot_direction` VARCHAR(50) COMMENT '运镜方式',
    `action` TEXT COMMENT '动作描述',
    `dialogue` TEXT COMMENT '台词',
    `character_id` VARCHAR(36) COMMENT '角色ID',
    `scene_id` VARCHAR(36) COMMENT '场景ID',
    `character_image_url` VARCHAR(512) COMMENT '角色图',
    `scene_image_url` VARCHAR(512) COMMENT '场景图',
    `grid_image_url` VARCHAR(512) COMMENT '宫格图',
    `grid_prompt` TEXT COMMENT '宫格提示词',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/generating/completed/failed',
    `extra_data` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_drama_episode` (`drama_id`, `episode_number`),
    FOREIGN KEY (`drama_id`) REFERENCES `dramas`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分镜表';

-- ----------------------------
-- 视频表 (videos)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `videos` (
    `id` VARCHAR(36) PRIMARY KEY,
    `drama_id` VARCHAR(36) NOT NULL,
    `episode_number` INT NOT NULL,
    `storyboard_id` VARCHAR(36),
    `video_url` VARCHAR(512) COMMENT '视频URL',
    `duration` FLOAT COMMENT '时长(秒)',
    `provider` VARCHAR(50) COMMENT '生成厂商',
    `model` VARCHAR(100) COMMENT '模型',
    `status` VARCHAR(20) DEFAULT 'pending',
    `task_id` VARCHAR(100) COMMENT '厂商任务ID',
    `error_message` TEXT,
    `extra_data` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_drama_episode` (`drama_id`, `episode_number`),
    FOREIGN KEY (`drama_id`) REFERENCES `dramas`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';

-- ----------------------------
-- 音频表 (audios)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `audios` (
    `id` VARCHAR(36) PRIMARY KEY,
    `drama_id` VARCHAR(36) NOT NULL,
    `episode_number` INT NOT NULL,
    `storyboard_id` VARCHAR(36),
    `character_id` VARCHAR(36),
    `audio_url` VARCHAR(512),
    `text` TEXT COMMENT '配音文本',
    `provider` VARCHAR(50),
    `voice_id` VARCHAR(100),
    `duration` FLOAT,
    `status` VARCHAR(20) DEFAULT 'pending',
    `task_id` VARCHAR(100),
    `error_message` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_drama_episode` (`drama_id`, `episode_number`),
    FOREIGN KEY (`drama_id`) REFERENCES `dramas`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音频表';

-- ----------------------------
-- AI配置表 (ai_configs)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_configs` (
    `id` VARCHAR(36) PRIMARY KEY,
    `provider` VARCHAR(50) NOT NULL COMMENT '厂商',
    `api_type` VARCHAR(30) NOT NULL COMMENT '类型: text/image/video/tts',
    `base_url` VARCHAR(512),
    `api_key` VARCHAR(256),
    `model` VARCHAR(100),
    `priority` INT DEFAULT 0,
    `enabled` TINYINT(1) DEFAULT 1,
    `config_json` TEXT COMMENT '额外配置JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_provider_type` (`provider`, `api_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置表';

-- ----------------------------
-- 素材表 (assets)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `assets` (
    `id` VARCHAR(36) PRIMARY KEY,
    `drama_id` VARCHAR(36),
    `type` VARCHAR(30) NOT NULL COMMENT '类型: image/video/audio',
    `filename` VARCHAR(255),
    `file_path` VARCHAR(512) COMMENT '存储路径',
    `file_url` VARCHAR(512) COMMENT '访问URL',
    `file_size` BIGINT,
    `mime_type` VARCHAR(100),
    `width` INT,
    `height` INT,
    `duration` FLOAT,
    `source_type` VARCHAR(30) COMMENT '来源: generated/uploaded/reference',
    `extra_data` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_drama_id` (`drama_id`),
    FOREIGN KEY (`drama_id`) REFERENCES `dramas`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材表';

-- ----------------------------
-- 任务日志表 (task_logs)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `task_logs` (
    `id` VARCHAR(36) PRIMARY KEY,
    `drama_id` VARCHAR(36),
    `task_type` VARCHAR(50) NOT NULL,
    `task_id` VARCHAR(100),
    `status` VARCHAR(20),
    `progress` INT DEFAULT 0,
    `message` TEXT,
    `result` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_drama_id` (`drama_id`),
    INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';