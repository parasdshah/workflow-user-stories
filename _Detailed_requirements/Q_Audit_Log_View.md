# Q. Audit Log Screen

## Goal
Provide a user interface to view, search, and filter the system audit trail (`AUDIT_TRAIL` table). This allows administrators and auditors to track changes to workflows, stages, and configuration.

## Functional Requirements

### 1. Audit Log List (Grid View)
-   **Columns**:
    -   **Timestamp**: `changedAt` (Format: `YYYY-MM-DD HH:mm:ss`)
    -   **Entity Type**: `entityName` (e.g., `StageConfig`, `WorkflowMaster`)
    -   **Entity ID**: `entityId`
    -   **Action**: `action` (e.g., `CREATE`, `UPDATE`, `DELETE`)
    -   **User**: `changedBy`
    -   **Changes**: Button/Link to "View Details" (opens modal)
-   **Sorting**: Default sort by `changedAt` DESC (Newest first).
-   **Pagination**: Standard pagination (10/20/50 rows per page).

### 2. Filtering & Search
-   **Entity Type Filter**: Dropdown (All, StageConfig, WorkflowMaster, etc.)
-   **Action Filter**: Dropdown (All, CREATE, UPDATE, DELETE)
-   **Date Range**: Start Date and End Date pickers.
-   **User Search**: Text input for `changedBy`.
-   **Entity ID Search**: Text input for `entityId`.

### 3. Change Details Modal
-   **Title**: Audit Detail
-   **Content**: 
    -   Display the `changes` (JSON/CLOB) field in a readable format.
    -   Preferably a syntax-highlighted JSON view or a Diff view if possible.

## Technical Requirements

### Backend
-   **Endpoint**: `GET /api/audit-logs`
-   **Parameters**:
    -   `page`, `size` (Pagination)
    -   `entityName` (Optional filtering)
    -   `action` (Optional filtering)
    -   `changedBy` (Optional filtering)
    -   `startDate`, `endDate` (Optional date range)
-   **Repository**: Use `AuditTrailRepository` with `Specification` or Query methods for filtering.

### Frontend
-   **Page**: `AuditLog.tsx`
-   **Route**: `/audit`
-   **Components**: 
    -   Mantine `Table` or `Datatable`.
    -   `Modal` for details (using `<Code>` or `<JsonInput>` read-only for JSON display).
