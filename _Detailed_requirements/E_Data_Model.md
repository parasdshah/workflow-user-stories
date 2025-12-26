# E. Data Model - Detailed User Stories

## E.1 Data Storage Strategy
**User Story**: As an Architect, I want to define a hybrid data storage strategy, so that we can balance structured query performance with flexible data gathering.

**Acceptance Criteria**:
- [ ] **Core Business Data**: Stored in standard relational tables (e.g., `LoanApplication`).
- [ ] **Process Variables**: Stored in Flowable's `act_ru_variable`.
- [ ] **Flexible Data**: JSON blobs or EAV for dynamic screen fields.
- [ ] Application Service manages the persistence of payload data before/after workflow steps.

**Dependencies**:
- Prerequisite for: [C.3 Reuse Screens](../C_Screen_Mapping.md#c3-reuse-screens)

---

## E.2 Scalability
**User Story**: As an Architect, I want to evaluate scalability trade-offs, so that the system supports high transaction volumes.

**Acceptance Criteria**:
- [ ] `workflow-service` is stateless and scalable.
- [ ] Database is the single source of truth but optimized with indexes.
- [ ] Asynchronous jobs (timers) are handled by Flowable Async Executor.

**Dependencies**:
- None
