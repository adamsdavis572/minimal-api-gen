# Phase 0: Research - MediatR Implementation Decoupling

**Date**: 2025-11-21  
**Feature**: 006-mediatr-decoupling  
**Purpose**: Research MediatR patterns, template design decisions, and implementation approaches

## Research Tasks

### R1: MediatR CQRS Pattern in .NET 8 Minimal APIs

**Reference**: https://raghavendramurthy.com/posts/cqrs-mediatr-in-net/

**Decision**: Use standard MediatR request/handler pattern with IRequest<TResponse> and IRequestHandler<TRequest, TResponse>

**Key Findings**:
1. **Commands** (state mutations):
   - Implement `IRequest<TResponse>` where TResponse is the operation result
   - Properties match OpenAPI request body + path/query parameters
   - Example: `public record CreateEmployeeCommand : IRequest<int>` returns employee ID

2. **Queries** (state retrieval):
   - Implement `IRequest<TResponse>` where TResponse is the data model
   - Properties match OpenAPI path/query parameters only (no body)
   - Example: `public record GetEmployeeByIdQuery(int Id) : IRequest<Employee>`

3. **Handlers**:
   - Implement `IRequestHandler<TRequest, TResponse>`
   - Async methods: `Task<TResponse> Handle(TRequest request, CancellationToken cancellationToken)`
   - Business logic lives here, NOT in endpoints

4. **Endpoints** (Minimal API):
   ```csharp
   app.MapPost("/employees", async (IMediator mediator, CreateEmployeeCommand command) =>
   {
       var employeeId = await mediator.Send(command);
       return Results.Created($"/employees/{employeeId}", employeeId);
   });
   ```

5. **Registration**:
   ```csharp
   builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
   ```

**Rationale**: This is the standard .NET MediatR pattern with proven track record. Article demonstrates real-world implementation that we'll adapt for code generation.

**Alternatives Considered**:
- Using MediatR notifications (INotification) - rejected, out of scope per spec
- Custom mediator implementation - rejected, MediatR is battle-tested
- CQRS without MediatR - rejected, loses clean separation of concerns

---

### R2: DTO Generation Strategy

**Decision**: Generate separate DTO classes for command/query contracts (Design Decision Q3:B)

**Rationale**:
1. **Separation of Concerns**: DTOs represent the API contract (what comes in), Models represent domain entities (what exists in system)
2. **Validation Flexibility**: DTOs can have different validation rules than models (e.g., CreatePetDto requires name, but Pet model might allow null after creation)
3. **Evolution**: API contracts can evolve independently from domain models
4. **Best Practice**: Article shows separate EmployeeDto vs Employee model

**Implementation Approach**:
- **For Commands**: Generate `Create{Model}Dto` and `Update{Model}Dto` from requestBody schema
  - Example: POST /pet with Pet requestBody → `CreatePetDto` + `AddPetCommand` references CreatePetDto
- **For Queries**: Usually no separate DTO needed (queries typically return full models)
  - Exception: If query has complex body (rare), generate `{Operation}QueryDto`
- **Mapping**: Commands contain DTO as property or flatten DTO properties into command
  - Chosen: **Flatten** - `AddPetCommand` has all Pet properties directly (simpler for generated code)

**Template Requirements**:
- New `commandDto.mustache` for Create/Update DTOs
- Logic in MinimalApiServerCodegen to detect requestBody and generate DTO class name
- Command/Query templates reference these DTOs via properties

---

### R3: Template Conditional Logic for useMediatr Flag

**Decision**: Use Mustache conditional sections `{{#useMediatr}}...{{/useMediatr}}` in existing templates

**Implementation Strategy**:
1. **api.mustache** (endpoint file):
   ```mustache
   {{#useMediatr}}
   // MediatR delegation
   var command = new {{commandClass}}(...);
   var result = await mediator.Send(command, cancellationToken);
   return Results.Created(...);
   {{/useMediatr}}
   {{^useMediatr}}
   // TODO: Implement {{operationId}} logic
   return Results.Ok(new {{returnType}}());
   {{/useMediatr}}
   ```

2. **program.mustache**:
   ```mustache
   {{#useMediatr}}
   builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
   {{/useMediatr}}
   ```

3. **project.csproj.mustache**:
   ```mustache
   {{#useMediatr}}
   <PackageReference Include="MediatR" Version="12.2.0" />
   {{/useMediatr}}
   ```

**Rationale**: Mustache's built-in conditionals are simple and well-understood. Avoids creating entirely separate template sets.

