# Feature 007-config-fixes: DTO Validation Architecture - Documentation Summary

**Branch**: `007-config-fixes` | **Status**: Design Complete | **Date**: 2025-12-16

## Quick Navigation

- **[spec.md](spec.md)** - Feature specification with 5 user stories, 33 functional requirements, 12 success criteria
- **[plan.md](plan.md)** - Implementation plan with Phase 0 research, Phase 1 design (templates), Phase 2 tasks
- **[data-model.md](data-model.md)** - Entity catalog: DTOs, Validators, Commands, Queries, Handlers, Models
- **[contracts/](contracts/)** - Concrete code examples:
  - [dto-examples.md](contracts/dto-examples.md) - 6 DTO class examples
  - [dto-validator-examples.md](contracts/dto-validator-examples.md) - 4 validator examples with constraint mapping

---

## What Changed from 006-mediatr-decoupling?

### Breaking Change: Commands Now Reference DTOs (Not Models)

**Before (006)**:
```csharp
public record AddPetCommand : IRequest<Pet>
{
    public Pet pet { get; init; }  // ❌ References Model directly
}
```

**After (007)**:
```csharp
public record AddPetCommand : IRequest<Pet>
{
    public AddPetDto pet { get; init; }  // ✅ References DTO
}
```

### New Responsibility: Handlers Map DTO→Model

**Before (006)**:
```csharp
public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
{
    // request.pet is Pet Model - ready to use
}
```

**After (007)**:
```csharp
public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
{
    // Map DTO to Model
    var pet = new Pet 
    { 
        Id = request.pet.Id,
        Name = request.pet.Name,
        // ...all properties
    };
    
    // TODO: Business logic with Model
}
```

### New Architecture: Separate DTOs + Validation

```
Before (006):
Endpoint → Command(Model) → Handler → Business Logic

After (007):
Endpoint → Command(DTO) → Validate DTO → Handler → Map DTO→Model → Business Logic
```

---

## Feature Priorities

### P0: DTO Architecture (Blocking - Must Fix)
- Generate DTOs/ directory separate from Models/
- Commands/Queries reference DTOs (not Models)
- Handlers map DTO→Model
- **Why P0**: Fundamental CQRS violation from 006 - Commands shouldn't reference domain Models directly

### P1: FluentValidation on DTOs (High Value)
- Generate validators for DTOs with 7 constraint types
- Validate at API boundary before handlers
- Return 400 with RFC 7807 ProblemDetails on validation failure
- **Why P1**: FluentValidation always included but never used - delivers immediate value

### P1: Enhanced Petstore Schema (Testing Enabler)
- Add minLength, maxLength, pattern, minimum, maximum, minItems, maxItems to petstore.yaml
- Covers all 7 constraint types for validation testing
- **Why P1**: Can't test validators without comprehensive test data

### P2: Exception Handler (Production Ready)
- Implement UseExceptionHandler middleware
- Catch ValidationException → 400
- Catch unhandled exceptions → 500 with ProblemDetails
- **Why P2**: Flag exists and defaults to true but does nothing

### P3: Configuration Cleanup (Technical Debt)
- Remove unused useRouteGroups flag
- Reduce configuration surface
- **Why P3**: Cleanup task - doesn't add functionality

---

## Implementation Status

### ✅ Completed (Design Phase)

1. **Research Complete** (Phase 0):
   - Analyzed FastEndpoints validation approach (Request wrappers + validators)
   - Analyzed AspNetCore generator approach ([Required] attributes on Models)
   - Identified CQRS violation in 006 (Commands reference Models)
   - Decided on true CQRS with separate DTOs

2. **Specification Complete** ([spec.md](spec.md)):
   - 5 user stories with acceptance scenarios
   - 33 functional requirements (FR-000 to FR-033)
   - 12 success criteria with measurable outcomes
   - Edge cases documented

3. **Plan Complete** ([plan.md](plan.md)):
   - Phase 0: 7 key research findings documented
   - Phase 1: Complete template designs:
     - dto.mustache (DTO class generation)
     - dtoValidator.mustache (7 constraint types mapped)
     - command.mustache modifications (reference DTOs)
     - handler.mustache modifications (DTO→Model mapping)
     - Enhanced petstore.yaml design (6 constraint examples)
     - Exception handler design

4. **Data Model Complete** ([data-model.md](data-model.md)):
   - 6 entity definitions (DTO, Validator, Command, Query, Handler, Model)
   - Entity relationship diagram
   - DTO vs Model comparison table
   - Template context structures
   - Breaking changes documented

5. **Contract Examples Complete** ([contracts/](contracts/)):
   - 6 DTO examples (AddPetDto, CategoryDto, TagDto, UpdatePetDto, PlaceOrderDto, CreateUserDto)
   - 4 Validator examples showing all 7 constraint types
   - Constraint mapping reference table
   - Validation execution flow
   - Error response examples

### ❌ Pending (Implementation Phase)

1. **Template Creation**:
   - dto.mustache (new file)
   - dtoValidator.mustache (new file)
   - Modify command.mustache (reference DTOs)
   - Modify query.mustache (reference DTOs)
   - Modify handler.mustache (add DTO→Model mapping)
   - Modify program.mustache (add exception handler, validator registration)
   - Modify project.csproj.mustache (conditional FluentValidation packages)

2. **Generator Logic**:
   - Add DTO generation logic to MinimalApiServerCodegen.java
   - Add validator generation logic
   - Populate template context with DTO information
   - Remove useRouteGroups flag

3. **Petstore Schema Enhancement**:
   - Update petstore-tests/petstore.yaml with 6 constraint examples

