-- =================================================================
-- Initial Schema for Application Tables
-- Based on JPA Entities
-- Note: Flowable tables (ACT_*) are not included as they are 
--      managed by the Flowable Engine.
-- =================================================================

-- 1. Workflow Master
CREATE TABLE IF NOT EXISTS workflow_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_name VARCHAR(255) NOT NULL UNIQUE,
    workflow_code VARCHAR(255) NOT NULL UNIQUE,
    completion_api_endpoint VARCHAR(255),
    associated_module VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    sla_duration_days DECIMAL(19, 2),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 2. Stage Configuration
CREATE TABLE IF NOT EXISTS stage_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_code VARCHAR(255) NOT NULL,
    stage_name VARCHAR(255) NOT NULL,
    stage_code VARCHAR(255) NOT NULL,
    sequence_order INT NOT NULL,
    is_nested_workflow BOOLEAN,
    nested_workflow_code VARCHAR(255),
    pre_entry_hook VARCHAR(255),
    post_entry_hook VARCHAR(255),
    pre_exit_hook VARCHAR(255),
    post_exit_hook VARCHAR(255),
    reminder_template_id1 VARCHAR(255),
    reminder_template_id2 VARCHAR(255),
    sla_duration_days DECIMAL(19, 2),
    allowed_actions TEXT,
    parallel_grouping VARCHAR(255),
    is_rule_stage BOOLEAN DEFAULT FALSE,
    rule_key VARCHAR(255)
);

-- 3. Screen Definition
CREATE TABLE IF NOT EXISTS screen_definition (
    screen_code VARCHAR(255) NOT NULL PRIMARY KEY,
    description VARCHAR(255),
    layout_json CLOB,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 4. Screen Mapping
CREATE TABLE IF NOT EXISTS screen_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stage_code VARCHAR(255) NOT NULL,
    screen_code VARCHAR(255) NOT NULL,
    access_type VARCHAR(255) -- 'EDITABLE' or 'READ_ONLY'
);

-- 5. Audit Trail
CREATE TABLE IF NOT EXISTS audit_trail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(255),
    entity_id VARCHAR(255),
    action VARCHAR(255),
    changed_by VARCHAR(255),
    changed_at TIMESTAMP,
    changes CLOB
);
