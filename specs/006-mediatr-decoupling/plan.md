# Implementation Plan: MediatR Implementation Decoupling

**Branch**: `006-mediatr-decoupling` | **Date**: 2025-11-21 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-mediatr-decoupling/spec.md`

**Note**: This plan implements CQRS pattern with MediatR library for ASP.NET Core Minimal APIs, with optional toggle via `useMediatr` configuration flag.

## Summary

Decouple API implementation from generated Minimal API server stubs using the MediatR library and CQRS pattern. The generator will create clean endpoint stubs that delegate to MediatR handlers, with separate Command/Query classes and handler scaffolds. This removes the technical debt of hardcoded Pet-specific logic in templates and enables a production-ready, maintainable architecture. The feature includes a `useMediatr` configuration flag (default: false) to support both simple TODO stubs and full MediatR architecture.

**Technical Approach** (based on https://raghavendramurthy.com/posts/cqrs-mediatr-in-net/):
- Commands for state mutations (POST, PUT, PATCH, DELETE) implementing `IRequest<TResponse>`
- Queries for state retrieval (GET) implementing `IRequest<TResponse>`
- Handlers implementing `IRequestHandler<TRequest, TResponse>` with TODO scaffolds
- Endpoints inject `IMediator` and call `SendAsync()` to delegate to handlers
- Separate DTOs generated for command/query contracts (not reusing models directly)
- FluentValidation remains inline in endpoints (not moved to MediatR pipeline)
- File-level regeneration protection for handlers (skip if file exists)

## Technical Context

**Language/Version**: Java 11 (generator), C# 11+ / .NET 8.0 (generated code)  
**Primary Dependencies**: 
  - Generator: OpenAPI Generator 7.17.0, Maven 3.8.9+, Mustache template engine
  - Generated Code: MediatR 12.x (conditional), FluentValidation 11.x, ASP.NET Core 8.0
**Storage**: In-memory (for baseline tests only - production uses handler implementations)  
**Testing**: xUnit 2.5.3.1, FluentAssertions 6.12.0, Microsoft.AspNetCore.Mvc.Testing 8.0  
**Target Platform**: .NET 8.0 server applications (Linux, Windows, macOS)
**Project Type**: Code generator (single Java project) generating ASP.NET Core web API projects  
**Performance Goals**: 
  - Generator: Build time < 60s, code generation < 10s for petstore.yaml
  - Generated code: Standard web API expectations (< 100ms p95 for simple CRUD)
**Constraints**: 
  - Must maintain OpenAPI Generator framework compatibility
  - Must preserve Phase 5 functionality (array conversion, complex query params, basePath)
  - Generated handlers must not be overwritten on regeneration
  - Default behavior (useMediatr=false) must provide simplest stubs
**Scale/Scope**: 
  - Petstore spec: 21 operations across 3 tags (Pet, Store, User)
  - Expected to handle OpenAPI specs with 100+ operations
  - Generated project size: ~50-200 files depending on spec complexity

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Inheritance-First Architecture ✅ PASS
**Status**: Compliant  
**Rationale**: This feature extends the existing `MinimalApiServerCodegen` class (which already extends `AspNetCoreServerCodegen`). No new inheritance is needed - we're adding configuration flags and new template files. The inheritance chain remains intact.

### Principle II: Test-Driven Refactoring (NON-NEGOTIABLE) ✅ PASS WITH CONDITIONS
**Status**: Compliant with TDD approach required  
**Conditions**:
1. **Baseline Phase**: Current 7 xUnit tests MUST pass with `useMediatr=false` (maintain current behavior)
2. **Refactor Phase**: Implement MediatR generation with `useMediatr=true`
3. **Migration Phase**: Move Pet-specific in-memory storage logic currently embedded in `api.mustache` template (Dictionary<long, Pet>, _nextId, _lock, CRUD operations) to concrete MediatR handler implementations in test fixtures
4. **Validation Phase**: Same 7 tests MUST pass using the new MediatR handler implementations
**Rationale**: This is the "Refactor" step identified in Phase 5 technical debt. Tests verify endpoint behavior regardless of whether implementation uses inline code or MediatR handlers. The Pet-specific logic currently hardcoded in templates (technical debt) must be extracted to proper handler implementations that can be used to drive integration tests, demonstrating the MediatR pattern works correctly.

### Principle III: Template Reusability ✅ PASS
**Status**: Compliant  
**Rationale**: Model templates (`model.mustache`, `modelRecord.mustache`, etc.) remain unchanged. New templates only for MediatR artifacts (commands, queries, handlers). Existing `api.mustache` will be refactored to remove Pet-specific logic but structure preserved.

### Principle IV: Phase-Gated Progression ✅ PASS
**Status**: Compliant  
**Current Position**: End of Phase 5 (Implementation with technical debt identified)
**This Feature**: Phase 6 - Refactoring to remove technical debt and enable MediatR pattern
**Gates**: 
  - Phase 0 Research: MediatR patterns, DTO generation, template conditionals
  - Phase 1 Design: Data model for commands/queries, contract templates
  - Phase 2 Tasks: Breakdown into testable increments

### Principle V: Build Tool Integration ✅ PASS
**Status**: Compliant  
**Rationale**: All build commands continue to use `devbox run mvn` and `devbox run dotnet`. No changes to build tool integration required.

## Project Structure

### Documentation (this feature)

```text
specs/006-mediatr-decoupling/
├── spec.md              # Feature specification (complete)
├── plan.md              # This file (implementation plan)
├── research.md          # Phase 0 output (MediatR patterns, template design)
├── data-model.md        # Phase 1 output (Command/Query/Handler contracts)
├── quickstart.md        # Phase 1 output (usage examples)
├── contracts/           # Phase 1 output (Mustache template contracts)
│   ├── command.mustache.contract.md
│   ├── query.mustache.contract.md
│   ├── handler.mustache.contract.md
│   └── dto.mustache.contract.md
└── checklists/
    └── requirements.md  # Quality validation checklist (complete)
