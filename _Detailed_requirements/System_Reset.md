# System Reset Utility

## 1. Overview
The System Reset Utility provides a mechanism to completely clean the system state. This is useful for development, testing, and resetting environments. It ensures that all workflow definitions, instances, and associated configuration data are removed, effectively returning the system to a "clean slate" (preserving the schema structure but wiping data).

## 2. Functional Requirements

### 2.1. Reset API
-   **Endpoint**: `POST /api/system/reset`
-   **Description**: Triggers the system reset process.
-   **Response**: 200 OK on success, 500 Internal Server Error on failure.

### 2.2. Scope of Reset
The reset operation must perform the following actions:
1.  **Undeploy Workflows**:
    -   Terminate all running process instances.
    -   Delete all deployment definitions from the Flowable engine.
    -   Clear Flowable history (optional/implicit in cascade delete).
2.  **Clean Configuration Data**:
    -   Delete all `StageConfig` records (and associated `StageAction`s).
    -   Delete all `WorkflowMaster` records.
    -   Delete all `AuditLog` records.
    -   Delete any uploaded DMN rules (`RuleRepository`).

## 3. Technical Requirements

### 3.1. Service Component
-   **Class**: `com.workflow.service.service.SystemResetService`
-   **Logic**:
    -   Inject `RepositoryService` to fetch and delete deployments.
    -   Use `repositoryService.deleteDeployment(deploymentId, true)` with `cascade=true` to ensure instances are removed.
    -   Inject JPA Repositories (`StageConfigRepository`, `WorkflowMasterRepository`, etc.) and use `deleteAll()` methods.
    -   Ensure transactional integrity logic (or best-effort sequence).

### 3.2. Controller
-   **Class**: `com.workflow.service.controller.SystemController`
-   **Security**: This endpoint should ideally be protected or restricted to ADMIN roles (future scope).

## 4. Acceptance Criteria
-   After calling the reset API, `ACT_RE_DEPLOYMENT` table should be empty.
-   `stage_config`, `stage_actions`, `workflow_master` tables should be empty.
-   The system should remain stable and ready for new deployments/configurations immediately after reset.
