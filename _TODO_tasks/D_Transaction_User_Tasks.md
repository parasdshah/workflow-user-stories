# D. Transaction User - Detailed Tasks

## D.1 View Case Status
- [x] **Backend API**:
    - [x] Create/Update `CaseController` with `GET /api/runtime/cases/{id}`.
    - [x] Implement `CaseService.getCaseDetails(String id)`.
    - [x] Return DTO with: Case ID, Workflow Name, Current Status (Active/Ended), Start Time.
- [x] **Frontend**:
    - [x] Update `TaskInbox` or Case View to show overall status badges.

## D.2 View Stage Status
- [x] **Backend API**:
    - [x] Create/Update `CaseController` with `GET /api/runtime/cases/{id}/stages`.
    - [x] Implement `CaseService.getActiveStages(String id)`.
    - [x] Map Flowable `Task` to internal `StageDTO`.
    - [x] Return: Stage Name, Stage Code, Assignee, Created Time, Due Date.
- [x] **Frontend**:
    - [x] Create `CaseTimeline` component.
    - [x] Visualize active vs completed stages.

## D.3 SLA Notifications
- [x] **Backend verification**:
    - [x] Ensure `SlaNotificationService` is triggered correctly (already implemented in G.5).
    - [x] (Optional) Enhance `SlaNotificationService` to send real email/mock log with detailed context.
- [x] **Frontend**:
    - [x] Display "Breached" status on case/stage if SLA is exceeded.

## D.4 Workflow Initiation (New)
- [x] **Backend API**:
    - [x] Create `POST /api/runtime/cases` to start a new process instance.
    - [x] Payload: `workflowCode`, `initialVariables` (Map).
    - [x] Return: `caseId` (Process Instance ID).
- [x] **Frontend**:
    - [x] Add "Start Case" button on `WorkflowList` or dedicated `NewCase` page.
    - [x] Form to select Workflow Code (if not on list) and input initial data.
    - [x] Navigate to Case View upon success.
- [x] **Test Cases**:
    - [x] Update `CaseControllerTest` for initiation.
    - [x] Integration test: Start workflow -> Check status Active.

## Dependencies
- [x] G.5 SLA Mapping (Completed)
- [x] Workflow Execution (Core engine running)
