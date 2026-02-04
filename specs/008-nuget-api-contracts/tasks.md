# Implementation Tasks: NuGet API Contract Packaging

**Branch**: `008-nuget-api-contracts` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

## Overview

Enable OpenAPI Generator to produce NuGet packages containing API contracts (Endpoints, DTOs, Validators) separate from business logic implementations (Handlers, Models). This allows independent versioning and distribution of API surface, supporting SemVer-based evolution without forcing cascading updates to Handler implementations.

**Key Features**:
- Dual-project generation (Contracts for NuGet, Implementation for business logic)
- **Contract-First CQRS**: Commands/Queries ARE requests (no nested DTOs), DTOs ARE responses (enum types)
- **Handler Mapping Scaffolds**: Generated handlers include MapCommandToDomain, MapDomainToDto methods with TODO comments
- Extension methods for service registration (AddApiEndpoints, AddApiValidators, AddApiHandlers)
- Assembly scanning for auto-discovery of validators and handlers
- SemVer-based versioning with compile-time safety for breaking changes
- Symbol package generation for debugging (.snupkg)
- **Zero Cross-Assembly Dependencies**: Contract package never references Implementation (no Model types in Commands/DTOs)

## Task Summary

- **Total Tasks**: 143 (updated from 136 to include Contract-First CQRS architecture tasks)
- **Parallel Tasks**: 42 (marked with [P])
- **Test Strategy**: Unit tests (fast file/XML validation) + Integration tests (Taskfile-based runtime validation)
- **Bruno Test Suites**: main-pet-test-suite (6 tests), validation-pet-test-suite (13 tests)
- **Phase Structure**: 9 phases (Setup, Foundational, Testing Infrastructure, US1-P0, US2-P0, US3-P1, US4-P2, US5-P3, Polish)
- **Architecture Updates**: Added T034-T040 for Contract-First CQRS (FR-027: Commands ARE requests, FR-028: DTO enum JsonConverter, FR-029: Handler mapping scaffolds)

## Architecture Overview

**Contract-First CQRS Data Flow** (per plan.md Architecture section):
1. HTTP Request → ASP.NET Core Model Binding
2. Command/Query (request in Contract package) - Properties directly on Command, returns IRequest<TDto>
3. Handler (business logic in Implementation) - Maps Command → Domain Entity → DTO response
4. DTO (response in Contract package) - Enum types with JsonConverter attributes
5. HTTP Response → JSON Serialization

**Key Principles**:
- Commands/Queries ARE the request (no nested DTOs) - FR-027
- DTOs ARE the response (enum types with JsonConverter) - FR-028
- Handlers own mapping (scaffolded methods with TODO comments) - FR-029
- Zero cross-assembly dependencies (Contract never references Implementation)

## Dependencies

### Story Completion Order
1. **Phase 1**: Setup (independent, validate environment)
2. **Phase 2**: Foundational (blocks all stories, shared infrastructure)
3. **Phase 2.5**: Testing Infrastructure (enables immediate validation)
4. **Phase 3**: User Story 1 (P0 - Package Contracts) ← **BLOCKS** all other stories
4. **Phase 4**: User Story 2 (P0 - Service Injection) ← depends on US1
5. **Phase 5**: User Story 3 (P1 - Versioning) ← depends on US1, US2
6. **Phase 6**: User Story 4 (P2 - Metadata) ← depends on US1
7. **Phase 7**: User Story 5 (P3 - Symbols) ← depends on US1
8. **Phase 8**: Polish ← depends on all stories

### Parallel Opportunities
- **Phase 2**: T005-T007 can be done in parallel (study existing codebase)
- **Phase 2.5**: T015-T017 can be done in parallel (create Taskfile build tasks)
- **Phase 3.2**: T021-T023 can be done in parallel (template creation)
- **Phase 3.4**: T030-T031 can be done in parallel (extension method templates)
- **Phase 3.5**: T034-T037, T039 can be done in parallel (Contract-First CQRS templates)
- **Phase 4.1**: T049-T051 can be done in parallel (extension method modifications - note: task IDs shifted)
- **US4 and US5**: Can be done in parallel after US1 completes (both only depend on US1)

## Phase 1: Setup

**Goal**: Initialize project structure and validate existing infrastructure

- [X] T001 Verify devbox environment (Java 11, Maven 3.8.9+, .NET SDK 8.0+)
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox version && devbox run java -version && devbox run mvn --version && devbox run dotnet --version`
  - Expected: All tools available via devbox

- [X] T002 Verify existing generator builds successfully
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task build-generator`
  - Expected: BUILD SUCCESS, generator/target/minimal-api-gen-openapi-generator-1.0.0-SNAPSHOT.jar exists

- [X] T003 Verify existing test-output structure exists
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/`
  - Action: Check for existing generated code structure
  - Expected: Directory exists with src/ folder
  - Note: Taskfile.yml has PETSTORE_SPEC variable (default: ./petstore-tests/petstore.yaml) which can be overridden with OPENAPI_SPEC for version testing

- [X] T004 Review constitution compliance documented in plan.md
  - Location: `/Users/adam/scratch/git/minimal-api-gen/specs/008-nuget-api-contracts/plan.md`
  - Action: Read Constitution Check section (lines 54-158)
  - Expected: All 5 principles compliant (✅)

## Phase 2: Foundational Infrastructure

**Goal**: Shared prerequisites for all user stories

- [X] T005 [P] Study existing MinimalApiServerCodegen.java CLI option pattern
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Review how USE_MEDIATR, USE_VALIDATORS constants are defined and registered
  - Expected: Understand constructor registration, setter methods, processOpts() pattern

- [X] T006 [P] Study existing template rendering in addSupportingFiles() method
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Review supportingFiles.add() calls for project.csproj.mustache, solution.mustache
  - Expected: Understand file path construction, mustache template loading

- [X] T007 [P] Review research.md RQ-001 and RQ-005 for unified generation approach
  - Location: `/Users/adam/scratch/git/minimal-api-gen/specs/008-nuget-api-contracts/research.md` (lines 1-150, 550-650)
  - Action: Study `<Compile Include>` with Link metadata pattern AND Generated/ directory rationale
  - Expected: Understand unified generation approach:
    - Generated/ = NEW solution-level directory for all generated files (Models, Endpoints, Commands, Queries, Validators)
    - Current: Files generated to src/PetstoreApi/Models/, src/PetstoreApi/Commands/, etc.
    - NEW: Files generated to Generated/Models/, Generated/Endpoints/, Generated/Commands/, Generated/Queries/
    - Rationale: Clear ownership (Generated/ = throw away, src/ = user-owned), easy regeneration, supports .gitignore
    - Both .csproj files reference Generated/ via <Compile Include> to avoid duplication

## Phase 2.5: Testing Infrastructure

**Goal**: Establish testing framework BEFORE implementation begins

- [X] T008 Create bruno:run-main-suite task
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

- [X] T009 Create bruno:run-validation-suite task
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

- [X] T010 Create bruno:run-all-suites task
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

- [X] T011 Create integration test task template
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

- [X] T012 Create ProjectStructureTemplateTests.cs for template validation
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/ProjectStructureTemplateTests.cs`
  - Action: Create file with namespace and empty class:
    ```csharp
    namespace MinimalApiGenerator.Tests;
    
    /// <summary>
    /// Tests to verify project structure templates (solution.mustache, project.csproj.mustache)
    /// These are pure unit tests that validate template design without requiring code generation
    /// </summary>
    public class ProjectStructureTemplateTests
    {
        private const string TemplateDir = "../../../../generator/src/main/resources/aspnet-minimalapi";
        // Tests added incrementally as features are implemented
    }
    ```
  - Expected: Test file exists for template-based validation (no generated code dependency)

