# Implementation Plan: Test-Driven Refactoring to Minimal API

**Branch**: `004-minimal-api-refactoring` | **Date**: 2025-11-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-minimal-api-refactoring/spec.md`

**Note**: This plan was generated following the speckit.plan workflow after merging completed work from Feature 003 (baseline test suite).

## Summary

Refactor the OpenAPI generator from FastEndpoints to Minimal API using Test-Driven Development (TDD). The generator currently produces FastEndpoints code (class-based endpoints with `Endpoint<TRequest, TResponse>` base class). This refactoring will modify Java methods and Mustache templates to generate ASP.NET Core Minimal APIs (functional route handlers with `app.MapGet/Post/etc()`) while maintaining functional equivalence proven by the 7 passing integration tests from Feature 003. The approach leverages reusability analysis from Feature 001: model templates remain unchanged (100% reusable), supporting templates require modification (70-85% reusable), and operation templates need complete replacement (0% reusable).

## Technical Context

**Generator Language/Version**: Java 11 (OpenAPI Generator framework requirement)  
**Generated Code Language**: C# 11+ / .NET 8.0 (target for Minimal APIs)  
**Primary Dependencies**: 
- Generator: OpenAPI Generator 7.18.0-SNAPSHOT, Maven 3.8.9+, Mustache templates
- Generated Code: ASP.NET Core 8.0 Minimal APIs, Swashbuckle.AspNetCore (OpenAPI), FluentValidation.AspNetCore
**Storage**: In-memory Dictionary for baseline tests (from Feature 003), N/A for generator itself  
**Testing**: 
- Generator: JUnit (Java unit tests for generator logic)
- Generated Code: xUnit 2.x + Microsoft.AspNetCore.Mvc.Testing (7 integration tests from Feature 003)
**Target Platform**: 
- Generator: JVM (cross-platform via devbox)
- Generated Code: .NET 8.0 runtime (Linux/Windows/macOS)
**Project Type**: Code generator (produces web API projects)  
**Performance Goals**: 
- Generator build: <60s (SC-001 from Feature 003)
- Generated code compilation: <10s
- Test execution: <30s for 7+ tests (SC-004 from Feature 003)
**Constraints**: 
- MUST maintain 100% test pass rate from Feature 003 (7/7 tests GREEN)
- MUST NOT modify model templates (Constitution Principle III - Template Reusability)
- MUST use devbox for all build commands (Constitution Principle V)
- MUST follow TDD RED-GREEN cycle (Constitution Principle II - NON-NEGOTIABLE)
**Scale/Scope**: 
- 17 Mustache templates total (from Feature 001 analysis)
- 4 templates reusable unchanged (24% - model templates)
- 3 templates require modification (18% - supporting files)
- 10 templates need replacement (58% - operation templates)
- Target: Under 10 TDD iterations to achieve GREEN (SC-005)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Initial Check (Pre-Research)

**Principle I: Inheritance-First Architecture** ✅ PASS
- Generator extends `AspNetCoreServerCodegen` (established in Feature 002)
- Only overriding specific methods: `processOpts()`, `postProcessOperationsWithModels()`, `apiTemplateFiles()`, `supportingFiles()`
- Model generation logic inherited unchanged
- **Action**: Continue with inheritance approach

**Principle II: Test-Driven Refactoring** ✅ PASS
- 7 passing integration tests exist from Feature 003 (baseline/Golden Standard)
- Tests validate CRUD operations: Create (201), Read (200/404), Update (200/404), Delete (204/404)
- Test suite execution time: 0.8s (well under 30s requirement)
- **Action**: Use Feature 003 tests as contract for refactoring validation

**Principle III: Template Reusability** ✅ PASS
- Reusability Matrix from Feature 001 confirms 4 model templates are framework-agnostic
- Template catalog documents 100% reusability for: `model.mustache`, `modelClass.mustache`, `modelRecord.mustache`, `enumClass.mustache`
- **Action**: Do NOT modify model templates during refactoring

**Principle IV: Phase-Gated Progression** ✅ PASS
- Feature 001 (Analysis): Complete - Reusability Matrix and Template Catalog exist
- Feature 002 (Scaffolding): Complete - Generator structure with inheritance established
- Feature 003 (Baseline Validation): Complete - 7/7 tests passing for FastEndpoints output
- Feature 004 (Refactoring): Current phase - proceeding with TDD approach
- **Action**: Follow TDD cycle: Modify generator/templates → Rebuild → Regenerate → Run tests → Fix → Repeat

**Principle V: Build Tool Integration** ✅ PASS
- All build commands use `devbox run` wrapper (verified in Feature 003)
- Maven: `devbox run mvn clean package`
- .NET: `devbox run dotnet build`, `devbox run dotnet test`
- **Action**: Continue using devbox for all commands

### Post-Design Check (After Phase 1)

[To be completed after research and design phases]

## Project Structure

### Documentation (this feature)

```text
specs/004-minimal-api-refactoring/
├── spec.md              # Feature specification (pre-existing)
├── plan.md              # This file (generated by /speckit.plan)
├── research.md          # Phase 0: Template refactoring patterns, Minimal API best practices
├── data-model.md        # Phase 1: operationsByTag structure, template variable mapping
├── quickstart.md        # Phase 1: TDD workflow, generator modification cycle
├── contracts/           # Phase 1: Template transformation contracts
│   ├── ProgramCs.md     # program.mustache transformation contract
│   ├── ProjectCsproj.md # project.csproj.mustache transformation contract
│   ├── TagEndpoints.md  # New TagEndpoints.cs.mustache contract
│   └── EndpointMapper.md # New EndpointMapper.cs.mustache contract
└── tasks.md             # Phase 2: Generated by /speckit.tasks (not by /speckit.plan)
```

### Source Code (repository root)

```text
generator/                           # OpenAPI Generator custom implementation
├── pom.xml                         # Maven build configuration
├── devbox.json                     # Build environment specification
├── src/main/java/
│   └── org/openapitools/codegen/languages/
│       └── MinimalApiServerCodegen.java  # Generator Java class (MODIFY)
├── src/main/resources/aspnet-minimalapi/  # Mustache templates
│   ├── model.mustache              # REUSE UNCHANGED (Framework-agnostic)
│   ├── modelClass.mustache         # REUSE UNCHANGED (Framework-agnostic)
│   ├── modelRecord.mustache        # REUSE UNCHANGED (Framework-agnostic)
│   ├── enumClass.mustache          # REUSE UNCHANGED (Framework-agnostic)
│   ├── program.mustache            # MODIFY (Remove FastEndpoints, add Minimal API)
│   ├── project.csproj.mustache     # MODIFY (Replace FastEndpoints packages)
│   ├── solution.mustache           # REUSE UNCHANGED
│   ├── gitignore                   # REUSE UNCHANGED
│   ├── appsettings.json            # REUSE UNCHANGED
│   ├── appsettings.Development.json # REUSE UNCHANGED
│   ├── Properties/launchSettings.json # REUSE UNCHANGED
│   ├── readme.mustache             # MODIFY (Update framework references)
│   ├── TagEndpoints.cs.mustache    # CREATE NEW (Replace endpoint.mustache)
│   ├── EndpointMapper.cs.mustache  # CREATE NEW (Map all tag endpoints)
│   ├── [DELETE] endpoint.mustache  # DELETE (FastEndpoints-specific)
│   ├── [DELETE] request.mustache   # DELETE (FastEndpoints validation pattern)
│   ├── [DELETE] requestClass.mustache # DELETE (Part of FastEndpoints pattern)
│   ├── [DELETE] requestRecord.mustache # DELETE (Part of FastEndpoints pattern)
│   ├── [DELETE] endpointType.mustache # DELETE (FastEndpoints base class)
│   ├── [DELETE] endpointRequestType.mustache # DELETE (FastEndpoints pattern)
│   ├── [DELETE] endpointResponseType.mustache # DELETE (FastEndpoints pattern)
│   ├── [DELETE] loginRequest.mustache # DELETE (FastEndpoints-specific)
│   └── [DELETE] userLoginEndpoint.mustache # DELETE (FastEndpoints-specific)
└── target/                         # Build output (generator JAR)

