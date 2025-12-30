# Z. Action-Based Routing Requirements

## 1. Overview
Currently, "Allowed Actions" (e.g., Approve, Reject) are stored as a simple list of strings. The workflow implicitly moves to the *next* stage regardless of the action taken (unless a Listener handles it, which is hidden logic).
**Action-Based Routing** allows configuring a specific **Target Stage** for each action directly in the UI.

### Use Case
*   **Approve**: Moves to **Next Stage** (e.g., "Manager Approval").
*   **Reject**: Moves to **Previous Stage** (e.g., "Correction") or **End Event**.
*   **On-Hold**: Stays on **Current Stage**.

---

## 2. Configuration UI Changes (`WorkflowEditor.tsx`)

### Stage Modal -> "Actions" Tab
**Fields per Action:**
1.  **Action Label**: Text (e.g., "Approve").
2.  **Button Style**: Dropdown [ Primary | Success | Danger | Warning | Default ].
3.  **Target Type**: Dropdown [ "Next Stage" | "Specific Stage" | "End Workflow" ].
4.  **Target Stage**: Dropdown (Visible if "Specific Stage").
5.  **Post-Action Status**: Text (Optional).
    *   *Function*: Updates the `Case Status` (e.g. from "IN_PROGRESS" to "APPROVED") when this action is taken.

**Example Configuration:**
| Action | Style | Target | Status Update |
| :--- | :--- | :--- | :--- |
| Approve | Primary | Next | - |
| Terminate | Danger | End | `TERMINATED` |
| Rework | Warning | `STG_REV` | `INFO_REQUIRED` |

---

## 3. Stage Entry Conditions (Conditional Stages)

In addition to routing *from* a stage, we need to control entry *into* a stage.

### Stage Modal -> "General" Tab
**New Field: `Entry Condition`**
*   **Type**: Expression / Rule (String).
*   **Logic**:
    *   Before entering this stage, the engine evaluates the expression (e.g., `${amount > 10000}`).
    *   **True**: Enter the stage.
    *   **False**: Skip this stage and proceed to the *next* stage in sequence (or follow default path).

### Use Case
*   **Stage**: "Senior Manager Approval".
*   **Entry Condition**: `${loanAmount >= 500000}`.
*   **Flow**: If `loanAmount` is 100k, the workflow automatically skips "Senior Manager" and goes to the next step.

---

## 4. Backend Data Model Changes

### New Entity: `StageAction` (Table: `stage_actions`)
Instead of a JSON string, we will use a dedicated table for actions to allow better querying and scalability.

**Table Schema:**
*   `id` (UUID/Long)
*   `stage_id` (FK to `stage_config`) or `stage_code` + `workflow_code`
*   `action_label` (String) - e.g. "APPROVE"
*   `button_style` (String) - e.g. "primary"
*   `target_type` (String) - [NEXT, SPECIFIC, END]
*   `target_stage_code` (String) - e.g. "STG_02"
*   `post_action_status` (String) - e.g. "APPROVED"

### Update `StageConfig`
*   **Remove**: `allowedActions` (JSON field).
*   **Add**: One-to-Many relationship to `StageAction`.
*   **Keep**: `entryCondition` (Text).

---

## 5. BPMN Generation Changes (`BpmnGeneratorService.java`)

### Handling Entry Conditions
*   **Standard approach**: Wrap the stage in an **Exclusive Gateway** (Skip Logic).
    *   **Gateway Split**:
        *   Path 1 (Condition Met): Go to User Task.
        *   Path 2 (Default): Bypass User Task -> Go to Next Stage.
    *   **Gateway Join**: Merge paths after User Task.

### Handling Case Status
*   Use a **Task Listener** (`complete` event) on the User Task.
*   When task completes, read the `status` from the `allowedActions` config for the chosen outcome.
*   Update the `WorkflowMaster` (or Case) status entity.
