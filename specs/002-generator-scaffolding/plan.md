# Implementation Plan: Generator Scaffolding via Inheritance

**Branch**: `002-generator-scaffolding` | **Date**: 2025-11-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-generator-scaffolding/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Create a custom OpenAPI Generator for ASP.NET Core Minimal APIs by scaffolding a standalone generator project using the OpenAPI Generator `meta` command. The generator (`MinimalApiServerCodegen`) will extend `AbstractCSharpCodegen` and initially replicate the FastEndpoints implementation from Feature 001 analysis. All 17 templates and 4 static files will be copied from the upstream aspnet-fastendpoints/ directory to establish a working baseline. The generator will be built and tested in this project (~/scratch/git/minimal-api-gen/generator/) in isolation from the upstream openapi-generator repository, enabling independent development and refactoring.

## Technical Context

**Language/Version**: Java 8+ (OpenAPI Generator compatibility), C# 11+ (generated code target)
**Primary Dependencies**: OpenAPI Generator framework (AbstractCSharpCodegen base class), Maven 3.8.9+, Mustache template engine
**Storage**: File system (template resources in JAR, generated code output to disk)
**Testing**: Manual validation via `java -jar` execution against petstore.yaml, compiled with dotnet build
**Target Platform**: JVM (generator runtime), .NET 8+ (generated code runtime)
**Project Type**: Code generator (CLI tool producing web API projects)
**Performance Goals**: Build generator in <2 minutes, generate complete project in <10 seconds
**Constraints**: Must extend AbstractCSharpCodegen (not DefaultCodegen), templates must bundle in JAR, must be discoverable via ServiceLoader
**Scale/Scope**: 222-line Java class (matching AspnetFastendpointsServerCodegen), 17 Mustache templates, 4 static files, 11 CLI options

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Inheritance-First Architecture ✅
- **Status**: PASS
- **Evidence**: Spec FR-004 requires extending AbstractCSharpCodegen (not reimplementing from scratch)
- **Compliance**: Generator inherits C#-specific functionality from AbstractCSharpCodegen, only overriding 15 methods for Minimal API customization

### Principle II: Test-Driven Refactoring ⚠️
- **Status**: DEFERRED (Feature 003)
- **Evidence**: Spec explicitly defers test suite creation to Feature 003 (Out of Scope section)
- **Justification**: Scaffolding phase establishes baseline FastEndpoints output; TDD cycle begins in Feature 004 when refactoring to Minimal API
- **Compliance Path**: Feature 003 will create xUnit test suite for FastEndpoints output (RED baseline), Feature 004 refactors to Minimal API (GREEN target)

### Principle III: Template Reusability ✅
- **Status**: PASS
- **Evidence**: Spec identifies 4 model templates (model.mustache, modelClass.mustache, modelRecord.mustache, enumClass.mustache) as 100% reusable (Feature 001 reusability-matrix.md)
- **Compliance**: All 17 templates copied unchanged in this phase; model templates remain framework-agnostic (no modification needed in Feature 004)

### Principle IV: Phase-Gated Progression ✅
- **Status**: PASS
- **Evidence**: This is Phase 2 (Scaffolding), blocked on Phase 1 (Analysis - Feature 001) completion
- **Compliance**: Feature 001 delivered method-override-map.md (15 methods), template-catalog.md (17 templates), reusability-matrix.md - all gates passed
- **Next Gate**: Phase 3 (Baseline Validation - Feature 003) requires working generator from this phase

### Principle V: Build Tool Integration ✅
- **Status**: PASS
- **Evidence**: Spec FR-017 requires `devbox run mvn clean package`, SC-011 requires `devbox run dotnet build`
- **Compliance**: All build commands routed through devbox for environment consistency

**Overall Assessment**: All applicable principles satisfied. Principle II deferred per explicit project plan (scaffolding → validation → refactoring sequence).

## Project Structure

### Documentation (this feature)

```text
specs/002-generator-scaffolding/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (COMPLETE)
├── data-model.md        # Phase 1 output (COMPLETE)
├── quickstart.md        # Phase 1 output (COMPLETE)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created yet)
```

Note: No contracts/ directory needed - this feature produces a code generator (tool), not an API.

### Source Code (repository root)

