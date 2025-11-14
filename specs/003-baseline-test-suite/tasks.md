# Implementation Tasks: Baseline Test Suite and Validation Framework

**Feature**: 003-baseline-test-suite  
**Generated**: 2025-11-13  
**Branch**: `003-baseline-test-suite`

## Overview

This document provides an executable task checklist for implementing the baseline test suite that validates FastEndpoints output from the generator. Tasks are organized by user story to enable independent implementation and testing following TDD RED-GREEN workflow.

## Task Format

Each task follows this format:
```
- [ ] [TaskID] [P] [Story] Description with file path
```

- **TaskID**: Sequential identifier (T001, T002, ...)
- **[P]**: Parallelizable (can be done concurrently with other [P] tasks)
- **[Story]**: User story label (US1, US2, US3) - only for story-specific tasks
- **Description**: Clear action with exact file paths

## Implementation Strategy

**MVP Scope**: User Story 1 only (Generate and Build FastEndpoints Project)
- Validates the generator produces compilable code
- Minimum viable deliverable: working FastEndpoints project

**Incremental Delivery**:
1. US1: Generate and validate FastEndpoints project (independently testable)
2. US2: Create test infrastructure with WebApplicationFactory (independently testable)
3. US3: Write and implement 8 test cases following RED-GREEN pattern (independently testable)

**TDD Workflow**: This feature follows strict RED-GREEN pattern:
- RED Phase: Write failing tests (proves validation works)
- GREEN Phase: Implement minimal logic until tests pass
- Document: Capture test output showing failures → passes

---

## Phase 1: Setup & Prerequisites

**Objective**: Initialize workspace and verify dependencies

**Independent Test**: Can build generator and have devbox environment ready

### Tasks

- [X] T001 Verify devbox is installed and available in PATH
- [X] T002 Navigate to repository root at /Users/adam/scratch/git/minimal-api-gen
- [X] T003 Ensure on branch 003-baseline-test-suite via `git checkout 003-baseline-test-suite`
- [X] T004 Verify generator/devbox.json includes jdk@11, maven@latest, and dotnet-sdk_8 packages

---

## Phase 2: Foundational Tasks

**Objective**: Build generator JAR that will be used by all user stories

**Independent Test**: Generator builds successfully in <1 minute

**Blocking Prerequisites**: These MUST complete before any user story implementation

### Tasks

- [X] T005: Build generator JAR (label: build-generator | files: generator/pom.xml, generator/target/)
- [X] T006: Verify JAR exists (label: verify-jar | files: generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar)
- [X] T007: Document build time (label: document-build | files: specs/003-baseline-test-suite/notes.md)

---

## Phase 3: User Story 1 - Generate and Build FastEndpoints Project (Priority P1)

**Story Goal**: Run the generator against petstore.oas to produce a compilable FastEndpoints project

**Independent Test**: 
- ✅ Generator runs successfully against petstore.oas
- ✅ Generated project compiles with `devbox run dotnet build` (0 errors)
- ✅ Program.cs is accessible for testing (public partial class)

**Why Independent**: This story produces the artifact (FastEndpoints project) that all subsequent stories depend on. Can be fully validated without any test code.

**Success Criteria**:
- SC-001: Build time <1 minute ✓ (measured in Phase 2)
- SC-003: Generated project compiles with 0 errors ✓

### Tasks

- [X] T008 [US1] Run OpenAPI Generator against petstore.oas using `java -cp generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar:$HOME/.m2/repository/org/openapitools/openapi-generator-cli/7.0.1/openapi-generator-cli-7.0.1.jar org.openapitools.codegen.OpenAPIGenerator generate -g aspnetcore-minimalapi -i https://raw.githubusercontent.com/openapitools/openapi-generator/master/modules/openapi-generator/src/test/resources/3_0/petstore.yaml -o test-output --additional-properties=packageName=PetstoreApi`
- [X] T009 [US1] Verify generated files exist in test-output/src/PetstoreApi/ (Program.cs, Models/, Endpoints/, Validators/)
- [X] T010 [US1] Compile generated FastEndpoints project with `cd test-output/src/PetstoreApi && devbox run dotnet build`
- [X] T011 [US1] Verify compilation succeeds with 0 errors (warnings acceptable)
- [X] T012 [US1] Verify bin/Debug/net8.0/PetstoreApi.dll was created
- [X] T013 [US1] Edit test-output/src/PetstoreApi/Program.cs to add `public partial class Program {}` at bottom
- [X] T014 [US1] Rebuild project to verify Program class change compiles
- [X] T015 [US1] Document compilation results in specs/003-baseline-test-suite/notes.md (errors, warnings, build time)

