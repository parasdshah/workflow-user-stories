# ADR-003: Microservices Architecture with Eureka

**Status**: Accepted

**Date**: 2026-01-04

## Context

The workflow system needs to be architected to support:
- Independent deployment and scaling of components
- Service discovery and load balancing
- API gateway for unified entry point
- Separation of concerns between workflow engine and other services
- Future extensibility with additional microservices

### Alternatives Considered

1. **Microservices with Eureka** - Spring Cloud Netflix service discovery
2. **Monolithic Application** - Single deployable application
3. **Kubernetes-Native** - Use Kubernetes service discovery
4. **API Gateway Only** - Direct service-to-service calls without discovery

## Decision

We have adopted a **microservices architecture** with the following components:
- **service-registry** (Eureka Server) - Service discovery
- **api-gateway** - API gateway and routing
- **workflow-service** - Core workflow engine
- **workflow-delegates** - Custom workflow task delegates

### Rationale

**Pros**:
- **Independent Scaling**: Each service can be scaled independently based on load
- **Technology Flexibility**: Different services can use different technologies
- **Fault Isolation**: Failure in one service doesn't bring down the entire system
- **Team Autonomy**: Different teams can own different services
- **Deployment Independence**: Services can be deployed without affecting others
- **Service Discovery**: Eureka provides automatic service registration and discovery
- **Load Balancing**: Client-side load balancing with Ribbon/Spring Cloud LoadBalancer

**Cons**:
- **Operational Complexity**: More services to deploy, monitor, and maintain
- **Network Latency**: Inter-service communication adds latency
- **Distributed Debugging**: Harder to trace requests across services
- **Data Consistency**: Distributed transactions are complex
- **Infrastructure Overhead**: Requires service registry, API gateway, etc.

## Architecture Overview

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  API Gateway    │ :8080
│  (api-gateway)  │
└────────┬────────┘
         │
         │ (Service Discovery)
         ▼
┌──────────────────┐
│ Service Registry │ :8761
│    (Eureka)      │
└────────┬─────────┘
         │
         │ (Registers)
         ▼
┌──────────────────────┐
│  Workflow Service    │ :8081
│ (workflow-service)   │
└──────────────────────┘
         │
         │ (Delegates)
         ▼
┌──────────────────────┐
│ Workflow Delegates   │
│(workflow-delegates)  │
└──────────────────────┘
```

## Service Responsibilities

### Service Registry (Eureka Server)
- Service registration and discovery
- Health monitoring of registered services
- Provides service instance information to clients

### API Gateway
- Single entry point for all client requests
- Request routing to appropriate services
- Cross-cutting concerns (authentication, rate limiting, CORS)
- Load balancing across service instances

### Workflow Service
- Core workflow engine (Flowable)
- Workflow definition management
- Case execution and task management
- BPMN deployment
- DMN decision tables
- Audit logging

### Workflow Delegates
- Custom service task implementations
- External system integrations
- Reusable workflow components

## Consequences

### Positive

- **Scalability**: Can scale workflow-service independently for high load
- **Resilience**: Service registry provides automatic failover
- **Flexibility**: Easy to add new microservices (notification service, reporting service, etc.)
- **Development Speed**: Teams can work on services independently
- **Technology Evolution**: Can upgrade services independently

### Negative

- **Learning Curve**: Developers need to understand distributed systems
- **Testing Complexity**: Integration testing requires multiple services
- **Deployment Overhead**: Need to deploy and manage multiple services
- **Monitoring**: Requires distributed tracing and centralized logging
- **Network Reliability**: Dependent on network stability

### Mitigation Strategies

1. **Service Mesh**: Consider Istio/Linkerd for advanced traffic management
2. **Distributed Tracing**: Implement Spring Cloud Sleuth + Zipkin
3. **Centralized Logging**: Use ELK stack or similar
4. **Circuit Breakers**: Implement Resilience4j for fault tolerance
5. **API Contracts**: Use OpenAPI specs for service contracts
6. **Local Development**: Docker Compose for running all services locally

## Configuration

### Eureka Client Configuration (workflow-service)

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### Service Registration

Services automatically register with Eureka on startup and send heartbeats every 30 seconds.

## Future Considerations

### Potential Additional Services

1. **Notification Service**: Email/SMS notifications for workflow events
2. **Reporting Service**: Analytics and reporting on workflow execution
3. **Document Service**: Document generation and storage
4. **Integration Service**: External system integrations (CRM, ERP, etc.)
5. **User Service**: User management and authentication

### Migration to Kubernetes

If migrating to Kubernetes:
- Replace Eureka with Kubernetes service discovery
- Use Kubernetes Ingress instead of API Gateway
- Leverage Kubernetes ConfigMaps and Secrets
- Use Kubernetes health checks instead of Eureka health monitoring

## Implementation Notes

- Using Spring Cloud Netflix Eureka for service discovery
- API Gateway uses Spring Cloud Gateway
- Services communicate via REST APIs
- CORS configured to allow frontend access
- Each service has its own database schema (or database)

## References

- [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
- [Microservices Patterns](https://microservices.io/patterns/index.html)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Eureka Documentation](https://github.com/Netflix/eureka/wiki)
