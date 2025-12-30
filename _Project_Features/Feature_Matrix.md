# Feature Matrix & Status

## 1. Developed Features (In Scope & Completed)

### Workflow Management
- [x] **Workflow Configuration**: Create, Edit, and Save workflows with metadata (Name, Code, SLA).
- [x] **Stage Management**: Add, Edit, Delete, and Reorder stages.
- [x] **Screen Mapping**: Associate specific screens (Editable/Read-Only) with workflow stages.
- [x] **Soft Delete**: Undeploy workflows while retaining history; ability to rollback.
- [x] **Version Control**: Auto-versioning of deployments via Flowable.

### Runtime Engine
- [x] **Process Generation**: Dynamic generation of BPMN 2.0 XML from JSON Stage Config.
- [x] **Execution**: Start Workflow instances (Cases).
- [x] **Task Management**: User Tasks assigned to groups/users.
- [x] **Hooks**: Pre/Post Entry and Exit Java Hooks for stages.
- [x] **Workflow Actors**:Individual 

### Advanced Workflow Logic
- [x] **Nested Workflows**: Embed child workflows within a parent stage.
- [x] **Parallel Execution**: Group stages to execute concurrently (Parallel Gateway).
- [x] **Business Rule tasks (DMN)**: Integrate Decision Tables as workflow stages.

### User Interface
- [x] **Task Inbox**: View active tasks and claim/complete them.
- [x] **Timeline View**: Visual history of case progression, including actions taken.
- [x] **Deployment History**: View past versions of workflows.
- [x] **BPMN Visualizer (Read-Only)**: Auto-generated diagram of the workflow structure.
- [x] **Dashboard Stats**: Basic counters for active/completed cases.
- [x] **Audit Log**: Searchable history of system actions.
- [x] **Rule Management**: UI to upload and list DMN (CSV) rules.

### Backend Infrastructure
- [x] **H2 Database**: Persistent file-based storage.
- [x] **Migration Scripts**: SQL patches (V0, V1, V2) for schema evolution.
- [x] **API Gateway**: Centralized routing (Port 8080).
- [x] **Service Registry**: Eureka-based discovery.

---

## 2. Pending Features (Future Scope)

### Core Workflow Enhancements
- [ ] **Rule-Based Routing**: Conditional branching based on DMN/Variable outputs (e.g., If Risk=High -> Go to Stage X).
- [ ] **Dynamic Assignee**: Assign tasks based on logic/matrix rather than static candidate groups.
- [ ] **Timer Boundary Events**: Configurable timeouts/escalations in UI (Backend partially supports SLA).
- [ ] **External Connectors**: UI to which API to be called on completion of a stage.
- [ ] **Workflow Actors**: Committee, Pool of Users 
- [ ] **Search APIs**: Open tasks per user, pool, committee, etc.

### Runtime Engine
- [ ] **SLA Tracking**: Tracking duration against configured SLAs.

### User Interface Extensions
- [ ] **Graphical BPMN Editor (Edit Mode)**: Drag-and-drop workflow builder (currently using Form/List view).
- [ ] **Sync Graphical BPMN Editor with Form/List view**: 

### Integration & Security
- [ ] **Notification Channel Config**: UI to configure Email/SMS templates (currently placeholders).
