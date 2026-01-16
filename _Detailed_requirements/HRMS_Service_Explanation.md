# HRMS Service Explanation

The `hrms-service` is responsible for managing organizational structure (Regions, Products, Roles) and handling the **Matrix Assignment** logic to dynamic resolve approvers for workflows.

## Core Components

1.  **Management API** (`HrmsManagementController`): CRUD operations for setting up the organization matrix.
2.  **Adapter API** (`AdapterController`): External facing API used by the `workflow-service` to resolve users and fetch attributes.
3.  **Resolution Engine** (`MatrixResolutionService`): The core logic that finds the "Best Match" employee based on scope specificity.

## Sequence Diagram: Matrix Resolution Flow

This diagram illustrates how the `workflow-service` interacts with `hrms-service` to find an assignee for a task.

```mermaid
sequenceDiagram
    participant Workflow as Workflow Service
    participant Adapter as AdapterController
    participant Engine as MatrixResolutionService
    participant RegionRepo as RefRegionRepository
    participant ProductRepo as RefProductRepository
    participant MatrixRepo as EmployeeMatrixAssignmentRepository

    Note over Workflow, Adapter: Runtime: Need to find approver for a Task

    Workflow->>Adapter: POST /api/adapter/resolve-users
    Note left of Adapter: Body: { region: "Mumbai",<br/>product: "Home Loan",<br/>role: "CREDIT_MANAGER",<br/>amount: 500000 }

    Adapter->>Engine: resolveUsers(request)
    
    rect rgb(240, 248, 255)
        note right of Engine: 1. Resolve Scope Hierarchy
        Engine->>RegionRepo: findByRegionName("Mumbai")
        RegionRepo-->>Engine: Region(id=10, path="/1/5/10/")
        Engine->>Engine: parseRegionPath("/1/5/10/") -> [1, 5, 10]
        
        Engine->>ProductRepo: findByProductName("Home Loan")
        ProductRepo-->>Engine: Product(id=20, segment="Retail")
    end

    rect rgb(255, 240, 245)
        note right of Engine: 2. Fetch Candidates
        Engine->>MatrixRepo: findByRoleAndRegionIds("CREDIT_MANAGER", [1, 5, 10])
        MatrixRepo-->>Engine: List<Assignment> (Candidates in Mumbai, West, India...)
    end

    rect rgb(240, 255, 240)
        note right of Engine: 3. Filter & Score (In-Memory)
        loop For Each Candidate
            Engine->>Engine: Check Product Match (Exact > Segment > Global)
            Engine->>Engine: Check Limit (Limit >= 500k)
            Engine->>Engine: Calculate Specificity Score
        end
        Engine->>Engine: Select Max(Score) -> "Best Match"
    end

    Engine-->>Adapter: ResolutionResponse(userIds=[EMP001], reason="Matched...")
    Adapter-->>Workflow: 200 OK { userIds: ["EMP001"] }
```

## Sequence Diagram: User Attributes & Limits

This diagram shows how user attributes (including aggregated approval limits) are fetched.

```mermaid
sequenceDiagram
    participant Client as Frontend / Workflow
    participant Adapter as AdapterController
    participant EmpRepo as EmployeeMasterRepository
    participant MatrixRepo as EmployeeMatrixAssignmentRepository

    Client->>Adapter: GET /api/adapter/users/{userId}/attributes
    
    Adapter->>EmpRepo: findById(userId)
    EmpRepo-->>Adapter: Employee Details

    Adapter->>MatrixRepo: findByEmployeeId(userId)
    MatrixRepo-->>Adapter: List<Assignments>

    Note right of Adapter: Logic: Max(ApprovalLimit) from all assignments
    Adapter->>Adapter: Calculate Max Limit

    Adapter-->>Client: UserAttributes { role: "Variable", limit: 1000000, ... }
```
