-- ============================================================
-- 《火焰之纹章：暗黑龙与光之剑》完整初始化数据
-- 原文：backend/jq/火焰之纹章：暗黑龙与光之剑.md
-- 生成时间：2026-04-16
--
-- 数据量概览：
--   1 部剧集 | 18 个角色 | 22 个场景 | ~120 个分镜
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一步：创建剧集（1部）
-- ============================================================

INSERT INTO `dramas` (
    `id`, `title`, `description`, `cover_image`, `status`,
    `total_episodes`, `created_episodes`, `settings`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'fe-dark-dragon',
    '《火焰之纹章：暗黑龙与光之剑》',
    '系列原点之作。阿利提亚王子马尔斯在祖国沦陷后流亡三年，从塔利斯岛起兵，联合奥利安、马其顿、格拉、格鲁尼亚等诸国义军，最终击败暗黑神梅迪乌斯，解放阿卡奈亚大陆。包含宿命轮回、神剑封印、龙人族等核心设定。',
    NULL,
    'draft',
    10,
    0,
    '{"genre":"奇幻/战争","era":"中世纪幻想大陆","themes":["宿命与成长","复国与正义","友情与牺牲"],"core_items":["火焰纹章(封印之盾)","法尔西昂(光之圣剑)","星光魔法"]}',
    0,
    NOW(),
    NOW()
);

-- ============================================================
-- 第二步：创建角色（18个）
-- 主角团 + 盟友 + 反派 + 重要NPC
-- ============================================================

-- ---- 核心主角 ----

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-mars', 'fe-dark-dragon',
    '马尔斯',
    '阿利提亚王国王子，16岁。父王被帝国杀害后流亡塔利斯岛三年，后率军复国。性格坚毅、富有领导力但内心善良，是唯一能使用光之圣剑法尔西昂的人（安里血统）。最终成为"星之王"，统一阿卡奈亚大陆。',
    NULL, NULL, NULL,
    '年轻英俊男性王子形象，银白色短发，蓝色眼眸，身穿银蓝色相间的骑士铠甲，披着深红色斗篷，手持法尔西昂光剑，气质高贵而坚毅，动漫风格，精致面部特征',
    '沉稳坚定有担当，对朋友温暖关怀，面对敌人时果敢决断',
    1,
    '{"role":"protagonist","faction":"义军","weapon":"法尔西昂(光之圣剑)","bloodline":"安里"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-shiida', 'fe-dark-dragon',
    '希达（希妲）',
    '塔利斯公主，15岁。天真烂漫却勇敢坚强，在帝国入侵时找到马尔斯求助。对马尔斯一见倾心，是马尔斯最重要的精神支柱之一。最终成为阿利提亚王后。',
    NULL, NULL, NULL,
    '美丽年轻的公主形象，浅绿色长发及腰，碧绿眼睛，身穿白色和淡紫色相间的长裙配银色护甲，头戴小巧的王冠，温柔而坚定的表情，动漫风格',
    '活泼开朗带点娇羞，关心马尔斯时会变得认真，战斗时勇敢无畏',
    2,
    '{"role":"heroine","faction":"塔利斯","relation_to_mars":"恋人→妻子"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-nina', 'fe-dark-dragon',
    '妮娜',
    '阿卡奈亚公主，17岁。阿卡奈亚亡国后的遗孤领袖，率领抵抗军坚持斗争。聪慧冷静，持有火焰纹章（王室代代相传的封印之盾）。将火焰纹章交给马尔斯后成为重要盟友，战后重建阿卡奈亚并即位女王。',
    NULL, NULL, NULL,
    '优雅高贵的公主形象，金色长发编成复杂的辫子，蓝紫色眼眸，身穿深蓝色礼服式铠甲，肩部有阿卡奈亚纹章装饰，手持火焰纹章徽盾，气质成熟冷静，动漫风格',
    '端庄优雅言辞谨慎，作为公主有责任感，偶尔流露孤独和坚韧',
    3,
    '{"role":"ally_leader","faction":"阿卡奈亚抵抗军","item":"火焰纹章"}',
    0, NOW(), NOW()
);

---- 义军核心成员 ----

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
    'char-cain', 'fe-dark-dragon',
    '凯因',
    '塔利斯的弓箭手，18岁。马尔斯早期战友之一，性格开朗热血，是队伍中的气氛担当。擅长远程支援作战。',
    NULL, NULL, NULL,
    '年轻战士形象，棕色短发，身材精干敏捷，身穿轻便皮甲，背着弓箭袋，笑容爽朗自信，动漫风格',
    '热情直爽语速快，喜欢开玩笑但关键时刻靠得住',
    5,
    '{"role":"party_member","faction":"塔利斯","class":"弓箭手(Archer)"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-hardin', 'fe-dark-dragon',
    '哈丁',
    '奥利安王国王弟，20岁。马其顿围城期间坚守奥利安王城的英雄。勇猛善战、为人正直，加入义军后成为马尔斯最得力的战将之一。后期命运复杂。',
    NULL, NULL, NULL,
    '英武的年轻贵族骑士形象，金棕色短发，锐利的灰色眼睛，身披奥利安王家纹饰的重甲，手持骑枪，气场强大自信，动漫风格',
    '豪迈自信声音洪亮，直言不讳，对马尔斯既尊重又有竞争意识',
    6,
    '{"role":"party_leader","faction":"奥利安","class":"骑士(Paladin)"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-minerva', 'fe-dark-dragon',
    '密涅瓦',
    '马其顿王国的公主，19岁。被称为"赤色龙骑兵"的女武神，率领马其顿最强龙骑兵团。起初效忠于帝国，因不满暴政且妹妹玛莉亚被囚禁而倒戈加入义军。性格骄傲但不失正义感。',
    NULL, NULL, NULL,
    '威严冷艳的女性骑士形象，紫红色长发扎成高马尾，琥珀色竖瞳，身穿红色重型龙骑甲胄，背负巨型战斧，坐骑为红色飞龙（可选），气场强大不怒自威，动漫风格',
    '高傲冷峻语气简短，内心柔软但对陌生人戒备，谈论妹妹时语气软化',
    7,
    '{"role":"ally_commander","faction":"马其顿→义军","class":"龙骑(Dragon Knight)","title":"赤色龙骑兵"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-maria', 'fe-dark-dragon',
    '玛莉亚',
    '密涅瓦的妹妹，14岁。马其顿王国的第二公主，心地纯洁善良。被帝国囚禁在迪鲁城地牢中，被马尔斯救出后成为队伍中的治愈者（僧侣）。',
    NULL, NULL, NULL,
    '清纯可爱的少女形象，粉色长发柔顺垂落，水汪汪的大眼睛，身穿白色修女袍配淡金色镶边，手中握着治疗杖，天真无邪的笑容，动漫风格',
    '温软胆怯说话轻声细语，对姐姐密涅瓦非常依赖，害怕战斗但愿意帮助他人',
    8,
    '{"role":"healer","faction":"马其顿→义军","class":"僧侣(Cleric)","relation":{"sister":"密涅瓦"}}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-link', 'fe-dark-dragon',
    '林克',
    '格拉王国的王子，18岁。父亲是背叛阿卡奈亚投靠帝国的暴君，林克对父亲的罪行深感悔恨。最终选择大义灭亲杀死父亲，带领格拉军队归降义军赎罪。',
    NULL, NULL, NULL,
    '忧郁的年轻王子形象，深蓝色短发略显凌乱，深邃的黑眼睛，身穿黑色和银色相间的格拉王室铠甲，表情常带愧疚和决绝，动漫风格',
    '低沉内敛说话犹豫，背负罪恶感，决定行动后会变得异常坚决',
    9,
    '{"role":"redeemed_antihero","faction":"格拉→义军","tragic_element":"弑父赎罪"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-camus', 'fe-dark-dragon',
    '卡缪',
    '格鲁尼亚王国的名将，22岁。被誉为"格鲁尼亚之狼"，统领帝国最强的重骑兵军团。与马尔斯交战后败北，被饶恕后假死隐退。武艺超群且有荣誉感，是令人敬畏的对手。',
    NULL, NULL, NULL,
    '冷峻的青年将军形象，冰蓝色短发整齐向后梳，冷漠的浅色眼眸，身穿黑色格鲁尼亚重装骑士甲，手持长枪，面容俊美但毫无笑意，动漫风格',
    '冷漠简洁字斟句酌，军人作风不讲废话，重视荣誉和对手',
    10,
    '{"role":"honorable_rival","faction":"格鲁尼亚","class":"重装甲(Social Knight)","fate":"假死隐退"}',
    0, NOW(), NOW()
);

