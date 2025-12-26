# J. Dynamic Deployment

| ID | Actor | Requirement / User Story | Type | Acceptance Criteria |
|:---|:---|:---|:---|:---|
| J.1 | Configurator | Deploy workflow with automatic version management | Technical | New deployment creates versioned process definition |
| J.2 | Configurator | Support deployment rollback to previous versions | Functional | List/revert to prior deployments via REST API |
| J.3 | System | Auto-undeploy superseded workflow versions | Technical | Keep only N latest versions (configurable) |
| J.4 | Configurator | Deploy workflow changes without stopping running instances | Functional | Flowable versioned process definitions |
| J.5 | System | Validate deployment before committing to Flowable | Technical | Schema validation + dry-run simulation |
| J.6 | Configurator | Bulk deploy multiple workflows in single operation | Functional | POST /deploy/bulk with workflow codes array |
| J.7 | System | Monitor deployment status asynchronously | Technical | WebSocket/Async response for long-running deploys |
| J.8 | Admin | View deployment history and statistics | Functional | GET /deployments?workflowCode=XXX |