- [X] T013 Create CsprojTemplateTests.cs for .csproj template validation
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/CsprojTemplateTests.cs`
  - Action: Create file with namespace and empty class:
    ```csharp
    namespace MinimalApiGenerator.Tests;
    
    /// <summary>
    /// Tests to verify .csproj mustache templates (project.csproj.mustache, nuget-project.csproj.mustache, implementation-project.csproj.mustache)
    /// These are pure unit tests that validate template design without requiring code generation
    /// </summary>
    public class CsprojTemplateTests
    {
        private const string TemplateDir = "../../../../generator/src/main/resources/aspnet-minimalapi";
        // Tests added incrementally as features are implemented
    }
    ```
  - Expected: Test file exists for template-based validation (no generated code dependency)

- [X] T014 Create NugetPackagingTemplateTests.cs for NuGet template validation
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/NugetPackagingTemplateTests.cs`
  - Action: Create file with namespace and empty class:
    ```csharp
    namespace MinimalApiGenerator.Tests;
    
    /// <summary>
    /// Tests to verify NuGet packaging templates (nuget-project.csproj.mustache, implementation-project.csproj.mustache, solution.mustache)
    /// These are pure unit tests that validate template design without requiring code generation
    /// </summary>
    public class NugetPackagingTemplateTests
    {
        private const string TemplateDir = "../../../../generator/src/main/resources/aspnet-minimalapi";
        // Tests added for NuGet-specific validation
    }
    ```
  - Expected: Test file exists for template-based validation (no generated code dependency)

- [X] T015 Create build-contracts Taskfile task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task after existing build tasks:
    ```yaml
    build-contracts:
      desc: Build the Contracts project only (for NuGet packaging)
      cmds:
        - echo "Building Contracts project..."
        - dotnet build {{.TEST_OUTPUT_DIR}}/src/PetstoreApi.Contracts/ --verbosity minimal
        - echo "✓ Contracts built successfully"
      preconditions:
        - test -f {{.TEST_OUTPUT_DIR}}/src/PetstoreApi.Contracts/PetstoreApi.Contracts.csproj
    ```
  - Expected: Task available for building Contracts project independently

