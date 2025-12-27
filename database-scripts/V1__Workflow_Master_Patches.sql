-- =================================================================
-- Patch: Add 'status' column to workflow_master
-- Reason: To support Soft Delete (Undeploy History Retention)
-- Date: 2025-12-27
-- Applied By: DatabaseAutoPatcher.java (in workflow-service)
-- =================================================================

ALTER TABLE workflow_master ADD COLUMN IF NOT EXISTS status VARCHAR(255) DEFAULT 'ACTIVE';

-- Ensure legacy rows are populated
UPDATE workflow_master SET status = 'ACTIVE' WHERE status IS NULL;