**Template Organization**:
- Conditional blocks in existing templates for simple changes
- New templates (command.mustache, query.mustache, handler.mustache) only generated when useMediatr=true
- MinimalApiServerCodegen controls which templates are processed based on flag

---

### R4: Handler Regeneration Protection

**Decision**: Use OpenAPI Generator's `.openapi-generator-ignore` patterns (Design Decision Q2:A)

**Implementation**:
1. **First Generation**: 
   - Generate all handlers with `@Generated` annotation or special comment
   - Add `Handlers/**/*.cs` to `.openapi-generator-ignore` file automatically

2. **Subsequent Generations**:
   - OpenAPI Generator framework skips files matching ignore patterns
   - Developers can edit handlers without fear of overwriting

3. **Force Regeneration** (if needed):
   - Developer removes handler file or removes from .openapi-generator-ignore
   - Next generation recreates the scaffold

**Code Implementation in MinimalApiServerCodegen.java**:
```java
@Override
public void processOpts() {
    super.processOpts();
    
    if (useMediatr) {
        // Add handler protection to ignore file
        supportingFiles.add(new SupportingFile("openapi-generator-ignore.mustache",
            "", ".openapi-generator-ignore"));
    }
}
```

**Template for .openapi-generator-ignore**:
```
# Protect handler implementations from regeneration
Handlers/**/*.cs
```

**Rationale**: This is the standard OpenAPI Generator pattern for protecting user code. Simple, well-documented, requires no custom logic.

---

### R5: Response Type Mapping from OpenAPI to MediatR IRequest<T>

**Decision**: Match OpenAPI response schemas exactly (Design Decision Q4:A)

**Mapping Rules**:
1. **200/201 with schema**: `IRequest<ModelType>`
   - Example: GET /pet/{id} returns Pet → `IRequest<Pet>`
   - Example: POST /pet returns Pet → `IRequest<Pet>`

2. **204 No Content**: `IRequest<Unit>`
   - Example: DELETE /pet/{id} → `IRequest<Unit>`
   - Note: MediatR.Unit is the "void" equivalent

3. **Multiple success responses**: Use primary success response (200/201)
   - Example: POST can return 200 or 201 → use 201 schema
   - Rationale: Actual HTTP status code set in endpoint, not handler

4. **Array responses**: `IRequest<IEnumerable<ModelType>>`
   - Example: GET /pets returns Pet[] → `IRequest<IEnumerable<Pet>>`

5. **Primitive responses**: `IRequest<int>`, `IRequest<string>`, etc.
   - Example: Operation returns integer ID → `IRequest<int>`

**Implementation in MinimalApiServerCodegen.java**:
```java
private String getMediatrResponseType(CodegenOperation operation) {
    if (operation.returnType == null || operation.returnType.equals("void")) {
        return "Unit"; // MediatR.Unit for void operations
    }
    
    if (operation.returnContainer != null && operation.returnContainer.equals("array")) {
        return "IEnumerable<" + operation.returnBaseType + ">";
    }
    
    return operation.returnType; // Direct model type or primitive
}
```

**Rationale**: Accurate type mapping ensures generated code compiles and matches API contract. Handlers can return the exact type specified in OpenAPI spec.

---

### R6: Validation Integration Strategy

**Decision**: Keep FluentValidation inline in endpoints (Design Decision Q5:A)

**Rationale**:
1. **Simplicity**: Current generator already supports `useValidators` flag with inline validation
2. **No Breaking Changes**: Moving validation to MediatR pipeline requires ValidationBehavior setup (out of scope for this feature)
3. **Flexibility**: Developers can manually add pipeline behaviors later if desired

**Generated Code Pattern** (when useMediatr=true AND useValidators=true):
```csharp
group.MapPost("/pet", async (IMediator mediator, [FromBody] CreatePetDto dto, IValidator<CreatePetDto> validator) =>
{
    var validationResult = await validator.ValidateAsync(dto);
    if (!validationResult.IsValid)
    {
        return Results.ValidationProblem(validationResult.ToDictionary());
    }
    
    var command = new AddPetCommand { /* map dto properties */ };
    var result = await mediator.Send(command);
    return Results.Created($"/pet/{result.Id}", result);
})
```

**Future Enhancement** (Out of Scope):
- Add `useMediatrPipeline` flag to generate ValidationBehavior and LoggingBehavior
- Move validation into MediatR pipeline for cleaner endpoints

---

### R7: Command/Query Property Mapping from OpenAPI Parameters

**Decision**: Flatten all parameters into command/query properties

**Mapping Strategy**:

1. **Path Parameters** → Command/Query properties:
   ```csharp
   // GET /pet/{petId}
   public record GetPetByIdQuery
   {
       public long PetId { get; init; }
   }
   ```

