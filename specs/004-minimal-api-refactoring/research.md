# Research: Template Refactoring Patterns & Minimal API Best Practices

**Feature**: 004-minimal-api-refactoring  
**Research Phase**: Phase 0  
**Date**: 2025-11-14

## Purpose

Resolve all "NEEDS CLARIFICATION" items from Technical Context and establish patterns for refactoring FastEndpoints generator to Minimal API generator using TDD approach.

---

## Research Areas

### 1. ASP.NET Core Minimal API Patterns

**Question**: What are the core patterns for Minimal API endpoint registration and how do they differ from FastEndpoints?

**Findings**:

**FastEndpoints Pattern** (Current):
```csharp
// One class per operation
public class AddPetEndpoint : Endpoint<AddPetRequest, Pet>
{
    public override void Configure()
    {
        Post("/v2/pet");
        AllowAnonymous();
        WithTags("pet");
    }
    
    public override async Task HandleAsync(AddPetRequest req, CancellationToken ct)
    {
        // Implementation
        await SendCreatedAtAsync<GetPetByIdEndpoint>(new { petId = addedPet.Id }, addedPet, cancellation: ct);
    }
}
```

**Minimal API Pattern** (Target):
```csharp
// Extension method grouping operations by tag
public static class PetEndpoints
{
    public static RouteGroupBuilder MapPetEndpoints(this RouteGroupBuilder group)
    {
        group.MapPost("/pet", AddPet)
             .Produces<Pet>(201)
             .ProducesProblem(400)
             .WithTags("pet");
             
        group.MapGet("/pet/{petId}", GetPetById)
             .Produces<Pet>(200)
             .ProducesProblem(404)
             .WithTags("pet");
             
        // More operations...
        
        return group;
    }
    
    private static async Task<IResult> AddPet(Pet pet, /* dependencies */)
    {
        // Implementation
        return TypedResults.Created($"/pet/{pet.Id}", pet);
    }
    
    private static async Task<IResult> GetPetById(long petId, /* dependencies */)
    {
        // Implementation
        return pet is not null ? TypedResults.Ok(pet) : TypedResults.NotFound();
    }
}
```

**Program.cs registration**:
```csharp
var app = builder.Build();

var v2 = app.MapGroup("/v2");
v2.MapPetEndpoints();
v2.MapStoreEndpoints();
v2.MapUserEndpoints();

app.Run();
```

**Decision**: Use **route group** pattern with extension methods, one class per OpenAPI tag
**Rationale**: 
- Aligns with ASP.NET Core conventions
- Groups related operations logically (by tag)
- Reduces class count (one per tag vs one per operation)
- Maintains testability via `WebApplicationFactory`

**Alternatives Considered**:
- Top-level statements in Program.cs: Rejected - too cluttered for APIs with many operations
- Minimal API with lambda expressions inline: Rejected - reduces testability and code organization
- Carter library: Rejected - adds external dependency, Minimal APIs are sufficient

---

### 2. FluentValidation Integration without FastEndpoints

**Question**: How do we integrate FluentValidation in Minimal APIs without FastEndpoints' built-in validator support?

**Findings**:

**FastEndpoints Approach** (Current):
```csharp
public class AddPetRequestValidator : Validator<AddPetRequest>
{
    public AddPetRequestValidator()
    {
        RuleFor(x => x.pet.Name).NotEmpty();
    }
}

// Auto-discovery and execution via FastEndpoints
```

**Minimal API Approach** (Target):
```csharp
// 1. Define validator
public class PetValidator : AbstractValidator<Pet>
{
    public PetValidator()
    {
        RuleFor(x => x.Name).NotEmpty().WithMessage("Pet name is required");
        RuleFor(x => x.PhotoUrls).NotEmpty().WithMessage("At least one photo URL required");
    }
}

// 2. Register in Program.cs
builder.Services.AddValidatorsFromAssemblyContaining<PetValidator>();

// 3. Manual validation in endpoint
private static async Task<IResult> AddPet(Pet pet, IValidator<Pet> validator)
{
    var validationResult = await validator.ValidateAsync(pet);
    if (!validationResult.IsValid)
    {
        return TypedResults.ValidationProblem(validationResult.ToDictionary());
    }
    
    // Proceed with logic
}
```

