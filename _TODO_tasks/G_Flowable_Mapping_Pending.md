# G. Flowable Mapping - Technical Implementation Details

The `BpmnGeneratorService` logic includes mapping for SLA Timers (G.5), Completion Service Task (G.6), and dynamic Form Keys (G.2).

## 1. G.5 SLA Mapping (Timer Boundary Event)
- **Status**: Implemented.
- **Requirement**: Attach a non-interrupting timer boundary event to User Tasks if an SLA is defined.
- **Trigger**: `StageConfig.slaDurationDays` (or derived from Workflow).
- **Implementation**:
    1.  Converts `slaDurationDays` (BigDecimal) to ISO-8601 string (e.g. `PT12H`).
    2.  Creates `BoundaryEvent` attached to the user task.
    3.  Sets `cancelActivity` to `false` (non-interrupting).

## 2. G.6 Completion Mapping
- **Status**: Implemented.
- **Requirement**: Call an external API when the workflow completes.
- **Implementation**:
    1.  Before the `EndEvent`, checks if `WorkflowMaster.completionApiEndpoint` is present.
    2.  If yes, injects a `ServiceTask` instead of connecting last stage directly to `EndEvent`.
    3.  Sets `delegateExpression` to `${completionService}`.

## 3. G.2 Form Key Refinement
- **Status**: Implemented.
- **Requirement**: Use the actual mapped `screenCode`.
- **Implementation**:
    - Uses `ScreenMappingRepository` to look up the correct screen code for the given stage code.
    - Sets `userTask.setFormKey(screenCode)`.