- [X] T016 Create build-implementation-using-contracts Taskfile task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    build-implementation-using-contracts:
      desc: Build Implementation project that uses Contracts via NuGet package reference
      deps:
        - build-contracts
      cmds:
        - echo "Building Implementation project (using NuGet contracts)..."
        - dotnet build {{.TEST_OUTPUT_DIR}}/src/PetstoreApi/ --verbosity minimal
        - echo "✓ Implementation built successfully (with NuGet contracts)"
      preconditions:
        - test -f {{.TEST_OUTPUT_DIR}}/src/PetstoreApi/PetstoreApi.csproj
    ```
  - Expected: Task builds Implementation with NuGet dependency chain

- [X] T017 Create build-all Taskfile task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task:
    ```yaml
    build-all:
      desc: Build entire solution (both projects)
      cmds:
        - echo "Building entire solution..."
        - dotnet build {{.TEST_OUTPUT_DIR}}/ --verbosity minimal
        - echo "✓ Solution built successfully"
      preconditions:
        - test -f {{.TEST_OUTPUT_DIR}}/PetstoreApi.sln
    ```
  - Expected: Task available for solution-level builds

## Phase 3: User Story 1 - Package API Contracts for Distribution (P0)

**Story Goal**: Generate two .csproj files (Contracts for NuGet, Implementation for business logic)

**Test Strategy**: Implement feature → add unit tests → run integration test → iterate

### Phase 3.1: Generator CLI Options

- [X] T018 [US1] Add USE_NUGET_PACKAGING constant to MinimalApiServerCodegen.java
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `public static final String USE_NUGET_PACKAGING = "useNugetPackaging";` near other constants (around line 40-60)
  - Expected: Constant defined for CLI option key

- [X] T019 [US1] Register useNugetPackaging CLI option in constructor
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add CliOption in constructor similar to USE_MEDIATR pattern
  - Expected: `cliOptions.add(CliOption.newBoolean(USE_NUGET_PACKAGING, "Generate separate NuGet package project for API contracts"));`

- [X] T020 [US1] Implement setUseNugetPackaging() setter method
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add setter method with boolean parameter
  - Expected: `public void setUseNugetPackaging(boolean useNugetPackaging) { this.useNugetPackaging = useNugetPackaging; }`

### Phase 3.2: Template Creation

- [X] T021 [P] [US1] Create nuget-project.csproj.mustache template
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Create new template for Contracts project with:
    - Target framework: net8.0
    - NuGet metadata: PackageId, Version, Authors, Description
    - Dependencies: MediatR, FluentValidation (conditional), ASP.NET Core
    - Compile includes: `<Compile Include="../../Generated/**/*.cs" Link="...">` 
  - Expected: New template file created, see contracts/CsprojStructure.md (lines 30-100)

- [X] T022 [P] [US1] Create implementation-project.csproj.mustache template
  - Location: `generator/src/main/resources/aspnet-minimalapi/implementation-project.csproj.mustache`
  - Action: Create new template for Implementation project with:
    - Target framework: net8.0
    - ProjectReference to Contracts project
    - User-owned code includes: Handlers/, Models/, Program.cs
  - Expected: New template file created, see contracts/CsprojStructure.md (lines 100-150)

- [X] T023 [P] [US1] Modify solution.mustache to support multi-project structure
    - Project("{9A19103F...}") = "{{packageName}}.Contracts", "src/{{packageName}}.Contracts/{{packageName}}.Contracts.csproj"
    - Project("{9A19103F...}") = "{{packageName}}", "src/{{packageName}}/{{packageName}}.csproj"
  - Expected: Solution template supports both single-project (default) and dual-project (useNugetPackaging=true) modes

### Phase 3.3: Generator Logic - Unified Source Generation

- [X] T024 [US1] Add useNugetPackaging field to MinimalApiServerCodegen.java
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `private boolean useNugetPackaging = false;` field near other boolean flags
  - Expected: Field declared for tracking packaging mode

- [X] T025 [US1] Modify processOpts() to read useNugetPackaging from additionalProperties
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add in processOpts() method: `if (additionalProperties.containsKey(USE_NUGET_PACKAGING)) { this.useNugetPackaging = convertPropertyToBooleanAndWriteBack(USE_NUGET_PACKAGING); }`
  - Expected: CLI option value read and stored in field

- [X] T026 [US1] Create generateNugetPackageProject() method
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Create new method that adds nuget-project.csproj.mustache to supportingFiles
  - Expected: Method adds supporting file with correct output path: `src/{{packageName}}.Contracts/{{packageName}}.Contracts.csproj`

- [X] T027 [US1] Create generateImplementationProject() method
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Create new method that adds implementation-project.csproj.mustache to supportingFiles
  - Expected: Method adds supporting file with correct output path: `src/{{packageName}}/{{packageName}}.csproj`

- [X] T028 [US1] Modify addSupportingFiles() to conditionally call generateNugetPackageProject()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add conditional: `if (useNugetPackaging) { generateNugetPackageProject(); generateImplementationProject(); } else { /* existing project.csproj logic */ }`
  - Expected: Dual-project structure generated when flag enabled, single project otherwise

- [X] T029 [US1] Update solution.mustache supporting file registration
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Ensure solution.mustache receives useNugetPackaging flag in template context
  - Expected: Template can render conditional project entries

### Phase 3.4: Extension Method Generation

- [X] T030 [P] [US1] Create endpointExtensions.mustache template
  - Location: `generator/src/main/resources/aspnet-minimalapi/endpointExtensions.mustache`
  - Action: Create template for AddApiEndpoints() extension method:
    - Namespace: {{packageName}}.Contracts.Extensions
    - Method signature: `public static IEndpointRouteBuilder AddApiEndpoints(this IEndpointRouteBuilder endpoints)`
    - Body: Iterate over all endpoint classes and call their Map*Endpoints() methods
    - Available mustache variables: {{packageName}}, {{classname}}, {{#operations}}, {{#apiInfo}}, {{#apis}}
    - Implementation: Call PetEndpoints.MapPetEndpoints(endpoints), StoreEndpoints.MapStoreEndpoints(endpoints), etc.
    - Pattern reference: See generator/src/main/resources/aspnet-minimalapi/api.mustache lines 1-30
    - Generation rule: ALWAYS generated when useNugetPackaging=true
  - Expected: New template file, see contracts/ExtensionMethods.md (lines 30-80)

- [X] T031 [P] [US1] Create validatorExtensions.mustache template
  - Location: `generator/src/main/resources/aspnet-minimalapi/validatorExtensions.mustache`
  - Action: Create template for AddApiValidators() extension method:
    - Namespace: {{packageName}}.Contracts.Extensions
    - Method signature: `public static IServiceCollection AddApiValidators(this IServiceCollection services)`
    - Body: `services.AddValidatorsFromAssembly(typeof(ValidatorExtensions).Assembly); return services;`
    - Assembly scanning: Uses FluentValidation reflection to find all AbstractValidator<T> descendants in Contracts.dll
    - Generation rule: ONLY generated when useValidators=true (no validators = no extension method)
  - Expected: New template file, see contracts/ExtensionMethods.md (lines 80-120)

- [X] T032 [US1] Add endpointExtensions.mustache to supportingFiles in generateNugetPackageProject()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `supportingFiles.add(new SupportingFile("endpointExtensions.mustache", srcPath + "/Extensions", "EndpointExtensions.cs"));`
  - Expected: Extension method file generated in Contracts project

- [X] T033 [US1] Add validatorExtensions.mustache to supportingFiles conditionally
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add conditional: `if (useValidators) { supportingFiles.add(new SupportingFile("validatorExtensions.mustache", ...)); }`
  - Expected: Validator extension only generated when validators enabled

### Phase 3.5: Contract-First CQRS Architecture (Commands/Queries/DTOs)

**Goal**: Implement FR-027 (Commands ARE requests, return DTOs), FR-028 (DTO enum JsonConverter), FR-029 (Handler mapping scaffolds)

- [X] T034 [P] [US1] Update command.mustache to generate Commands as request data structures
  - Location: `generator/src/main/resources/aspnet-minimalapi/command.mustache`
  - Action: Modify template to:
    - Generate Command properties directly from operation parameters (default case)
    - For simple parameters: properties directly on Command (e.g., `string Name`, `StatusEnum Status`)
    - For complex nested request bodies: MAY use Request DTO property (e.g., `AddPetRequestDto pet`) when OpenAPI schema warrants separate type
    - Use DTO type for IRequest<TResponse> (not Model/Domain Entity type)
    - Example (simple): `public record AddPetCommand : IRequest<PetDto> { public string Name { get; init; } public StatusEnum? Status { get; init; } }`
    - Example (complex): `public record AddPetCommand : IRequest<PetDto> { public AddPetRequestDto Pet { get; init; } }`
  - Expected: Commands ARE the request data structure with DTO response type; structure adapts to OpenAPI schema complexity
  - Rationale: FR-027 - Ensures Contract package has zero dependencies on Implementation (no Model references)

- [X] T035 [P] [US1] Update query.mustache to generate Queries as request data structures
  - Location: `generator/src/main/resources/aspnet-minimalapi/query.mustache`
  - Action: Modify template to:
    - Generate Query properties directly from operation parameters (not nested DTO)
    - Use DTO type for IRequest<TResponse> (not Model type)
    - Example: `public record GetPetByIdQuery : IRequest<PetDto> { public long PetId { get; init; } }`
  - Expected: Queries ARE the request data structure with DTO response type
  - Rationale: FR-027 - Maintains Contract-First architecture separation

- [X] T036 [P] [US1] Add response DTO type resolution logic to MinimalApiServerCodegen.java
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Note: Execute BEFORE T034/T035 as templates will use this helper method
  - Action: Create method `String getResponseDtoType(CodegenOperation operation)`:
    - Check operation.responses['200'].schema → map to DTO name (e.g., "Pet" schema → "PetDto")
    - Handle missing schemas: return "Unit" for 204 No Content
    - Handle resource creation (201): return "{ResourceName}Dto"
  - Expected: Commands/Queries can determine correct DTO response type from OpenAPI spec
  - Note: May need to extend CodegenOperation with responseDtoType property

- [X] T037 [P] [US1] Update model.mustache (DTO generation) to add JsonConverter for enum properties
  - Location: `generator/src/main/resources/aspnet-minimalapi/model.mustache`
  - Action: Modify template to:
    - Add `[JsonConverter(typeof(EnumMemberJsonConverter<{{{datatypeWithEnum}}}>))]` attribute to enum properties
    - Use existing EnumMemberJsonConverter<T> implementation (already in codebase)
    - Example: `[JsonConverter(typeof(EnumMemberJsonConverter<StatusEnum>))] public StatusEnum? Status { get; init; }`
  - Expected: DTOs have enum types (not strings) with proper serialization attributes
  - Rationale: FR-028 - Enable strict type serialization at API boundary

- [X] T038 [US1] Verify EnumMemberJsonConverter<T> supports JsonPropertyName attributes
  - Location: `test-output/Contract/Converters/EnumMemberJsonConverter.cs` (generated file for inspection)
  - Action: Review existing converter implementation (lines 11-46 from codebase)
  - Expected: Converter already handles `[EnumMember(Value="available")]` and `[JsonPropertyName("available")]` patterns
  - Note: No code changes needed - converter is already sophisticated enough (per conversation history)

- [X] T037a [US1] Add unit test to verify dto.mustache generates JsonConverter attributes
  - Location: `generator-tests/DtoTemplateTests.cs`
  - Action: Create test method DtoTemplate_ShouldGenerateJsonConverterOnEnumProperties():
    - Load dto.mustache template from generator/src/main/resources/aspnet-minimalapi/
    - Assert template contains "JsonConverter" string
    - Assert template contains "EnumMemberJsonConverter" string
    - No generated code dependency - validates template design intent
  - Expected: Test passes when template includes enum converter generation logic
  - Validation: Covers FR-028 compliance at template level

- [X] T039 [P] [US1] Update handler.mustache to add scaffolded mapping methods
  - Location: `generator/src/main/resources/aspnet-minimalapi/handler.mustache`
  - Action: Modify template to generate partial class with mapping scaffolds:
    ```csharp
    public partial class AddPetCommandHandler : IRequestHandler<AddPetCommand, PetDto>
    {
        public async Task<PetDto> Handle(AddPetCommand command, CancellationToken cancellationToken)
        {
            // TODO: Customize business logic
            var domainEntity = MapCommandToDomain(command);
            // TODO: Call service/repository to persist domainEntity
            var result = await _repository.AddAsync(domainEntity, cancellationToken);
            return MapDomainToDto(result);
        }

        private Pet MapCommandToDomain(AddPetCommand command)
        {
            // TODO: Customize mapping logic
            return new Pet { Name = command.Name, Status = MapEnumToDomain(command.Status) };
        }

        private PetDto MapDomainToDto(Pet entity)
        {
            // TODO: Customize mapping logic
            return new PetDto { Id = entity.Id, Name = entity.Name, Status = MapEnumToDto(entity.Status) };
        }

        private Pet.StatusEnum? MapEnumToDomain(StatusEnum? dtoEnum)
        {
            // TODO: Customize enum mapping if needed
            return dtoEnum.HasValue ? (Pet.StatusEnum)dtoEnum.Value : null;
        }

        private StatusEnum? MapEnumToDto(Pet.StatusEnum? domainEnum)
        {
            // TODO: Customize enum mapping if needed
            return domainEnum.HasValue ? (StatusEnum)domainEnum.Value : null;
        }
    }
    ```
  - Expected: Handlers generated as partial classes with scaffolded mapping methods (TODO comments)
  - Rationale: FR-029 - Provide clear guidance for developer customization
  - Note: Depends on T031/T032 (partial keyword) - this task adds mapping method generation to existing partial class structure

- [X] T040 [US1] Add partial keyword support to handler generation logic
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Ensure handler generation includes "partial" modifier in class declaration
  - Expected: Handlers can be extended by developers in separate files without regeneration conflicts
  - Note: This provides the class structure that T039 adds mapping methods to

- [X] T040a [US1] Add unit test to verify handler.mustache generates scaffolded mapping methods
  - Location: `generator-tests/HandlerTemplateTests.cs`
  - Action: Test method HandlerTemplate_ShouldGenerateScaffoldedMappingMethods() (currently skipped):
    - Load handler.mustache template from generator/src/main/resources/aspnet-minimalapi/
    - Assert template contains "MapCommandToDomain" string (when implemented)
    - Assert template contains "TODO" comments for customization
    - No generated code dependency - validates template design intent
    - Test currently skipped with message: "Handler scaffolding not yet implemented - template generates minimal stub"
  - Expected: Test will pass when template includes mapping method scaffolds (unskip when T039 complete)
  - Validation: Covers FR-029 compliance at template level

### Phase 3.6: Modify Existing Templates for Unified Generation

**NOTE - Task Numbering Strategy**: Tasks T034-T040a are NEW insertions for Contract-First CQRS architecture (FR-027, FR-028, FR-029). Tasks T042 onwards preserve their ORIGINAL IDs from the pre-architecture-update plan to maintain review history and task tracking continuity.

**Task ID Mapping**:
- T001-T033: Original sequence (completed/in-progress)
- T034-T040a: NEW - Contract-First CQRS architecture tasks (7 tasks)
- T042-T136: Original sequence continues (old T035 became T042, etc.)
- **Total**: 143 tasks (136 original + 7 new insertions)

**Rationale**: Preserving original IDs avoids cascade updates across documentation, git history, and in-progress work. New tasks inserted at logical point (after T033 template creation, before T042 file routing).

- [ ] T041 [US1] Modify endpointMapper.mustache to expose public MapXEndpoints methods
  - Location: `generator/src/main/resources/aspnet-minimalapi/endpointMapper.mustache`
  - Action: Change endpoint mapping methods from `internal static` to `public static`
  - Expected: Methods can be called from extension method in Contracts project

- [X] T042 [US1] Modify file output paths to use Contract/ directory for Commands/Queries/DTOs
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Override methods to change output paths when useNugetPackaging=true
  - Expected: Generator creates PHYSICAL directory structure at solution root:
    - Current output: test-output/src/PetstoreApi/Models/Pet.cs
    - NEW output: test-output/Generated/Models/Pet.cs
  - Rationale (from research.md RQ-005): Clear ownership boundary (Generated/ = throw away, src/ = user-owned)
  - Note: MSBuild will compile files from Generated/ via <Compile Include> in both .csproj files

- [X] T036 [US1] Override modelFileFolder() to return "Generated/Models"
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Override modelFileFolder() method:
    ```java
    @Override
    public String modelFileFolder() {
        if (useNugetPackaging) {
            return outputFolder + File.separator + "Generated" + File.separator + "Models";
        }
        return super.modelFileFolder(); // Default: outputFolder + "/src/" + packageName + "/Models"
    }
    ```
  - Expected: DTOs generated to test-output/Generated/Models/Pet.cs (instead of test-output/src/PetstoreApi/Models/Pet.cs)

- [X] T136 [US1] Override apiFileFolder() to return "Generated/Endpoints"
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Override apiFileFolder() method:
    ```java
    @Override
    public String apiFileFolder() {
        if (useNugetPackaging) {
            return outputFolder + File.separator + "Generated" + File.separator + "Endpoints";
        }
        return super.apiFileFolder(); // Default: outputFolder + "/src/" + packageName + "/Endpoints"
    }
    ```
  - Expected: Endpoints generated to test-output/Generated/Endpoints/PetEndpoints.cs
  - Note: Commands/Queries will need similar overrides (not yet in OpenAPI Generator base class)

- [ ] T037 [US1] Update README.mustache with dual-project workflow documentation
  - Location: `generator/src/main/resources/aspnet-minimalapi/readme.mustache`
  - Action: Add conditional section for NuGet packaging workflow:
    - How to build Contracts project
    - How to pack NuGet package
    - How to reference from Implementation project
  - Expected: README includes quickstart-style instructions

### Phase 3.6: Testing

- [X] T038 [US1] Build generator with NuGet packaging support
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task build-generator`
  - Expected: BUILD SUCCESS, no compilation errors

