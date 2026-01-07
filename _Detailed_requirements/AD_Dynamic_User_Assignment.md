# Dynamic User Assignment & Global Scope Lookup

## 1. Objective
To dynamically assign users to tasks based on historical data within the same case, ensuring continuity.
- **Scenario**: A workflow loops back to a previous stage (A -> B -> C -> A). The user who performed 'A' initially should be assigned 'A' again, provided they remain active.
- **Advanced Scenario**: This logic must work even if the previous occurrence of 'A' was in a parent process or a parallel sibling process (Cross-Process / Nested Workflow support).

## 2. Requirement: Recurring Assignment
When a specific stage (e.g., "Stage A") is created:
1.  Check the execution history for this Case (Business Key).
2.  Find the **last completed** instance of "Stage A".
3.  Retrieve the `assignee` of that task.
4.  Validation: Check if the user is still **ACTIVE** in the system.
    - **If Active**: Assign the new task to this user.
    - **If Inactive/Not Found**: Fallback to default assignment (e.g., Round Robin, Candidate Group, or specific default user).

## 3. Requirement: Global Scope (Cross-Process)
Assignee lookup must traverse the entire Case hierarchy, not just the local Process Instance.
- **Problem**: Flowable creates a new `ProcessInstanceId` for every `CallActivity` (Nested Workflow). A query limited to `delegateTask.getProcessInstanceId()` will fail to find tasks performed in the Parent or Sister processes.
- **Solution**: Search by **Business Key** (Case ID) to retrieve history from the Global Scope.

### 3.1 Key Implementation Details
1.  **Business Key Propagation**: 
    - The `CallActivity` element in BPMN **MUST** be configured to inherit the Business Key.
    - Flowable Attribute: `inheritBusinessKey="true"`
    - *Action Item*: Update `BpmnGeneratorService.java` to set this attribute for all nested workflows.

2.  **Hook Logic (TaskListener)**:
    - The logic resides in a `TaskListener` bound to the `create` event (Post Entry Hook).
    - **Query Strategy**:
      ```java
      historyService.createHistoricTaskInstanceQuery()
          .processInstanceBusinessKey(caseId) // <--- Scopes to the entire Case
          .taskDefinitionKey(targetStageCode) // e.g., "Stage_A"
          .finished()
          .orderByHistoricTaskInstanceEndTime().desc()
          .list();
      ```

## 4. Configuration Model
To make this reusable, `StageConfig` should ideally support parametrizing this behavior (though currently implemented via custom Java hooks).

### Proposed Configuration Fields (Future Enhancement)
- `assignmentStrategy`: `HISTORY_BASED` | `DEFAULT`
- `assignmentSourceStage`: `Stage_A` (If looking up a different stage's user)
- `fallbackAssignee`: User/Group to use if historical user is invalid.

## 5. Technical Implementation (Current)
Until the full configuration model is added, use the **Post Entry Hook** field in the Stage Configuration.

**Class**: `com.workflow.service.hooks.PreviousUserAssignmentHook` (Example)
**Trigger**: Task `create` event.

### Algorithm
1.  **Get Context**: `caseId = delegateTask.getVariable("caseId")` (or `getBusinessKey()`).
2.  **Define Target**: Determine which stage definition to look up (default: same stage code).
3.  **Query History**: Search `HistoricTaskInstance` by `processInstanceBusinessKey` + `taskDefinitionKey`.
4.  **Validate User**: Call `UserService.isActive(userId)`.
5.  **Assign**: `delegateTask.setAssignee(userId)`.
