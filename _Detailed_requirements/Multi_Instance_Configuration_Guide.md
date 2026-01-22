# Multi-Instance Configuration Guide: Document Batch Example

This guide demonstrates how to configure an end-to-end **Batch Processing** workflow using the new Multi-Instance features.

## Scenario
We want to upload a batch of 3 documents. The system should spawn 3 parallel "Document Review" processes. The parent process should wait until all 3 reviews are completed.

## Prerequisite: The Child Workflow
First, create the workflow that will handle a *single* item.

1.  **Create Workflow**:
    *   **Name**: Document Review
    *   **Code**: `DOC_REVIEW_CHILD`
2.  **Add Stage**:
    *   **Name**: Checker Review
    *   **Code**: `CHECKER_STAGE`
    *   **Type**: User Task
    *   **Actions**: `Approve`, `Reject`
3.  **Save**: This workflow expects variables like `docName` or `docId` to be passed to it.

---

## Step 1: Configure the Parent Workflow

1.  **Create Workflow**:
    *   **Name**: Batch Master Workflow
    *   **Code**: `BATCH_MASTER`
2.  **Add Stage (The Multi-Instance Stage)**:
    *   **Name**: Process Batch
    *   **Code**: `BATCH_PROCESSING_STAGE`
    *   **Type**: **Nested Workflow** (Call Activity)
    *   **Nested Workflow Code**: `DOC_REVIEW_CHILD` (The code from the Prerequisite)
3.  **Configure Multi-Instance**:
    *   Go to the **Configuration** tab of the stage.
    *   Scroll to **Multi-Instance (Parallel Loop)**.
    *   **Enable Switch**: Turn **ON** ("Enable Multi-Instance Execution").
    *   **Collection Variable**: Enter `documentList`
        *   *This is the name of the list variable you will send when starting the process.*
    *   **Element Variable**: Enter `singleDoc`
        *   *This is the name of the variable that will hold the single item inside the child process.*
4.  **Save** the stage and the workflow.

---

## Step 2: Start the Process (Runtime)

To test this, you need to start the `BATCH_MASTER` workflow with a JSON payload containing the `documentList`.

**API Endpoint**: `POST /api/runtime/cases`

**Payload**:
```json
{
  "workflowCode": "BATCH_MASTER",
  "userId": "admin",
  "variables": {
    "documentList": [
      { "docId": "101", "name": "Invoice_A.pdf" },
      { "docId": "102", "name": "Invoice_B.pdf" },
      { "docId": "103", "name": "Invoice_C.pdf" }
    ]
  }
}
```

## Step 3: Verify Execution

1.  **Parent Process**: Starts and moves to `BATCH_PROCESSING_STAGE`.
2.  **Child Processes**: The system automatically spawns **3** instances of `DOC_REVIEW_CHILD`.
    *   **Instance 1**: Has variable `singleDoc` = `{ "docId": "101", "name": "Invoice_A.pdf" }`
    *   **Instance 2**: Has variable `singleDoc` = `{ "docId": "102", "name": "Invoice_B.pdf" }`
    *   **Instance 3**: Has variable `singleDoc` = `{ "docId": "103", "name": "Invoice_C.pdf" }`
3.  **User Tasks**: The "Checker Review" task will appear 3 times in the task list (one for each document).
4.  **Completion**: The Parent Process will remain in `BATCH_PROCESSING_STAGE` until **ALL 3** child workflows are completed.

---

## Key Concepts
*   **Collection**: The big list (`documentList`).
*   **Element**: The single item (`singleDoc`) extracted from the list for each instance.
*   **Parallel**: All instances start at the same time.