4. **Testing**:
   - Update baseline tests (Commands now have DTOs, not Models)
   - Create new validator tests
   - Test all 8 configuration combinations
   - Verify Bruno tests pass with validation

---

## 7 Validation Constraint Types

| OpenAPI | FluentValidation | Example |
|---------|------------------|---------|
| required | NotEmpty() | `RuleFor(x => x.Name).NotEmpty()` |
| minLength/maxLength | Length(min, max) | `RuleFor(x => x.Name).Length(1, 100)` |
| pattern | Matches(regex) | `RuleFor(x => x.Email).Matches("^[a-zA-Z0-9._%+-]+@...")` |
| minimum | GreaterThanOrEqualTo(N) | `RuleFor(x => x.Quantity).GreaterThanOrEqualTo(1)` |
| maximum | LessThanOrEqualTo(N) | `RuleFor(x => x.Quantity).LessThanOrEqualTo(1000)` |
| minItems/maxItems | Must(x => x.Count...) | `RuleFor(x => x.PhotoUrls).Must(x => x.Count >= 1)` |
| nested object | SetValidator() | `RuleFor(x => x.Category).SetValidator(new CategoryDtoValidator())` |

---

## Next Steps

### For Implementation (Phase 2)

1. **Read** [plan.md](plan.md) Phase 2 task breakdown
2. **Create** dto.mustache and dtoValidator.mustache templates
3. **Modify** existing templates (command, handler, program, project)
4. **Update** MinimalApiServerCodegen.java with DTO generation logic
5. **Enhance** petstore.yaml with validation constraints
6. **Test** with `devbox run mvn clean package && ./run-generator.sh --additional-properties useValidators=true`
7. **Verify** DTOs/ and Validators/ directories created in test-output/
8. **Update** baseline tests to expect DTOs in Commands
9. **Run** Bruno tests to verify 400 validation responses

### For Review

- Review [spec.md](spec.md) for requirements clarity
- Review [plan.md](plan.md) template designs for completeness
- Review [contracts/](contracts/) examples for accuracy
- Approve breaking change from 006 (Commands reference DTOs)

---

## Configuration Matrix (8 Combinations)

| useMediatr | useValidators | useGlobalExceptionHandler | useProblemDetails | Result |
|-----------|--------------|--------------------------|-------------------|--------|
| false | false | false | false | Minimal API only (no MediatR, no DTOs, no validation) |
| false | false | false | true | Minimal API + ProblemDetails |
| false | false | true | false | Minimal API + exception handler (JSON) |
| false | false | true | true | Minimal API + exception handler (ProblemDetails) |
| true | false | false | false | MediatR + DTOs (no validation) |
| true | false | false | true | MediatR + DTOs + ProblemDetails (no validation) |
| true | false | true | false | MediatR + DTOs + exception handler (JSON) |
| **true** | **true** | **true** | **true** | **Full stack (recommended)** |

---

## Key Files to Generate

```
test-output/src/PetstoreApi/
├── DTOs/                          # NEW directory
│   ├── AddPetDto.cs              # NEW
│   ├── UpdatePetDto.cs           # NEW
│   ├── CategoryDto.cs            # NEW
│   ├── TagDto.cs                 # NEW
│   ├── PlaceOrderDto.cs          # NEW
│   └── CreateUserDto.cs          # NEW
├── Validators/                    # NEW directory
│   ├── AddPetDtoValidator.cs     # NEW
│   ├── UpdatePetDtoValidator.cs  # NEW
│   ├── CategoryDtoValidator.cs   # NEW
│   ├── PlaceOrderDtoValidator.cs # NEW
│   └── CreateUserDtoValidator.cs # NEW
├── Commands/
│   └── AddPetCommand.cs          # MODIFIED (references AddPetDto, not Pet)
├── Handlers/
│   └── AddPetHandler.cs          # MODIFIED (adds DTO→Model mapping)
├── Program.cs                     # MODIFIED (exception handler, validator registration)
└── PetstoreApi.csproj            # MODIFIED (conditional FluentValidation packages)
```

---

## Success Verification

### DTO Generation
```bash
cd generator && devbox run mvn clean package
./run-generator.sh --additional-properties useMediatr=true,useValidators=true
ls -la test-output/src/PetstoreApi/DTOs/        # Should see 5+ DTO files
ls -la test-output/src/PetstoreApi/Validators/  # Should see 5+ Validator files
```

### Validator Constraint Verification
```bash
grep -r "Length(1, 100)" test-output/src/PetstoreApi/Validators/  # minLength/maxLength
grep -r "Matches" test-output/src/PetstoreApi/Validators/         # pattern
grep -r "GreaterThanOrEqualTo" test-output/src/PetstoreApi/Validators/  # minimum
grep -r "SetValidator" test-output/src/PetstoreApi/Validators/    # nested validation
```

### Bruno API Test
```bash
cd bruno/OpenAPI\ Petstore
curl -X POST http://localhost:5198/v2/pet \
  -H "Content-Type: application/json" \
  -d '{"name":"","photoUrls":[]}' \
  -i | head -20
# Should return 400 with validation errors in ProblemDetails format
```

---

## References

- **FastEndpoints** generator: `/path/to/openapi-generator/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/`
- **AspNetCore** generator: `/path/to/openapi-generator/modules/openapi-generator/src/main/resources/aspnetcore/`
- **FluentValidation** docs: https://docs.fluentvalidation.net/
- **RFC 7807** ProblemDetails: https://tools.ietf.org/html/rfc7807
- **CQRS** pattern: https://martinfowler.com/bliki/CQRS.html

---

**Status**: All design documentation complete. Ready for implementation (Phase 2).

**Command to Start Implementation**: (TBD - awaiting implementation tooling decision)
