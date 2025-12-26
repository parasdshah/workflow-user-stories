# J. Dynamic Deployment - Detailed User Stories

## J.1 Auto Versioning
**User Story**: As a Configurator, I want the system to handle versioning automatically, so that I don't overwrite history when deploying updates.

**Acceptance Criteria**:
- [ ] Flowable Engine automatically increments version on deployment of a process with the same key.
- [ ] New instances use the latest version.
- [ ] Old instances continue on their original version.

**Dependencies**:
- Relies on: [F.7 Deployment](../F_BPMN_Generation.md#f7-one-click-deployment)

---

## J.2 Rollback Support
**User Story**: As a Configurator, I want to rollback to a previous deployment version, so that I can quickly recover from a bad configuration.

**Acceptance Criteria**:
- [ ] `GET /api/deployments?workflowCode=X` to list versions.
- [ ] `POST /api/deployments/rollback` with target version ID.
- [ ] System re-deploys the old XML as the new latest head.

**Dependencies**:
- Relies on: [J.8 Deployment History](#j8-deployment-history)

---

## J.3 Auto-undeploy
**User Story**: As a System, I want to auto-undeploy superseded versions if they have no running instances, to save database space.

**Acceptance Criteria**:
- [ ] Configurable limit (e.g., keep last 5 versions).
- [ ] Scheduled job checks for old, unused definitions and deletes them.

**Dependencies**:
- None

---

## J.4 Hot Deployment
**User Story**: As a Configurator, I want to deploy workflow changes without stopping the server or running instances.

**Acceptance Criteria**:
- [ ] Deployment happens at runtime via API.
- [ ] No server restart required.
- [ ] Active instances are unaffected (they stick to their version definition).

**Dependencies**:
- Relies on: [Flowable Engine Architecture](../H_Infrastructure.md)

---

## J.5 Pre-deployment Validation
**User Story**: As a System, I want to validate the deployment (Dry Run) before committing, ensuring valid BPMN.

**Acceptance Criteria**:
- [ ] Parse BPMN via `BpmnXMLConverter`.
- [ ] Check for model errors (disconnected nodes, missing gateways).
- [ ] Report errors before `repositoryService.deploy()`.

**Dependencies**:
- Prerequisite for: [F.7 Deployment](../F_BPMN_Generation.md#f7-one-click-deployment)

---

## J.6 Bulk Deployment
**User Story**: As a Configurator, I want to deploy multiple workflows in a single operation, so that I can promote a full release set.

**Acceptance Criteria**:
- [ ] `POST /api/deployments/bulk` accepts list of codes.
- [ ] Iterates and deploys each.
- [ ] Transactional: All succeed or all fail (optional, or report partial success).

**Dependencies**:
- None

---

## J.7 Async Monitor
**User Story**: As a System, I want to monitor long-running deployments asynchronously, preventing timeouts.

**Acceptance Criteria**:
- [ ] Return 202 Accepted and a Job ID.
- [ ] Client polls for status.

**Dependencies**:
- None

---

## J.8 Deployment History
**User Story**: As an Admin, I want to view the history of deployments for a workflow, including timestamps and version numbers.

**Acceptance Criteria**:
- [ ] Fetch inputs from `RepositoryService.createDeploymentQuery()`.
- [ ] Display Version, Deploy Time, and Name.

**Dependencies**:
- Prerequisite for: [J.2 Rollback](#j2-rollback-support)
