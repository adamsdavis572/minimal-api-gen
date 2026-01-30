# Implementation Plan: NuGet API Contract Packaging

**Branch**: `008-nuget-api-contracts` | **Date**: 2026-01-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/008-nuget-api-contracts/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Enable OpenAPI Generator to produce NuGet packages containing API contracts (Endpoints, DTOs, Validators) separate from business logic implementations (Handlers, Models). This allows independent versioning and distribution of API surface, supporting SemVer-based evolution without forcing cascading updates to Handler implementations. Technical approach: Generate two .csproj files (Contracts for NuGet packaging, Implementation for business logic), provide DI extension methods for service registration, and use assembly scanning for auto-discovery of validators and handlers.

## Technical Context

**Language/Version**: 
- **Generator**: Java 8 (OpenAPI Generator framework compatibility requirement, pom.xml maven-compiler-plugin source/target 1.8)
- **Generated Code**: C# 11+ / .NET 8.0 (target framework net8.0 in generated .csproj files)

**Primary Dependencies**: 
- **Generator**: OpenAPI Generator 7.17.0 (pom.xml), Maven 3.8.9+, Mustache template engine, AbstractCSharpCodegen base class
- **Generated Code**: 
  - ASP.NET Core 8.0 Minimal APIs framework
  - MediatR 12.2.0 (request/response mediation with assembly scanning)
  - FluentValidation 11.9.0 + DependencyInjectionExtensions (validator registration)
  - Swashbuckle.AspNetCore 6.5.0 (OpenAPI documentation)
  - .NET SDK 8.0+ for `dotnet pack` command

**Storage**: 
- **Generator**: File system (templates in JAR at src/main/resources/aspnet-minimalapi/, generated code output to disk)
- **Generated Code**: N/A (API contracts are stateless, business logic storage handled by Implementation project)

**Testing**: 
- **Generator**: JUnit 5.10.2 (Java unit tests, currently minimal per Feature 002)
- **Generated Code**: 
  - xUnit 2.x + Microsoft.AspNetCore.Mvc.Testing for integration tests (established in Feature 003)
  - Bruno CLI for API contract validation (established in Feature 007, 14 test scenarios)
- **Validation**: 
  - `dotnet build` (compilation check for both projects)
  - `dotnet pack` (package creation check)
  - Bruno test suite run against NuGet-packaged endpoints (verify API contract compatibility)

**Target Platform**: 
- **Generator**: JVM (cross-platform via devbox isolation)
- **Generated Code**: .NET 8.0 runtime (Linux/Windows/macOS)
- **Distribution**: NuGet feeds (NuGet.org, Azure Artifacts, private feeds)

**Project Type**: Code generator (produces web API projects with dual-csproj structure for packaging)

**Performance Goals**: 
- NuGet package generation time <5 seconds for typical OpenAPI spec (20 operations, 10 DTOs)
- Generated package size <500KB (SC-002)
- API routing performance within 5% of inline endpoints (SC-010)
- Compilation time <10 seconds for both projects (SC-009)

**Constraints**: 
- Must maintain inheritance-first architecture (extend AbstractCSharpCodegen, not reimplement)
- Must preserve template reusability (model templates unchanged, framework-agnostic DTOs)
- Must support backward-compatible OpenAPI changes without Handler code changes (SC-004)
- Must cause compilation errors for breaking OpenAPI changes (SC-005, force explicit updates)
- Must follow SemVer conventions for package versioning (SC-006)

**Scale/Scope**: 
- Typical API: 20-50 operations, 10-30 DTOs, 5-15 validators
- Large API: 100+ operations, 50+ DTOs, 30+ validators
- Package metadata: 5 configurable properties (packageId, packageVersion, packageAuthors, packageDescription, includeSymbols)
- DI registration: 2-3 extension method calls (AddApiEndpoints required, AddApiValidators recommended, AddApiHandlers optional)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Inheritance-First Architecture

**Status**: COMPLIANT - No violations

**Evidence**:
- Feature builds on existing MinimalApiServerCodegen which extends AbstractCSharpCodegen
- No new base class creation required
- NuGet packaging achieved through template additions and new CLI options
- Leverages existing model generation (DTOs remain framework-agnostic per Principle III)

**Action**: None required. Proceed with template-based approach.

---

### ✅ II. Test-Driven Refactoring

**Status**: COMPLIANT - Existing test suite extends naturally

