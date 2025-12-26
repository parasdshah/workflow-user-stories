# B. Stage Configuration

| ID | Actor | Requirement / User Story | Type | Acceptance Criteria |
|:---|:---|:---|:---|:---|
| B.1 | Configurator | Configure stage name, code, case status, notifications | Functional | Unique stage per workflow; template linkage |
| B.2 | Configurator | Mark stages as Normal/Workflow with nested workflow code | Functional | callActivity for workflow stages |
| B.3 | Configurator | Define pre-entry, post-entry, pre-exit, post-exit Java hooks | Technical | Class FQNs ・・・ ExecutionListener/TaskListener |
| B.4 | Configurator | Validate for cyclical stage dependencies | Technical | Graph algorithm rejects loops |
| B.5 | Configurator | Define stage execution sequence | Functional | sequence_order ・・・ sequenceFlow |