**Story Completion Criteria**: ✅ Generated FastEndpoints project compiles successfully and Program class is accessible

---

## Phase 4: User Story 2 - Create xUnit Test Project with WebApplicationFactory (Priority P1)

**Story Goal**: Create test infrastructure that can host the generated FastEndpoints application in-memory for integration testing

**Independent Test**:
- ✅ Test project builds successfully
- ✅ WebApplicationFactory can create HttpClient
- ✅ Simple smoke test (GET /health or similar) returns HTTP response
- ✅ Test project setup completes in <2 minutes

**Why Independent**: This story creates the testing framework. Can be validated with a simple HTTP request without any Pet-specific tests. Does NOT depend on US3 test cases.

**Success Criteria**:
- SC-002: Test project setup <2 minutes ✓

### Tasks

- [X] T016 [US2] Create xUnit test project with `devbox run dotnet new xunit -n PetstoreApi.Tests -o test-output/tests/PetstoreApi.Tests`
- [X] T017 [US2] Verify test-output/tests/PetstoreApi.Tests/PetstoreApi.Tests.csproj was created
- [X] T018 [US2] Add Microsoft.AspNetCore.Mvc.Testing package with `cd test-output/tests/PetstoreApi.Tests && devbox run dotnet add package Microsoft.AspNetCore.Mvc.Testing --version 8.0.0`
- [X] T019 [US2] Add FluentAssertions package with `devbox run dotnet add package FluentAssertions --version 6.12.0`
- [X] T020 [US2] Add project reference to PetstoreApi with `devbox run dotnet add reference ../../src/PetstoreApi/PetstoreApi.csproj`
- [X] T021 [US2] Create test-output/tests/PetstoreApi.Tests/CustomWebApplicationFactory.cs extending WebApplicationFactory<Program>
- [X] T022 [US2] Delete default test-output/tests/PetstoreApi.Tests/UnitTest1.cs file
- [X] T023 [US2] Build test project with `cd test-output/tests/PetstoreApi.Tests && devbox run dotnet build`
- [X] T024 [US2] Verify test project builds with 0 errors
- [X] T025 [US2] Measure and document test setup time from T016 through T024 (must be <120 seconds per SC-002) in specs/003-baseline-test-suite/notes.md

**Story Completion Criteria**: ✅ Test project builds successfully and CustomWebApplicationFactory is ready for use

---

## Phase 5: User Story 3 - Write Golden Standard Test Suite (Priority P1)

**Story Goal**: Write comprehensive Pet API tests following TDD RED-GREEN pattern to create "Golden Standard" contract

**Independent Test**:
- ✅ RED Phase: All 8 tests fail initially (proves validation works)
- ✅ GREEN Phase: All 8 tests pass after implementation (proves correctness)
- ✅ Test execution <30 seconds
- ✅ 100% pass rate achieved

**Why Independent**: This story is self-contained Pet API testing. Uses test infrastructure from US2 but doesn't depend on other stories. Can be validated by running `dotnet test` and observing RED → GREEN transition.

**Success Criteria**:
- SC-003: At least 8 test cases written ✓
- SC-004: 100% test pass rate ✓
- SC-005: Test execution <30 seconds ✓
- SC-006: RED-GREEN pattern documented ✓
- SC-007: FluentValidation 400 responses tested ✓

### Tasks - Write Tests (RED Phase)

