# L. Task History and Progress Actions Visibility Tasks

## 1. Backend Implementation (workflow-service)
- [x] **DTO Update**: Update `StageDTO` to include `actionTaken` (String). <!-- id: 11 -->
- [x] **CaseService**: Update `getStages(caseId)` logic. <!-- id: 12 -->
    - Fetch historic task instances for the case.
    - For each completed stage/task, retrieve the task-local variable or process variable `outcome`.
    - Map `outcome` to `StageDTO.actionTaken`.
- [x] **Task History API**: Implement `GET /api/runtime/tasks/history` (or similar). <!-- id: 13 -->
    - Fetch completed tasks where `assignee` matches current user.
    - Return list of tasks with case details (Case ID, Name, End Time).

## 2. Frontend Implementation (workflow-ui)
- [x] **CaseTimeline Update**: Modify `CaseTimeline.tsx`. <!-- id: 14 -->
    - Update `StageDTO` interface in frontend.
    - Check if `status === 'COMPLETED'` and `actionTaken` is present.
    - Display `actionTaken` (e.g., as a Badge or Text) in the timeline item.
    - Differentiate colors (e.g., 'REJECT' -> red, 'APPROVE' -> green).
- [x] **Task Inbox History**: Modify `TaskInbox.tsx`. <!-- id: 15 -->
    - Add "Active" and "History" tabs.
    - Implement fetching of historical tasks.
    - Render historical tasks list (similar to active but read-only link).
