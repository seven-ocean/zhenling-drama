-- ============================================================
-- BUG00013 修复：AI 配置表增加 Token 单价字段
-- 用于追踪每个 AI 服务的 Token 消耗成本
-- 执行时间：2026-04-14
-- 幂等：可重复执行（IF NOT EXISTS）
-- ============================================================

ALTER TABLE `ai_configs`
ADD COLUMN IF NOT EXISTS `token_price` DECIMAL(10,6) NULL DEFAULT 0.000200
COMMENT 'Token单价(元/Token)，如0.000200表示每Token 0.0002元'
AFTER `config_json`;

-- 验证
SELECT id, provider, api_type, model, token_price FROM `ai_configs` LIMIT 5;
