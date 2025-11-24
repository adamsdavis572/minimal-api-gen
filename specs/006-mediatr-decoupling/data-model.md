# Phase 1: Data Model - MediatR Artifacts

**Date**: 2025-11-21  
**Feature**: 006-mediatr-decoupling  
**Purpose**: Define the structure of generated MediatR commands, queries, handlers, and DTOs

## Entity Definitions

### 1. Command

**Purpose**: Represents a state-mutating operation (POST, PUT, PATCH, DELETE)

**Properties**:
- `ClassName`: PascalCase name (e.g., `AddPetCommand`)
- `Namespace`: Generated project namespace (e.g., `PetstoreApi.Commands`)
- `ResponseType`: Return type for IRequest<T> (e.g., `Pet`, `Unit`)
- `Parameters`: List of parameter properties from OpenAPI

**Structure**:
```csharp
public record {ClassName} : IRequest<{ResponseType}>
{
    {#parameters}
    public {dataType} {paramName} { get; init; }
    {/parameters}
}
```

**Validation Rules**:
- Must implement `IRequest<TResponse>`
- Property names must be PascalCase
- All properties use `init` accessor (immutable record)
- ResponseType must match OpenAPI response schema

**State Transitions**: N/A (stateless request object)

**Relationships**:
- One Command per POST/PUT/PATCH/DELETE operation
- Referenced by Handler (1:1)
- Referenced by Endpoint (1:1)

---

### 2. Query

**Purpose**: Represents a state-retrieval operation (GET)

**Properties**:
- `ClassName`: PascalCase name (e.g., `GetPetByIdQuery`)
- `Namespace`: Generated project namespace (e.g., `PetstoreApi.Queries`)
- `ResponseType`: Return type for IRequest<T> (e.g., `Pet`, `IEnumerable<Pet>`)
- `Parameters`: List of path/query/header parameters from OpenAPI

**Structure**:
```csharp
public record {ClassName} : IRequest<{ResponseType}>
{
    {#parameters}
    public {dataType} {paramName} { get; init; }
    {/parameters}
}
```