**Decision**: Use **manual validation** with dependency injection of validators
**Rationale**:
- Standard FluentValidation patterns
- Explicit validation calls provide control
- Compatible with ASP.NET Core DI
- No additional libraries needed

**Alternatives Considered**:
- FluentValidation.AspNetCore automatic validation: Rejected - requires model binding configuration, adds complexity
- Custom endpoint filter: Rejected - over-engineering for simple use case
- Data annotations: Rejected - less expressive than FluentValidation

---

### 3. Template Transformation Strategy

**Question**: What is the safest incremental approach to refactor templates while maintaining TDD RED-GREEN cycle?

**Findings**:

**Transformation Sequence** (Based on Reusability Matrix from Feature 001):

**Phase A: Infrastructure First (Modify Supporting Templates)**
1. `project.csproj.mustache`: Remove FastEndpoints packages, add Swashbuckle + FluentValidation
2. `program.mustache`: Remove `AddFastEndpoints()`/`UseFastEndpoints()`, add Minimal API setup
3. `readme.mustache`: Update framework references

**Phase B: Delete FastEndpoints Templates**
4. Delete `endpoint.mustache`, `request.mustache`, `requestClass.mustache`, `requestRecord.mustache`
5. Delete `endpointType.mustache`, `endpointRequestType.mustache`, `endpointResponseType.mustache`
6. Delete `loginRequest.mustache`, `userLoginEndpoint.mustache`

**Phase C: Create Minimal API Templates**
7. Create `TagEndpoints.cs.mustache` (generates one class per tag with `MapXxx()` methods)
8. Create `EndpointMapper.cs.mustache` (generates `MapAllEndpoints()` extension method)

**Phase D: Modify Java Generator Logic**
9. `processOpts()`: Remove FastEndpoints CLI options, add Minimal API options
10. `apiTemplateFiles()`: Remove `endpoint.mustache` mapping
11. `supportingFiles()`: Register new `TagEndpoints.cs.mustache` (runs once per tag)
12. `postProcessOperationsWithModels()`: Group operations by tag into `operationsByTag` map

**Decision**: Follow **A → B → C → D** sequence with tests run after each phase
**Rationale**:
- Infrastructure changes first enable Minimal API compilation
- Delete before create avoids file conflicts
- Template creation before Java logic ensures templates exist when referenced
- Each phase has clear validation: does project compile? Do tests run (even if fail)?

**Alternatives Considered**:
- Big-bang replacement: Rejected - too risky, hard to debug failures
- Java logic first: Rejected - templates won't compile without infrastructure changes
- Create before delete: Rejected - may cause file conflicts and confusion

---

### 4. operationsByTag Data Structure Implementation

**Question**: How do we group operations by tag in `postProcessOperationsWithModels()` for template consumption?

**Findings**:

**Current Structure** (FastEndpoints):
```java
// Each operation generates one file via endpoint.mustache
Map<String, String> apiTemplateFiles = new HashMap<>();
apiTemplateFiles.put("endpoint.mustache", "Endpoint.cs");
```

**Target Structure** (Minimal API):
```java
@Override
public Map<String, ModelsMap> postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
    Map<String, ModelsMap> result = super.postProcessOperationsWithModels(objs, allModels);
    
    // Group operations by tag
    OperationsMap operations = (OperationsMap) objs;
    List<CodegenOperation> ops = operations.getOperations().getOperation();
    
    Map<String, List<CodegenOperation>> operationsByTag = new HashMap<>();
    for (CodegenOperation op : ops) {
        List<String> tags = op.tags;
        if (tags == null || tags.isEmpty()) {
            tags = List.of("Default");  // Fallback for untagged operations
        }
        
        for (String tag : tags) {
            operationsByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(op);
        }
    }
    
    objs.put("operationsByTag", operationsByTag);
    return result;
}
```

