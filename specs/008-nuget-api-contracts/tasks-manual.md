## Phase 2.5: Testing Infrastructure

**Goal**: Establish testing framework before implementation begins

- [ ] T008 Create bruno:run-main-suite task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    bruno:run-main-suite:
      desc: "Run main pet test suite (6 tests - CRUD operations)"
      dir: bruno/OpenAPI_Petstore/pet/main-pet-test-suite
      cmds:
        - bru run --env local
    ```
  - Expected: Task defined, ready to use in integration tests

- [ ] T009 Create bruno:run-validation-suite task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    bruno:run-validation-suite:
      desc: "Run validation test suite (13 tests - FluentValidation)"
      dir: bruno/OpenAPI_Petstore/pet/validation-pet-test-suite
      cmds:
        - bru run --env local
    ```
  - Expected: Task defined for validator testing

- [ ] T010 Create bruno:run-all-suites task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    bruno:run-all-suites:
      desc: "Run both main + validation suites (19 tests total)"
      cmds:
        - task: bruno:run-main-suite
        - task: bruno:run-validation-suite
    ```
  - Expected: Combined test runner for full validation

- [ ] T011 Create integration test task template
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add comment template for integration tests:
    ```yaml
    # ==============================================================================
    # Integration Tests - Different Generator Parameter Combinations
    # ==============================================================================
    # Pattern: generate → run unit tests → build → start API → run Bruno → stop
    # Tests added incrementally as features are implemented
    ```
  - Expected: Structure ready for test tasks

- [ ] T012 Create GeneratedProjectStructureTests.cs skeleton
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/GeneratedProjectStructureTests.cs`
  - Action: Create file with namespace and empty class:
    ```csharp
    namespace MinimalApiGenerator.Tests;
    
    public class GeneratedProjectStructureTests
    {
        // Tests added incrementally as features are implemented
        // Categories: Baseline, WithValidators, NuGetPackaging
    }
    ```
  - Expected: Test file exists, ready for incremental tests

- [ ] T013 Create CsprojMetadataTests.cs skeleton
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/CsprojMetadataTests.cs`
  - Action: Create file with namespace and empty class
  - Expected: Test file exists, ready for incremental tests

- [ ] T014 Create ProjectReferenceTests.cs skeleton
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/ProjectReferenceTests.cs`
  - Action: Create file with namespace and empty class
  - Expected: Test file exists, ready for NuGet-specific tests

## Phase 3: User Story 1 - Package API Contracts for Distribution (P0)

**Story Goal**: Generate two .csproj files (Contracts for NuGet, Implementation for business logic)

**Test Strategy**: Implement feature → add unit tests → run integration test → iterate

### Phase 3.1-3.5: Implementation (T015-T045)
[Keep existing T008-T028 tasks, renumbered to T015-T032]

### Phase 3.6: Testing (T033-T052)

- [ ] T033 [US1] Build generator with NuGet packaging support
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task build-generator`
  - Expected: BUILD SUCCESS, no compilation errors

- [ ] T034 [US1] Generate test-output with useNugetPackaging=true
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,packageId=PetstoreApi.Contracts,packageVersion=1.0.0"`
  - Expected: Generation succeeds, dual-project structure created

[... keep existing T031-T039 validation tasks, renumbered to T035-T043 ...]

- [ ] T044 Copy test handlers to Implementation project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task copy-test-stubs`
  - Expected: Handlers copied from petstore-tests/TestHandlers/

- [ ] T045 Build Implementation project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/src/PetstoreApi/ --verbosity minimal`
  - Expected: Build succeeds, 0 errors, PetstoreApi.dll produced

### Phase 3.7: Unit Tests for NuGet Packaging

