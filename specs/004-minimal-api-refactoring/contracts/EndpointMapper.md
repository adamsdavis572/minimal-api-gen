# Contract: EndpointMapper.cs.mustache → EndpointMapper.cs

**Template**: `EndpointMapper.cs.mustache` (CREATE - New)  
**Output**: `test-output/src/PetstoreApi/Extensions/EndpointMapper.cs`  
**Status**: CREATE (orchestrates all tag endpoint groups)

---

## Purpose

Generate a single extension method that maps ALL endpoint groups to the application. This provides a centralized registration point called from `Program.cs`.

**Pattern**: Extension method on `IEndpointRouteBuilder` that creates route groups per tag.

---

## Template Structure

```mustache
using {{packageName}}.Endpoints;

namespace {{packageName}}.Extensions;

public static class EndpointMapper
{
    public static IEndpointRouteBuilder MapAllEndpoints(this IEndpointRouteBuilder app)
    {
        {{#routePrefix}}
        var v2 = app.MapGroup("{{routePrefix}}").WithTags("API v2");
        {{#operationsByTag}}
        v2.Map{{tagPascalCase}}Endpoints();
        {{/operationsByTag}}
        {{/routePrefix}}
        {{^routePrefix}}
        {{#operationsByTag}}
        app.Map{{tagPascalCase}}Endpoints();
        {{/operationsByTag}}
        {{/routePrefix}}

        return app;
    }
}
```

---

## Data Structure

### Input: operationsByTag
```json
{
  "routePrefix": "/v2",
  "operationsByTag": [
    {
      "tag": "pet",
      "tagPascalCase": "Pet"
    },
    {
      "tag": "store",
      "tagPascalCase": "Store"
    },
    {
      "tag": "user",
      "tagPascalCase": "User"
    }
  ]
}
```

**Note**: Only tag names needed (not full operation lists).

---

## Template Variables

| Variable | Type | Default | Example | Purpose |
|----------|------|---------|---------|---------|
| `{{packageName}}` | String | Required | `"PetstoreApi"` | Root namespace |
| `{{routePrefix}}` | String | `""` | `"/v2"` | API version prefix (optional) |
| `{{operationsByTag}}` | List | Required | `[{tag: "pet", tagPascalCase: "Pet"}, ...]` | Tag names for endpoint groups |
| `{{tagPascalCase}}` | String | Computed | `"Pet"` | PascalCase tag name |

---

## Conditional Logic

### With Route Prefix
```csharp
var v2 = app.MapGroup("/v2").WithTags("API v2");
v2.MapPetEndpoints();
v2.MapStoreEndpoints();
v2.MapUserEndpoints();
```

**Result**: All endpoints prefixed with `/v2` (e.g., `/v2/pet`, `/v2/store/order`)

### Without Route Prefix
```csharp
app.MapPetEndpoints();
app.MapStoreEndpoints();
app.MapUserEndpoints();
```

**Result**: Endpoints at root (e.g., `/pet`, `/store/order`)

---

## Example Output

**File**: `test-output/src/PetstoreApi/Extensions/EndpointMapper.cs`

```csharp
using PetstoreApi.Endpoints;

namespace PetstoreApi.Extensions;

public static class EndpointMapper
{
    public static IEndpointRouteBuilder MapAllEndpoints(this IEndpointRouteBuilder app)
    {
        var v2 = app.MapGroup("/v2").WithTags("API v2");
        
        v2.MapPetEndpoints();
        v2.MapStoreEndpoints();
        v2.MapUserEndpoints();

        return app;
    }
}
```

---

## Validation Rules

### 1. Extension Method Pattern
```csharp
public static IEndpointRouteBuilder MapAllEndpoints(this IEndpointRouteBuilder app)
{
    // ...
    return app; // Allow method chaining
}
```

**Requirements**:
- Must be `static` method
- Must extend `IEndpointRouteBuilder`
- Must return `IEndpointRouteBuilder` for chaining

### 2. Using Statements
```csharp
using {{packageName}}.Endpoints;
```

**Required**: Import all `{Tag}Endpoints` classes for extension method calls.

