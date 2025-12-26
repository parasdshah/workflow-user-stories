# L. Task History and Progress Actions Visibility

## 1. Functional Requirements

### 1.1 Task Inbox - Historical Tasks
**Goal**: Users need to see a history of tasks they have previously worked on and the actions they took.
- **UI Requirement**: The Task Inbox should have a "History" or "Completed" tab.
- **View Requirement**: Clicking on a historical task should open a view identical to the existing "View Case" (or Case Details) screen, allowing the user to see the full context and timeline.
- **Data Requirement**: The system must filter historical task instances where the current user was the assignee or participant.

### 1.2 Case Progress - Action Visibility
**Goal**: The Case Timeline (Progress View) must explicitly show the *result* or *action* taken for each completed stage, not just that it is "Completed".
- **UI Requirement**: In the Case Timeline, for "COMPLETED" stages, display the specific action (e.g., "APPROVE", "REJECT", "SUBMIT") that led to completion.
- **Visuals**: Use distinct badges or text colors for different actions (e.g., Green for Approve, Red for Reject) if possible, or simply text.
- **Data Requirement**: The `StageDTO` or timeline response must include the `outcome` variable associated with the completed task.

## 2. Technical Implementation

### 2.1 Backend Changes
- **CaseService**:
  - Update `getStages(caseId)` to fetch historical variables for each stage's task.
  - Specifically, retrieve the `outcome` variable (stored as a process variable or task variable) for completed tasks.
  - Map this `outcome` to a new field `actionTaken` in `StageDTO`.
  - Implement `getUserTaskHistory(userId)` endpoint to fetch completed tasks for the inbox.

### 2.2 Frontend Changes
- **TaskInbox.tsx**:
  - Add a "History" tab.
  - Fetch and list completed tasks.
  - Link to `/cases/{id}` (CaseView).
- **CaseTimeline.tsx**:
  - Display `stage.actionTaken` in the timeline item for completed stages.

## 3. Acceptance Criteria
- [ ] User can switch to "History" tab in Inbox.
- [ ] User sees list of completed tasks.
- [ ] Clicking a completed task opens the Case details.
- [ ] Case Timeline shows "APPROVE", "REJECT", etc., for completed stages.
