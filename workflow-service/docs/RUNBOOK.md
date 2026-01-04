# Workflow Service Operational Runbook

This runbook provides operational procedures, troubleshooting guides, and maintenance instructions for the Workflow Service.

## Service Overview

- **Service Name**: workflow-service
- **Port**: 8081
- **Dependencies**: Eureka Service Registry (port 8761)
- **Database**: H2 embedded (file: `./data/workflowdb`)
- **Technology**: Spring Boot 3.2.3, Flowable 7.0.0, Java 17

## Quick Reference

| Item | Value |
|------|-------|
| Health Check | `http://localhost:8081/actuator/health` |
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| H2 Console | `http://localhost:8081/h2-console` |
| Eureka Dashboard | `http://localhost:8761` |
| Log Location | Console output (configure file logging as needed) |

---

## Startup Procedures

### Prerequisites Check

Before starting the service, verify:

1. **Java Version**
   ```bash
   java -version
   # Should show Java 17 or higher
   ```

2. **Eureka Service Registry**
   ```bash
   curl http://localhost:8761
   # Should return Eureka dashboard
   ```

3. **Port Availability**
   ```bash
   # Windows
   netstat -ano | findstr :8081
   # Should return empty (port available)
   ```

### Standard Startup

1. **Navigate to service directory**
   ```bash
   cd c:\workspace\workflow\workflow-user-stories\workflow-service
   ```

2. **Start the service**
   ```bash
   mvn spring-boot:run
   ```

3. **Verify startup**
   - Watch console for "Started WorkflowServiceApplication"
   - Check health endpoint: `http://localhost:8081/actuator/health`
   - Verify Eureka registration: `http://localhost:8761`

### Production Startup

```bash
java -jar workflow-service-1.0.0-SNAPSHOT.jar
```

With custom configuration:
```bash
java -jar workflow-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Shutdown Procedures

### Graceful Shutdown

1. **Send SIGTERM** (Ctrl+C in console)
   - Spring Boot will initiate graceful shutdown
   - Allows in-flight requests to complete
   - Deregisters from Eureka

2. **Verify shutdown**
   - Check Eureka dashboard - service should be removed
   - Verify port 8081 is released

### Force Shutdown

If graceful shutdown hangs:

```bash
# Windows - Find process
netstat -ano | findstr :8081

# Kill process (replace PID)
taskkill /PID <PID> /F
```

---

## Health Checks

### Application Health

```bash
curl http://localhost:8081/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Database Health

Access H2 console: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:file:./data/workflowdb`
- Username: `sa`
- Password: `password`

Run test query:
```sql
SELECT COUNT(*) FROM WORKFLOW_MASTER;
```

### Flowable Engine Health

Check deployed workflows:
```bash
curl http://localhost:8081/api/deployments
```

### Eureka Registration

Visit: `http://localhost:8761`
- Verify "WORKFLOW-SERVICE" appears in registered instances
- Status should be "UP"

---

## Common Operations

### Deploy a Workflow

1. **Create workflow definition** via API:
   ```bash
   curl -X POST http://localhost:8081/api/workflows \
     -H "Content-Type: application/json" \
     -H "X-User-Id: admin" \
     -d '{
       "code": "SAMPLE_WF",
       "name": "Sample Workflow",
       "description": "Test workflow"
     }'
   ```

2. **Add stages** to workflow

3. **Deploy to Flowable**:
   ```bash
   curl -X POST http://localhost:8081/api/deployments/SAMPLE_WF
   ```

### Initiate a Case

```bash
curl -X POST http://localhost:8081/api/runtime/cases \
  -H "Content-Type: application/json" \
  -d '{
    "workflowCode": "SAMPLE_WF",
    "userId": "user123",
    "variables": {
      "requestId": "REQ-001"
    }
  }'
```

### View Audit Logs

```bash
# All logs
curl http://localhost:8081/api/audit-logs

# Filtered by entity
curl "http://localhost:8081/api/audit-logs?entityName=WorkflowMaster&page=0&size=20"
```

### System Reset (Development Only)

> [!CAUTION]
> This operation deletes all workflows and configuration data!

```bash
curl -X POST http://localhost:8081/api/system/reset
```

---

## Troubleshooting

### Service Won't Start

**Symptom**: Application fails to start

**Common Causes**:

1. **Port 8081 already in use**
   ```bash
   # Check what's using the port
   netstat -ano | findstr :8081
   # Kill the process or change port in application.yml
   ```

2. **Eureka not available**
   ```
   Error: Connection refused to http://localhost:8761/eureka/
   ```
   - Start Eureka service registry first
   - Or disable Eureka: `eureka.client.enabled=false`

3. **Database corruption**
   ```
   Error: Database may be already in use
   ```
   - Stop all instances of the service
   - Delete `./data/workflowdb.lock.db` file
   - Restart service

4. **Java version mismatch**
   ```
   Error: Unsupported class file major version
   ```
   - Verify Java 17+ is installed and active

### Deployment Failures

**Symptom**: Workflow deployment returns 500 error

**Troubleshooting Steps**:

1. **Check workflow definition**
   ```bash
   # Preview BPMN before deploying
   curl http://localhost:8081/api/deployments/preview/WORKFLOW_CODE
   ```

2. **Check Flowable logs**
   - Look for BPMN validation errors in console
   - Common issues: missing stages, invalid transitions

3. **Verify workflow exists**
   ```bash
   curl http://localhost:8081/api/workflows/WORKFLOW_CODE
   ```

### Case Execution Issues

**Symptom**: Case stuck or not progressing

**Troubleshooting Steps**:

