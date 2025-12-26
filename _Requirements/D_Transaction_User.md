# D. Transaction User

| ID | Actor | Requirement / User Story | Type | Acceptance Criteria |
|:---|:---|:---|:---|:---|
| D.1 | Transaction User | View overall case status | Functional | /case/{id}/status ・・・ RuntimeService |
| D.2 | Transaction User | View stage status with assignees | Functional | /case/{id}/stages ・・・ TaskService |
| D.3 | Transaction User | Receive SLA/reminder notifications | Functional | Timer boundary events trigger |
