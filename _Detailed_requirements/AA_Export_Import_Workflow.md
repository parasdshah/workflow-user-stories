# AA. Workflow Export & Import with Encryption

## 1. Overview
Enable users to export a full workflow configuration (including stages, actions, rules, and layout) into a portable, encrypted file. This file can then be imported into another environment (e.g., UAT to Prod) or used as a backup.

## 2. User Stories

### Story 1: Export Workflow
**As a** Workflow Admin
**I want to** export a workflow definition from the Workflow List
**So that** I can share it or back it up.
- **Acceptance Criteria**:
    - [ ] A dedicated "Export" button exists on the Workflow List (or per row action).
    - [ ] Clicking export generates a JSON file containing:
        - Workflow Master data (Name, Code, SLA)
        - All Stage Configurations (Sequence, Type, Hooks, Parallel Grouping)
        - All Rules (Routing, Entry Conditions)
        - All Actions (Labels, Styles, Targets)
    - [ ] **Encryption**: The file content is encrypted using AES-256 (or similar) with a system-defined key to prevent tampering.
    - [ ] The file is downloaded to the user's browser (e.g., `workflow_CODE_timestamp.enc`).

### Story 2: Import Workflow
**As a** Workflow Admin
**I want to** import an encrypted workflow configuration file
**So that** I can restore it or migrate a workflow to this environment.
- **Acceptance Criteria**:
    - [ ] A dedicated "Import" button exists on the Workflow List.
    - [ ] Clicking "Import" opens a file upload modal.
    - [ ] User selects a `.enc` file.
    - [ ] System decrypts the file.
    - [ ] **Validation**: System validates the structure.
    - [ ] **Conflict Handling**: If a workflow with the same code exists:
        - Ask user to "Overwrite" or "Create Copy" (or just fail/warn). *MVP: Overwrite or Fail.*
    - [ ] Upon success, the workflow and all its related entities (Stages, Actions) are saved to the database.

## 3. Technical Requirements

### 3.1 Data Structure (JSON before encryption)
```json
{
  "workflow": {
    "workflowCode": "LOAN_PROCESS",
    "workflowName": "Corporate Loan",
    "slaDurationDays": 5
  },
  "stages": [
    {
      "stageCode": "INIT",
      "stageName": "Initiation",
      "sequenceOrder": 1,
      "actions": [...],
      "routingRules": [...]
    },
    ...
  ]
}
```

### 3.2 Security (Encryption)
- **Algorithm**: AES-256 is recommended.
- **Key Management**: Use a secret key configured in `application.properties` (e.g., `workflow.export.secret`).
- The exported file should be binary or Base64 encoded ciphertext.

### 3.3 Backend API
- `GET /api/workflow/export/{workflowCode}` -> Returns `byte[]` (File download).
- `POST /api/workflow/import` -> Accepts `MultipartFile`.

### 3.4 UI Placement
- **location**: `WorkflowList.tsx` (The main landing page for workflows).
- **Export Icon**:
    - Add an "Export" `IconDownload` button to the **Actions column** of the Workflow Grid for each row.
- **Import Icon**:
    - Add an "Import Workflow" `IconUpload` button next to the "Create Workflow" button in the **Top Toolbar**.

## 4. UI Design

### Workflow List Toolbar
`[ + Create Workflow ] [ ^ Import Workflow ]`

### Workflow List Grid Actions
`| Name | Code | ... | Actions |`
`| ...  | ...  | ... | [Edit] [Delete] [Export] |`

