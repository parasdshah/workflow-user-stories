# Y. Cross-Process Data Flow and Rework Patterns

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

---

## Y.6 Export/Import Support
**User Story**: As an Administrator, I want the Export/Import functionality to include the new Action Types and Exception Handling rules, so that I can migrate complex Rework/Error patterns between environments without manual reconfiguration.

**JSON Schema Updates**:

1.  **Stage Actions**: Include `actionType` and `errorCode`.
    ```json
    "stages": [
      {
        "stageCode": "REVIEW_STEP",
        "actions": [
          {
            "label": "Rework",
            "actionType": "ERROR_TRIGGER",  // New Field
            "errorCode": "REWORK_REQUIRED"  // New Field
          }
        ]
      }
    ]
    ```

2.  **Stage Exception Rules** (For Call Activity settings):
    *   The export payload for a Stage must now include a list of exception mappings.
    ```json
    "stages": [
      {
         "stageCode": "CALL_SUB_PROCESS",
         "type": "WORKFLOW",
         "nestedWorkflowCode": "SUB_FLOW_XYZ",
         "exceptionRules": [              // New Section
           {
             "errorCode": "REWORK_REQUIRED",
             "targetStageCode": "INIT_STEP"
           }
         ]
      }
    ]
    ```

**Acceptance Criteria**:
- [ ] Exporting a workflow with "Trigger Error" actions preserves the `actionType` and `errorCode` in the JSON/ENC file.
- [ ] Exporting a workflow with Call Activity Exception Rules preserves the mapping (`errorCode` -> `targetStageCode`).
- [ ] Import validates that `targetStageCode` exists in the imported workflow (or creates a placeholder if order is dependent, though ideally validation happens post-structure creation).
- [ ] Importing restores these configurations exactly as defined.
