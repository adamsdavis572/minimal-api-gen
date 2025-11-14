# Feature Specification: Baseline Test Suite and Validation Framework

**Feature Branch**: `003-baseline-test-suite`  
**Created**: 2025-11-10  
**Status**: Draft  
**Input**: User description: "Build xUnit test framework and golden standard test suite for FastEndpoints output validation"

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

### User Story 1 - Generate and Build FastEndpoints Project (Priority: P1)

As a generator developer, I need to run the generator from Feature 002 against petstore.oas to produce a FastEndpoints project that compiles successfully so that I have a baseline for testing.

**Why this priority**: Without a working FastEndpoints project, there is nothing to test. This is the prerequisite for all test development.

**Independent Test**: Can be fully tested by building the generator, running it against petstore.oas, and compiling the resulting C# project with devbox run dotnet build.

**Acceptance Scenarios**:

1. **Given** the generator from Feature 002, **When** I build it with devbox run mvn clean package, **Then** the build succeeds
2. **Given** the built generator and petstore.oas, **When** I run generation with `-g aspnetcore-minimalapi`, **Then** a FastEndpoints project is created
3. **Given** the generated project, **When** I run devbox run dotnet build, **Then** the C# project compiles with zero errors

---

### User Story 2 - Create xUnit Test Project with WebApplicationFactory (Priority: P1)

As a test developer, I need to create an xUnit test project that references the generated FastEndpoints project and uses Microsoft.AspNetCore.Mvc.Testing so that I can write integration tests against HTTP endpoints.

**Why this priority**: The test infrastructure is required before any test cases can be written. This establishes the testing framework.

**Independent Test**: Can be fully tested by creating the test project, adding the necessary NuGet packages, creating CustomWebApplicationFactory, and verifying a simple smoke test passes.

**Acceptance Scenarios**:

1. **Given** the compiled FastEndpoints project, **When** I create an xUnit test project, **Then** it references the FastEndpoints project
2. **Given** the test project, **When** I add Microsoft.AspNetCore.Mvc.Testing and FluentAssertions, **Then** packages restore successfully
3. **Given** the test project, **When** I create CustomWebApplicationFactory<Program>, **Then** it compiles and can host the application
4. **Given** the factory, **When** I create an HttpClient and call a test endpoint, **Then** I receive an HTTP response

---

### User Story 3 - Write Golden Standard Test Suite (Priority: P1)

As a test developer, I need to write comprehensive tests for all PetStore operations (AddPet, GetPet, UpdatePet, DeletePet) covering happy paths and validation failures so that I have a contract proving FastEndpoints correctness.

**Why this priority**: This test suite is the "Golden Standard" that will validate both FastEndpoints (now) and Minimal API (Phase 4) implementations. It's the core deliverable of this phase.

**Independent Test**: Can be fully tested by running all tests and verifying they pass after implementing stubbed logic in the generated endpoint handlers.

**Acceptance Scenarios**:

1. **Given** the test infrastructure, **When** I write happy path tests for AddPet, **Then** tests initially fail (RED phase)
2. **Given** failing tests, **When** I implement HandleAsync logic in AddPet endpoint to return valid responses, **Then** tests pass (GREEN phase)
3. **Given** the AddPet endpoint, **When** I send invalid data, **Then** FluentValidation returns 400 Bad Request
4. **Given** all PetStore operations, **When** I write complete test coverage, **Then** I have tests for at least 8 scenarios (4 operations × 2 test types)

---

### Edge Cases

- What happens when the generated FastEndpoints project has compilation errors?
- How does the test suite handle missing or misconfigured CustomWebApplicationFactory?
- What happens if FluentValidation is not properly registered in the generated project?
- How does the test framework behave when the HTTP client cannot connect to the test server?
- What happens when test data doesn't match the OpenAPI schema expectations?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST build the generator from Feature 002 using devbox run mvn clean package
- **FR-002**: System MUST run the generator against petstore.oas to produce a FastEndpoints project
- **FR-003**: Generated FastEndpoints project MUST compile with devbox run dotnet build
- **FR-004**: System MUST create an xUnit test project referencing the generated project
- **FR-005**: Test project MUST include Microsoft.AspNetCore.Mvc.Testing package
- **FR-006**: Test project MUST include FluentAssertions package for assertion syntax
- **FR-007**: System MUST create CustomWebApplicationFactory<Program> class
- **FR-008**: Test suite MUST create HttpClient from WebApplicationFactory
- **FR-009**: Test suite MUST include happy path tests for all PetStore CRUD operations
- **FR-010**: Test suite MUST include unhappy path tests validating FluentValidation responses
- **FR-011**: All tests MUST initially fail (RED phase) before implementation
- **FR-012**: System MUST implement stubbed HandleAsync logic in generated endpoints
- **FR-013**: All tests MUST pass (GREEN phase) after implementation
- **FR-014**: Test suite MUST cover at minimum: AddPet, GetPet, UpdatePet, DeletePet operations
- **FR-015**: Each operation MUST have both successful and validation failure test cases

### Key Entities

- **Generated Project**: The FastEndpoints C# project output from the generator; Key attributes: csproj file, endpoint classes, validators, models
- **Test Project**: xUnit test project for integration testing; Key attributes: project references, test classes, CustomWebApplicationFactory
- **Test Case**: Individual xUnit test method; Key attributes: test name, HTTP request setup, expected response, assertions
- **CustomWebApplicationFactory**: ASP.NET Core test host; Key attributes: Program class reference, HttpClient factory, test configuration
- **Golden Standard Test Suite**: Complete collection of passing tests; Key attributes: test coverage, happy/unhappy paths, validation scenarios

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Generated FastEndpoints project builds in under 1 minute with zero errors
- **SC-002**: Test project created and all dependencies restored in under 2 minutes
- **SC-003**: Complete test suite written with at least 8 test cases covering 4 operations × 2 scenarios each
- **SC-004**: 100% of tests pass after implementing stubbed endpoint logic
- **SC-005**: Test suite executes in under 30 seconds for full run
- **SC-006**: All tests follow RED-GREEN pattern (documented as initially failing, then passing)
- **SC-007**: Test coverage includes validation of FluentValidation 400 responses with error details

## Assumptions

- The generator from Feature 002 produces compilable FastEndpoints code
- petstore.oas is available and contains standard CRUD operations for Pet resource
- devbox provides both Java/Maven for generator build and .NET SDK for C# compilation
- The generated project uses standard ASP.NET Core patterns compatible with WebApplicationFactory
- FluentValidation is properly configured in the generated Program.cs
- All endpoint handlers have stubbed HandleAsync methods that can be filled with logic

## Out of Scope

- Testing non-PetStore operations (Store, User APIs)
- Performance or load testing
- Security testing (authentication, authorization)
- UI or frontend testing
- Automated test generation from OpenAPI spec
- Refactoring to Minimal API patterns (Phase 4)
- Continuous integration setup
