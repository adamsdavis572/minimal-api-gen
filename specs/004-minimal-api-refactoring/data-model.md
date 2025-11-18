# Data Model: operationsByTag Structure & Template Variables

**Feature**: 004-minimal-api-refactoring  
**Phase**: 1 (Design & Contracts)  
**Date**: 2025-11-14

## Purpose

Define the data structures passed from Java generator to Mustache templates for Minimal API code generation, with focus on the `operationsByTag` grouping mechanism.

---

## Core Data Structures

### 1. operationsByTag Map

**Type**: `Map<String, List<CodegenOperation>>`  
**Purpose**: Group all API operations by their OpenAPI tag for generating one endpoint class per tag  
**Created In**: `postProcessOperationsWithModels()` method  
**Consumed By**: `TagEndpoints.cs.mustache`

**Structure**:
```java
{
    "pet": [
        CodegenOperation { operationId="addPet", httpMethod="POST", path="/pet", ... },
        CodegenOperation { operationId="getPetById", httpMethod="GET", path="/pet/{petId}", ... },
        CodegenOperation { operationId="updatePet", httpMethod="PUT", path="/pet", ... },
        CodegenOperation { operationId="deletePet", httpMethod="DELETE", path="/pet/{petId}", ... },
        // ... more pet operations
    ],
    "store": [
        CodegenOperation { operationId="placeOrder", httpMethod="POST", path="/store/order", ... },
        // ... more store operations
    ],
    "user": [
        CodegenOperation { operationId="createUser", httpMethod="POST", path="/user", ... },
        // ... more user operations
    ]
}
```

**Edge Cases**:
- **Untagged Operations**: Assigned to "Default" tag
- **Multi-Tagged Operations**: Added to list for EACH tag (operation appears in multiple classes)
- **Empty Tag List**: Falls back to "Default" tag

**Validation Rules**:
- At least one operation must exist per tag (enforced by OpenAPI spec typically)
- Tag names are case-sensitive
- Tag names must be valid C# identifier-safe strings (convert to PascalCase)

---

### 2. CodegenOperation Fields (Relevant for Minimal API)

**Source**: OpenAPI Generator's `CodegenOperation` class  
**Used In**: Loop within `operationsByTag` in templates

| Field | Type | Example | Purpose | Mustache Variable |
|-------|------|---------|---------|-------------------|
| `operationId` | String | `"addPet"` | Unique operation identifier | `{{operationId}}` |
| `operationIdPascalCase` | String | `"AddPet"` | PascalCase for C# method names | `{{operationIdPascalCase}}` |
| `httpMethod` | String | `"Post"` | HTTP method (Post/Get/Put/Delete) | `{{httpMethod}}` |
| `path` | String | `"/pet"` | URL path template | `{{path}}` |
| `summary` | String | `"Add a new pet"` | Operation description | `{{summary}}` |
| `tags` | List<String> | `["pet"]` | OpenAPI tags | `{{#tags}}{{.}}{{/tags}}` |
| `allParams` | List<CodegenParameter> | `[{name: "pet", dataType: "Pet", ...}]` | All parameters | `{{#allParams}}...{{/allParams}}` |
| `responses` | List<CodegenResponse> | `[{code: "201", dataType: "Pet", ...}]` | Response definitions | `{{#responses}}...{{/responses}}` |
| `returnType` | String | `"Pet"` | Primary success response type | `{{returnType}}` |
| `hasAuthMethods` | Boolean | `false` | Requires authentication | `{{#hasAuthMethods}}...{{/hasAuthMethods}}` |

---

### 3. CodegenParameter Fields

**Used In**: Endpoint method signatures  
**Loop**: `{{#allParams}}...{{/allParams}}`

