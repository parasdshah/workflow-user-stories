# design: External User & Role Integration Strategy

## 1. The Core Problem
You want the **Workflow Service** to be a generic "Product" used by any company.
*   **Company A** uses Active Directory + Workday.
*   **Company B** uses Okta + Salesforce.
*   **Company C** uses a custom SQL User Table.

If the Workflow Service hardcodes "Business Segment" or stores a "Role Table", it becomes rigid and requires customized data syncing for every client.

## 2. The Solution: "Provider Adapter Pattern"
**Do not manage user information in the Workflow Service.**
Instead, the Workflow Service should define a **Standard Integration Contract** (API) that it uses to ask questions. The *Client Company* is responsible for providing the answers.

### Architecture
1.  **Workflow Service (The Product)**
    *   Knows **Generic Logic**: "I need to find the Manager of User X".
    *   Does **Not Know**: Who the manager is, or where that data lives.
    *   Mechanism: Calls an external, configurable webhook/API.

2.  **Integration Adapter (The Glue)**
    *   A small, lightweight service (or generic adapters we provide).
    *   Implements the Workflow Service's "Question API".
    *   Connects to the Client's REAL data source (AD, HRIS, CRM).

3.  **Identity Provider / HRIS (The Source of Truth)**
    *   Holds the actual Users, Roles, Segments, Supervisors.

---

## 3. The Integration Contract (API)
The Workflow Service exposes (or calls) a standard interface. Let's call it the **User & Role Resolution Protocol**.

### Endpoint 1: Resolve Participants
**Request** (From Workflow Engine):
```json
POST /api/adapter/resolve-users
{
  "context": {
    "workflowType": "LOAN_ORIGINATION",
    "transactionAmount": 50000,
    "region": "APAC"
  },
  "criteria": {
    "role": "CREDIT_APPROVER",
    "relation": "SUPERVISOR_OF", // Optional
    "sourceUser": "alice.staff"  // Optional
  }
}
```

**Response** (From Adapter):
```json
{
  "users": [ "bob.manager" ],
  "metadata": { "reason": "Bob is the APAC Credit Approver" }
}
```

### Endpoint 2: Get User Attributes (Context Enrichment)
Used to populate email templates or rule decisions without storing data.
**Request**: `GET /api/adapter/users/alice.staff/attributes?fields=email,title,limit`
**Response**: `{ "email": "alice@corp.com", "limit": 10000 }`

---

## 4. How It Works in Practice
**Scenario**: "Assign to Supervisor based on Region"

1.  **Configuration (BA)**: In the Workflow UI, the BA selects:
    *   Assignment Type: `External Resolution`
    *   Role Key: `SUPERVISOR`
    *   Context Variables: `[ Region ]`

2.  **Step 1 (Runtime)**: The Workflow Engine hits the **Task Creation** step.
3.  **Step 2 (The Call)**: The Engine constructs a payload:
    *   `criteria.role = "SUPERVISOR"`
    *   `context.Region = "North"`
    *   Destination: `Config.USER_ADAPTER_URL` (e.g., `http://company-internal-service/wf-adapter`)
4.  **Step 3 (Resolution)**:
    *   The **Connector** receives the request.
    *   It queries **Active Directory** for the Manager of the user in the "North" OU.
    *   It returns `["Manager_Mike"]`.
5.  **Step 4 (Assignment)**: The Workflow Service assigns the task to `Manager_Mike`.

## 5. Benefits of this Design
1.  **Zero PII Storage**: The Workflow Service never stores names, emails, or hierarchy. It only stores IDs temporarily for active tasks.
2.  **Product Portability**: You can deploy this binary at ANY company. You just change the `USER_ADAPTER_URL`.
3.  **Real-Time Accuracy**: If a manager changes in HRIS at 9:00 AM, the workflow sees it at 9:01 AM. No nightly sync jobs required.
4.  **Complex Logic Hiding**: The "Adapter" can handle messy logic (e.g., "If User is CEO, route to Board"). The Workflow Service stays clean.

## 6. Where does "Role" live?
*   **Global Roles** (Admin, User): In the Identity Provider (OIDC/JWT claims) for login access.
*   **Workflow Roles** (Approver, Reviewer): Dynamically resolved via the Adapter at runtime.

## 7. Handling Distributed Authorities & Limits

A common challenge is that **Role** (e.g., "Senior Manager") might be in HRIS, but the **Approval Limit** (e.g., "$50,000") is in a Core Banking System, a standalone database, or perhaps not digitized at all.

### Strategy: The Aggregation Pattern
The **Integration Adapter** acts as an aggregator. When the Workflow Service asks for user attributes or resolution, the Adapter:
1.  Calls **HRIS** to get the Role.
2.  Calls **Limit DB** (or Core Banking) using the User ID to get the specific financial authority.
3.  Combine (or Filter) the results before returning them to the Workflow.

### Handling "No Limit Data" (Manual/Fallback)
For clients (like some banks) that do **not** store approval limits digitally:

1.  **Option A: Adapter-Side Local Table**
    *   The Adapter can maintain a simple `Limits.json` or local SQL table that the IT team manages manually.
    *   *Values*: `John_Doe: 50000`, `Jane_Smith: 100000`.

2.  **Option B: "Infinite" Fallback**
    *   If no limit is found, the Adapter returns a flag `manual_verification_required: true`.
    *   The Workflow can then route the task with a warning: "Please verify authority manually."

3.  **Option C: Role-Based Defaulting**
    *   If specific user limits are missing, default to the Role's standard limit.
    *   *Logic*: "User is Senior Manager, no specific limit found -> Apply Standard Senior Manager Limit ($100k)."
