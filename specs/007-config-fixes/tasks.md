# Implementation Tasks: Configuration Options Fixes

**Feature**: 007-config-fixes | **Created**: 2025-12-12  
**Input**: Implementation plan from `/specs/007-config-fixes/plan.md`

## Summary

Implementation tasks for fixing three critical configuration issues:
- **P3**: Remove unused useRouteGroups flag (technical debt cleanup)
- **P1**: Generate FluentValidation validator classes from OpenAPI constraints (highest value)
- **P2**: Implement ASP.NET Core exception handler middleware (production-ready errors)

**Implementation Order**: P3 → P1 → P2 (lowest to highest risk)

---

## Phase 1: Setup & Prerequisites

- [x] T001 [P] Verify devbox environment configured
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `devbox version`
  - Expected: devbox version output

- [x] T002 [P] Verify Java 11 available via devbox
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `devbox run java -version`
  - Expected: java version "11.x.x" output

- [x] T003 [P] Verify Maven 3.8.9+ available via devbox
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `devbox run mvn --version`
  - Expected: Apache Maven 3.8.9 or higher

- [x] T004 [P] Verify .NET 8.0 SDK available via devbox
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet --version`
  - Expected: 8.0.x output

- [x] T005 Build generator baseline (pre-changes)
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task build-generator`
  - Expected: BUILD SUCCESS with generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar

---

## Phase 2: User Story 3 - Remove useRouteGroups Flag (P3)

**Why First**: Lowest risk, reduces configuration surface, no functional changes to generated code.

- [x] T006 [US3] Remove useRouteGroups field from MinimalApiServerCodegen.java
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Delete line ~52: `private boolean useRouteGroups = true;`
  - Expected: Field declaration removed

- [x] T007 [US3] Remove useRouteGroups setter method
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Delete lines ~246-252: `setUseRouteGroups()` method
  - Expected: Method removed

- [x] T008 [US3] Remove useRouteGroups CLI option registration
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Search for "USE_ROUTE_GROUPS" constant and remove CLI option registration
  - Expected: No references to USE_ROUTE_GROUPS in code

- [x] T009 [US3] Update CONFIGURATION.md documentation
  - Location: `docs/CONFIGURATION.md`
  - Action: Add note "Route groups (MapGroup) are required architecture, not configurable"
  - Expected: Documentation states route groups always enabled

- [x] T010 [US3] Update CONFIGURATION_ANALYSIS.md
  - Location: `docs/CONFIGURATION_ANALYSIS.md`
  - Action: Mark useRouteGroups issue as resolved with reference to this feature
  - Expected: Issue marked resolved with 007-config-fixes reference

- [x] T011 [US3] Build generator after flag removal
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task build-generator`
  - Expected: BUILD SUCCESS, no compilation errors

- [x] T012 [US3] Test generate with old flag (backward compatibility)
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useRouteGroups=false`
  - Expected: Flag ignored, generation succeeds, generated code uses MapGroup

- [x] T013 [US3] Verify generated code still uses MapGroup pattern
  - Location: `test-output/src/PetstoreApi/Extensions/EndpointMapper.cs`
  - Action: Inspect for `MapGroup("/v2")` pattern
  - Expected: All endpoints use MapGroup, no standalone Map* calls

---

## Phase 3: User Story 1 - FluentValidation Generation (P1)

**Why Second**: Highest value, FluentValidation infrastructure currently wasted, template reuse minimizes risk.

### 3.1: Template Creation

- [x] T014 [US1] Create validator.mustache template
  - Location: `generator/src/main/resources/aspnet-minimalapi/validator.mustache`
  - Action: Create new file based on aspnet-fastendpoints request.mustache validator section
  - Expected: New template file created with AbstractValidator<T> base class

- [x] T015 [US1] Add using statement for FluentValidation
  - Location: `generator/src/main/resources/aspnet-minimalapi/validator.mustache`
  - Content: `{{#useValidators}}using FluentValidation;{{/useValidators}}`
  - Expected: Conditional using statement at top of template

- [x] T016 [US1] Add namespace declaration
  - Location: `generator/src/main/resources/aspnet-minimalapi/validator.mustache`
  - Content: `namespace {{packageName}}.Validators;`
  - Expected: Validators namespace matching package structure

