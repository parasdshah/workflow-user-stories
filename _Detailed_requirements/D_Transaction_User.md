# D. Transaction User - Detailed User Stories

## D.1 View Case Status
**User Story**: As a Transaction User, I want to view the overall status of my case (e.g., Loan Application), so that I know if it is Active, Completed, or Terminated.

**Acceptance Criteria**:
- [ ] `GET /api/runtime/cases/{id}` returns case details.
- [ ] Status is derived from the Flowable Process Instance (`ACTIVE` or `ENDED`).

**Dependencies**:
- Dependent on: [Execution of Workflow](../G_Flowable_Mapping.md)

---

## D.2 View Stage Status
**User Story**: As a Transaction User, I want to see which stage the case is currently in and who it is assigned to, so that I can identify bottlenecks.

**Acceptance Criteria**:
- [ ] `GET /api/runtime/cases/{id}/stages` returns active tasks.
- [ ] Returns Stage Name, Stage Code, Created Time, and Assignee.
- [ ] Completed stages can be retrieved from History Service.

**Dependencies**:
- Dependent on: [B.1 Stage Configuration](../B_Stage_Configuration.md)

---

## D.3 SLA Notifications
**User Story**: As a Transaction User or Manager, I want to receive notifications when an SLA is breached or a reminder is set, so that I can take timely action.

**Acceptance Criteria**:
- [ ] Timer Boundary Event fires when duration is exceeded.
- [ ] Listener attached to the timer triggers the Notification Service.
- [ ] Notification contains case context and stage details.

**Dependencies**:
- Dependent on: [A.3 SLA Tracking](../A_Workflow_Definition.md#a3-sla-tracking)
- Dependent on: [F.5 SLA Timer Gen](../F_BPMN_Generation.md#f5-sla-timer-events)
