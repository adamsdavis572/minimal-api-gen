# Feature Specification: Test-Driven Refactoring to Minimal API

**Feature Branch**: `004-minimal-api-refactoring`  
**Created**: 2025-11-10  
**Status**: Draft  
**Input**: User description: "Refactor generator and templates from FastEndpoints to Minimal API using TDD"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Refactor Java Logic for Minimal API (Priority: P1)

As a generator developer, I need to refactor the Java methods in MinimalApiServerCodegen (processOpts, postProcessOperationsWithModels, apiTemplateFiles, supportingFiles) to remove FastEndpoints logic and add Minimal API-specific logic so that the generator prepares the correct data model for Minimal API templates.

**Why this priority**: The Java logic determines what data is available to templates. Without refactored Java code, templates cannot generate Minimal API patterns correctly.

**Independent Test**: Can be fully tested by modifying the methods, rebuilding the generator, and verifying it compiles and runs without errors (tests will fail at this stage - RED phase).

**Acceptance Scenarios**:

1. **Given** processOpts(), **When** I remove FastEndpoints options (useMediatR), **Then** I add Minimal API options (useRouteGroups, useGlobalExceptionHandler)
2. **Given** postProcessOperationsWithModels(), **When** I refactor it, **Then** I group operations by tag into an operationsByTag map
3. **Given** apiTemplateFiles(), **When** I remove endpoint.mustache mapping, **Then** no per-operation files are generated
4. **Given** supportingFiles(), **When** I refactor it, **Then** I register TagEndpoints.cs.mustache to run once per tag

---

### User Story 2 - Create Minimal API Templates (Priority: P1)

As a generator developer, I need to create new Mustache templates (TagEndpoints.cs.mustache, EndpointMapper.cs.mustache) and refactor existing templates (program.cs.mustache, csproj.mustache) to generate Minimal API code instead of FastEndpoints so that the output uses modern ASP.NET Core patterns.

**Why this priority**: Templates produce the actual code output. Without Minimal API templates, the generator cannot create the target code structure.

**Independent Test**: Can be fully tested by creating the templates, regenerating the project, and verifying Minimal API code is produced (tests will still fail - RED phase continues).

**Acceptance Scenarios**:

1. **Given** csproj.mustache, **When** I remove FastEndpoints package reference, **Then** I add FluentValidation.DependencyInjectionExtensions
2. **Given** program.cs.mustache, **When** I remove UseFastEndpoints(), **Then** I add AddValidatorsFromAssemblyContaining(), AddExceptionHandler(), and app.MapAllEndpoints()
3. **Given** new EndpointMapper.cs.mustache, **When** I implement it, **Then** it loops over operationsByTag to generate app.MapPetEndpoints(), app.MapStoreEndpoints()
4. **Given** new TagEndpoints.cs.mustache, **When** I implement it, **Then** it generates MapPetEndpoints class with group.MapPost(...) stubs for each operation

---

### User Story 3 - Iterative TDD Cycle Until Tests Pass (Priority: P1)

As a generator developer, I need to iteratively rebuild, regenerate, run tests, fix issues, and repeat until all tests from Feature 003 pass so that I prove functional equivalence between FastEndpoints and Minimal API implementations.

**Why this priority**: This is the validation phase that proves the refactoring is correct. Without passing tests, we have no confidence in the new generator.

**Independent Test**: Can be fully tested by running the test suite repeatedly after each change, tracking failures, fixing templates/Java code, and verifying when all tests finally pass (GREEN phase).

**Acceptance Scenarios**:

1. **Given** refactored generator and templates, **When** I rebuild with devbox run mvn clean package, **Then** build succeeds
2. **Given** rebuilt generator, **When** I regenerate against petstore.oas, **Then** Minimal API project is created
3. **Given** regenerated project, **When** I run tests from Feature 003, **Then** tests initially fail (RED phase documented)
4. **Given** test failures, **When** I fix template logic and regenerate, **Then** fewer tests fail
5. **Given** iterative fixes, **When** I implement all required endpoint logic, **Then** all tests pass (GREEN phase achieved)

