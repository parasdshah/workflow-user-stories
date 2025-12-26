# I. Validation - Detailed User Stories

## I.1 Cycle Detection
**User Story**: As a System, I want to validate stage dependencies to ensure no cycles exist, so that the workflow does not enter an infinite loop.

**Acceptance Criteria**:
- [ ] Graph-based validation logic (`Depth First Search` or `Tarjan's`).
- [ ] Executed pre-save or pre-generation.
- [ ] Error returned if `Stage A` connects to `Stage B` which connects back to `Stage A`.

**Dependencies**:
- Verifies: [B.5 Stage Sequence](../B_Stage_Configuration.md#b5-stage-sequence)

---

## I.2 Hook Class Check
**User Story**: As a System, I want to verify that configured hook classes actually exist and implement the required interfaces, so that runtime `ClassNotFoundExceptions` are avoided.

**Acceptance Criteria**:
- [ ] Check `Class.forName(fqn)` during configuration save.
- [ ] Verify `ExecutionListener.class.isAssignableFrom(clazz)`.
- [ ] Warn or error if class is missing.

**Dependencies**:
- Verifies: [B.3 Java Hooks](../B_Stage_Configuration.md#b3-java-hooks)

---

## I.3 SLA Validation
**User Story**: As a System, I want to validate the SLA duration format, ensuring it is a positive number.

**Acceptance Criteria**:
- [ ] `slaDurationDays` must be >= 0.
- [ ] Validated via Bean Validation (`@Min(0)`).

**Dependencies**:
- Verifies: [A.3 SLA Tracking](../A_Workflow_Definition.md#a3-sla-tracking)

---

## I.4 Screen Integrity
**User Story**: As a System, I want to ensure that mapped screen codes refer to valid, existing Screen Definitions.

**Acceptance Criteria**:
- [ ] If `ScreenDefinition` lookup is implemented, validate `screenCode` exists in that table.

**Dependencies**:
- Verifies: [C.1 Map Screens](../C_Screen_Mapping.md#c1-map-screens-to-stages)
