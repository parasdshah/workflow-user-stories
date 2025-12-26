# M. Advanced Deployment Tasks

## 1. Backend Implementation (workflow-service)
- [x] **DeploymentService - Undeploy**: Implement `undeployWorkflow(String deploymentId)`.
- [x] **DeploymentService - Rollback**: Implement `rollbackWorkflow(String deploymentId)`.
    - Retrieve resource name via `repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult()`.
    - Get InputStream via `repositoryService.getResourceAsStream(...)`.
    - Re-deploy.
- [x] **DeploymentController Endpoints**:
    - `DELETE /api/deployments/{id}`
    - `POST /api/deployments/{id}/rollback`

## 2. Frontend Implementation (workflow-ui)
- [x] **DeploymentHistory - UI Updates**:
    - Add `Actions` column to the table.
- [x] **Undeploy Feature**:
    - Add "Delete" icon/button.
    - Add Confirmation Modal ("Are you sure? This will delete process instances...").
    - Call DELETE API.
- [x] **Rollback Feature**:
    - Add "Rollback" icon/button.
    - Add Verification Modal ("Type ROLLBACK to confirm").
    - Input validation for "ROLLBACK" string.
    - Call POST Rollback API.

## 3. Verification
- [ ] **Automated Tests**: Create `DeploymentFeaturesTest.java`.
    - Test `undeployWorkflow`.
    - Test `rollbackWorkflow` (deploy V1, deploy V2, rollback to V1).
- [ ] **Manual Verification**: Verify UI flows.
