# Admin Manual Reassignment

## Overview
This feature allows authorized Admin users to intervene in active workflows and manually reassign tasks. This is critical for handling staff unavailability, workload balancing, or correcting assignment errors.

## User Stories

### Story 1: Reassigning an Assigned Case (Override)
**As an** Admin User
**I want to** reassign a case that is currently assigned to a specific user (User A) to a new user (User B)
**So that** I can ensure the case moves forward if User A is unavailable, overloaded, or incorrectly assigned.

**Acceptance Criteria:**
- [ ] Admin can view a list of open/active workflows.
- [ ] Admin can select a specific case currently assigned to a user.
- [ ] Admin can select a new assignee from valid users (optionally filtered by Role/Group).
- [ ] Upon confirmation, the task is reassigned to the new user.
- [ ] **Audit Trail:** The system must record:
    - Previous Assignee (User A).
    - New Assignee (User B).
    - Reassignment Reason (entered by Admin).
    - Date and Time of reassignment.
    - User ID of the Admin performing the action.

### Story 2: Assigning a Group Queue Case (Distribution)
**As an** Admin User
**I want to** assign a case currently sitting in a Group Queue (Unassigned) to a specific user
**So that** I can manually distribute workload or ensure urgent cases are picked up immediately.

**Acceptance Criteria:**
- [ ] Admin can view cases in Group Queues.
- [ ] Admin can select a case that has no specific assignee (Candidate Group only).
- [ ] Admin can assign the case to a specific user.
- [ ] The task status changes from "Unassigned/Queue" to "Assigned".
- [ ] **Audit Trail:** The system must record:
    - Previous State (Group Queue: [Group Name]).
    - New Assignee.
    - Assignment Reason.
    - Date and Time.
    - User ID of the Admin performing the action.

## Functional Requirements

### UI Requirements
1.  **Active Workflows Dashboard:**
    - A centralized view of all active cases.
    - Filters for: Workflow Name, Stage, Current Assignee, Status (Active/Queue).
2.  **Reassignment Modal/Screen:**
    - Display current case details (Case ID, Stage, Current Assignee/Queue).
    - Dropdown/Search to select New Assignee.
    - Text Area for "Reason for Reassignment" (Mandatory).
    - "Reassign" Confirm Button.

### Backend Requirements
1.  **API Endpoint:** New endpoint to handle administrative reassignment (e.g., `POST /api/runtime/tasks/{taskId}/reassign`).
2.  **Permission Check:** Ensure only users with `ADMIN` or `REASSIGN_PRIVILEGE` can execute this.
3.  **Audit Logging:** Store the reassignment history in a separate audit table or Flowable's identity link history with comments.

## Data Points for Audit
- `caseId`
- `taskId`
- `stageName`
- `previousAssignee` (or `previousGroup`)
- `newAssignee`
- `reason`
- `modifiedBy` (Admin User)
- `timestamp`
