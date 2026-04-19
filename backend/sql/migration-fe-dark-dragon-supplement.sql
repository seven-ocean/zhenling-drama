-- ============================================================
-- 《火焰之纹章：暗黑龙与光之剑》补数SQL
--
-- 缺失数据：
--   角色 x4：谢刚/杰刚、玛奴、梅迪乌斯、阿利提亚先王
--   场景 x4：卡达因神殿、龙人圣所、暗黑神殿、阿利提亚城头
--   分镜 x12：整个EP05 第三章·马其顿转折
--
-- 执行方式：在已有数据的基础上补充执行即可
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、补缺角色（4个）
-- ============================================================

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-jagen', 'fe-dark-dragon',
    '谢刚（杰刚）',
    '塔利斯王国的老骑士，约50岁。马尔斯流亡期间的守护者和导师。忠诚可靠、经验丰富，是马尔斯最信赖的老臣。教导马尔斯战斗技巧和为君之道。',
    NULL, NULL, NULL,
    '年长的骑士形象，灰白短发和胡须，皱纹深刻但眼神锐利，身穿磨损但保养良好的重甲，手持长枪，威严而慈祥的气质，动漫风格',
    '沉稳老练语速较慢，称呼马尔斯为"殿下"，忠言逆耳但充满关爱',
    4,
    '{"role":"mentor","faction":"塔利斯","class":"骑士(Paladin)"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-manu', 'fe-dark-dragon',
    '玛奴（马努）',
    '佩拉迪龙人岛的龙人族战士。起初对人类闯入龙人领地感到愤怒，与马尔斯冲突后认可其使命，透露了龙人族内部矛盾的情报。可化为人形和龙形。',
    NULL, NULL, NULL,
    '神秘的非人类种族形象，银白色长发飘逸，竖立的龙瞳（金色），尖耳朵，身穿带有鳞片纹理的古老服饰，周围隐约有龙气缭绕，半人半龙的奇幻美感，动漫风格',
    '古奥庄重用词考究，带着古老种族的距离感和逐渐消融的戒备',
    11,
    '{"role":"dragon_ally","faction":"龙人族(守序)","ability":"变身龙形"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-medius', 'fe-dark-dragon',
    '梅迪乌斯（暗黑龙）',
    '最终BOSS。远古暗黑龙的化身，每隔百年复活一次企图毁灭人类文明。本体为巨大的黑暗之龙，以多鲁亚帝国为傀儡统治阿卡奈亚大陆。只有法尔西昂能将其彻底消灭——这是系列的宿命轮回核心设定。',
    NULL, NULL, NULL,
    '恐怖的暗黑龙形态，巨大无比的身躯覆盖黑色鳞片，红色发光的眼睛如同深渊，口中喷吐黑暗火焰和闪电，背后展开遮天蔽日的漆黑双翼，周围空间扭曲变形，终极BOSS压迫感，动漫风格',
    '不需要人类语言——通过心灵感应传递毁灭意志，存在本身即是绝望和恐惧',
    13,
    '{"role":"final_boss","faction":"暗黑势力","type":"暗黑龙(复活)","weakness":"法尔西昂(光之圣剑)","motif":"百年轮回"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-alis_king', 'fe-dark-dragon',
    '阿利提亚国王（先王）',
    '马尔斯的父亲，阿利提亚前代国王。帝国入侵时坚守王城直至战死。他的牺牲激励了马尔斯走上复国之路。马尔斯最终在收复的王城中祭奠他。',
    NULL, NULL, NULL,
    '已故的英明君主回忆形象，中年男性的肖像画质感，金色短发梳理整齐，温和而坚定的眼神，身穿完整的王室礼甲，身后是阿利提亚国旗，神圣而哀伤的氛围，动漫风格',
    '仅存在于闪回和回忆中——语调庄重慈爱，对儿子的临别嘱托',
    16,
    '{"role":"deceased_motivator","faction":"阿利提亚","status":"阵亡（帝国入侵时）","relation":{"son":"马尔斯"}}',
    0, NOW(), NOW()
);