```

### Source Code - Generator (Java)

```text
generator/
├── src/main/java/org/openapitools/codegen/languages/
│   └── MinimalApiServerCodegen.java  # MODIFY: Add useMediatr flag, DTO generation logic
├── src/main/resources/aspnet-minimalapi/
│   ├── api.mustache                   # MODIFY: Remove vendor extensions, add conditional MediatR delegation
│   ├── command.mustache               # NEW: Generate Command classes
│   ├── query.mustache                 # NEW: Generate Query classes
│   ├── handler.mustache               # NEW: Generate Handler scaffolds
│   ├── commandDto.mustache            # NEW: Generate DTOs for commands (CreatePetDto, UpdatePetDto)
│   ├── queryDto.mustache              # NEW: Generate DTOs for queries (if needed)
│   ├── mediatrRegistration.mustache  # NEW: Generate MediatR DI registration extension
│   ├── program.mustache               # MODIFY: Conditional MediatR registration call
│   ├── project.csproj.mustache        # MODIFY: Conditional MediatR package reference
│   └── [existing templates unchanged]
└── pom.xml  # No changes needed
```

### Source Code - Generated Output (C# when useMediatr=true)

```text
test-output/src/PetstoreApi/
├── Commands/                          # NEW: Flat structure per design decision
│   ├── AddPetCommand.cs               # Command with properties from request body
│   ├── UpdatePetCommand.cs
│   ├── DeletePetCommand.cs
│   └── [one per POST/PUT/PATCH/DELETE operation]
├── Queries/                           # NEW: Flat structure per design decision
│   ├── GetPetByIdQuery.cs             # Query with properties from path/query params
│   ├── FindPetsByStatusQuery.cs
│   ├── GetAllPetsQuery.cs
│   └── [one per GET operation]
├── Handlers/                          # NEW: Flat structure per design decision
│   ├── AddPetCommandHandler.cs        # Scaffold with TODO, NOT regenerated if exists
│   ├── GetPetByIdQueryHandler.cs
│   └── [one per command/query]
├── DTOs/                              # NEW: Separate from Models/
│   ├── CreatePetDto.cs                # Properties from request body schema
│   ├── UpdatePetDto.cs
│   └── [DTOs for command/query contracts]
├── Models/                            # UNCHANGED: Existing model generation
│   ├── Pet.cs
│   ├── Order.cs
│   └── User.cs
├── Features/                          # MODIFIED: Endpoints with MediatR delegation
│   ├── PetApiEndpoints.cs             # Clean endpoints calling mediator.Send()
│   ├── StoreApiEndpoints.cs
│   └── UserApiEndpoints.cs
├── Extensions/
│   ├── EndpointMapper.cs              # UNCHANGED
│   └── MediatrServiceExtensions.cs   # NEW: MediatR registration (when useMediatr=true)
├── Program.cs                         # MODIFIED: Conditional UseMediatr() call
└── PetstoreApi.csproj                 # MODIFIED: Conditional MediatR package reference
```

### Source Code - Generated Output (C# when useMediatr=false)

```text
test-output/src/PetstoreApi/
├── Models/                            # Same as above
├── Features/                          # MODIFIED: Endpoints with TODO comments
│   ├── PetApiEndpoints.cs             # Simple stubs with // TODO: Implement logic
│   ├── StoreApiEndpoints.cs
│   └── UserApiEndpoints.cs
├── Extensions/
│   └── EndpointMapper.cs              # UNCHANGED
├── Program.cs                         # No MediatR registration
└── PetstoreApi.csproj                 # No MediatR package reference
```

**Structure Decision**: This follows a **single code generator project** (Java) that produces ASP.NET Core web API projects (C#). The generated structure uses **flat organization by artifact type** (Commands/, Queries/, Handlers/) per design decision Q1:B. Handler files are protected from regeneration using file existence checks in the generator code (`File.exists()` in `processHandler()` method).

---

## Phase 0: Research

**Deliverable**: [research.md](./research.md)

Research completed covering:
- R1: MediatR CQRS Pattern (IRequest/IRequestHandler interfaces, registration)
- R2: DTO Generation Strategy (separate DTOs, flatten properties into commands)
- R3: Template Conditional Logic ({{#useMediatr}} sections)
- R4: Handler Regeneration Protection (File.exists() check, no overwrite)
- R5: Response Type Mapping (IRequest<T>, IRequest<Unit>, IRequest<IEnumerable<T>>)
- R6: Validation Integration (keep inline in endpoints, not MediatR pipeline)
- R7: Parameter Mapping (flatten all params into command/query properties)
- R8: File Naming Conventions (AddPetCommand, GetPetByIdQuery, AddPetCommandHandler)

**Key Findings**:
- Flat file structure (Commands/, Queries/, Handlers/) simpler than feature-first nesting
- File-level regeneration protection (programmatic check) more flexible than .openapi-generator-ignore
- Matching OpenAPI response types exactly ensures generated code compiles correctly
- Separating API contract (commands/queries always regen) from implementation (handlers never regen) is critical

---

## Phase 1: Design & Contracts

**Deliverables**:
- [data-model.md](./data-model.md) - Entity definitions with properties, validation, relationships
- [contracts/](./contracts/) - Mustache template contracts
  - [command.mustache.contract.md](./contracts/command.mustache.contract.md)
  - [query.mustache.contract.md](./contracts/query.mustache.contract.md)
  - [handler.mustache.contract.md](./contracts/handler.mustache.contract.md)
- [quickstart.md](./quickstart.md) - Usage examples for developers

**Data Model Summary**:
- **Command**: State mutations (POST/PUT/DELETE), `public record` implementing `IRequest<TResponse>`, properties from all operation parameters, always regenerated
- **Query**: State retrieval (GET), `public record` implementing `IRequest<T>` or `IEnumerable<T>`, properties from path/query/header only, always regenerated
- **Handler**: Business logic processor, `public class` implementing `IRequestHandler<TRequest, TResponse>`, contains TODO scaffold, **NEVER regenerated after first creation**
- **Endpoint**: Modified existing with conditional MediatR delegation (`mediator.Send()`) vs TODO stubs

**Template Contracts**: Define input data models, Mustache template structure, output examples, validation rules, generation logic, and regeneration behavior for each artifact type.

---

## Next Steps

This plan document is now **COMPLETE** for Phase 1 (Design & Contracts). To proceed with implementation:

1. **Run task generation**: Use `/speckit.tasks` command to generate `tasks.md` with detailed implementation steps
2. **Begin implementation**: Follow Phase 2 tasks systematically
3. **Maintain TDD**: Run baseline tests with `useMediatr=false`, implement MediatR generation, verify same tests pass with handler implementations
4. **Review gates**: Phase 1 → Phase 2 requires all contracts validated, Phase 2 → Phase 3 requires all tests green

**Branch**: Continue work on `006-mediatr-decoupling`  
**Files Generated**: plan.md, research.md, data-model.md, contracts/, quickstart.md  
**Status**: ✅ Planning phase complete - ready for task generation