### 3. Route Group Configuration
```csharp
var v2 = app.MapGroup("/v2")
    .WithTags("API v2")           // Optional: Swagger tag grouping
    .WithOpenApi();               // Optional: OpenAPI metadata
```

**Chainable Methods**:
- `.WithTags()`: Group endpoints in Swagger UI
- `.WithOpenApi()`: Include in OpenAPI spec
- `.RequireAuthorization()`: Apply auth globally

---

## Dependencies

**Generated Files** (must exist):
- `Endpoints/PetEndpoints.cs` (provides `MapPetEndpoints()`)
- `Endpoints/StoreEndpoints.cs` (provides `MapStoreEndpoints()`)
- `Endpoints/UserEndpoints.cs` (provides `MapUserEndpoints()`)
- ... one per OpenAPI tag

**Framework APIs**:
- `IEndpointRouteBuilder` (.NET 8.0)
- `RouteGroupBuilder` (.NET 8.0)

---

## Java Generator Logic

### postProcessOperationsWithModels()

```java
@Override
public Map<String, ModelsMap> postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
    Map<String, ModelsMap> result = super.postProcessOperationsWithModels(objs, allModels);
    
    // Extract unique tags (lightweight - names only)
    Set<String> uniqueTags = new HashSet<>();
    OperationsMap operations = (OperationsMap) objs;
    List<CodegenOperation> ops = operations.getOperations().getOperation();
    
    for (CodegenOperation op : ops) {
        List<String> tags = op.tags != null && !op.tags.isEmpty() 
            ? op.tags 
            : Arrays.asList("Default");
        
        for (String tag : tags) {
            uniqueTags.add(tag);
        }
    }
    
    // Convert to list of maps for Mustache
    List<Map<String, Object>> tagList = new ArrayList<>();
    for (String tag : uniqueTags) {
        Map<String, Object> tagMap = new HashMap<>();
        tagMap.put("tag", tag);
        tagMap.put("tagPascalCase", camelize(sanitizeName(tag)));
        tagList.add(tagMap);
    }
    
    objs.put("operationsByTag", tagList);
    return result;
}
```

**Note**: Same `operationsByTag` structure used by `TagEndpoints.cs.mustache`, but this template only needs tag names (not full operation lists).

---

## TDD Verification

**Build Test**:
```bash
cd test-output/src/PetstoreApi
devbox run dotnet build
```

Expected: ✅ Build succeeded, no errors

**Endpoint Registration Test**:
```bash
devbox run dotnet run
```

Check logs for endpoint routes:
```
info: Microsoft.Hosting.Lifetime[14]
      Now listening on: http://localhost:5002
info: Microsoft.AspNetCore.Routing.EndpointMiddleware[0]
      Executing endpoint '/v2/pet'
```

**Integration Test**:
```bash
cd test-output/tests/PetstoreApi.Tests
devbox run dotnet test
```

Expected: ✅ 7 tests passed (from Feature 003 baseline)

---

## Alternative Patterns (Not Used)

### 1. Individual MapGroup Per Tag
```csharp
var petGroup = app.MapGroup("/v2/pet").WithTags("Pet");
petGroup.MapPetEndpoints();

var storeGroup = app.MapGroup("/v2/store").WithTags("Store");
storeGroup.MapStoreEndpoints();
```

**Reason Not Used**: Redundant - each `{Tag}Endpoints` class already creates its own group.

### 2. Minimal APIs Without Groups
```csharp
app.MapPost("/v2/pet", ...);
app.MapGet("/v2/pet/{id}", ...);
// ... all inline
```

**Reason Not Used**: Violates US-002 requirement for route groups, poor code organization.

### 3. Attribute Routing
```csharp
[ApiController]
[Route("/v2/[controller]")]
public class PetController : ControllerBase { ... }
```

**Reason Not Used**: Not Minimal API pattern (uses MVC controllers instead).

---

## Complexity Tracking

**Cyclomatic Complexity**: 1 (simple loop, no branches)  
**Lines of Code**: 15-20 (scales with number of tags)  
**Dependencies**: N (one per tag - linear growth)

**Acceptable**: Linear growth matches OpenAPI spec complexity.
