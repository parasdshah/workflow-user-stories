# User Workload Storyboard Requirements

## 1. Overview
This is an enhancement to the Workload Dashboard that visualizes user tasks in a Kanban-style board. When expanding a user's row in the dashboard grid, instead of a simple list, a 3-column board is displayed to show the lifecycle of tasks assigned to that user.

## 2. Functional Requirements

### 2.1. Storyboard Layout
- **Swimlanes / Columns**:
    1.  **New**: Tasks that are currently **Active** (Pending).
    2.  **In-progress**: Tasks that have been **Started** or are ongoing.
    3.  **Closed**: Tasks that are **Completed** (History).

- **Metrics**: Header row of the board shows counts for (New | In-progress | Closed).

### 2.2. Logic
- **New**: `Task.status == 'Active'` AND `variables['status']` is NULL or Empty.
- **In-Progress**: `Task.status == 'Active'` AND `variables['status']` is NOT Empty (e.g., 'WIP', 'Reviewed').
- **Closed**: `HistoricTask.endTime != null` (Completed tasks).

### 2.3. Actions
- **Move to In-Progress**: Ability to transition a task from "New" to "In-Progress" (e.g., via a button on the card).
- **View Case**: Link to the detailed Case Timeline.

## 3. Data Requirements
- **Backend API Update**:
    - Need to fetch *Historic/Completed* tasks for the user.
    - Need to allow updating task variables (to set status='WIP').

## 4. UI/UX
- **Expanded Detail**:
    - Displays a 3-Column Board: [New] [In-Progress] [Closed].
    - Cards in columns show Task Details (Case ID, Stage Name, Due Date).