- [ ] T046 Add NuGet structure validation tests
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/GeneratedProjectStructureTests.cs`
  - Action: Add tests with `[Trait("Category", "NuGetPackaging")]`:
    - Test_NuGetMode_CreatesGeneratedDirectory()
    - Test_NuGetMode_CreatesModelsInGeneratedFolder()
    - Test_NuGetMode_CreatesEndpointsInGeneratedFolder()
    - Test_NuGetMode_CreatesContractsAndImplementationProjects()
  - Expected: 4 unit tests validating Generated/ directory structure

- [ ] T047 Add NuGet metadata validation tests
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/CsprojMetadataTests.cs`
  - Action: Add tests with `[Trait("Category", "NuGetPackaging")]`:
    - Test_ContractsCsproj_ContainsPackageId()
    - Test_ContractsCsproj_ContainsVersionProperty()
    - Test_ContractsCsproj_ContainsCompileIncludeToGenerated()
  - Expected: 3 unit tests validating .csproj MSBuild properties

- [ ] T048 Add project reference validation tests
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/ProjectReferenceTests.cs`
  - Action: Add tests with `[Trait("Category", "NuGetPackaging")]`:
    - Test_ImplementationCsproj_ReferencesContractsProject()
    - Test_SolutionFile_ContainsBothProjects()
    - Test_ProjectReferencePath_IsCorrectRelative()
  - Expected: 3 unit tests validating project relationships

### Phase 3.8: Integration Test

- [ ] T049 Create test:integration:with-nuget task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    test:integration:with-nuget:
      desc: "Integration test - NuGet packaging mode"
      cmds:
        - echo "🧪 Integration Test - NuGet Packaging"
        - task: build-generator
        - task: generate-petstore-minimal-api
          vars: {ADDITIONAL_PROPS: "useMediatr=true,useNugetPackaging=true"}
        - task: copy-test-stubs
        - dotnet test generator-tests/ --filter "Category=NuGetPackaging" --verbosity minimal
        - dotnet build test-output/ --verbosity minimal
        - task: api:start
        - defer: { task: api:stop }
        - task: api:wait
        - task: bruno:run-main-suite
        - echo "✅ NuGet integration test passed (6 tests)"
    ```
  - Expected: Complete integration test task for US1

- [ ] T050 **RUN test:integration:with-nuget**
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task test:integration:with-nuget`
  - Expected: Generate dual-project, run 10 unit tests (fast), 6 Bruno tests (runtime) - ALL PASS
  - **PURPOSE**: Validate US1 implementation immediately

- [ ] T051 Review test failures and fix issues
  - Action: If T050 fails, iterate on implementation (T015-T045)
  - Expected: All tests pass before proceeding to US2

- [ ] T052 Run dotnet pack on Contracts project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `mkdir -p packages && devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ --configuration Release --output ./packages/`
  - Expected: .nupkg file created in packages/ directory


## Phase 4: User Story 2 - Inject Services and Handlers from Host Application (P0)

**Story Goal**: Provide extension methods for clean DI registration

### Phase 4.1-4.3: Implementation (T053-T063)
[Keep existing T040-T047 tasks, renumbered to T053-T060]

### Phase 4.4: Unit Tests for Baseline Configuration

- [ ] T061 Add baseline structure validation tests
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/GeneratedProjectStructureTests.cs`
  - Action: Add tests with `[Trait("Category", "Baseline")]`:
    - Test_Baseline_GeneratesExtensionsFolder()
    - Test_Baseline_GeneratesEndpointExtensions()
    - Test_Baseline_DoesNotGenerateValidatorExtensions()
  - Expected: 3 unit tests validating baseline project structure

- [ ] T062 Add baseline metadata validation tests
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/CsprojMetadataTests.cs`
  - Action: Add tests with `[Trait("Category", "Baseline")]`:
    - Test_Baseline_ContainsMediatRPackage()
    - Test_Baseline_DoesNotContainFluentValidationPackage()
  - Expected: 2 unit tests validating baseline .csproj dependencies

### Phase 4.5: Integration Test

- [ ] T063 Create test:integration:baseline task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task following same pattern as T049
  - Expected: Integration test for baseline configuration

- [ ] T064 Update Program.cs to call extension methods
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi/Program.cs`
  - Action: Manually add calls to AddApiHandlers(), AddApiEndpoints()
  - Expected: Program.cs configured for DI

