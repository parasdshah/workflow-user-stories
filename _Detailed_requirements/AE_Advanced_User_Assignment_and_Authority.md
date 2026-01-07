# Advanced User Assignment & Approval Authority Design

## 1. Core Philosophy: The Independent Service Pattern
To keep the Workflow Service **independent**, it must **not** contain hardcoded business logic about "Who is a Manager" or "Who approves loans > $10k".
Instead, it should act as an **Orchestrator** that delegates decisions to specialized components or generic configurations.

**Pattern**:
1.  **Configuration**: `StageConfig` stores *Rules* (e.g., "Requires Role 'Manager'"), not individuals.
2.  **Resolution**: At runtime (Task Creation), the Workflow Service calls an **Identity Provider Interface** (or specific beans) to resolve these rules into actual users.

---

## 2. Configuration Model (`StageConfig`)
We propose extending `StageConfig` with a structured `assignmentConfig` JSON field.

### 2.1 Supported Assignment Types

#### Type 1: Single Assigned User (Static or Variable)
Assigns to a specific known user or a variable passed from a previous stage.
```json
{
  "type": "SINGLE_USER",
  "value": "${initiator}", // or fixed "john.doe"
  "fallback": "admin"
}
```

#### Type 2: Dynamic Users from Pool (Candidate Groups)
Refers to a role/group. Any member can "claim" the task.
```json
{
  "type": "CANDIDATE_GROUP",
  "value": "LOAN_MANAGERS", 
  "attribute": "REGION_NORTH" // Optional: Filter by attribute
}
```

#### Type 3: Dynamic Users from Round Robin Logic
Assigns to *one* user from a group, effectively load balancing.
```json
{
  "type": "ROUND_ROBIN",
  "group": "UNDERWRITERS",
  "key": "REGION_SOUTH" // Context key for separate RR queues
}
```

#### Type 4: Committee (Role/Group Based)
**Parallel Approvals**: Requires `N` users from a group to approve.
**BPMN**: Multi-Instance User Task.
```json
{
  "type": "COMMITTEE_ROLE",
  "group": "CREDIT_COMMITTEE",
  "minApprovals": "50%", // or fixed number "3"
  "completionCondition": "${nrOfCompletedInstances / nrOfInstances >= 0.5}"
}
```

#### Type 5: Committee (Specific Users)
**Parallel Approvals**: Specific list of users must ALL (or subset) approve.
```json
{
  "type": "COMMITTEE_LIST",
  "users": ["alice", "bob", "charlie"],
  "minApprovals": "100%"
}
```

---

## 3. Approval Authorities (Matrix)
Different users have different limits. Since the workflow is independent, it shouldn't "know" these limits. Use **DMN (Decision Model and Notation)** or an **External Rule Engine**.

### Strategy: Pre-Task Evaluation
Before a task is created (or during creation), the system evaluates **WHO** is eligible based on the transaction Amount/Type.

**Scenario**: Transaction Amount = $50,000.
**Rules**:
- Junior: Limit $10k
- Senior: Limit $100k

**Flow**:
1.  **Workflow**: Enters Stage. execution variable `amount` = 50000.
2.  **Listener (`TaskListener`)**:
    - Connects to `ApprovalMatrixService` (Interface).
    - Sends: `{ role: "APPROVER", context: { amount: 50000 } }`
    - Returns: `['senior_user_1', 'senior_user_2']` or Group `SENIOR_APPROVERS`.
3.  **Assignment**: Sets `candidateUsers` or `assignee` based on response.

### JSON Config for Authority
```json
{
  "authorityRule": "DMN_APPROVAL_MATRIX_V1", // Key of DMN table
  "inputVariables": ["amount", "productType"]
}
```

---

## 4. Technical Implementation Strategies

### A. The "Resolver" Interface
Create a unified interface in `workflow-service` to resolve abstract configs into users.

```java
public interface UserAssignmentResolver {
    // Returns List of User IDs
    List<String> resolveUsers(AssignmentConfig config, DelegateExecution execution);
}
```

### B. Implementation Mapping
- **Round Robin**:
    - Needs a database table `RoundRobinState (group_key, last_assigned_index)`.
    - Resolver fetches group members, checks state, picks next, updates state.
- **Committee**:
    - Needs **BPMN Modification**. The Stage element in BPMN must be a `UserTask` with `MultiInstanceLoopCharacteristics`.
    - **Dynamic Switch**: If you want *some* stages to be Single and *some* Committee using the **same** BPMN diagram element, you must use `Collection`-based Multi-instance where the collection contains 1 user (Single) or N users (Committee).
    - **Recommended**: Always model "Stage" as a generic Multi-Instance task. If "Single User", the collection has size 1. This unifies the logic.

### C. DMN Integration (Flowable DMN)
1.  Define a DMN Table in Flowable.
    - Inputs: `Amount`, `RiskLevel`.
    - Outputs: `RequiredRole`, `MinApprovals`.
2.  Link this DMN to the Workflow (Rule Task before User Task).
3.  The DMN Output (`RequiredRole`) feeds into the Assignment Logic.

---

## 5. Summary of Architecture
1.  **StageConfig**: Add `assignmentConfig` (JSON) and `authorityConfig` (JSON).
2.  **BpmnGenerator**: 
    - Generate ALL flexible stages as **Multi-Instance** User Tasks.
    - Loop Collection: `${stageParticipants}`.
    - Completion Condition: `${stageCompletionCondition}`.
3.  **Runtime (Listener)**:
    - **Start Listener**: Parses Config -> Calls Resolver/DMN -> Populates `${stageParticipants}` (List of strings).
    - **Task Listener**: Handles specific "Round Robin" logic if we don't want Multi-instance (just picking one).

This approach completely decouples the **Workflow Definitions** from the **User Data**.
