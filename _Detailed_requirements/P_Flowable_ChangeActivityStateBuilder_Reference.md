# Flowable ChangeActivityStateBuilder Reference

## Overview
The **`ChangeActivityStateBuilder`** is a powerful API in Flowable key for implementing "Jump" or "GoTo" features. It allows the execution token of a process instance to be moved arbitrarily from one activity (node) to another, bypassing standard sequence flows.

## Use Cases
1.  **Return to Maker / Rewind**: Moving a workflow back to an earlier stage (e.g., from Review back to Initiation).
2.  **Skipping Steps**: Bypassing tasks dynamically.
3.  **Ad-hoc Flow**: Handling exceptions by jumping to error handling states.

## Java API Usage

Access via `RuntimeService`.

### 1. Basic Jump (One Activity to Another)
Move from a current activity to a target activity.

```java
@Autowired
private RuntimeService runtimeService;

public void jumpToTask(String processInstanceId, String currentActivityId, String targetActivityId) {
    runtimeService.createChangeActivityStateBuilder()
        .processInstanceId(processInstanceId)
        .moveActivityIdTo(currentActivityId, targetActivityId) // From -> To
        .changeState();
}
```
*   **`currentActivityId`**: The ID of the activity where the token currently IS (e.g., `"reviewTask"`).
*   **`targetActivityId`**: The ID of the activity where you want the token TO BE (e.g., `"initialStep"`).

### 2. Passing Variables During Jump
Update process variables as part of the state change transaction.

```java
runtimeService.createChangeActivityStateBuilder()
    .processInstanceId(processInstanceId)
    .moveActivityIdTo("reviewTask", "initialSubmission")
    .processVariable("status", "NEEDS_CORRECTION")
    .processVariable("rejectionReason", "Missing documents")
    .changeState();
```

### 3. Handling Parallel Gateways
*   **Many-to-One**: Collecting multiple parallel tokens to a single wait state.
    ```java
    List<String> currentActivityIds = Arrays.asList("parallelTaskA", "parallelTaskB");
    runtimeService.createChangeActivityStateBuilder()
        .processInstanceId(procId)
        .moveActivityIdsToSingleActivityId(currentActivityIds, "afterJoinTask")
        .changeState();
    ```

*   **One-to-Many**: Moving from a single task *into* a parallel split.
    ```java
    runtimeService.createChangeActivityStateBuilder()
        .processInstanceId(procId)
        .moveSingleActivityIdToActivityIds("singleTask", Arrays.asList("parallelTaskA", "parallelTaskB"))
        .changeState();
    ```

## Important Considerations

1.  **Task Cancellation**: Moving away from a User Task deletes/cancels it. It will have a `DELETE_REASON_` in history and is not considered "completed".
2.  **Activity IDs**: Use the BPMN XML `id` attribute, NOT the runtime `taskId`.
3.  **Execution ID Volatility**: Execution IDs (`ACT_RU_EXECUTION`) may change during the jump. Do not persist them across this operation.
4.  **Transactionality**: The jump is atomic. If the target state fails to initialize, the jump rolls back.

## Example Implementation

A "Reject to First Stage" implementation pattern:

```java
public void rejectToFirstStage(String caseId) {
    // 1. Get the current active task/activity
    Task currentTask = taskService.createTaskQuery().processInstanceId(caseId).singleResult();
    String currentActivityId = currentTask.getTaskDefinitionKey();
    
    // 2. Define target (BPMN ID of first stage)
    String firstStageActivityId = "stage_001_initial_entry"; 

    // 3. Execute Jump
    runtimeService.createChangeActivityStateBuilder()
        .processInstanceId(caseId)
        .moveActivityIdTo(currentActivityId, firstStageActivityId)
        .processVariable("caseStatus", "RETURNED") 
        .changeState();
}
```
