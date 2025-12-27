# O. Enhancements and Fixes Tasks

## 1. Soft Delete (Undeploy Fix)
- [ ] **WorkflowMaster.java**: Add `private String status;` (ACTIVE, DELETED).
- [ ] **DeploymentService.java**:
    - Modify `undeployWorkflow(id)` to find `WorkflowMaster` by workflowCode and set `status = "DELETED"`.
    - Remove `repositoryService.deleteDeployment(...)` call.
    - (Optionally) Suspend process definition in Flowable if needed, or rely on UI hiding. (Decided: Just DB soft delete for now, maybe suspend).
- [ ] **WorkflowController.java**:
    - Update `getAllWorkflows` (or equivalent) to filter out "DELETED" workflows if the frontend requests "active only". Or let frontend handle it.

## 2. Add Stage Modal UI
- [ ] **WorkflowEditor.tsx**:
    - Update `StageModal` (or the form inside it).
    - Wrap form fields in `<SimpleGrid cols={2} spacing="md">`.
    - Set Modal `size="lg"`.

## 3. Workflow Dashboard Stats
- [ ] **WorkflowStatsDTO.java**: Create class with fields `workflowCode`, `deployed (boolean)`, `activeInstances (long)`, `completedInstances (long)`.
- [ ] **WorkflowController.java**: Add `GET /api/workflows/stats`.
- [ ] **WorkflowList.tsx**:
    - Fetch stats on load.
    - Render status icon (Green/Gray).
    - Render counts badges.
