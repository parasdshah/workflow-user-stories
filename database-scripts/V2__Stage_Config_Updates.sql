-- =================================================================
-- Patch: Updates to Stage Config for Rules and Parallel Execution
-- Reason: To support Business Rule Stages and Parallel Grouping
-- Date: 2025-12-28
-- =================================================================

-- 1. Parallel Execution
ALTER TABLE stage_config ADD COLUMN IF NOT EXISTS parallel_grouping VARCHAR(255);

-- 2. Business Rule Integration
ALTER TABLE stage_config ADD COLUMN IF NOT EXISTS is_rule_stage BOOLEAN DEFAULT FALSE;
ALTER TABLE stage_config ADD COLUMN IF NOT EXISTS rule_key VARCHAR(255);

-- 3. Ensure booleans are not null (optional, safe default)
UPDATE stage_config SET is_rule_stage = FALSE WHERE is_rule_stage IS NULL;