1. **Check case status**
   ```bash
   curl http://localhost:8081/api/runtime/cases/{caseId}
   ```

2. **View case stages**
   ```bash
   curl http://localhost:8081/api/runtime/cases/{caseId}/stages
   ```

3. **Check Flowable async executor**
   - Verify `flowable.async-executor-activate=true` in config
   - Check for exceptions in logs

4. **Check task variables**
   - Missing required variables can block task completion

### High Memory Usage

**Symptom**: Service consuming excessive memory

**Mitigation**:

1. **Increase JVM heap**
   ```bash
   java -Xmx2g -jar workflow-service.jar
   ```

2. **Check for memory leaks**
   - Use JVisualVM or similar tool
   - Look for growing process instance count

3. **Clean up completed processes**
   - Flowable retains history by default
   - Consider implementing history cleanup job

### Database Issues

**Symptom**: Database errors or slow queries

**Troubleshooting**:

1. **Check database size**
   ```bash
   # Check file size
   dir .\data\workflowdb.mv.db
   ```

2. **Backup and compact**
   ```sql
   -- In H2 console
   SHUTDOWN COMPACT;
   ```

3. **For production**: Migrate to PostgreSQL/MySQL

---

## Monitoring

### Key Metrics to Watch

1. **Service Health**
   - Endpoint: `/actuator/health`
   - Alert if: Status != UP

2. **Eureka Registration**
   - Check: Service appears in Eureka dashboard
   - Alert if: Service not registered for > 2 minutes

3. **Active Process Instances**
   - Monitor: Number of running cases
   - Alert if: Unusual spike or continuous growth

4. **Database Size**
   - Monitor: `./data/workflowdb.mv.db` file size
   - Alert if: > 1GB (for H2)

5. **Response Times**
   - Monitor: API endpoint latency
   - Alert if: p95 > 2 seconds

### Logging

**Log Levels**:
```yaml
logging:
  level:
    com.workflow.service: DEBUG
    org.flowable: INFO
    org.springframework: WARN
```

**Important Log Patterns**:
- `Started WorkflowServiceApplication` - Successful startup
- `Deployment failed` - BPMN deployment errors
- `Error completing task` - Task execution failures

---

## Backup and Recovery

### Backup Procedures

**H2 Database Backup**:
```bash
# Stop the service
# Copy database files
copy .\data\workflowdb.mv.db .\backups\workflowdb-backup-20260104.mv.db
```

**Automated Backup Script** (PowerShell):
```powershell
$date = Get-Date -Format "yyyyMMdd-HHmmss"
Copy-Item ".\data\workflowdb.mv.db" ".\backups\workflowdb-$date.mv.db"
```

### Recovery Procedures

1. **Stop the service**
2. **Restore database file**
   ```bash
   copy .\backups\workflowdb-backup-20260104.mv.db .\data\workflowdb.mv.db
   ```
3. **Start the service**
4. **Verify data integrity**

---

## Configuration Management

### Environment-Specific Configuration

Use Spring profiles:

**application-dev.yml**:
```yaml
logging:
  level:
    com.workflow.service: DEBUG
```

**application-prod.yml**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/workflowdb
logging:
  level:
    com.workflow.service: INFO
```

Run with profile:
```bash
java -jar workflow-service.jar --spring.profiles.active=prod
```

### Sensitive Configuration

Use environment variables for secrets:
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}
workflow:
  export:
    secret: ${EXPORT_SECRET}
```

---

## Performance Tuning

### JVM Tuning

```bash
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar workflow-service.jar
```

### Database Connection Pool

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Flowable Async Executor

```yaml
flowable:
  async-executor-activate: true
  async:
    executor:
      core-pool-size: 2
      max-pool-size: 10
      queue-size: 100
```

---

## Security Considerations

### Current State (Development)

> [!WARNING]
> The current configuration is NOT production-ready:
> - No authentication/authorization
> - H2 console publicly accessible
> - Default database credentials
> - CORS allows all origins

### Production Hardening

1. **Enable Spring Security**
2. **Disable H2 console**: `spring.h2.console.enabled=false`
3. **Configure CORS properly**
4. **Use secrets management** (Vault, AWS Secrets Manager)
5. **Enable HTTPS/TLS**
6. **Implement rate limiting**

---

## Emergency Contacts

| Role | Contact | Availability |
|------|---------|--------------|
| Workflow Service Team | workflow-support@example.com | 24/7 |
| DevOps Team | devops@example.com | Business hours |
| Database Admin | dba@example.com | On-call |

---

## Appendix

### Useful SQL Queries

**Count workflows**:
```sql
SELECT COUNT(*) FROM WORKFLOW_MASTER;
```

**Active process instances**:
```sql
SELECT COUNT(*) FROM ACT_RU_EXECUTION WHERE PARENT_ID_ IS NULL;
```

**Recent deployments**:
```sql
SELECT * FROM ACT_RE_DEPLOYMENT ORDER BY DEPLOY_TIME_ DESC LIMIT 10;
```

### API Quick Reference

| Operation | Method | Endpoint |
|-----------|--------|----------|
| List workflows | GET | `/api/workflows` |
| Create workflow | POST | `/api/workflows` |
| Deploy workflow | POST | `/api/deployments/{code}` |
| Initiate case | POST | `/api/runtime/cases` |
| Get case details | GET | `/api/runtime/cases/{id}` |
| Complete task | POST | `/api/runtime/cases/{caseId}/tasks/{taskId}/complete` |
| View audit logs | GET | `/api/audit-logs` |
| System reset | POST | `/api/system/reset` |

---

**Last Updated**: 2026-01-04  
**Version**: 1.0.0  
**Maintained by**: Workflow Service Team