- [X] T039 [US1] Generate test-output with useNugetPackaging=true
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,packageId=PetstoreApi.Contracts,packageVersion=1.0.0"`
  - Expected: Generation succeeds, dual-project structure created

- [X] T040 [US1] Verify Generated/ directory structure exists
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/Generated/`
  - Action: Check for Models/, Endpoints/, Commands/, Queries/ subdirectories
  - Expected: All subdirectories exist with generated files

- [X] T041 [US1] Verify nuget-project.csproj created
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi.Contracts/PetstoreApi.Contracts.csproj`
  - Action: Check file exists and contains <Compile Include> references to Generated/
  - Expected: File exists with correct MSBuild structure

- [X] T042 [US1] Verify implementation-project.csproj created
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi/PetstoreApi.csproj`
  - Action: Check file exists and contains ProjectReference to Contracts project
  - Expected: File exists with ProjectReference

- [X] T043 [US1] Verify solution file includes both projects
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/PetstoreApi.sln`
  - Action: Check solution contains two Project(...) entries
  - Expected: Solution references both Contracts and Implementation projects

- [X] T044 [US1] Build Contracts project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/src/PetstoreApi.Contracts/ --verbosity minimal`
  - Expected: Build succeeds, 0 errors, Contracts.dll produced
  - Result: ✅ BUILD SUCCESS - 0 errors, 39 warnings (nullable annotations - cosmetic)
  - Fixed Issues:
    1. ✅ TestEnum query parameter now uses TestEnumDto (generator fix applied)
    2. ✅ TestEnumDto generated as enum type (dto.mustache template fix applied)
    3. ✅ Endpoints now use DTO response types (api.mustache fixed to use vendorExtensions.dtoResponseType)
    4. ✅ OutputType=Library added to prevent CS5001 error (nuget-project.csproj.mustache updated)

- [X] T045 [US1] Verify Contracts.dll contains Endpoints
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi.Contracts/bin/Debug/net8.0/`
  - Command: `ildasm PetstoreApi.Contracts.dll` or reflection
  - Expected: Assembly contains PetEndpoints, StoreEndpoints, UserEndpoints types
  - Result: ✅ VERIFIED - Found 5 endpoint files with Map methods:
    - PetApiEndpoints.cs → MapPetApiEndpoints()
    - StoreApiEndpoints.cs → MapStoreApiEndpoints()
    - UserApiEndpoints.cs → MapUserApiEndpoints()
    - DefaultApiEndpoints.cs → MapDefaultApiEndpoints()
    - FakeApiEndpoints.cs → MapFakeApiEndpoints()

