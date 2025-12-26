# C. Screen Mapping - Technical TODOs

The entities `ScreenMapping` and `ScreenDefinition` exist, but API layers and validation logic are needed.

## 1. API Implementation (Backend)
- [x] **ScreenDefinition Controller**:
    - [x] `POST /api/screens`: Create/Update screen definition (JSON layout).
    - [x] `GET /api/screens`: List all available screens (for Configurator dropdown).
    - [x] `GET /api/screens/{screenCode}`: Get specific screen details.
- [x] **ScreenMapping Controller**:
    - [x] `POST /api/stages/{stageCode}/mapping`: Create/Update mapping.
    - [x] `GET /api/stages/{stageCode}/mapping`: Get current mapping.

## 2. Validation Logic
- [x] Ensure `screenCode` referenced in `ScreenMapping` exists in `ScreenDefinition` table.
- [x] Validate `layoutJson` schema (optional, but recommended if invalid JSON crashes frontend).

## 3. Frontend Integration (Workflow UI)
- [x] **Configurator UI**:
    - [x] Dropdown to select from `GET /api/screens`.
    - [x] Toggle for `AccessType` (Editable/Read-Only).
- [x] **Runtime UI**:
    - [x] Fetch form key from BPMN task.
    - [x] Call `GET /api/screens/{formKey}` to render layout.
    - [x] Respect `AccessType` (disable inputs if Read-Only).