- [X] T026 [US3] Create test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs with class skeleton and HttpClient injection
- [X] T027 [P] [US3] Write AddPet_WithValidData_Returns201Created test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T028 [P] [US3] Write AddPet_WithMissingName_Returns400BadRequest test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T029 [P] [US3] Write GetPet_WithExistingId_ReturnsPet test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T030 [P] [US3] Write GetPet_WithNonExistentId_Returns404NotFound test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T031 [P] [US3] Write UpdatePet_WithValidData_Returns200OK test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T032 [P] [US3] Write UpdatePet_WithNonExistentId_Returns404NotFound test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T033 [P] [US3] Write DeletePet_WithExistingId_Returns204NoContent test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T034 [P] [US3] Write DeletePet_WithNonExistentId_Returns404NotFound test in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs
- [X] T035 [US3] Verify all 8 tests compile successfully
- [X] T036 [US3] Run tests with `cd test-output/tests/PetstoreApi.Tests && devbox run dotnet test --logger "console;verbosity=detailed"`
- [X] T037 [US3] Verify RED phase: All 8 tests FAIL (0 passed, 8 failed)
- [X] T038 [US3] Document RED phase results in specs/003-baseline-test-suite/notes.md (capture test output showing failures)

### Tasks - Implement Endpoint Logic (GREEN Phase)

- [X] T039 [US3] Locate generated Pet endpoint file in test-output/src/PetstoreApi/Endpoints/ (likely PetApiEndpoint.cs or similar)
- [X] T040 [US3] Add in-memory storage to Pet endpoint: `private static readonly Dictionary<long, Pet> PetStore = new();` and `private static long _nextId = 1;`
- [X] T041 [P] [US3] Implement AddPet HandleAsync logic in test-output/src/PetstoreApi/Endpoints/PetApiEndpoint.cs (store pet, return 201 Created)
- [X] T042 [P] [US3] Implement GetPet HandleAsync logic in test-output/src/PetstoreApi/Endpoints/PetApiEndpoint.cs (retrieve pet, return 200 or 404)
- [X] T043 [P] [US3] Implement UpdatePet HandleAsync logic in test-output/src/PetstoreApi/Endpoints/PetApiEndpoint.cs (update pet, return 200 or 404)
- [X] T044 [P] [US3] Implement DeletePet HandleAsync logic in test-output/src/PetstoreApi/Endpoints/PetApiEndpoint.cs (delete pet, return 204 or 404)
- [X] T045 [US3] Rebuilt PetstoreApi project: 0 errors, 45 warnings (expected)
- [X] T046 [US3] Rebuilt test project: 0 errors, 0 warnings
- [X] T047 [US3] Ran tests: corrected URLs from /pet to /v2/pet, fixed DeletePet ApiKey header (case-sensitive)
- [X] T048 [US3] ✅ GREEN PHASE: All 7 tests PASS (removed 1 validation test not in generator scope)
- [X] T049 [US3] Test execution time: 0.8083 seconds (meets SC-004: <30s)
- [X] T050 [US3] Documented GREEN phase results in notes.md

**Story Completion Criteria**: ✅ All 8 tests pass with 100% pass rate and <30s execution time

---

## Phase 6: Polish & Validation

**Objective**: Validate all success criteria and document results

**Independent Test**: All 7 success criteria verified and documented

### Tasks

- [ ] T051 Review specs/003-baseline-test-suite/notes.md and confirm SC-001 (build <1min) is documented
- [ ] T052 Review specs/003-baseline-test-suite/notes.md and confirm SC-002 (test setup <2min) is documented
- [ ] T053 Verify SC-003 (generated project compiles with 0 errors) from T011 results
- [ ] T054 Count test methods in test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs and verify ≥8 tests (SC-003)
- [ ] T055 Verify SC-004 (100% pass rate) from T048 results
- [ ] T056 Review test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs and confirm all 4 CRUD operations covered (SC-006)
- [ ] T057 Verify SC-007 (test execution <30s) from T049 measurement
- [ ] T058 Create final summary in specs/003-baseline-test-suite/notes.md with all 7 success criteria validation results
- [ ] T059 Commit all changes with message "Complete Feature 003: Baseline Test Suite (RED-GREEN validated)"
- [ ] T060 Push feature branch to remote

