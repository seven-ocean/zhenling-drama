-- 角色表增加试听音频URL字段
-- 用于存储角色专属音色的试听音频文件URL

ALTER TABLE `characters`
ADD COLUMN IF NOT EXISTS `preview_audio_url` VARCHAR(1024) DEFAULT NULL COMMENT '试听音频URL（OSS归档）';