-- ============================================================
-- 二、补缺场景（4个）
-- ============================================================

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'kadain-shrine', 'fe-dark-dragon',
    '卡达因神殿',
    '安里王室遗留的神圣殿堂，封印着光之剑法尔西昂。古老而庄严。',
    NULL,
    '卡达因遗迹', '正午',
    '古老的神殿内景，纯白色的石柱直达穹顶，地面刻满发光的神圣符文，殿堂尽头的高台上悬浮着一把散发着柔和光芒的剑（法尔西昂），整个空间充满神圣的金色光辉，超凡脱俗的神性氛围，动漫风格',
    '{"chapter":"第七章","episodes":["17"],"sacred_item":"法尔西昂(光之圣剑)"}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'dragon-sanctuary', 'fe-dark-dragon',
    '龙人圣所',
    '哥多传授星光魔法的神圣空间。龙人族最隐秘的圣地。',
    NULL,
    '龙人岛最深处', '永恒暮光',
    '超越凡间的奇幻圣所，没有明确的天花板——上方是无尽的星河旋涡缓缓转动，脚下是由纯粹光芒构成的平台，四周漂浮着古代龙族的雕像和卷轴，哥多的身影站在星河中心，极致的奇幻神性美感，动漫风格',
    '{"chapter":"第七章","episodes":["18"],"magic_taught":"星光魔法"}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'dark-shrine', 'fe-dark-dragon',
    '暗黑神殿',
    '梅迪乌斯和卡涅夫的据点。最邪恶的场所——暗黑仪式的舞台、最终BOSS战的场地。',
    NULL,
    '多鲁亚帝国核心', '永夜',
    '终极邪恶的殿堂，巨大的空间由黑色玄武岩构建，无数蜡烛和暗红火焰提供诡异照明，地面绘有召唤法阵（发光的暗色纹路），中央是一个升起的祭坛，上方悬挂着扭曲的空间裂缝，绝对的黑暗与恐惧氛围，动漫风格',
    '{"chapter":"第八章","episodes":["20","21"],"final_boss_location":true}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'altea-city-wall', 'fe-dark-dragon',
    '阿利提亚城头（结局）',
    '战后重建的阿利提亚都城城头。马尔斯与希达眺望和平大陆的经典结局画面。',
    NULL,
    '阿利提亚王城', '日出',
    '清晨阳光下的城楼顶部，石质垛口上站着两个身影（远景），前方是正在苏醒的城市——屋顶炊烟升起，街道上行人渐多，远处的田野绿意盎然，金色朝阳从东方地平线洒满整幅画面，温暖治愈的希望之光，动漫风格',
    '{"chapter":"结局","episodes":["22"],"finale_scene":true}',
    0, NOW(), NOW()
);


-- ============================================================
-- 三、补缺分镜 —— EP05：第三章·马其顿转折（12个）
--
-- 关卡8：迪鲁城外围 | 关卡9：迪鲁城救援 | 关卡10：马其顿的抉择
-- ============================================================

INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep05-s01','fe-dark-dragon',5,8,1,'wide','航拍推进','迪鲁城外围的战场废墟。义军部队在烧焦的土地上行军向迪鲁城推进。远处城墙在烟尘中若隐若现。',NULL,NULL,'deil-outer','pending','{"mood":"战争逼近"}',0,NOW(),NOW()),
('sb-ep05-s02','fe-dark-dragon',5,8,2,'medium','侧面跟拍','一名骑马的信使从后方追上马尔斯，递出一封密信。信封上盖着马其顿王室火漆印。','信使：马尔斯殿下！一封来自马其顿公主密涅瓦的密信！是……求援信！',NULL,'deil-outer','pending','{"mood":"意外情报"}',0,NOW(),NOW()),
('sb-ep05-s03','fe-dark-dragon',5,8,3,'close-up','特写信件内容（画外音朗读）','密涅瓦的声音：（画外）如果你能看到这封信……请救救我的妹妹玛莉亚。她被关在迪鲁城的地牢里……我无能为力。',NULL,'deil-outer','pending','{"mood":"悬念铺垫"}',0,NOW(),NOW()),
('sb-ep05-s04','fe-dark-dragon',5,9,1,'wide','仰拍','夜色中迪鲁城的阴森轮廓——黑色石墙插满尖刺，塔楼上火把摇曳。',NULL,NULL,'deil-castle','pending','{"mood":"黑暗要塞"}',0,NOW(),NOW()),
('sb-ep05-s05','fe-dark-dragon',5,9,2,'medium','手持晃动跟拍','马尔斯率突击队潜入城堡内部。走廊狭窄昏暗，巡逻士兵的脚步声回响。',NULL,'char-mars','deil-castle','pending','{"mood":"潜行紧张"}',0,NOW(),NOW()),
('sb-ep05-s06','fe-dark-dragon',5,9,3,'medium','缓慢推入','地牢深处——铁栅栏门后，一个瘦弱的少女身影蜷缩在角落。玛莉亚抬起头眼中含泪看到来人。',NULL,'char-maria','deil-dungeon','pending','{"mood":"悲情相遇"}',0,NOW(),NOW()),
('sb-ep05-s07','fe-dark-dragon',5,9,4,'close-up','特写玛莉亚的脸','玛莉亚颤抖着站起来抓住牢门栏杆。','玛莉亚：你……你是来救我的吗？姐姐……姐姐还好吗？','char-maria','deil-dungeon','pending','{"mood":"脆弱与希望"}',0,NOW(),NOW()),
('sb-ep05-s08','fe-dark-dragon',5,9,5,'wide','快速动作','谢刚用重剑劈开牢门锁。马尔斯扶住虚弱的玛莉亚。外面传来警报声——被发现！',NULL,'char-mars','deil-dungeon','pending','{"mood":"紧急脱出"}',0,NOW(),NOW()),
('sb-ep05-s09','fe-dark-dragon',5,10,1,'medium','正面中景','野战营地中密涅瓦与马尔斯对峙。密涅瓦身穿红色龙骑甲，气场强大但眼神复杂。',NULL,'char-minerva','macedon-camp','pending','{"mood":"微妙会面"}',0,NOW(),NOW()),
('sb-ep05-s10','fe-dark-dragon',5,10,2,'close-up','正反打','密涅瓦看着被救出的妹妹玛莉亚，坚硬的外壳终于裂开一丝缝隙。','密涅瓦：玛莉亚……（深吸一口气转向马尔斯）你救了她。我欠你一次。','char-minerva','macedon-camp','pending','{"mood":"情感软化"}',0,NOW(),NOW()),
('sb-ep05-s11','fe-dark-dragon',5,10,3,'medium','过肩镜头','密涅瓦表达对帝国的不满和对自由的渴望。背景中她的龙骑兵团安静地聆听。','密涅瓦：我曾经相信帝国的理想……但我看到的只有暴政和杀戮。从今天起——我和我的龙骑兵团，站在你这边。','char-minerva','macedon-camp','pending','{"mood":"关键倒戈","key_moment":true}',0,NOW(),NOW()),
('sb-ep05-s12','fe-dark-dragon',5,10,4,'wide','航拍拉升后拉','营地上空，马其顿的红色旗帜被降下取而代之的是义军的联合旗号。镜头越拉越高展现整片大陆的格局变化。',NULL,NULL,'macedon-camp','pending','{"mood":"势力重组","episode_end":true}',0,NOW(),NOW());


-- ============================================================
-- 补数完成统计
--   角色: +4 (char-jagen, char-manu, char-medius, char-alis_king)
--   场景: +4 (kadain-shrine, dragon-sanctuary, dark-shrine, altea-city-wall)
--   分镜: +12 (EP05 全部 sb-ep05-s01~s12)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
