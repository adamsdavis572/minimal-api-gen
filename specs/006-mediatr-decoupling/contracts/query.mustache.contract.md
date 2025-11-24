# Query Template Contract

**Template File**: `generator/src/main/resources/aspnet-minimalapi/query.mustache`  
**Output Pattern**: `Queries/{QueryClassName}.cs`  
**Generated When**: `useMediatr=true` AND operation is GET

## Input Data Model

```java
{
    "queryClassName": "GetPetByIdQuery",         // PascalCase query name
    "namespace": "PetstoreApi.Queries",          // Project namespace + .Queries
    "responseType": "Pet",                        // IRequest<T> response type
    "isCollection": false,                        // true if returns array/list
    "collectionType": "IEnumerable<Pet>",        // Used if isCollection=true
    "allParams": [                                // Path/query/header params only (no body)
        {
            "paramName": "petId",
            "dataType": "long",
            "isPathParam": true,
            "required": true,
            "description": "ID of pet to return"
        }
    ],
    "imports": [
        "MediatR",
        "PetstoreApi.Models"
    ]
}
```

## Template Structure

```mustache
using MediatR;
{{#imports}}
using {{.}};
{{/imports}}

namespace {{namespace}};

/// <summary>
/// Query for {{operationId}} operation
/// </summary>
public record {{queryClassName}} : IRequest<{{#isCollection}}{{collectionType}}{{/isCollection}}{{^isCollection}}{{responseType}}{{/isCollection}}>
{
    {{#allParams}}
    {{^isBodyParam}}
    {{#description}}
    /// <summary>
    /// {{description}}
    /// </summary>
    {{/description}}
    public {{dataType}} {{baseName}} { get; init; }{{^required}} = default!;{{/required}}
    
    {{/isBodyParam}}
    {{/allParams}}
}
```

## Output Example

**Input**: GET /pet/{petId} operation from petstore.yaml

**Output**: `Queries/GetPetByIdQuery.cs`
```csharp
using MediatR;
using PetstoreApi.Models;

namespace PetstoreApi.Queries;

/// <summary>
/// Query for getPetById operation
/// </summary>
public record GetPetByIdQuery : IRequest<Pet>
{
    /// <summary>
    /// ID of pet to return
    /// </summary>
    public long PetId { get; init; }
}
```

**Input**: GET /pet/findByStatus?status=available,pending

**Output**: `Queries/FindPetsByStatusQuery.cs`
```csharp
using MediatR;
using PetstoreApi.Models;

namespace PetstoreApi.Queries;

/// <summary>
/// Query for findPetsByStatus operation
/// </summary>
public record FindPetsByStatusQuery : IRequest<IEnumerable<Pet>>
{
    /// <summary>
    /// Status values that need to be considered for filter
    /// </summary>
    public string[] Status { get; init; }
}
```

## Validation Rules

1. **File name**: Must match `{queryClassName}.cs` pattern
2. **Namespace**: Must be `{projectName}.Queries`
3. **Record type**: Must use `record` keyword
4. **IRequest<T>**: 
   - Single item: `IRequest<Pet>`
   - Collection: `IRequest<IEnumerable<Pet>>`
5. **No body params**: Queries never have request bodies
6. **Array handling**: Use `T[]` for array query params (Phase 5 convention)

## Generation Logic (MinimalApiServerCodegen.java)

```java
private void processQuery(CodegenOperation operation) {
    // Only for GET operations
    if (!operation.httpMethod.equals("GET")) {
        return;
    }
    
    Map<String, Object> queryModel = new HashMap<>();
    queryModel.put("queryClassName", getQueryClassName(operation));
    queryModel.put("namespace", packageName + ".Queries");
    
    // Handle collection responses
    boolean isCollection = "array".equals(operation.returnContainer) || 
                          "list".equals(operation.returnContainer);
    queryModel.put("isCollection", isCollection);
    
    if (isCollection) {
        queryModel.put("collectionType", "IEnumerable<" + operation.returnBaseType + ">");
        queryModel.put("responseType", operation.returnBaseType);
    } else {
        queryModel.put("responseType", operation.returnType != null ? operation.returnType : "Unit");
    }
    
    // Filter out body params (shouldn't exist for GET, but safety check)
    List<CodegenParameter> nonBodyParams = operation.allParams.stream()
        .filter(p -> !p.isBodyParam)
        .collect(Collectors.toList());
    queryModel.put("allParams", nonBodyParams);
    queryModel.put("imports", getRequiredImports(operation));
    
    String filename = "Queries/" + getQueryClassName(operation) + ".cs";
    supportingFiles.add(new SupportingFile("query.mustache", "", filename));
}

private String getQueryClassName(CodegenOperation operation) {
    // getPetById → GetPetByIdQuery
    // findPetsByStatus → FindPetsByStatusQuery
    return camelize(operation.operationId) + "Query";
}
```

## Regeneration Behavior

- **First Generation**: File is created
- **Subsequent Generations**: File is **ALWAYS regenerated**
- **Protection**: Queries are NOT protected (they're API contract)

**Rationale**: Same as commands - queries define the API contract and should regenerate when spec changes.

## Edge Cases

1. **No parameters**: Generate empty record (e.g., `GetAllPetsQuery` with no properties)
2. **Complex query params**: 
   - Arrays: Use `string[]` per Phase 5
   - Objects: Use HttpContext pattern in endpoint, deserialize to model, pass to query
3. **Pagination params**: Include as normal properties (e.g., `int PageSize`, `int PageNumber`)
4. **Header authentication**: Include as query properties (e.g., `string? ApiKey`)
