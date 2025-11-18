# Tasks: Test-Driven Refactoring to Minimal API

**Input**: Design documents from `/specs/004-minimal-api-refactoring/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Feature 003 baseline tests (7 integration tests) serve as Golden Standard. No new test creation required - tests MUST pass unchanged.

**Organization**: Tasks are grouped by user story (US1, US2, US3) to enable TDD cycle tracking. All three user stories are P1 priority and work together to achieve the refactoring goal.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Generator**: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
- **Templates**: `generator/src/main/resources/aspnet-minimalapi/`
- **Generated Output**: `test-output/src/PetstoreApi/`
- **Tests**: `test-output/tests/PetstoreApi.Tests/` (unchanged from Feature 003)

---

## Phase 1: Setup (TDD Environment)

**Purpose**: Prepare TDD cycle infrastructure and validate baseline

- [X] T001 Verify Feature 003 baseline: Run `devbox run dotnet test` in test-output/tests/PetstoreApi.Tests/ and confirm 7/7 tests pass
- [X] T002 Create TDD cycle tracking file at specs/004-minimal-api-refactoring/tdd-cycles.md for iteration recording
- [X] T003 Backup current FastEndpoints templates to generator/src/main/resources/aspnet-minimalapi-backup/ for reference

---

## Phase 2: Foundational Infrastructure (T004-T010) - ~45 min

**Parallel tracks**: Dependencies & Program.cs (T004-T008 | 25min) → Build → Regenerate (T009-T010 | 20min)

- [X] **T004**: `project.csproj.mustache` - remove FastEndpoints PackageReferences [5min | depends: T003]
- [X] **T005**: `project.csproj.mustache` - add Swashbuckle.AspNetCore 6.5.0, FluentValidation packages [5min | depends: T004]
- [X] **T006**: `program.mustache` - remove FastEndpoints setup code [5min | depends: T003]
- [X] **T007**: `program.mustache` - add Minimal API setup (AddEndpointsApiExplorer, AddSwaggerGen) [5min | depends: T006]
- [X] **T008**: `program.mustache` - add placeholder calls (AddValidatorsFromAssemblyContaining, MapAllEndpoints) [5min | depends: T007]
- [X] **T009**: Build generator JAR (mvn package) [<60s per SC-001 | depends: T008]
- [X] **T010**: Regenerate test-output project, confirm compilation fails (missing endpoints), document expected errors [5min | depends: T009]

**Checkpoint**: Infrastructure templates modified - project compiles but endpoints missing

---

## Phase 3: User Story 1 - Refactor Java Logic for Minimal API (Priority: P1)

**Goal**: Modify Java generator methods to prepare operationsByTag data structure for Minimal API templates

**Independent Test**: Generator builds successfully, can be invoked, produces operationsByTag data visible in logs/debug

### TDD State: RED (Expected)

**Note**: At start of US1, tests will fail because FastEndpoints endpoints are deleted but Minimal API endpoints not yet created

### Implementation for User Story 1

- [X] T011 [US1] Modify processOpts() in generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java to remove useMediatR CLI option
- [X] T012 [US1] Modify processOpts() in generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java to add useRouteGroups and useGlobalExceptionHandler CLI options
- [X] T013 [US1] Modify postProcessOperationsWithModels() in MinimalApiServerCodegen.java to implement operationsByTag grouping per data-model.md specification
- [X] T014 [US1] Add computed fields in postProcessOperationsWithModels(): tagPascalCase, operationIdPascalCase, returnType, successCode, resultMethod per data-model.md
- [X] T015 [US1] Handle edge cases in postProcessOperationsWithModels(): untagged operations → "Default" tag, multi-tagged operations → appear in each tag group
- [X] T016 [US1] Modify apiTemplateFiles() in MinimalApiServerCodegen.java to remove endpoint.mustache mapping (no per-operation file generation)
- [X] T017 [US1] Modify supportingFiles() in MinimalApiServerCodegen.java to register TagEndpoints.cs.mustache to run once per unique tag
- [X] T018 [US1] Modify supportingFiles() in MinimalApiServerCodegen.java to register EndpointMapper.cs.mustache to run once per project
- [X] T019 [US1] Build generator with `devbox run mvn clean package` from generator/ and verify successful compilation
- [X] T020 [US1] Add debug logging to postProcessOperationsWithModels() to print operationsByTag structure for verification

**Checkpoint**: Java generator logic refactored - ready to feed data to new templates

---

## Phase 4: User Story 2 - Create Minimal API Templates (Priority: P1)

**Goal**: Create new Mustache templates and delete FastEndpoints templates to generate Minimal API code structure

**Independent Test**: Generator produces Minimal API files (PetEndpoints.cs, EndpointMapper.cs) with correct structure, project compiles

### TDD State: RED → Approaching GREEN

**Note**: After US2, project structure will be correct but endpoint logic may be incomplete (tests may partially pass)

### Delete FastEndpoints Templates

- [X] T021 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/endpoint.mustache (FastEndpoints-specific)
- [X] T022 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/request.mustache (FastEndpoints validation pattern)
- [X] T023 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/requestClass.mustache (FastEndpoints pattern)
- [X] T024 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/requestRecord.mustache (FastEndpoints pattern)
- [X] T025 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/endpointType.mustache (FastEndpoints base class)
- [X] T026 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/endpointRequestType.mustache (FastEndpoints pattern)
- [X] T027 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/endpointResponseType.mustache (FastEndpoints pattern)
- [X] T028 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/loginRequest.mustache (FastEndpoints-specific)
- [X] T029 [P] [US2] Delete generator/src/main/resources/aspnet-minimalapi/userLoginEndpoint.mustache (FastEndpoints-specific)

### Create Minimal API Templates

- [X] T030 [US2] Create generator/src/main/resources/aspnet-minimalapi/TagEndpoints.cs.mustache following contracts/TagEndpoints.md specification
- [X] T031 [US2] Implement TagEndpoints.cs.mustache template variables: packageName, operationsByTag, tag, tagPascalCase, operations per data-model.md
- [X] T032 [US2] Implement TagEndpoints.cs.mustache operation loop: generate Map{Tag}Endpoints() extension method with group.MapPost/Get/Put/Delete calls
- [X] T033 [US2] Implement TagEndpoints.cs.mustache parameter binding: [FromBody] for body params, [FromHeader] for header params, automatic for path/query
- [X] T034 [US2] Implement TagEndpoints.cs.mustache FluentValidation pattern: IValidator<T> DI injection, await ValidateAsync(), Results.ValidationProblem()
- [X] T035 [US2] Implement TagEndpoints.cs.mustache response metadata: .WithName(), .WithSummary(), .Produces<T>(), .ProducesProblem()
- [X] T036 [US2] Implement TagEndpoints.cs.mustache return types: Results.Created/Ok/NoContent/NotFound/ValidationProblem per resultMethod mapping
- [X] T037 [US2] Create generator/src/main/resources/aspnet-minimalapi/EndpointMapper.cs.mustache following contracts/EndpointMapper.md specification
- [X] T038 [US2] Implement EndpointMapper.cs.mustache template variables: packageName, routePrefix, operationsByTag (tag names only)
- [X] T039 [US2] Implement EndpointMapper.cs.mustache MapAllEndpoints() method with route group creation and Map{Tag}Endpoints() calls
- [X] T040 [US2] Build generator with `devbox run mvn clean package` from generator/ and verify successful compilation
- [X] T041 [US2] Regenerate test-output project, verify new files created: Endpoints/PetEndpoints.cs, Extensions/EndpointMapper.cs
- [X] T042 [US2] Run `devbox run dotnet build` from test-output/src/PetstoreApi/ and verify compilation succeeds

**Checkpoint**: Minimal API templates created - project structure correct, compiles successfully

---

## Phase 5: User Story 3 - Iterative TDD Cycle Until Tests Pass (Priority: P1)

**Goal**: Iteratively fix template logic and inject PetStore CRUD operations until all Feature 003 tests pass (GREEN)

**Independent Test**: Run `devbox run dotnet test` and track pass/fail count - iteration complete when 7/7 tests GREEN

### TDD Cycle: RED → GREEN Iterations

- [ ] T043 [US3] TDD Cycle 1: Run `devbox run dotnet test` from test-output/tests/PetstoreApi.Tests/ and record baseline failures in tdd-cycles.md
- [ ] T044 [US3] TDD Cycle 1: Analyze test output for root causes (404 Not Found, 422 Validation, 500 Server Error, etc.)
- [ ] T045 [US3] TDD Cycle 1: Fix identified issues in TagEndpoints.cs.mustache or EndpointMapper.cs.mustache (route registration, path templates, etc.)
- [ ] T046 [US3] TDD Cycle 1: Rebuild generator (`devbox run mvn clean package`), regenerate project, rerun tests and document results in tdd-cycles.md

- [ ] T047 [US3] TDD Cycle 2: Inject PetStore static class logic into TagEndpoints.cs.mustache for CRUD operations (AddPet, GetPetById, UpdatePet, DeletePet)
- [ ] T048 [US3] TDD Cycle 2: Verify PetStore.AddPet() returns Pet with auto-incremented ID and Location header in Created() response
- [ ] T049 [US3] TDD Cycle 2: Verify PetStore.GetPetById() returns Pet or NotFound() based on existence
- [ ] T050 [US3] TDD Cycle 2: Verify PetStore.UpdatePet() returns Ok(updated) or NotFound() based on existence
- [ ] T051 [US3] TDD Cycle 2: Verify PetStore.DeletePet() validates ApiKey header, returns NoContent() or NotFound()
- [ ] T052 [US3] TDD Cycle 2: Rebuild, regenerate, rerun tests - target: 4-5 tests passing

- [ ] T053 [US3] TDD Cycle 3: Fix remaining validation issues (FluentValidation DI registration, ValidationProblem responses)
- [ ] T054 [US3] TDD Cycle 3: Fix parameter binding issues ([FromBody], [FromHeader] attributes)
- [ ] T055 [US3] TDD Cycle 3: Fix response type issues (TypedResults vs Results, status codes)
- [ ] T056 [US3] TDD Cycle 3: Rebuild, regenerate, rerun tests - target: 6-7 tests passing

- [ ] T057 [US3] TDD Cycle 4+: Continue iterative fixes until all 7 tests pass (may require additional cycles)
- [ ] T058 [US3] Final validation: Run `devbox run dotnet test --logger "console;verbosity=detailed"` and confirm 7/7 GREEN
- [ ] T059 [US3] Document final TDD cycle count in tdd-cycles.md and specs/004-minimal-api-refactoring/plan.md

**Checkpoint**: All Feature 003 tests pass (7/7 GREEN) - refactoring functionally equivalent to baseline

---

## Phase 6: User Story Validation (Cross-Cutting)

**Goal**: Validate all user stories completed successfully and document TDD process

- [ ] T060 [US1] Validate US1: Review MinimalApiServerCodegen.java changes - operationsByTag logic present, FastEndpoints options removed, Minimal API options added
- [ ] T061 [US2] Validate US2: Review template files - TagEndpoints.cs.mustache and EndpointMapper.cs.mustache exist, FastEndpoints templates deleted
- [ ] T062 [US3] Validate US3: Review tdd-cycles.md - RED→GREEN progression documented, final cycle shows 7/7 tests pass
- [ ] T063 Run quickstart.md validation: Follow quickstart.md TDD workflow guide and verify all steps executable
- [ ] T064 Verify model templates unchanged: Confirm model.mustache, modelClass.mustache, modelRecord.mustache, enumClass.mustache not modified (Constitution Principle III)
- [ ] T065 Verify SC-001: Confirm all FastEndpoints Java logic removed (processOpts, postProcessOperationsWithModels clean)
- [ ] T066 Verify SC-002: Confirm all Minimal API templates created and all FastEndpoints templates deleted (file count matches plan)
- [ ] T067 Verify SC-003: Run `devbox run dotnet build` and confirm zero compilation errors
- [ ] T068 Verify SC-004: Run `devbox run dotnet test` and confirm 7/7 tests pass (100% pass rate from Feature 003)
- [ ] T069 Verify SC-005: Count TDD cycles in tdd-cycles.md and confirm under 10 iterations (target met)
- [ ] T070 Verify SC-006: Verify model template files unchanged (git diff shows no modifications to 4 model templates)
- [ ] T071 Verify SC-007: Verify generated code compiles and tests pass without manual modifications (no manual edits to test-output/)

---

## Phase 7: Polish & Documentation

**Purpose**: Clean up, validate build performance, document results

- [ ] T072 [P] Run generator build performance test: `time devbox run mvn clean package` from generator/ and verify <60s (Feature 003 SC-001)
- [ ] T073 [P] Run generated code build test: `time devbox run dotnet build` from test-output/src/PetstoreApi/ and verify <10s
- [ ] T074 [P] Run test execution performance test: `time devbox run dotnet test` from test-output/tests/PetstoreApi.Tests/ and verify <30s (Feature 003 SC-004)
- [ ] T075 Live server validation: Start server with `devbox run dotnet run --urls "http://localhost:5002"` and verify Swagger UI at /swagger
- [ ] T076 Live server curl validation: Run curl tests from quickstart.md against running server (POST/GET/PUT/DELETE operations)
- [ ] T077 Update plan.md Phase sections with completion status and actual TDD cycle count
- [ ] T078 Update .github/copilot-instructions.md with Feature 004 completion and technologies used
- [ ] T079 Git commit with message: "feat(004): Complete Minimal API refactoring with TDD (X cycles, 7/7 tests pass)"
- [ ] T080 Remove backup templates from generator/src/main/resources/aspnet-minimalapi-backup/ (cleanup)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001-T003) - BLOCKS all user story work
- **User Story 1 (Phase 3)**: Depends on Foundational (T004-T010) - Java logic refactoring
- **User Story 2 (Phase 4)**: Depends on User Story 1 (T011-T020) - Templates need operationsByTag data structure
- **User Story 3 (Phase 5)**: Depends on User Story 2 (T021-T042) - TDD cycle needs both Java logic and templates
- **Validation (Phase 6)**: Depends on User Story 3 (T043-T059) - Validates all stories complete
- **Polish (Phase 7)**: Depends on Validation (T060-T071) - Final refinements

### User Story Dependencies

- **US1 → US2**: Templates consume operationsByTag data prepared by US1 Java logic
- **US2 → US3**: TDD cycle requires both Java logic (US1) and templates (US2) to generate code
- **US3 validates US1+US2**: Passing tests prove both Java logic and templates work correctly together

**Note**: Unlike typical multi-story features, these three stories MUST execute sequentially due to technical dependencies (data preparation → template consumption → validation).

### Within Each User Story

**User Story 1 (Java Logic)**:
- CLI options (T011-T012) before data processing (T013-T015)
- Data structure (T013-T015) before template registration (T016-T018)
- All changes before build (T019)

**User Story 2 (Templates)**:
- Delete old templates (T021-T029) can run in parallel
- Create TagEndpoints template (T030-T036) before EndpointMapper template (T037-T039) - EndpointMapper calls TagEndpoints methods
- Build (T040) before regeneration (T041-T042)

**User Story 3 (TDD Cycles)**:
- Each cycle must complete before next cycle starts
- Within a cycle: Run tests → Analyze → Fix → Rebuild → Regenerate → Rerun tests
- Cycles continue until GREEN (7/7 tests pass)

### Parallel Opportunities

**Phase 1 (Setup)**: All tasks sequential (T001 validates baseline, T002 creates tracking, T003 backs up files)

**Phase 2 (Foundational)**: 
- T004-T005 can run in parallel (same file, different sections)
- T006-T008 can run in parallel (same file, different sections)
- Build/regenerate (T009-T010) must be sequential after template changes

**Phase 4 (User Story 2 - Delete FastEndpoints Templates)**:
- T021-T029 (9 delete tasks) can ALL run in parallel - different files, no dependencies

**Phase 6 (Validation)**:
- T060-T062 (US validation) can run in parallel - different review targets
- T063-T071 (SC validation) can run in parallel - different verification checks

**Phase 7 (Polish)**:
- T072-T074 (performance tests) can run in parallel - different commands
- T075-T076 (live server tests) must be sequential (start server, then curl)

---

## Parallel Example: Phase 4 Delete Templates (9 tasks simultaneously)

```bash
# Terminal 1-9: Delete all FastEndpoints templates in parallel
rm generator/src/main/resources/aspnet-minimalapi/endpoint.mustache &
rm generator/src/main/resources/aspnet-minimalapi/request.mustache &
rm generator/src/main/resources/aspnet-minimalapi/requestClass.mustache &
rm generator/src/main/resources/aspnet-minimalapi/requestRecord.mustache &
rm generator/src/main/resources/aspnet-minimalapi/endpointType.mustache &
rm generator/src/main/resources/aspnet-minimalapi/endpointRequestType.mustache &
rm generator/src/main/resources/aspnet-minimalapi/endpointResponseType.mustache &
rm generator/src/main/resources/aspnet-minimalapi/loginRequest.mustache &
rm generator/src/main/resources/aspnet-minimalapi/userLoginEndpoint.mustache &
wait

