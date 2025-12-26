# A. Workflow Definition - Detailed User Stories

## A.1 Define Workflow
**User Story**: As a Configurator, I want to define a new workflow with a unique name, code, and completion API endpoint, so that the system can identify and execute the process and notify external systems upon completion.

**Acceptance Criteria**:
- [ ] System accepts a unique `workflowCode` (alphanumeric, no spaces).
- [ ] System accepts a display-friendly `workflowName`.
- [ ] System accepts an optional `completionApiEndpoint` URL.
- [ ] Error is raised if `workflowCode` already exists.
- [ ] On defining, status is set to 'DRAFT' or 'ACTIVE'.
- [ ] The completion API is successfully called with the case payload when the workflow instance reaches the End Event.

**Dependencies**:
- Related to: [A.6 Audit Trail](./A_Workflow_Definition.md#a6-audit-trail)
- Prerequisite for: [F.1 BPMN Generation](../F_BPMN_Generation.md#f1-automated-bpmn-generator)

---

## A.2 Reminder Configuration
**User Story**: As a Configurator, I want to configure notification templates for Reminder 1 and Reminder 2, so that users can be nudged if they delay tasks.

**Acceptance Criteria**:
- [ ] UI allows selection/entry of `reminderTemplateId1` and `reminderTemplateId2` at the workflow level.
- [ ] Stored template IDs correspond to valid templates in the Notification Engine (mock or real).
- [ ] These defaults can be overridden at the Stage level (B.1).

**Dependencies**:
- Related to: [D.3 SLA Notifications](../D_Transaction_User.md#d3-sla-notifications)
- Dependent on: [Notification Service] (External)

---

## A.3 SLA Tracking
**User Story**: As a Configurator, I want to set an SLA duration in decimal days (e.g., 0.5 days), so that the system can track performance and trigger escalations.

**Acceptance Criteria**:
- [ ] System accepts decimal input (e.g., 0.5 for 12 hours, 1.5 for 36 hours).
- [ ] Value is stored as `BigDecimal`.
- [ ] Backend converts decimal days to ISO-8601 duration string (e.g., `PT12H`) for Flowable timers.

**Dependencies**:
- Prerequisite for: [F.5 BPMN SLA Timer Generation](../F_BPMN_Generation.md#f5-sla-timer-events)
- Prerequisite for: [I.3 SLA Validation](../I_Validation.md#i3-sla-validation)

---

## A.4 Module Association
**User Story**: As a Configurator, I want to associate a workflow with a specific module (e.g., Sales, HR), so that it appears in the relevant context within the application.

**Acceptance Criteria**:
- [ ] UI provides a dropdown or input for `associatedModule`.
- [ ] System persists this association.
- [ ] Workflow List API supports filtering by Module.

**Dependencies**:
- None

---

## A.5 CRUD Operations
**User Story**: As a Configurator, I want to perform Create, Read, Update, and Delete operations on workflows via REST APIs, so that I can manage the lifecycle of definitions.

**Acceptance Criteria**:
- [ ] `POST /api/workflows`: Create new.
- [ ] `PUT /api/workflows/{id}`: Update existing.
- [ ] `GET /api/workflows`: List (paginated).
- [ ] `GET /api/workflows/{code}`: Get details.
- [ ] `DELETE /api/workflows/{code}`: Soft delete or Archive (if no running instances).

**Dependencies**:
- Prerequisite for: [Frontend UI](../Workflow_UI.md)

---

## A.6 Audit Trail
**User Story**: As a Configurator, I want an audit trail of changes to workflow configurations, so that I can trace who changed what and when.

**Acceptance Criteria**:
- [ ] `AuditTrail` table records: `entityName` (Workflow/Stage), `entityId`, `action` (INSERT/UPDATE), `changedBy`, `changedAt`, `changes` (JSON Diff).
- [ ] Every save operation triggers an audit log entry.
- [ ] API available to view audit history for a specific workflow.

**Dependencies**:
- Dependent on: [A.1 Define Workflow](#a1-define-workflow)
