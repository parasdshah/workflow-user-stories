# G. Flowable Mapping - Detailed User Stories

## G.1 Workflow -> Process
**User Story**: As a System, I want to map the `WorkflowMaster` entity to the BPMN `Process` element, ensuring ID consistency.

**Acceptance Criteria**:
- [ ] `process.id` = `workflowCode`.
- [ ] `process.name` = `workflowName`.
- [ ] `process.isExecutable` = `true`.

**Dependencies**:
- Related to: [F.1 Generator](../F_BPMN_Generation.md#f1-automated-bpmn-generator)

---

## G.2 Normal Stage -> UserTask
**User Story**: As a System, I want to map "Normal" stages to `UserTask` elements, enabling human interaction.

**Acceptance Criteria**:
- [ ] `userTask.id` = `stageCode`.
- [ ] `userTask.name` = `stageName`.
- [ ] `formKey` = `screenCode` (from Mapping).

**Dependencies**:
- Related to: [F.2 Map Stages](../F_BPMN_Generation.md#f2-map-stages)

---

## G.3 Workflow Stage -> CallActivity
**User Story**: As a System, I want to map "Workflow" stages to `CallActivity` elements, enabling sub-process invocation.

**Acceptance Criteria**:
- [ ] `callActivity.calledElement` = `nestedWorkflowCode`.
- [ ] `callActivity.inheritVariables` = `true` (typically).

**Dependencies**:
- Related to: [B.2 Nested Workflow](../B_Stage_Configuration.md#b2-nested-workflow)

---

## G.4 Hooks Mapping
**User Story**: As a System, I want to map configured hooks to Flowable listeners to execute custom code.

**Acceptance Criteria**:
- [ ] `executionListener` on `start` event -> Pre-Entry Hook.
- [ ] `executionListener` on `end` event -> Post-Exit Hook.

**Dependencies**:
- Related to: [B.3 Java Hooks](../B_Stage_Configuration.md#b3-java-hooks)

---

## G.5 SLA Mapping
**User Story**: As a System, I want to map SLA definitions to Timer Boundary Events.

**Acceptance Criteria**:
- [ ] `timerEventDefinition.timeDuration` = ISO-8601 string.
- [ ] Attached to relevant Task.

**Dependencies**:
- Related to: [F.5 SLA Timer Generation](../F_BPMN_Generation.md#f5-sla-timer-events)

---

## G.6 Completion Mapping
**User Story**: As a System, I want to map the workflow completion to a Service Task that calls the configured completion API.

**Acceptance Criteria**:
- [ ] If `completionApiEndpoint` is defined in A.1.
- [ ] Generate a `ServiceTask` before the End Event.
- [ ] `delegateExpression` = `${completionService}` which handles the HTTP call.

**Dependencies**:
- Related to: [A.1 Define Workflow](../A_Workflow_Definition.md#a1-define-workflow)
