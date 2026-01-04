# ADR-002: H2 Embedded Database

**Status**: Accepted (for Development/Demo)

**Date**: 2026-01-04

## Context

The workflow service requires a database for:
- Workflow configuration storage (WorkflowMaster, StageConfig, ScreenDefinition, etc.)
- Flowable engine state (process instances, tasks, variables, history)
- Audit trail and change history
- DMN decision table deployments

For development and demonstration purposes, we need a database that:
- Requires minimal setup and configuration
- Can run embedded within the application
- Supports SQL and JPA
- Provides a web console for debugging
- Can be easily replaced with a production database

### Alternatives Considered

1. **H2** - Embedded Java SQL database
2. **PostgreSQL** - Production-grade relational database
3. **MySQL** - Popular open-source database
4. **In-Memory Collections** - Simple Java collections without persistence

## Decision

We have chosen **H2 embedded database** for development and demonstration environments.

### Rationale

**Pros**:
- **Zero Configuration**: No external database server required
- **Embedded Mode**: Runs in-process with the application
- **File-Based Persistence**: Data persists across restarts
- **H2 Console**: Built-in web console for database inspection
- **SQL Compatibility**: Supports standard SQL with PostgreSQL/MySQL compatibility modes
- **Fast Startup**: Quick initialization for development and testing
- **Small Footprint**: Minimal disk space and memory usage
- **JPA Support**: Full Hibernate/JPA compatibility

**Cons**:
- **Not Production-Ready**: Not suitable for production workloads
- **Single Connection**: Limited concurrent access in file mode
- **Performance**: Slower than production databases under load
- **No Clustering**: Cannot be used in distributed deployments
- **Limited Features**: Missing advanced features of production databases

## Consequences

### Positive

- **Developer Experience**: Developers can run the service without database setup
- **Demo Friendly**: Easy to demonstrate the service without infrastructure
- **Testing**: Simplifies integration testing with embedded database
- **Portability**: Database file can be easily backed up and shared
- **Quick Iteration**: Fast development cycles without database management overhead

### Negative

- **Production Migration Required**: Must migrate to production database before deployment
- **Performance Limitations**: Not representative of production performance
- **Data Loss Risk**: File corruption can cause data loss
- **Scalability**: Cannot handle production load or concurrent users

### Mitigation Strategies

1. **Clear Documentation**: Clearly document that H2 is for development only
2. **Database Abstraction**: Use JPA to ensure database portability
3. **Migration Path**: Provide clear instructions for PostgreSQL/MySQL migration
4. **Configuration Profiles**: Support multiple database configurations via Spring profiles
5. **Warnings**: Add warnings in logs and documentation about production use

## Production Recommendations

For production deployment, we recommend:

### PostgreSQL (Recommended)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflowdb
    driver-class-name: org.postgresql.Driver
    username: workflow_user
    password: ${DB_PASSWORD}
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

### MySQL
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/workflowdb
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: workflow_user
    password: ${DB_PASSWORD}
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
```

## Implementation Notes

- H2 configured in file mode: `jdbc:h2:file:./data/workflowdb`
- H2 console enabled at `/h2-console` for debugging
- Database files stored in `./data/` directory
- DDL auto-update enabled for development convenience
- Default credentials: `sa` / `password` (should be changed for any shared environment)

## Migration Checklist

When migrating to production database:

- [ ] Add production database driver dependency to `pom.xml`
- [ ] Update `application.yml` with production database configuration
- [ ] Set `spring.jpa.hibernate.ddl-auto` to `validate` or `none`
- [ ] Run Flyway/Liquibase migrations for schema management
- [ ] Configure connection pooling (HikariCP)
- [ ] Set up database backups and disaster recovery
- [ ] Configure database monitoring and alerting
- [ ] Test Flowable compatibility with production database
- [ ] Load test with production-like data volumes

## References

- [H2 Database Documentation](https://www.h2database.com/)
- [Spring Boot Database Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.datasource)
- [Flowable Database Support](https://www.flowable.com/open-source/docs/bpmn/ch03-Configuration#databaseConfiguration)