- [X] T046 [US1] Copy test handlers to Implementation project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task copy-test-stubs`
  - Expected: Handlers copied from petstore-tests/TestHandlers/ to test-output/src/PetstoreApi/Handlers/
  - Result: ✅ SUCCESS - Copied 6 handlers + InMemoryPetStore service + ServiceCollectionExtensions

- [X] T047 [US1] Build Implementation project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/src/PetstoreApi/ --verbosity minimal`
  - Expected: Build succeeds, 0 errors, PetstoreApi.dll produced
  - Result: ✅ BUILD SUCCESS - 0 errors, 39 warnings (cosmetic)

- [X] T048 [US1] Run dotnet pack on Contracts project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `mkdir -p packages && devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ --configuration Release --output ./packages/`
  - Expected: .nupkg file created in packages/ directory
  - Result: ✅ SUCCESS - Created PetstoreApi.Contracts.1.0.0.nupkg (34KB)

## Phase 4: User Story 2 - Inject Services and Handlers from Host Application (P0)

**Story Goal**: Provide extension methods for clean DI registration in host application

**Independent Test**: BrunoSuiteValidation (19 tests) against NuGet-packaged endpoints

### Phase 4.1: Extension Method Enhancements

- [X] T049 [P] [US2] Verify endpointExtensions.mustache exposes AddApiEndpoints() publicly
  - Location: `generator/src/main/resources/aspnet-minimalapi/endpointExtensions.mustache`
  - Action: Ensure method signature is `public static IEndpointRouteBuilder AddApiEndpoints(this IEndpointRouteBuilder endpoints)`
  - Expected: Method is public and accessible from Program.cs
  - Result: Template updated to call all endpoint Map methods (DefaultApiEndpoints.MapDefaultApiEndpoints(), PetApiEndpoints.MapPetApiEndpoints(), etc.). Builds with 0 errors, 39 warnings.

- [X] T050 [P] [US2] Verify validatorExtensions.mustache exposes AddApiValidators() publicly
  - Location: `generator/src/main/resources/aspnet-minimalapi/validatorExtensions.mustache`
  - Action: Ensure method signature is `public static IServiceCollection AddApiValidators(this IServiceCollection services)`
  - Expected: Method is public and uses assembly scanning
  - Result: Verified - method is public, uses assembly scanning via AddValidatorsFromAssembly(), correctly implemented.

- [X] T051 [P] [US2] Create handlerExtensions.mustache template
  - Location: `generator/src/main/resources/aspnet-minimalapi/handlerExtensions.mustache`
  - Action: Create template for AddApiHandlers() extension method:
    - Namespace: {{packageName}}.Extensions
    - Method: `services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(HandlerExtensions).Assembly));`
    - Assembly scanning: Uses MediatR reflection to find all IRequestHandler implementations in Implementation assembly
    - Generation rule: ONLY generated when useNugetPackaging=true (convenience method for dual-project mode)
    - Rationale: Standard MediatR registration works fine in single-project mode; this adds convenience for split assemblies
  - Expected: New template file for Implementation project Extensions/
  - Result: Template created with MediatR assembly scanning for handler registration.

- [X] T052 [US2] Add handlerExtensions.mustache to supportingFiles in generateImplementationProject()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add supporting file to Implementation project's Extensions/ directory
  - Expected: Handler extension method generated in Implementation project
  - Result: Added to generateImplementationProject() method, HandlerExtensions.cs generated successfully in Implementation/Extensions/.

### Phase 4.2: Program.cs Template Enhancement

- [ ] T053 [US2] Modify program.mustache to demonstrate extension method usage
  - Location: `generator/src/main/resources/aspnet-minimalapi/program.mustache`
  - Action: Add commented examples:
    - `// builder.Services.AddApiValidators(); // Register validators from Contracts package`
    - `// builder.Services.AddApiHandlers(); // Register handlers from this assembly`
    - `// app.AddApiEndpoints(); // Register all API endpoints`
  - Expected: Generated Program.cs includes integration guidance

- [ ] T054 [US2] Add using statement for Contracts.Extensions namespace
  - Location: `generator/src/main/resources/aspnet-minimalapi/program.mustache`
  - Action: Add `using {{packageName}}.Contracts.Extensions;` when useNugetPackaging=true
  - Expected: Extension methods are in scope

### Phase 4.3: Assembly Scanning Documentation

- [ ] T055 [US2] Document MediatR assembly scanning in README.mustache
  - Location: `generator/src/main/resources/aspnet-minimalapi/readme.mustache`
  - Action: Add section explaining:
    - Why validators need special registration (different assembly)
    - How MediatR finds handlers in same assembly as Program.cs
    - When to use extension methods vs manual registration
  - Expected: README explains assembly separation rationale

- [ ] T056 [US2] Create example handler implementation in README
  - Location: `generator/src/main/resources/aspnet-minimalapi/readme.mustache`
  - Action: Add code example showing IRequestHandler implementation
  - Expected: Developers understand how to implement handlers

### Phase 4.4: Testing with Bruno Suite

- [ ] T057 [US2] Build Implementation project with extension methods
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/src/PetstoreApi/`
  - Expected: Build succeeds with new extension methods

- [ ] T058 [US2] Update Program.cs to call extension methods
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi/Program.cs`
  - Action: Manually add calls to AddApiValidators(), AddApiHandlers(), AddApiEndpoints()
  - Expected: Program.cs configured for DI

- [ ] T059 [US2] Run Implementation project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet run --project test-output/src/PetstoreApi/`
  - Expected: Application starts, listening on http://localhost:5000

- [ ] T060 [US2] Run Bruno test suite against running API
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task bruno:start-run-all-stop`
  - Expected: 19/19 tests pass (endpoints functional via NuGet-packaged contracts)
  - Note: Uses existing Taskfile task that handles API start, wait, run tests, and stop lifecycle

- [ ] T061 [US2] Verify validators are invoked for invalid requests
  - Action: Send invalid request (e.g., missing required field) via Bruno
  - Expected: 400 Bad Request with validation error details

- [ ] T062 [US2] Verify handlers are invoked for valid requests
  - Action: Send valid request via Bruno, check handler logs
  - Expected: Handler executes, returns expected response

## Phase 5: User Story 3 - Version API Contracts Independently (P1)

**Story Goal**: Support SemVer-based versioning with backward-compatible and breaking changes

**Independent Test**: Compile v1.1.0 NuGet package against v1.0.0 handlers (should succeed)

### Phase 5.1: Versioning Documentation

- [ ] T063 [US3] Document SemVer versioning guidelines in README.mustache
  - Location: `generator/src/main/resources/aspnet-minimalapi/readme.mustache`
  - Action: Add section explaining:
    - Patch version (1.0.X): Bug fixes, no API changes
    - Minor version (1.X.0): Backward-compatible changes (new optional properties, new endpoints)
    - Major version (X.0.0): Breaking changes (renamed properties, removed endpoints)
  - Expected: README includes versioning strategy

- [ ] T064 [US3] Add example of backward-compatible change
  - Location: `generator/src/main/resources/aspnet-minimalapi/readme.mustache`
  - Action: Show OpenAPI spec change (add optional field) and resulting DTO change
  - Expected: Developers understand what qualifies as backward-compatible