- [ ] T065 **RUN test:integration:baseline**
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task test:integration:baseline`
  - Expected: Generate baseline, run 5 unit tests, 6 Bruno tests - ALL PASS
  - **PURPOSE**: Validate US2 implementation immediately

- [ ] T066 Verify validators are NOT invoked (baseline has no validators)
  - Action: Send invalid request via Bruno, expect it to succeed (no validation)
  - Expected: 200 OK even for invalid data (baseline behavior)

## Phase 6: User Story 4 - Configure Package Metadata (P2)

### Phase 6.1-6.3: Implementation (T085-T100)
[Keep existing tasks]

### Phase 6.4: Validator Unit Tests

- [ ] T101 Add validator-specific tests
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/GeneratedProjectStructureTests.cs`
  - Action: Add tests with `[Trait("Category", "WithValidators")]`:
    - Test_WithValidators_GeneratesValidatorExtensions()
    - Test_WithValidators_GeneratesValidatorFiles()
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/CsprojMetadataTests.cs`
  - Action: Add test:
    - Test_WithValidators_ContainsFluentValidationPackage()
  - Expected: 3 unit tests validating validator generation

### Phase 6.5: Integration Test with Validators

- [ ] T102 Create test:integration:with-validators task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    test:integration:with-validators:
      desc: "Integration test - with FluentValidation enabled"
      cmds:
        - echo "🧪 Integration Test - With Validators"
        - task: build-generator
        - task: generate-petstore-minimal-api
          vars: {ADDITIONAL_PROPS: "useMediatr=true,useValidators=true"}
        - task: copy-test-stubs
        - dotnet test generator-tests/ --filter "Category=WithValidators" --verbosity minimal
        - dotnet build test-output/ --verbosity minimal
        - task: api:start
        - defer: { task: api:stop }
        - task: api:wait
        - task: bruno:run-all-suites
        - echo "✅ Validator integration test passed (19 tests)"
    ```
  - Expected: Complete integration test for validators

- [ ] **RUN test:integration:with-validators**
  - Expected: 3 unit tests + 19 Bruno tests - ALL PASS

## Phase 8: Polish & Integration

### Phase 8.1: Taskfile Integration
[T115-T119: Keep existing Taskfile tasks for pack-nuget, etc.]

### Phase 8.2: Documentation Updates
[Keep existing T113-T116 doc updates]

### Phase 8.3: Regression Testing

- [ ] T120 Create test:integration:all task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    test:integration:all:
      desc: "Run all integration tests sequentially (regression check)"
      cmds:
        - task: test:integration:baseline
        - task: test:integration:with-validators
        - task: test:integration:with-nuget
    ```
  - Expected: Full regression test suite

- [ ] T121 **RUN test:integration:all** (Final regression check)
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task test:integration:all`
  - Expected: All 3 integration suites pass (baseline: 6 Bruno tests, validators: 19 Bruno tests, nuget: 6 Bruno tests)
  - **PURPOSE**: Confirm all features still work together

### Phase 8.4: Final Metrics

- [ ] T122 Verify package size under 500KB
  - Location: `/Users/adam/scratch/git/minimal-api-gen/packages/`
  - Command: `ls -lh *.nupkg`
  - Expected: Size < 500KB (SC-002)

- [ ] T123 Verify build time under 10 seconds
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `time devbox run dotnet build test-output/`
  - Expected: Total time < 10 seconds (SC-009)

- [ ] T124 Create feature completion checklist
  - Location: `/Users/adam/scratch/git/minimal-api-gen/specs/008-nuget-api-contracts/COMPLETION_CHECKLIST.md`
  - Action: Document all success criteria from spec.md with pass/fail status
  - Expected: Comprehensive validation checklist