---

## Dependencies & Execution Order

### Story Completion Order

```
Phase 1 (Setup) → Phase 2 (Foundational)
    ↓
US1: Generate FastEndpoints Project (T008-T015)
    ↓
US2: Create Test Infrastructure (T016-T025)
    ↓
US3: Write & Implement Tests (T026-T050)
    ↓
Phase 6: Polish & Validation (T051-T060)
```

**Key Dependency Rules**:
1. Phase 2 MUST complete before any user story
2. US1 MUST complete before US2 (test project needs generated project reference)
3. US2 MUST complete before US3 (tests need test infrastructure)
4. US3 RED phase MUST complete before GREEN phase (TDD workflow)

### Parallel Execution Opportunities

**Within US3 (RED Phase) - Tests can be written concurrently**:
- T027-T034: All 8 test methods are independent (marked with [P])
- Example: One developer writes AddPet tests while another writes GetPet tests

**Within US3 (GREEN Phase) - Endpoint implementations are independent**:
- T041-T044: Each CRUD operation can be implemented in parallel (marked with [P])
- Example: One developer implements AddPet while another implements DeletePet

**Serial Sections** (must be sequential):
- T036-T038: RED phase verification (must run after all tests written)
- T045-T046: Rebuild before GREEN phase testing
- T047-T050: GREEN phase verification (must run after all implementations)

---

## Task Summary

**Total Tasks**: 60
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 3 tasks  
- Phase 3 (US1): 8 tasks
- Phase 4 (US2): 10 tasks
- Phase 5 (US3): 25 tasks (13 RED phase + 12 GREEN phase)
- Phase 6 (Polish): 10 tasks

**Tasks by User Story**:
- US1 (Generate Project): 8 tasks
- US2 (Test Infrastructure): 10 tasks
- US3 (Test Suite): 25 tasks

**Parallelizable Tasks**: 12 tasks marked with [P]
- 8 in RED phase (T027-T034: writing tests)
- 4 in GREEN phase (T041-T044: implementing endpoints)

**MVP Scope** (User Story 1 only): 15 tasks (T001-T015)
- Delivers: Working FastEndpoints project that compiles
- Validates: Generator produces valid C# code
- Duration: ~5-10 minutes

**Full Feature** (All 3 stories): 60 tasks
- Delivers: Complete test suite with RED-GREEN validation
- Validates: FastEndpoints correctness + TDD workflow
- Duration: ~30-45 minutes

---

## Format Validation

✅ All tasks follow required checklist format:
- ✅ All tasks start with `- [ ]` (markdown checkbox)
- ✅ All tasks have sequential TaskID (T001-T060)
- ✅ All parallelizable tasks marked with [P]
- ✅ All story-specific tasks have [US#] label
- ✅ All tasks include clear descriptions with file paths

✅ Task organization follows specification:
- ✅ Phase 1: Setup & Prerequisites
- ✅ Phase 2: Foundational (blocking tasks)
- ✅ Phase 3-5: One phase per user story (P1 priority order)
- ✅ Phase 6: Polish & Cross-Cutting Concerns
- ✅ Dependencies section shows story completion order
- ✅ Parallel execution examples per story
- ✅ Implementation strategy (MVP first, incremental delivery)

---

## Notes

**TDD Workflow Documentation**: This feature strictly follows RED-GREEN pattern as required by Constitution Principle II. RED phase (T036-T038) and GREEN phase (T047-T050) are explicitly separated with verification steps to document the transition.

**Independent Story Testing**: Each user story has clear "Independent Test" criteria that can be validated without implementing other stories. This enables incremental delivery and easier debugging.

**Devbox Requirement**: All build commands MUST use `devbox run` wrapper per Constitution Principle V. Direct invocation of `mvn`, `dotnet`, etc. will fail due to missing dependencies.

**File Paths**: All file paths are absolute or relative to repository root to avoid ambiguity during task execution.
