# X. Rule-Based Routing Requirements

## 1. Overview
Current workflows in the system are primarily **sequential** (Stage 1 -> Stage 2 -> Stage 3) or **parallel** (Stage 2a + 2b). 
**Rule-Based Routing** introduces **Conditional Branching**, where the workflow path diverges based on the output of a Business Rule (DMN) or a Process Variable.

### Core Concept
1.  **Rule Execution**: A stage (User Task or DMN) produces a result (e.g., `RiskLevel = "HIGH"`).
2.  **Decision Gateway**: An **Exclusive Gateway** checks this result.
3.  **Routing**: 
    *   If `RiskLevel == "HIGH"`, proceed to **Enhanced Due Diligence** (Stage X).
    *   If `RiskLevel == "LOW"`, proceed to **Auto Approval** (Stage Y).

---

## 2. Configuration UI Changes (`WorkflowEditor.tsx`)

To support non-linear flows within the existing "Sequence Order" list paradigm, we need to introduce **"Jump/Branch Logic"** configuration in the Stage Modal.

### A. Stage Configuration Modal
We will add a new section (or Tab) called **"Routing & Branching"**.

**New Fields:**
1.  **Enable Branching**: Checkbox. If unchecked, the flow continues to the next sequence number (Default behavior).
2.  **Routing Conditions** (Dynamic List):
    *   **Condition Expression**: A JUEL expression or simple equality check.
        *   *Simple Mode*: Variable Name (`riskScore`) | Operator (`>`) | Value (`100`).
        *   *Advanced Mode*: Full Expression (`${riskScore > 100 && customerType == 'VIP'}`).
    *   **Target Stage Code**: Dropdown of available stages in the workflow.
3.  **Default Path**: logic for when no condition is met (e.g., "Continue to next" or "Go to Stage Z").

### B. Visual Indicator
*   In the **Stage List** (Table view), stages with branching logic should have a specific icon (e.g., `IconArrowsSplit`).
*   In the **Diagram View** (`BpmnVisualizer`), arrows should visually point to the specific target stages instead of just the next one.

---

## 3. Backend Data Model Changes

### A. `StageConfig.java`
We need to store the branching logic.

```java
@Column(columnDefinition = "TEXT")
private String routingRules; // JSON Structure
```

**JSON Structure Example:**
```json
[
  {
    "condition": "${riskLevel == 'HIGH'}",
    "targetStageCode": "STG_EDD"
  },
  {
    "condition": "${riskLevel == 'LOW'}",
    "targetStageCode": "STG_AUTO_APPROVE"
  }
]
```

---

## 4. BPMN Generation Changes (`BpmnGeneratorService.java`)

The linear generation logic (`current -> next`) must be upgraded to a **Graph-based Generation**.

**Algorithm Update:**
1.  **Map Stages**: Create a Map of `StageCode -> FlowElement` for all stages first.
2.  **Connect Flows**: Iterate through stages.
    *   **If `isRuleStage` OR Standard Stage has Routing Rules**:
        1.  Create the Task (ServiceTask/UserTask).
        2.  Create an **Exclusive Gateway** immediately after the task.
        3.  Connect Task -> Gateway.
        4.  **For each Routing Rule**:
            *   Create a Sequence Flow from **Gateway** -> **Target Stage Element**.
            *   Set the `conditionExpression` on the Sequence Flow.
        5.  **Default Flow**: Connect Gateway -> Next Sequence Stage (or explicit default target).
    *   **Else (Standard Linear)**:
        1.  Connect Task -> Next Sequence Stage.

---

## 5. Technical Implementation Steps

1.  **Backend**: Add `routingRules` field to `StageConfig` and DTOs.
2.  **Frontend**: Update `StageConfig` interface and create the **Routing Logic UI** in `WorkflowEditor`.
3.  **Generator**: Rewrite `linkStages` method in `BpmnGeneratorService` to support arbitrary target connections via Gateways.
4.  **Visualization**: Update `BpmnVisualizer` to render edges based on `routingRules` instead of just `sequenceOrder`.
