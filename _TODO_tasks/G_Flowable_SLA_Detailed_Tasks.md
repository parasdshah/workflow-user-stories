# G.5 SLA Mapping - Detailed Tasks

The basic SLA Timer Boundary Event is implemented, but it lacks stage-level granularity and an actionable trigger path (e.g., sending a notification).

## 1. Entity Updates
- [x] **Stage Level SLA**:
    - [x] Add `BigDecimal slaDurationDays` to `StageConfig` entity.
    - [x] Update `StageConfigRepository` (if needed).
    - [x] Update `StageConfig` UI in Frontend to allow setting SLA per stage.

## 2. BPMN Generation Enhancements
- [x] **Precise Duration Calculation**:
    - [x] Logic: Use `StageConfig.slaDurationDays` if present; else fallback to `WorkflowMaster.slaDurationDays`.
    - [x] If neither is present, skip adding the timer.
- [x] **Actionable Path (Notification)**:
    - [x] Create a `ServiceTask` for "SLA Notification".
    - [x] Set `delegateExpression` to `${slaNotificationService}`.
    - [x] **Crucial**: Create a `SequenceFlow` from the `BoundaryEvent` to this `ServiceTask`.
    - [x] Connect `ServiceTask` to an `EndEvent` (or merge back if appropriate, but usually SLA notification is a one-off side-effect).

## 3. Service Implementation
- [x] **SLA Notification Bean**:
    - [x] Implement a `JavaDelegate` named `slaNotificationService`.
    - [x] Logic: Log a warning or toggle a flag (stub for now, but ready for Email integration).

## 4. Verification
- [x] **Unit Tests**:
    - [x] Verify `SequenceFlow` exists between BoundaryEvent and Notification Task.
    - [x] Verify Stage SLA overrides Global SLA.