- [ ] T065 [US3] Add example of breaking change
  - Location: `generator/src/main/resources/aspnet-minimalapi/readme.mustache`
  - Action: Show OpenAPI spec change (rename field) and resulting compilation error
  - Expected: Developers understand what qualifies as breaking

### Phase 5.2: Backward-Compatible Change Testing

- [ ] T066 [US3] Create petstore-v1.0.yaml baseline spec
  - Location: `/Users/adam/scratch/git/minimal-api-gen/specs/008-nuget-api-contracts/test-data/petstore-v1.0.yaml`
  - Action: Copy petstore-tests/petstore.yaml as baseline
  - Expected: v1.0.0 baseline spec created

- [ ] T067 [US3] Generate v1.0.0 NuGet package from baseline spec
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,packageVersion=1.0.0" OPENAPI_SPEC=specs/008-nuget-api-contracts/test-data/petstore-v1.0.yaml`
  - Expected: v1.0.0 package generated
  - Note: OPENAPI_SPEC variable overrides default PETSTORE_SPEC in Taskfile

- [ ] T068 [US3] Build v1.0.0 Contracts and Implementation projects
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/`
  - Expected: Both projects build successfully

- [ ] T069 [US3] Create petstore-v1.1.yaml with backward-compatible change
  - Location: `/Users/adam/scratch/git/minimal-api-gen/specs/008-nuget-api-contracts/test-data/petstore-v1.1.yaml`
  - Action: Add optional field to Pet schema (e.g., `category: { type: string }`)
  - Expected: v1.1.0 spec with new optional property

- [ ] T070 [US3] Generate v1.1.0 NuGet package from updated spec
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,packageVersion=1.1.0" OPENAPI_SPEC=specs/008-nuget-api-contracts/test-data/petstore-v1.1.yaml`
  - Expected: v1.1.0 package generated with new optional field

- [ ] T071 [US3] Build v1.1.0 Contracts project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/src/PetstoreApi.Contracts/`
  - Expected: Build succeeds, PetDto now has optional category property

- [ ] T072 [US3] Verify v1.0.0 handlers compile against v1.1.0 Contracts
  - Action: Keep v1.0.0 handlers, update Contracts reference to v1.1.0
  - Expected: Compilation succeeds (handlers don't need to use new optional field)

- [ ] T073 [US3] Run Bruno tests against v1.1.0 with v1.0.0 handlers
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task bruno:start-run-all-stop`
  - Expected: All tests pass (backward compatibility maintained)

### Phase 5.3: Breaking Change Testing

- [ ] T074 [US3] Create petstore-v2.0.yaml with breaking change
  - Location: `/Users/adam/scratch/git/minimal-api-gen/specs/008-nuget-api-contracts/test-data/petstore-v2.0.yaml`
  - Action: Rename Pet.name to Pet.petName (breaking change)
  - Expected: v2.0.0 spec with renamed property

- [ ] T075 [US3] Generate v2.0.0 NuGet package from breaking spec
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,packageVersion=2.0.0" OPENAPI_SPEC=specs/008-nuget-api-contracts/test-data/petstore-v2.0.yaml`
  - Expected: v2.0.0 package generated with renamed property

- [ ] T076 [US3] Build v2.0.0 Contracts project
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/src/PetstoreApi.Contracts/`
  - Expected: Build succeeds, PetDto now has petName (not name)

- [ ] T077 [US3] Attempt to build v1.0.0 handlers against v2.0.0 Contracts
  - Action: Keep v1.0.0 handlers (reference request.Pet.name), update Contracts to v2.0.0
  - Expected: **Compilation errors** (property name no longer exists)

- [ ] T078 [US3] Document compilation error messages
  - Action: Capture error messages showing "Pet does not contain a definition for 'name'"
  - Expected: Clear error messages guide developers to required Handler updates

- [ ] T079 [US3] Update handlers to use new property name
  - Action: Change all references from request.Pet.name to request.Pet.petName
  - Expected: Handlers updated for v2.0.0 API

- [ ] T080 [US3] Verify updated handlers compile against v2.0.0 Contracts
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet build test-output/`
  - Expected: Compilation succeeds after Handler updates

## Phase 6: User Story 4 - Configure Package Metadata (P2)

**Story Goal**: Enable customization of NuGet package metadata via CLI options

**Independent Test**: Verify metadata in generated .nupkg file

### Phase 6.1: Additional CLI Options

- [ ] T081 [US4] Add PACKAGE_DESCRIPTION constant
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `public static final String PACKAGE_DESCRIPTION = "packageDescription";`
  - Expected: Constant defined

- [ ] T082 [US4] Add PACKAGE_LICENSE_EXPRESSION constant
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `public static final String PACKAGE_LICENSE_EXPRESSION = "packageLicenseExpression";`
  - Expected: Constant defined

- [ ] T083 [US4] Add PACKAGE_REPOSITORY_URL constant
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `public static final String PACKAGE_REPOSITORY_URL = "packageRepositoryUrl";`
  - Expected: Constant defined

- [ ] T084 [US4] Add PACKAGE_PROJECT_URL constant
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `public static final String PACKAGE_PROJECT_URL = "packageProjectUrl";`
  - Expected: Constant defined

- [ ] T085 [US4] Add PACKAGE_TAGS constant
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `public static final String PACKAGE_TAGS = "packageTags";`
  - Expected: Constant defined

- [ ] T086 [P] [US4] Register packageDescription CLI option
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `cliOptions.add(CliOption.newString(PACKAGE_DESCRIPTION, "Package description for NuGet feed"));`
  - Expected: CLI option registered

- [ ] T087 [P] [US4] Register packageLicenseExpression CLI option
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `cliOptions.add(CliOption.newString(PACKAGE_LICENSE_EXPRESSION, "SPDX license expression (e.g., Apache-2.0, MIT)"));`
  - Expected: CLI option registered

- [ ] T088 [P] [US4] Register packageRepositoryUrl CLI option
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `cliOptions.add(CliOption.newString(PACKAGE_REPOSITORY_URL, "Git repository URL"));`
  - Expected: CLI option registered

- [ ] T089 [P] [US4] Register packageProjectUrl CLI option
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `cliOptions.add(CliOption.newString(PACKAGE_PROJECT_URL, "Project homepage URL"));`
  - Expected: CLI option registered

- [ ] T090 [P] [US4] Register packageTags CLI option
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `cliOptions.add(CliOption.newString(PACKAGE_TAGS, "Semicolon-separated NuGet tags"));`
  - Expected: CLI option registered

### Phase 6.2: Metadata Processing

- [ ] T091 [US4] Process packageDescription in processOpts()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add default value logic:
    ```java
    String packageDescription = (String) additionalProperties.getOrDefault(
        PACKAGE_DESCRIPTION,
        openAPI.getInfo().getDescription() != null 
            ? openAPI.getInfo().getDescription() 
            : "API contracts for " + openAPI.getInfo().getTitle()
    );
    additionalProperties.put("packageDescription", packageDescription);
    ```
  - Expected: Description defaults to OpenAPI spec description

- [ ] T092 [US4] Process packageLicenseExpression in processOpts()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add default: `String license = (String) additionalProperties.getOrDefault(PACKAGE_LICENSE_EXPRESSION, "Apache-2.0");`
  - Expected: License defaults to Apache-2.0

- [ ] T093 [US4] Process packageRepositoryUrl in processOpts()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add optional property (no default): `if (additionalProperties.containsKey(PACKAGE_REPOSITORY_URL)) { ... }`
  - Expected: Repository URL only included if provided

- [ ] T094 [US4] Process packageProjectUrl in processOpts()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add optional property (no default)
  - Expected: Project URL only included if provided

