# Organization Calendar & User Delegation (OOO) Design

## 1. Objective
To ensure workflows respect time-off, both at an organizational level (Holidays) and individual level (Vacation/Sick Leave).
1.  **SLA Calculation**: Due dates should exclude Organization Holidays.
2.  **Assignment**: Tasks should not be assigned to users on leave; they should be automatically re-routed to a designated substitute.

---

## 2. Backend Architecture

### 2.1 Data Model
New entities are required to store calendar and leave data.

> [!NOTE]
> **Architectural Decision: Persistence vs. External Fetch**
> Unlike User Data (which changes often and is complex), **Holiday Data** is static (changes once a year) but accessed frequently (every SLA calculation loop).
> *   **Recommendation**: **Persist Locally** in Workflow Service.
> *   **Reasoning**: Real-time fetching for every date in a 10-day SLA calculation is too slow and risky.
> *   **Sync Strategy**: Provide an API `POST /api/calendars/import` to allow the External Adapter to push holiday updates to the Workflow Service annually or on change.

**Entity: `OrgHoliday`**
```java
@Entity
public class OrgHoliday {
    @Id private Long id;
    private LocalDate date;
    private String description; 
    @Column(nullable = false)
    private String region; // "US", "IN", "APAC", "GLOBAL"
}
```

**Entity: `UserLeave` (OOO Configuration)**
```java
@Entity
public class UserLeave {
    @Id private Long id;
    private String userId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String substituteUserId; 
    private boolean active;
}
```

### 2.2 Service Layer: `CalendarService` & `UserContext`
A central service to query availability, requiring **Regional Context**.
*   `boolean isHoliday(LocalDate date, String region)`
*   `UserLeave getActiveLeave(String userId)`
*   `LocalDate calculateSlaDueDate(LocalDate startDate, int durationDays, String region)`

**User Home Location Lookup**:
To know *which* region to apply, we fetch the User's profile from the **External User Adapter** (Design AH).
*   `UserContext context = userAdapter.getContext(userId);`
*   `String homeRegion = context.getRegion();` // e.g., "IN"

---

## 3. Workflow Integration (Logic)

### 3.1 SLA & Due Dates (Org Calendar)
Flowable provides a `BusinessCalendar` interface. We will implement a custom `OrgBusinessCalendar`.
*   **Mechanism**: When a Timer or Due Date is set (e.g., `PT48H`), Flowable calls `resolveDuedate`.
*   **Logic**:
    1.  Start at `Now`.
    2.  Add hours/days.
    3.  If the calculated date falls on a **Weekend** or **OrgHoliday**, shift to the next business day.
*   **Result**: An SLA of "2 Days" might span 4 actual days if there is a weekend + holiday.

### 3.2 Automatic Substitution (User Calendar)
This logic hooks into the **User Assignment Resolver** (defined in Design `AE`).

**Updated Assignment Flow**:
1.  **Identify Implied User**: The logic (Rule/RoundRobin) selects `Alice`.
2.  **Availability Check**: Call `CalendarService.getActiveLeave("Alice")`.
3.  **Substitution**:
    *   **If User is Present**: Assign to `Alice`.
    *   **If User is OOO**:
        *   Check for `substituteUserId` (e.g., `Bob`).
        *   **Assign to Bob** (optionally add a task variable `originalAssignee=Alice` for tracking).
        *   *Notification*: Email Bob telling him he is covering for Alice.
4.  **Fallback**: If Bob is ALSO away, or no substitute defined, fallback to the **Task Manager** or **Admin Group**.

---

## 4. UI Requirements

### 4.1 Admin: Organization Holidays
**Screen**: `Admin > Workflow Settings > Holiday Calendar`
*   **View**: FullCalendar.js style month view.
*   **Action**: Click a date to toggle "Holiday".
*   **Feature**: "Import Holidays" (CSV/API) for bulk setup.

### 4.2 User: Out of Office (OOO) Settings
**Screen**: `User Profile > Availability`
*   **Toggle**: "Enable Out of Office"
*   **Date Range**: [Start Date] - [End Date]
*   **Substitute Selection**: Dropdown [ Select User... ]
    *   *Validation*: Cannot select a user who is also OOO.
*   **Message**: "Reason for leave" (Optional, for audit).

---

## 5. Summary of Flow
1.  **Admin** configures "Dec 25" as Holiday.
2.  **User (Alice)** requests leave "Jan 1 - Jan 5" and selects "Bob" as substitute.
3.  **Workflow**:
    *   Task created on **Dec 24** with **2 Day SLA**.
    *   Use Org Calendar: Dec 25 is skip. Due Date = **Dec 27**.
    *   Task created on **Jan 2** intended for **Alice**.
    *   System checks Leave -> Finds Alice away -> Assigns to **Bob**.