---

### Edge Cases

- What happens when the operationsByTag grouping contains no operations for a tag?
- How does the generator handle OpenAPI specs with operations that have no tags?
- What happens if FluentValidation registration fails in the generated program.cs?
- How does the generator behave when template syntax errors occur during regeneration?
- What happens when tests pass for some operations but fail for others?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST remove FastEndpoints CLI options from processOpts()
- **FR-002**: System MUST add Minimal API CLI options (useRouteGroups, useGlobalExceptionHandler) to processOpts()
- **FR-003**: System MUST modify postProcessOperationsWithModels() to group operations by tag
- **FR-004**: System MUST modify apiTemplateFiles() to remove endpoint.mustache mapping
- **FR-005**: System MUST modify supportingFiles() to register TagEndpoints.cs.mustache per tag
- **FR-006**: System MUST delete endpoint.mustache template
- **FR-007**: System MUST create EndpointMapper.cs.mustache template
- **FR-008**: System MUST create TagEndpoints.cs.mustache template
- **FR-009**: System MUST refactor csproj.mustache to remove FastEndpoints and add FluentValidation.DependencyInjectionExtensions
- **FR-010**: System MUST refactor program.cs.mustache to use Minimal API patterns
- **FR-011**: TagEndpoints.cs.mustache MUST generate one class per OpenAPI tag
- **FR-012**: TagEndpoints.cs.mustache MUST generate MapPost/MapGet/MapPut/MapDelete calls per operation
- **FR-013**: EndpointMapper.cs.mustache MUST generate extension method calling all Map*Endpoints methods
- **FR-014**: System MUST preserve model templates unchanged (reuse principle)
- **FR-015**: System MUST maintain validator.mustache for FluentValidation support
- **FR-016**: Generated code MUST compile with devbox run dotnet build
- **FR-017**: All tests from Feature 003 MUST pass against regenerated Minimal API project

### Key Entities

- **Java Generator Logic**: Method implementations in MinimalApiServerCodegen; Key attributes: overridden methods, data model preparation, template registration
- **Mustache Template**: Code generation template file; Key attributes: template type (new/refactored/unchanged), Mustache syntax, data model variables consumed
- **Operations By Tag Map**: Java data structure grouping operations; Key attributes: tag name keys, operation list values, passed to templates
- **TDD Cycle Iteration**: One round of build-generate-test-fix; Key attributes: iteration number, test pass count, identified issues, fixes applied
- **Minimal API Project**: Generated ASP.NET Core project; Key attributes: endpoint mapper, tag endpoint classes, route groups, FluentValidation integration

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All FastEndpoints-specific Java logic removed from generator within first iteration
- **SC-002**: All Minimal API-specific templates created and FastEndpoints templates deleted
- **SC-003**: Generated Minimal API project compiles with zero errors
- **SC-004**: 100% of tests from Feature 003 pass against Minimal API output (target: all 8+ tests GREEN)
- **SC-005**: Complete TDD cycle (RED → GREEN) documented with number of iterations (target: under 10 iterations)
- **SC-006**: Model templates remain unchanged proving 99-100% reusability
- **SC-007**: No manual code changes required in generated project to make tests pass (generator produces correct code)

## Assumptions

- The test suite from Feature 003 is comprehensive and proves functional equivalence
- Model templates are truly framework-agnostic and require no changes
- FluentValidation patterns are compatible between FastEndpoints and Minimal APIs
- The operationsByTag grouping correctly handles all OpenAPI spec structures
- Tests can be pointed to newly generated project without test code modifications
- Sufficient time allocated for iterative TDD cycles (may require multiple attempts)

## Out of Scope

- Adding new test cases beyond Feature 003 baseline
- Performance optimization of generated code
- Adding Minimal API features not present in FastEndpoints (new capabilities)
- Documentation of changes (Phase 5)
- Cleanup of old templates (Phase 5)
- Production-ready error handling in generated code
