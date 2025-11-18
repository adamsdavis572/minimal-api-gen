# Contract: TagEndpoints.cs.mustache → {Tag}Endpoints.cs

**Template**: `TagEndpoints.cs.mustache` (CREATE - New)  
**Output**: `test-output/src/PetstoreApi/Endpoints/{Tag}Endpoints.cs` (one per OpenAPI tag)  
**Status**: CREATE (replaces endpoint.mustache, request.mustache, validators.mustache)

---

## Purpose

Generate one endpoint class per OpenAPI tag using Minimal API route groups. Each class contains:
1. Extension method: `Map{Tag}Endpoints(this RouteGroupBuilder group)`
2. Inline endpoint definitions: `group.MapPost/Get/Put/Delete(...)`
3. Manual FluentValidation: `IValidator<T>` DI + `ValidateAsync()`

---

## Template Structure

```mustache
{{#operationsByTag}}
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using {{packageName}}.Models;
using {{packageName}}.Validators;

namespace {{packageName}}.Endpoints;

public static class {{tagPascalCase}}Endpoints
{
    public static RouteGroupBuilder Map{{tagPascalCase}}Endpoints(this RouteGroupBuilder group)
    {
        {{#operations}}
        group.Map{{httpMethod}}("{{path}}", {{#hasValidator}}async ([FromBody] {{bodyParam.dataType}} request, IValidator<{{bodyParam.dataType}}> validator) => 
        {
            // Manual validation
            var validationResult = await validator.ValidateAsync(request);
            if (!validationResult.IsValid)
            {
                return Results.ValidationProblem(validationResult.ToDictionary());
            }
            
            // Operation logic
            {{#returnType}}
            // TODO: Implement {{operationIdPascalCase}} logic
            var result = new {{returnType}} { /* ... */ };
            return Results.{{resultMethod}}<{{returnType}}>(result);
            {{/returnType}}
            {{^returnType}}
            return Results.{{resultMethod}}();
            {{/returnType}}
        }{{/hasValidator}}{{^hasValidator}}({{#allParams}}{{dataType}} {{paramName}}{{^-last}}, {{/-last}}{{/allParams}}) => 
        {
            // Operation logic
            {{#returnType}}
            // TODO: Implement {{operationIdPascalCase}} logic
            var result = new {{returnType}} { /* ... */ };
            return Results.{{resultMethod}}<{{returnType}}>(result);
            {{/returnType}}
            {{^returnType}}
            return Results.{{resultMethod}}();
            {{/returnType}}
        }{{/hasValidator}})
        .WithName("{{operationIdPascalCase}}")
        .WithSummary("{{summary}}")
        {{#produces}}
        .Produces<{{dataType}}>({{code}})
        {{/produces}}
        {{#errors}}
        .ProducesProblem({{code}})
        {{/errors}};

        {{/operations}}
        return group;
    }
}
{{/operationsByTag}}
```

---

## Data Structure

### Input: operationsByTag
```json
{
  "operationsByTag": [
    {
      "tag": "pet",
      "tagPascalCase": "Pet",
      "operations": [
        {
          "operationId": "addPet",
          "operationIdPascalCase": "AddPet",
          "httpMethod": "Post",
          "path": "/pet",
          "summary": "Add a new pet to the store",
          "hasValidator": true,
          "bodyParam": {
            "paramName": "pet",
            "dataType": "Pet"
          },
          "returnType": "Pet",
          "resultMethod": "Created",
          "successCode": "201",
          "produces": [
            { "code": 201, "dataType": "Pet" }
          ],
          "errors": [
            { "code": 400 },
            { "code": 422 }
          ]
        },
        {
          "operationId": "getPetById",
          "operationIdPascalCase": "GetPetById",
          "httpMethod": "Get",
          "path": "/pet/{petId}",
          "summary": "Find pet by ID",
          "hasValidator": false,
          "allParams": [
            {
              "paramName": "petId",
              "dataType": "long",
              "isPathParam": true
            }
          ],
          "returnType": "Pet",
          "resultMethod": "Ok",
          "successCode": "200",
          "produces": [
            { "code": 200, "dataType": "Pet" }
          ],
          "errors": [
            { "code": 404 }
          ]
        }
        // ... more operations
      ]
    }
    // ... more tags
  ]
}
```