2. **Query Parameters** → Command/Query properties:
   ```csharp
   // GET /pet/findByStatus?status=available
   public record FindPetsByStatusQuery
   {
       public string[] Status { get; init; } // Array per Phase 5
   }
   ```

3. **Request Body** → Command properties (flattened):
   ```csharp
   // POST /pet with Pet body
   public record AddPetCommand : IRequest<Pet>
   {
       public string Name { get; init; }
       public string[] PhotoUrls { get; init; }
       public Category Category { get; init; }
       // ... all Pet properties
   }
   ```

4. **Header Parameters** → Command/Query properties:
   ```csharp
   public record DeletePetCommand
   {
       public long PetId { get; init; }
       public string ApiKey { get; init; } // from header
   }
   ```

5. **Complex Query Parameters** → Use existing Phase 5 HttpContext pattern:
   ```csharp
   // Endpoint receives HttpContext, deserializes to DTO, creates command
   group.MapGet("/test", async (IMediator mediator, HttpContext httpContext) =>
   {
       var dataJson = httpContext.Request.Query["data"].FirstOrDefault();
       var data = JsonSerializer.Deserialize<Pet>(dataJson);
       
       var query = new TestQuery { Data = data };
       var result = await mediator.Send(query);
       return Results.Ok(result);
   })
   ```

**Template Logic**:
- Loop through `allParams` in command/query templates
- Generate properties with correct C# types (leveraging Phase 5 array conversion)
- Maintain parameter validation attributes from OpenAPI spec

---

### R8: File Naming Conventions

**Decision**: Follow C# conventions and MediatR community patterns

**Naming Rules**:
1. **Commands**: `{Verb}{Model}Command.cs`
   - AddPetCommand, UpdatePetCommand, DeletePetCommand
   - Verb matches operation intent (Add for POST, Update for PUT, Delete for DELETE)

2. **Queries**: `{Verb}{Model}Query.cs` or `Get{Model}By{Criteria}Query.cs`
   - GetPetByIdQuery, FindPetsByStatusQuery, GetAllPetsQuery
   - Use "Get" for single item, "Find" or "GetAll" for collections

3. **Handlers**: `{RequestName}Handler.cs`
   - AddPetCommandHandler, GetPetByIdQueryHandler
   - Matches request name with "Handler" suffix

4. **DTOs**: `{Verb}{Model}Dto.cs`
   - CreatePetDto, UpdatePetDto
   - Aligns with command names

**Operation ID to Class Name Mapping**:
- OpenAPI operationId: `addPet` → `AddPetCommand`
- OpenAPI operationId: `getPetById` → `GetPetByIdQuery`
- Use existing `toModelName()` and `camelize()` methods in MinimalApiServerCodegen

---

## Implementation Checklist

Based on research, these items must be implemented:

### Generator Code (MinimalApiServerCodegen.java)
- [ ] Add `useMediatr` CLI option (boolean, default false)
- [ ] Add `getMediatrResponseType()` method for IRequest<T> mapping
- [ ] Add `generateCommandClassName()` method for naming
- [ ] Add `generateQueryClassName()` method for naming
- [ ] Add logic to register new templates (command, query, handler, dto)
- [ ] Add logic to populate command/query data models with parameters
- [ ] Update `processOpts()` to conditionally add MediatR templates
- [ ] Add `.openapi-generator-ignore` generation for handler protection

### New Templates
- [ ] `command.mustache` - Command class generation
- [ ] `query.mustache` - Query class generation
- [ ] `handler.mustache` - Handler scaffold generation
- [ ] `commandDto.mustache` - DTO generation (if needed)
- [ ] `mediatrRegistration.mustache` - DI extension method

### Modified Templates
- [ ] `api.mustache` - Add conditional MediatR delegation vs TODO stubs
- [ ] `program.mustache` - Add conditional MediatR registration
- [ ] `project.csproj.mustache` - Add conditional MediatR package reference
- [ ] Remove vendor extension logic (x-isAddPet, etc.)

### Testing
- [ ] Verify useMediatr=false maintains current behavior (7 tests pass)
- [ ] Implement test handlers for useMediatr=true (7 tests pass with handlers)
- [ ] Verify handler files not regenerated on second run
- [ ] Verify MediatR package only in csproj when useMediatr=true

---

## Next Steps

Proceed to **Phase 1: Design & Contracts** to create:
1. `data-model.md` - Command/Query/Handler entity definitions
2. `contracts/` - Detailed Mustache template contracts
3. `quickstart.md` - Usage examples for developers
