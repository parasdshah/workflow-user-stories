# User Workload Dashboard Requirements

## 1. Overview
The goal is to provide a dashboard that allows supervisors and administrators to view the current workload of all users in the system. The dashboard will display a list of users who have pending tasks, along with the count of tasks assigned to them. Users can expand each row to view the detailed list of cases pending with that specific user.

## 2. Functional Requirements

### 2.1. Workload Grid
- **Search**: A search bar at the top to filter users by Name or ID. Filtering happens real-time (client-side).
- **Pagination**: The grid displays 20 users per page. Standard pagination controls (Next, Prev, Page Numbers) are located at the bottom.
- **Display**: A grid/table view listing all users with at least one active task.
- **Columns**:
    - **User Name**: The full name of the assignee (e.g., "Grace 1").
    - **Pending Cases Count**: The total number of active tasks assigned to this user.
- **Sorting**: Default sort by "Pending Cases Count" (Descending).

### 2.2. Detailed Case View (Expandable Row)
- **Interaction**: Clicking on a user row expands it to show a nested table or list.
- **Content**: The expanded view lists all individual tasks for that user.
- **Columns**:
    - **Case ID**: Link to the Case View.
    - **Stage Name**: Name of the current task (e.g., "Approval Stage").
    - **Created Time**: When the task was assigned.
    - **Due Date**: (Optional) Deadline for the task.

### 2.3. Data Freshness
- The dashboard should reflect real-time (or near real-time) data from the workflow engine.

## 3. Technical Requirements

### 3.1. Backend API
- **Endpoint**: `GET /api/runtime/stats/workload`
- **Logic**:
    1.  Fetch all active tasks from Flowable (`Active Task Query`).
    2.  Group tasks by `assignee` ID.
    3.  Resolve assignee IDs to Full Names using `UserAdapterClient`.
    4.  Return an aggregated list.
- **Response Structure**:
    ```json
    [
      {
        "userId": "EMP001",
        "userName": "Alice Smith",
        "pendingCount": 5,
        "tasks": [
          {
            "taskId": "...",
            "caseId": "...",
            "stageName": "Review",
            "createdTime": "..."
          }
        ]
      }
    ]
    ```

### 3.2. Frontend
- **Route**: `/workload`
- **Components**:
    - **Mantine Table**: Use `Table` with expandable rows feature or `Accordion`.
    - **Navigation**: Clicking the "Case ID" in the expanded view redirects to `/cases/:id`.

## 4. Dependencies
- **HRMS Service**: Required for resolving User IDs to Names.
- **Workflow Service**: Source of truth for active tasks.

## 5. Navigation & Entry Point
- **Global Header**: Validated under the **"Tasks"** dropdown menu.
- **Menu Item**: select **"Workload Dashboard"**.
- **URL**: Clicking the link navigates to `/workload`.