---

## Template Variables

### Tag-Level Variables

| Variable | Type | Source | Example | Purpose |
|----------|------|--------|---------|---------|
| `{{tag}}` | String | OpenAPI tag | `"pet"` | Original tag name |
| `{{tagPascalCase}}` | String | Computed | `"Pet"` | Class name (PascalCase) |
| `{{operations}}` | List | Grouped ops | `[...]` | Operations for this tag |

### Operation-Level Variables

| Variable | Type | Source | Example | Purpose |
|----------|------|--------|---------|---------|
| `{{operationId}}` | String | OpenAPI | `"addPet"` | Unique operation ID |
| `{{operationIdPascalCase}}` | String | Computed | `"AddPet"` | Method name (PascalCase) |
| `{{httpMethod}}` | String | OpenAPI | `"Post"` | HTTP method (Post/Get/Put/Delete/Patch) |
| `{{path}}` | String | OpenAPI | `"/pet"` | URL path template |
| `{{summary}}` | String | OpenAPI | `"Add a new pet"` | Operation description |
| `{{hasValidator}}` | Boolean | Computed | `true` | Body parameter requires validation |
| `{{bodyParam}}` | Object | Extracted | `{paramName: "pet", dataType: "Pet"}` | Request body parameter |
| `{{allParams}}` | List | OpenAPI | `[{paramName: "petId", dataType: "long"}]` | All parameters |
| `{{returnType}}` | String | Extracted | `"Pet"` | Primary success response type |
| `{{resultMethod}}` | String | Computed | `"Created"` | Results.* method name |
| `{{successCode}}` | String | Extracted | `"201"` | Success HTTP status code |
| `{{produces}}` | List | OpenAPI responses | `[{code: 201, dataType: "Pet"}]` | Success responses |
| `{{errors}}` | List | OpenAPI responses | `[{code: 400}, {code: 404}]` | Error responses |

---

## Computed Fields

### 1. hasValidator
**Logic**:
```java
boolean hasValidator = operation.allParams.stream()
    .anyMatch(param -> param.isBodyParam && !param.isPrimitiveType);
```

**Purpose**: Determine if operation needs FluentValidation (body parameter with complex type).

### 2. bodyParam
**Logic**:
```java
CodegenParameter bodyParam = operation.allParams.stream()
    .filter(param -> param.isBodyParam)
    .findFirst()
    .orElse(null);
```

**Purpose**: Extract request body parameter for validator injection.

### 3. resultMethod
**Logic**:
```java
String resultMethod = switch (operation.successCode) {
    case "200" -> "Ok";
    case "201" -> "Created";
    case "202" -> "Accepted";
    case "204" -> "NoContent";
    default -> "StatusCode";
};
```

**Purpose**: Map HTTP status code to `Results.*` method name.

### 4. produces / errors
**Logic**:
```java
List<Map<String, Object>> produces = operation.responses.stream()
    .filter(r -> r.is2xx)
    .map(r -> Map.of("code", r.code, "dataType", r.dataType))
    .toList();

List<Map<String, Object>> errors = operation.responses.stream()
    .filter(r -> r.is4xx || r.is5xx)
    .map(r -> Map.of("code", r.code))
    .toList();
```

**Purpose**: Split responses into success (.Produces) and error (.ProducesProblem) lists.

---

## Example Output

**File**: `test-output/src/PetstoreApi/Endpoints/PetEndpoints.cs`

