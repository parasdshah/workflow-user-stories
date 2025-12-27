# V. Rule Stage Integration (Decision Tables in Workflow)

## 1. Overview
This document defines how **Decision Tables (DMN)**, created via CSV upload (see `U_DMN_Integration.md`), are executed within a Workflow.
We are introducing a new Stage Type: **Business Rule Stage**.

## 2. Functional Requirements

### 2.1 Stage Configuration Update
The `StageConfig` entity must support defining a stage as a "Rule Execution" step.

*   **New Field**: `stageType` (Enum/String).
    *   Values: `UserTask` (Default), `NestedWorkflow`, `BusinessRule`.
*   **New Field**: `ruleKey` (String).
    *   Stores the unique Key of the DMN table to execute (e.g., `RISK_CHECK_RULE`).
    *   Only relevant if `stageType == BusinessRule`.

### 2.2 Workflow Editor UI
The "Add/Edit Stage" Modal will be refactored to support distinct modes:

1.  **Step Type Selector**: Radio buttons or Tiles.
    *   **[User Task]**: Standard form-based task.
    *   **[Nested Workflow]**: Calls another workflow.
    *   **[Business Rule]**: Executes a decision table.

2.  **Conditional Inputs**:
    *   If **Business Rule** is selected:
        *   Show "Rule Selection" Dropdown (lists available uploaded DMNs).
        *   Hide "Screen Mapping", "Hooks", "SLA" (unless we want SLA on rules, which is rare).

### 2.3 BPMN Generation Logic
The `BpmnGeneratorService` must recognize the `BusinessRule` type and generate the corresponding Flowable BPMN element.

**Mapping:**
*   **Element**: `<businessRuleTask>` (Flowable DMN Task).
*   **Attributes**:
    *   `id`: `stageCode`
    *   `type`: `dmn`
    *   `field`: `decisionTableReferenceKey` = `ruleKey`
*   **Flow**:
    *   It sits in the sequence just like a User Task.
    *   It effectively takes **Process Variables** as Inputs.
    *   It writes **Result Variables** back to the Process.

### 2.4 Runtime Behavior
1.  **Input Mapping**: The DMN Engine automatically reads variables from the Process Instance matching the DMN Input Column names (e.g., `loanAmount`).
2.  **Execution**: The DMN rules are evaluated.
3.  **Output Mapping**: The DMN Output Column values (e.g., `riskLevel`) are automatically saved as Process Variables.
4.  **Audit**: The execution is logged in Flowable's History tables (`ACT_DMN_HI_DECISION_EXECUTION`).

## 3. Example Scenario
**Stage 1 (User Task)**:
*   User enters `Amount: 5000`, `Score: 650`.

**Stage 2 (Business Rule)**:
*   Config: Key=`RISK_RULE`.
*   Executes: Checks `Score < 700`.
*   Result: Sets `riskLevel = "HIGH"`.

**Stage 3 (User Task)**:
*   User sees "Risk Level: HIGH" on their screen (via mapped variable).

## 4. Acceptance Criteria
*   [ ] I can configure a stage to be a "Business Rule".
*   [ ] I can select an uploaded Rule Key.
*   [ ] The generated BPMN XML contains a valid `<businessRuleTask>`.
*   [ ] The workflow executes the rule without error.
*   [ ] Variables set by the rule are available in subsequent stages.
