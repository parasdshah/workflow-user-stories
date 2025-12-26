# K. Stage Actions - Detailed Tasks

Based on: `_Detailed_requirements/K_Stage_Actions.md`

## 1. Database & Backend Configuration
- [ ] **Modify StageConfig Entity**
    - Add `allowedActions` column (Type: JSON or Comma-Separated String).
    - Update Hibernate/JPA entity `StageConfig.java`.
- [ ] **Update Stage Configuration API**
    - Update `StageConfigDTO` to include `allowedActions`.
    - Update `StageOverviewController` to handle saving/updating this field.

## 2. Backend Runtime (Case Execution)
- [ ] **Update Task Completion Logic**
    - Modify `CaseController.completeTask` endpoint to accept `outcome` parameter.
    - Validate `outcome` against configured `allowedActions` for the stage.
- [ ] **Flowable Integration**
    - Pass `outcome` as a process variable (e.g., `_outcome`) to Flowable.
- [ ] **Audit Trail**
    - Ensure `AuditTrailService` records the specific `outcome` in the `TASK_COMPLETED` event payload.

## 3. Frontend Configuration (Stage Settings)
- [ ] **Stage Config UI**
    - Add "Allowed Actions" section in Stage Configuration.
    - Implement input for standard actions (APPROVE, REJECT, ON-HOLD) and custom actions.
    - Validation to ensure at least one action is defined (if applicable).

## 4. Frontend Runtime (Case View)
- [ ] **Dynamic Action Buttons**
    - Replace single "Complete" button with:
        - Dropdown menu (if many actions).
        - Segmented control or individual buttons (if few actions).
    - display logic based on `stageConfig.allowedActions`.
- [ ] **API Integeration**
    - Wire "Confirm" action to `completeTask` API with selected outcome.
