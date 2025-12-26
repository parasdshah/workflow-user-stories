# F. BPMN Generation - Detailed User Stories

## F.1 Automated BPMN Generator
**User Story**: As a Configurator, I want the system to automatically generate valid BPMN 2.0 XML from my metadata configuration, so that I don't have to manually draw diagrams using complex tools.

**Acceptance Criteria**:
- [ ] Service exists that takes `WorkflowMaster` and List<`StageConfig`> as input.
- [ ] Outputs a valid XML String conforming to BPMN 2.0 schema.
- [ ] Includes Start Event, Sequence Flows, Tasks/Activities, and End Event.

**Dependencies**:
- Dependent on: [A.1 Define Workflow](../A_Workflow_Definition.md)
- Prerequisite for: [F.7 Deployment](#f7-one-click-deployment)

---

## F.2 Map Stages
**User Story**: As a System, I want to map configured stages to the appropriate BPMN elements (UserTask or CallActivity), ensuring the process flows sequentially.

**Acceptance Criteria**:
- [ ] Iterate through stages sorted by `sequenceOrder`.
- [ ] If `isNestedWorkflow` is false -> Generate `UserTask`.
- [ ] If `isNestedWorkflow` is true -> Generate `CallActivity`.
- [ ] Generate `SequenceFlow` from Previous Element -> Current Element.

**Dependencies**:
- Dependent on: [B.2 Nested Workflow](../B_Stage_Configuration.md#b2-nested-workflow)
- Dependent on: [B.5 Stage Sequence](../B_Stage_Configuration.md#b5-stage-sequence)

---

## F.3 Inject Hooks
**User Story**: As a System, I want to inject the configured Java hooks as BPMN listeners, so that the code executes at the right time in the engine.

**Acceptance Criteria**:
- [ ] `preEntryHook` added as `ExecutionListener` (event=`start`) on the Activity.
- [ ] `postExitHook` added as `ExecutionListener` (event=`end`) on the Activity.
- [ ] `ImplementationType` set to `class`.

**Dependencies**:
- Dependent on: [B.3 Java Hooks](../B_Stage_Configuration.md#b3-java-hooks)

---

## F.4 Embed Screen Mapping
**User Story**: As a System, I want to embed the screen code into the BPMN UserTask, so that the frontend knows which form to render when fetching the task.

**Acceptance Criteria**:
- [ ] At generation time, lookup `ScreenMapping` for the stage.
- [ ] Set `flowable:formKey` attribute of `UserTask` to the `screenCode`.

**Dependencies**:
- Dependent on: [C.1 Map Screens](../C_Screen_Mapping.md#c1-map-screens-to-stages)

---

## F.5 SLA Timer Events
**User Story**: As a System, I want to generate boundary timer events for stages or workflows with SLAs, so that the engine triggers a timeout path.

**Acceptance Criteria**:
- [ ] Calculate ISO-8601 duration (e.g. `PT4H`) from `slaDurationDays`.
- [ ] Attach `BoundaryEvent` (cancelActivity=false) to the `UserTask`.
- [ ] Connect Boundary Event to a Service Task (Notification) or Listener.

**Dependencies**:
- Dependent on: [A.3 SLA Tracking](../A_Workflow_Definition.md#a3-sla-tracking)
- Implements: [G.5 SLA Mapping](../G_Flowable_Mapping.md#g5-sla-mapping)

---

## F.6 Preview BPMN
**User Story**: As a Configurator, I want to preview the generated BPMN XML (and a diagram if possible) before deployment, so that I can verify the logic.

**Acceptance Criteria**:
- [ ] `GET /api/workflows/{code}/preview` returns the generated XML without deploying.
- [ ] UI renders this XML using a BPMN Viewer (e.g., bpmn-js).

**Dependencies**:
- Dependent on: [F.1 Generator](#f1-automated-bpmn-generator)

---

## F.7 One-click Deployment
**User Story**: As a Configurator, I want to deploy the generated BPMN to the Flowable Engine with one click, so that the workflow becomes executable immediately.

**Acceptance Criteria**:
- [ ] `POST /api/workflows/{code}/deploy` triggers generation and deployment.
- [ ] Uses Flowable `RepositoryService.createDeployment()`.
- [ ] Returns the new Process Definition ID and Version.

**Dependencies**:
- Implemented by: [J.1 Dynamic Deployment](../J_Dynamic_Deployment.md#j1-auto-versioning)
