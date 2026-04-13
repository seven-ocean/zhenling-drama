/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80029
 Source Host           : localhost:3306
 Source Schema         : zhenling_drama

 Target Server Type    : MySQL
 Target Server Version : 80029
 File Encoding         : 65001

 Date: 13/04/2026 22:22:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_configs
-- ----------------------------
DROP TABLE IF EXISTS `ai_configs`;
CREATE TABLE `ai_configs`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '厂商: openai/minimax/gemini/ali/volcengine',
  `api_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型: text/image/video/tts',
  `base_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API地址',
  `api_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API密钥(请替换为真实key)',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名',
  `priority` int(0) NULL DEFAULT 0 COMMENT '优先级(数字越小越优先)',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用: 1启用/0禁用',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '额外配置JSON',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_provider_type`(`provider`, `api_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_configs
-- ----------------------------
INSERT INTO `ai_configs` VALUES ('b8030a23-342f-11f1-baf9-005056c00001', 'openai', 'image', 'https://api.openai.com/v1', 'sk-your-openai-key-here', 'gpt-image-1', 1, 0, '{\"size\": \"1024x1024\", \"quality\": \"standard\"}', 0, '2026-04-10 00:18:18', '2026-04-10 00:35:20');
INSERT INTO `ai_configs` VALUES ('b8060768-342f-11f1-baf9-005056c00001', 'minimax', 'video', 'https://api.minimaxi.chat/v1', 'your-minimax-key-here', 'video-01', 1, 1, '{\"mode\": \"single\"}', 0, '2026-04-10 00:18:18', '2026-04-10 00:18:18');
INSERT INTO `ai_configs` VALUES ('b8084c12-342f-11f1-baf9-005056c00001', 'openai', 'text', 'https://api.openai.com/v1', 'sk-your-openai-key-here', 'gpt-4o', 1, 1, '{\"temperature\": 0.7, \"max_tokens\": 4096}', 0, '2026-04-10 00:18:18', '2026-04-10 00:18:18');
INSERT INTO `ai_configs` VALUES ('b80ab79c-342f-11f1-baf9-005056c00001', 'minimax', 'tts', 'https://api.minimaxi.chat/v1', 'your-minimax-key-here', 'speech-02-turbo', 1, 1, '{\"voice_id\": \"female-tianmei\"}', 0, '2026-04-10 00:18:18', '2026-04-10 00:18:18');

-- ----------------------------
-- Table structure for assets
-- ----------------------------
DROP TABLE IF EXISTS `assets`;
CREATE TABLE `assets`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'UUID',
  `drama_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '剧集ID',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型: image/video/audio/file',
  `filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `file_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存储路径',
  `file_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问URL',
  `file_size` bigint(0) NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MIME类型',
  `width` int(0) NULL DEFAULT NULL COMMENT '图片宽度',
  `height` int(0) NULL DEFAULT NULL COMMENT '图片高度',
  `duration` float NULL DEFAULT NULL COMMENT '音视频时长(秒)',
  `source_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'uploaded' COMMENT '来源: generated/uploaded/reference',
  `extra_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展数据JSON',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drama_id`(`drama_id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '素材表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for audios
-- ----------------------------
DROP TABLE IF EXISTS `audios`;
CREATE TABLE `audios`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `drama_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `episode_number` int(0) NOT NULL,
  `storyboard_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `character_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `audio_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配音文本',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `voice_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `duration` float NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT 'pending/generating/completed/failed',
  `task_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `extra_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drama_episode`(`drama_id`, `episode_number`) USING BTREE,
  CONSTRAINT `audios_ibfk_1` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '音频表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for characters
-- ----------------------------
DROP TABLE IF EXISTS `characters`;
CREATE TABLE `characters`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `drama_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属剧集ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '角色描述',
  `image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色形象图',
  `voice_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联音色ID',
  `voice_provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '音色厂商',
  `appearance_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '形象提示词',
  `dialogue_style` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '对话风格',
  `sort_order` int(0) NULL DEFAULT 0 COMMENT '排序',
  `extra_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展数据JSON',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drama_id`(`drama_id`) USING BTREE,
  CONSTRAINT `characters_ibfk_1` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of characters
-- ----------------------------
INSERT INTO `characters` VALUES ('demochar001', 'demo-drama-001', '林小雨', '女主角，26岁，独立设计师。性格外冷内热，外表干练但内心柔软。', NULL, NULL, NULL, '年轻亚洲女性，黑色长发微卷，穿着简约时尚的米色风衣，精致妆容，眼神温柔坚定，城市街景背景', '简洁干练偶尔流露感性', 1, NULL, 0, '2026-04-10 00:18:18', '2026-04-13 22:22:06');
INSERT INTO `characters` VALUES ('demochar002', 'demo-drama-001', '顾辰', '男主角，29岁，科技公司CEO。沉稳内敛，做事雷厉风行但对待感情笨拙。', NULL, NULL, NULL, '英俊亚洲男性，短发利落，穿深色高定西装，气质成熟稳重，眼神深邃，办公室背景', '低沉冷静偶尔带一丝温柔', 2, NULL, 0, '2026-04-10 00:18:18', '2026-04-13 22:22:08');
INSERT INTO `characters` VALUES ('demochar003', 'demo-drama-001', '苏珊', '女配角，25岁，林小雨的闺蜜兼同事。活泼开朗，是团队的开心果。', NULL, NULL, NULL, '年轻亚洲女性，扎着马尾辫，穿休闲彩色卫衣，笑容灿烂，咖啡厅背景', '热情开朗语速较快', 3, NULL, 0, '2026-04-10 00:18:18', '2026-04-13 22:22:14');

-- ----------------------------
-- Table structure for dramas
-- ----------------------------
DROP TABLE IF EXISTS `dramas`;
CREATE TABLE `dramas`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'UUID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '剧集标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '剧集描述',
  `cover_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图URL',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'draft' COMMENT '状态: draft/in_progress/completed',
  `total_episodes` int(0) NULL DEFAULT 0 COMMENT '总集数',
  `created_episodes` int(0) NULL DEFAULT 0 COMMENT '已生成集数',
  `settings` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'JSON配置',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除: 0正常/1已删',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '剧集表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dramas
-- ----------------------------
INSERT INTO `dramas` VALUES ('demo-drama-001', '示例短剧《都市奇缘》', '一个发生在现代都市的爱情悬疑故事，讲述两位主角在意外相遇后的命运交织。', NULL, 'in_progress', 5, 0, NULL, 0, '2026-04-10 00:18:18', '2026-04-10 11:21:15');
INSERT INTO `dramas` VALUES ('demo-drama-002', '测试短剧《古风江湖》', '古代武侠题材的测试用例。', NULL, 'draft', 3, 1, NULL, 0, '2026-04-10 00:18:18', '2026-04-10 00:30:18');

-- ----------------------------
-- Table structure for scenes
-- ----------------------------
DROP TABLE IF EXISTS `scenes`;
CREATE TABLE `scenes`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `drama_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '场景描述',
  `image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景图',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地点',
  `time_of_day` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '时间段',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI绘图提示词',
  `extra_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drama_id`(`drama_id`) USING BTREE,
  CONSTRAINT `scenes_ibfk_1` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场景表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenes
-- ----------------------------
INSERT INTO `scenes` VALUES ('demo-scene-001', 'demo-drama-001', '设计工作室', '林小雨工作的创意设计空间，充满艺术气息和灵感元素。', NULL, '上海市中心CBD写字楼', '白天', '现代化开放式办公室设计工作室，大落地窗俯瞰城市天际线，白色墙面装饰着各种设计手稿和色彩样板，暖色调灯光，ins风', NULL, 0, '2026-04-10 00:18:18', '2026-04-10 00:18:18');
INSERT INTO `scenes` VALUES ('demo-scene-002', 'demo-drama-001', '高级餐厅', '两人第一次正式约会的法式餐厅，氛围浪漫优雅。', NULL, '上海外滩', '夜晚', '高档法式餐厅内部，烛光餐桌，窗外是璀璨的城市夜景和黄浦江倒影，水晶吊灯，浪漫氛围，电影质感光效', NULL, 0, '2026-04-10 00:18:18', '2026-04-10 00:18:18');
INSERT INTO `scenes` VALUES ('demo-scene-003', 'demo-drama-001', '雨中街道', '两人在雨天意外相遇的街角，命运的开端。', NULL, '上海老城区街道', '黄昏转夜晚', '城市老街区石板路街道下着细雨，路灯昏黄温暖的光晕，湿漉漉的地面反射着霓虹灯招牌，文艺电影感构图，有人撑伞', NULL, 0, '2026-04-10 00:18:18', '2026-04-10 00:18:18');

-- ----------------------------
-- Table structure for storyboards
-- ----------------------------
DROP TABLE IF EXISTS `storyboards`;
CREATE TABLE `storyboards`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `drama_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `episode_number` int(0) NOT NULL COMMENT '集数',
  `scene_number` int(0) NOT NULL COMMENT '场景序号',
  `shot_number` int(0) NOT NULL COMMENT '镜头序号',
  `shot_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'medium' COMMENT '镜头类型',
  `shot_direction` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '运镜方式',
  `action` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '动作描述',
  `dialogue` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '台词',
  `character_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色ID',
  `scene_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景ID',
  `character_image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色图',
  `scene_image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景图',
  `grid_image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '宫格图',
  `grid_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '宫格提示词',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT 'pending/generating/completed/failed',
  `extra_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drama_episode`(`drama_id`, `episode_number`) USING BTREE,
  CONSTRAINT `storyboards_ibfk_1` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分镜表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for task_logs
-- ----------------------------
DROP TABLE IF EXISTS `task_logs`;
CREATE TABLE `task_logs`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `drama_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型',
  `task_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外部任务ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
  `progress` int(0) NULL DEFAULT 0 COMMENT '进度 0-100',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '进度信息',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '结果JSON',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drama_id`(`drama_id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for videos
-- ----------------------------
DROP TABLE IF EXISTS `videos`;
CREATE TABLE `videos`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `drama_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `episode_number` int(0) NOT NULL,
  `storyboard_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `video_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '视频URL',
  `duration` float NULL DEFAULT NULL COMMENT '时长(秒)',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成厂商',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT 'pending/generating/completed/failed',
  `task_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂商任务ID',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `extra_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drama_episode`(`drama_id`, `episode_number`) USING BTREE,
  CONSTRAINT `videos_ibfk_1` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视频表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
