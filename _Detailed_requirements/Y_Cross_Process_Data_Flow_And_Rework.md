# Y. Cross-Process Data Flow and Rework Patterns

## Y.1 Cross-Process Data Persistence
**User Story**: As a Workflow Designer, I want data to persist and flow seamlessly between independent workflows (e.g., ABC and XYZ) when they are invoked sequentially as Call Activities, so that context (like transaction IDs, form data, or approval status) is preserved throughout the entire lifecycle.

**Acceptance Criteria**:
- [ ] **Variable Mapping**: The system must support mapping of process variables from the Parent Workflow to the Child Workflow (Input) and from the Child Workflow back to the Parent Workflow (Output).
    - `in`: Pass variable `parentVar` -> `childVar`.
    - `out`: Pass variable `childVar` -> `parentVar` (updating the parent's state).
- [ ] **Inherit Variables Option**: Support the `inheritVariables="true"` configuration in Call Activities for simple use cases where all data should be shared.
- [ ] **Persistence**: Variables modified in Flow ABC must be available in the Parent Scope after ABC completes, ensuring they are available to be passed into Flow XYZ.
- [ ] **History**: The history service must link the Parent Process Instance with the Child Process Instances (ABC and XYZ) to allow tracing the full data lineage.

**Dependencies**:
- Related to: [A.1 Define Workflow](../A_Workflow_Definition.md)

---

## Y.2 Rework Loop Logic (XYZ -> ABC)
**User Story**: As a Workflow Designer, I want to trigger a rework loop where a decision in a later workflow (XYZ) causes the process to return to the start of an earlier workflow (ABC), ensuring previous steps can be re-executed or corrected.

**Acceptance Criteria**:
- [ ] **Rework Signal/Variable**: The XYZ workflow must effectively communicate a "Rework Requied" status back to the Parent workflow. This is typically done by setting a specific process variable (e.g., `outcome="REWORK"`, `reworkRequired=true`) before the XYZ process ends.
- [ ] **Parent Loop Gateway**: The Parent Workflow must have an Exclusive Gateway immediately following the XYZ Call Activity.
    - If `reworkRequired == true`: Flow directs back to the Start/Entry of the ABC Call Activity.
    - If `reworkRequired == false`: Flow proceeds to the next step (End).
- [ ] **State Reset**: When looping back to ABC, the system should allow (via configuration) whether to retain previous data or reset specific variables for the fresh run.

**Dependencies**:
- Dependent on: [Y.1 Cross-Process Data Persistence](#y1-cross-process-data-persistence) (to pass the rework flag).

---

## Y.3 Implementation Reference (Technical)
**User Story**: As a Developer, I need a clear pattern for implementing this using Flowable BPMN constructs.

**Implementation Details**:
1.  **Parent Process**: `Start -> [Call Activity: ABC] -> [Call Activity: XYZ] -> [Exclusive Gateway] -> End`
    *   **Gateway Logic**: Condition `${outcome == 'REWORK'}` targets `[Call Activity: ABC]`.
2.  **Call Activity ABC**:
    *   `in parameters`: Business Data.
    *   `out parameters`: Updated Business Data.
3.  **Call Activity XYZ**:
    *   `in parameters`: Business Data.
    *   `out parameters`: `outcome` (e.g., string value 'REWORK', 'APPROVED').
4.  **BPMN XML Example Snippet**:
    ```xml
    <process id="parentProcess">
      <startEvent id="start" />
      <sequenceFlow sourceRef="start" targetRef="callABC" />
      
      <callActivity id="callABC" calledElement="flowABC" flowable:inheritVariables="true">
         <!-- Optional: Explicit In/Out mapping if not inheriting -->
      </callActivity>
      <sequenceFlow sourceRef="callABC" targetRef="callXYZ" />
      
      <callActivity id="callXYZ" calledElement="flowXYZ" flowable:inheritVariables="true" />
      <sequenceFlow sourceRef="callXYZ" targetRef="checkResult" />
      
      <exclusiveGateway id="checkResult" />
      <!-- Loop back to ABC -->
      <sequenceFlow sourceRef="checkResult" targetRef="callABC">
        <conditionExpression xsi:type="tFormalExpression">${outcome == 'REWORK'}</conditionExpression>
      </sequenceFlow>
      <!-- Continue to End -->
      <sequenceFlow sourceRef="checkResult" targetRef="end">
        <conditionExpression xsi:type="tFormalExpression">${outcome == 'APPROVED'}</conditionExpression>
      </sequenceFlow>
      
      <endEvent id="end" />
    </process>
    ```

---

## Y.4 Alternative: Boundary Error Events (Cleaner Approach)
**User Story**: As a Workflow Designer, I want to use BPMN Error Events to handle rework scenarios, so that the main flow remains clean and semantic exceptions are clearly distinguished from normal logic.

**Rationale**:
This approach is "cleaner" than using gateways because:
1.  **Semantics**: It explicitly treats "Rework" as an exception to the happy path.
2.  **Simplicity**: It removes the need for an Exclusive Gateway after the Call Activity in the parent process.
3.  **Encapsulation**: The child process throws a specific error code, and the parent catches it without needing to parse generic string variables like `outcome`.

**Implementation Details**:
1.  **Child Process (XYZ)**:
    *   End Event is defined as an **Error End Event**.
    *   **Error Code**: `REWORK_REQUIRED`.
2.  **Parent Process**:
    *   **Boundary Event**: Attach an **Error Boundary Event** to the `[Call Activity: XYZ]`.
    *   **Error Ref**: Matches `REWORK_REQUIRED`.
    *   **Sequence Flow**: The flow from the Boundary Event connects directly back to `[Call Activity: ABC]`.

**BPMN XML Example Snippet**:
```xml
<!-- PARENT PROCESS -->
<process id="parentProcess">
  <startEvent id="start" />
  <sequenceFlow sourceRef="start" targetRef="callABC" />
  
  <callActivity id="callABC" calledElement="flowABC" flowable:inheritVariables="true" />
  <sequenceFlow sourceRef="callABC" targetRef="callXYZ" />
  
  <callActivity id="callXYZ" calledElement="flowXYZ" flowable:inheritVariables="true" />
  
  <!-- Happy Path -->
  <sequenceFlow sourceRef="callXYZ" targetRef="end" />
  
  <!-- Boundary Event Catching "REWORK" -->
  <boundaryEvent id="catchRework" attachedToRef="callXYZ">
    <errorEventDefinition errorRef="REWORK_ERROR" />
  </boundaryEvent>
  
  <!-- Loop Back -->
  <sequenceFlow sourceRef="catchRework" targetRef="callABC" />
  
  <endEvent id="end" />
</process>

<!-- CHILD PROCESS (XYZ) -->
<process id="flowXYZ">
  <startEvent id="xStart" />
  <sequenceFlow sourceRef="xStart" targetRef="reviewTask" />
  <userTask id="reviewTask" name="Review" />
  
  <exclusiveGateway id="decision" />
  <!-- Normal End -->
  <sequenceFlow sourceRef="decision" targetRef="xEnd">
    <conditionExpression xsi:type="tFormalExpression">${approved}</conditionExpression>
  </sequenceFlow>
  <!-- Error End -->
  <sequenceFlow sourceRef="decision" targetRef="reworkEnd">
     <conditionExpression xsi:type="tFormalExpression">${!approved}</conditionExpression>
  </sequenceFlow>
  
  <endEvent id="xEnd" />
  <endEvent id="reworkEnd">
    <errorEventDefinition errorRef="REWORK_ERROR" />
  </endEvent>
</process>
```

---

## Y.5 UI Configuration Changes
**User Story**: As a Workflow Configurator, I want to configure specific actions to trigger BPMN Error Events (Rework) instead of normal completion, so that I can define exception paths directly in the Stage/Action builder.

**Configuration Screen Changes**:
1.  **Stage Configuration -> Action List**:
    *   Existing: List of "Allowed Actions" (e.g., Approve, Reject).
    *   **New Field**: For each action, add an **"Action Type"** dropdown/selector.
        *   Options:
            1.  `Standard Completion` (Default) - Completes the task normally.
            2.  `Trigger Error / Rework` - Throws a BPMN Error.
    *   **New Field**: **"Error Code"** (Visible only if "Trigger Error" is selected).
        *   Input: Text field (e.g., `REWORK_REQUIRED`).

2.  **Parent Workflow Configuration (Call Activity)**:
    *   If a Stage is configured as a **Nested Workflow / Call Activity**:
    *   **New Section**: **"Exception Handling / Rework Paths"**.
        *   **Add Exception Rule**:
            *   **Error Code**: (Dropdown or Text, matching the Child's error codes).
            *   **Target Stage**: Dropdown of existing stages in the Parent workflow (e.g., Select "Stage A (ABC)").
    *   *Effect*: This matches the `REWORK_REQUIRED` error from the child and creates the feedback loop to the target stage.

**Updated Data Model (StageConfig / ActionConfig)**:
```json
{
  "stageName": "Review Stage",
  "actions": [
    {
      "label": "Approve",
      "type": "COMPLETION",
      "outcome": "APPROVED"
    },
    {
      "label": "Rework",
      "type": "ERROR_TRIGGER",
      "errorCode": "REWORK_REQUIRED",
      "outcome": "REWORK"
    }
  ]
}
```

**Impact on BPMN Generation**:
- **Standard Completion**: Generates normal Sequence Flows based on outcome variables (exclusive gateways).
- **Error Trigger**:
    - Inside a **User Task (Child Workflow)**: The validation logic checks the Action Type. If "Trigger Error", it generates a gateway that routes to an **End Error Event** with the specified `errorCode` instead of the normal End Event.
    - Inside a **Call Activity (Parent Workflow)**: The Generator sees the "Exception Handling" rules. It automatically attaches a **Boundary Error Event** catch to the Call Activity for each rule, and routes the Sequence Flow to the selected "Target Stage".
