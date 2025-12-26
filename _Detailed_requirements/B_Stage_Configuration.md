# B. Stage Configuration - Detailed User Stories

## B.1 Configure Stage Details
**User Story**: As a Configurator, I want to configure stage details including name, code, case status, and notification templates, so that the workflow has distinct steps with specific behaviors.

**Acceptance Criteria**:
- [ ] Unique `stageCode` within the workflow.
- [ ] `stageName` for display.
- [ ] `caseStatus` to update the overall case status when this stage is active (e.g., 'In Review').
- [ ] Option to set Stage-specific SLA and Reminder templates (overriding Workflow defaults).

**Dependencies**:
- Dependent on: [A.1 Define Workflow](../A_Workflow_Definition.md#a1-define-workflow)
- Prerequisite for: [F.2 Map Stages](../F_BPMN_Generation.md#f2-map-stages)

---

## B.2 Nested Workflow
**User Story**: As a Configurator, I want to mark a stage as a "Workflow" type and provide a nested workflow code, so that complex processes can be modularized and reused.

**Acceptance Criteria**:
- [ ] UI checkbox for `isNestedWorkflow`.
- [ ] If checked, `nestedWorkflowCode` input is mandatory.
- [ ] Backend generates a BPMN `CallActivity` referencing the nested code.
- [ ] Validation ensures the nested workflow code exists.

**Dependencies**:
- Prerequisite for: [G.3 Call Activity Mapping](../G_Flowable_Mapping.md#g3-workflow-stage-callactivity)
- Dependent on: [A.1 Define Workflow](../A_Workflow_Definition.md#a1-define-workflow) (Defining the Child workflow)

---

## B.3 Java Hooks
**User Story**: As a Configurator, I want to define Java hooks (Pre/Post Entry, Pre/Post Exit), so that custom logic (e.g., external API calls, data transformation) can run at specific points.

**Acceptance Criteria**:
- [ ] Inputs for Fully Qualified Name (FQN) of Java classes implementing `ExecutionListener` or `JavaDelegate`.
- [ ] `preEntryHook`: Runs on `start` event of the activity.
- [ ] `postExitHook`: Runs on `end` event of the activity.
- [ ] System verifies these classes exist on the classpath (I.2).

**Dependencies**:
- Prerequisite for: [G.4 Hooks Mapping](../G_Flowable_Mapping.md#g4-hooks-mapping)
- Related to: [I.2 Hook Validation](../I_Validation.md#i2-hook-class-check)

---

## B.4 Cyclical Dependency Validation
**User Story**: As a Configurator, I want the system to validate against cyclical dependencies in stage configuration, so that infinite loops in the process are preventing.

**Acceptance Criteria**:
- [ ] Application runs a graph traversal algorithm (DFS) on save.
- [ ] Detects loops in stage sequences (e.g., A -> B -> A).
- [ ] Rejects the save operation with a descriptive error if a cycle is found.

**Dependencies**:
- Dependent on: [B.5 Execution Sequence](#b5-stage-sequence)
- Implemented by: [I.1 Cycle Detection](../I_Validation.md#i1-cycle-detection)

---

## B.5 Stage Sequence
**User Story**: As a Configurator, I want to define the order of execution for stages using a sequence order, so that the BPMN generator knows how to connect them.

**Acceptance Criteria**:
- [ ] `sequenceOrder` integer field.
- [ ] Stages are generated in the BPMN `process` in ascending order of this sequence.
- [ ] `SequenceFlow` elements connect Stage N to Stage N+1.

**Dependencies**:
- Prerequisite for: [F.2 Map Stages](../F_BPMN_Generation.md#f2-map-stages)