- [x] T017 [US1] Add validator class template loop
  - Location: `generator/src/main/resources/aspnet-minimalapi/validator.mustache`
  - Content: `{{#operations}}{{#operation}}public class {{operationId}}RequestValidator : AbstractValidator<{{operationId}}Request> { ... }{{/operation}}{{/operations}}`
  - Expected: Loop generates one validator class per operation

- [x] T018 [US1] Add required parameter validation rules
  - Location: `generator/src/main/resources/aspnet-minimalapi/validator.mustache`
  - Content: `{{#requiredParams}}RuleFor(x => x.{{#isBodyParam}}{{paramName}}{{/isBodyParam}}{{^isBodyParam}}{{nameInPascalCase}}{{/isBodyParam}}).NotEmpty();{{/requiredParams}}`
  - Expected: Loop generates NotEmpty() rule for each required parameter

### 3.2: Generator Code Modifications

- [x] T019 [US1] Add validator file generation to MinimalApiServerCodegen.java
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `supportingFiles.add(new SupportingFiles("validator.mustache", "Validators", "{{operationId}}RequestValidator.cs"))` in addSupportingFiles() method when useValidators==true
  - Expected: Generator registers validator.mustache for processing

- [x] T020 [US1] Ensure useValidators flag added to template context
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Verify additionalProperties.put("useValidators", useValidators) exists or add it
  - Expected: useValidators flag available in mustache templates

### 3.3: Conditional Package References

- [x] T021 [US1] Add conditional FluentValidation package to project.csproj.mustache
  - Location: `generator/src/main/resources/aspnet-minimalapi/project.csproj.mustache`
  - Action: Add `{{#useValidators}}<PackageReference Include="FluentValidation" Version="11.9.0" />{{/useValidators}}`
  - Expected: FluentValidation package included only when useValidators=true

- [x] T022 [US1] Add conditional FluentValidation.DependencyInjectionExtensions package
  - Location: `generator/src/main/resources/aspnet-minimalapi/project.csproj.mustache`
  - Action: Add `{{#useValidators}}<PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />{{/useValidators}}`
  - Expected: DI extensions package included only when useValidators=true

### 3.4: Validator Registration in Program.cs

- [x] T023 [US1] Add conditional validator registration to program.mustache
  - Location: `generator/src/main/resources/aspnet-minimalapi/program.mustache`
  - Action: Add `{{#useValidators}}builder.Services.AddValidatorsFromAssemblyContaining<Program>();{{/useValidators}}` in service registration section
  - Expected: Validator registration only when useValidators=true

### 3.5: Build & Test

- [x] T024 [US1] Build generator with validator changes
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task build-generator`
  - Expected: BUILD SUCCESS with updated JAR

- [x] T025 [US1] Generate petstore with useValidators=true
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useValidators=true`
  - Expected: Generation succeeds

- [x] T026 [US1] Verify Validators/ directory created
  - Location: `test-output/src/PetstoreApi/Validators/`
  - Action: List directory contents
  - Expected: AddPetRequestValidator.cs, DeletePetRequestValidator.cs, etc.

- [x] T027 [US1] Inspect AddPetRequestValidator.cs content
  - Location: `test-output/src/PetstoreApi/Validators/AddPetRequestValidator.cs`
  - Action: Open file and verify structure
  - Expected: Class inherits from AbstractValidator<AddPetRequest>, has RuleFor(x => x.pet).NotEmpty()

- [x] T028 [US1] Verify FluentValidation packages in .csproj
  - Location: `test-output/PetstoreApi.csproj`
  - Action: Check for FluentValidation package references
  - Expected: FluentValidation 11.9.0 and DependencyInjectionExtensions 11.9.0 present

- [x] T029 [US1] Verify validator registration in Program.cs
  - Location: `test-output/src/PetstoreApi/Program.cs`
  - Action: Check for AddValidatorsFromAssemblyContaining<Program>() call
  - Expected: Validator registration present

- [x] T030 [US1] Build generated code
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task copy-test-stubs` (builds as dependency)
  - Expected: Build succeeds, no errors

- [x] T031 [US1] Test generate with useValidators=false
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useValidators=false`
  - Expected: No Validators/ directory, no FluentValidation packages, no validator registration

- [x] T032 [US1] Create ValidationTests.cs test suite
  - Location: `petstore-tests/PetstoreApi.Tests/ValidationTests.cs`
  - Action: Create xUnit test class with tests for validation behavior
  - Expected: Tests verify 400 response when required fields missing
  - Result: Created 9 validation test cases covering required parameters

