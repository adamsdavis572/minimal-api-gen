# Implementation Plan: DTO Validation Architecture

**Branch**: `007-config-fixes` | **Date**: 2025-12-12 | **Updated**: 2025-12-16 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/007-config-fixes/spec.md`

## Summary

Implement true CQRS with separate DTOs and comprehensive validation architecture:

1. **P0: DTO Architecture** - Generate separate DTO classes for API contracts, Commands/Queries reference DTOs (not Models), maintain Models for domain logic
2. **P1: FluentValidation on DTOs** - Generate validators for DTOs with comprehensive OpenAPI constraint support (minLength, maxLength, pattern, minimum, maximum, minItems, maxItems)
3. **P1: Enhanced Petstore Schema** - Update petstore.yaml with diverse validation constraints for testing
4. **P2: Exception Handler** - Implement ASP.NET Core's UseExceptionHandler middleware with ValidationException handling
5. **P3: Configuration Cleanup** - Remove unused useRouteGroups flag

**Technical Approach**: 
- Generate DTOs/ directory separate from Models/ for true CQRS separation
- DTOs are API contract (immutable records matching requestBody schema)
- Models are domain entities (may differ from DTOs)
- Validators target DTOs at API boundary (before MediatR handlers)
- Handlers responsible for DTO→Model mapping
- Validation occurs before business logic execution

**This fixes technical debt from 006-mediatr-decoupling** where Commands referenced Models directly instead of DTOs, violating CQRS principles.

## Technical Context

**Language/Version**: Java 11 (generator build), C# 11+ / .NET 8.0 (generated code)  
**Primary Dependencies**:  
- Generator: OpenAPI Generator framework 7.x, Maven 3.8.9+, Mustache template engine  
- Generated Code: FluentValidation 11.9.0, ASP.NET Core 8.0+, MediatR 12.x (from 006)  
**Storage**: File system (templates in JAR, generated code output to disk)  
**Testing**: xUnit + FluentAssertions (from baseline test suite - feature 003), Bruno CLI (API tests)  
**Target Platform**: .NET 8.0+ server applications (Linux/Windows/macOS)  
**Project Type**: Code generator with mustache templates  
**Performance Goals**: Generator execution <5 seconds for petstore spec, generated validation <1ms per request, DTO mapping overhead <0.5ms  
**Constraints**:  
- Must maintain backwards compatibility when useMediatr=false (no DTOs, use Models)
- Must preserve existing Models/ directory and classes (unchanged)
- DTOs must exactly match OpenAPI requestBody schemas (no modifications)
- All builds through devbox run commands (constitution requirement)  
- Test-driven: baseline tests must pass with DTO refactoring (constitution requirement)  
- Validators MUST target DTOs, not Models (CQRS separation)
**Scale/Scope**: 
- 14 configuration options (plus DTO generation logic)
- ~20 operations in petstore spec → 5+ unique DTOs
- 15+ validation constraints across schemas (after petstore.yaml enhancements)
- 7 constraint types to map to FluentValidation rules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

✅ **I. Inheritance-First Architecture**  
- Generator extends AbstractCSharpCodegen (established in feature 002)
- Changes are strategic overrides, not reimplementation
- **Status**: PASS - modifications to existing generator class methods

✅ **II. Test-Driven Refactoring**  
- Baseline test suite exists from feature 003
- Tests must be updated to expect DTOs in Commands (breaking change from 006)
- Validators must validate DTOs before handlers execute
- Exception handling validated through integration tests
- Bruno tests verify 400 responses for validation failures
- **Status**: PASS - will follow RED-GREEN-REFACTOR with DTO migration

✅ **III. Template Reusability**  
- Model templates remain unchanged (domain models separate from DTOs)
- DTO templates similar to Model templates but target requestBody schemas
- Validator templates target DTOs, not Models
- Command/Query templates updated to reference DTOs instead of Models
- **Status**: PASS - new DTO templates, model templates untouched

✅ **IV. Phase-Gated Progression**  
- Phase 0: Research DTO generation patterns, validation constraint mapping, nested validation
- Phase 1: Design DTO template structure, validator template with all constraint types, Command/DTO integration
- Phase 2: Implementation with test validation (expect test updates for DTO changes)
- **Status**: PASS - following gated approach

✅ **V. Build Tool Integration**  
- All Maven commands via `devbox run mvn`
- All dotnet commands via `devbox run dotnet`
- **Status**: PASS - documented in commands

**Overall Gate Status**: ✅ PASS - No violations, proceed to Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/007-config-fixes/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output - template comparison analysis
├── data-model.md        # Phase 1 output - validator class structure
├── quickstart.md        # Phase 1 output - usage examples
├── contracts/           # Phase 1 output - generated code samples
└── tasks.md             # Phase 2 output - NOT created by /speckit.plan
```

