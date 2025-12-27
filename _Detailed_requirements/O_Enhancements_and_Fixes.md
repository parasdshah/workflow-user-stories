# O. Enhancements and Fixes

## 1. Bugs
### 1.1 History Retention on Undeploy
**Issue**: Currently, undeploying a workflow cascades and deletes all runtime and historic process instances associated with it.
**Requirement**: "I don't want that to happen."
**Solution**: 
- **Soft Delete Strategy**: Do NOT call Flowable's `deleteDeployment` (which cascades).
- Add a `status` field to `WorkflowMaster` (e.g., "ACTIVE", "INACTIVE", "DELETED").
- `undeployWorkflow` will merely update this status to "DELETED" or "INACTIVE".
- The Workflow List UI should filter or visually mark these as undeployed.
- **Decision**: Mark as soft delete in local DB. Do NOT change cascade settings (i.e. don't use deleteDeployment at all).

## 2. Functional Enhancements
### 2.1 Add Stage UI Layout
**Issue**: "Add Stage UI has lot of scroll".
**Requirement**: optimize layout.
**Solution**:
- Update `WorkflowEditor.tsx` modal.
- Use a 2-column Grid layout for form fields.
- Make the modal wider (`size="lg"` or `xl`).

### 2.2 Workflow Dashboard Stats
**Issue**: "Show using icon which of the workflow are deployed and how many active tasks are running on each workflow and how many completed cases are there in each workflow."
**Requirement**: Dashboard visualization.
**Solution**:
- Update `WorkflowList.tsx`.
- For each workflow card/row, display:
    - **Status Icon**: Green dot for deployed (active), Gray for undeployed.
    - **Active Tasks**: Count of `runtimeService.createProcessInstanceQuery().processDefinitionKey(key).active().count()`.
    - **Completed Cases**: Count of `historyService.createHistoricProcessInstanceQuery().processDefinitionKey(key).finished().count()`.
- **Backend**: Add a new DTO `WorkflowStatsDTO` and an endpoint `GET /api/workflows/stats` to aggregate this data efficiently.
