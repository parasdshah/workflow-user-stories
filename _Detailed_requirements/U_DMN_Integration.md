# U. CSV-to-DMN Integration (Rule Engine)

## Overview
Instead of requiring users to craft complex DMN XML files, we will allow them to upload simple **CSV files**. The system will parse these files, convert them into Flowable DMN 1.1 XML structure, and deploy them to the engine.

## 1. CSV Format Specification
To enable automatic parsing, the CSV must follow a strict header convention:

### Header Format
*   **inputs**: Prefix with `IN:`. The value is the *variable name* (e.g., `IN:loanAmount`).
*   **outputs**: Prefix with `OUT:`. The value is the *variable name* (e.g., `OUT:riskLevel`).
*   **Data Types**: (Optional) Can be inferred or suffixed (e.g., `IN:age:number`). For now, we assume standard DMN string/number inference.

### Example File: `risk-rules.csv`
```csv
IN:amount,IN:creditScore,OUT:riskLevel,OUT:autoApprove
< 1000,>= 700,LOW,true
>= 1000,>= 700,MEDIUM,false
ANY,< 700,HIGH,false
```
*(Note: "ANY" or empty cell implies no condition for that column)*

---

## 2. User Stories

### Story U-1: Rule Management UI
**As a** Workflow Configurator,
**I want to** access a "Rule Management" screen,
**So that** I can view and manage decision tables.

**Acceptance Criteria:**
*   New Menu Item: "Rules / DMN".
*   List View: Shows all uploaded Rule sets (ID, Name, Version, Last Updated).
*   Action "Create New Rule": Opens a modal to enter Name and Key.

### Story U-2: Upload and Convert CSV
**As a** Workflow Configurator,
**I want to** upload a CSV file for a Rule definition,
**So that** it is automatically converted to a DMN table.

**Acceptance Criteria:**
*   Upload Form: Select CSV file.
*   **Backend Validation**:
    *   Verify Header row exists.
    *   Verify at least one `IN:` and one `OUT:` column.
*   **Conversion Logic**:
    *   Parse CSV.
    *   Generate XML using `flowable-dmn-xml-converter` or string builder.
    *   **Deploy**: Deploy the generated DMN to Flowable (`dmnRepositoryService.createDeployment()`).
*   **Persistence**: Store the raw CSV content (blob) and the generated Deployment ID in a `RuleDefinition` entity.

### Story U-3: Versioning Rules
**As a** user,
**I want** simply uploading a new CSV with the same "Rule Key" to create a new version,
**So that** existing running instances might continue (or update) and I can track history.

**Acceptance Criteria:**
*   If I upload for Key `RISK_RULE` again, it increments the version (Flowable handles this natively with Deployments).
*   UI shows "Version 2", "Version 1", etc.
*   Ability to download the original CSV for any version.

### Story U-4: Integrating Rule in Workflow
**As a** Workflow Configurator,
**I want to** select a "Rule" when configuring a Workflow Stage,
**So that** the workflow executes logic at that step.

**Acceptance Criteria:**
*   In `StageConfig` > "Add Stage":
    *   Select Type: "Business Rule".
    *   Dropdown: Shows available Rules (from Story U-1).
*   **Runtime**: When workflow hits this stage, it executes the DMN and outputs variables to the case.

---

## 3. Technical Implementation Tasks

### Backend
1.  **Dependencies**: Add `flowable-spring-boot-starter-dmn`.
2.  **Entity**: `RuleMaster` (id, name, key, description). `RuleVersion` (id, rule_id, csv_content_blob, version, deployment_id).
3.  **Service**: `DmnConversionService`
    *   Input: `MultipartFile` (CSV).
    *   Output: `String` (XML).
    *   Logic: Parse CSV headers/rows -> Construct DMN XML.
4.  **API**:
    *   `POST /api/rules`: Create/Upload.
    *   `GET /api/rules`: List.
    *   `GET /api/rules/{key}/csv`: Download.

### Frontend
1.  **Page**: `RuleList.tsx` (Table of rules).
2.  **Modal**: `RuleUploadModal.tsx` (File upload).
3.  **Integration**: Update `WorkflowEditor.tsx` to query `/api/rules` for the "Business Rule" stage type.