### Source Code (repository root)

```text
generator/
├── src/main/java/org/openapitools/codegen/languages/
│   └── MinimalApiServerCodegen.java           # Modify: add DTO + validator generation logic
├── src/main/resources/aspnet-minimalapi/
│   ├── dto.mustache                            # CREATE: new template for DTO classes
│   ├── dtoValidator.mustache                   # CREATE: new template for DTO validators
│   ├── command.mustache                        # Modify: reference DTOs instead of Models
│   ├── query.mustache                          # Modify: reference DTOs instead of Models
│   ├── handler.mustache                        # Modify: add DTO→Model mapping logic
│   ├── api.mustache                            # Modify: validate DTOs before MediatR
│   ├── program.mustache                        # Modify: conditional FluentValidation registration
│   ├── project.csproj.mustache                 # Modify: conditional FluentValidation packages
│   └── README.md                               # UPDATE: document DTO architecture + flags
└── pom.xml                                      # No changes

petstore-tests/
└── petstore.yaml                                # ENHANCE: add validation constraints

test-output/                                     # Generated code for validation
└── src/PetstoreApi/
    ├── DTOs/                                    # NEW: generated DTO classes
    │   ├── AddPetDto.cs
    │   ├── UpdatePetDto.cs
    │   ├── CategoryDto.cs
    │   └── Validators/
    │       ├── AddPetDtoValidator.cs
    │       ├── CategoryDtoValidator.cs
    │       └── [...]
    ├── Models/                                  # UNCHANGED: domain models
    │   ├── Pet.cs
    │   └── [...]
    ├── Commands/
    │   └── AddPetCommand.cs                     # Modified: references AddPetDto
    ├── Handlers/
    │   └── AddPetCommandHandler.cs              # Modified: maps DTO → Model
    ├── Program.cs                               # Modified: validator registration
    └── PetstoreApi.csproj                       # Modified: conditional packages

docs/
├── CONFIGURATION.md                             # UPDATE: document validator generation
└── CONFIGURATION_ANALYSIS.md                    # UPDATE: mark issues resolved

petstore-tests/
└── PetstoreApi.Tests/
    ├── ValidationTests.cs                       # NEW: test validator behavior
    └── ExceptionHandlerTests.cs                 # NEW: test exception responses
```

**Structure Decision**: Code generator pattern with mustache templates. All modifications constrained to generator code and templates. Generated output validated through test projects.

---

## Phase 0: Research & Template Analysis

**Status**: ✅ COMPLETE

**Deliverable**: [research.md](research.md)