---- 龙人族 ----

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
    'char-gotoh', 'fe-dark-dragon',
    '哥多',
    '龙人族的贤者长老。拥有千年智慧和强大的魔法力量，预言马尔斯将成为拯救大陆的英雄。向马尔斯传授星光魔法——唯一能破除卡涅夫暗黑屏障的法术。',
    NULL, NULL, NULL,
    '古老的龙人贤者形象，长长的白发和胡须，智慧深邃的眼睛（异色瞳一蓝一金），身穿绣满符文的白袍，手持发光的法杖，周身环绕微弱的光芒，神圣而庄严，动漫风格',
    '缓慢深沉充满哲理，说话像吟诵预言，语气中带着千年的沧桑',
    12,
    '{"role":"wise_mentor","faction":"龙人族(守序)","ability":"传授星光魔法","title":"龙人贤者"}',
    0, NOW(), NOW()
);

---- 反派阵营 ----

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
    'char-ganev', 'fe-dark-dragon',
    '卡涅夫',
    '多鲁亚帝国的黑暗祭司/法师，梅迪乌斯的代理人。在暗黑神殿主持暗黑仪式，制造暗黑魔法屏障保护梅迪乌斯。只有哥多教授的星光魔法才能破除其屏障。冷酷残忍的宗教狂热者。',
    NULL, NULL, NULL,
    '邪恶的黑暗祭司形象，瘦高的身躯裹在黑色长袍中兜帽遮住大半张脸，只露出的皮肤苍白如纸，眼中闪烁诡异的红光，手中持镶嵌黑水晶的法杖，周围漂浮暗影粒子，阴森恐怖，动漫风格',
    '阴柔尖锐充满狂热信仰，视人类为献祭品，谈论暗黑之神时近乎癫狂',
    14,
    '{"role":"boss_minion","faction":"多鲁亚帝国","class":"黑暗祭司(Dark Bishop)","ability":"暗黑魔法屏障","defeated_by":"星光魔法"}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-gra_king', 'fe-dark-dragon',
    '格拉国王',
    '林克之父，格拉王国的暴君。当年背叛阿卡奈亚投靠帝国换取权力，统治残暴。被亲生儿子林克刺杀——这是全剧最沉重的悲剧时刻之一。',
    NULL, NULL, NULL,
    '堕落的老年国王形象，花白的乱发和浓密的灰胡须，肥胖臃肿的身躯穿着华丽的金边王袍，脸上布满贪婪和暴戾，坐在铺满毛皮的王座上，昏暗烛光下的阴影扭曲，动漫风格',
    '暴虐贪婪声音粗哑，习惯用命令口吻说话，提到权力时两眼放光',
    15,
    '{"role":"tragic_antagonist","faction":"格拉（帝国附庸）","fate":"被儿子林克弑杀","crime":"背叛阿卡奈亚"}',
    0, NOW(), NOW()
);

---- 重要NPC ----

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

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-elis', 'fe-dark-dragon',
    '艾莉丝',
    '马尔斯的姐姐，阿利提亚公主。帝国入侵后被囚禁在暗黑神殿深处，是马尔斯最终决战的重要动力之一。被马尔斯救出后姐弟重逢。',
    NULL, NULL, NULL,
    '温柔的公主形象（受困状态），银白色长发（与马尔斯相似）凌乱披散，苍白的脸庞但眼中仍有希望，身穿破旧的白色长裙，手腕上有镣铐的痕迹，脆弱而坚强的美，动漫风格',
    '虚弱但温暖，见到马尔斯时哽咽呼唤弟弟，声音颤抖但充满爱意',
    17,
    '{"role":"damsel_rescued","faction":"阿利提亚","status":"被囚禁→获救","relation":{"brother":"马尔斯"}}',
    0, NOW(), NOW()
);

INSERT INTO `characters` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `voice_id`, `voice_provider`,
    `appearance_prompt`, `dialogue_style`, `sort_order`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'char-talis_king', 'fe-dark-dragon',
    '塔利斯国王',
    '希达的父亲，塔利斯岛的统治者。在帝国入侵时支持马尔斯，为其提供船只和物资开启征程。开明的明君。',
    NULL, NULL, NULL,
    '和蔼的中年国王形象，棕灰色短发和修剪整齐的胡须，身穿塔利斯风格的海洋蓝色王服，坐在朴素但整洁的书房中，书架上摆满航海图，睿智温和的气质，动漫风格',
    '温和而有远见，说话条理清晰，对女儿希达宠溺但尊重她的选择',
    18,
    '{"role":"supporting_ally","faction":"塔利斯","action":"资助马尔斯出海复国"}',
    0, NOW(), NOW()
);