test-output/                        # Generated project output (regenerated each iteration)
├── src/PetstoreApi/
│   ├── Program.cs                  # Generated from program.mustache
│   ├── PetstoreApi.csproj          # Generated from project.csproj.mustache
│   ├── Models/                     # Generated from model templates (UNCHANGED)
│   │   ├── Pet.cs
│   │   ├── Category.cs
│   │   ├── Tag.cs
│   │   └── ...
│   └── Endpoints/                  # Generated from TagEndpoints.cs.mustache (NEW STRUCTURE)
│       ├── PetEndpoints.cs         # All pet operations in one class
│       ├── StoreEndpoints.cs       # All store operations in one class
│       ├── UserEndpoints.cs        # All user operations in one class
│       └── EndpointMapper.cs       # Extension method to register all endpoints
└── tests/PetstoreApi.Tests/        # Test suite from Feature 003 (UNCHANGED)
    ├── PetEndpointTests.cs         # 7 integration tests
    └── CustomWebApplicationFactory.cs
```

**Structure Decision**: This is a code generator refactoring project. The generator source code lives in `generator/` and produces output in `test-output/`. The refactoring follows the reusability analysis from Feature 001: model templates remain in place (24% unchanged), supporting templates are modified (18%), and operation templates are replaced (58%). The test suite from Feature 003 validates functional equivalence without modification.

## Complexity Tracking

> **No violations detected** - All constitution principles are satisfied. This feature follows TDD with existing test suite, maintains inheritance architecture, reuses model templates, follows phase gates, and uses devbox for all builds.

---

## Phase 0: Research (COMPLETE ✅)

**Status**: ✅ COMPLETE  
**Date**: 2025-11-14  
**Deliverable**: `research.md` (350+ lines)

**Research Areas Resolved**:
1. **Minimal API Patterns**: ✅ Route groups by tag decision (vs class-per-operation)
2. **FluentValidation**: ✅ Manual validation with DI (vs automatic)
3. **Transformation Strategy**: ✅ Infrastructure→Delete→Create→Java sequence (4 phases)
4. **operationsByTag Implementation**: ✅ `Map<String, List<CodegenOperation>>` structure
5. **Test Compatibility**: ✅ Zero modifications required (WebApplicationFactory works as-is)

**Key Decisions**:
- Use `RouteGroupBuilder` pattern: One `{Tag}Endpoints.cs` class per OpenAPI tag
- Manual FluentValidation: `IValidator<T>` DI + `await validator.ValidateAsync()`
- Transformation sequence: Phase A (Infrastructure), Phase B (Delete FastEndpoints), Phase C (Create Minimal API), Phase D (Java refactor)
- operationsByTag: Group operations in `postProcessOperationsWithModels()` with "Default" tag fallback
- Test suite: Feature 003 tests work unchanged (WebApplicationFactory compatible)

**Validation**: All "NEEDS CLARIFICATION" items from Technical Context resolved.

---

## Phase 1: Design & Contracts (COMPLETE ✅)

**Status**: ✅ COMPLETE  
**Date**: 2025-11-14  
**Deliverables**: 
- `data-model.md` (defines operationsByTag structure)
- `contracts/ProgramCs.md` (Program.cs transformation spec)
- `contracts/ProjectCsproj.md` (PetstoreApi.csproj transformation spec)
- `contracts/TagEndpoints.md` (TagEndpoints.cs.mustache contract)
- `contracts/EndpointMapper.md` (EndpointMapper.cs.mustache contract)
- `quickstart.md` (TDD workflow guide)
- Agent context updated (copilot-instructions.md)

### data-model.md
**Purpose**: Document operationsByTag data structure and template variable mappings.

**Key Content**:
- `Map<String, List<CodegenOperation>>` structure with tag names as keys
- CodegenOperation fields: operationId, operationIdPascalCase, httpMethod, path, returnType, etc.
- Computed fields: tagPascalCase, returnType, successCode, resultMethod
- Java implementation in `postProcessOperationsWithModels()` method
- Mustache template consumption patterns

### contracts/ProgramCs.md
**Transformation**: FastEndpoints → Minimal API program setup

**Changes**:
- **REMOVE**: `FastEndpoints`, `FastEndpoints.Swagger` packages
- **ADD**: `Swashbuckle.AspNetCore`, `FluentValidation` DI
- **Service Registration**: `AddEndpointsApiExplorer()`, `AddSwaggerGen()`, `AddValidatorsFromAssemblyContaining<Program>()`
- **Middleware**: `UseSwagger()`, `UseSwaggerUI()`, `MapAllEndpoints()`

### contracts/ProjectCsproj.md
**Transformation**: FastEndpoints dependencies → Minimal API dependencies

**Changes**:
- **REMOVE**: `FastEndpoints` (5.29.0), `FastEndpoints.Swagger` (5.29.0)
- **ADD**: `Swashbuckle.AspNetCore` (6.5.0), `FluentValidation` (11.9.0), `FluentValidation.DependencyInjectionExtensions` (11.9.0)
- **PropertyGroup**: Enable `ImplicitUsings`, keep `Nullable=enable`

### contracts/TagEndpoints.md
**Template**: `TagEndpoints.cs.mustache` (NEW - replaces endpoint.mustache, request.mustache, validators.mustache)

**Structure**:
- One class per OpenAPI tag: `{TagPascalCase}Endpoints`
- Extension method: `Map{TagPascalCase}Endpoints(this RouteGroupBuilder group)`
- Inline endpoint definitions: `group.MapPost/Get/Put/Delete()`
- Manual validation: `IValidator<T>` DI + `ValidateAsync()`
- Response metadata: `.WithName()`, `.WithSummary()`, `.Produces<T>()`, `.ProducesProblem()`

### contracts/EndpointMapper.md
**Template**: `EndpointMapper.cs.mustache` (NEW - orchestrates all tag groups)

**Structure**:
- Single extension method: `MapAllEndpoints(this IEndpointRouteBuilder app)`
- Route prefix support: `var v2 = app.MapGroup("/v2")`
- Calls: `v2.MapPetEndpoints()`, `v2.MapStoreEndpoints()`, etc.

### quickstart.md
**Purpose**: TDD workflow guide for implementation

**Phases**:
- Phase A: Initial RED (expected test failures)
- Phase B: First GREEN (fix routing)
- Phase C: Validation GREEN (wire FluentValidation)
- Phase D: Logic Injection GREEN (inject PetStore CRUD)

**Key Commands**:
- Build generator: `devbox run mvn clean package`
- Generate code: `devbox run java -cp ... generate`
- Build C#: `devbox run dotnet build`
- Run tests: `devbox run dotnet test`

**TDD Cycle Tracking**: Template for recording RED→GREEN iterations.

### Agent Context Update
**File**: `.github/copilot-instructions.md`

**Added**:
- Database: In-memory Dictionary for baseline tests (from Feature 003)
- Recent Changes: Feature 004 entry

**Preserved**: Manual additions section (between markers)

---

## Next Steps

**Phase 2**: Generate tasks.md (via `/speckit.tasks` command - separate from `/speckit.plan`)

**Sequence**:
1. Run `/speckit.tasks` in chat (NOT manual editing)
2. Tool will load plan.md, research.md, contracts/
3. Generate tasks.md with work breakdown structure
4. User reviews and confirms tasks
5. Proceed to Phase 3+ (actual implementation)

**Note**: Planning phase (Phases 0-1) is now COMPLETE. Do NOT proceed to implementation until tasks.md is generated via `/speckit.tasks` command.