**Key Findings**:
1. **DTO Architecture**: AspNetCore uses Models directly with [Required] attributes, FastEndpoints uses records with constructor params, true CQRS needs separate DTOs
2. **Validation Location**: AspNetCore validates Models via attributes, FastEndpoints validates Request wrappers, CQRS should validate DTOs at API boundary
3. **Template Patterns**: Can generate DTOs similar to Models but target requestBody schemas instead of component schemas
4. **Constraint Mapping**: OpenAPI constraints map cleanly to FluentValidation rules (minLength→Length, pattern→Matches, minimum→GreaterThan, etc.)
5. **Nested Validation**: FluentValidation SetValidator() chains validators for nested DTOs
6. **Exception Handling**: ASP.NET Core middleware can distinguish ValidationException (400) from other exceptions (500)
7. **OpenAPI Generator**: Framework provides access to schema constraints via CodegenProperty properties

**Decisions Made**:
- **DTO Generation**: Create separate dto.mustache template targeting requestBody schemas (not component schemas like Model)
- **DTO Structure**: C# records (like Commands/Queries) for immutability, exact match to requestBody schema
- **Validator Target**: DTOs, not Models - validates API contract before domain logic
- **Validator Generation**: Create dtoValidator.mustache with comprehensive constraint mapping (7 types)
- **Command/Query Changes**: Reference DTOs via properties (e.g., `public AddPetDto pet { get; init; }`)
- **Handler Responsibility**: Manual DTO→Model mapping (no AutoMapper, keeps it explicit)
- **Exception Handler**: Standard UseExceptionHandler with ValidationException→400, others→500
- **Petstore Schema**: Enhance with 6+ constraint examples to test all validation types
- **Implementation Order**: P0 (DTOs) → P1 (Validators + Schema) → P2 (Exception Handler) → P3 (Cleanup)

---

## Phase 1: Design & Contracts

**Status**: ✅ COMPLETE

**Deliverables**:
- [data-model.md](data-model.md) - DTO and validator class structures
- [contracts/dto-examples.md](contracts/dto-examples.md) - Generated DTO samples
- [contracts/dto-validator-examples.md](contracts/dto-validator-examples.md) - Generated validator samples with all constraint types
- [contracts/command-dto-integration.md](contracts/command-dto-integration.md) - Commands referencing DTOs
- [contracts/handler-mapping-examples.md](contracts/handler-mapping-examples.md) - Handler DTO→Model mapping
- [contracts/exception-handler-examples.md](contracts/exception-handler-examples.md) - Middleware samples
- [quickstart.md](quickstart.md) - User documentation and migration guide from 006

**Design Decisions**:

### 1. DTO Template Structure

**Location**: `generator/src/main/resources/aspnet-minimalapi/dto.mustache`

**Content** (similar to model.mustache but targets requestBody):
```mustache
// <auto-generated>
// DTO generated from OpenAPI requestBody schema
// </auto-generated>

namespace {{packageName}}.DTOs;

/// <summary>
/// {{description}}
/// </summary>
public record {{classname}}
{
{{#vars}}
    /// <summary>
    /// {{description}}
    /// </summary>
    public {{{dataType}}}{{^required}}?{{/required}} {{name}} { get; init; }{{#defaultValue}} = {{{.}}};{{/defaultValue}}
{{/vars}}
}
```

**Invocation**: Generate during `processOperations()` for each unique requestBody schema

---

### 2. DTO Validator Template Structure

**Location**: `generator/src/main/resources/aspnet-minimalapi/dtoValidator.mustache`