-- ============================================================
-- 第三步：创建场景（22个）
-- 按剧情出现顺序排列
-- ============================================================

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'scene-talis-village', 'fe-dark-dragon',
    '塔利斯村',
    '塔利斯岛的宁静渔村，马尔斯流亡三年的居所。木质房屋沿海岸排列，村民们以捕鱼为生。',
    NULL,
    '塔利斯岛', '白天',
    '中世纪幻想风格的海边小村庄，木制房屋错落排列在岩石海岸上，远处是蔚蓝的大海，几艘渔船停泊在浅湾，炊烟袅袅升起，晴朗的天气，温暖的阳光，动漫风格',
    '{"chapter":"序章","episodes":["1"]}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'scene-talis-castle', 'fe-dark-dragon',
    '塔利斯城',
    '塔利斯岛的主城，石墙环绕的城堡。帝国军围攻时这里成为最后的防线。',
    NULL,
    '塔利斯岛', '黄昏',
    '中世纪城堡外观，灰白色城墙在夕阳下泛着金光，城门口有守卫和村民慌乱奔跑，远处的天际线有黑烟升起暗示战斗逼近，紧张氛围，动漫风格',
    '{"chapter":"序章","episodes":["2"]}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'scene-orlean-coast', 'fe-dark-dragon',
    '奥利安海岸',
    '奥利安王国的海滨地带，马尔斯一行登陆的地方。沙滩延伸至悬崖峭壁。',
    NULL,
    '奥利安王国', '清晨',
    '广阔的海岸线景观，金色沙滩与高耸的白垩岩崖相接，海浪拍打礁石溅起白色泡沫，远处可见奥利安王城的轮廓，晨光中的薄雾，清新开阔的氛围，动漫风格',
    '{"chapter":"第一章","episodes":["3"]}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'scene-orlen-castle', 'fe-dark-dragon',
    '奥利安王城',
    '奥利安王国的首都城堡，被马其顿军队围困期间哈丁坚守于此。城内物资紧缺但士气尚存。',
    NULL,
    '奥利安王国', '白天',
    '宏伟的中世纪王城内部庭院，石砌建筑带有奥利安蓝白纹饰旗帜，士兵们在城墙缺口处加固防御，城内平民排队领取稀少的食物，坚毅而压抑的氛围，动漫风格',
    '{"chapter":"第一章","episodes":["4"]}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (
    `id`, `drama_id`, `name`, `description`, `image_url`, `location`,
    `time_of_day`, `prompt`, `extra_data`, `deleted`,
    `created_at`, `updated_at`
) VALUES (
    'scene-akaneia-resistance', 'fe-dark-dragon',
    '阿卡奈亚抵抗军营地',
    '妮娜公主领导的抵抗军秘密基地。隐藏在森林深处的临时营寨。',
    NULL,
    '阿卡奈亚废墟周边', '夜晚',
    '森林中的秘密营地，多顶帆布帐篷散布在巨树之间，篝火燃烧映照着战士们疲惫的面孔，中央大帐挂着残破的阿卡奈亚国旗，周围设置了暗哨警戒，紧张而团结的氛围，动漫风格',
    '{"chapter":"第一章","episodes":["5"]}',
    0, NOW(), NOW()
);

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'dragon-island-entrance','fe-dark-dragon',
    '龙人岛入口','佩拉迪龙人岛的岸边入口，奇异的植被和空气中弥漫的魔法气息。',NULL,
    '龙人岛','黄昏',
    '神秘的岛屿岸边，巨大的奇异植物发出微弱的荧光，空气中有肉眼可见的魔力粒子漂浮，远处山体上有古老的龙形雕刻若隐若现，紫红色天空下的奇幻景观，神秘氛围，动漫风格',
    '{"chapter":"第二章","episodes":["6"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'dragon-cave','fe-dark-dragon',
    '龙人洞穴','通往岛屿内部的天然洞穴系统，钟乳石和地下河构成的危险地形。堕落地龙出没之处。',NULL,
    '龙人岛深处','黑暗',
    '幽深的地下洞穴，发光的苔藓和水晶簇提供微弱照明，地面上有巨大爪痕和鳞片痕迹暗示龙的栖息，地下河发出低沉的水声，狭窄通道通向更深的未知区域，危险神秘氛围，动漫风格',
    '{"chapter":"第二章","episodes":["7"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'deil-outer','fe-dark-dragon',
    '迪鲁城外围','迪鲁城周边的平原战场，玛莉亚被囚禁的城堡近郊。',NULL,
    '迪鲁领地','白天',
    '战火洗礼过的平原废墟，烧焦的土地和断裂的兵器散落各处，远处迪鲁城的高耸城墙在烟尘中若隐若现，乌云压顶的阴沉天空，肃杀的战争前奏氛围，动漫风格',
    '{"chapter":"第三章","episodes":["8"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'deil-castle','fe-dark-dragon',
    '迪鲁城','囚禁玛莉亚的地牢所在城堡。阴森的帝国监狱要塞。',NULL,
    '迪鲁领地','夜晚',
    '阴暗的要塞城堡，黑色石墙上插满尖锐的铁刺，城门紧闭只有火把提供照明，塔楼上巡逻的士兵剪影，整体色调偏冷灰蓝色，压迫感十足的监狱氛围，动漫风格',
    '{"chapter":"第三章","episodes":["9"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'deil-dungeon','fe-dark-dragon',
    '迪鲁城地牢','玛莉亚被囚禁的地下牢房。潮湿、黑暗、绝望。',NULL,
    '迪鲁城地下','黑暗',
    '狭小阴湿的石室牢房，墙壁渗水长满青苔，地面铺着干草，角落里有一张破旧的小床，铁栅栏门上的锁锈迹斑斑，唯一的光源来自高处窄小的透气窗透入的一丝月光，绝望氛围，动漫风格',
    '{"chapter":"第三章","episodes":["9"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'macedon-camp','fe-dark-dragon',
    '马其顿野战营地','密涅瓦与马尔斯会面的野外营地。龙骑兵团驻扎之地。',NULL,
    '马其顿边境','傍晚',
    '军旅野营地的景象，数顶大型军用帐篷呈环形布置，中央空地上拴着披甲的战马，远处的篝火上烤着肉食，身穿红色龙骑甲的士兵们擦拭武器或喂马，红色旗帜在风中飘扬，豪迈粗犷的氛围，动漫风格',
    '{"chapter":"第三章","episodes":["10"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'gra-border','fe-dark-dragon',
    '格拉边境防线','格拉王国设置的边境防御工事。林克的部队在此阻击义军。',NULL,
    '格拉王国边境','白天',
    '边境军事要塞，木制拒马和壕沟组成的防线后方是格拉军队的营帐，远处可见格拉国旗（黑底金纹），天空中有侦查骑兵的尘土扬起，对峙的紧张感，动漫风格',
    '{"chapter":"第四章","episodes":["11"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'gra-throne-room','fe-dark-dragon',
    '格拉王宫大殿','暴君格拉国王的王座厅。林克弑父的悲剧发生地。',NULL,
    '格拉王城','夜晚',
    '奢华但令人窒息的王座大厅，黑色大理石柱支撑着高耸的天花板，金丝织就的地毯通向尽头的王座，两侧点燃的长明灯投下摇曳的光影，王座上的暴君身影被阴影吞噬，沉重压抑的戏剧性空间，动漫风格',
    '{"chapter":"第四章","episodes":["12"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'grunia-plains','fe-dark-dragon',
    '格鲁尼亚平原','卡缪率领重骑兵军团与义军交战的广阔平原。',NULL,
    '格鲁尼亚王国','白天',
    '一望无际的草原战场，草地上布满了车辙印和马蹄坑，远处地平线上黑压压的重装骑兵方阵正在推进，天空中盘旋的战鹰，史诗级战争场面的壮阔与残酷并存，动漫风格',
    '{"chapter":"第五章","episodes":["13"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'grunia-castle','fe-dark-dragon',
    '格鲁尼亚城堡','卡缪与马尔斯决斗的城堡大厅。格鲁尼亚的军事要塞核心。',NULL,
    '格鲁尼亚首都','黄昏',
    '坚固的军事堡垒内部，厚重的石墙上挂满了历代名将的铠甲和武器作为装饰，中央是一个圆形的决斗场区域，高处的窗户射入橙红色的夕阳余晖形成丁达尔效应，肃穆庄严的武士道氛围，动漫风格',
    '{"chapter":"第五章","episodes":["14"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'altea-border','fe-dark-dragon',
    '阿利提亚边境','马尔斯阔别三年后重返祖国土地的第一站。情感浓烈的归乡时刻。',NULL,
    '阿利提亚边境','黎明',
    '黎明时分的国境线，一条古道穿过起伏的丘陵通向远方阿利提亚的方向，路边的界碑上依稀可辨"ALTEA"的字样（已被破坏），晨雾笼罩的大地逐渐被金色的曙光穿透，充满希望与感伤交织的情绪，动漫风格',
    '{"chapter":"第六章","episodes":["15"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'altea-throne-room','fe-dark-dragon',
    '阿利提亚王座大厅','被占领后收复的阿利提亚王城主殿。马尔斯在此祭奠父王并宣告复国。',NULL,
    '阿利提亚王城','白天',
    '收复后的王座大厅，阳光透过彩色玻璃穹顶洒下斑驳光影，破碎的王座已被重新修复，墙壁上残留的战斗痕迹尚未完全清理，地上摆放着鲜花和祭品——这是祭奠先王的场所，庄严肃穆中带着新生的希望，动漫风格',
    '{"chapter":"第六章","episodes":["16"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'kadain-shrine','fe-dark-dragon',
    '卡达因神殿','安里王室遗留的神圣殿堂，封印着光之剑法尔西昂。古老而庄严。',NULL,
    '卡达因遗迹','正午',
    '古老的神殿内景，纯白色的石柱直达穹顶，地面刻满发光的神圣符文，殿堂尽头的高台上悬浮着一把散发着柔和光芒的剑（法尔西昂），整个空间充满神圣的金色光辉，超凡脱俗的神性氛围，动漫风格',
    '{"chapter":"第七章","episodes":["17"]}','{"sacred_item":"法尔西昂(光之圣剑)"}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'dragon-sanctuary','fe-dark-dragon',
    '龙人圣所','哥多传授星光魔法的神圣空间。龙人族最隐秘的圣地。',NULL,
    '龙人岛最深处','永恒暮光',
    '超越凡间的奇幻圣所，没有明确的天花板——上方是无尽的星河旋涡缓缓转动，脚下是由纯粹光芒构成的平台，四周漂浮着古代龙族的雕像和卷轴，哥多的身影站在星河中心，极致的奇幻神性美感，动漫风格',
    '{"chapter":"第七章","episodes":["18"]}','{"magic_taught":"星光魔法"}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'dolhr-territory','fe-dark-dragon',
    '多鲁亚帝国本土','义军攻入帝国腹地的荒原。焦土般的敌国领土。',NULL,
    '多鲁亚帝国','黄昏',
    '被战争摧毁的帝国领土，黑色的火山岩地貌，地表裂开喷出硫磺烟雾，远处有被烧毁的城镇残骸，天空呈现出不祥的暗红色，大军行进的剪影在地平线上推进，末日般苍凉的氛围，动漫风格',
    '{"chapter":"第八章","episodes":["19"]}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'dark-shrine','fe-dark-dragon',
    '暗黑神殿','梅迪乌斯和卡涅夫的据点。最邪恶的场所——暗黑仪式的舞台、最终BOSS战的场地。',NULL,
    '多鲁亚帝国核心','永夜',
    '终极邪恶的殿堂，巨大的空间由黑色玄武岩构建，无数蜡烛和暗红火焰提供诡异照明，地面绘有召唤法阵（发光的暗色纹路），中央是一个升起的祭坛，上方悬挂着扭曲的空间裂缝，绝对的黑暗与恐惧氛围，动漫风格',
    '{"chapter":"第八章","episodes":["20","21"]}','{"final_boss_location":true}',0,NOW(),NOW());

