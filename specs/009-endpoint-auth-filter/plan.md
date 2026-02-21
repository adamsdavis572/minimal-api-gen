# Implementation Plan: Endpoint Authentication & Authorization Filter

**Branch**: `009-endpoint-auth-filter` | **Date**: 2025-02-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-endpoint-auth-filter/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement authentication and authorization for generated Minimal API endpoints using `IEndpointFilter` pattern without modifying generated code. The solution uses:
- **Authorization Policies**: Two policies (ReadAccess, WriteAccess) based on permission claims
- **Endpoint Filter**: `IEndpointFilter` implementation that maps endpoint names to policies
- **Route Group Extension**: New `AddAuthorizedApiEndpoints()` method that applies filter to route group
- **Zero Code Modification**: Generated files in Contract directory remain unchanged
- **Testability**: All existing tests pass, with new Bruno tests for authorization scenarios

## Technical Context

**Language/Version**: C# 11+ / .NET 8.0 (generated code target), Java 11 (generator build)  
**Primary Dependencies**: 
- ASP.NET Core 8.0 (Minimal APIs, `IEndpointFilter`, `IAuthorizationService`)
- FluentValidation (existing - validators already integrated)
- MediatR (existing - command/query handlers already integrated)
- OpenAPI Generator framework (Java side - custom generator inherits from `AspNetCoreServerCodegen`)

**Storage**: In-memory (for baseline tests only - production uses handler implementations)  
**Testing**: 
- xUnit (45 existing unit tests - must continue passing)
- Bruno CLI (27 existing integration tests + new authorization tests)
- `Microsoft.AspNetCore.Mvc.Testing` (`CustomWebApplicationFactory`)

**Target Platform**: ASP.NET Core 8.0 web server (Linux/Windows/Mac)  
**Project Type**: Code generator + generated web API (dual concern: generator templates + runtime API)  
**Performance Goals**: Authorization overhead < 5ms per request (FR-SC-006)  
**Constraints**: 
- **Zero modification to generated Contract code** (core requirement)
- Authorization logic MUST be injectable without touching generated endpoints
- All 45 existing tests MUST pass without modification
- Generated endpoint names (`.WithName("AddPet")`) used for policy mapping

**Scale/Scope**: 
- Petstore API: 11 endpoints (3 tags: Pet, Store, User)
- 2 authorization policies (ReadAccess, WriteAccess)
- 11 endpoint-to-policy mappings (in filter configuration)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Principle I: Inheritance-First Architecture
- **Status**: PASS
- **Rationale**: Feature operates at generated code level (test artifacts), not generator implementation. Modifies templates via new `useAuthentication` flag similar to existing `useMediatr`/`useValidators` flags.

### ✅ Principle II: Test-Driven Refactoring
- **Status**: PASS
- **Rationale**: All 45 existing xUnit tests MUST pass without modification (GATE requirement). New Bruno tests for authorization scenarios will be added following existing patterns.

### ✅ Principle III: Template Reusability
- **Status**: PASS
- **Rationale**: Model templates remain unchanged. Only `program.mustache`, `endpointExtensions.mustache`, and potentially `api.mustache` require conditional blocks for authorization configuration.

### ✅ Principle IV: Phase-Gated Progression
- **Status**: PASS
- **Rationale**: Following /speckit.plan workflow (Phase 0: Research, Phase 1: Design, Phase 2: Tasks). Constitution check performed before research phase as required.

### ✅ Principle V: Build Tool Integration
- **Status**: PASS
- **Rationale**: All commands continue using `devbox run task <task-name>` pattern. No changes to build tooling required.

## Project Structure

### Documentation (this feature)

```text
specs/009-endpoint-auth-filter/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command - NEXT)
├── data-model.md        # Phase 1 output (/speckit.plan command - NEXT)
├── quickstart.md        # Phase 1 output (/speckit.plan command - NEXT)
├── contracts/           # Phase 1 output (/speckit.plan command - NEXT)
│   ├── PermissionEndpointFilter.md   # IEndpointFilter implementation contract
│   ├── AuthorizedEndpointExtensions.md   # Extension method contract
│   └── ProgramConfigChanges.md       # Authentication/authorization wiring contract
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Test Artifacts Directory (petstore-tests/ - SOURCE for test stubs)

petstore-tests/
├── TestHandlers/                  # [EXISTING] Test MediatR handlers
│   ├── GetPetByIdQueryHandler.cs
│   ├── AddPetCommandHandler.cs
│   └── ...
├── TestExtensions/                # [EXISTING] Test service registration
│   └── ServiceCollectionExtensions.cs
├── Auth/                          # [NEW] Authorization test artifacts
│   ├── PermissionEndpointFilter.cs        # IEndpointFilter implementation
│   └── AuthorizedEndpointExtensions.cs    # AddAuthorizedApiEndpoints() extension
└── PetstoreApi/                   # [NEW] Test configuration
    └── Program.cs                         # Test Program.cs with builder methods

# Generated Code Structure (test-output/ AFTER running gen:copy-test-stubs)

test-output/
├── Contract/                       # GENERATED - no modifications allowed
│   ├── Endpoints/
│   │   ├── PetEndpoints.cs        # Generated endpoints with .WithName() metadata
│   │   ├── StoreEndpoints.cs
│   │   └── UserEndpoints.cs
│   ├── Extensions/
│   │   └── EndpointExtensions.cs  # Generated: AddApiEndpoints() method [EXISTING]
│   └── ...
│
└── src/PetstoreApi/                # Test artifacts COPIED here by gen:copy-test-stubs
    ├── Filters/
    │   └── PermissionEndpointFilter.cs      # [COPIED from Auth/]
    ├── Extensions/
    │   ├── ServiceCollectionExtensions.cs   # [EXISTING - copied from TestExtensions/]
    │   └── AuthorizedEndpointExtensions.cs  # [COPIED from Auth/]
    ├── Handlers/                    # [EXISTING - copied from TestHandlers/]
    │   └── ...
    └── Program.cs                   # [COPIED from PetstoreApi/ - NOT generated]

# Test Infrastructure (petstore-tests/)

petstore-tests/
├── PetstoreApi.Tests/
│   ├── CustomWebApplicationFactory.cs   # [EXISTING] Test host configuration
│   ├── PetEndpointTests.cs              # [EXISTING] xUnit tests - must pass unchanged
│   └── ...
└── bruno/
    ├── pet/
    │   ├── add-pet.bru               # [EXISTING] Bruno tests
    │   └── ...
    ├── auth-suite/                   # [NEW] Authorization test suite
    │   ├── add-pet-authorized.bru    # [NEW] Test with write claim
    │   ├── add-pet-unauthorized.bru  # [NEW] Test without write claim
    │   ├── delete-pet-unauthorized.bru   # [NEW] Test read claim on write endpoint
    │   ├── update-pet-authorized.bru     # [NEW] Test write claim on PUT
    │   ├── get-pet-authorized.bru        # [NEW] Test read claim on GET
    │   ├── get-pet-unauthenticated.bru   # [NEW] Test no auth
    │   ├── get-inventory-authorized.bru  # [NEW] Test read claim on store
    │   └── get-user-unauthorized.bru     # [NEW] Test no permission claim
    └── bruno.json
```

