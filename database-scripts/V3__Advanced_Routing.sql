-- =================================================================
-- Patch: Advanced Routing (Entry Conditions)
-- Reason: To support skipping stages based on rules (Advanced Routing)
-- Date: 2025-12-30
-- =================================================================

-- 1. Entry Conditions
ALTER TABLE stage_config ADD COLUMN IF NOT EXISTS entry_condition TEXT;
