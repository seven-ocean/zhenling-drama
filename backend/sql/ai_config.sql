-- ----------------------------
-- AI配置表 (ai_configs)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_configs` (
    `id` VARCHAR(36) PRIMARY KEY,
    `provider` VARCHAR(50) NOT NULL COMMENT '厂商: openai/minimax/gemini/ali/volcengine',
    `api_type` VARCHAR(30) NOT NULL COMMENT '类型: text/image/video/tts',
    `base_url` VARCHAR(512) COMMENT 'API地址',
    `api_key` VARCHAR(256) COMMENT 'API密钥',
    `model` VARCHAR(100) COMMENT '模型名',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `config_json` TEXT COMMENT '额外配置JSON',
    `deleted` TINYINT(1) DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_provider_type` (`provider`, `api_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置表';