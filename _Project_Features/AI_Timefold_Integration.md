# Intelligent Task Assignment with Timefold (OptaPlanner)

This document outlines the plan to replace simple heuristic assignment (Round Robin) with an AI-optimized constraint solver using **Timefold Solver** (formerly OptaPlanner).

## 1. Goal
Move from "Next Available User" to "Best Suited User" assignment logic.
**Objectives**:
- **Maximize Skills Match**: Assign tasks to users who match the required skills (Hard Constraint).
- **Minimize Workload**: Balance the number of active tasks across users (Soft Constraint).
- **Prioritize High Priority**: Ensure high-priority tasks are assigned to the most efficient users (Soft Constraint).

## 2. Dependencies
Add the following to `workflow-service/pom.xml`.
*Note: Timefold is compatible with Spring Boot 3 and Java 17.*

```xml
<dependency>
    <groupId>ai.timefold.solver</groupId>
    <artifactId>timefold-solver-spring-boot-starter</artifactId>
    <version>1.1.0</version> <!-- Check for latest version -->
</dependency>
```

## 3. Domain Model (The "Planning Problem")

We need to map our Workflow concepts to Timefold's `@PlanningSolution`, `@PlanningEntity`, and `@PlanningVariable`.

### A. The Resource: `User` (Problem Fact)
Represents the available workforce.
```java
public class User {
    private String id;
    private List<String> skills;
    private int currentWorkload; // Existing tasks unrelated to this batch
    // getters/setters
}
```

### B. The Assignment: `TaskAssignment` (Planning Entity)
Represents a task that needs an assignee.
```java
@PlanningEntity
public class TaskAssignment {
    
    // Unique ID for the task
    @PlanningId
    private String taskId;
    
    private String requiredSkill;
    private int priority;
    
    // This is what Timefold will optimize!
    @PlanningVariable
    private User assignedUser; 

    // getters/setters
}
```

### C. The Schedule: `TaskSchedule` (Planning Solution)
Holds all data for a specific assignment request or batch.
```java
@PlanningSolution
public class TaskSchedule {
    
    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<User> userList;
    
    @PlanningEntityCollectionProperty
    private List<TaskAssignment> taskList;

    @PlanningScore
    private HardSoftScore score;
    
    // getters/setters
}
```

## 4. Constraints (The "Rules")

We define the rules in a `ConstraintProvider`.

```java
public class TaskAssignmentConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
            requiredSkillConflict(factory),
            minimizeWorkload(factory)
        };
    }

    // HARD CONSTRAINT: User must have the required skill
    private Constraint requiredSkillConflict(ConstraintFactory factory) {
        return factory.forEach(TaskAssignment.class)
            .filter(task -> !task.getAssignedUser().getSkills().contains(task.getRequiredSkill()))
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Missing required skill");
    }

    // SOFT CONSTRAINT: Minimize max workload per user
    private Constraint minimizeWorkload(ConstraintFactory factory) {
        return factory.forEach(TaskAssignment.class)
            .groupBy(TaskAssignment::getAssignedUser, ConstraintCollectors.count())
            .penalize(HardSoftScore.ONE_SOFT, count -> count * count) // Quadratic penalty for fairness
            .asConstraint("Minimize workload");
    }
}
```

## 5. Integration Strategy (The "Listener")

We create a new `TimefoldAssignmentListener` that replaces `RoundRobinAssignmentListener`.

### Logic Flow:
1.  **Trigger**: Workflow enters a User Task. Listener fires.
2.  **Fetch Data**:
    *   Get Candidate Group (e.g., "Underwriters").
    *   Fetch all Users in that group from `hrms-service` (including their skills/load).
3.  **Build Problem**: Create a `TaskSchedule` with 1 `TaskAssignment` (the current task) and `N` Users.
    *   *Note: For better results, we could batch assignments, but per-task is easier to start.*
4.  **Solve**:
    ```java
    @Autowired
    private SolverManager<TaskSchedule, String> solverManager;

    // ... in notify() ...
    TaskSchedule problem = buildProblem(delegateTask, candidates);
    UUID problemId = UUID.randomUUID();
    
    // Solve synchronously for simple single-task assignment (fast, <100ms)
    TaskSchedule solution = solverManager.solve(problemId.toString(), problem).getFinalBestSolution();
    
    delegateTask.setAssignee(solution.getTaskList().get(0).getAssignedUser().getId());
    ```

## 6. Migration Steps
1.  **Refactor**: Create package `com.workflow.service.solver`.
2.  **Implement**: Create the Pojo classes and ConstraintProvider.
3.  **Config**: Add `solver` config to `application.properties` (e.g., termination time 500ms).
4.  **Replace**: Update `BcpmGeneratorService` to use `TimefoldAssignmentListener` instead of `RoundRobinAssignmentListener`.
