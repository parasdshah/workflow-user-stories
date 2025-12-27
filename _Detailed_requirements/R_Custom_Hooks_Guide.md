# Flowable Hooks (Listeners) Guide

This guide explains how to create custom logic (Hooks) for Flowable, how to structure them in a separate project, and how to run them.

## 1. Types of Hooks
There are two main types of listeners in Flowable:

1.  **Execution Listener**: Fires on process events (Start, End, Take transition).
    -   Interface: `org.flowable.engine.delegate.ExecutionListener`
2.  **Task Listener**: Fires on User Task events (Create, Assignment, Complete, Delete).
    -   Interface: `org.flowable.engine.delegate.TaskListener`

## 2. Sample Java Class

Here is a sample class that implements **both** interfaces, allowing it to be used in various contexts.

```java
package com.external.hooks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.DelegateTask;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

// @Component makes it a Spring Bean.
// If you use Full Qualified Name (FQN) in Flowable, @Component is not strictly required 
// unless you want to inject other Spring services (like EmailService).
@Component("myCustomAuditHook") 
public class MyCustomAuditHook implements ExecutionListener, TaskListener {

    // ExecutionListener: Triggered on Process Start/End or Transitions
    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String currentActivity = execution.getCurrentActivityId();
        
        System.out.println("[HOOK-EXECUTION] Event: " + eventName + ", Activity: " + currentActivity);
        
        // Example: Set a variable
        execution.setVariable("lastVisitedNode", currentActivity);
    }

    // TaskListener: Triggered on User Task Create/Complete
    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String assignee = delegateTask.getAssignee();
        
        System.out.println("[HOOK-TASK] Event: " + eventName + ", Task: " + delegateTask.getName() + ", Assignee: " + assignee);
        
        // Example: Validate on Complete
        if ("complete".equals(eventName)) {
            if (delegateTask.getVariable("isApproved") == null) {
                // You can throw exceptions to block completion
                // throw new RuntimeException("Approval status is required!");
            }
        }
    }
}
```

## 3. Can this be in a Separate Project?
**YES.** This is a common pattern for keeping business logic separate from the core workflow engine.

### How to Structure "Separate Project"
1.  Create a standard Maven/Gradle project (e.g., `company-workflow-hooks`).
2.  Add `flowable-engine` (or `flowable-engine-common-api`) as a `provided` dependency (so you have access to the interfaces).
3.  Write your hook classes in this project.
4.  Build the project into a JAR file (e.g., `company-workflow-hooks-1.0.jar`).

### Maven Dependency Example (in `company-workflow-hooks`)
```xml
<dependencies>
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-engine</artifactId>
        <version>6.8.0</version> <!-- Match your server version -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 4. How to Run with Flowable
For Flowable (or `workflow-service`) to execute your code, the **Class** must be on the **Classpath**.

### Option A: Build-time Dependency (Recommended for Spring Boot)
If you are building `workflow-service` as a Spring Boot application:
1.  Install your hooks jar to local maven repo or Nexus.
2.  Add it as a dependency in `workflow-service/pom.xml`.
    ```xml
    <dependency>
        <groupId>com.company</groupId>
        <artifactId>company-workflow-hooks</artifactId>
        <version>1.0</version>
    </dependency>
    ```
3.  Rebuild `workflow-service`. Spring Boot will include the JAR in `BOOT-INF/lib`.
4.  Spring Component Scan will detect `@Component` annotations if packages align, or you can import them.

### Option B: Runtime Classpath (External JARs)
If you want to drop a JAR without rebuilding:
1.  Launch the Java process with the external JAR in the classpath using the `-cp` (Classpath) argument or Spring Boot's loader properties (PropertiesLauncher).
2.  **Note:** Standard Spring Boot "Executable JARs" are self-contained. Adding external JARs at runtime usually requires switching to the `PropertiesLauncher` or unpacking the Boot JAR. **Option A is significantly easier.**

## 5. Referencing in BPMN / UI
In the Workflow Editor (or BPMN XML), you configure the listener:

*   **Class Delegate**: Enter the Fully Qualified Name: `com.external.hooks.MyCustomAuditHook`
*   **Delegate Expression** (if Spring Bean): `${myCustomAuditHook}`
