# N. Nested Workflow Visibility

## 1. Functional Requirements

### 1.1 Parent Context Identification
**Goal**: Users viewing a sub-process (child workflow) must be able to identify and navigate to its parent workflow.
- **Backend**: The system must expose the `parentCaseId` (Super Process Instance ID) for every case.
- **UI**: 
    - If a case has a parent, display a "Parent Context" indicator (e.g., Breadcrumb or Header Badge).
    - Provide a clickable link to navigate to the Parent Case.

## 2. Technical Implementation

### 2.1 Backend Changes
- **CaseDTO**: Add `private String parentCaseId;`.
- **CaseService**:
    - `mapToCaseDTO(ProcessInstance)`: Populate `parentCaseId` from `processInstance.getSuperProcessInstanceId()`.
    - `mapToCaseDTO(HistoricProcessInstance)`: Populate `parentCaseId` from `historicProcessInstance.getSuperProcessInstanceId()`.

### 2.2 Frontend Changes (Future Scope)
- Update `CaseView` to check for `parentCaseId` and render navigation elements.