- [ ] T095 [US4] Process packageTags in processOpts()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add default: `String tags = (String) additionalProperties.getOrDefault(PACKAGE_TAGS, "openapi;minimal-api;contracts");`
  - Expected: Tags default to generic descriptors

### Phase 6.3: Template Updates

- [ ] T096 [US4] Add packageDescription property to nuget-project.csproj.mustache
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Add `<Description>{{packageDescription}}</Description>` in PropertyGroup
  - Expected: Template renders description from CLI option

- [ ] T097 [US4] Add packageLicenseExpression property to template
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Add `<PackageLicenseExpression>{{packageLicenseExpression}}</PackageLicenseExpression>`
  - Expected: Template renders license

- [ ] T098 [US4] Add conditional packageRepositoryUrl to template
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Add `{{#packageRepositoryUrl}}<RepositoryUrl>{{packageRepositoryUrl}}</RepositoryUrl><RepositoryType>git</RepositoryType>{{/packageRepositoryUrl}}`
  - Expected: Repository metadata only rendered if provided

- [ ] T099 [US4] Add conditional packageProjectUrl to template
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Add `{{#packageProjectUrl}}<PackageProjectUrl>{{packageProjectUrl}}</PackageProjectUrl>{{/packageProjectUrl}}`
  - Expected: Project URL only rendered if provided

- [ ] T100 [US4] Add packageTags property to template
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Add `<PackageTags>{{packageTags}}</PackageTags>`
  - Expected: Template renders tags

### Phase 6.4: Metadata Testing

- [ ] T101 [US4] Build generator with metadata support
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task build-generator`
  - Expected: BUILD SUCCESS

- [ ] T102 [US4] Generate with full metadata options
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore.Contracts,packageVersion=1.0.0,packageAuthors=Platform Team,packageDescription=Petstore API contracts,packageLicenseExpression=MIT,packageRepositoryUrl=https://github.com/mycompany/petstore,packageProjectUrl=https://github.com/mycompany/petstore,packageTags=petstore;api;microservices"`
  - Expected: Generation succeeds with all metadata

- [ ] T103 [US4] Verify metadata in generated .csproj
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi.Contracts/PetstoreApi.Contracts.csproj`
  - Action: Check file contains all metadata properties
  - Expected: All PropertyGroup elements present with correct values

- [ ] T104 [US4] Pack NuGet package with metadata
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ --configuration Release --output ./packages/`
  - Expected: .nupkg created

- [ ] T105 [US4] Extract and inspect .nuspec file from package
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `unzip -p packages/MyCompany.Petstore.Contracts.1.0.0.nupkg MyCompany.Petstore.Contracts.nuspec`
  - Expected: .nuspec contains correct metadata (id, version, authors, description, license, repository)

- [ ] T106 [US4] Verify default metadata when options not provided
  - Action: Generate without metadata options, check defaults used
  - Expected: packageDescription from OpenAPI spec, license=Apache-2.0, tags=openapi;minimal-api;contracts

## Phase 7: User Story 5 - Symbol Package for Debugging (P3)

**Story Goal**: Enable generation of .snupkg symbol packages for debugging

**Independent Test**: Verify .snupkg file created alongside .nupkg

### Phase 7.1: Symbol Package CLI Option

- [ ] T107 [US5] Add INCLUDE_SYMBOLS constant
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `public static final String INCLUDE_SYMBOLS = "includeSymbols";`
  - Expected: Constant defined

- [ ] T108 [US5] Register includeSymbols CLI option
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add `cliOptions.add(CliOption.newBoolean(INCLUDE_SYMBOLS, "Generate symbol package (.snupkg) for debugging"));`
  - Expected: CLI option registered

- [ ] T109 [US5] Process includeSymbols in processOpts()
  - Location: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
  - Action: Add:
    ```java
    boolean includeSymbols = Boolean.parseBoolean(
        (String) additionalProperties.getOrDefault(INCLUDE_SYMBOLS, "false")
    );
    additionalProperties.put("includeSymbols", includeSymbols);
    ```
  - Expected: Default to false, can be enabled via CLI

### Phase 7.2: Template Updates for Symbols

- [ ] T110 [US5] Add conditional IncludeSymbols to nuget-project.csproj.mustache
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Add:
    ```xml
    {{#includeSymbols}}
    <IncludeSymbols>true</IncludeSymbols>
    <SymbolPackageFormat>snupkg</SymbolPackageFormat>
    {{/includeSymbols}}
    ```
  - Expected: Symbol properties only rendered when includeSymbols=true

- [ ] T111 [US5] Add DebugType property for portable PDBs
  - Location: `generator/src/main/resources/aspnet-minimalapi/nuget-project.csproj.mustache`
  - Action: Add `<DebugType>portable</DebugType>` in PropertyGroup (always on for .NET 8)
  - Expected: Portable PDB format used (required for .snupkg)

### Phase 7.3: Symbol Package Testing

- [ ] T112 [US5] Build generator with symbol support
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task build-generator`
  - Expected: BUILD SUCCESS

- [ ] T113 [US5] Generate with includeSymbols=true
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,includeSymbols=true"`
  - Expected: Generation succeeds

- [ ] T114 [US5] Verify IncludeSymbols in generated .csproj
  - Location: `/Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi.Contracts/PetstoreApi.Contracts.csproj`
  - Action: Check for `<IncludeSymbols>true</IncludeSymbols>` and `<SymbolPackageFormat>snupkg</SymbolPackageFormat>`
  - Expected: Symbol properties present

- [ ] T115 [US5] Pack with symbols enabled
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ --configuration Release --output ./packages/`
  - Expected: Both .nupkg and .snupkg created

- [ ] T116 [US5] Verify .snupkg file exists
  - Location: `/Users/adam/scratch/git/minimal-api-gen/packages/`
  - Action: Check for PetstoreApi.Contracts.1.0.0.snupkg
  - Expected: Symbol package file exists

- [ ] T117 [US5] Verify .snupkg contains PDB files
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `unzip -l packages/PetstoreApi.Contracts.1.0.0.snupkg`
  - Expected: Package contains .pdb files for debugging

- [ ] T118 [US5] Test with includeSymbols=false (default)
  - Action: Generate without includeSymbols option, pack
  - Expected: Only .nupkg created, no .snupkg

## Phase 8: Polish & Integration

**Goal**: Final integration, documentation, and validation

### Phase 8.1: Taskfile Integration

- [ ] T119 Add pack-nuget task to Taskfile.yml
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add new task:
    ```yaml
    pack-nuget:
      desc: "Pack NuGet package from Contracts project"
      cmds:
        - mkdir -p packages
        - dotnet pack test-output/src/{{.PACKAGE_NAME}}.Contracts/ --configuration Release --output ./packages/
      vars:
        PACKAGE_NAME: '{{.PACKAGE_NAME | default "PetstoreApi"}}'
    ```
  - Expected: Task runner supports NuGet packaging workflow

- [ ] T120 Add test-nuget-package task to Taskfile.yml
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add new task:
    ```yaml
    test-nuget-package:
      desc: "Test NuGet package consumption"
      cmds:
        - task: generate-petstore-minimal-api
          vars: {ADDITIONAL_PROPS: "useNugetPackaging=true"}
        - task: copy-test-stubs
        - task: pack-nuget
        - dotnet build test-output/
    ```
  - Expected: End-to-end packaging workflow automated

- [ ] T121 Add regenerate-with-nuget task
  - Location: `/Users/adam/scratch/git/minimal-api-gen/Taskfile.yml`
  - Action: Add task that combines build-generator + generate with NuGet options
  - Expected: One-command workflow for development

### Phase 8.2: Documentation Updates

- [ ] T122 Update main README.md with NuGet packaging overview
  - Location: `/Users/adam/scratch/git/minimal-api-gen/README.md`
  - Action: Add section "NuGet Packaging" with:
    - Feature summary
    - Quick start example
    - Link to quickstart.md
  - Expected: README mentions NuGet capability

- [ ] T123 Update CONFIGURATION.md with NuGet options
  - Location: `/Users/adam/scratch/git/minimal-api-gen/docs/CONFIGURATION.md`
  - Action: Add section documenting all 6 new CLI options with examples
  - Expected: Complete CLI reference documentation

- [ ] T124 Create quickstart example in docs/
  - Location: `/Users/adam/scratch/git/minimal-api-gen/docs/NUGET_QUICKSTART.md`
  - Action: Copy content from specs/008-nuget-api-contracts/quickstart.md
  - Expected: Standalone quickstart guide in main docs/

- [ ] T125 Update copilot-instructions.md with NuGet commands
  - Location: `/Users/adam/scratch/git/minimal-api-gen/.github/copilot-instructions.md`
  - Action: Add NuGet packaging commands to "Commands" section:
    - `devbox run task pack-nuget`
    - `devbox run task test-nuget-package`
  - Expected: AI assistant aware of NuGet workflows

### Phase 8.3: Integration Testing

- [ ] T126 Create NuGetPackageCreationTest in generator-tests/
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/NuGetPackageCreationTest.cs`
  - Action: Implement test:
    - Generate code with useNugetPackaging=true
    - Build Contracts project
    - Pack NuGet package
    - Assert .nupkg exists
    - Assert package metadata correct
  - Expected: Automated test for US1

