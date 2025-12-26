# G. Flowable Mapping

| ID | Actor | Requirement / User Story | Type | Acceptance Criteria |
|:---|:---|:---|:---|:---|
| G.1 | System | Workflow ・・・ process element | Technical | id=name from WorkflowMaster |
| G.2 | System | Normal stage ・・・ userTask | Technical | formKey=screenCode |
| G.3 | System | Workflow stage ・・・ callActivity | Technical | calledElement=nestedWorkflowCode |
| G.4 | System | Hooks ・・・ executionListener/taskListener | Technical | Event: start/create/complete/end |
| G.5 | System | SLA ・・・ timerBoundaryEvent | Technical | timerDuration=PT{slaHours}H |
| G.6 | System | Completion ・・・ serviceTask | Technical | delegateExpression=completionService |