**Template Consumption** (`TagEndpoints.cs.mustache`):
```mustache
{{#operationsByTag}}
{{#-first}}
using Microsoft.AspNetCore.Http.HttpResults;
using {{packageName}}.Models;

namespace {{packageName}}.Endpoints;

public static class {{tagPascalCase}}Endpoints
{
    public static RouteGroupBuilder Map{{tagPascalCase}}Endpoints(this RouteGroupBuilder group)
    {
{{/-first}}
        {{#operations}}
        group.Map{{httpMethod}}("{{path}}", {{operationIdPascalCase}})
             .Produces<{{returnType}}>({{successCode}})
             {{#errorCodes}}.ProducesProblem({{code}}){{/errorCodes}}
             .WithTags("{{tag}}");
        {{/operations}}
{{#-last}}
        return group;
    }
    
    {{#operations}}
    private static async Task<IResult> {{operationIdPascalCase}}({{#allParams}}{{dataType}} {{paramName}}{{^-last}}, {{/-last}}{{/allParams}})
    {
        // TODO: Implement {{operationIdPascalCase}}
        throw new NotImplementedException();
    }
    {{/operations}}
}
{{/-last}}
{{/operationsByTag}}
```

**Decision**: Use **Map<String, List<CodegenOperation>>** with tag name as key
**Rationale**:
- Natural grouping by OpenAPI tag
- Mustache can iterate over map entries
- Supports operations with multiple tags (loop over tags, add to each list)
- Handles untagged operations with "Default" fallback

**Alternatives Considered**:
- Nested OperationsMap objects: Rejected - overly complex data structure
- Flat list with tag filtering in template: Rejected - Mustache has limited conditional logic
- Single file for all operations: Rejected - violates separation of concerns by tag

---

### 5. Test Suite Compatibility

**Question**: Will Feature 003 tests work unchanged against Minimal API output?

**Findings**:

**Test Structure** (from Feature 003):
```csharp
public class PetEndpointTests : IClassFixture<CustomWebApplicationFactory<Program>>
{
    private readonly HttpClient _client;
    
    public PetEndpointTests(CustomWebApplicationFactory<Program> factory)
    {
        _client = factory.CreateClient();
    }
    
    [Fact]
    public async Task AddPet_WithValidData_Returns201Created()
    {
        var response = await _client.PostAsJsonAsync("/v2/pet", newPet);
        response.StatusCode.Should().Be(HttpStatusCode.Created);
        // ...
    }
}
```

**Compatibility Analysis**:
- ✅ `WebApplicationFactory<Program>` works with Minimal APIs (standard ASP.NET Core)
- ✅ HTTP method and URL structure unchanged (`POST /v2/pet`)
- ✅ Status codes remain the same (201, 200, 204, 404)
- ✅ JSON serialization behavior identical
- ⚠️ **ONLY IF**: Program.cs exposes `public partial class Program {}` (Feature 003 already has this)

**Decision**: Tests require **ZERO modifications** - only regenerate project and rerun
**Rationale**:
- Tests validate HTTP contract, not implementation details
- Minimal APIs produce identical HTTP responses
- `WebApplicationFactory` abstracts framework differences

**Alternatives Considered**: N/A - no alternatives needed, tests work as-is

---

## Key Decisions Summary

| Decision Point | Choice | Rationale |
|----------------|--------|-----------|
| Endpoint Organization | Route groups by tag | Aligns with ASP.NET Core conventions, reduces class count |
| Validation Approach | Manual with DI validators | Standard FluentValidation pattern, explicit control |
| Transformation Sequence | Infrastructure → Delete → Create → Java | Safest incremental path, clear validation at each step |
| operationsByTag Structure | Map<String, List<Operation>> | Natural tag grouping, Mustache-compatible |
| Test Compatibility | Zero modifications | Tests validate HTTP contract, framework-agnostic |

---

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|-----------|
| operationsByTag grouping fails for operations with multiple tags | Medium | High | Loop over all tags per operation, add to each list |
| Untagged operations break template logic | Low | Medium | Use "Default" tag as fallback |
| FluentValidation registration missing breaks validation | Low | High | Add explicit validator registration in program.mustache |
| Generated route paths don't match test URLs | Medium | High | Preserve `/v2` prefix and path structure from OpenAPI spec |
| TypedResults methods not available in .NET 8 | Low | High | Verify Microsoft.AspNetCore.Http.HttpResults namespace availability (added in .NET 7) |

---

## Next Steps (Phase 1)

1. Create `data-model.md`: Document operationsByTag structure, template variables for Minimal API
2. Create `contracts/`: Define transformation contracts for each template
3. Create `quickstart.md`: Document TDD workflow for this refactoring
4. Update agent context with new technologies (Minimal APIs, TypedResults, route groups)