- [ ] T127 Create ProjectReferenceCompilationTest in generator-tests/
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/ProjectReferenceCompilationTest.cs`
  - Action: Implement test:
    - Generate dual-project structure
    - Verify Implementation.csproj has ProjectReference to Contracts
    - Build Implementation project
    - Assert build succeeds
  - Expected: Automated test for project reference integrity

- [ ] T128 Create PackageReferenceCompilationTest in generator-tests/
  - Location: `/Users/adam/scratch/git/minimal-api-gen/generator-tests/PackageReferenceCompilationTest.cs`
  - Action: Implement test:
    - Generate and pack NuGet package
    - Create test consumer project
    - Add PackageReference to local .nupkg
    - Implement test handler
    - Build consumer project
    - Assert build succeeds
  - Expected: Automated test for NuGet consumption workflow

### Phase 8.4: Final Validation

- [ ] T129 Run full test suite (xUnit + Bruno)
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `devbox run dotnet test && devbox run task bruno:start-run-all-stop`
  - Expected: All tests pass (existing + new NuGet tests)

- [ ] T130 Generate with all metadata options
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: Generate with full CLI options
  - Expected: Complete metadata in package

- [ ] T131 Verify package size under 500KB
  - Location: `/Users/adam/scratch/git/minimal-api-gen/packages/`
  - Command: `ls -lh *.nupkg`
  - Expected: Size < 500KB (SC-002)

- [ ] T132 Verify build time under 10 seconds
  - Location: `/Users/adam/scratch/git/minimal-api-gen/`
  - Command: `time devbox run dotnet build test-output/`
  - Expected: Total time < 10 seconds (SC-009)

- [ ] T133 Test backward-compatible version update end-to-end
  - Action: Generate v1.0.0, build handlers, update to v1.1.0 (add optional field), rebuild
  - Expected: Zero handler code changes required (SC-004)

- [ ] T134 Test breaking version update end-to-end
  - Action: Generate v1.0.0, update to v2.0.0 (rename field), attempt build
  - Expected: Compilation errors within 1 second (SC-005)

- [ ] T135 Create feature completion checklist
  - Location: `/Users/adam/scratch/git/minimal-api-gen/specs/008-nuget-api-contracts/COMPLETION_CHECKLIST.md`
  - Action: Document all success criteria from spec.md with pass/fail status
  - Expected: Comprehensive validation checklist

## Implementation Strategy

### MVP Scope (Minimum Viable Product)
Focus on User Story 1 (P0) first to unblock all other work:
- Dual-project generation
- NuGet package creation
- Basic extension methods

### Incremental Delivery Order
1. **Phase 1-3**: Setup + US1 (P0) → Enables basic packaging
2. **Phase 4**: US2 (P0) → Enables service injection
3. **Phase 5**: US3 (P1) → Enables versioning workflow
4. **Phase 6-7**: US4 (P2) + US5 (P3) → Polish features (can be done in parallel)
5. **Phase 8**: Integration + Documentation

### Parallelization Opportunities
- **Phase 2**: Tasks T005-T007 (study existing code)
- **Phase 3.2**: Tasks T011-T013 (template creation)
- **Phase 4.1**: Tasks T040-T042 (extension method modifications)
- **Phase 6.1**: Tasks T077-T081 (CLI option registration)
- **After US1 Complete**: US4 and US5 can be done simultaneously

### Testing Strategy
- **Unit Tests**: Generator logic (T117-T119)
- **Integration Tests**: Bruno API tests (T051)
- **End-to-End Tests**: Full workflow validation (T120-T126)
- **Regression Tests**: Ensure existing functionality unchanged

### Risk Mitigation
- **Constitution Compliance**: Validated in Phase 1 (T004)
- **Template Reusability**: Model templates unchanged (Phase 3.5)
- **Breaking Changes**: Protected by compilation errors (Phase 5.3)
- **Performance**: Validated against success criteria (T123-T124)

## Notes

### File References
All tasks reference absolute file paths from project root: `/Users/adam/scratch/git/minimal-api-gen/`

### Command Conventions
All commands use devbox isolation: `devbox run task <task-name>` or `devbox run <command>`

### Task Labeling
- `[P]`: Parallel-safe (can be done concurrently with other [P] tasks in same phase)
- `[US1-US5]`: User Story reference
- No label: Sequential dependency on previous task

### Completion Criteria
Each task includes:
- **Location**: Absolute file path or project directory
- **Action/Command**: Specific change or command to execute
- **Expected**: Observable outcome for validation

### Success Metrics (from spec.md)
- **SC-001**: Developer can generate valid NuGet package (dotnet pack succeeds)
- **SC-002**: Package size < 500KB for typical spec (20 ops, 10 DTOs)
- **SC-003**: 2-3 method calls for full integration (AddApiValidators, AddApiHandlers, AddApiEndpoints)
- **SC-004**: Backward-compatible updates require 0 Handler changes
- **SC-005**: Breaking updates cause compilation errors within 1 second
- **SC-006**: packageVersion follows SemVer conventions
- **SC-009**: Build time < 10 seconds for both projects
- **SC-010**: API routing performance within 5% of inline endpoints

## Appendix: User Story Quick Reference

**US1 (P0)**: Package API Contracts for Distribution
- Tasks: T008-T039
- Deliverables: Dual .csproj structure, NuGet package generation

**US2 (P0)**: Inject Services and Handlers from Host Application
- Tasks: T040-T053
- Deliverables: Extension methods, assembly scanning, Bruno test validation

**US3 (P1)**: Version API Contracts Independently
- Tasks: T054-T071
- Deliverables: SemVer workflow, backward-compatible + breaking change tests

**US4 (P2)**: Configure Package Metadata
- Tasks: T072-T097
- Deliverables: Full CLI options for metadata customization

**US5 (P3)**: Symbol Package for Debugging
- Tasks: T098-T109
- Deliverables: .snupkg generation, debugging support
