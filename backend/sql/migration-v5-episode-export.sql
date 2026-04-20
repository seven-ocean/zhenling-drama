-- 整集导出记录表
CREATE TABLE IF NOT EXISTS `episode_exports` (
    `id` VARCHAR(32) NOT NULL COMMENT '导出ID',
    `drama_id` VARCHAR(32) NOT NULL COMMENT '剧集ID',
    `episode_number` INT NOT NULL COMMENT '集数',
    `export_url` VARCHAR(1024) DEFAULT NULL COMMENT '导出视频URL',
    `duration` FLOAT DEFAULT NULL COMMENT '视频时长(秒)',
    `status` VARCHAR(20) DEFAULT 'processing' COMMENT '状态: processing/completed/failed',
    `shot_count` INT DEFAULT 0 COMMENT '分镜数量',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `extra_data` TEXT DEFAULT NULL COMMENT '扩展数据(JSON)',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_drama_episode` (`drama_id`, `episode_number`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整集导出记录表';
