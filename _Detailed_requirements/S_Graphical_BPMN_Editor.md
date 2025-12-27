# S. Graphical BPMN Editor (React Flow)

## Goal
Implement a graphical interface to visualize and edit BPMN process definitions directly within the application, replacing or augmenting the current form-based/JSON approach. Use **React Flow** as the underlying library for rendering and interaction.

## Functional Requirements

### 1. Visualization (ReadOnly Mode)
-   **Render Flow**: Display the sequence of stages/tasks as nodes connected by edges (sequence flows).
-   **Auto-Layout**: Automatically arrange nodes (e.g., using Dagre or Elkjs) to ensure a readable, left-to-right or top-to-bottom flow.
-   **Node Types**:
    -   **Start Event**: Circle icon.
    -   **User Task**: Rounded rectangle, displaying the Stage Name/Code.
    -   **Gateways**: Diamond shape for decision points (Exclusive/Parallel).
    -   **End Event**: Bold circle icon.
-   **Edges**: Arrows connecting nodes, labeled with condition expressions if applicable.

### 2. Editing Capabilities (Editor Mode)
-   **Drag-and-Drop Palette**: A sidebar containing standard BPMN elements.
-   **Connecting Nodes**: Allow users to draw connections (edges) with validation.
-   **Detailed Component Configuration**:
    -   **User Task**:
        -   **Name/Code**: Identifier for the stage.
        -   **Assignment**: Assignee (${assignee}) or Candidate Groups (comma-separated).
        -   **Form/Screen**: Link to a `ScreenDefinition` (Screen Code).
        -   **Listeners**: Task Listeners (Create, Complete) and Execution Listeners (Start, End).
    -   **Process Task (Service Task)**:
        -   **Implementation**: Java Class (Delegate) or Delegate Expression.
        -   **Class Name**: FQN of the Java class (e.g., `com.company.MyDelegate`).
        -   **Expression**: Spring bean expression (e.g., `${myService.doSomething()}`).
    -   **Decision Gateway (Exclusive/Inclusive)**:
        -   **Logic**: Define conditions on outgoing edges (e.g., `${amount > 1000}`).
        -   **Default Flow**: Select one edge as the default path if no conditions match.
    -   **Call Activity (Nested Workflow)**:
        -   **Called Element**: Select from existing `WorkflowMaster` codes (Dropdown).
        -   **IO Parameters**: Map input variables to pass to the child process and output variables to retrieve.
        -   **Fallback**: Define behavior if the generic child process fails (optional).
-   **Edge Configuration**: Clicking an edge allows editing the condition expression (e.g., `${status == 'APPROVED'}`).

### 3. Synchronization
-   **Two-Way Sync**: Changes in the graphical editor must update the underlying JSON/BPMN model configuration, and vice versa.
-   **Validation**: Prevent invalid connections (e.g., disconnected nodes, cycles if restricted).

## Technical Constraints
-   **Library**: `reactflow` (or `@xyflow/react`).
-   **Layout Engine**: `dagre` (recommended for simple deterministic layout) or `elkjs`.
-   **Persistence**: The state (node positions) can be saved to `screen_definition` or `workflow_master` layout fields to preserve the user's manual arrangement, or re-calculated on load.
