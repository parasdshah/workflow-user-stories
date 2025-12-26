# B.3 Java Hooks - Technical TODOs

Currently, the system only fully supports `preEntryHook` (start) and `postExitHook` (end). The standard requires 4 distinct hooks.

## 1. Backend Mapping (`BpmnGeneratorService.java`)
The mapping logic needs to be updated to inject `TaskListener` elements for UserTasks.

### A. Post-Entry Hook
- **Current Status**: Implemented.
- **Requirement**: Run logic *after* the User Task is created/assigned but *before* user interaction.
- **Implementation**:
    - For `UserTask`: Inject `<flowable:taskListener event="create" class="..." />`.
    - **Note**: This hook is only applicable to UserTasks. For `CallActivity`, it is mapped to execution listener on start (pre-entry).

### B. Pre-Exit Hook
- **Current Status**: Implemented.
- **Requirement**: Run logic *when* the user completes the task but *before* the transaction commits/process moves on.
- **Implementation**:
    - For `UserTask`: Inject `<flowable:taskListener event="complete" class="..." />`.
    - **Note**: Similar to Post-Entry, this is best supported via TaskListeners on UserTasks.

## 2. Validation (`WorkflowDefinitionService.java`)
- **Current Status**: Implemented.
- **Requirement**: Ensure the provided class name exists on the classpath and implements `ExecutionListener` or `TaskListener`.
- **Implementation**:
    ```java
    public void validateHook(String fqn) {
        try {
            Class<?> clazz = Class.forName(fqn);
            // Check if it implements ExecutionListener or TaskListener
        } catch (ClassNotFoundException e) {
            throw new ValidationException("Hook class not found: " + fqn);
        }
    }
    ```

## 3. Frontend UI (`WorkflowEditor.tsx`)
- **Current Status**: Implemented.
- **Requirement**: Allow Configurator to input FQNs for all 4 hooks.
- **Implementation**:
    - Add 4 `TextInput` fields to the "Add/Edit Stage" modal.
    - Fields: `Pre-Entry Class`, `Post-Entry Class`, `Pre-Exit Class`, `Post-Exit Class`.
    - Bind these to the `StageConfig` model.
