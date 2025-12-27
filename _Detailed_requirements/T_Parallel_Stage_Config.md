# Parallel Stage Configuration Design

To implement **Option 2: Parallel Groups**, we will explicitly link stages that should run simultaneously.

## 1. Database & Entity Change (`StageConfig`)
We will add one optional field:
- **Field Name**: `parallelGrouping` (String)
- **Purpose**: Identifies a set of stages that run in parallel.
- **Rules**:
  1. All stages with the same `parallelGrouping` **MUST** have the same `sequenceOrder`.
  2. If a stage has a `parallelGrouping`, it will be wrapped in a Parallel Gateway (Split/Join).

### Updated JSON Payload
```json
[
  {
    "stageName": "Initial Entry",
    "stageCode": "ENTRY",
    "sequenceOrder": 1,
    "parallelGrouping": null
  },
  {
    "stageName": "Credit Check (Equifax)",
    "stageCode": "CREDIT_CHECK_1",
    "sequenceOrder": 2,
    "parallelGrouping": "PARALLEL_CHECKS"  <-- Group Id
  },
  {
    "stageName": "Background Check (Internal)",
    "stageCode": "BG_CHECK",
    "sequenceOrder": 2,
    "parallelGrouping": "PARALLEL_CHECKS"  <-- Same Group Id
  },
  {
    "stageName": "Final Approval",
    "stageCode": "APPROVAL",
    "sequenceOrder": 3,
    "parallelGrouping": null
  }
]
```

---

## 2. Form Configuration (`Add/Edit Stage` Modal)

We will modify the **Add Stage Modal** in `WorkflowEditor.tsx` to include a new "Parallel Execution" section.

### UI Changes
Below the "Sequence Order" field, we add:

1.  **Checkbox**: `[ ] Run in parallel with other stages?`
2.  **Input Field** (Visible only if checked):
    -   **Label**: `Parallel Group ID`
    -   **Placeholder**: `e.g. GROUP_A`
    -   **Description**: *"Enter a unique ID. Assign this same ID and Sequence Order to all stages you want to run simultaneously."*

### Form Validation Logic
-   If "Run in parallel" is checked, `Parallel Group ID` is **Required**.
-   The UI will hint that "Sequence Order" should match the other stages in this group.

---

## 3. Visualizer Representation (`BpmnVisualizer`)
The BPMN generator will automatically detect these groups:
-   **Before** the stages: It inserts a **Parallel Gateway (Split `+`)**.
-   **The Stages**: Displayed vertically stacked (or handled by Dagre layout).
-   **After** the stages: It inserts a **Parallel Gateway (Join `+`)**.
-   **Flow**: `Previous Stage` -> `Split` -> `Stage 1` & `Stage 2` -> `Join` -> `Next Stage`.

## 4. Work to be Done
1.  **Backend**: Add `parallelGrouping` to `StageConfig.java`.
2.  **Frontend**: Update `WorkflowEditor.tsx` to add the Checkbox and Input field.
3.  **Generator**: Update `BpmnGeneratorService.java` to handle grouping and generate `<parallelGateway>`.