| Field | Type | Example | Purpose | Mustache Variable |
|-------|------|---------|---------|-------------------|
| `paramName` | String | `"petId"` | Parameter name (camelCase) | `{{paramName}}` |
| `dataType` | String | `"long"` | C# type | `{{dataType}}` |
| `isPathParam` | Boolean | `true` | From URL path | `{{#isPathParam}}...{{/isPathParam}}` |
| `isQueryParam` | Boolean | `false` | From query string | `{{#isQueryParam}}...{{/isQueryParam}}` |
| `isBodyParam` | Boolean | `false` | From request body | `{{#isBodyParam}}...{{/isBodyParam}}` |
| `isHeaderParam` | Boolean | `false` | From HTTP header | `{{#isHeaderParam}}...{{/isHeaderParam}}` |
| `required` | Boolean | `true` | Parameter is required | `{{#required}}...{{/required}}` |
| `description` | String | `"ID of pet"` | Parameter documentation | `{{description}}` |

**Parameter Binding** (Minimal API):
- **Path**: Bound automatically from route template (`{petId}`)
- **Query**: Bound automatically from query string
- **Body**: Use `[FromBody]` attribute or named parameter
- **Header**: Use `[FromHeader]` attribute

---

### 4. CodegenResponse Fields

**Used In**: `.Produces<T>()` and `.ProducesProblem()` method chains  
**Loop**: `{{#responses}}...{{/responses}}`

| Field | Type | Example | Purpose | Mustache Variable |
|-------|------|---------|---------|-------------------|
| `code` | String | `"201"` | HTTP status code | `{{code}}` |
| `dataType` | String | `"Pet"` | Response body type | `{{dataType}}` |
| `is2xx` | Boolean | `true` | Success response | `{{#is2xx}}...{{/is2xx}}` |
| `is4xx` | Boolean | `false` | Client error | `{{#is4xx}}...{{/is4xx}}` |
| `is5xx` | Boolean | `false` | Server error | `{{#is5xx}}...{{/is5xx}}` |
| `message` | String | `"Invalid input"` | Response description | `{{message}}` |

**Response Patterns** (Minimal API):
- **2xx**: `.Produces<DataType>(code)`
- **4xx/5xx**: `.ProducesProblem(code)` or `.ProducesValidationProblem(code)`

---

## Template Variable Mappings

### TagEndpoints.cs.mustache Variables

| Variable | Type | Source | Example Value | Purpose |
|----------|------|--------|---------------|---------|
| `{{packageName}}` | String | CLI option | `"PetstoreApi"` | Root namespace |
| `{{operationsByTag}}` | Map | postProcessOperations | `{"pet": [...]}` | Grouped operations |
| `{{tag}}` | String | Map key | `"pet"` | Current tag being iterated |
| `{{tagPascalCase}}` | String | Computed | `"Pet"` | Tag in PascalCase for class name |
| `{{operations}}` | List | Map value | `[CodegenOperation, ...]` | Operations for current tag |
| `{{operation}}` | CodegenOperation | List item | `{operationId: "addPet", ...}` | Current operation |

### EndpointMapper.cs.mustache Variables

| Variable | Type | Source | Example Value | Purpose |
|----------|------|--------|---------------|---------|
| `{{packageName}}` | String | CLI option | `"PetstoreApi"` | Root namespace |
| `{{operationsByTag}}` | Map | postProcessOperations | `{"pet": [...], "store": [...]}` | All tag groups |
| `{{tag}}` | String | Map key | `"pet"` | Tag name for method call |
| `{{tagPascalCase}}` | String | Computed | `"Pet"` | Tag in PascalCase for method name |

### program.mustache Variables

| Variable | Type | Source | Example Value | Purpose |
|----------|------|--------|---------------|---------|
| `{{packageName}}` | String | CLI option | `"PetstoreApi"` | Assembly name |
| `{{useAuthentication}}` | Boolean | CLI option | `false` | Enable JWT auth |
| `{{useResponseCaching}}` | Boolean | CLI option | `false` | Enable response caching |
| `{{useProblemDetails}}` | Boolean | CLI option | `true` | Use RFC 9457 errors |
| `{{routePrefix}}` | String | CLI option | `"/v2"` | API version prefix |

### project.csproj.mustache Variables

| Variable | Type | Source | Example Value | Purpose |
|----------|------|--------|---------------|---------|
| `{{packageName}}` | String | CLI option | `"PetstoreApi"` | Project name |
| `{{useAuthentication}}` | Boolean | CLI option | `false` | Include JWT packages |

---

## Data Flow

