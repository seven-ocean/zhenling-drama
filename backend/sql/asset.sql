-- ----------------------------
-- 素材表 (assets)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `assets` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'UUID',
    `drama_id` VARCHAR(36) COMMENT '剧集ID',
    `type` VARCHAR(30) NOT NULL COMMENT '类型: image/video/audio/file',
    `filename` VARCHAR(255) COMMENT '原始文件名',
    `file_path` VARCHAR(512) COMMENT '存储路径',
    `file_url` VARCHAR(512) COMMENT '访问URL',
    `file_size` BIGINT COMMENT '文件大小(字节)',
    `mime_type` VARCHAR(100) COMMENT 'MIME类型',
    `width` INT COMMENT '图片宽度',
    `height` INT COMMENT '图片高度',
    `duration` FLOAT COMMENT '音视频时长(秒)',
    `source_type` VARCHAR(30) DEFAULT 'uploaded' COMMENT '来源: generated/uploaded/reference',
    `extra_data` TEXT COMMENT '扩展数据JSON',
    `deleted` TINYINT(1) DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_drama_id` (`drama_id`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材表';