```text
generator/                                    # Custom OpenAPI Generator project
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/openapitools/codegen/
│   │   │       └── languages/
│   │   │           └── MinimalApiServerCodegen.java   # Generator class (15 methods)
│   │   └── resources/
│   │       ├── aspnet-minimalapi/            # Template directory
│   │       │   ├── endpoint.mustache         # 9 operation templates
│   │       │   ├── request.mustache
│   │       │   ├── requestClass.mustache
│   │       │   ├── requestRecord.mustache
│   │       │   ├── endpointType.mustache
│   │       │   ├── endpointRequestType.mustache
│   │       │   ├── endpointResponseType.mustache
│   │       │   ├── loginRequest.mustache
│   │       │   ├── userLoginEndpoint.mustache
│   │       │   ├── program.mustache          # 4 supporting templates
│   │       │   ├── project.csproj.mustache
│   │       │   ├── solution.mustache
│   │       │   ├── readme.mustache
│   │       │   ├── model.mustache            # 4 model templates
│   │       │   ├── modelClass.mustache
│   │       │   ├── modelRecord.mustache
│   │       │   ├── enumClass.mustache
│   │       │   ├── gitignore                 # 4 static files
│   │       │   ├── appsettings.json
│   │       │   ├── appsettings.Development.json
│   │       │   └── Properties/
│   │       │       └── launchSettings.json
│   │       └── META-INF/services/
│   │           └── org.openapitools.codegen.CodegenConfig  # ServiceLoader registration
│   └── test/
│       └── java/                             # (Future: Feature 003 test suite)
├── pom.xml                                   # Maven build configuration
└── target/                                   # Build output
    └── openapi-generator-minimalapi-1.0.0.jar  # Executable generator JAR

specs/                                        # Feature specifications
├── 001-fastendpoints-analysis/              # Analysis artifacts (input for 002)
│   ├── method-override-map.md               # 15 methods to implement
│   ├── template-catalog.md                  # 17 templates to copy
│   └── reusability-matrix.md                # Template classification
└── 002-generator-scaffolding/              # This feature
    ├── plan.md, research.md, data-model.md, quickstart.md
    └── tasks.md                             # (Created by /speckit.tasks)
```

**Structure Decision**: Single Maven project in `generator/` directory. This is a tool project (CLI code generator), not a web/mobile application. The generator produces ASP.NET Core projects when executed, but those are output artifacts, not part of this repository's source structure.

## Complexity Tracking

No violations - all Constitution principles satisfied or appropriately deferred.

---

## Phase Completion Status

### Phase 0: Outline & Research ✅ COMPLETE
- **research.md**: 8 research questions resolved
  - RQ-001: OpenAPI Generator meta command usage
  - RQ-002: AbstractCSharpCodegen inheritance rationale
  - RQ-003: Files to copy from meta output
  - RQ-004: Generator class naming and location
  - RQ-005: 15 methods from Feature 001 analysis
  - RQ-006: Template inventory and source location
  - RQ-007: 4-stage validation strategy
  - RQ-008: Maven build configuration
- **Blockers**: None identified

### Phase 1: Design & Contracts ✅ COMPLETE
- **data-model.md**: 8 entities modeled
  - GeneratorProject, GeneratorClass, MethodSignature, CLIOption
  - TemplateSet, ServiceRegistration, GeneratorJAR, GeneratedProject
  - Entity relationships and data flow documented
  - Invariants and validation rules specified
- **quickstart.md**: 10-step implementation guide
  - Step 1: Scaffold with meta command
  - Step 2: Copy to this project
  - Step 3: Rename generator class
  - Step 4: Implement 15 methods
  - Step 5: Update ServiceLoader registration
  - Step 6: Copy templates from upstream
  - Step 7: Build with Maven
  - Step 8: Verify discovery
  - Step 9: Generate test project
  - Step 10: Validate compilation
- **Agent Context**: Updated copilot-instructions.md with Feature 002 technologies
- **contracts/**: Not applicable (tool project, not API)

### Phase 2: Task Breakdown ⏳ PENDING
- Requires `/speckit.tasks` command execution
- Will generate tasks.md based on spec.md user stories

---

## Implementation Readiness

**Status**: READY FOR IMPLEMENTATION (Phase 2 tasks)

**Next Action**: Execute `/speckit.tasks` command to generate tasks.md

**Prerequisites Met**:
- ✅ Feature 001 analysis complete (method-override-map.md, template-catalog.md available)
- ✅ Research complete (all unknowns resolved)
- ✅ Design complete (data model and contracts specified)
- ✅ Quickstart guide complete (step-by-step instructions)
- ✅ Constitution check passed (all principles satisfied)
- ✅ Agent context updated (copilot-instructions.md)

**Artifacts Generated**:
- `specs/002-generator-scaffolding/plan.md` (this file)
- `specs/002-generator-scaffolding/research.md` (8 research questions)
- `specs/002-generator-scaffolding/data-model.md` (8 entities)
- `specs/002-generator-scaffolding/quickstart.md` (10 implementation steps)
- `.github/copilot-instructions.md` (updated with Feature 002 tech)

**Ready for**: Task generation (`/speckit.tasks`) and implementation (`/speckit.implement`)