```csharp
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;
using PetstoreApi.Validators;

namespace PetstoreApi.Endpoints;

public static class PetEndpoints
{
    public static RouteGroupBuilder MapPetEndpoints(this RouteGroupBuilder group)
    {
        // POST /pet - Add a new pet
        group.MapPost("/pet", async ([FromBody] Pet pet, IValidator<Pet> validator) => 
        {
            // Manual validation
            var validationResult = await validator.ValidateAsync(pet);
            if (!validationResult.IsValid)
            {
                return Results.ValidationProblem(validationResult.ToDictionary());
            }
            
            // Operation logic (TODO: Implement via generator logic injection)
            var createdPet = PetStore.AddPet(pet);
            return Results.Created($"/v2/pet/{createdPet.Id}", createdPet);
        })
        .WithName("AddPet")
        .WithSummary("Add a new pet to the store")
        .Produces<Pet>(201)
        .ProducesProblem(400)
        .ProducesProblem(422);

        // GET /pet/{petId} - Get pet by ID
        group.MapGet("/pet/{petId}", (long petId) => 
        {
            var pet = PetStore.GetPetById(petId);
            return pet != null 
                ? Results.Ok(pet) 
                : Results.NotFound();
        })
        .WithName("GetPetById")
        .WithSummary("Find pet by ID")
        .Produces<Pet>(200)
        .ProducesProblem(404);

        // PUT /pet - Update existing pet
        group.MapPut("/pet", async ([FromBody] Pet pet, IValidator<Pet> validator) => 
        {
            var validationResult = await validator.ValidateAsync(pet);
            if (!validationResult.IsValid)
            {
                return Results.ValidationProblem(validationResult.ToDictionary());
            }
            
            var updated = PetStore.UpdatePet(pet);
            return updated 
                ? Results.Ok(pet) 
                : Results.NotFound();
        })
        .WithName("UpdatePet")
        .WithSummary("Update an existing pet")
        .Produces<Pet>(200)
        .ProducesProblem(404)
        .ProducesProblem(422);

        // DELETE /pet/{petId} - Delete pet
        group.MapDelete("/pet/{petId}", ([FromHeader(Name = "ApiKey")] string apiKey, long petId) => 
        {
            // TODO: Validate ApiKey (if required)
            var deleted = PetStore.DeletePet(petId);
            return deleted 
                ? Results.NoContent() 
                : Results.NotFound();
        })
        .WithName("DeletePet")
        .WithSummary("Deletes a pet")
        .Produces(204)
        .ProducesProblem(404);

        return group;
    }
}
```

---

## Validation Rules

### 1. Parameter Binding
- **Path**: Bound automatically from route template (`{petId}`)
- **Body**: Use `[FromBody]` attribute
- **Header**: Use `[FromHeader(Name = "...")]` attribute
- **Query**: Bound automatically from query string

### 2. FluentValidation Pattern
```csharp
var validationResult = await validator.ValidateAsync(request);
if (!validationResult.IsValid)
{
    return Results.ValidationProblem(validationResult.ToDictionary());
}
```

### 3. Response Metadata
- `.WithName()`: Unique operation ID (for URL generation)
- `.WithSummary()`: Swagger documentation
- `.Produces<T>()`: Success response with type
- `.ProducesProblem()`: Error response (RFC 9457)

### 4. Return Types
- `Results.Created()`: 201 with Location header
- `Results.Ok()`: 200 with body
- `Results.NoContent()`: 204 without body
- `Results.NotFound()`: 404
- `Results.ValidationProblem()`: 422 with validation errors

---

## Dependencies

**Generated Files**:
- `Models/{Model}.cs` (data models)
- `Validators/{Model}Validator.cs` (FluentValidation validators)

**Framework APIs**:
- `RouteGroupBuilder` (.NET 8.0)
- `Results` (.NET 8.0 TypedResults)
- `IValidator<T>` (FluentValidation 11.9.0)

**NuGet Packages**:
- `FluentValidation` (11.9.0)
- `Swashbuckle.AspNetCore` (6.5.0)

---

## TDD Verification

**Tests Must Pass** (from Feature 003):
```csharp
[Fact]
public async Task AddPet_ReturnsCreated()
{
    // Test expects 201, Location header, Pet body
}

[Fact]
public async Task GetPetById_ReturnsOk()
{
    // Test expects 200, Pet body
}

[Fact]
public async Task UpdatePet_ReturnsOk()
{
    // Test expects 200, updated Pet body
}

[Fact]
public async Task DeletePet_WithApiKey_ReturnsNoContent()
{
    // Test expects 204, empty body
}

[Fact]
public async Task GetPetById_NotFound()
{
    // Test expects 404
}
```

**Validation Steps**:
1. Generate `PetEndpoints.cs` with template
2. Inject PetStore logic (in-memory CRUD)
3. Run `devbox run dotnet test`
4. If RED → analyze test output, fix template, regenerate
5. Repeat until GREEN (all 7 tests pass)
