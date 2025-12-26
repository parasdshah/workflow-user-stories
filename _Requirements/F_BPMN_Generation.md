# F. BPMN Generation

| ID | Actor | Requirement / User Story | Type | Acceptance Criteria |
|:---|:---|:---|:---|:---|
| F.1 | Configurator | Automated BPMN XML generator service | Technical | Valid Flowable BPMN 2.0 XML |
| F.2 | System | Map stages to userTask/callActivity | Technical | Sequential sequenceFlow generation |
| F.3 | System | Inject hooks as BPMN listeners | Technical | executionListener/taskListener tags |
| F.4 | System | Embed screen mappings as formKey | Technical | flowable:formKey=screenCode |
| F.5 | System | Generate SLA/reminder timer events | Technical | timerDuration from SLA days |
| F.6 | Configurator | Preview BPMN XML before deployment | Functional | /workflow/{code}/bpmn/preview |
| F.7 | Configurator | One-click BPMN deployment | Functional | /workflow/{code}/deploy ・・・ RepositoryService |
