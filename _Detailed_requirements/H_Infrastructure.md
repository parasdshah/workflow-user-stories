# H. Infrastructure - Detailed User Stories

## H.1 H2 Schema Initialization
**User Story**: As an Admin, I want the H2 database schema to be initialized automatically on startup, so that the application works out-of-the-box.

**Acceptance Criteria**:
- [ ] Liquibase/Flyway scripts present for `workflow_master`, `stage_config`, `screen_mapping`, etc.
- [ ] Flowable tables (`ACT_*`) are created by the process engine automatically.
- [ ] Seed data for modules or basic templates is loaded.

**Dependencies**:
- None

---

## H.2 Spring Boot REST APIs
**User Story**: As a Developer, I want to expose RESTful APIs for all configuration and runtime operations, observing standard verbs and status codes.

**Acceptance Criteria**:
- [ ] APIs under `/api/workflows` and `/api/runtime`.
- [ ] Use DTOs to separate Entity model from API contract.
- [ ] Support CORS for frontend access.

**Dependencies**:
- Supports: [Frontend UI](../Workflow_UI.md)

---

## H.3 Exception Handling & Validation
**User Story**: As a Developer, I want a centralized exception handling mechanism, so that clients receive consistent error responses (e.g., 400 Bad Request with field error details).

**Acceptance Criteria**:
- [ ] `@ControllerAdvice` implemented.
- [ ] Handles `MethodArgumentNotValidException` for validation errors.
- [ ] Handles `ResourceNotFoundException` with 404.
- [ ] Returns standardized JSON error object.

**Dependencies**:
- None
