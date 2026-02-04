# Sticky Role Assignment (Prior Actor Assignment)

## Overview
This feature ensures continuity and efficiency in workflows where the same role appears multiple times (e.g., Maker -> Checker -> Maker). If a role re-occurs in the same workflow instance, the system should automatically assign the task to the same user who performed the role previously, rather than selecting a new user from the pool or using Round Robin.

## User Stories

### Story 1: Auto-Assignment to Prior Actor
**As a** Process Owner
**I want** the system to automatically assign tasks to the specific user who previously worked on the workflow if the role matches
**So that** context is preserved and the same person handles the iterative steps (e.g., replying to queries, reworking).

**Scenario:**
1.  **Stage 1 (RM):** User **Alice** initiates/approves.
2.  **Stage 2 (Credit Analyst):** User **Ivy** processes.
3.  **Stage 3 (Financial Analyst):** User **Bob** processes.
4.  **Stage 4 (RM):** The workflow returns to the **RM** role.
    *   **Current Behavior:** Round Robin or Manual assignment might select any RM.
    *   **New Behavior:** The system identifies **Alice** was the previous RM for this specific case and auto-assigns Stage 4 to **Alice**.

**Acceptance Criteria:**
- [ ] During task assignment, check if the "Sticky Role" or "Prior Actor" rule is enabled for the stage.
- [ ] If enabled, query the case history to find the most recent assignee for the *same role* (or specific target stage).
- [ ] If a prior user is found, assign the task to them.
- [ ] If no prior user is found (first time this role appears), fall back to the standard assignment mechanism (Round Robin, Manual, etc.).

### Story 2: Handling Out-of-Office (OOO) for Prior Actors
**As a** System
**I want to** check if the prior actor is available (not OOO) before assigning
**So that** tasks do not get stuck with unavailable users.

**Acceptance Criteria:**
- [ ] Before assigning to the identified Prior Actor (e.g., Alice), check their availability status (OOO).
- [ ] **If User is Available:** Assign task to Alice.
- [ ] **If User is OOO:** 
    *   Follow the standard **Substitution/Delegation** logic defined for that user.
    *   If no delegation is defined, fall back to the standard assignment mechanism for the stage (e.g., Round Robin pool).

## Functional Requirements

### Configuration
1.  **Stage Config:**
    *   New Assignment Mechanism: `STICKY` or `PRIOR_ACTOR`.
    *   Optionally, allow defining the "Target Role" to look up (default to the Stage's configured Role).

### Backend Logic
1.  **Assignment Listener:** logic needs to be enhanced (or a new `StickyAssignmentListener` created).
2.  **History Query:**
    *   Query `HistoricTaskInstance` for the current process instance ID.
    *   Filter by the Role associated with the current stage (needs mapping from Task Definition Key -> Stage -> Role).
    *   Sort by End Time (descending) to get the latest actor.
3.  **Availability Check:**
    *   Call `CalendarService` or `UserAdapter` to check `isUserOOO(userId)`.
    *   If OOO, resolve substitute.

## Edge Cases
- **Role Change / User Validity:**
    - If the prior user (e.g., Alice) has changed roles and is no longer a member of the required Candidate Group for the current stage:
        - **Action:** The system must validate the user's current group membership.
        - **Fallback:** If invalid, immediately fall back to **Manual Assignment (Group Queue)**.
- **Multiple Prior Actors:** If Stage 1 was Alice, Stage 4 is Alice, Stage 7 is RM again.
    - Default to the *most recent* actor (Alice).
- **No Substitute Configured:**
    - If the user is OOO but no delegation rule is set:
    - **Fallback:** Proceed to **Manual Assignment (Group Queue)**.