```text
OpenAPI Spec (petstore.yaml)
    ↓
[Java Generator]
    ↓
MinimalApiServerCodegen.postProcessOperationsWithModels()
    ├── Extract operations from OperationsMap
    ├── Group by tag into Map<String, List<CodegenOperation>>
    ├── Handle untagged → "Default" tag
    ├── Compute PascalCase tag names
    └── Add "operationsByTag" to objs
    ↓
[Mustache Templates]
    ├── TagEndpoints.cs.mustache
    │   ├── Loop: {{#operationsByTag}}
    │   ├── Generate: {TagPascalCase}Endpoints class
    │   ├── Generate: Map{TagPascalCase}Endpoints() method
    │   └── Generate: group.MapPost/Get/Put/Delete() calls
    ├── EndpointMapper.cs.mustache
    │   ├── Loop: {{#operationsByTag}}
    │   └── Generate: v2.MapPetEndpoints(), v2.MapStoreEndpoints(), etc.
    └── program.mustache
        └── Call: app.MapAllEndpoints()
    ↓
Generated C# Code (test-output/src/PetstoreApi/)
```

---

## Computed Fields Required

**Java Generator Responsibilities**:
1. **tagPascalCase**: Convert OpenAPI tag to PascalCase
   - Example: `"pet"` → `"Pet"`, `"store-order"` → `"StoreOrder"`
2. **operationIdPascalCase**: Convert operation ID to PascalCase
   - Example: `"addPet"` → `"AddPet"`, `"findPetsByStatus"` → `"FindPetsByStatus"`
3. **returnType**: Extract primary success response type (first 2xx response)
   - Example: `responses[{code: "201", dataType: "Pet"}]` → `"Pet"`
4. **successCode**: Extract primary success status code
   - Example: `201` for POST, `200` for GET/PUT, `204` for DELETE

**Implementation** (MinimalApiServerCodegen.java):
```java
@Override
public Map<String, ModelsMap> postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
    Map<String, ModelsMap> result = super.postProcessOperationsWithModels(objs, allModels);
    
    // Get operations
    OperationsMap operations = (OperationsMap) objs;
    List<CodegenOperation> ops = operations.getOperations().getOperation();
    
    // Group by tag
    Map<String, Object> operationsByTag = new HashMap<>();
    Map<String, List<CodegenOperation>> grouped = new HashMap<>();
    
    for (CodegenOperation op : ops) {
        List<String> tags = op.tags != null && !op.tags.isEmpty() 
            ? op.tags 
            : Arrays.asList("Default");
        
        for (String tag : tags) {
            String tagPascal = camelize(sanitizeName(tag));
            
            // Compute additional fields
            op.vendorExtensions.put("operationIdPascalCase", camelize(op.operationId));
            op.vendorExtensions.put("tagPascalCase", tagPascal);
            
            // Extract return type and success code
            for (CodegenResponse response : op.responses) {
                if (response.is2xx) {
                    op.vendorExtensions.put("returnType", response.dataType);
                    op.vendorExtensions.put("successCode", response.code);
                    break;
                }
            }
            
            grouped.computeIfAbsent(tag, k -> new ArrayList<>()).add(op);
        }
    }
    
    // Convert to list of maps for Mustache iteration
    List<Map<String, Object>> tagList = new ArrayList<>();
    for (Map.Entry<String, List<CodegenOperation>> entry : grouped.entrySet()) {
        Map<String, Object> tagMap = new HashMap<>();
        tagMap.put("tag", entry.getKey());
        tagMap.put("tagPascalCase", camelize(sanitizeName(entry.getKey())));
        tagMap.put("operations", entry.getValue());
        tagList.add(tagMap);
    }
    
    objs.put("operationsByTag", tagList);
    return result;
}
```

---

## Validation Checklist

- [ ] All operations grouped correctly by tag
- [ ] PascalCase conversion works for all tag names (including hyphens, underscores)
- [ ] Untagged operations assigned to "Default" tag
- [ ] Multi-tagged operations appear in all relevant tag groups
- [ ] returnType extracted from first 2xx response
- [ ] successCode matches returnType's status code
- [ ] All CodegenOperation fields accessible in templates
- [ ] Template variables match Java data structure field names