INSERT INTO `scenes` (`id`,`drama_id`,`name`,`description`,`image_url`,`location`,`time_of_day`,`prompt`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES (
    'altea-city-wall','fe-dark-dragon',
    '阿利提亚城头（结局）','战后重建的阿利提亚都城城头。马尔斯与希达眺望和平大陆的经典结局画面。',NULL,
    '阿利提亚王城','日出',
    '清晨阳光下的城楼顶部，石质垛口上站着两个身影（远景），前方是正在苏醒的城市——屋顶炊烟升起，街道上行人渐多，远处的田野绿意盎然，金色朝阳从东方地平线洒满整幅画面，温暖治愈的希望之光，动漫风格',
    '{"chapter":"结局","episodes":["22"]}','{"finale_scene":true}',0,NOW(),NOW());

-- ============================================================
-- 第四步：创建分镜数据（~120个）
--
-- 分集说明：
--   EP01 序章·塔利斯的黎明     （关卡1-2）
--   EP02 第一章·奥利安的解放   （关卡3-4）
--   EP03 第一章·阿卡奈亚遗孤   （关卡5）
--   EP04 第二章·龙人的考验     （关卡6-7）
--   EP05 第三章·马其顿转折     （关卡8-10）
--   EP06 第四章·格拉赎罪       （关卡11-12）
--   EP07 第五章·格鲁尼亚挑战   （关卡13-14）
--   EP08 第六章·复国之路       （关卡15-16）
--   EP09 第七章·决战准备       （关卡17-18）
--   EP10 第八章·暗黑神殿决战   （关卡19-21 + 结局）
-- ============================================================

-- ==================== EP01：序章 · 塔利斯的黎明 ====================
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep01-s01','fe-dark-dragon',1,1,1,'wide','航拍俯冲','镜头从高空俯瞰塔利斯岛全景，宁静的渔村炊烟袅袅。突然远处海面出现多艘海盗船逼近。',NULL,NULL,'scene-talis-village','pending','{"mood":"宁静到紧张"}',0,NOW(),NOW()),
('sb-ep01-s02','fe-dark-dragon',1,1,2,'medium','手持晃动','村民们四散奔逃，海盗士兵已跳上岸挥舞弯刀砍向房屋，火焰蔓延。','村民A：海盗来了！快跑！！',NULL,'scene-talis-village','pending','{"mood":"恐慌混乱"}',0,NOW(),NOW()),
('sb-ep01-s03','fe-dark-dragon',1,1,3,'medium','正面推镜','希达公主穿着便服气喘吁吁地跑进来，抓住马尔斯的手臂。','希达：马尔斯！求求你！村民们需要你的帮助！','char-shiida','scene-talis-village','pending','{"mood":"紧急恳求"}',0,NOW(),NOW()),
('sb-ep01-s04','fe-dark-dragon',1,1,4,'close-up','侧面特写','马尔斯眼神从犹豫变为坚定，握紧拳头，斗篷在风中扬起。','马尔斯：谢刚、凯因！召集所有人——我们不能再躲藏了。','char-mars','scene-talis-village','pending','{"mood":"觉醒决心","key_moment":true}',0,NOW(),NOW()),
('sb-ep01-s05','fe-dark-dragon',1,1,5,'wide','横向移动','马尔斯持剑冲锋在前，谢刚掩护侧翼，凯因后方射箭支援。激烈战斗场面。','马尔斯：为了塔利斯！为了所有庇护我们的人！','char-mars','scene-talis-village','pending','{"mood":"热血激昂"}',0,NOW(),NOW()),
('sb-ep01-s06','fe-dark-dragon',1,1,6,'medium','缓慢后拉','战斗结束后马尔斯收剑入鞘，希达站在不远处看着他。','希达：（独白）这个人……和传闻中的王子不一样。','char-shiida','scene-talis-village','pending','{"mood":"温情萌芽"}',0,NOW(),NOW()),
('sb-ep01-s07','fe-dark-dragon',1,2,1,'wide','远景固定','塔利斯城外地平线上帝国军方阵推进，无数军旗猎猎作响。','守卫长官：帝国军主力……超过三千人！',NULL,'scene-talis-castle','pending','{"mood":"绝望压迫"}',0,NOW(),NOW()),
('sb-ep01-s08','fe-dark-dragon',1,2,2,'medium','手持晃动','国王书房内一片混乱，地图被打翻，国王看着窗外火光。','塔利斯国王：他们是来抓马尔斯的。如果他落入帝国手中……希望就没了。','char-talis_king','scene-talis-castle','pending','{"mood":"紧迫决策"}',0,NOW(),NOW()),
('sb-ep01-s09','fe-dark-dragon',1,2,3,'wide','快速推进','夜色中马尔斯率小队突围，箭矢从身后飞来，他挥剑格挡护送希达前进。','马尔斯：希达！别回头！往前走！','char-mars','scene-talis-castle','pending','{"mood":"生死逃亡"}',0,NOW(),NOW()),
('sb-ep01-s10','fe-dark-dragon',1,2,4,'close-up','正反打','码头边国王将戒指交给马尔斯，船只已备好。','塔利斯国王：这艘船送你们去奥利安。马尔斯……为每一个受苦的人去解放它吧。','char-talis_king','scene-talis-castle','pending','{"mood":"庄重嘱托"}',0,NOW(),NOW()),
('sb-ep01-s11','fe-dark-dragon',1,2,5,'wide','航拍拉升','月光下海面航行，马尔斯和希达并肩站在船头。镜头拉高船只变成小点。','马尔斯：再见塔利斯……你好阿卡奈亚。','char-mars','scene-talis-castle','pending','{"mood":"史诗启程","episode_end":true}',0,NOW(),NOW());

-- ==================== EP02：第一章 · 奥利安的解放 ====================
-- 关卡3：奥利安的海岸 | 关卡4：奥利安王城
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep02-s01','fe-dark-dragon',2,3,1,'wide','固定远景','船只在晨光中抵达奥利安海岸，白垩岩崖和金色沙滩映入眼帘。马尔斯跳上沙滩。',NULL,NULL,'scene-orlean-coast','pending','{"mood":"新大陆"}',0,NOW(),NOW()),
('sb-ep02-s02','fe-dark-dragon',2,3,2,'medium','侧面跟拍','马尔斯一行在海岸边遭遇马其顿巡逻队，双方拔出武器对峙。','巡逻队长：站住！这片海岸已经被多鲁亚帝国接管了！',NULL,'scene-orlean-coast','pending','{"mood":"首次交锋"}',0,NOW(),NOW()),
('sb-ep02-s03','fe-dark-dragon',2,3,3,'wide','快速剪辑','战斗快速展开——凯因射倒前排敌人、马尔斯突进斩杀指挥官。巡逻队溃散。','凯因：这就是帝国的精锐？太弱了吧！','char-cain','scene-orlean-coast','pending','{"mood":"速胜"}',0,NOW(),NOW()),
('sb-ep02-s04','fe-dark-dragon',2,3,4,'medium','推镜','俘虏的敌军士兵跪地求饶，透露奥利安王城被围困的情报。','俘虏：求饶命！奥利安王城……被马其顿军团围了一个月了！城里快撑不住了！',NULL,'scene-orlean-coast','pending','{"mood":"情报获取"}',0,NOW(),NOW()),
('sb-ep02-s05','fe-dark-dragon',2,4,1,'wide','航拍俯冲','奥利安王城全景——城墙残破，外围布满马其顿军营，烟柱从城中升起。',NULL,NULL,'scene-orlen-castle','pending','{"mood":"围城压迫"}',0,NOW(),NOW()),
('sb-ep02-s06','fe-dark-dragon',2,4,2,'medium','手持跟拍','马尔斯率义军从侧翼发起突袭，突破马其顿包围圈。混乱中箭矢横飞。','马尔斯：哈丁！我们是来救你们的！坚持住！','char-mars','scene-orlen-castle','pending','{"mood":"突围激战"}',0,NOW(),NOW()),
('sb-ep02-s07','fe-dark-dragon',2,4,3,'close-up','正反打','城门口马尔斯与哈丁汇合。两人互相打量对方——未来的战友初次见面。','哈丁：你就是马尔斯？……比我想象中的年轻。但你的剑术我认可。','char-hardin','scene-orlen-castle','pending','{"mood":"英雄相惜"}',0,NOW(),NOW()),
('sb-ep02-s08','fe-dark-dragon',2,4,4,'wide','后拉','马其顿指挥官被击败后剩余敌军撤退。城门大开，奥利安士兵欢呼涌出。','士兵们：万岁！援军来了！奥利安得救了！',NULL,'scene-orlen-castle','pending','{"mood":"胜利解放"}',0,NOW(),NOW()),
('sb-ep02-s09','fe-dark-dragon',2,4,5,'medium','缓慢横移','王城内哈丁向马尔斯介绍奥利安的情况，地图铺在桌上。','哈丁：马其顿不是唯一的威胁。整个阿卡奈亚大陆都在帝国的铁蹄之下。要赢，我们需要更多的盟友。','char-hardin','scene-orlen-castle','pending','{"mood":"战略规划"}',0,NOW(),NOW()),
('sb-ep02-s10','fe-dark-dragon',2,4,6,'close-up','特写','马尔斯看着地图上标注的各国位置，眼神坚定。','马尔斯：那我们就一个一个去解放。从现在开始——奥利安就是我们第一个根据地。','char-mars','scene-orlen-castle','pending','{"mood":"决心确立","episode_end":true}',0,NOW(),NOW());

-- ==================== EP03：第一章 · 阿卡奈亚遗孤 ====================
-- 关卡5：阿卡奈亚的遗孤（妮娜加入）
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep03-s01','fe-dark-dragon',3,5,1,'medium','推进','森林深处秘密营地，妮娜公主站在中央大帐前迎接马尔斯一行。她身后是残破的阿卡奈亚国旗。',NULL,NULL,'scene-akaneia-resistance','pending','{"mood":"神秘相遇"}',0,NOW(),NOW()),
('sb-ep03-s02','fe-dark-dragon',3,5,2,'close-up','正面特写','妮娜的面部特写——金发蓝眼，表情庄重而疲惫。她直视马尔斯的眼睛。','妮娜：你就是安里王室最后的血脉……马尔斯王子。我是阿卡奈亚的妮娜。','char-nina','scene-akaneia-resistance','pending','{"mood":"正式介绍"}',0,NOW(),NOW()),
('sb-ep03-s03','fe-dark-dragon',3,5,3,'medium','过肩镜头','妮娜讲述阿卡奈亚灭亡的经过。闪回画面叠加在她的面部轮廓上——燃烧的城市、逃难的人民。','妮娜：那一夜……火焰吞没了整座王城。父王母后……所有的臣民……只有我们少数人逃了出来。','char-nina','scene-akaneia-resistance','pending','{"mood":"悲剧回忆"}',0,NOW(),NOW()),
('sb-ep03-s04','fe-dark-dragon',3,5,4,'close-up','反应镜头','马尔斯听完后的表情——同情与愤怒交织。他握紧拳头。','马尔斯：（低声）帝国会为这一切付出代价的。我向你保证，妮娜公主。','char-mars','scene-akaneia-resistance','pending','{"mood":"共情与承诺"}',0,NOW(),NOW()),
('sb-ep03-s05','fe-dark-dragon',3,5,5,'medium','中景','妮娜从怀中取出火焰纹章徽盾——古老的封印之盾散发着微弱的光芒。',NULL,'char-nina','scene-akaneia-resistance','pending','{"mood":"圣物展示"}',0,NOW(),NOW()),
('sb-ep03-s06','fe-dark-dragon',3,5,6,'close-up','特写','妮娜将火焰纹章递向马尔斯。','妮娜：这是火焰纹章……阿卡奈亚王室代代相传的封印之盾。它能增幅神器的力量、压制暗黑之力。现在——它属于你了。','char-nina','scene-akaneia-resistance','pending','{"mood":"权力交接","key_moment":true}',0,NOW(),NOW()),
('sb-ep03-s07','fe-dark-dragon',3,5,7,'wide','后拉','马尔斯接过火焰纹章，周围所有人（希达、谢刚、哈丁等）注视着这一刻。营地上空星光初现。','妮娜：带着它去吧，马尔斯。让火焰之光照亮整个大陆——就像它曾经照亮阿卡奈亚一样。','char-nina','scene-akaneia-resistance','pending','{"mood":"史诗传承","episode_end":true}',0,NOW(),NOW());

-- ==================== EP04：第二章 · 龙人的考验 ====================
-- 关卡6：龙人岛入口 | 关卡7：龙人洞穴
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep04-s01','fe-dark-dragon',4,6,1,'wide','航拍','龙人岛的奇异景观——荧光植物遍布岸边，紫红色天空下空气中有魔法粒子飘浮。马尔斯一行踏上岛屿。',NULL,NULL,'dragon-island-entrance','pending','{"mood":"奇幻异境"}',0,NOW(),NOW()),
('sb-ep04-s02','fe-dark-dragon',4,6,2,'medium','低角度仰拍','玛奴从岩石后方现身——半人半龙的异族特征清晰可见（竖瞳尖耳），周身环绕着龙气，神情愤怒。',NULL,'char-manu','dragon-island-entrance','pending','{"mood":"紧张对峙"}',0,NOW(),NOW()),
('sb-ep04-s03','fe-dark-dragon',4,6,3,'close-up','正反打','玛奴质问人类闯入者，声音中带着古老种族的威严。','玛奴：人类……你们总是带来战争和毁灭。这片土地不欢迎你们。','char-manu','dragon-island-entrance','pending','{"mood":"种族冲突"}',0,NOW(),NOW()),
('sb-ep04-s04','fe-dark-dragon',4,6,4,'medium','平视','马尔斯没有拔剑而是放下武器，以诚意回应玛奴的敌意。','马尔斯：我不是来征服的。我来这里是为了寻找对抗暗黑力量的方法——为了所有生灵，包括龙人族。','char-mars','dragon-island-entrance','pending','{"mood":"化解敌意"}',0,NOW(),NOW()),
('sb-ep04-s05','fe-dark-dragon',4,6,5,'close-up','特写玛奴的表情变化','玛奴眼中的愤怒逐渐消退，取而代之的是审视和一丝惊讶。','玛奴：（独白）这个人类……他的眼中没有贪婪。只有……纯粹的意志。','char-manu','dragon-island-entrance','pending','{"mood":"态度转变"}',0,NOW(),NOW()),
('sb-ep04-s06','fe-dark-dragon',4,7,1,'wide','环境展示','进入龙人洞穴内部——幽深的空间里发光苔藓和水晶簇提供照明，远处传来低沉的咆哮声。',NULL,NULL,'dragon-cave','pending','{"mood":"危险探索"}',0,NOW(),NOW()),
('sb-ep04-s07','fe-dark-dragon',4,7,2,'wide','快速摇摄','堕落地龙从黑暗中扑出！巨大的身躯和利爪撕裂空气。马尔斯和玛奴联手迎战。',NULL,NULL,'dragon-cave','pending','{"mood":"突发战斗"}',0,NOW(),NOW()),
('sb-ep04-s08','fe-dark-dragon',4,7,3,'medium','动作跟随','哥多从洞穴深处出现——白发长者手持发光法杖释放神圣魔法驱散堕地龙。龙人战士形态威严。',NULL,'char-gotoh','dragon-cave','pending','{"mood":"强力支援"}',0,NOW(),NOW()),
('sb-ep04-s09','fe-dark-dragon',4,7,4,'close-up','正面特写','哥多注视着马尔斯，千年智慧的目光仿佛看穿了一切。','哥多：你就是那个人……预言中的少年。命运之轮已经开始转动了。','char-gotoh','dragon-cave','pending','{"mood":"预言揭示"}',0,NOW(),NOW()),
('sb-ep04-s10','fe-dark-dragon',4,7,5,'wide','后拉','洞穴深处，哥多向众人示意前方有更深的道路。马尔斯回头看向洞口方向的光明。',NULL,'char-gotoh','dragon-cave','pending','{"mood":"新的使命","episode_end":true}',0,NOW(),NOW());

-- ==================== EP05：第三章 · 马其顿转折 ====================
-- 关卡8：迪鲁城外围 | 关卡9：迪鲁城救援 | 关卡10：马其顿的抉择
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep05-s01','fe-dark-dragon',5,8,1,'wide','航拍推进','迪鲁城外围的战场废墟。义军部队在烧焦的土地上行军向迪鲁城推进。远处城墙在烟尘中若隐若现。',NULL,NULL,'deil-outer','pending','{"mood":"战争逼近"}',0,NOW(),NOW()),
('sb-ep05-s02','fe-dark-dragon',5,8,2,'medium','侧面跟拍','一名骑马的信使从后方追上马尔斯，递出一封密信。信封上盖着马其顿王室火漆印。','信使：马尔斯殿下！一封来自马其顿公主密涅瓦的密信！是……求援信！',NULL,'deil-outer','pending','{"mood":"意外情报"}',0,NOW(),NOW()),
('sb-ep05-s03','fe-dark-dragon',5,8,3,'close-up','特写信件内容（画外音朗读）','密涅瓦的声音：（画外）如果你能看到这封信……请救救我的妹妹玛莉亚。她被关在迪鲁城的地牢里……我无能为力。',NULL,'deil-outer','pending','{"mood":"悬念铺垫"}',0,NOW(),NOW()),
('sb-ep05-s04','fe-dark-dragon',5,9,1,'wide','仰拍','夜色中迪鲁城的阴森轮廓——黑色石墙插满尖刺，塔楼上火把摇曳。',NULL,NULL,'deil-castle','pending','{"mood":"黑暗要塞"}',0,NOW(),NOW()),
('sb-ep05-s05','fe-dark-dragon',5,9,2,'medium','手持晃动跟拍','马尔斯率突击队潜入城堡内部。走廊狭窄昏暗，巡逻士兵的脚步声回荡。',NULL,'char-mars','deil-castle','pending','{"mood":"潜行紧张"}',0,NOW(),NOW()),
('sb-ep05-s06','fe-dark-dragon',5,9,3,'medium','缓慢推入','地牢深处——铁栅栏门后，一个瘦弱的少女身影蜷缩在角落。玛莉亚抬起头眼中含泪看到来人。',NULL,'char-maria','deil-dungeon','pending','{"mood":"悲情相遇"}',0,NOW(),NOW()),
('sb-ep05-s07','fe-dark-dragon',5,9,4,'close-up','特写玛莉亚的脸','玛莉亚颤抖着站起来抓住牢门栏杆。','玛莉亚：你……你是来救我的吗？姐姐……姐姐还好吗？','char-maria','deil-dungeon','pending','{"mood":"脆弱与希望"}',0,NOW(),NOW()),
('sb-ep05-s08','fe-dark-dragon',5,9,5,'wide','快速动作','谢刚用重剑劈开牢门锁。马尔斯扶住虚弱的玛莉亚。外面传来警报声——被发现！',NULL,'char-mars','deil-dungeon','pending','{"mood":"紧急脱出"}',0,NOW(),NOW()),
('sb-ep05-s09','fe-dark-dragon',5,10,1,'medium','正面中景','野战营地中密涅瓦与马尔斯对峙。密涅瓦身穿红色龙骑甲，气场强大但眼神复杂。',NULL,'char-minerva','macedon-camp','pending','{"mood":"微妙会面"}',0,NOW(),NOW()),
('sb-ep05-s10','fe-dark-dragon',5,10,2,'close-up','正反打','密涅瓦看着被救出的妹妹玛莉亚，坚硬的外壳终于裂开一丝缝隙。','密涅瓦：玛莉亚……（深吸一口气转向马尔斯）你救了她。我欠你一次。','char-minerva','macedon-camp','pending','{"mood":"情感软化"}',0,NOW(),NOW()),
('sb-ep05-s11','fe-dark-dragon',5,10,3,'medium','过肩镜头','密涅瓦表达对帝国的不满和对自由的渴望。背景中她的龙骑兵团安静地聆听。','密涅瓦：我曾经相信帝国的理想……但我看到的只有暴政和杀戮。从今天起——我和我的龙骑兵团，站在你这边。','char-minerva','macedon-camp','pending','{"mood":"关键倒戈","key_moment":true}',0,NOW(),NOW()),
('sb-ep05-s12','fe-dark-dragon',5,10,4,'wide','航拍拉升后拉','营地上空，马其顿的红色旗帜被降下取而代之的是义军的联合旗号。镜头越拉越高展现整片大陆的格局变化。',NULL,NULL,'macedon-camp','pending','{"mood":"势力重组","episode_end":true}',0,NOW(),NOW());

-- ==================== EP06：第四章 · 格拉赎罪 ====================
-- 关卡11：格拉边境 | 关卡12：格拉王城（弑父）
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep06-s01','fe-dark-dragon',6,11,1,'wide','横向移动','格拉边境防线前两军对峙。义军在左侧，格拉军队在右侧。林克骑马站在格拉阵线最前方。',NULL,NULL,'gra-border','pending','{"mood":"内战前夕"}',0,NOW(),NOW()),
('sb-ep06-s02','fe-dark-dragon',6,11,2,'medium','长焦压缩','林克的面部特写——表情矛盾痛苦。他的手握住剑柄又松开反复多次。',NULL,'char-link','gra-border','pending','{"mood":"内心挣扎"}',0,NOW(),NOW()),
('sb-ep06-s03','fe-dark-dragon',6,11,3,'medium','正面对话','战斗爆发前的短暂停火。马尔斯骑马上前与林克对话。两人之间隔着一段距离。','马尔斯：林克王子！你的父亲背叛了阿卡奈亚——但你不一定要继承他的罪孽！','char-mars','gra-border','pending','{"mood":"劝降"}',0,NOW(),NOW()),
('sb-ep06-s04','fe-dark-dragon',6,12,1,'wide','俯拍','格拉王宫大殿内部全景。黑金色调的奢华空间令人窒息。王座上的国王身影模糊不清。',NULL,NULL,'gra-throne-room','pending','{"mood":"压抑王宫"}',0,NOW(),NOW()),
('sb-ep06-s05','fe-dark-dragon',6,12,2,'close-up','侧光特写','格拉国王的脸——肥胖、贪婪、暴戾。他正在大笑着下达残酷的命令。','格拉国王：杀掉他们！把那个叛徒马尔斯的头挂到城门上！谁敢后退一律斩立决！','char-gra_king','gra-throne-room','pending','{"mood":"暴君嘴脸"}',0,NOW(),NOW()),
('sb-ep06-s06','fe-dark-dragon',6,12,3,'medium','缓慢推镜','林克独自走进大殿。他的脚步声在大理石地面回响。两侧侍从纷纷退避。',NULL,'char-link','gra-throne-room','pending','{"mood":"孤身赴会"}',0,NOW(),NOW()),
('sb-ep06-s07','fe-dark-dragon',6,12,4,'close-up','正反打交替','林克与父亲的对峙——全剧最沉重的对话场景。','林克：父亲……你当年出卖阿卡奈亚的时候想过今天吗？','char-link','gra-throne-room','pending','{"mood":"父子对决"}',0,NOW(),NOW()),
('sb-ep06-s08','fe-dark-dragon',6,12,5,'extreme-close-up','极端特写','格拉国王狂怒的表情转为惊愕——林克的剑已刺入他的胸膛。慢镜头中鲜血滴落大理石地面。','格拉国王：你……你这个逆子……！！','char-gra_king','gra-throne-room','pending','{"mood":"弑父悲剧"}',0,NOW(),NOW()),
('sb-ep06-s09','fe-dark-dragon',6,12,6,'medium','高角度俯拍','林克跪在父亲的尸体旁。剑从他手中滑落发出金属撞击声。他没有哭但肩膀剧烈颤抖。','林克：（低声几乎听不见）原谅我……父亲。这是……格拉最后的救赎。','char-link','gra-throne-room','pending','{"mood":"悲剧余韵","key_moment":true}',0,NOW(),NOW()),
('sb-ep06-s10','fe-dark-dragon',6,12,7,'wide','缓慢后拉','大殿门口马尔斯和众人在沉默中注视着这一幕。林克站起转身面向他们——脸上带着一种死寂后的坚定。','林克：格拉军队……从现在开始听从马尔斯殿下的命令。这是我唯一能做的补偿。','char-link','gra-throne-room','pending','{"mood":"赎罪归顺","episode_end":true}',0,NOW(),NOW());

-- ==================== EP07：第五章 · 格鲁尼亚挑战 ====================
-- 关卡13：格鲁尼亚平原 | 关卡14：格鲁尼亚城堡（卡缪决斗）
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep07-s01','fe-dark-dragon',7,13,1,'wide','史诗级广角','格鲁尼亚平原上一望无际的重骑兵方阵正向义军推进。地面的震动通过镜头传递给观众。战鹰在天空盘旋。',NULL,NULL,'grunia-plains','pending','{"mood":"史诗大战"}',0,NOW(),NOW()),
('sb-ep07-s02','fe-dark-dragon',7,13,2,'medium','侧面跟随','卡缪骑黑马冲在最前方——冰蓝色短发、冷漠眼神、黑色重甲。他是"格鲁尼亚之狼"。',NULL,'char-camus','grunia-plains','pending','{"mood":"强敌登场"}',0,NOW(),NOW()),
('sb-ep07-s03','fe-dark-dragon',7,13,3,'wide','快速剪辑','双方骑兵碰撞的瞬间——金属撞击声、马嘶声、战士怒吼交织在一起。混乱激烈的骑兵战。',NULL,NULL,'grunia-plains','pending','{"mood":"激战高潮"}',0,NOW(),NOW()),
('sb-ep07-s04','fe-dark-dragon',7,13,4,'close-up','慢动作特写','卡缪与马尔斯在乱军中擦身而过。两人的武器相撞迸出火花。时间仿佛在这一刻凝固。',NULL,'char-mars','grunia-plains','pending','{"mood":"宿命交锋"}',0,NOW(),NOW()),
('sb-ep07-s05','fe-dark-dragon',7,14,1,'wide','环境展示','格鲁尼亚城堡大厅内部——历代名将的铠甲和兵器作为装饰挂在石墙上。中央圆形区域是决斗场。夕阳透过高窗射入。',NULL,NULL,'grunia-castle','pending','{"mood":"肃穆决斗场"}',0,NOW(),NOW()),
('sb-ep07-s06','fe-dark-dragon',7,14,2,'medium','环绕拍摄','卡缪和马尔斯面对面站立。周围所有人退开形成一个圆圈。只有他们两个和彼此的武器。',NULL,'char-camus','grunia-castle','pending','{"mood":"决战氛围"}',0,NOW(),NOW()),
('sb-ep07-s07','fe-dark-dragon',7,14,3,'close-up','快速剪辑','一对一决斗——剑与枪的交锋。每一击都精准致命。汗水飞溅、呼吸急促、眼神锐利如刀。','卡缪：不错的剑术……但你还是太年轻了。','char-camus','grunia-castle','pending','{"mood":"技艺切磋"}',0,NOW(),NOW()),
('sb-ep07-s08','fe-dark-dragon',7,14,4,'extreme-close-up','极端特写','决胜一瞬间——马尔斯找到卡缪防御的破绽将剑尖停在对方的喉咙前。',NULL,'char-mars','grunia-castle','pending','{"mood":"胜负分晓"}',0,NOW(),NOW()),
('sb-ep07-s09','fe-dark-dragon',7,14,5,'medium','反应镜头','马尔斯收回剑。他伸出手想拉起卡缪——不是羞辱而是尊重。','马尔斯：我不杀值得尊敬的对手。卡缪……你的忠诚应该属于更好的主人。','char-mars','grunia-castle','pending','{"mood":"饶恕与尊重"}',0,NOW(),NOW()),
('sb-ep07-s10','fe-dark-dragon',7,14,6,'wide','后拉','卡缪看着马尔斯伸出的手片刻后转身离去。他的背影消失在暗处——假死隐退的开始。',NULL,'char-camus','grunia-castle','pending','{"mood":"神秘退场","key_moment":"卡缪假死伏笔","episode_end":true}',0,NOW(),NOW());

-- ==================== EP08：第六章 · 复国之路 ====================
-- 关卡15：阿利提亚边境 | 关卡16：阿利提亚王城
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep08-s01','fe-dark-dragon',8,15,1,'wide','缓慢推进','黎明时分的阿利提亚边境。古道穿过丘陵通向远方被战火熏黑的天际线。晨雾中马尔斯停下脚步。',NULL,NULL,'altea-border','pending','{"mood":"归乡感伤"}',0,NOW(),NOW()),
('sb-ep08-s02','fe-dark-dragon',8,15,2,'close-up','侧面特写','马尔斯的侧脸——眼眶微微泛红但强忍着不流露。他的手抚摸着路边残破的界碑。','马尔斯：（低声）三年了……我终于回来了，父亲。','char-mars','altea-border','pending','{"mood":"情感爆发边缘"}',0,NOW(),NOW()),
('sb-ep08-s03','fe-dark-dragon',8,15,3,'medium','过肩镜头','希达从身后轻轻握住马尔斯的手。两人并肩望向阿利提亚的方向。',NULL,'char-shiida','altea-border','pending','{"mood":"陪伴与支持","romantic_beat":true}',0,NOW(),NOW()),
('sb-ep08-s04','fe-dark-dragon',8,16,1,'wide','快速推镜','阿利提亚王城攻防战——义军从三面发起总攻。城墙上帝国守军顽强抵抗。投石机和箭塔齐发。',NULL,NULL,'altea-throne-room','pending','{"mood":"复国决战"}',0,NOW(),NOW()),
('sb-ep08-s05','fe-dark-dragon',8,16,2,'wide','航拍俯冲','马尔斯率突击队率先登上城墙缺口！旗帜在风中展开——阿利提亚的蓝银色国旗重新飘扬在城头。',NULL,'char-mars','altea-throne-room','pending','{"mood":"夺旗时刻","key_moment":"收复王城"}',0,NOW(),NOW()),
('sb-ep08-s06','fe-dark-dragon',8,16,3,'medium','缓慢推进','战斗结束后的王座大厅。阳光透过彩色玻璃穹顶洒下斑驳光影。地上摆放着鲜花和祭品——祭奠先王。',NULL,NULL,'altea-throne-room','pending','{"mood":"肃穆哀悼"}',0,NOW(),NOW()),
('sb-ep08-s07','fe-dark-dragon',8,16,4,'close-up','正面特写','马尔斯跪在祭品前的背影。他低下头肩膀微微颤抖。',NULL,'char-mars','altea-throne-room','pending','{"mood":"悼念父亲"}',0,NOW(),NOW()),
('sb-ep08-s08','fe-dark-dragon',8,16,5,'medium','后拉','马尔斯站起转身面对所有战友们。阳光照在他脸上。',NULL,'char-mars','altea-throne-room','pending','{"mood":"宣告复国"}',0,NOW(),NOW()),
('sb-ep08-s09','fe-dark-dragon',8,16,6,'wide','环绕拉升','所有角色齐聚王座大厅——马尔斯、希达、妮娜、密涅瓦、哈丁、林克……每个人脸上都带着不同的表情（坚定/欣慰/期待）。',NULL,NULL,'altea-throne-room','pending','{"mood":"全员集结","episode_end":true}',0,NOW(),NOW());

-- ==================== EP09：第七章 · 决战准备 ====================
-- 关卡17：卡达因神殿（法尔西昂） | 关卡18：龙人圣所（星光魔法）
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep09-s01','fe-dark-dragon',9,17,1,'wide','缓慢推进','卡达因神殿内景——纯白石柱直达穹顶，地面刻满发光的神圣符文。空气中有金色尘埃在光束中漂浮。',NULL,NULL,'kadain-shrine','pending','{"mood":"神圣庄严"}',0,NOW(),NOW()),
('sb-ep09-s02','fe-dark-dragon',9,17,2,'wide','仰拍特写','殿堂尽头的高台上——一把剑悬浮在空中散发着柔和的光芒。那就是法尔西昂，光之圣剑。',NULL,NULL,'kadain-shrine','pending','{"mood":"神器显现"}',0,NOW(),NOW()),
('sb-ep09-s03','fe-dark-dragon',9,17,3,'close-up','反应镜头','马尔斯走向法尔西昂。当他伸手触碰剑柄时整个大殿的光芒瞬间暴涨然后稳定下来。剑选择了他。',NULL,'char-mars','kadain-shrine','pending','{"mood":"神器认主","key_moment":true}',0,NOW(),NOW()),
('sb-ep09-s04','fe-dark-dragon',9,17,4,'medium','正反打','妮娜将火焰纹章放在法尔西昂旁边。两者产生共鸣——封印之盾增幅了光之剑的力量。',NULL,'char-nina','kadain-shrine','pending','{"mood":"双神器共鸣"}',0,NOW(),NOW()),
('sb-ep09-s05','fe-dark-dragon',9,18,1,'wide','360度旋转','龙人圣所——没有天花板只有无尽星河旋涡。哥多站在光芒构成的平台中央等待。',NULL,'char-gotoh','dragon-sanctuary','pending','{"mood":"超凡圣地"}',0,NOW(),NOW()),
('sb-ep09-s06','fe-dark-dragon',9,18,2,'medium','缓慢推进','哥多向马尔斯传授星光魔法。古老的语言从他口中流出化作星点融入马尔斯体内。',NULL,'char-gotoh','dragon-sanctuary','pending','{"mood":"魔法传承"}',0,NOW(),NOW()),
('sb-ep09-s07','fe-dark-dragon',9,18,3,'close-up','特写','马尔斯睁开眼睛——瞳孔中有星光流转。他抬起手掌心凝聚出一团星光。',NULL,'char-mars','dragon-sanctuary','pending','{"mood":"力量觉醒"}',0,NOW(),NOW()),
('sb-ep09-s08','fe-dark-dragon',9,18,4,'medium','中景','哥多解释星光魔法的作用——唯一能破除卡涅夫暗黑屏障的法术。',NULL,'char-gotoh','dragon-sanctuary','pending','{"mood":"关键情报"}',0,NOW(),NOW()),
('sb-ep09-s09','fe-dark-dragon',9,18,5,'wide','后拉','众人站在龙人圣所中准备就绪。马尔斯手持法尔西昂，身上同时有火焰纹章和星光的加持。','哥多：去吧，星之王马尔斯。你的命运不是逃避——是终结这百年的轮回。','char-gotoh','dragon-sanctuary','pending','{"mood":"最终启程","episode_end":true}',0,NOW(),NOW());

-- ==================== EP10：第八章 · 暗黑神殿决战 + 结局 ====================
-- 关卡19：帝国本土 | 关卡20-21：暗黑神殿 | 结局
INSERT INTO `storyboards` (`id`,`drama_id`,`episode_number`,`scene_number`,`shot_number`,`shot_type`,`shot_direction`,`action`,`dialogue`,`character_id`,`scene_id`,`status`,`extra_data`,`deleted`,`created_at`,`updated_at`) VALUES
('sb-ep10-s01','fe-dark-dragon',10,19,1,'wide','史诗航拍','多鲁亚帝国本土全景——焦黑的火山岩地貌，硫磺烟雾喷涌，暗红色天空下义军大军的剪影在地平线上推进。',NULL,NULL,'dolhr-territory','pending','{"mood":"末日征途"}',0,NOW(),NOW()),
('sb-ep10-s02','fe-dark-dragon',10,19,2,'wide','横向快速移动','义军各部队协同作战突破帝国防线。马其顿龙骑兵空中突袭、格鲁尼亚重骑地面冲锋、奥利安步兵两翼包抄。',NULL,NULL,'dolhr-territory','pending','{"mood":"联合攻势"}',0,NOW(),NOW()),
('sb-ep10-s03','fe-dark-dragon',10,19,3,'medium','跟拍','马尔斯冲在最前方。法尔西昂在他手中发出耀眼的白光所到之处帝国的黑暗力量纷纷溃散。','马尔斯：全军听令——目标暗黑神殿！一个不留地推进！！','char-mars','dolhr-territory','pending','{"mood":"势不可挡"}',0,NOW(),NOW()),

-- 暗黑神殿内部 - 对决卡涅夫
('sb-ep10-s04','fe-dark-dragon',10,20,1,'wide','环境展示','暗黑神殿内部——黑色玄武岩构建的巨大空间。无数蜡烛提供诡异照明。地面绘有召唤法阵。中央祭坛上方悬挂着空间裂缝。',NULL,NULL,'dark-shrine','pending','{"mood":"终极邪恶场所"}',0,NOW(),NOW()),
('sb-ep10-s05','fe-dark-dragon',10,20,2,'low-angle','仰拍','卡涅夫站在祭坛上——瘦高身躯裹在黑色长袍中兜帽遮脸，眼中闪烁红光。手中黑水晶法杖散发的黑暗能量形成一道屏障。',NULL,'char-ganev','dark-shrine','pending','{"mood":"BOSS登场"}',0,NOW(),NOW()),
('sb-ep10-s06','fe-dark-dragon',10,20,3,'medium','正面冲突','卡涅夫释放暗黑魔法攻击。马尔斯用法尔西昂格挡但被震退——普通攻击无法穿透屏障。',NULL,'char-ganev','dark-shrine','pending','{"mood":"陷入劣势"}',0,NOW(),NOW()),
('sb-ep10-s07','fe-dark-dragon',10,20,4,'close-up','特写觉醒','马尔斯闭上眼回忆哥多的教诲。当他再次睁眼时瞳孔中的星光暴涨——右手释放出星光魔法！',NULL,'char-mars','dark-shrine','pending','{"mood":"关键时刻"}',0,NOW(),NOW()),
('sb-ep10-s08','fe-dark-dragon',10,20,5,'wide','特效爆炸','星光击中卡涅夫的暗黑屏障——屏障像玻璃一样碎裂！卡涅夫发出惊恐的尖叫。',NULL,'char-ganev','dark-shrine','pending','{"mood":"逆转胜利"}',0,NOW(),NOW()),

-- 最深处 - 救出艾莉丝
('sb-ep10-s09','fe-dark-dragon',10,21,1,'medium','缓慢推进','暗黑神殿最深处的一个囚室。艾莉丝蜷缩在角落——银白发凌乱苍白的脸但眼中还有希望。',NULL,'char-elis','dark-shrine','pending','{"mood":"姐弟重逢前奏"}',0,NOW(),NOW()),
('sb-ep10-s10','fe-dark-dragon',10,21,2,'close-up','情感特写','马尔斯冲进来跪在艾莉斯面前。姐姐看到弟弟的那一刻泪水夺眶而出。','艾莉斯：马尔斯……真的是你……我的弟弟长大了……','char-elis','dark-shrine','pending','{"mood":"催泪重逢"}',0,NOW(),NOW()),
('sb-ep10-s11','fe-dark-dragon',10,21,3,'medium','拥抱镜头','姐弟拥抱在一起。背景中其他伙伴默默转身给他们留出空间。',NULL,'char-mars','dark-shrine','pending','{"mood":"温情时刻"}',0,NOW(),NOW()),

-- 最终BOSS - 梅迪乌斯
('sb-ep10-s12','fe-dark-dragon',10,21,4,'wide','极致广角低机位','暗黑神殿最深处崩裂——巨大的暗黑龙梅迪乌斯从地下升起！体型遮天蔽日，黑色鳞片覆盖全身，红色眼睛如同深渊，漆黑双翼展开几乎填满整个画面。',NULL,'char-medius','dark-shrine','pending','{"mood":"终极BOSS降临","scale":"史诗级"}',0,NOW(),NOW()),
('sb-ep10-s13','fe-dark-dragon',10,21,5,'extreme-wide','远景对峙','马尔斯独自面对巨龙的渺小身影。法尔西昂在他手中光芒越来越亮。火焰纹章在他胸口发出共鸣之光。',NULL,'char-mars','dark-shrine','pending','{"mood":"宿命对决"}',0,NOW(),NOW()),
('sb-ep10-s14','fe-dark-dragon',10,21,6,'wide','快速剪辑','史诗级战斗——梅迪乌斯的暗黑吐息 vs 法尔西昂的圣光斩击。每一次碰撞都引发能量冲击波摧毁周围的建筑结构。',NULL,'char-medius','dark-shrine','pending','{"mood":"终极激战"}',0,NOW(),NOW()),
('sb-ep10-s15','fe-dark-dragon',10,21,7,'slow-motion','慢动作决胜','马尔斯跃起到半空——法尔西昂吸收了火焰纹章的全部力量爆发出前所未有的光辉。一击刺入梅迪乌斯的心脏位置！',NULL,'char-mars','dark-shrine','pending','{"mood":"终结一击","key_moment":"全剧高潮"}',0,NOW(),NOW()),
('sb-ep10-s16','fe-dark-dragon',10,21,8,'wide','延时效果','梅迪乌斯发出最后的咆哮后身体开始崩解——化为无数黑色的碎片消散在空气中。暗黑神殿开始坍塌但光芒从裂缝中涌入。',NULL,'char-medius','dark-shrine','pending','{"mood":"BOSS消亡"}',0,NOW(),NOW()),

-- 结局
('sb-ep10-s17','fe-dark-dragon',10,22,1,'wide','日出航拍','时间跳跃后的画面——清晨阳光下的阿利提亚城楼顶部。石质垛口上站着两个身影眺望远方苏醒的城市。',NULL,NULL,'altea-city-wall','pending','{"mood":"和平到来"}',0,NOW(),NOW()),
('sb-ep10-s18','fe-dark-dragon',10,22,2,'medium','侧面中景','马尔斯和希达并肩站着。马尔斯穿着王室礼服不再是铠甲。希达靠在他的肩头。两人的表情平静而幸福。','希达：终于结束了……对吧？','char-shiida','altea-city-wall','pending','{"mood":"温情结局"}',0,NOW(),NOW()),
('sb-ep10-s19','fe-dark-dragon',10,22,3,'medium','正面对话','马尔斯转头看向希达微笑。远处城市的钟声响起——新纪元的开始。','马尔斯：不……这只是新的开始。（看向远方的大陆）还有很多事情要做呢。','char-mars','altea-city-wall','pending','{"mood":"展望未来"}',0,NOW(),NOW()),
('sb-ep10-s20','fe-dark-dragon',10,22,4,'wide','最终拉升后拉','镜头缓缓升高越过城楼——展现整个阿卡奈亚大陆的鸟瞰图。各国旗帜在风中飘扬，田野绿意盎然，城市炊烟袅袅。画面渐变为标题字幕。',NULL,NULL,'altea-city-wall','pending','{"mood":"史诗结局","series_finale":true}',0,NOW(),NOW());

-- ============================================================
-- 数据统计
--   剧集(dramas):     1 条
--   角色(characters): 18 条
--   场景(scenes):     22 条
--   分镜(storyboards): 96 条 (EP01:11 + EP02:10 + EP03:7 + EP04:10 + EP05:12 + EP06:10 + EP07:10 + EP08:9 + EP09:9 + EP10:18)
--
-- 执行方式：
--   此SQL不包含建表语句（依赖 zhenling_drama.sql 已创建的表结构）
--   直接在 MySQL 中执行即可：
--     mysql -u root -p zhenling_drama < migration-fe-dark-dragon.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
