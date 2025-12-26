# C. Screen Mapping - Detailed User Stories

## C.1 Map Screens to Stages
**User Story**: As a Configurator, I want to map a specific screen (UI form) to a workflow stage, so that the end-user sees the correct form when performing the task.

**Acceptance Criteria**:
- [ ] `StageScreenMapping` table stores `stageCode` and `screenCode`.
- [ ] Relation is One-to-One or Many-to-One (One stage typically has one main screen).
- [ ] BPMN generator embeds this `screenCode` as the `formKey` in the User Task.

**Dependencies**:
- Dependent on: [B.1 Stage Configuration](../B_Stage_Configuration.md#b1-configure-stage-details)
- Prerequisite for: [F.4 Embed Form Key](../F_BPMN_Generation.md#f4-embed-screen-mapping)

---

## C.2 Access Type
**User Story**: As a Configurator, I want to mark screens as `EDITABLE` or `READ_ONLY` for a specific stage, so that I can control data integrity based on the process phase.

**Acceptance Criteria**:
- [ ] `accessType` enum (`EDITABLE`, `READ_ONLY`) stored in mapping.
- [ ] Frontend uses this flag to disable/enable form inputs.

**Dependencies**:
- Prerequisite for: [Frontend Rendering](../Workflow_UI.md)

---

## C.3 Reuse Screens
**User Story**: As a Configurator, I want to reuse existing screen definitions across different workflows, so that I don't have to rebuild common forms (e.g., Customer Details).

**Acceptance Criteria**:
- [ ] Separate `ScreenDefinition` entity storing JSON layout/metadata.
- [ ] Configurator can select from a list of existing `ScreenDefinition`s when mapping to a stage.

**Dependencies**:
- Related to: [E.1 Data Model](../E_Data_Model.md)