**Content** (comprehensive constraint mapping):
```mustache
using FluentValidation;

namespace {{packageName}}.DTOs.Validators;

/// <summary>
/// Validator for {{classname}} DTO
/// </summary>
public class {{classname}}Validator : AbstractValidator<{{classname}}>
{
    public {{classname}}Validator()
    {
    {{#vars}}
        {{#required}}
        RuleFor(x => x.{{name}}).NotEmpty();
        {{/required}}
        {{#minLength}}{{#maxLength}}
        RuleFor(x => x.{{name}}).Length({{minLength}}, {{maxLength}});
        {{/maxLength}}{{/minLength}}
        {{#pattern}}
        RuleFor(x => x.{{name}}).Matches("{{pattern}}");
        {{/pattern}}
        {{#minimum}}
        RuleFor(x => x.{{name}}).GreaterThanOrEqualTo({{minimum}});
        {{/minimum}}
        {{#maximum}}
        RuleFor(x => x.{{name}}).LessThanOrEqualTo({{maximum}});
        {{/maximum}}
        {{#minItems}}{{#maxItems}}
        RuleFor(x => x.{{name}}).Must(x => x != null && x.Count >= {{minItems}} && x.Count <= {{maxItems}})
            .WithMessage("{{name}} must have between {{minItems}} and {{maxItems}} items");
        {{/maxItems}}{{/minItems}}
        {{#complexType}}
        RuleFor(x => x.{{name}}).SetValidator(new {{dataType}}Validator());
        {{/complexType}}
    {{/vars}}
    }
}
```

**Invocation**: Generate for each DTO when `useValidators==true`

---

### 3. Command/Query Template Modifications

**Location**: `generator/src/main/resources/aspnet-minimalapi/command.mustache` and `query.mustache`

**Change**: Reference DTOs instead of Models for body parameters

**Before** (006-mediatr-decoupling):
```csharp
public record AddPetCommand : IRequest<Pet>
{
    public Pet pet { get; init; }  // ← References Model directly
}
```

**After** (007-config-fixes):
```csharp
public record AddPetCommand : IRequest<Pet>
{
    public AddPetDto pet { get; init; }  // ← References DTO
}
```

**Template Logic**:
```mustache
{{#bodyParam}}
    public {{#isDto}}{{dtoType}}{{/isDto}}{{^isDto}}{{dataType}}{{/isDto}} {{paramName}} { get; init; }
{{/bodyParam}}
```

---

### 4. Handler Template Modifications

**Location**: `generator/src/main/resources/aspnet-minimalapi/handler.mustache`

**Change**: Add DTO→Model mapping responsibility

**Before** (006-mediatr-decoupling):
```csharp
public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
{
    // TODO: Implement AddPet logic
    // request.pet is already a Pet Model
}
```

**After** (007-config-fixes):
```csharp
public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
{
    // TODO: Map DTO to Model
    var pet = new Pet
    {
        Id = request.pet.Id,
        Name = request.pet.Name,
        PhotoUrls = request.pet.PhotoUrls,
        // ... map all properties
    };
    
    // TODO: Implement AddPet business logic with Model
    return pet;
}
```

**Template Logic**:
```mustache
{{#hasBodyParam}}
    // TODO: Map DTO to Model
    var {{modelVar}} = new {{responseType}}
    {
        {{#bodyParam}}{{#vars}}
        {{name}} = request.{{bodyParamName}}.{{name}},
        {{/vars}}{{/bodyParam}}
    };
{{/hasBodyParam}}
```

---

### 5. Enhanced Petstore Schema Design

**Location**: `petstore-tests/petstore.yaml`

**Changes**: Add validation constraints to test all FluentValidation rule types

**Pet Schema Enhancements**:
```yaml
Pet:
  type: object
  required:
    - name
    - photoUrls
  properties:
    name:
      type: string
      minLength: 1        # NEW
      maxLength: 100      # NEW
    photoUrls:
      type: array
      items:
        type: string
      minItems: 1         # NEW
      maxItems: 10        # NEW
```

**User Schema Enhancements**:
```yaml
User:
  properties:
    username:
      type: string
      minLength: 3        # NEW
      maxLength: 50       # NEW
    email:
      type: string
      pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'  # NEW
```

**Order Schema Enhancements**:
```yaml
Order:
  properties:
    quantity:
      type: integer
      format: int32
      minimum: 1          # NEW
      maximum: 1000       # NEW
```

**Category Schema Enhancements**:
```yaml
Category:
  properties:
    name:
      type: string
      minLength: 1        # NEW
```

---

### 6. Exception Handler Design