**Validation Rules**:
- Must implement `IRequest<TResponse>`
- No request body properties (queries don't have bodies)
- Property names must be PascalCase
- All properties use `init` accessor (immutable record)

**State Transitions**: N/A (stateless request object)

**Relationships**:
- One Query per GET operation
- Referenced by Handler (1:1)
- Referenced by Endpoint (1:1)

---

### 3. Handler

**Purpose**: Processes a command or query and contains business logic

**Properties**:
- `ClassName`: PascalCase name (e.g., `AddPetCommandHandler`)
- `Namespace`: Generated project namespace (e.g., `PetstoreApi.Handlers`)
- `RequestType`: Command or Query class name (e.g., `AddPetCommand`)
- `ResponseType`: Return type (e.g., `Pet`, `Unit`)
- `IsGenerated`: Boolean indicating if file is managed or user-owned

**Structure**:
```csharp
public class {ClassName} : IRequestHandler<{RequestType}, {ResponseType}>
{
    // TODO: Add dependencies via constructor injection
    
    public async Task<{ResponseType}> Handle({RequestType} request, CancellationToken cancellationToken)
    {
        // TODO: Implement {operationId} logic
        {#hasReturnType}
        return default({ResponseType});
        {/hasReturnType}
        {^hasReturnType}
        return Unit.Value;
        {/hasReturnType}
    }
}
```

**Validation Rules**:
- Must implement `IRequestHandler<TRequest, TResponse>`
- Handle method must be async and return `Task<TResponse>`
- File must NOT be regenerated if it exists (protected by .openapi-generator-ignore)

**State Transitions**:
1. **Generated**: File created with TODO comments on first generation
2. **User-Owned**: Developer implements logic, file becomes protected
3. **Protected**: Subsequent generations skip this file

**Relationships**:
- One Handler per Command/Query (1:1)
- Registered in DI container (1:1 with interface)

---

### 4. CommandDto (Optional - for complex request bodies)

**Purpose**: Data Transfer Object for command request bodies

**Properties**:
- `ClassName`: PascalCase name (e.g., `CreatePetDto`)
- `Namespace`: Generated project namespace (e.g., `PetstoreApi.DTOs`)
- `Properties`: List of properties from requestBody schema

**Structure**:
```csharp
public record {ClassName}
{
    {#properties}
    public {dataType} {propertyName} { get; init; }
    {/properties}
}
```

**Validation Rules**:
- Properties match OpenAPI requestBody schema exactly
- Property names must be PascalCase
- All properties use `init` accessor (immutable record)

**State Transitions**: N/A (stateless DTO)

**Relationships**:
- Referenced by Command properties
- May have FluentValidation validator (if useValidators=true)

**Decision Note**: Per research, we'll **flatten DTO properties into Command** rather than nesting DTOs. This simplifies templates and reduces file count. DTOs only generated if explicitly needed for validation separation.

---

### 5. Endpoint (Modified Existing Entity)

**Purpose**: Minimal API route definition that delegates to MediatR

**Properties**:
- `HttpMethod`: GET, POST, PUT, DELETE, etc.
- `Route`: Path template (e.g., `/pet/{petId}`)
- `OperationId`: PascalCase operation name
- `RequestType`: Command or Query class name (when useMediatr=true)
- `HasMediatr`: Boolean flag for conditional rendering

**Structure (when useMediatr=true)**:
```csharp
group.Map{HttpMethod}("{route}", async (IMediator mediator{#parameters}, {dataType} {paramName}{/parameters}) =>
{
    {#hasValidation}
    var validationResult = await validator.ValidateAsync(dto);
    if (!validationResult.IsValid)
    {
        return Results.ValidationProblem(validationResult.ToDictionary());
    }
    {/hasValidation}
    
    var {requestVar} = new {RequestType}
    {
        {#parameters}
        {PropertyName} = {paramName},
        {/parameters}
    };
    
    var result = await mediator.Send({requestVar}, cancellationToken);
    {#responseHandling}
    return Results.{HttpStatusMethod}(result);
    {/responseHandling}
})
.WithName("{operationId}")
.WithSummary("{summary}")
.Produces<{responseType}>({statusCode});
```

**Structure (when useMediatr=false)**:
```csharp
group.Map{HttpMethod}("{route}", ({#parameters}{dataType} {paramName}{#hasMore}, {/hasMore}{/parameters}) =>
{
    // TODO: Implement {operationId} logic
    {#hasReturnType}
    return Results.Ok(new {returnType}());
    {/hasReturnType}
    {^hasReturnType}
    return Results.NoContent();
    {/hasReturnType}
})
.WithName("{operationId}")
.WithSummary("{summary}");
```

**Validation Rules**:
- Must inject IMediator when useMediatr=true
- Must NOT have inline business logic when useMediatr=true
- Must have TODO comment when useMediatr=false

**Relationships**:
- References Command or Query (1:1)
- Registered in EndpointMapper

---

## Data Flow (useMediatr=true)

```
HTTP Request
    ↓
Endpoint (api.mustache) 
    ↓ [validates if useValidators=true]
    ↓ [creates Command/Query from parameters]
    ↓
IMediator.Send(request)
    ↓
Handler.Handle(request, cancellationToken)
    ↓ [business logic - user implemented]
    ↓
return Result
    ↓
Endpoint returns Results.{Status}(result)
    ↓
HTTP Response
```

## Data Flow (useMediatr=false)

```
HTTP Request
    ↓
Endpoint (api.mustache)
    ↓ [TODO: Implement logic]
    ↓
return Results.{Status}(defaultValue)
    ↓
HTTP Response
```

---

## OpenAPI → MediatR Mapping Examples

### Example 1: POST /pet (Create)

**OpenAPI**:
```yaml
/pet:
  post:
    operationId: addPet
    requestBody:
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Pet'
    responses:
      '201':
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Pet'
```

**Generated Command**:
```csharp
public record AddPetCommand : IRequest<Pet>
{
    public string Name { get; init; }
    public string[] PhotoUrls { get; init; }
    public Category? Category { get; init; }
    public Tag[]? Tags { get; init; }
    public PetStatus Status { get; init; }
}
```

**Generated Handler**:
```csharp
public class AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>
{
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        // TODO: Implement addPet logic
        // Example: Save to database, assign ID, return created entity
        throw new NotImplementedException();
    }
}
```

**Generated Endpoint**:
```csharp
group.MapPost("/pet", async (IMediator mediator, [FromBody] AddPetCommand command) =>
{
    var result = await mediator.Send(command, cancellationToken);
    return Results.Created($"/pet/{result.Id}", result);
})
.WithName("AddPet")
.Produces<Pet>(201);
```

---

### Example 2: GET /pet/{petId} (Read by ID)

**OpenAPI**:
```yaml
/pet/{petId}:
  get:
    operationId: getPetById
    parameters:
      - name: petId
        in: path
        required: true
        schema:
          type: integer
          format: int64
    responses:
      '200':
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Pet'
```

**Generated Query**:
```csharp
public record GetPetByIdQuery : IRequest<Pet>
{
    public long PetId { get; init; }
}
```

**Generated Handler**:
```csharp
public class GetPetByIdQueryHandler : IRequestHandler<GetPetByIdQuery, Pet>
{
    public async Task<Pet> Handle(GetPetByIdQuery request, CancellationToken cancellationToken)
    {
        // TODO: Implement getPetById logic
        // Example: Query from database by request.PetId
        throw new NotImplementedException();
    }
}
```

**Generated Endpoint**:
```csharp
group.MapGet("/pet/{petId}", async (IMediator mediator, long petId) =>
{
    var query = new GetPetByIdQuery { PetId = petId };
    var result = await mediator.Send(query, cancellationToken);
    return result is not null ? Results.Ok(result) : Results.NotFound();
})
.WithName("GetPetById")
.Produces<Pet>(200)
.Produces(404);
```

---

### Example 3: DELETE /pet/{petId} (Delete)

**OpenAPI**:
```yaml
/pet/{petId}:
  delete:
    operationId: deletePet
    parameters:
      - name: petId
        in: path
        required: true
        schema:
          type: integer
          format: int64
      - name: api_key
        in: header
        required: false
        schema:
          type: string
    responses:
      '204':
        description: Successful operation
```

**Generated Command**:
```csharp
public record DeletePetCommand : IRequest<Unit>
{
    public long PetId { get; init; }
    public string? ApiKey { get; init; }
}
```

**Generated Handler**:
```csharp
public class DeletePetCommandHandler : IRequestHandler<DeletePetCommand, Unit>
{
    public async Task<Unit> Handle(DeletePetCommand request, CancellationToken cancellationToken)
    {
        // TODO: Implement deletePet logic
        // Example: Delete from database by request.PetId
        return Unit.Value;
    }
}
```

**Generated Endpoint**:
```csharp
group.MapDelete("/pet/{petId}", async (IMediator mediator, long petId, [FromHeader(Name = "api_key")] string? apiKey) =>
{
    var command = new DeletePetCommand { PetId = petId, ApiKey = apiKey };
    var result = await mediator.Send(command, cancellationToken);
    return Results.NoContent();
})
.WithName("DeletePet")
.Produces(204);
```

---

## Summary

This data model defines 5 key entities:
1. **Command** - State mutation requests (POST/PUT/DELETE)
2. **Query** - State retrieval requests (GET)
3. **Handler** - Business logic processors (user-implemented)
4. **CommandDto** - Optional DTOs (simplified by flattening)
5. **Endpoint** - Route definitions (modified existing)

The model supports both `useMediatr=true` (full CQRS) and `useMediatr=false` (simple stubs) through conditional template logic.

Next: Create template contracts in `contracts/` directory.
