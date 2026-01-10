# HRMS Service Sequence Diagrams

## 1. Matrix User Resolution Flow (Runtime)
This flow illustrates how the Workflow Engine (via Adapter) queries the HRMS Service to find the correct user based on Matrix rules (Region, Product, Limit).

```mermaid
sequenceDiagram
    participant WF as Workflow Service
    participant API as HRMS API (MatrixController)
    participant SVC as MatrixResolutionService
    participant REPO as MatrixAssignmentRepo
    participant REG as RegionRepo
    participant DB as HRMS Database

    Note over WF, DB: Workflow Needs an Approver
    WF->>API: POST /api/matrix/resolve
    Note right of WF: Payload: { role: "APPROVER",<br/>region: "Mumbai",<br/>product: "Home Loan",<br/>amount: 50000 }

    API->>SVC: findUsers(criteria)
    
    Note right of SVC: Step 1: Expand Region Hierarchy
    SVC->>REG: findByRegionName("Mumbai")
    REG-->>SVC: Region Entity (Path: /1/5/20/50/)
    SVC->>SVC: Identify Parents: [Mumbai, India, APAC, Global]

    Note right of SVC: Step 2: Query Matrix Table
    SVC->>REPO: findByRoleAndScope(Role, RegionList, Product)
    REPO->>DB: SELECT * FROM Matrix WHERE...
    DB-->>REPO: List<Assignment>
    REPO-->>SVC: [UserA(Mumbai), UserB(India)]

    Note right of SVC: Step 3: Filter by Limit
    SVC->>SVC: Filter(User.Limit >= 50000)
    
    Note right of SVC: Step 4: Pick Most Specific
    SVC->>SVC: Sort by Region Granularity
    
    SVC-->>API: Result: UserA
    API-->>WF: { userId: "UserA", email: "..." }
```

## 2. Reference Data Sync (Onboarding)
How data enters the system (Seeding or Sync API).

```mermaid
sequenceDiagram
    actor Admin
    participant API as HRMS API (SeedController)
    participant SD as HrmsDataSeeder
    participant DB as HRMS Database

    Admin->>API: POST /api/seed/reset
    API->>SD: run()
    SD->>DB: Check Existing Count
    alt Empty DB
        SD->>DB: Insert Regions (Tree)
        SD->>DB: Insert Business Segments
        SD->>DB: Insert Roles & Products
        SD->>DB: Generate 50 Employees
        SD->>DB: Insert Matrix Assignments
    else Data Exists
        SD-->>API: Skip
    end
    API-->>Admin: "Seeding Completed"
```