**Middleware Pattern** (in program.mustache):
```mustache
{{#useGlobalExceptionHandler}}
app.UseExceptionHandler(exceptionHandlerApp =>
{
    exceptionHandlerApp.Run(async context =>
    {
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        {{#useProblemDetails}}
        context.Response.ContentType = "application/problem+json";
        
        var exceptionHandlerPathFeature = context.Features.Get<IExceptionHandlerPathFeature>();
        var exception = exceptionHandlerPathFeature?.Error;
        
        var problemDetails = new ProblemDetails
        {
            Status = StatusCodes.Status500InternalServerError,
            Title = "An error occurred while processing your request",
            Detail = exception?.Message ?? "An unexpected error occurred"
        };
        
        await context.Response.WriteAsJsonAsync(problemDetails);
        {{/useProblemDetails}}
        {{^useProblemDetails}}
        context.Response.ContentType = "application/json";
        
        var errorResponse = new
        {
            error = "Internal Server Error",
            message = exception?.Message ?? "An unexpected error occurred"
        };
        
        await context.Response.WriteAsJsonAsync(errorResponse);
        {{/useProblemDetails}}
    });
});
{{/useGlobalExceptionHandler}}
```

---

### 3. Conditional Package References

**In project.csproj.mustache**:
```mustache
{{#useValidators}}
<PackageReference Include="FluentValidation" Version="11.9.0" />
<PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />
{{/useValidators}}
```

**In program.mustache**:
```mustache
{{#useValidators}}
builder.Services.AddValidatorsFromAssemblyContaining<Program>();
{{/useValidators}}
```

---

### 4. Flag Removal Changes

**MinimalApiServerCodegen.java modifications**:
1. Delete line 52: `private boolean useRouteGroups = true;`
2. Delete lines 246-252: `setUseRouteGroups()` method
3. Remove CLI option registration (search for "USE_ROUTE_GROUPS")

**Documentation updates**:
- docs/CONFIGURATION.md: Add note "Route groups (MapGroup) are required architecture"
- docs/CONFIGURATION_ANALYSIS.md: Mark useRouteGroups as resolved

---

## Phase 2: Implementation Plan (Tasks Command)

**Note**: This phase is executed via `/speckit.tasks` command, not `/speckit.plan`.

**High-Level Task Breakdown**:

### Task Group 1: Flag Cleanup (P3 - Lowest Risk)
1. Remove useRouteGroups from MinimalApiServerCodegen.java
2. Update CONFIGURATION.md
3. Update CONFIGURATION_ANALYSIS.md
4. Build generator: `task build-generator`
5. Verify no compilation errors

### Task Group 2: Validator Generation (P1 - Medium Risk)
1. Create validator.mustache template
2. Modify MinimalApiServerCodegen.java to generate validator files
3. Modify program.mustache for conditional validator registration
4. Modify project.csproj.mustache for conditional packages
5. Remove inline validator logic from api.mustache (if present)
6. Build generator: `task build-generator`
7. Generate petstore: `./run-generator.sh --additional-properties useValidators=true`
8. Verify validator files created in test-output/src/PetstoreApi/Validators/
9. Build and test generated code: `task test-server-stubs`
10. Create ValidationTests.cs in petstore-tests
11. Tests run automatically via task test-server-stubs

### Task Group 3: Exception Handler (P2 - Highest Risk)
1. Modify program.mustache to add exception handler middleware
2. Build generator: `task build-generator`
3. Generate petstore: `./run-generator.sh --additional-properties useGlobalExceptionHandler=true,useProblemDetails=true`
4. Verify middleware in test-output/src/PetstoreApi/Program.cs
5. Create ExceptionHandlerTests.cs in petstore-tests
6. Run tests: `task test-server-stubs`

### Task Group 4: Integration Testing
1. Test all configuration combinations (see quickstart.md matrix)
2. Verify backwards compatibility
3. Update baseline test suite if needed

---

## Success Criteria Validation