**Evidence**:
- Feature 003 established xUnit test suite (7 integration tests)
- Current tests validate: endpoint routing, request/response serialization, validation, HTTP status codes
- Tests use CustomWebApplicationFactory pattern (hosts generated code)
- NuGet packaging doesn't change API behavior, only distribution mechanism
- Plan: Extend test suite with 2 new tests:
  1. Package creation test (`dotnet pack` succeeds, .nupkg exists)
  2. Package consumption test (reference .nupkg, endpoints still functional)

**Action**: Add test cases to existing test-output-tests/ project in Phase 2 implementation:
1. NuGetPackageCreationTest (verify .nupkg exists with correct metadata)
2. ProjectReferenceCompilationTest (verify Implementation.csproj builds with Contracts reference)
3. BrunoSuiteAgainstNuGetPackagedEndpoints (verify all 14 Bruno API tests pass against NuGet-packaged endpoints, proving API contract integrity)

---

### ✅ III. Template Reusability

**Status**: COMPLIANT - Model templates unchanged

**Evidence**:
- Model templates (model.mustache, modelEnum.mustache, modelValidator.mustache) remain framework-agnostic
- NuGet packaging only affects:
  - project.csproj.mustache (new: NuGet package .csproj)
  - solution.mustache (new: multi-project solution structure)
  - endpointMapper.mustache (no change, reused in both projects)
  - serviceCollectionExtensions.mustache (minor change for extension method exposure)
- DTOs are POCOs, usable across packaging modes

**Action**: None required. Preserve existing model template architecture.

---

### ✅ IV. Phase-Gated Progression

**Status**: COMPLIANT - Plan follows gate structure

**Evidence**:
- **Phase 0 (Research)**: Resolve .csproj file linking, assembly scanning patterns, unified source generation approach
- **Phase 1 (Design)**: data-model.md (entities), contracts/ (CLI options), quickstart.md (workflow)
- **Phase 2 (Implementation)**: Will be in tasks.md (gate: Phase 1 complete + constitution re-check passed)
- **Phase 3 (Validation)**: Test suite extension (gate: implementation complete)
- **Phase 4 (Documentation)**: Update copilot-instructions.md (gate: tests passing)

**Action**: Follow planned phase sequence. Do not implement before research/design complete.

---

### ✅ V. Build Tool Integration

**Status**: COMPLIANT - Devbox already established

**Evidence**:
- Existing workflow: `devbox run task build-generator`, `devbox run task generate-petstore-minimal-api`
- New commands will use same pattern: `devbox run dotnet pack test-output/src/PetstoreApi.Contracts/`
- Taskfile.yml will be extended with `pack-nuget` task
- No direct mvn/dotnet/java commands used

**Action**: Update Taskfile.yml with NuGet pack tasks in Phase 2 implementation.

---

### Summary

**Overall Status**: ✅ ALL PRINCIPLES COMPLIANT

No constitution violations detected. Feature aligns with:
- Inheritance model (extends existing generator)
- TDD workflow (extends existing test suite)
- Template reuse (model templates unchanged)
- Phase gating (research → design → implement → test)
- Build tool isolation (devbox-based workflow)

**Gate Result**: PASS - Proceed to Phase 0 Research

---

## Re-Evaluation: Constitution Check After Phase 1 Design

*GATE: Phase 1 (research, data-model, contracts, quickstart) complete. Re-check for design-level violations.*

### ✅ I. Inheritance-First Architecture (Re-check)

**Status**: COMPLIANT - Design preserves inheritance model

**Evidence from Phase 1**:
- research.md RQ-001: Uses MSBuild `<Compile Include>` patterns (standard, no custom code generation)
- data-model.md: Generator CLI Options entity shows 6 new options added to existing MinimalApiServerCodegen
- contracts/CLI-Options.md: All options integrate with existing AbstractCSharpCodegen patterns
- No new base classes proposed, no framework reimplementation

**Action**: None required. Design respects inheritance principle.

---

### ✅ II. Test-Driven Refactoring (Re-check)

**Status**: COMPLIANT - Test strategy defined

