# N. Nested Workflow Visibility Tasks

## 1. Backend Implementation
- [ ] **CaseDTO Update**: Add `parentCaseId` field.
- [ ] **CaseService Update**: Map `superProcessInstanceId` to `parentCaseId` for both Runtime and History queries.

## 2. Verification
- [ ] **Manual Verification**: 
    - Trigger a nested workflow.
    - Fetch child case details via API.
    - Verify `parentCaseId` is populated with the parent's ID.