From spec.md, these criteria must be met:

- **SC-001**: ✅ Generator produces working validator classes - verify by checking Validators/ directory
- **SC-002**: ✅ Generated API rejects invalid requests - verify with curl test returning 400
- **SC-003**: ✅ FluentValidation packages excluded when useValidators=false - verify .csproj
- **SC-004**: ✅ Exception handler returns RFC 7807 errors - verify response has type, title, status, detail fields
- **SC-005**: ✅ Configuration surface reduced by 1 - verify useRouteGroups removed from code/docs
- **SC-006**: ✅ Existing tests pass - run baseline test suite
- **SC-007**: ✅ All configs compile - test 8 configuration matrix combinations
- **SC-008**: ✅ Constraints preserved - verify petstore validators contain rules for 15+ constraints

---

## Post-Phase 1 Constitution Re-Check

*Re-evaluate after design is complete*

✅ **I. Inheritance-First Architecture**  
- Design uses method overrides in MinimalApiServerCodegen
- No reimplementation of base functionality
- **Status**: PASS

✅ **II. Test-Driven Refactoring**  
- ValidationTests.cs and ExceptionHandlerTests.cs planned
- Will follow RED-GREEN-REFACTOR with baseline suite
- **Status**: PASS

✅ **III. Template Reusability**  
- Model templates unchanged in design
- Only operation-level templates (validator.mustache, program.mustache) modified
- **Status**: PASS

✅ **IV. Phase-Gated Progression**  
- Phase 0 research complete
- Phase 1 design complete
- Phase 2 awaits `/speckit.tasks` command
- **Status**: PASS

✅ **V. Build Tool Integration**  
- All commands documented with `devbox run` prefix
- **Status**: PASS

**Overall Re-Check Status**: ✅ PASS - Design adheres to constitution

---

## Known Limitations & Future Enhancements

### Current Implementation (Phase 1)
- **Validator Rules**: Only `.NotEmpty()` for required parameters
- **Exception Detail**: May expose sensitive information in production

### Future Enhancements (Out of Scope)
- Add `.Matches(pattern)` for regex validation
- Add `.Length(min, max)` for string length validation
- Add `.GreaterThanOrEqualTo(min)` / `.LessThanOrEqualTo(max)` for numeric ranges
- Sanitize exception messages in production mode
- Custom validation rule templates

---

## Appendices

### A. Template File Comparison

| Template | aspnet-fastendpoints | aspnet-minimalapi | Changes Needed |
|----------|---------------------|-------------------|----------------|
| request.mustache | Has validator section | N/A (we create validator.mustache) | Extract validator section |
| program.mustache | FastEndpoints config | AddValidatorsFromAssemblyContaining | Add registration |
| project.csproj.mustache | No FluentValidation | Add conditionally | Add package refs |

### B. References

- aspnet-fastendpoints source: https://github.com/OpenAPITools/openapi-generator/tree/main/modules/openapi-generator/src/main/resources/aspnet-fastendpoints
- FluentValidation docs: https://docs.fluentvalidation.net/
- ASP.NET Core exception handling: https://learn.microsoft.com/en-us/aspnet/core/fundamentals/error-handling
- OpenAPI Generator docs: https://openapi-generator.tech/

### C. Agent Context Update

**Status**: Ready to update  
**Command**: `.specify/scripts/bash/update-agent-context.sh copilot`

**Technologies to Add**:
- FluentValidation 11.9.0 (validator library)
- AbstractValidator<T> (base class pattern)
- ASP.NET Core UseExceptionHandler middleware
- ProblemDetails RFC 7807 format

**Note**: No new build tools required. All existing technologies (Java, Maven, C#, .NET) already documented.

---

**END OF PLAN - Ready for `/speckit.tasks` command**
````
```

**Structure Decision**: Code generator pattern with mustache templates. All modifications constrained to generator code and templates. Generated output validated through test projects.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
