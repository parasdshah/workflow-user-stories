# M. Advanced Deployment Features

## 1. Functional Requirements

### 1.1 Undeploy Workflow
**Goal**: Allow administrators to remove a specific deployment of a workflow.
- **UI Requirement**: In the "Deployment History" list, each row should have an "Undeploy" or "Delete" button.
- **Interaction**: Clicking "Undeploy" must trigger a confirmation modal warning about potential data loss (if cascading).
- **Backend Behavior**: The system should delete the deployment from the Flowable engine.
- **Constraints**: 
    - Should likely cascade to delete running instances associated with that deployment ID to avoid orphaned states.

### 1.2 Rollback Workflow
**Goal**: Allow administrators to revert to a previous version of a workflow.
- **UI Requirement**: In the "Deployment History" list, each row (representing a past version) should have a "Rollback" button.
- **Interaction**: 
    - Clicking "Rollback" opens a modal.
    - The modal must require the user to type **"ROLLBACK"** (case-sensitive) to confirm the action.
- **Backend Behavior**:
    - The system identifies the BPMN XML resource associated with the selected deployment ID.
    - It re-deploys this XML as a *new* version (Deployment) to become the active definition.
    - It does *not* delete the distinct history of the previous versions, just restores the logic.

## 2. Technical Implementation

### 2.1 Backend Changes
- **DeploymentService**:
    - `undeploy(String deploymentId)`: Call `repositoryService.deleteDeployment(deploymentId, true)`.
    - `rollback(String deploymentId)`: 
        - Fetch process definition for deployment.
        - `repositoryService.getResourceAsStream(deploymentId, resourceName)`.
        - `deployWorkflow(xmlString)`.
- **DeploymentController**:
    - `DELETE /api/deployments/{id}`
    - `POST /api/deployments/{id}/rollback`

### 2.2 Frontend Changes
- **DeploymentHistory.tsx**:
    - Add "Actions" column.
    - Implement "Undeploy" button with confirmation Modal.
    - Implement "Rollback" button with "Type ROLLBACK" Modal.