**Evidence from Phase 1**:
- quickstart.md Steps 4-5: Build verification for both projects
- quickstart.md Step 6: Package creation test (`dotnet pack`)
- quickstart.md Steps 8-12: Package consumption test (consumer app)
- Extends existi4 new test cases in tasks.md:
1. NuGetPackageCreationTest (verify .nupkg exists with correct metadata)
2. ProjectReferenceCompilationTest (verify Implementation.csproj builds with Contracts reference)
3. PackageReferenceCompilationTest (verify consumer app builds with .nupkg reference)
4. BrunoSuiteValidation (run 14 Bruno API tests against NuGet-packaged endpoints to verify API contract compatibility
1. NuGetPackageCreationTest (verify .nupkg exists with correct metadata)
2. ProjectReferenceCompilationTest (verify Implementation.csproj builds with Contracts reference)
3. PackageReferenceCompilationTest (verify consumer app builds with .nupkg reference)

---

### ✅ III. Template Reusability (Re-check)

**Status**: COMPLIANT - Model templates unchanged

**Evidence from Phase 1**:
- Project Structure section: model.mustache, modelEnum.mustache, modelValidator.mustache marked UNCHANGED
- research.md RQ-005: Generated/ directory approach preserves single-source generation
- contracts/CsprojStructure.md: File inclusion uses MSBuild, not template duplication
- DTOs remain framework-agnostic POCOs

**Action**: None required. Template reuse preserved.

---

### ✅ IV. Phase-Gated Progression (Re-check)

**Status**: COMPLIANT - Phase 1 deliverables complete

**Phase 1 Deliverables** (all complete):
- ✅ research.md (5 research questions resolved)
- ✅ data-model.md (6 entities documented)
- ✅ contracts/ (3 contract files: CLI-Options, CsprojStructure, ExtensionMethods)
- ✅ quickstart.md (15-step workflow)
- ✅ copilot-instructions.md updated via update-agent-context.sh

**Gate Status**: Phase 1 COMPLETE. Ready for Phase 2 (tasks.md generation via `/speckit.tasks`).

**Action**: Proceed to `/speckit.tasks` command to generate implementation task breakdown.

---

### ✅ V. Build Tool Integration (Re-check)

**Status**: COMPLIANT - Devbox workflow extended

**Evidence from Phase 1**:
- quickstart.md consistently uses `devbox run` prefix for all commands
- No direct mvn/dotnet/task commands documented
- New workflow commands follow existing patterns:
  - `devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true"`
  - `devbox run dotnet pack test-output/src/PetstoreApi.Contracts/`
  - `devbox run dotnet build test-output/`

**Action**: Update Taskfile.yml in Phase 2 to add:
- `pack-nuget` task (runs dotnet pack)
- `test-nuget-package` task (runs consumption workflow)

---

### Summary (Post-Design)

**Overall Status**: ✅ ALL PRINCIPLES REMAIN COMPLIANT

Phase 1 design artifacts (research, data-model, contracts, quickstart) introduce no constitution violations. Design aligns with:
- Inheritance model (6 new CLI options, no new base classes)
- TDD workflow (3 new test cases planned)
- Template reuse (model templates unchanged, unified generation)
- Phase gating (Phase 1 complete, Phase 2 ready to start)
- Build tool isolation (devbox commands throughout)

**Gate Result**: PASS - Approved to proceed to Phase 2 (Implementation Planning via `/speckit.tasks`)

**Next Command**: `/speckit.tasks` to generate detailed task breakdown

## Project Structure

### Documentation (this feature)

```text
specs/008-nuget-api-contracts/
├── spec.md              # Feature specification (completed)
├── checklists/
│   └── requirements.md  # Quality validation (completed, 16/16 passing)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (pending)
├── data-model.md        # Phase 1 output (pending)
├── quickstart.md        # Phase 1 output (pending)
├── contracts/           # Phase 1 output (pending)
│   ├── CLI-Options.md       # Generator CLI properties
│   ├── CsprojStructure.md   # .csproj file contracts
│   └── ExtensionMethods.md  # DI registration API
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Generator source (Java/Mustache templates)
generator/
├── src/main/java/org/openapitools/codegen/languages/
│   └── MinimalApiServerCodegen.java          # Extends AbstractCSharpCodegen
│       # New methods to add:
│       # - setUseNugetPackaging() setter
│       # - setPackageId() setter
│       # - setPackageVersion() setter
│       # - setPackageAuthors() setter
│       # - setPackageDescription() setter
│       # - setIncludeSymbols() setter
│       # - generateNugetPackageProject() method
│       # - generateImplementationProject() method
│       # New CLI options:
│       # - USE_NUGET_PACKAGING = "useNugetPackaging"
│       # - PACKAGE_ID = "packageId"
│       # - PACKAGE_VERSION = "packageVersion"
│       # - PACKAGE_AUTHORS = "packageAuthors"
│       # - PACKAGE_DESCRIPTION = "packageDescription"
│       # - INCLUDE_SYMBOLS = "includeSymbols"
└── src/main/resources/aspnet-minimalapi/      # Mustache templates
    ├── model.mustache                         # UNCHANGED (framework-agnostic DTOs)
    ├── modelEnum.mustache                     # UNCHANGED
    ├── modelValidator.mustache                # UNCHANGED
    ├── api.mustache                           # UNCHANGED (endpoint registration logic)
    ├── command.mustache                       # UNCHANGED (MediatR commands)
    ├── query.mustache                         # UNCHANGED (MediatR queries)
    ├── handler.mustache                       # UNCHANGED (request handlers)
    ├── project.csproj.mustache                # MODIFIED (conditional NuGet metadata)
    ├── nuget-project.csproj.mustache          # NEW (NuGet package project)
    ├── implementation-project.csproj.mustache # NEW (Implementation project)
    ├── solution.mustache                      # MODIFIED (multi-project structure)
    ├── program.mustache                       # UNCHANGED (stays in Implementation)
    ├── endpointMapper.mustache                # MODIFIED (expose public API)
    ├── serviceCollectionExtensions.mustache   # MODIFIED (expose AddApiValidators/AddApiHandlers)
    └── readme.mustache                        # MODIFIED (document dual-project workflow)

# Generated output (when useNugetPackaging=true)
test-output/
├── PetstoreApi.sln                            # Multi-project solution
├── src/
│   ├── PetstoreApi.Contracts/                 # NuGet package project
│   │   ├── PetstoreApi.Contracts.csproj       # PackageId, Version, Authors, Description
│   │   ├── Endpoints/
│   │   │   └── PetEndpoints.cs                # Endpoint registration (public API)
│   │   ├── DTOs/
│   │   │   ├── AddPetDto.cs                   # Data transfer objects
│   │   │   └── PetDto.cs
│   │   ├── Validators/
│   │   │   └── AddPetDtoValidator.cs          # FluentValidation validators
│   │   ├── Commands/                          # MediatR commands
│   │   │   └── AddPetCommand.cs
│   │   ├── Queries/                           # MediatR queries
│   │   │   └── GetPetByIdQuery.cs
│   │   ├── Extensions/
│   │   │   ├── EndpointMapper.cs              # AddApiEndpoints() extension
│   │   │   └── ServiceCollectionExtensions.cs # AddApiValidators() extension
│   │   └── Converters/
│   │       ├── EnumMemberJsonConverter.cs     # JSON serialization support
│   │       └── EnumMemberJsonConverterFactory.cs
│   └── PetstoreApi/                           # Implementation project
│       ├── PetstoreApi.csproj                 # References PetstoreApi.Contracts via ProjectReference
│       ├── Handlers/
│       │   ├── AddPetHandler.cs               # Business logic implementations
│       │   └── GetPetByIdHandler.cs
│       ├── Models/
│       │   └── Pet.cs                         # Domain models (internal)
│       ├── Program.cs                         # Entry point with DI setup
│       ├── appsettings.json
│       ├── appsettings.Development.json
│       └── Properties/
│           └── launchSettings.json
└── tests/
    └── PetstoreApi.Tests/                     # Integration tests
        ├── PetstoreApi.Tests.csproj           # References PetstoreApi.Contracts + PetstoreApi
        ├── EndpointTests.cs                   # HTTP integration tests (Feature 003)
        └── NugetPackagingTests.cs             # NEW: Package creation/consumption tests

# Build artifacts (generated by dotnet pack)
test-output/src/PetstoreApi.Contracts/
├── bin/Release/net8.0/
│   └── PetstoreApi.Contracts.1.0.0.nupkg      # Distributable NuGet package
└── obj/                                        # Build intermediates
```

**Structure Decision**: 

This feature introduces a **dual-project structure** when `useNugetPackaging=true` is enabled:

1. **Contracts Project** (`PetstoreApi.Contracts.csproj`): Contains API surface layer (Endpoints, DTOs, Validators, Commands, Queries, Extension Methods). This is the NuGet package that gets distributed via `dotnet pack`.

2. **Implementation Project** (`PetstoreApi.csproj`): Contains business logic (Handlers, Models, Program.cs). References Contracts project via `<ProjectReference>` during local development. When consuming published NuGet package, this reference changes to `<PackageReference>`.

When `useNugetPackaging=false` (default), generator produces traditional single-project structure from Features 002-007.

The solution file (`.sln`) coordinates both projects for local development. The generator uses **unified source generation** approach: all files are generated once to `src/Shared/` temporary directory, then copied to appropriate project directories based on their role (API contract vs implementation).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