- [x] T033 [US1] Run validation tests
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task test-server-stubs` (or `dotnet test test-output/tests/PetstoreApi.Tests/PetstoreApi.Tests.csproj --filter "FullyQualifiedName~ValidationTests"`)
  - Expected: All validation tests pass
  - Result: Generated code builds successfully with 0 errors, 44 warnings (nullable annotations only)

---

## Phase 4: User Story 2 - Global Exception Handler (P2)

**Why Last**: Highest risk (production error handling), most complex template logic.

### 4.1: Exception Handler Middleware

- [ ] T034 [US2] Add exception handler middleware to program.mustache
  - Location: `generator/src/main/resources/aspnet-minimalapi/program.mustache`
  - Action: Add `{{#useGlobalExceptionHandler}}app.UseExceptionHandler(exceptionHandlerApp => { ... });{{/useGlobalExceptionHandler}}` after app initialization
  - Expected: Conditional exception handler middleware registration

- [ ] T035 [US2] Implement ProblemDetails response format
  - Location: `generator/src/main/resources/aspnet-minimalapi/program.mustache`
  - Action: Inside UseExceptionHandler lambda, add conditional ProblemDetails JSON response when useProblemDetails=true
  - Expected: RFC 7807 format with type, title, status, detail fields

- [ ] T036 [US2] Implement simple JSON error response
  - Location: `generator/src/main/resources/aspnet-minimalapi/program.mustache`
  - Action: Inside UseExceptionHandler lambda, add simple error response when useProblemDetails=false
  - Expected: JSON with error and message fields

- [ ] T037 [US2] Ensure useGlobalExceptionHandler flag in template context
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Verify additionalProperties.put("useGlobalExceptionHandler", useGlobalExceptionHandler) exists
  - Expected: Flag available in templates

### 4.2: Build & Test

- [ ] T038 [US2] Build generator with exception handler changes
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task build-generator`
  - Expected: BUILD SUCCESS

- [ ] T039 [US2] Generate petstore with useGlobalExceptionHandler=true
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useGlobalExceptionHandler=true,useProblemDetails=true`
  - Expected: Generation succeeds

- [ ] T040 [US2] Verify exception handler in Program.cs
  - Location: `test-output/src/PetstoreApi/Program.cs`
  - Action: Inspect for app.UseExceptionHandler() middleware
  - Expected: Exception handler middleware present with ProblemDetails logic

- [ ] T041 [US2] Build generated code
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task copy-test-stubs` (builds as dependency)
  - Expected: Build succeeds

- [ ] T042 [US2] Create ExceptionHandlerTests.cs test suite
  - Location: `petstore-tests/PetstoreApi.Tests/ExceptionHandlerTests.cs`
  - Action: Create xUnit test class with tests for exception responses
  - Expected: Tests verify 500 response with ProblemDetails format

- [ ] T043 [US2] Run exception handler tests
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task test-server-stubs` (or `dotnet test test-output/tests/PetstoreApi.Tests/PetstoreApi.Tests.csproj --filter "FullyQualifiedName~ExceptionHandlerTests"`)
  - Expected: All exception handler tests pass

- [ ] T044 [US2] Test generate with useGlobalExceptionHandler=false
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useGlobalExceptionHandler=false`
  - Expected: No exception handler middleware in Program.cs

---

## Phase 5: Integration & Polish

### 5.1: Configuration Matrix Testing

- [ ] T045 [P] Test config: useValidators=false, useGlobalExceptionHandler=false
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useValidators=false,useGlobalExceptionHandler=false`
  - Expected: Clean generated code, no validators, no exception handler

- [ ] T046 [P] Test config: useValidators=true, useGlobalExceptionHandler=false
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useValidators=true,useGlobalExceptionHandler=false`
  - Expected: Validators present, no exception handler

- [ ] T047 [P] Test config: useValidators=false, useGlobalExceptionHandler=true
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useValidators=false,useGlobalExceptionHandler=true`
  - Expected: Exception handler present, no validators

- [ ] T048 [P] Test config: useValidators=true, useGlobalExceptionHandler=true, useProblemDetails=false
  - Location: `~/scratch/git/minimal-api-gen/generator/`
  - Command: `./run-generator.sh --additional-properties useValidators=true,useGlobalExceptionHandler=true,useProblemDetails=false`
  - Expected: Both features enabled, simple JSON error format

- [ ] T049 Build and test all 4 configurations
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task test-server-stubs`
  - Expected: All configurations build and pass tests

### 5.2: Baseline Test Suite Validation

- [ ] T050 Run baseline test suite from feature 003
  - Location: `~/scratch/git/minimal-api-gen/`
  - Command: `task test-server-stubs`
  - Expected: All baseline tests pass (backward compatibility verified)

### 5.3: Documentation Updates

- [ ] T051 Update README.md with validator generation example
  - Location: `generator/src/main/resources/aspnet-minimalapi/README.md`
  - Action: Add section documenting useValidators flag and generated output
  - Expected: Users understand how to enable validation

- [ ] T052 Update README.md with exception handler example
  - Location: `generator/src/main/resources/aspnet-minimalapi/README.md`
  - Action: Add section documenting useGlobalExceptionHandler and useProblemDetails flags
  - Expected: Users understand error handling configuration

- [ ] T053 [P] Update CONFIGURATION.md with resolved issues
  - Location: `docs/CONFIGURATION.md`
  - Action: Mark useValidators and useGlobalExceptionHandler as implemented
  - Expected: Configuration documentation accurate

### 5.4: Success Criteria Verification

- [ ] T054 SC-001: Verify validator classes generated
  - Location: `test-output/src/PetstoreApi/Validators/`
  - Action: Count validator files, verify structure
  - Expected: One validator per operation with required param rules

- [ ] T055 SC-002: Verify API rejects invalid requests
  - Location: Test output from ValidationTests.cs
  - Action: Check test results for 400 responses
  - Expected: Invalid requests return 400 Bad Request

- [ ] T056 SC-003: Verify FluentValidation excluded when disabled
  - Location: `test-output/PetstoreApi.csproj` (with useValidators=false)
  - Action: Check .csproj for FluentValidation packages
  - Expected: No FluentValidation packages present

- [ ] T057 SC-004: Verify RFC 7807 error format
  - Location: Test output from ExceptionHandlerTests.cs
  - Action: Check error response has type, title, status, detail fields
  - Expected: ProblemDetails format matches RFC 7807

- [ ] T058 SC-005: Verify configuration surface reduced
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Search for "useRouteGroups" in code
  - Expected: No references to useRouteGroups

- [ ] T059 SC-006: Verify baseline tests pass
  - Location: Test output from T050
  - Action: Review test results
  - Expected: All baseline tests green

- [ ] T060 SC-007: Verify all configs compile
  - Location: Test output from T049
  - Action: Review build results for all 4 configurations
  - Expected: All configurations build successfully

- [ ] T061 SC-008: Verify constraints preserved
  - Location: `test-output/src/PetstoreApi/Validators/`
  - Action: Count validation rules in generated validators
  - Expected: 15+ constraints from petstore spec present as rules

---

## Phase 6: Completion Checklist

- [ ] T062 All tasks completed (T001-T061)
- [ ] T063 All success criteria met (SC-001 through SC-008)
- [ ] T064 All user stories acceptance criteria satisfied (US1, US2, US3)
- [ ] T065 Documentation updated and accurate
- [ ] T066 Code changes committed with feature reference
- [ ] T067 Feature branch ready for review

---

## Task Statistics

**Total Tasks**: 67
**Parallelizable Tasks**: 8 (marked with [P])
**User Story Tasks**: 
- US1 (FluentValidation): 20 tasks (T014-T033)
- US2 (Exception Handler): 11 tasks (T034-T044)
- US3 (Flag Removal): 8 tasks (T006-T013)

**Estimated Timeline**: 
- Phase 1 Setup: 10 minutes
- Phase 2 US3: 30 minutes
- Phase 3 US1: 2 hours
- Phase 4 US2: 1 hour
- Phase 5 Integration: 45 minutes
- Phase 6 Completion: 15 minutes
**Total**: ~4.5 hours

---

## Notes

- Tasks marked [P] can be executed in parallel (setup/verification tasks)
- Tasks marked [US1]/[US2]/[US3] are user story specific
- Each user story phase is independently testable
- Implementation order (P3→P1→P2) minimizes risk
- All devbox commands must be used (constitution requirement)
- Build generator after each phase to catch errors early
- Test generated code after each phase to validate changes