**Structure Decision**: 
- **NuGet Packaging Mode** (useNugetPackaging=true)
  - Contract assembly: Contains generated endpoints (immutable)
  - PetstoreApi (src/): Receives COPIED test artifacts (filters, extensions, Program.cs)
  - Test artifacts copied via `gen:copy-test-stubs` task (like existing test handlers)
  - Generator changes MINIMAL: only add hook for AuthorizedEndpointExtensions template when useAuthentication=true

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Status**: No violations - complexity tracking not required

All constitution principles satisfied. No additional complexity introduced beyond IEndpointFilter pattern (standard ASP.NET Core feature).

---

## Planning Completion Summary

**Status**: ✅ **COMPLETE** - Ready for `/speckit.tasks` command

### Artifacts Created

#### Phase 0: Research (Complete ✅)
- **research.md**: All 5 technical questions resolved
  - RQ-001: IEndpointFilter implementation pattern
  - RQ-002: EndpointNameMetadata retrieval
  - RQ-003: Route group filter application
  - RQ-004: Authentication/authorization policy configuration
  - RQ-005: Testing strategies (Bruno + xUnit)

#### Phase 1: Design & Contracts (Complete ✅)
- **data-model.md**: 4 entities defined with relationships
  - Authorization Policy
  - Endpoint Name Metadata
  - Endpoint-to-Policy Mapping
  - Permission Claim
  
- **contracts/**: 3 implementation contracts
  - `PermissionEndpointFilter.md`: IEndpointFilter implementation
  - `AuthorizedEndpointExtensions.md`: Extension method wrapper
  - `ProgramConfigChanges.md`: Program.cs configuration updates
  
- **quickstart.md**: Step-by-step developer guide (7 steps, 15-20 minutes)

#### Agent Context (Complete ✅)
- **Updated**: `.github/copilot-instructions.md`
- **Technologies Added**: IEndpointFilter, IAuthorizationService, EndpointNameMetadata

### Key Technical Decisions

1. **Filter Pattern**: Use `IEndpointFilter` with dictionary-based endpoint-to-policy mapping
2. **Metadata Source**: Leverage existing `.WithName()` metadata (no template changes)
3. **Extension Method**: New `AddAuthorizedApiEndpoints()` alongside generated `AddApiEndpoints()`
4. **Policy Configuration**: Extend `useAuthentication` flag, add policy definitions to program.mustache
5. **Immutability**: Zero modifications to Contract package (separation via Implementation package)

### Implementation Approach

**Phase**: Test artifacts first, minimal generator hooks second
1. Create authorization files in `petstore-tests/Auth/`:
   - `PermissionEndpointFilter.cs`
   - `AuthorizedEndpointExtensions.cs`
2. Create/update `petstore-tests/PetstoreApi/Program.cs` with builder methods:
   - `ConfigureAuthServices()` - can be commented/uncommented
   - `ConfigureAuthMiddleware()` - can be commented/uncommented
   - Choice between `AddApiEndpoints()` vs `AddAuthorizedApiEndpoints()`
3. Update `gen:copy-test-stubs` task in Taskfile.yml to copy Auth/ files and Program.cs
4. Verify all 45 existing tests pass
5. Add new authorization tests (Bruno + xUnit)
6. **If successful**: Add MINIMAL generator hook (only AuthorizedEndpointExtensions template, reuse existing useAuthentication flag for policies)

### Validation Gates

- [ ] All 45 existing xUnit tests pass without modification
- [ ] New authorization tests pass (write permission required for POST/PUT/DELETE)
- [ ] New authorization tests pass (read permission required for GET)
- [ ] Generated Contract code remains unchanged (git diff shows no changes)
- [ ] Authorization overhead < 5ms per request
- [ ] Bruno integration tests pass with authentication headers

### Next Command

```bash
# User should run this when ready to implement:
/speckit.tasks
```

This will generate `tasks.md` with granular implementation tasks based on these planning artifacts.

---

**Planning Date**: 2025-02-15  
**Planner**: GitHub Copilot (Claude Sonnet 4.5)  
**Specification**: [spec.md](./spec.md)  
**Feature Branch**: `009-endpoint-auth-filter`
