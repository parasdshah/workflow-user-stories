# User Stories: Multi-Instance Implementation

## 1. EPIC: Parallel Partial Batch Processing
**Description**: As a Process Designer, I want to configure stages to run in parallel for each item in a list, so that I can handle batches of documents efficiently without creating complex loops.

---

## 2. User Stories

### US-1: Configure Multi-Instance Call Activity
**As a** Workflow Designer
**I want** to enable "Multi-Instance" execution on a Call Activity (Nested Workflow) stage
**So that** the system spawns a child workflow for each item in a collection.

**Acceptance Criteria:**
- [x] In the Workflow Editor (Configuration Tab), a "Multi-Instance" toggle is visible for Nested Workflow stages.
- [x] When enabled, "Collection Variable" and "Element Variable" inputs are shown.
- [x] Saving the stage persists `isMultiInstance`, `miCollectionVariable`, and `miElementVariable` to the backend.

### US-2: Backend Support for Parallel Looping
**As a** System Architect
**I want** the BpmnGenerator to recognize Multi-Instance configurations
**So that** the generated BPMN XML includes standard Flowable Multi-Instance Loop Characteristics.

**Acceptance Criteria:**
- [x] `StageConfig` entity stores MI fields.
- [x] BPMN Generator injects `<multiInstanceLoopCharacteristics>` with `isSequential="false"`.
- [x] The `flowable:collection` matches the configured `miCollectionVariable`.
- [x] The `flowable:elementVariable` matches the configured `miElementVariable`.

### US-3: Runtime Execution of Partial Batches
**As a** Maker (User)
**I want** to select a subset of documents and trigger a review
**So that** multiple review tasks are created immediately (one per document) and I don't have to wait for them sequentially.

**Acceptance Criteria:**
- [x] If a list variable (e.g., `["doc1", "doc2"]`) is passed to the MI stage, exactly 2 child process instances are started.
- [x] Checkers see 2 distinct tasks in their inbox.
- [x] The Parent Process waits at the Call Activity until ALL child instances are completed.
- [x] Variables passed to the child process include the specific `elementVariable` data (e.g., `doc1` for Instance 1).

---

## 3. Technical Notes
- **Implementation**: Uses Flowable's native `<callActivity>` with `<multiInstanceLoopCharacteristics>`.
- **Concurrency**: Configured as Parallel (not Sequential) to allow simultaneous reviews.
- **Data Flow**: The element variable is copied into the child process scope automatically by Flowable.
