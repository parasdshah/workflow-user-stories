# Simplified User Assignment UI for Business Analysts

## 1. The Core Concept: "No-Code" Rule Builder
The goal is to hide technical complexity behind a unified **"Assignment Wizard"**.
Instead of separate "Candidates" and "Authority" sections, we define a single flow: **Mechanism** (How?) -> **Participants** (Who?).

## 2. Re-imagined UI Flow

### Step 1: Choose Assignment Mechanism (The "How")
*Select how the workflow interacts with the users.*
*   **Standard Group Action**: A group of users receives the task; anyone can "claim" (pick up) the work.
*   **Round Robin**: The system automatically rotates assignment among members of a group to balance load.
*   **Committee (Consensus)**: The task is sent to all members; a specific number must approve.
*   **Single User**: Assigned to one specific individual (rare, usually for "Return to Initiator").

### Step 2: Define Participants (The "Who")
*Select how the system determines which users belong to the mechanism chosen above.*

**Option A: Fixed Role/Group (Simple)**
*Use when the group of users is always the same.*
*   **UI**: Single Dropdown Selector.
*   **Example**: Select `[ Credit Managers ]`.
*   *Result*: The "Credit Managers" group is used for the Round Robin or Committee.

**Option B: Rule-Based / Authority Matrix (Advanced)**
*Use when the group changes based on data (Limits, Location, Product).*
*   **UI**: "If-Then" Rule Builder Table.
*   **Example**:
    *   IF `Amount < 10k` THEN Use Group `[ Junior Analysts ]`
    *   IF `Amount >= 10k` THEN Use Group `[ Senior Analysts ]`
*   *Result*: The system first evaluates the rule to find the correct Group, THEN applies the Mechanism (e.g., Round Robin) to that group.

---

## 3. Comparison of Logic

**Old Confusing Way:**
1.  Pick Candidates (e.g., Managers)
2.  Pick Authority (e.g., Senior Managers if > 10k)
*   *Confusion*: Which one wins? Do they merge?

**New Unified Way:**
1.  **Mechanism**: Round Robin
2.  **Participants**:
    *   [X] **Based on Rules** (Toggle On)
        *   Rule 1: If > 10k -> `Senior Managers`
        *   Rule 2: Else -> `Junior Managers`

## 4. Implementation Strategy (Frontend)

The UI state should reflect this "Source of Truth" choice:

```typescript
interface StageAssignmentConfig {
  mechanism: 'CLAIM' | 'ROUND_ROBIN' | 'COMMITTEE';
  
  // "Who" determination strategy
  participantSource: 'FIXED' | 'RULE_BASED';

  // If FIXED
  fixedGroup?: string; // e.g., "MANAGERS"

  // If RULE_BASED
  rules?: {
     condition: string; // "amount > 10000"
     targetGroup: string; // "SENIOR_MANAGERS"
  }[];

  // Committee specifics
  committeeConfig?: {
    minApprovals: number; // e.g. 3
  };
}
```

## 5. Benefits
*   **Clarity**: BAs understand that "Authority" is just a way of filtering "Who" gets the task.
*   **Simplicity**: If they don't have authority limits, they stick to "Fixed Role" and never see the complex rule table.
*   **Flexibility**: The "Mechanism" (e.g., Round Robin) can work with ANY source (Fixed or Rule-Based). You can have a "Round Robin of Senior Managers".
