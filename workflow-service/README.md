# Workflow Service

A comprehensive workflow orchestration service built with Spring Boot and Flowable BPMN engine, providing dynamic workflow definition, execution, and management capabilities.

## Overview

The Workflow Service is a microservice that enables business process automation through:
- **Dynamic Workflow Definition**: Create and manage workflows without code deployment
- **BPMN 2.0 Support**: Full BPMN process engine powered by Flowable
- **DMN Decision Tables**: Business rule management with DMN support
- **Screen Mapping**: Dynamic UI screen configuration for workflow stages
- **Audit Trail**: Complete change history and audit logging
- **Export/Import**: Workflow portability with encryption support

## Architecture

```
workflow-service/
├── controller/       # REST API endpoints
├── service/         # Business logic layer
├── repository/      # Data access layer
├── entity/          # JPA entities
├── dto/             # Data transfer objects
├── config/          # Configuration classes
└── util/            # Utility classes
```

### Key Components

- **Flowable Engine**: BPMN 2.0 process execution engine
- **H2 Database**: Embedded database for development/demo
- **Spring Data JPA**: Data persistence layer
- **Eureka Client**: Service discovery integration
- **SpringDoc OpenAPI**: API documentation (Swagger UI)

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.3
- **Flowable**: 7.0.0
- **Spring Cloud**: 2023.0.0
- **H2 Database**: Embedded
- **Maven**: Build tool
- **SpringDoc OpenAPI**: 2.3.0

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Eureka Service Registry (running on port 8761)

### Build

```bash
cd workflow-service
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

The service will start on port **8081**.

## API Documentation

### Swagger UI

Access the interactive API documentation at:
```
http://localhost:8081/swagger-ui.html
```

### OpenAPI Specification

View the raw OpenAPI 3.0 specification at:
```
http://localhost:8081/v3/api-docs
```

### API Endpoints

The service provides the following API groups:

| Tag | Description | Base Path |
|-----|-------------|-----------|
| Workflow Management | Workflow definitions, stages, screens | `/api/workflows` |
| Case Runtime | Case instances and task execution | `/api/runtime/cases` |
| BPMN Deployment | Deploy and manage BPMN processes | `/api/deployments` |
| DMN Rules | Decision table management | `/api/rules` |
| Audit Logs | Change history and audit trail | `/api/audit-logs` |
| System Management | System operations | `/api/system` |
| Runtime Status | Case status queries | `/api/runtime` |
| Screen Definitions | UI screen management | `/api/screens` |
| Screen Mappings | Screen-to-stage mappings | `/api/stages` |
| Module Management | Application modules | `/api/modules` |
| Workflow Export/Import | Workflow portability | `/api/workflow` |

## Configuration

### Application Properties

Key configuration in [application.yml](file:///c:/workspace/workflow/workflow-user-stories/workflow-service/src/main/resources/application.yml):

```yaml
server:
  port: 8081

spring:
  application:
    name: workflow-service
  datasource:
    url: jdbc:h2:file:./data/workflowdb
    username: sa
    password: password

flowable:
  database-schema-update: true
  async-executor-activate: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Database

**H2 Console** is available at:
```
http://localhost:8081/h2-console
```

Connection details:
- **JDBC URL**: `jdbc:h2:file:./data/workflowdb`
- **Username**: `sa`
- **Password**: `password`

Database files are stored in `./data/` directory.

## Development

### Project Structure

- **Controllers**: REST API endpoints with OpenAPI annotations
- **Services**: Business logic and workflow orchestration
- **Repositories**: JPA repositories for data access
- **Entities**: JPA entities (WorkflowMaster, StageConfig, ScreenDefinition, etc.)
- **DTOs**: Data transfer objects for API requests/responses

### Key Features

#### 1. Dynamic Workflow Definition
Create workflows with stages and screens without code deployment.

#### 2. BPMN Generation
Automatically generates BPMN 2.0 XML from workflow definitions.

#### 3. DMN Decision Tables
Upload CSV files to create DMN decision tables for business rules.

#### 4. Screen Mapping
Map UI screens to workflow stages with access control.

#### 5. Audit Trail
Automatic tracking of all configuration changes with user attribution.

#### 6. Workflow Export/Import
Export workflows in encrypted or JSON format for portability.

## Testing

Run tests with:
```bash
mvn test
```

## Deployment

### Production Considerations

> [!WARNING]
> The current configuration uses H2 embedded database. For production:
> - Replace H2 with a production database (PostgreSQL, MySQL, etc.)
> - Configure proper security and authentication
> - Review and adjust Flowable async executor settings
> - Set up proper logging and monitoring

### Building for Production

```bash
mvn clean package
java -jar target/workflow-service-1.0.0-SNAPSHOT.jar
```

## Monitoring

### Actuator Endpoints

Health check endpoint:
```
http://localhost:8081/actuator/health
```

Additional actuator endpoints can be enabled in `application.yml`.

## Architecture Decisions

See [Architecture Decision Records](file:///c:/workspace/workflow/workflow-user-stories/workflow-service/docs/adr/) for key architectural decisions:

- [ADR-001: Flowable Workflow Engine](file:///c:/workspace/workflow/workflow-user-stories/workflow-service/docs/adr/001-flowable-workflow-engine.md)
- [ADR-002: H2 Embedded Database](file:///c:/workspace/workflow/workflow-user-stories/workflow-service/docs/adr/002-h2-embedded-database.md)
- [ADR-003: Microservices Architecture](file:///c:/workspace/workflow/workflow-user-stories/workflow-service/docs/adr/003-microservices-architecture.md)

## Operational Runbook

For operational procedures, troubleshooting, and maintenance, see the [Runbook](file:///c:/workspace/workflow/workflow-user-stories/workflow-service/docs/RUNBOOK.md).

## Contributing

### Code Style
- Follow Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Add OpenAPI annotations to all new endpoints
- Write unit tests for business logic

### Commit Messages
Use conventional commit format:
```
feat: add new feature
fix: bug fix
docs: documentation changes
refactor: code refactoring
test: add tests
```

## License

Apache 2.0

## Support

For issues and questions, please contact the Workflow Service Team at workflow-support@example.com.
