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

 Date: 16/04/2026 22:46:34
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
  `token_price` decimal(10, 8) NULL DEFAULT NULL COMMENT 'token单价',
  `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_provider_type`(`provider`, `api_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_configs
-- ----------------------------
INSERT INTO `ai_configs` VALUES ('mm-image-01', 'minimax', 'image', 'https://api.minimaxi.com/v1', 'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8', 'image-01', 10, 1, NULL, 0, '2026-04-15 00:25:42', '2026-04-15 00:25:42', 0.00050000, 'MiniMax 图片生成模型 image-01。支持文生图和图生图，输出 URL 有效期24小时。可设置宽高比(16:9/1:1等)和尺寸(512~2048px)。');
INSERT INTO `ai_configs` VALUES ('mm-text-m25', 'minimax', 'text', 'https://api.minimaxi.com/v1', 'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8', 'MiniMax-M2.5', 10, 1, NULL, 0, '2026-04-15 00:25:42', '2026-04-16 16:21:32', 0.00010000, 'MiniMax 文本模型 M2.5，用于角色/场景描述生成、分镜AI拆解。支持 OpenAI 兼容格式。');
INSERT INTO `ai_configs` VALUES ('mm-tts-speech02hd', 'minimax', 'tts', 'https://api.minimaxi.com/v1', 'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8', 'speech-02-hd', 10, 1, NULL, 0, '2026-04-15 00:25:42', '2026-04-15 00:25:42', 0.00020000, 'MiniMax 语音合成 speech-02-hd 高清版。同步接口，返回音频URL或Hex数据。支持语速/音量/音高调节，默认音色 female-tianmei。单次最长10000字符。');
INSERT INTO `ai_configs` VALUES ('mm-video-hailuo23', 'minimax', 'video', 'https://api.minimaxi.com/v1', 'sk-api-mEukL7AaKs0oT91kbgn24RCWqztejVZ-6saBzLjjzZACpGqCg8MHcGBhQFMR-6B4hgTSJ11IoolibWYSShKbDoybVDsmkzDG9peIz8A9AkNZIp5RWa2gGu8', 'MiniMax-Hailuo-2.3', 10, 1, NULL, 0, '2026-04-15 00:25:42', '2026-04-15 00:25:42', 0.01000000, 'MiniMax 海螺视频模型 Hailuo 2.3（最新旗舰）。异步任务模式：创建→轮询状态→下载。支持 6s/10s 视频、1080P 分辨率、15种运镜指令([推进][拉远][左移]等)。');

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
-- Records of assets
-- ----------------------------
INSERT INTO `assets` VALUES ('192c7662f293411f87cbda4902cf30fb', 'demo-drama-001', 'image', 'character_demochar002.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fffc73afe-9e19-4211-a71f-8908c49948ee_aigc.jpeg?Expires=1776433865&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=rQ%2FdPgfjPVXtzzWKkqv1kxdrCEE%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"demochar002\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:51:07');
INSERT INTO `assets` VALUES ('1fee9df2e5a64317947ce0b24dca3b04', 'demo-drama-001', 'image', 'character_demochar002.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F09b9d24f-4c76-4b6b-9d55-86ab56089150_aigc.jpeg?Expires=1776433972&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=GdQTzbFOLyVvxmLQ5vm3Hhry61Y%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"demochar002\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:52:53');
INSERT INTO `assets` VALUES ('2f987d818f9d4fbc906cc3fa3ded767b', 'demo-drama-001', 'image', 'scene_demo-scene-003.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fbf86720f-af68-4939-89c3-3d7f38ad429e_aigc.jpeg?Expires=1776433806&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=2NTv9QjxXBIo4veBBGaz3%2F4Vv8w%3D', NULL, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"scene\",\"sceneId\":\"demo-scene-003\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:50:07');
INSERT INTO `assets` VALUES ('4fdfe0bdf20e4d5c85b06826d3e2c100', 'demo-drama-001', 'image', 'character_demochar001.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F2201595e-c29d-4e96-866f-f67cd7c25247_aigc.jpeg?Expires=1776433642&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=s6VvsUMrFHLIHa7jcmKilzG%2BMfE%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"demochar001\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:47:23');
INSERT INTO `assets` VALUES ('55c752ca21f847ef828bf5120d920288', NULL, 'image', '王天.png', 'drama-assets/2026/04/14/c8041b80d5b349e7a0a2587af3994472.png', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/14/c8041b80d5b349e7a0a2587af3994472.png', 1957436, 'image/png', 1536, 2730, NULL, 'oss', NULL, 0, '2026-04-14 23:58:10');
INSERT INTO `assets` VALUES ('62831fe523034f1aae466b8b6123fc8b', 'fe-dark-dragon', 'image', 'character_char-shiida.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fc9ae799e-fad8-4045-9a52-80f35a315861_aigc.jpeg?Expires=1776434900&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=pNc4SdJvjpUSOZOUgr3WxKTGE1E%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"char-shiida\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 22:08:21');
INSERT INTO `assets` VALUES ('6b0f2d35a7e24d5bb499f2c8e2a8fb46', 'demo-drama-001', 'image', 'scene_demo-scene-001.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F47ddc130-bcf0-44e2-93f1-dcda10b6ff6a_aigc.jpeg?Expires=1776433750&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=g0DQHFziQ%2FcVJALPOQuDnNixFRg%3D', NULL, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"scene\",\"sceneId\":\"demo-scene-001\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:49:11');
INSERT INTO `assets` VALUES ('8154c8a5a2c249c3a7329665eee58f1f', 'demo-drama-001', 'image', 'character_demochar002.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F09bd4bd5-168d-4ea6-9f3b-e2b8edf126b6_aigc.jpeg?Expires=1776433667&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=MgPYXJg9iuJ9myxL%2FYoj7KRSvgo%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"demochar002\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:47:48');
INSERT INTO `assets` VALUES ('8469e85acca247a4bebd7e8bc6d50d6f', 'demo-drama-001', 'image', 'character_demochar003.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fa627ee6b-a366-4a58-84db-db845e8c4255_aigc.jpeg?Expires=1776433704&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=0rexov%2FRKkjfCmUn0vrk%2FPtpz88%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"demochar003\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:48:25');
INSERT INTO `assets` VALUES ('8aef12e53fa445af92a186f37e3f6c5d', 'demo-drama-001', 'image', 'character_demochar003.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F0ee4c828-968c-4186-bcdc-9b2b68dd934a_aigc.jpeg?Expires=1776433927&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=wHkizQ6DRKZbCIEz9LqEpa5oRFQ%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"demochar003\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:52:08');
INSERT INTO `assets` VALUES ('b8c20bbec4b64915b7d472c828a1a7eb', 'fe-dark-dragon', 'image', 'character_char-mars.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fd630e8e1-1a61-4102-83e8-4fec356bd219_aigc.jpeg?Expires=1776434877&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=CdQSoPi9BEy6wtNc5AU28D2JhBc%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"char-mars\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 22:07:58');
INSERT INTO `assets` VALUES ('bdc25838eb2c4f3191d0a628f3cfdc03', NULL, 'image', '古风江湖.png', 'drama-assets/2026/04/16/26cc8dbb15f447f9b5fa2e2fe32a1470.png', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/16/26cc8dbb15f447f9b5fa2e2fe32a1470.png', 5048647, 'image/png', 2730, 1535, NULL, 'oss', NULL, 0, '2026-04-16 22:06:26');
INSERT INTO `assets` VALUES ('bf7f228e1eb546dea7d13a50be3c604d', NULL, 'image', '首页.mp4', 'drama-assets/2026/04/15/72039ff99e864a5988d0e8e84f34febd.mp4', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/15/72039ff99e864a5988d0e8e84f34febd.mp4', 45528487, 'video/mp4', NULL, NULL, NULL, 'oss', NULL, 0, '2026-04-15 00:41:03');
INSERT INTO `assets` VALUES ('c2bf7ebe739b40478039d8c7fda26a0f', 'demo-drama-001', 'image', 'scene_demo-scene-002.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F1a5c58ac-fe5a-448d-866d-92d0886d2eec_aigc.jpeg?Expires=1776433831&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=%2FG%2BC%2BTBbH%2BxtKXKtJ8cNTyBdXps%3D', NULL, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"scene\",\"sceneId\":\"demo-scene-002\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:50:32');
INSERT INTO `assets` VALUES ('ce759fc9498a4e6295769b8a543cab7e', NULL, 'image', '头像.png', 'drama-assets/2026/04/15/b9c4a2196bcd46098ba549c5103c12df.png', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/15/b9c4a2196bcd46098ba549c5103c12df.png', 1423821, 'image/png', 2048, 2048, NULL, 'oss', NULL, 0, '2026-04-15 00:40:41');
INSERT INTO `assets` VALUES ('cf20f7f32d3043f3a7aa768a5e1dffaa', 'demo-drama-001', 'image', 'character_demochar002.png', NULL, 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fab6dc34f-fb25-4ec1-ae45-861a27930c1d_aigc.jpeg?Expires=1776434008&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=eYX6waeOGV60I7apCtT2IO9wABg%3D', 0, 'image/png', NULL, NULL, NULL, 'ai_generated', '{\"type\":\"character\",\"characterId\":\"demochar002\",\"provider\":\"auto\",\"model\":\"null\"}', 0, '2026-04-16 21:53:30');
INSERT INTO `assets` VALUES ('e0c120ec5f88467894a2cc6b21fe8b17', NULL, 'image', '柳如烟.png', 'drama-assets/2026/04/14/b92268fd38fb48e18448eba66b6435d3.png', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/14/b92268fd38fb48e18448eba66b6435d3.png', 2885617, 'image/png', 1536, 2730, NULL, 'oss', NULL, 0, '2026-04-14 23:57:34');
INSERT INTO `assets` VALUES ('ee6ca752214b4b41b4149adb449e4278', NULL, 'image', '火纹.png', 'drama-assets/2026/04/16/1d4e4b258c0b4b82a0e501a8161b2f09.png', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/16/1d4e4b258c0b4b82a0e501a8161b2f09.png', 389026, 'image/png', 620, 358, NULL, 'oss', NULL, 0, '2026-04-16 22:04:20');

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
INSERT INTO `characters` VALUES ('char-cain', 'fe-dark-dragon', '凯因', '塔利斯的弓箭手，18岁。马尔斯早期战友之一，性格开朗热血，是队伍中的气氛担当。擅长远程支援作战。', NULL, NULL, NULL, '年轻战士形象，棕色短发，身材精干敏捷，身穿轻便皮甲，背着弓箭袋，笑容爽朗自信，动漫风格', '热情直爽语速快，喜欢开玩笑但关键时刻靠得住', 5, '{\"role\":\"party_member\",\"faction\":\"塔利斯\",\"class\":\"弓箭手(Archer)\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-camus', 'fe-dark-dragon', '卡缪', '格鲁尼亚王国的名将，22岁。被誉为\"格鲁尼亚之狼\"，统领帝国最强的重骑兵军团。与马尔斯交战后败北，被饶恕后假死隐退。武艺超群且有荣誉感，是令人敬畏的对手。', NULL, NULL, NULL, '冷峻的青年将军形象，冰蓝色短发整齐向后梳，冷漠的浅色眼眸，身穿黑色格鲁尼亚重装骑士甲，手持长枪，面容俊美但毫无笑意，动漫风格', '冷漠简洁字斟句酌，军人作风不讲废话，重视荣誉和对手', 10, '{\"role\":\"honorable_rival\",\"faction\":\"格鲁尼亚\",\"class\":\"重装甲(Social Knight)\",\"fate\":\"假死隐退\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-elis', 'fe-dark-dragon', '艾莉丝', '马尔斯的姐姐，阿利提亚公主。帝国入侵后被囚禁在暗黑神殿深处，是马尔斯最终决战的重要动力之一。被马尔斯救出后姐弟重逢。', NULL, NULL, NULL, '温柔的公主形象（受困状态），银白色长发（与马尔斯相似）凌乱披散，苍白的脸庞但眼中仍有希望，身穿破旧的白色长裙，手腕上有镣铐的痕迹，脆弱而坚强的美，动漫风格', '虚弱但温暖，见到马尔斯时哽咽呼唤弟弟，声音颤抖但充满爱意', 17, '{\"role\":\"damsel_rescued\",\"faction\":\"阿利提亚\",\"status\":\"被囚禁→获救\",\"relation\":{\"brother\":\"马尔斯\"}}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-ganev', 'fe-dark-dragon', '卡涅夫', '多鲁亚帝国的黑暗祭司/法师，梅迪乌斯的代理人。在暗黑神殿主持暗黑仪式，制造暗黑魔法屏障保护梅迪乌斯。只有哥多教授的星光魔法才能破除其屏障。冷酷残忍的宗教狂热者。', NULL, NULL, NULL, '邪恶的黑暗祭司形象，瘦高的身躯裹在黑色长袍中兜帽遮住大半张脸，只露出的皮肤苍白如纸，眼中闪烁诡异的红光，手中持镶嵌黑水晶的法杖，周围漂浮暗影粒子，阴森恐怖，动漫风格', '阴柔尖锐充满狂热信仰，视人类为献祭品，谈论暗黑之神时近乎癫狂', 14, '{\"role\":\"boss_minion\",\"faction\":\"多鲁亚帝国\",\"class\":\"黑暗祭司(Dark Bishop)\",\"ability\":\"暗黑魔法屏障\",\"defeated_by\":\"星光魔法\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-gotoh', 'fe-dark-dragon', '哥多', '龙人族的贤者长老。拥有千年智慧和强大的魔法力量，预言马尔斯将成为拯救大陆的英雄。向马尔斯传授星光魔法——唯一能破除卡涅夫暗黑屏障的法术。', NULL, NULL, NULL, '古老的龙人贤者形象，长长的白发和胡须，智慧深邃的眼睛（异色瞳一蓝一金），身穿绣满符文的白袍，手持发光的法杖，周身环绕微弱的光芒，神圣而庄严，动漫风格', '缓慢深沉充满哲理，说话像吟诵预言，语气中带着千年的沧桑', 12, '{\"role\":\"wise_mentor\",\"faction\":\"龙人族(守序)\",\"ability\":\"传授星光魔法\",\"title\":\"龙人贤者\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-gra_king', 'fe-dark-dragon', '格拉国王', '林克之父，格拉王国的暴君。当年背叛阿卡奈亚投靠帝国换取权力，统治残暴。被亲生儿子林克刺杀——这是全剧最沉重的悲剧时刻之一。', NULL, NULL, NULL, '堕落的老年国王形象，花白的乱发和浓密的灰胡须，肥胖臃肿的身躯穿着华丽的金边王袍，脸上布满贪婪和暴戾，坐在铺满毛皮的王座上，昏暗烛光下的阴影扭曲，动漫风格', '暴虐贪婪声音粗哑，习惯用命令口吻说话，提到权力时两眼放光', 15, '{\"role\":\"tragic_antagonist\",\"faction\":\"格拉（帝国附庸）\",\"fate\":\"被儿子林克弑杀\",\"crime\":\"背叛阿卡奈亚\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-hardin', 'fe-dark-dragon', '哈丁', '奥利安王国王弟，20岁。马其顿围城期间坚守奥利安王城的英雄。勇猛善战、为人正直，加入义军后成为马尔斯最得力的战将之一。后期命运复杂。', NULL, NULL, NULL, '英武的年轻贵族骑士形象，金棕色短发，锐利的灰色眼睛，身披奥利安王家纹饰的重甲，手持骑枪，气场强大自信，动漫风格', '豪迈自信声音洪亮，直言不讳，对马尔斯既尊重又有竞争意识', 6, '{\"role\":\"party_leader\",\"faction\":\"奥利安\",\"class\":\"骑士(Paladin)\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-link', 'fe-dark-dragon', '林克', '格拉王国的王子，18岁。父亲是背叛阿卡奈亚投靠帝国的暴君，林克对父亲的罪行深感悔恨。最终选择大义灭亲杀死父亲，带领格拉军队归降义军赎罪。', NULL, NULL, NULL, '忧郁的年轻王子形象，深蓝色短发略显凌乱，深邃的黑眼睛，身穿黑色和银色相间的格拉王室铠甲，表情常带愧疚和决绝，动漫风格', '低沉内敛说话犹豫，背负罪恶感，决定行动后会变得异常坚决', 9, '{\"role\":\"redeemed_antihero\",\"faction\":\"格拉→义军\",\"tragic_element\":\"弑父赎罪\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-maria', 'fe-dark-dragon', '玛莉亚', '密涅瓦的妹妹，14岁。马其顿王国的第二公主，心地纯洁善良。被帝国囚禁在迪鲁城地牢中，被马尔斯救出后成为队伍中的治愈者（僧侣）。', NULL, NULL, NULL, '清纯可爱的少女形象，粉色长发柔顺垂落，水汪汪的大眼睛，身穿白色修女袍配淡金色镶边，手中握着治疗杖，天真无邪的笑容，动漫风格', '温软胆怯说话轻声细语，对姐姐密涅瓦非常依赖，害怕战斗但愿意帮助他人', 8, '{\"role\":\"healer\",\"faction\":\"马其顿→义军\",\"class\":\"僧侣(Cleric)\",\"relation\":{\"sister\":\"密涅瓦\"}}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-mars', 'fe-dark-dragon', '马尔斯', '阿利提亚王国王子，16岁。父王被帝国杀害后流亡塔利斯岛三年，后率军复国。性格坚毅、富有领导力但内心善良，是唯一能使用光之圣剑法尔西昂的人（安里血统）。最终成为\"星之王\"，统一阿卡奈亚大陆。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fd630e8e1-1a61-4102-83e8-4fec356bd219_aigc.jpeg?Expires=1776434877&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=CdQSoPi9BEy6wtNc5AU28D2JhBc%3D', NULL, NULL, '年轻英俊男性王子形象，银白色短发，蓝色眼眸，身穿银蓝色相间的骑士铠甲，披着深红色斗篷，手持法尔西昂光剑，气质高贵而坚毅，动漫风格，精致面部特征', '沉稳坚定有担当，对朋友温暖关怀，面对敌人时果敢决断', 1, '{\"role\":\"protagonist\",\"faction\":\"义军\",\"weapon\":\"法尔西昂(光之圣剑)\",\"bloodline\":\"安里\"}', 0, '2026-04-16 21:51:30', '2026-04-16 22:08:00');
INSERT INTO `characters` VALUES ('char-minerva', 'fe-dark-dragon', '密涅瓦', '马其顿王国的公主，19岁。被称为\"赤色龙骑兵\"的女武神，率领马其顿最强龙骑兵团。起初效忠于帝国，因不满暴政且妹妹玛莉亚被囚禁而倒戈加入义军。性格骄傲但不失正义感。', NULL, NULL, NULL, '威严冷艳的女性骑士形象，紫红色长发扎成高马尾，琥珀色竖瞳，身穿红色重型龙骑甲胄，背负巨型战斧，坐骑为红色飞龙（可选），气场强大不怒自威，动漫风格', '高傲冷峻语气简短，内心柔软但对陌生人戒备，谈论妹妹时语气软化', 7, '{\"role\":\"ally_commander\",\"faction\":\"马其顿→义军\",\"class\":\"龙骑(Dragon Knight)\",\"title\":\"赤色龙骑兵\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('char-nina', 'fe-dark-dragon', '妮娜', '阿卡奈亚公主，17岁。阿卡奈亚亡国后的遗孤领袖，率领抵抗军坚持斗争。聪慧冷静，持有火焰纹章（王室代代相传的封印之盾）。将火焰纹章交给马尔斯后成为重要盟友，战后重建阿卡奈亚并即位女王。', NULL, NULL, NULL, '优雅高贵的公主形象，金色长发编成复杂的辫子，蓝紫色眼眸，身穿深蓝色礼服式铠甲，肩部有阿卡奈亚纹章装饰，手持火焰纹章徽盾，气质成熟冷静，动漫风格', '端庄优雅言辞谨慎，作为公主有责任感，偶尔流露孤独和坚韧', 3, '{\"role\":\"ally_leader\",\"faction\":\"阿卡奈亚抵抗军\",\"item\":\"火焰纹章\"}', 0, '2026-04-16 21:51:30', '2026-04-16 21:51:30');
INSERT INTO `characters` VALUES ('char-shiida', 'fe-dark-dragon', '希达（希妲）', '塔利斯公主，15岁。天真烂漫却勇敢坚强，在帝国入侵时找到马尔斯求助。对马尔斯一见倾心，是马尔斯最重要的精神支柱之一。最终成为阿利提亚王后。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fc9ae799e-fad8-4045-9a52-80f35a315861_aigc.jpeg?Expires=1776434900&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=pNc4SdJvjpUSOZOUgr3WxKTGE1E%3D', NULL, NULL, '美丽年轻的公主形象，浅绿色长发及腰，碧绿眼睛，身穿白色和淡紫色相间的长裙配银色护甲，头戴小巧的王冠，温柔而坚定的表情，动漫风格', '活泼开朗带点娇羞，关心马尔斯时会变得认真，战斗时勇敢无畏', 2, '{\"role\":\"heroine\",\"faction\":\"塔利斯\",\"relation_to_mars\":\"恋人→妻子\"}', 0, '2026-04-16 21:51:30', '2026-04-16 22:08:23');
INSERT INTO `characters` VALUES ('char-talis_king', 'fe-dark-dragon', '塔利斯国王', '希达的父亲，塔利斯岛的统治者。在帝国入侵时支持马尔斯，为其提供船只和物资开启征程。开明的明君。', NULL, NULL, NULL, '和蔼的中年国王形象，棕灰色短发和修剪整齐的胡须，身穿塔利斯风格的海洋蓝色王服，坐在朴素但整洁的书房中，书架上摆满航海图，睿智温和的气质，动漫风格', '温和而有远见，说话条理清晰，对女儿希达宠溺但尊重她的选择', 18, '{\"role\":\"supporting_ally\",\"faction\":\"塔利斯\",\"action\":\"资助马尔斯出海复国\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `characters` VALUES ('demochar001', 'demo-drama-001', '林小雨', '女主角，26岁，独立设计师。性格外冷内热，外表干练但内心柔软。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F2201595e-c29d-4e96-866f-f67cd7c25247_aigc.jpeg?Expires=1776433642&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=s6VvsUMrFHLIHa7jcmKilzG%2BMfE%3D', NULL, NULL, '年轻亚洲女性，黑色长发微卷，穿着简约时尚的米色风衣，精致妆容，眼神温柔坚定，城市街景背景', '简洁干练偶尔流露感性', 1, NULL, 0, '2026-04-10 00:18:18', '2026-04-16 21:47:27');
INSERT INTO `characters` VALUES ('demochar002', 'demo-drama-001', '顾辰', '男主角，29岁，科技公司CEO。沉稳内敛，做事雷厉风行但对待感情笨拙。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fab6dc34f-fb25-4ec1-ae45-861a27930c1d_aigc.jpeg?Expires=1776434008&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=eYX6waeOGV60I7apCtT2IO9wABg%3D', NULL, NULL, '男，29岁，看起来小鲜肉，短发利落，穿深色高定西装，气质成熟稳重，眼神深邃，办公室背景英俊亚洲男性，短', '低沉冷静偶尔带一丝温柔', 2, NULL, 0, '2026-04-10 00:18:18', '2026-04-16 21:53:32');
INSERT INTO `characters` VALUES ('demochar003', 'demo-drama-001', '苏珊', '女配角，25岁，林小雨的闺蜜兼同事。活泼开朗，是团队的开心果。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F0ee4c828-968c-4186-bcdc-9b2b68dd934a_aigc.jpeg?Expires=1776433927&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=wHkizQ6DRKZbCIEz9LqEpa5oRFQ%3D', NULL, NULL, '年轻亚洲女性，扎着马尾辫，穿休闲彩色卫衣，笑容灿烂，咖啡厅背景', '热情开朗语速较快', 3, NULL, 0, '2026-04-10 00:18:18', '2026-04-16 21:52:10');

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
INSERT INTO `dramas` VALUES ('demo-drama-001', '示例短剧《都市奇缘》', '一个发生在现代都市的爱情悬疑故事，讲述两位主角在意外相遇后的命运交织。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fbf86720f-af68-4939-89c3-3d7f38ad429e_aigc.jpeg?Expires=1776433806&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=2NTv9QjxXBIo4veBBGaz3%2F4Vv8w%3D', 'draft', 5, 0, NULL, 0, '2026-04-10 00:18:18', '2026-04-16 22:36:41');
INSERT INTO `dramas` VALUES ('demo-drama-002', '测试短剧《古风江湖》', '古代武侠题材的测试用例。', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/16/26cc8dbb15f447f9b5fa2e2fe32a1470.png', 'draft', 3, 1, NULL, 0, '2026-04-10 00:18:18', '2026-04-16 22:06:36');
INSERT INTO `dramas` VALUES ('fe-dark-dragon', '《火焰之纹章：暗黑龙与光之剑》', '系列原点之作。阿利提亚王子马尔斯在祖国沦陷后流亡三年，从塔利斯岛起兵，联合奥利安、马其顿、格拉、格鲁尼亚等诸国义军，最终击败暗黑神梅迪乌斯，解放阿卡奈亚大陆。包含宿命轮回、神剑封印、龙人族等核心设定。', 'https://avater2025.oss-cn-chengdu.aliyuncs.com/drama-assets/2026/04/16/1d4e4b258c0b4b82a0e501a8161b2f09.png', 'draft', 10, 0, '{\"genre\":\"奇幻/战争\",\"era\":\"中世纪幻想大陆\",\"themes\":[\"宿命与成长\",\"复国与正义\",\"友情与牺牲\"],\"core_items\":[\"火焰纹章(封印之盾)\",\"法尔西昂(光之圣剑)\",\"星光魔法\"]}', 0, '2026-04-16 21:51:30', '2026-04-16 22:04:30');

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
INSERT INTO `scenes` VALUES ('altea-border', 'fe-dark-dragon', '阿利提亚边境', '马尔斯阔别三年后重返祖国土地的第一站。情感浓烈的归乡时刻。', NULL, '阿利提亚边境', '黎明', '黎明时分的国境线，一条古道穿过起伏的丘陵通向远方阿利提亚的方向，路边的界碑上依稀可辨\"ALTEA\"的字样（已被破坏），晨雾笼罩的大地逐渐被金色的曙光穿透，充满希望与感伤交织的情绪，动漫风格', '{\"chapter\":\"第六章\",\"episodes\":[\"15\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('altea-throne-room', 'fe-dark-dragon', '阿利提亚王座大厅', '被占领后收复的阿利提亚王城主殿。马尔斯在此祭奠父王并宣告复国。', NULL, '阿利提亚王城', '白天', '收复后的王座大厅，阳光透过彩色玻璃穹顶洒下斑驳光影，破碎的王座已被重新修复，墙壁上残留的战斗痕迹尚未完全清理，地上摆放着鲜花和祭品——这是祭奠先王的场所，庄严肃穆中带着新生的希望，动漫风格', '{\"chapter\":\"第六章\",\"episodes\":[\"16\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('deil-castle', 'fe-dark-dragon', '迪鲁城', '囚禁玛莉亚的地牢所在城堡。阴森的帝国监狱要塞。', NULL, '迪鲁领地', '夜晚', '阴暗的要塞城堡，黑色石墙上插满尖锐的铁刺，城门紧闭只有火把提供照明，塔楼上巡逻的士兵剪影，整体色调偏冷灰蓝色，压迫感十足的监狱氛围，动漫风格', '{\"chapter\":\"第三章\",\"episodes\":[\"9\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('deil-dungeon', 'fe-dark-dragon', '迪鲁城地牢', '玛莉亚被囚禁的地下牢房。潮湿、黑暗、绝望。', NULL, '迪鲁城地下', '黑暗', '狭小阴湿的石室牢房，墙壁渗水长满青苔，地面铺着干草，角落里有一张破旧的小床，铁栅栏门上的锁锈迹斑斑，唯一的光源来自高处窄小的透气窗透入的一丝月光，绝望氛围，动漫风格', '{\"chapter\":\"第三章\",\"episodes\":[\"9\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('deil-outer', 'fe-dark-dragon', '迪鲁城外围', '迪鲁城周边的平原战场，玛莉亚被囚禁的城堡近郊。', NULL, '迪鲁领地', '白天', '战火洗礼过的平原废墟，烧焦的土地和断裂的兵器散落各处，远处迪鲁城的高耸城墙在烟尘中若隐若现，乌云压顶的阴沉天空，肃杀的战争前奏氛围，动漫风格', '{\"chapter\":\"第三章\",\"episodes\":[\"8\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('demo-scene-001', 'demo-drama-001', '设计工作室', '林小雨工作的创意设计空间，充满艺术气息和灵感元素。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F47ddc130-bcf0-44e2-93f1-dcda10b6ff6a_aigc.jpeg?Expires=1776433750&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=g0DQHFziQ%2FcVJALPOQuDnNixFRg%3D', '上海市中心CBD写字楼', '白天', '现代化开放式办公室设计工作室，大落地窗俯瞰城市天际线，白色墙面装饰着各种设计手稿和色彩样板，暖色调灯光，ins风', NULL, 0, '2026-04-10 00:18:18', '2026-04-16 21:49:13');
INSERT INTO `scenes` VALUES ('demo-scene-002', 'demo-drama-001', '高级餐厅', '两人第一次正式约会的法式餐厅，氛围浪漫优雅。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2F1a5c58ac-fe5a-448d-866d-92d0886d2eec_aigc.jpeg?Expires=1776433831&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=%2FG%2BC%2BTBbH%2BxtKXKtJ8cNTyBdXps%3D', '上海外滩', '夜晚', '高档法式餐厅内部，烛光餐桌，窗外是璀璨的城市夜景和黄浦江倒影，水晶吊灯，浪漫氛围，电影质感光效', NULL, 0, '2026-04-10 00:18:18', '2026-04-16 21:50:34');
INSERT INTO `scenes` VALUES ('demo-scene-003', 'demo-drama-001', '雨中街道', '两人在雨天意外相遇的街角，命运的开端。', 'https://hailuo-image-algeng-data.oss-cn-wulanchabu.aliyuncs.com/image_inference_output%2Ftalkie%2Fprod%2Fimg%2F2026-04-16%2Fbf86720f-af68-4939-89c3-3d7f38ad429e_aigc.jpeg?Expires=1776433806&OSSAccessKeyId=LTAI5tB2SwrRwAtD23etQUbC&Signature=2NTv9QjxXBIo4veBBGaz3%2F4Vv8w%3D', '上海老城区街道', '黄昏转夜晚', '城市老街区石板路街道下着细雨，路灯昏黄温暖的光晕，湿漉漉的地面反射着霓虹灯招牌，文艺电影感构图，有人撑伞', NULL, 0, '2026-04-10 00:18:18', '2026-04-16 21:50:09');
INSERT INTO `scenes` VALUES ('dolhr-territory', 'fe-dark-dragon', '多鲁亚帝国本土', '义军攻入帝国腹地的荒原。焦土般的敌国领土。', NULL, '多鲁亚帝国', '黄昏', '被战争摧毁的帝国领土，黑色的火山岩地貌，地表裂开喷出硫磺烟雾，远处有被烧毁的城镇残骸，天空呈现出不祥的暗红色，大军行进的剪影在地平线上推进，末日般苍凉的氛围，动漫风格', '{\"chapter\":\"第八章\",\"episodes\":[\"19\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('dragon-cave', 'fe-dark-dragon', '龙人洞穴', '通往岛屿内部的天然洞穴系统，钟乳石和地下河构成的危险地形。堕落地龙出没之处。', NULL, '龙人岛深处', '黑暗', '幽深的地下洞穴，发光的苔藓和水晶簇提供微弱照明，地面上有巨大爪痕和鳞片痕迹暗示龙的栖息，地下河发出低沉的水声，狭窄通道通向更深的未知区域，危险神秘氛围，动漫风格', '{\"chapter\":\"第二章\",\"episodes\":[\"7\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('dragon-island-entrance', 'fe-dark-dragon', '龙人岛入口', '佩拉迪龙人岛的岸边入口，奇异的植被和空气中弥漫的魔法气息。', NULL, '龙人岛', '黄昏', '神秘的岛屿岸边，巨大的奇异植物发出微弱的荧光，空气中有肉眼可见的魔力粒子漂浮，远处山体上有古老的龙形雕刻若隐若现，紫红色天空下的奇幻景观，神秘氛围，动漫风格', '{\"chapter\":\"第二章\",\"episodes\":[\"6\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('gra-border', 'fe-dark-dragon', '格拉边境防线', '格拉王国设置的边境防御工事。林克的部队在此阻击义军。', NULL, '格拉王国边境', '白天', '边境军事要塞，木制拒马和壕沟组成的防线后方是格拉军队的营帐，远处可见格拉国旗（黑底金纹），天空中有侦查骑兵的尘土扬起，对峙的紧张感，动漫风格', '{\"chapter\":\"第四章\",\"episodes\":[\"11\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('gra-throne-room', 'fe-dark-dragon', '格拉王宫大殿', '暴君格拉国王的王座厅。林克弑父的悲剧发生地。', NULL, '格拉王城', '夜晚', '奢华但令人窒息的王座大厅，黑色大理石柱支撑着高耸的天花板，金丝织就的地毯通向尽头的王座，两侧点燃的长明灯投下摇曳的光影，王座上的暴君身影被阴影吞噬，沉重压抑的戏剧性空间，动漫风格', '{\"chapter\":\"第四章\",\"episodes\":[\"12\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('grunia-castle', 'fe-dark-dragon', '格鲁尼亚城堡', '卡缪与马尔斯决斗的城堡大厅。格鲁尼亚的军事要塞核心。', NULL, '格鲁尼亚首都', '黄昏', '坚固的军事堡垒内部，厚重的石墙上挂满了历代名将的铠甲和武器作为装饰，中央是一个圆形的决斗场区域，高处的窗户射入橙红色的夕阳余晖形成丁达尔效应，肃穆庄严的武士道氛围，动漫风格', '{\"chapter\":\"第五章\",\"episodes\":[\"14\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('grunia-plains', 'fe-dark-dragon', '格鲁尼亚平原', '卡缪率领重骑兵军团与义军交战的广阔平原。', NULL, '格鲁尼亚王国', '白天', '一望无际的草原战场，草地上布满了车辙印和马蹄坑，远处地平线上黑压压的重装骑兵方阵正在推进，天空中盘旋的战鹰，史诗级战争场面的壮阔与残酷并存，动漫风格', '{\"chapter\":\"第五章\",\"episodes\":[\"13\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('macedon-camp', 'fe-dark-dragon', '马其顿野战营地', '密涅瓦与马尔斯会面的野外营地。龙骑兵团驻扎之地。', NULL, '马其顿边境', '傍晚', '军旅野营地的景象，数顶大型军用帐篷呈环形布置，中央空地上拴着披甲的战马，远处的篝火上烤着肉食，身穿红色龙骑甲的士兵们擦拭武器或喂马，红色旗帜在风中飘扬，豪迈粗犷的氛围，动漫风格', '{\"chapter\":\"第三章\",\"episodes\":[\"10\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('scene-akaneia-resistance', 'fe-dark-dragon', '阿卡奈亚抵抗军营地', '妮娜公主领导的抵抗军秘密基地。隐藏在森林深处的临时营寨。', NULL, '阿卡奈亚废墟周边', '夜晚', '森林中的秘密营地，多顶帆布帐篷散布在巨树之间，篝火燃烧映照着战士们疲惫的面孔，中央大帐挂着残破的阿卡奈亚国旗，周围设置了暗哨警戒，紧张而团结的氛围，动漫风格', '{\"chapter\":\"第一章\",\"episodes\":[\"5\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('scene-orlean-coast', 'fe-dark-dragon', '奥利安海岸', '奥利安王国的海滨地带，马尔斯一行登陆的地方。沙滩延伸至悬崖峭壁。', NULL, '奥利安王国', '清晨', '广阔的海岸线景观，金色沙滩与高耸的白垩岩崖相接，海浪拍打礁石溅起白色泡沫，远处可见奥利安王城的轮廓，晨光中的薄雾，清新开阔的氛围，动漫风格', '{\"chapter\":\"第一章\",\"episodes\":[\"3\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('scene-orlen-castle', 'fe-dark-dragon', '奥利安王城', '奥利安王国的首都城堡，被马其顿军队围困期间哈丁坚守于此。城内物资紧缺但士气尚存。', NULL, '奥利安王国', '白天', '宏伟的中世纪王城内部庭院，石砌建筑带有奥利安蓝白纹饰旗帜，士兵们在城墙缺口处加固防御，城内平民排队领取稀少的食物，坚毅而压抑的氛围，动漫风格', '{\"chapter\":\"第一章\",\"episodes\":[\"4\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('scene-talis-castle', 'fe-dark-dragon', '塔利斯城', '塔利斯岛的主城，石墙环绕的城堡。帝国军围攻时这里成为最后的防线。', NULL, '塔利斯岛', '黄昏', '中世纪城堡外观，灰白色城墙在夕阳下泛着金光，城门口有守卫和村民慌乱奔跑，远处的天际线有黑烟升起暗示战斗逼近，紧张氛围，动漫风格', '{\"chapter\":\"序章\",\"episodes\":[\"2\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `scenes` VALUES ('scene-talis-village', 'fe-dark-dragon', '塔利斯村', '塔利斯岛的宁静渔村，马尔斯流亡三年的居所。木质房屋沿海岸排列，村民们以捕鱼为生。', NULL, '塔利斯岛', '白天', '中世纪幻想风格的海边小村庄，木制房屋错落排列在岩石海岸上，远处是蔚蓝的大海，几艘渔船停泊在浅湾，炊烟袅袅升起，晴朗的天气，温暖的阳光，动漫风格', '{\"chapter\":\"序章\",\"episodes\":[\"1\"]}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');

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
-- Records of storyboards
-- ----------------------------
INSERT INTO `storyboards` VALUES ('0436611fabff410897f327e45fad40ad', 'demo-drama-001', 1, 31, 47, 'wide', '俯拍拉远', '城市全景，朝阳完全升起，新的一天开始，但城市中隐藏的暗流开始涌动，本集完', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('085b13f5aa0840adbe77b7485e742f77', 'demo-drama-001', 1, 4, 20, 'wide', '摇镜头', '咖啡馆门被推开，一位身着黑色风衣的年轻男子（顾言）步入店内，他摘下墨镜，环顾四周，目光最终落在窗边的林小溪身上', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('1959b42d1e49451a93597432062fee82', 'demo-drama-001', 1, 24, 40, 'wide', '俯拍', '城市另一端，一栋废弃的旧工厂内，神秘男子走入，身后跟着几名手下，他拿出那枚徽章放在桌上', '目标已经出现，按计划行动。', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('20e14bb375d44b5881034f326ba07334', 'demo-drama-001', 1, 27, 43, 'medium', '推镜头', '林小溪翻开笔记本最后一页，上面写着：\"如果有一天我消失了，替我找出真相\"——落款是顾言，日期是五年前', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('22a749dcf34c42ab9878da3faa5b7486', 'demo-drama-001', 1, 9, 9, 'close-up', '固定镜头', '林逸晨将雨伞递给苏雨桐，两人的手不经意间触碰', '不客气', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('235b0e11d0414f749ac6f1e92e975c7b', 'demo-drama-001', 1, 11, 11, 'medium', '固定镜头', '苏雨桐转身离开，回头看了林逸晨一眼，林逸晨也注视着她的背影', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('5050416050784c68952624d0ff5218cd', 'demo-drama-001', 1, 12, 12, 'close-up', '特写', '苏雨桐手中的画纸上隐约可见一幅都市风景画，画面中有个模糊的西装男子轮廓', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('5675b269cd9e4c6cbd655016502ff301', 'demo-drama-001', 1, 26, 42, 'wide', '拉镜头', '清晨的城市，朝阳升起，阳光照进林小溪的公寓，她独自站在窗前，手中紧握那本旧笔记本', '（内心独白）五年了，我以为一切都结束了...原来这只是开始。', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('5cb61ed22bc74a988ae95a10c9aeabd3', 'demo-drama-001', 1, 29, 45, 'wide', '固定镜头', '城市街道，车流中一辆黑色轿车内，顾言戴着墨镜坐在后座，窗外是林小溪公寓的方向，手中的手机显示着一条短信：\"游戏开始了\"', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('638a4ed155dd431cb3da6c9053f1aec0', 'demo-drama-001', 1, 22, 38, 'wide', '固定镜头', '地下通道的灯光突然闪烁，远处传来杂乱的脚步声，林小溪和顾言警惕地对视', '（远处脚步声）有人来了！快走！', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('68ce0b6ac4144662af3f24000dc73fc9', 'demo-drama-001', 1, 2, 2, 'medium', '手持跟随', '男主角林逸晨步履匆忙地走在人行道上，身穿黑色西装，背着公文包，手里拿着咖啡，时不时查看手表', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('6dc51d69727b4743b3f1c6a1a5720322', 'demo-drama-001', 1, 8, 8, 'medium', '固定镜头', '苏雨桐站稳身体，捡起散落的画纸，林逸晨帮她捡起掉落的雨伞', '谢谢', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('6ed8eb5defb04ba1b56ac4ca2c1ecf61', 'demo-drama-001', 1, 12, 28, 'extreme-close-up', '特写推进', '顾言手腕上露出一道深深的疤痕特写，那是五年前留下的痕迹，林小溪看到后脸色大变', '这疤痕...那是当年...', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('70616a7583e64a359326bf6b1a657ec2', 'demo-drama-001', 1, 5, 5, 'close-up', '快速跟拍', '林逸晨刚好走到斑马线中央，看到电动车冲向苏雨桐，他迅速伸手抓住苏雨桐的胳膊将她拉回路边', '小心！', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('740c9890119841d0ae70ce719e6a59a8', 'demo-drama-001', 1, 16, 16, 'wide', '航拍俯视', '都市的全景，车流涌动，人群熙攘，每个人都在自己的命运轨道上前行', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('75ff66d287d34bdab72ed0731d1dcfa1', 'demo-drama-001', 1, 1, 17, 'wide', '缓慢推进', '城市高空远景，俯瞰繁华都市夜景，车流如织，灯光闪烁，镜头缓缓下降扫过高楼林立的CBD区域', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('7952f4bf269e412c8c794ee121e56f5d', 'demo-drama-001', 1, 19, 35, 'wide', '快速推进', '城市的地下通道内，顾言和林小溪快速奔逃，脚步声在空旷的通道中回响，前方出现分岔口', '（林小溪气喘吁吁）顾言！你到底在躲什么？\\n（顾言）躲得不是我，是真相...五年前的事故不是意外！', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('7abe8407a152417492cd6fcd4f07dc61', 'demo-drama-001', 1, 14, 14, 'close-up', '特写', '林逸晨接起手机，屏幕显示\"公司秘书\"', '林总，会议还有十分钟开始', NULL, 'demo-scene-003', NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('7b4a57ba50174caf87c4d29b02bcd893', 'demo-drama-001', 1, 11, 27, 'medium', '固定镜头', '顾言停在林小溪面前三步之遥，表情复杂，欲言又止，眼神中闪过痛苦、思念和一丝难以捉摸的情绪', '小溪...好久不见。', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('7facac3682e94b97ad070e1c7d46e7b1', 'demo-drama-001', 1, 16, 32, 'wide', '拉镜头', '顾言快步走向咖啡馆后门，回头眼神示意林小溪跟上，林小溪犹豫片刻后快步跟上，两人的身影消失在后门', '（顾言低声）跟我来，我会解释一切...', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('80d5966f77db4582afb7b3b882fe14ff', 'demo-drama-001', 1, 10, 10, 'wide', '缓慢拉出', '清晨的阳光照在两人身上，周围是匆忙过路的行人，车流再次流动', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('87f38dabdf9f43d783f2e3eaf778f206', 'demo-drama-001', 1, 20, 36, 'medium', '固定镜头', '林小溪停下脚步，一把拉住顾言，神情坚定：什么真相？五年前到底发生了什么，你必须告诉我！', '什么真相？五年前到底发生了什么，你必须告诉我！', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('8a775078a88f4914bf2558330eaa9c17', 'demo-drama-001', 1, 7, 7, 'close-up', '特写缓慢拉出', '林逸晨低头看着怀中的苏雨桐，两人的目光第一次交汇，时间仿佛静止', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('8b5b2ccd01ca46b5bdc48f0ef1b1f6d2', 'demo-drama-001', 1, 8, 24, 'extreme-close-up', '固定特写', '林小溪手中的笔记本特写，照片中年轻男子的面容与五年后的顾言逐渐重叠，她的眼眶泛红，泪水在眼角凝聚', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('8cda385b3e2c489eb0e0b35be2961152', 'demo-drama-001', 1, 23, 39, 'medium', '快速跟拍', '顾言拉着林小溪向左边的通道跑去，身后脚步声越来越近，画面渐暗，只剩下奔跑的脚步声和急促的呼吸声', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('9247d03c06664287a0ffe242bb702171', 'demo-drama-001', 1, 6, 6, 'medium', '固定镜头', '苏雨桐被拉到路边，身体踉跄着倒入林逸晨怀中，画板包掉落在地上，画纸散落出来', '啊！', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('93cfadc4dd0b412bae24e8cd87972c42', 'demo-drama-001', 1, 18, 34, 'extreme-close-up', '特写', '神秘男子弯腰捡起地上的一枚徽章，徽章上刻着一个神秘的符号，他嘴角露出冷笑', '（冷笑）找到了...', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('963c07337a2f494a8b508eddcef36b65', 'demo-drama-001', 1, 1, 1, 'wide', '航拍缓慢推进', '清晨的都市全景，高楼林立，街道上车流如织，阳光透过玻璃幕墙在建筑表面反射出耀眼的光芒', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('9a5e269f4e1b4f5fa612215edacd6621', 'demo-drama-001', 1, 3, 19, 'medium', '推镜头', '林小溪翻开笔记本，页面上显示着一张模糊的老照片和几行手写文字，她的手指轻轻抚过照片，眼神变得复杂', '（内心独白）五年了，你到底在哪里...', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('a507c348fc68426a82900604d8810028', 'demo-drama-001', 1, 14, 30, 'close-up', '快速推镜头', '顾言迅速转头看向窗外，眼神警惕，低声说道：我们不能在这里说话，有人一直在跟踪我。', '我们不能在这里说话，有人一直在跟踪我。', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('a711b0c093ae4199b2d0f73f103635a0', 'demo-drama-001', 1, 5, 21, 'medium', '固定镜头', '顾言站在原地，身体微微一顿，显然认出了窗边的女子，手不自觉地握紧门把手，呼吸变得急促', '（内心独白）是她...居然是她...', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('b598dc527f9c4c27939f7092dfc03814', 'demo-drama-001', 1, 25, 41, 'extreme-close-up', '特写', '徽章特写，上面刻着与五年前车祸现场发现的同一符号，画面定格在此', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('bf1055d695594fc0988dd7c88be68862', 'demo-drama-001', 1, 28, 44, 'close-up', '固定特写', '林小溪的眼神从困惑逐渐变得坚定，她轻声自语：我会找出真相。', '（坚定地）我会找出真相。', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('c5f5aa045ad14067a897752b98b54eac', 'demo-drama-001', 1, 13, 29, 'medium', '360度环摇', '咖啡馆窗外，雨势变大，雨水模糊了玻璃，店内灯光摇曳，营造出悬疑压抑的氛围，远处仿佛有人影闪过', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('c93476a982664d89a342a96875c8cae6', 'demo-drama-001', 1, 17, 33, 'medium', '固定镜头', '咖啡馆前门，一位戴着帽子的神秘男子走入，四处张望后在顾言刚才站立的位置停下，地上有一滴血迹', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('cde6816741ad4dbd8a85730e69b35f4e', 'demo-drama-001', 1, 9, 25, 'medium', '跟拍镜头', '顾言深吸一口气，迈步向林小溪走去，每一步都显得沉重而犹豫，周围的顾客仿佛都消失了，整个咖啡馆只剩下他们两人', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('d1633e576ebb489ca99bc0ae18b83f31', 'demo-drama-001', 1, 4, 4, 'medium', '推镜头', '绿灯亮起，苏雨桐抬脚准备过马路，这时一辆电动车突然从右侧冲出', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('d1a884679b5342a9a76e70a608a6fa71', 'demo-drama-001', 1, 30, 46, 'extreme-close-up', '特写', '顾言手机屏幕特写，信息内容：\"游戏开始了\"，发送号码是一串乱码', '（顾言轻叹）终究还是把你卷入了...', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('d944135bc880411e8df0a04ae0d0fa71', 'demo-drama-001', 1, 21, 37, 'close-up', '拉镜头', '顾言转身面向林小溪，表情痛苦纠结，缓缓开口：五年前那场车祸...我父母不是意外死亡，他们是被人...（话未说完）', '五年前那场车祸...我父母不是意外死亡，他们是被人...', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('dbde1d8fd4234fb2b65c34104731e468', 'demo-drama-001', 1, 13, 13, 'medium', '手持跟随', '林逸晨目送苏雨桐消失在下班高峰的人流中，这时他的手机响起', '（手机铃声）', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('e45c355cd1774b918e3992e631a43784', 'demo-drama-001', 1, 15, 31, 'medium', '固定镜头', '林小溪一脸震惊和困惑，但很快恢复冷静，低头看了看手中的笔记本，又抬头看向顾言', '跟踪你？五年前你突然失踪，现在又说被人跟踪...这到底是怎么回事？', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('e5524f678a8e49bb82fbedd772a6d0b9', 'demo-drama-001', 1, 6, 22, 'close-up', '特写推进', '林小溪似乎感应到什么，缓缓抬起头，目光与顾言交汇，时间仿佛在这一刻静止，两人都是一脸震惊', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('ee9af9b2cfdc4edfa0b1d5a257b5ab26', 'demo-drama-001', 1, 15, 15, 'medium', '固定镜头', '林逸晨接电话的同时，目光再次望向苏雨桐离开的方向', '好，我马上回去', NULL, 'demo-scene-003', NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('f2d63fa4824043c7b2f10e24f0eecd25', 'demo-drama-001', 1, 3, 3, 'wide', '固定镜头', '女主角苏雨桐站在十字路口斑马线一端，身穿白色连衣裙，背着画板包，手中撑着蓝色雨伞，目光望向红灯', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:36:31', '2026-04-16 22:36:31');
INSERT INTO `storyboards` VALUES ('f8dee2d002b14b63b082252ee1527fb2', 'demo-drama-001', 1, 2, 18, 'wide', '固定镜头', '咖啡馆内景，窗边座位，一位年轻女子（林小溪）坐在窗边，手中握着一本旧式笔记本，望着窗外雨景，眼神中带着一丝忧虑和期待', '', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('fac6e6aa1ded43f4bd2aefc09510247b', 'demo-drama-001', 1, 10, 26, 'close-up', '拉镜头', '林小溪猛然站起，椅子被撞倒发出声响，她难以置信地后退一步，手中紧握的笔记本微微颤抖', '真的是你...顾言？你还活着？', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('ff2894200a944761b00f57d90890b15b', 'demo-drama-001', 1, 7, 23, 'medium', '叠化', '回忆片段：五年前的雨夜，同样是这家咖啡馆门前，年轻版的顾言和林小溪在雨中追逐，两人的手几乎要触碰在一起，却被一辆突然驶过的汽车分开', '（画外音女）顾言！不要走！\\n（画外音男）小溪，等我回来！', NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, 0, '2026-04-16 22:37:48', '2026-04-16 22:37:48');
INSERT INTO `storyboards` VALUES ('sb-ep01-s01', 'fe-dark-dragon', 1, 1, 1, 'wide', '航拍俯冲', '镜头从高空俯瞰塔利斯岛全景，宁静的渔村炊烟袅袅。突然远处海面出现多艘海盗船逼近。', NULL, NULL, 'scene-talis-village', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"宁静到紧张\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s02', 'fe-dark-dragon', 1, 1, 2, 'medium', '手持晃动', '村民们四散奔逃，海盗士兵已跳上岸挥舞弯刀砍向房屋，火焰蔓延。', '村民A：海盗来了！快跑！！', NULL, 'scene-talis-village', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"恐慌混乱\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s03', 'fe-dark-dragon', 1, 1, 3, 'medium', '正面推镜', '希达公主穿着便服气喘吁吁地跑进来，抓住马尔斯的手臂。', '希达：马尔斯！求求你！村民们需要你的帮助！', 'char-shiida', 'scene-talis-village', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"紧急恳求\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s04', 'fe-dark-dragon', 1, 1, 4, 'close-up', '侧面特写', '马尔斯眼神从犹豫变为坚定，握紧拳头，斗篷在风中扬起。', '马尔斯：谢刚、凯因！召集所有人——我们不能再躲藏了。', 'char-mars', 'scene-talis-village', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"觉醒决心\",\"key_moment\":true}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s05', 'fe-dark-dragon', 1, 1, 5, 'wide', '横向移动', '马尔斯持剑冲锋在前，谢刚掩护侧翼，凯因后方射箭支援。激烈战斗场面。', '马尔斯：为了塔利斯！为了所有庇护我们的人！', 'char-mars', 'scene-talis-village', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"热血激昂\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s06', 'fe-dark-dragon', 1, 1, 6, 'medium', '缓慢后拉', '战斗结束后马尔斯收剑入鞘，希达站在不远处看着他。', '希达：（独白）这个人……和传闻中的王子不一样。', 'char-shiida', 'scene-talis-village', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"温情萌芽\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s07', 'fe-dark-dragon', 1, 2, 1, 'wide', '远景固定', '塔利斯城外地平线上帝国军方阵推进，无数军旗猎猎作响。', '守卫长官：帝国军主力……超过三千人！', NULL, 'scene-talis-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"绝望压迫\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s08', 'fe-dark-dragon', 1, 2, 2, 'medium', '手持晃动', '国王书房内一片混乱，地图被打翻，国王看着窗外火光。', '塔利斯国王：他们是来抓马尔斯的。如果他落入帝国手中……希望就没了。', 'char-talis_king', 'scene-talis-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"紧迫决策\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s09', 'fe-dark-dragon', 1, 2, 3, 'wide', '快速推进', '夜色中马尔斯率小队突围，箭矢从身后飞来，他挥剑格挡护送希达前进。', '马尔斯：希达！别回头！往前走！', 'char-mars', 'scene-talis-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"生死逃亡\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s10', 'fe-dark-dragon', 1, 2, 4, 'close-up', '正反打', '码头边国王将戒指交给马尔斯，船只已备好。', '塔利斯国王：这艘船送你们去奥利安。马尔斯……为每一个受苦的人去解放它吧。', 'char-talis_king', 'scene-talis-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"庄重嘱托\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep01-s11', 'fe-dark-dragon', 1, 2, 5, 'wide', '航拍拉升', '月光下海面航行，马尔斯和希达并肩站在船头。镜头拉高船只变成小点。', '马尔斯：再见塔利斯……你好阿卡奈亚。', 'char-mars', 'scene-talis-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"史诗启程\",\"episode_end\":true}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s01', 'fe-dark-dragon', 2, 3, 1, 'wide', '固定远景', '船只在晨光中抵达奥利安海岸，白垩岩崖和金色沙滩映入眼帘。马尔斯跳上沙滩。', NULL, NULL, 'scene-orlean-coast', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"新大陆\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s02', 'fe-dark-dragon', 2, 3, 2, 'medium', '侧面跟拍', '马尔斯一行在海岸边遭遇马其顿巡逻队，双方拔出武器对峙。', '巡逻队长：站住！这片海岸已经被多鲁亚帝国接管了！', NULL, 'scene-orlean-coast', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"首次交锋\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s03', 'fe-dark-dragon', 2, 3, 3, 'wide', '快速剪辑', '战斗快速展开——凯因射倒前排敌人、马尔斯突进斩杀指挥官。巡逻队溃散。', '凯因：这就是帝国的精锐？太弱了吧！', 'char-cain', 'scene-orlean-coast', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"速胜\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s04', 'fe-dark-dragon', 2, 3, 4, 'medium', '推镜', '俘虏的敌军士兵跪地求饶，透露奥利安王城被围困的情报。', '俘虏：求饶命！奥利安王城……被马其顿军团围了一个月了！城里快撑不住了！', NULL, 'scene-orlean-coast', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"情报获取\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s05', 'fe-dark-dragon', 2, 4, 1, 'wide', '航拍俯冲', '奥利安王城全景——城墙残破，外围布满马其顿军营，烟柱从城中升起。', NULL, NULL, 'scene-orlen-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"围城压迫\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s06', 'fe-dark-dragon', 2, 4, 2, 'medium', '手持跟拍', '马尔斯率义军从侧翼发起突袭，突破马其顿包围圈。混乱中箭矢横飞。', '马尔斯：哈丁！我们是来救你们的！坚持住！', 'char-mars', 'scene-orlen-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"突围激战\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s07', 'fe-dark-dragon', 2, 4, 3, 'close-up', '正反打', '城门口马尔斯与哈丁汇合。两人互相打量对方——未来的战友初次见面。', '哈丁：你就是马尔斯？……比我想象中的年轻。但你的剑术我认可。', 'char-hardin', 'scene-orlen-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"英雄相惜\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s08', 'fe-dark-dragon', 2, 4, 4, 'wide', '后拉', '马其顿指挥官被击败后剩余敌军撤退。城门大开，奥利安士兵欢呼涌出。', '士兵们：万岁！援军来了！奥利安得救了！', NULL, 'scene-orlen-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"胜利解放\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s09', 'fe-dark-dragon', 2, 4, 5, 'medium', '缓慢横移', '王城内哈丁向马尔斯介绍奥利安的情况，地图铺在桌上。', '哈丁：马其顿不是唯一的威胁。整个阿卡奈亚大陆都在帝国的铁蹄之下。要赢，我们需要更多的盟友。', 'char-hardin', 'scene-orlen-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"战略规划\"}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep02-s10', 'fe-dark-dragon', 2, 4, 6, 'close-up', '特写', '马尔斯看着地图上标注的各国位置，眼神坚定。', '马尔斯：那我们就一个一个去解放。从现在开始——奥利安就是我们第一个根据地。', 'char-mars', 'scene-orlen-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"决心确立\",\"episode_end\":true}', 0, '2026-04-16 21:59:46', '2026-04-16 21:59:46');
INSERT INTO `storyboards` VALUES ('sb-ep03-s01', 'fe-dark-dragon', 3, 5, 1, 'medium', '推进', '森林深处秘密营地，妮娜公主站在中央大帐前迎接马尔斯一行。她身后是残破的阿卡奈亚国旗。', NULL, NULL, 'scene-akaneia-resistance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"神秘相遇\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep03-s02', 'fe-dark-dragon', 3, 5, 2, 'close-up', '正面特写', '妮娜的面部特写——金发蓝眼，表情庄重而疲惫。她直视马尔斯的眼睛。', '妮娜：你就是安里王室最后的血脉……马尔斯王子。我是阿卡奈亚的妮娜。', 'char-nina', 'scene-akaneia-resistance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"正式介绍\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep03-s03', 'fe-dark-dragon', 3, 5, 3, 'medium', '过肩镜头', '妮娜讲述阿卡奈亚灭亡的经过。闪回画面叠加在她的面部轮廓上——燃烧的城市、逃难的人民。', '妮娜：那一夜……火焰吞没了整座王城。父王母后……所有的臣民……只有我们少数人逃了出来。', 'char-nina', 'scene-akaneia-resistance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"悲剧回忆\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep03-s04', 'fe-dark-dragon', 3, 5, 4, 'close-up', '反应镜头', '马尔斯听完后的表情——同情与愤怒交织。他握紧拳头。', '马尔斯：（低声）帝国会为这一切付出代价的。我向你保证，妮娜公主。', 'char-mars', 'scene-akaneia-resistance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"共情与承诺\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep03-s05', 'fe-dark-dragon', 3, 5, 5, 'medium', '中景', '妮娜从怀中取出火焰纹章徽盾——古老的封印之盾散发着微弱的光芒。', NULL, 'char-nina', 'scene-akaneia-resistance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"圣物展示\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep03-s06', 'fe-dark-dragon', 3, 5, 6, 'close-up', '特写', '妮娜将火焰纹章递向马尔斯。', '妮娜：这是火焰纹章……阿卡奈亚王室代代相传的封印之盾。它能增幅神器的力量、压制暗黑之力。现在——它属于你了。', 'char-nina', 'scene-akaneia-resistance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"权力交接\",\"key_moment\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep03-s07', 'fe-dark-dragon', 3, 5, 7, 'wide', '后拉', '马尔斯接过火焰纹章，周围所有人（希达、谢刚、哈丁等）注视着这一刻。营地上空星光初现。', '妮娜：带着它去吧，马尔斯。让火焰之光照亮整个大陆——就像它曾经照亮阿卡奈亚一样。', 'char-nina', 'scene-akaneia-resistance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"史诗传承\",\"episode_end\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s01', 'fe-dark-dragon', 4, 6, 1, 'wide', '航拍', '龙人岛的奇异景观——荧光植物遍布岸边，紫红色天空下空气中有魔法粒子飘浮。马尔斯一行踏上岛屿。', NULL, NULL, 'dragon-island-entrance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"奇幻异境\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s02', 'fe-dark-dragon', 4, 6, 2, 'medium', '低角度仰拍', '玛奴从岩石后方现身——半人半龙的异族特征清晰可见（竖瞳尖耳），周身环绕着龙气，神情愤怒。', NULL, 'char-manu', 'dragon-island-entrance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"紧张对峙\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s03', 'fe-dark-dragon', 4, 6, 3, 'close-up', '正反打', '玛奴质问人类闯入者，声音中带着古老种族的威严。', '玛奴：人类……你们总是带来战争和毁灭。这片土地不欢迎你们。', 'char-manu', 'dragon-island-entrance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"种族冲突\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s04', 'fe-dark-dragon', 4, 6, 4, 'medium', '平视', '马尔斯没有拔剑而是放下武器，以诚意回应玛奴的敌意。', '马尔斯：我不是来征服的。我来这里是为了寻找对抗暗黑力量的方法——为了所有生灵，包括龙人族。', 'char-mars', 'dragon-island-entrance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"化解敌意\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s05', 'fe-dark-dragon', 4, 6, 5, 'close-up', '特写玛奴的表情变化', '玛奴眼中的愤怒逐渐消退，取而代之的是审视和一丝惊讶。', '玛奴：（独白）这个人类……他的眼中没有贪婪。只有……纯粹的意志。', 'char-manu', 'dragon-island-entrance', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"态度转变\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s06', 'fe-dark-dragon', 4, 7, 1, 'wide', '环境展示', '进入龙人洞穴内部——幽深的空间里发光苔藓和水晶簇提供照明，远处传来低沉的咆哮声。', NULL, NULL, 'dragon-cave', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"危险探索\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s07', 'fe-dark-dragon', 4, 7, 2, 'wide', '快速摇摄', '堕落地龙从黑暗中扑出！巨大的身躯和利爪撕裂空气。马尔斯和玛奴联手迎战。', NULL, NULL, 'dragon-cave', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"突发战斗\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s08', 'fe-dark-dragon', 4, 7, 3, 'medium', '动作跟随', '哥多从洞穴深处出现——白发长者手持发光法杖释放神圣魔法驱散堕地龙。龙人战士形态威严。', NULL, 'char-gotoh', 'dragon-cave', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"强力支援\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s09', 'fe-dark-dragon', 4, 7, 4, 'close-up', '正面特写', '哥多注视着马尔斯，千年智慧的目光仿佛看穿了一切。', '哥多：你就是那个人……预言中的少年。命运之轮已经开始转动了。', 'char-gotoh', 'dragon-cave', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"预言揭示\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep04-s10', 'fe-dark-dragon', 4, 7, 5, 'wide', '后拉', '洞穴深处，哥多向众人示意前方有更深的道路。马尔斯回头看向洞口方向的光明。', NULL, 'char-gotoh', 'dragon-cave', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"新的使命\",\"episode_end\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s01', 'fe-dark-dragon', 6, 11, 1, 'wide', '横向移动', '格拉边境防线前两军对峙。义军在左侧，格拉军队在右侧。林克骑马站在格拉阵线最前方。', NULL, NULL, 'gra-border', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"内战前夕\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s02', 'fe-dark-dragon', 6, 11, 2, 'medium', '长焦压缩', '林克的面部特写——表情矛盾痛苦。他的手握住剑柄又松开反复多次。', NULL, 'char-link', 'gra-border', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"内心挣扎\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s03', 'fe-dark-dragon', 6, 11, 3, 'medium', '正面对话', '战斗爆发前的短暂停火。马尔斯骑马上前与林克对话。两人之间隔着一段距离。', '马尔斯：林克王子！你的父亲背叛了阿卡奈亚——但你不一定要继承他的罪孽！', 'char-mars', 'gra-border', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"劝降\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s04', 'fe-dark-dragon', 6, 12, 1, 'wide', '俯拍', '格拉王宫大殿内部全景。黑金色调的奢华空间令人窒息。王座上的国王身影模糊不清。', NULL, NULL, 'gra-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"压抑王宫\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s05', 'fe-dark-dragon', 6, 12, 2, 'close-up', '侧光特写', '格拉国王的脸——肥胖、贪婪、暴戾。他正在大笑着下达残酷的命令。', '格拉国王：杀掉他们！把那个叛徒马尔斯的头挂到城门上！谁敢后退一律斩立决！', 'char-gra_king', 'gra-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"暴君嘴脸\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s06', 'fe-dark-dragon', 6, 12, 3, 'medium', '缓慢推镜', '林克独自走进大殿。他的脚步声在大理石地面回响。两侧侍从纷纷退避。', NULL, 'char-link', 'gra-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"孤身赴会\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s07', 'fe-dark-dragon', 6, 12, 4, 'close-up', '正反打交替', '林克与父亲的对峙——全剧最沉重的对话场景。', '林克：父亲……你当年出卖阿卡奈亚的时候想过今天吗？', 'char-link', 'gra-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"父子对决\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s08', 'fe-dark-dragon', 6, 12, 5, 'extreme-close-up', '极端特写', '格拉国王狂怒的表情转为惊愕——林克的剑已刺入他的胸膛。慢镜头中鲜血滴落大理石地面。', '格拉国王：你……你这个逆子……！！', 'char-gra_king', 'gra-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"弑父悲剧\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s09', 'fe-dark-dragon', 6, 12, 6, 'medium', '高角度俯拍', '林克跪在父亲的尸体旁。剑从他手中滑落发出金属撞击声。他没有哭但肩膀剧烈颤抖。', '林克：（低声几乎听不见）原谅我……父亲。这是……格拉最后的救赎。', 'char-link', 'gra-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"悲剧余韵\",\"key_moment\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep06-s10', 'fe-dark-dragon', 6, 12, 7, 'wide', '缓慢后拉', '大殿门口马尔斯和众人在沉默中注视着这一幕。林克站起转身面向他们——脸上带着一种死寂后的坚定。', '林克：格拉军队……从现在开始听从马尔斯殿下的命令。这是我唯一能做的补偿。', 'char-link', 'gra-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"赎罪归顺\",\"episode_end\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s01', 'fe-dark-dragon', 7, 13, 1, 'wide', '史诗级广角', '格鲁尼亚平原上一望无际的重骑兵方阵正向义军推进。地面的震动通过镜头传递给观众。战鹰在天空盘旋。', NULL, NULL, 'grunia-plains', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"史诗大战\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s02', 'fe-dark-dragon', 7, 13, 2, 'medium', '侧面跟随', '卡缪骑黑马冲在最前方——冰蓝色短发、冷漠眼神、黑色重甲。他是\"格鲁尼亚之狼\"。', NULL, 'char-camus', 'grunia-plains', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"强敌登场\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s03', 'fe-dark-dragon', 7, 13, 3, 'wide', '快速剪辑', '双方骑兵碰撞的瞬间——金属撞击声、马嘶声、战士怒吼交织在一起。混乱激烈的骑兵战。', NULL, NULL, 'grunia-plains', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"激战高潮\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s04', 'fe-dark-dragon', 7, 13, 4, 'close-up', '慢动作特写', '卡缪与马尔斯在乱军中擦身而过。两人的武器相撞迸出火花。时间仿佛在这一刻凝固。', NULL, 'char-mars', 'grunia-plains', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"宿命交锋\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s05', 'fe-dark-dragon', 7, 14, 1, 'wide', '环境展示', '格鲁尼亚城堡大厅内部——历代名将的铠甲和兵器作为装饰挂在石墙上。中央圆形区域是决斗场。夕阳透过高窗射入。', NULL, NULL, 'grunia-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"肃穆决斗场\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s06', 'fe-dark-dragon', 7, 14, 2, 'medium', '环绕拍摄', '卡缪和马尔斯面对面站立。周围所有人退开形成一个圆圈。只有他们两个和彼此的武器。', NULL, 'char-camus', 'grunia-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"决战氛围\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s07', 'fe-dark-dragon', 7, 14, 3, 'close-up', '快速剪辑', '一对一决斗——剑与枪的交锋。每一击都精准致命。汗水飞溅、呼吸急促、眼神锐利如刀。', '卡缪：不错的剑术……但你还是太年轻了。', 'char-camus', 'grunia-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"技艺切磋\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s08', 'fe-dark-dragon', 7, 14, 4, 'extreme-close-up', '极端特写', '决胜一瞬间——马尔斯找到卡缪防御的破绽将剑尖停在对方的喉咙前。', NULL, 'char-mars', 'grunia-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"胜负分晓\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s09', 'fe-dark-dragon', 7, 14, 5, 'medium', '反应镜头', '马尔斯收回剑。他伸出手想拉起卡缪——不是羞辱而是尊重。', '马尔斯：我不杀值得尊敬的对手。卡缪……你的忠诚应该属于更好的主人。', 'char-mars', 'grunia-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"饶恕与尊重\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep07-s10', 'fe-dark-dragon', 7, 14, 6, 'wide', '后拉', '卡缪看着马尔斯伸出的手片刻后转身离去。他的背影消失在暗处——假死隐退的开始。', NULL, 'char-camus', 'grunia-castle', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"神秘退场\",\"key_moment\":\"卡缪假死伏笔\",\"episode_end\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s01', 'fe-dark-dragon', 8, 15, 1, 'wide', '缓慢推进', '黎明时分的阿利提亚边境。古道穿过丘陵通向远方被战火熏黑的天际线。晨雾中马尔斯停下脚步。', NULL, NULL, 'altea-border', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"归乡感伤\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s02', 'fe-dark-dragon', 8, 15, 2, 'close-up', '侧面特写', '马尔斯的侧脸——眼眶微微泛红但强忍着不流露。他的手抚摸着路边残破的界碑。', '马尔斯：（低声）三年了……我终于回来了，父亲。', 'char-mars', 'altea-border', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"情感爆发边缘\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s03', 'fe-dark-dragon', 8, 15, 3, 'medium', '过肩镜头', '希达从身后轻轻握住马尔斯的手。两人并肩望向阿利提亚的方向。', NULL, 'char-shiida', 'altea-border', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"陪伴与支持\",\"romantic_beat\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s04', 'fe-dark-dragon', 8, 16, 1, 'wide', '快速推镜', '阿利提亚王城攻防战——义军从三面发起总攻。城墙上帝国守军顽强抵抗。投石机和箭塔齐发。', NULL, NULL, 'altea-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"复国决战\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s05', 'fe-dark-dragon', 8, 16, 2, 'wide', '航拍俯冲', '马尔斯率突击队率先登上城墙缺口！旗帜在风中展开——阿利提亚的蓝银色国旗重新飘扬在城头。', NULL, 'char-mars', 'altea-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"夺旗时刻\",\"key_moment\":\"收复王城\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s06', 'fe-dark-dragon', 8, 16, 3, 'medium', '缓慢推进', '战斗结束后的王座大厅。阳光透过彩色玻璃穹顶洒下斑驳光影。地上摆放着鲜花和祭品——祭奠先王。', NULL, NULL, 'altea-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"肃穆哀悼\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s07', 'fe-dark-dragon', 8, 16, 4, 'close-up', '正面特写', '马尔斯跪在祭品前的背影。他低下头肩膀微微颤抖。', NULL, 'char-mars', 'altea-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"悼念父亲\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s08', 'fe-dark-dragon', 8, 16, 5, 'medium', '后拉', '马尔斯站起转身面对所有战友们。阳光照在他脸上。', NULL, 'char-mars', 'altea-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"宣告复国\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep08-s09', 'fe-dark-dragon', 8, 16, 6, 'wide', '环绕拉升', '所有角色齐聚王座大厅——马尔斯、希达、妮娜、密涅瓦、哈丁、林克……每个人脸上都带着不同的表情（坚定/欣慰/期待）。', NULL, NULL, 'altea-throne-room', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"全员集结\",\"episode_end\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s01', 'fe-dark-dragon', 9, 17, 1, 'wide', '缓慢推进', '卡达因神殿内景——纯白石柱直达穹顶，地面刻满发光的神圣符文。空气中有金色尘埃在光束中漂浮。', NULL, NULL, 'kadain-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"神圣庄严\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s02', 'fe-dark-dragon', 9, 17, 2, 'wide', '仰拍特写', '殿堂尽头的高台上——一把剑悬浮在空中散发着柔和的光芒。那就是法尔西昂，光之圣剑。', NULL, NULL, 'kadain-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"神器显现\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s03', 'fe-dark-dragon', 9, 17, 3, 'close-up', '反应镜头', '马尔斯走向法尔西昂。当他伸手触碰剑柄时整个大殿的光芒瞬间暴涨然后稳定下来。剑选择了他。', NULL, 'char-mars', 'kadain-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"神器认主\",\"key_moment\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s04', 'fe-dark-dragon', 9, 17, 4, 'medium', '正反打', '妮娜将火焰纹章放在法尔西昂旁边。两者产生共鸣——封印之盾增幅了光之剑的力量。', NULL, 'char-nina', 'kadain-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"双神器共鸣\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s05', 'fe-dark-dragon', 9, 18, 1, 'wide', '360度旋转', '龙人圣所——没有天花板只有无尽星河旋涡。哥多站在光芒构成的平台中央等待。', NULL, 'char-gotoh', 'dragon-sanctuary', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"超凡圣地\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s06', 'fe-dark-dragon', 9, 18, 2, 'medium', '缓慢推进', '哥多向马尔斯传授星光魔法。古老的语言从他口中流出化作星点融入马尔斯体内。', NULL, 'char-gotoh', 'dragon-sanctuary', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"魔法传承\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s07', 'fe-dark-dragon', 9, 18, 3, 'close-up', '特写', '马尔斯睁开眼睛——瞳孔中有星光流转。他抬起手掌心凝聚出一团星光。', NULL, 'char-mars', 'dragon-sanctuary', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"力量觉醒\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s08', 'fe-dark-dragon', 9, 18, 4, 'medium', '中景', '哥多解释星光魔法的作用——唯一能破除卡涅夫暗黑屏障的法术。', NULL, 'char-gotoh', 'dragon-sanctuary', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"关键情报\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep09-s09', 'fe-dark-dragon', 9, 18, 5, 'wide', '后拉', '众人站在龙人圣所中准备就绪。马尔斯手持法尔西昂，身上同时有火焰纹章和星光的加持。', '哥多：去吧，星之王马尔斯。你的命运不是逃避——是终结这百年的轮回。', 'char-gotoh', 'dragon-sanctuary', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"最终启程\",\"episode_end\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s01', 'fe-dark-dragon', 10, 19, 1, 'wide', '史诗航拍', '多鲁亚帝国本土全景——焦黑的火山岩地貌，硫磺烟雾喷涌，暗红色天空下义军大军的剪影在地平线上推进。', NULL, NULL, 'dolhr-territory', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"末日征途\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s02', 'fe-dark-dragon', 10, 19, 2, 'wide', '横向快速移动', '义军各部队协同作战突破帝国防线。马其顿龙骑兵空中突袭、格鲁尼亚重骑地面冲锋、奥利安步兵两翼包抄。', NULL, NULL, 'dolhr-territory', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"联合攻势\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s03', 'fe-dark-dragon', 10, 19, 3, 'medium', '跟拍', '马尔斯冲在最前方。法尔西昂在他手中发出耀眼的白光所到之处帝国的黑暗力量纷纷溃散。', '马尔斯：全军听令——目标暗黑神殿！一个不留地推进！！', 'char-mars', 'dolhr-territory', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"势不可挡\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s04', 'fe-dark-dragon', 10, 20, 1, 'wide', '环境展示', '暗黑神殿内部——黑色玄武岩构建的巨大空间。无数蜡烛提供诡异照明。地面绘有召唤法阵。中央祭坛上方悬挂着空间裂缝。', NULL, NULL, 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"终极邪恶场所\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s05', 'fe-dark-dragon', 10, 20, 2, 'low-angle', '仰拍', '卡涅夫站在祭坛上——瘦高身躯裹在黑色长袍中兜帽遮脸，眼中闪烁红光。手中黑水晶法杖散发的黑暗能量形成一道屏障。', NULL, 'char-ganev', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"BOSS登场\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s06', 'fe-dark-dragon', 10, 20, 3, 'medium', '正面冲突', '卡涅夫释放暗黑魔法攻击。马尔斯用法尔西昂格挡但被震退——普通攻击无法穿透屏障。', NULL, 'char-ganev', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"陷入劣势\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s07', 'fe-dark-dragon', 10, 20, 4, 'close-up', '特写觉醒', '马尔斯闭上眼回忆哥多的教诲。当他再次睁眼时瞳孔中的星光暴涨——右手释放出星光魔法！', NULL, 'char-mars', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"关键时刻\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s08', 'fe-dark-dragon', 10, 20, 5, 'wide', '特效爆炸', '星光击中卡涅夫的暗黑屏障——屏障像玻璃一样碎裂！卡涅夫发出惊恐的尖叫。', NULL, 'char-ganev', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"逆转胜利\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s09', 'fe-dark-dragon', 10, 21, 1, 'medium', '缓慢推进', '暗黑神殿最深处的一个囚室。艾莉丝蜷缩在角落——银白发凌乱苍白的脸但眼中还有希望。', NULL, 'char-elis', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"姐弟重逢前奏\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s10', 'fe-dark-dragon', 10, 21, 2, 'close-up', '情感特写', '马尔斯冲进来跪在艾莉斯面前。姐姐看到弟弟的那一刻泪水夺眶而出。', '艾莉斯：马尔斯……真的是你……我的弟弟长大了……', 'char-elis', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"催泪重逢\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s11', 'fe-dark-dragon', 10, 21, 3, 'medium', '拥抱镜头', '姐弟拥抱在一起。背景中其他伙伴默默转身给他们留出空间。', NULL, 'char-mars', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"温情时刻\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s12', 'fe-dark-dragon', 10, 21, 4, 'wide', '极致广角低机位', '暗黑神殿最深处崩裂——巨大的暗黑龙梅迪乌斯从地下升起！体型遮天蔽日，黑色鳞片覆盖全身，红色眼睛如同深渊，漆黑双翼展开几乎填满整个画面。', NULL, 'char-medius', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"终极BOSS降临\",\"scale\":\"史诗级\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s13', 'fe-dark-dragon', 10, 21, 5, 'extreme-wide', '远景对峙', '马尔斯独自面对巨龙的渺小身影。法尔西昂在他手中光芒越来越亮。火焰纹章在他胸口发出共鸣之光。', NULL, 'char-mars', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"宿命对决\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s14', 'fe-dark-dragon', 10, 21, 6, 'wide', '快速剪辑', '史诗级战斗——梅迪乌斯的暗黑吐息 vs 法尔西昂的圣光斩击。每一次碰撞都引发能量冲击波摧毁周围的建筑结构。', NULL, 'char-medius', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"终极激战\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s15', 'fe-dark-dragon', 10, 21, 7, 'slow-motion', '慢动作决胜', '马尔斯跃起到半空——法尔西昂吸收了火焰纹章的全部力量爆发出前所未有的光辉。一击刺入梅迪乌斯的心脏位置！', NULL, 'char-mars', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"终结一击\",\"key_moment\":\"全剧高潮\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s16', 'fe-dark-dragon', 10, 21, 8, 'wide', '延时效果', '梅迪乌斯发出最后的咆哮后身体开始崩解——化为无数黑色的碎片消散在空气中。暗黑神殿开始坍塌但光芒从裂缝中涌入。', NULL, 'char-medius', 'dark-shrine', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"BOSS消亡\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s17', 'fe-dark-dragon', 10, 22, 1, 'wide', '日出航拍', '时间跳跃后的画面——清晨阳光下的阿利提亚城楼顶部。石质垛口上站着两个身影眺望远方苏醒的城市。', NULL, NULL, 'altea-city-wall', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"和平到来\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s18', 'fe-dark-dragon', 10, 22, 2, 'medium', '侧面中景', '马尔斯和希达并肩站着。马尔斯穿着王室礼服不再是铠甲。希达靠在他的肩头。两人的表情平静而幸福。', '希达：终于结束了……对吧？', 'char-shiida', 'altea-city-wall', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"温情结局\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s19', 'fe-dark-dragon', 10, 22, 3, 'medium', '正面对话', '马尔斯转头看向希达微笑。远处城市的钟声响起——新纪元的开始。', '马尔斯：不……这只是新的开始。（看向远方的大陆）还有很多事情要做呢。', 'char-mars', 'altea-city-wall', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"展望未来\"}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');
INSERT INTO `storyboards` VALUES ('sb-ep10-s20', 'fe-dark-dragon', 10, 22, 4, 'wide', '最终拉升后拉', '镜头缓缓升高越过城楼——展现整个阿卡奈亚大陆的鸟瞰图。各国旗帜在风中飘扬，田野绿意盎然，城市炊烟袅袅。画面渐变为标题字幕。', NULL, NULL, 'altea-city-wall', NULL, NULL, NULL, NULL, 'pending', '{\"mood\":\"史诗结局\",\"series_finale\":true}', 0, '2026-04-16 21:59:47', '2026-04-16 21:59:47');

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
