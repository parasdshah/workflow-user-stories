# ADR-001: Flowable Workflow Engine Selection

**Status**: Accepted

**Date**: 2026-01-04

## Context

The workflow service requires a robust BPMN 2.0 process engine to orchestrate business workflows. We need an engine that supports:
- Dynamic workflow definition and deployment
- BPMN 2.0 standard compliance
- DMN (Decision Model and Notation) support for business rules
- Spring Boot integration
- Active community and commercial support
- Scalability for enterprise use

### Alternatives Considered

1. **Flowable** - Open-source BPMN engine with Spring Boot starter
2. **Camunda** - Popular BPMN platform with extensive features
3. **Activiti** - Original open-source BPMN engine (Flowable is a fork)
4. **jBPM** - Red Hat's business process management solution
5. **Custom Implementation** - Build a simple workflow engine from scratch

## Decision

We have chosen **Flowable 7.0.0** as our workflow engine.

### Rationale

**Pros**:
- **Spring Boot Integration**: First-class Spring Boot starter with auto-configuration
- **BPMN 2.0 Compliance**: Full support for BPMN 2.0 specification
- **DMN Support**: Built-in DMN engine for decision tables and business rules
- **Active Development**: Regular releases and active community
- **Lightweight**: Can run embedded without external dependencies
- **API-First**: Comprehensive REST API for workflow management
- **Multi-tenancy**: Built-in support for multi-tenant deployments
- **Async Execution**: Robust async job executor for long-running processes
- **Database Agnostic**: Supports multiple databases (H2, PostgreSQL, MySQL, Oracle, etc.)

**Cons**:
- **Learning Curve**: BPMN 2.0 specification has complexity
- **Resource Usage**: Process engine adds memory and CPU overhead
- **Database Schema**: Flowable creates ~50 tables for its internal state
- **Version Compatibility**: Upgrading between major versions can be challenging

### Comparison with Alternatives

| Feature | Flowable | Camunda | Activiti | jBPM |
|---------|----------|---------|----------|------|
| Spring Boot Starter | ✅ Excellent | ✅ Good | ✅ Good | ⚠️ Limited |
| BPMN 2.0 Support | ✅ Full | ✅ Full | ✅ Full | ✅ Full |
| DMN Support | ✅ Built-in | ✅ Built-in | ❌ Limited | ✅ Built-in |
| Community | ✅ Active | ✅ Very Active | ⚠️ Moderate | ✅ Active |
| License | Apache 2.0 | Apache 2.0 | Apache 2.0 | Apache 2.0 |
| Commercial Support | ✅ Available | ✅ Available | ⚠️ Limited | ✅ Red Hat |

## Consequences

### Positive

- **Rapid Development**: Spring Boot starter enables quick setup and configuration
- **Standards-Based**: BPMN 2.0 compliance ensures portability and best practices
- **Extensibility**: Can customize and extend engine behavior through listeners and delegates
- **Tooling**: Can use BPMN modeling tools (though we generate BPMN programmatically)
- **Production-Ready**: Battle-tested engine used in enterprise applications

### Negative

- **Database Complexity**: Flowable's database schema is complex with many tables
- **Performance Overhead**: Process engine adds latency compared to simple state machines
- **Deployment Size**: Flowable dependencies increase application size (~15MB)
- **Lock-in**: Switching to another engine would require significant refactoring

### Mitigation Strategies

1. **Performance**: Use async executor for long-running tasks
2. **Complexity**: Abstract Flowable details behind service layer
3. **Database**: Use connection pooling and optimize queries
4. **Monitoring**: Implement metrics and logging for process execution

## Implementation Notes

- Using `flowable-spring-boot-starter-process` for BPMN support
- Using `flowable-spring-boot-starter-dmn` for decision table support
- Configured with H2 for development, but designed to support production databases
- Async executor enabled for background job processing

## References

- [Flowable Documentation](https://www.flowable.com/open-source/docs/)
- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)
- [Spring Boot Flowable Starter](https://github.com/flowable/flowable-engine/tree/main/modules/flowable-spring-boot)
