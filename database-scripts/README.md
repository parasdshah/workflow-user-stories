# Database Scripts Project

## Overview
This directory serves as the centralized repository for manually managed SQL scripts.
Currently, the `workflow-service` primarily uses `spring.jpa.hibernate.ddl-auto: update` for development speed, and Flowable manages its own tables (`ACT_*`) automatically.

However, for production consistency and manual patches (like specific column additions or data migrations), scripts should be documented here.

## Naming Convention
Use `V<Version>__Description.sql` (Flyway style) or `XXX_Description.sql`.

## Scripts
- `V0__Initial_Schema.sql`: DDL for application tables (`workflow_master`, `stage_config` etc.).
- `V1__Workflow_Master_Patches.sql`: specific patches applied to `workflow_master`.

## Flowable Tables
The engine uses standard `ACT_*` tables (e.g., `ACT_RU_TASK`, `ACT_HI_VARINST`).
Reference schemas can be found in the [Flowable Documentation](https://documentation.flowable.com/latest/assets/sql/create/flowable.h2.create.engine.sql).