# All deletions complete in parallel - proceed to T030 (create new templates)
```

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)

**Target**: All three user stories (US1, US2, US3) - they form a single atomic refactoring

**Rationale**: Cannot partially refactor code generator - need complete Java logic + templates + validation to prove correctness

**MVP Deliverable**: Generator that produces Minimal API code passing all 7 Feature 003 tests

### Incremental Delivery

**Iteration 1** (US1): Java logic refactored, generator compiles
- **Deliverable**: Modified MinimalApiServerCodegen.java with operationsByTag
- **Validation**: Generator builds, can be invoked (but output may not compile yet)

**Iteration 2** (US2): Templates created, project compiles
- **Deliverable**: TagEndpoints.cs.mustache and EndpointMapper.cs.mustache
- **Validation**: Generated project compiles (but tests fail - RED state)

**Iteration 3** (US3): TDD cycles complete, tests pass
- **Deliverable**: Fully functional Minimal API generator
- **Validation**: All 7 Feature 003 tests GREEN

### Risk Mitigation

**Risk 1**: TDD cycles exceed 10 iterations (SC-005 violation)
- **Mitigation**: Backup templates exist (T003), can restore and retry with different approach
- **Decision Point**: After cycle 5, review tdd-cycles.md and adjust strategy if needed

**Risk 2**: Model templates require changes (Constitution Principle III violation)
- **Mitigation**: Research.md confirms tests work unchanged, data-model.md ensures compatibility
- **Decision Point**: If model changes needed, STOP and re-plan (constitution violation)

**Risk 3**: Tests pass but manual code changes required (SC-007 violation)
- **Mitigation**: TDD cycle must fix templates, not generated output
- **Decision Point**: Any manual test-output/ edits indicate template bug - fix template and regenerate

### TDD Cycle Strategy

**Expected Progression**:
1. **Cycle 1** (Baseline RED): 0/7 tests pass - establish failure patterns
2. **Cycle 2** (Route Fixes): 2-3/7 tests pass - fix endpoint registration and routing
3. **Cycle 3** (Validation Fixes): 4-5/7 tests pass - wire FluentValidation correctly
4. **Cycle 4** (Logic Injection): 6-7/7 tests pass - inject PetStore CRUD operations
5. **Cycle 5+** (Edge Cases): 7/7 tests pass - fix remaining edge cases if needed

**Tracking**: tdd-cycles.md records each iteration with:
- Date
- Changes made
- Build status
- Test results (X/7 passed)
- Failures analyzed
- Next actions

---

## Task Summary

**Total Tasks**: 80
- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 7 tasks
- **Phase 3 (US1 - Java Logic)**: 10 tasks
- **Phase 4 (US2 - Templates)**: 22 tasks (9 deletes, 13 creates)
- **Phase 5 (US3 - TDD Cycles)**: 17 tasks (iterative)
- **Phase 6 (Validation)**: 12 tasks
- **Phase 7 (Polish)**: 9 tasks

**User Story Breakdown**:
- **US1**: 10 tasks (T011-T020) - Java generator logic refactoring
- **US2**: 22 tasks (T021-T042) - Template deletion and creation
- **US3**: 17 tasks (T043-T059) - TDD cycle iterations

**Parallel Opportunities**: 16 tasks marked [P]
- Phase 2: 0 parallel (sequential infrastructure changes)
- Phase 4: 9 parallel (delete FastEndpoints templates)
- Phase 6: 3 parallel (validation checks)
- Phase 7: 4 parallel (performance tests)

**Independent Test Criteria**:
- **US1**: Generator builds successfully, operationsByTag visible in logs
- **US2**: Generated project compiles, new Minimal API files present
- **US3**: All 7 Feature 003 tests pass (7/7 GREEN)

**MVP Scope**: All 3 user stories (atomic refactoring - cannot deliver partial generator)

---

## Format Validation

✅ **All tasks follow checklist format**: `- [ ] [ID] [P?] [Story] Description with file path`
✅ **Task IDs sequential**: T001 through T080
✅ **Story labels present**: [US1], [US2], [US3] on user story phase tasks
✅ **File paths included**: All implementation tasks specify exact file locations
✅ **Parallel markers**: [P] on 16 tasks that can run concurrently
✅ **Phases organized by user story**: Each phase maps to spec.md priorities (all P1)
✅ **Dependencies documented**: Clear execution order with blocking relationships explained
✅ **Independent test criteria**: Each user story has validation method
✅ **TDD approach**: Iterative cycle structure in Phase 5 (US3)
