# Workflow Service Sequence Diagrams

## 1. Workflow Definition & Deployment
This flow illustrates how a user creates a new workflow configuration, adds stages, and deploys it to the Flowable Engine.

```mermaid
sequenceDiagram
    actor BA as Business Analyst
    participant UI as Workflow UI
    participant WC as WorkflowController
    participant WDS as WorkflowDefinitionService
    participant BGS as BpmnGeneratorService
    participant DB as Database (StageConfig)
    participant ENG as Flowable Engine

    Note over BA, ENG: Phase 1: Configuration
    BA->>UI: Create "Loan Approval" Workflow
    UI->>WC: POST /api/workflows
    WC->>WDS: createWorkflow()
    WDS->>DB: Save WorkflowMaster
    DB-->>WDS: Ack
    WDS-->>WC: WorkflowId
    WC-->>UI: Success

    BA->>UI: Add Stages (Validation, Approval)
    UI->>WC: POST /api/workflows/{id}/stages
    WC->>WDS: saveStage()
    WDS->>DB: Save StageConfig
    DB-->>WDS: Ack
    WDS-->>WC: Success

    Note over BA, ENG: Phase 2: Deployment
    BA->>UI: Click "Publish/Deploy"
    UI->>WC: POST /api/deployments/{id}/deploy
    WC->>DS: DeploymentService.deploy()
    DS->>DB: Fetch All StageConfigs (Sorted)
    DB-->>DS: List<StageConfig>
    DS->>BGS: generateBpmnXml(stages)
    Note right of BGS: 1. Create Process & Start Event<br/>2. Iterate Stages -> Create UserTasks/ServiceTasks<br/>3. Connect Edges (Flows)<br/>4. Apply Hooks (Listeners)<br/>5. Auto Layout
    BGS-->>DS: BPMN XML String
    DS->>ENG: repositoryService.createDeployment().addString(xml).deploy()
    ENG-->>DS: DeploymentId
    DS->>DB: Update WorkflowMaster (status=ACTIVE)
    DS-->>WC: Success
    WC-->>UI: "Deployed Successfully"
```

## 2. Case Initiation & Execution
This flow shows how a new Case (Process Instance) is started and how tasks are completed.

```mermaid
sequenceDiagram
    actor User as End User
    participant App as Client App
    participant CC as CaseController
    participant CS as CaseService
    participant ENG as Flowable Engine
    participant DB as Flowable DB

    Note over User, DB: Start Case
    User->>App: "Start Loan Application"
    App->>CC: POST /api/cases/start (workflow=LOAN)
    CC->>CS: startCase(workflowCode, data)
    CS->>ENG: runtimeService.startProcessInstanceByKey(LOAN, data)
    ENG->>DB: Create Process Instance
    ENG->>DB: Create First Task (Stage 1)
    ENG-->>CS: ProcessInstance
    CS-->>CC: CaseId
    CC-->>App: CaseId: 101

    Note over User, DB: Complete Task
    User->>App: Complete "Validation" Task
    App->>CC: POST /api/cases/{id}/tasks/complete
    CC->>CS: completeTask(taskId, outcome)
    CS->>ENG: taskService.complete(taskId, vars)
    
    rect rgb(240, 240, 240)
        Note right of ENG: Flowable Internal Execution
        ENG->>DB: Update Task (Completed)
        ENG->>ENG: Evaluate Outgoing Sequence Flows
        ENG->>ENG: Check Conditions (${outcome}=='APPROVE')
        ENG->>DB: Create Next Task (Stage 2)
        Note right of ENG: Fire TaskListeners (Hooks)
    end

    CS-->>CC: Success
    CC-->>App: "Task Completed"

    Note over User, DB: View Progress
    User->>App: View Timeline
    App->>CC: GET /api/runtime/cases/{id}/stages
    CC->>CS: getStages(caseId)
    CS->>DB: Query HistoricActivityInstances
    DB-->>CS: List<History>
    CS->>CS: Map to StageDTO
    CS-->>CC: JSON Response
    CC-->>App: Render Timeline UI
```
