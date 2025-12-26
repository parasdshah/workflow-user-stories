# K. Stage Outcome Actions

## 1. Goal
Enable the configuration of multiple "Outcome Actions" (e.g., APPROVE, REJECT, ON-HOLD) for each stage in a workflow. These actions allow users to explicitly choose the result of a task, which drives the workflow path and is recorded for audit purposes.

## 2. Functional Requirements

### 2.1 Configuration
- The **Stage Configuration** screen must allow defining a list of **Allowed Actions** for each stage.
- **Default Actions**: The system should support standard actions like:
    - `APPROVE`
    - `REJECT`
    - `ON-HOLD`
    - `SUBMIT` (for initial stages)
- **Custom Actions**: Users should also be able to define custom action labels if needed.

### 2.2 Runtime Execution (Case View)
- On the **Case Details** page, when a user selects an active stage/task to "Take Action":
    - Instead of a single "Complete" button, the UI must present these actions.
    - **UI Element**: A **Dropdown** menu (or segmented control) listing the configured actions for that stage.
    - **Selection**: The user selects an action (e.g., "REJECT") and clicks confirm/submit.

### 2.3 Data & Audit
- **Audit Trail**: The selected action must be recorded in the `AuditTrail` table.
    - Entity: `Case` or `Task`
    - Action: `TASK_COMPLETED`
    - Payload: Must include `{"outcome": "REJECT", ...}`
- **Flowable Integration**: The selected action should be passed as a **Process Variable** (e.g., `_outcome` or `action`) to the Flowable engine to evaluate gateways (Exclusive Gateway) for decision routing (e.g., If `REJECT` -> Go to End; If `APPROVE` -> Go to Next Stage).

## 3. Technical Constraints
- The list of actions must be stored in the `StageConfig` entity (e.g., as a JSON array or comma-separated string).
- The `CaseController.completeTask` endpoint must accept an `outcome` parameter.
