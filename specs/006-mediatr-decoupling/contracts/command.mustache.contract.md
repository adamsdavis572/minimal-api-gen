# Command Template Contract

**Template File**: `generator/src/main/resources/aspnet-minimalapi/command.mustache`  
**Output Pattern**: `Commands/{CommandClassName}.cs`  
**Generated When**: `useMediatr=true` AND operation is POST/PUT/PATCH/DELETE

## Input Data Model

The template receives a `CodegenOperation` object enhanced with MediatR-specific properties:

```java
{
    "commandClassName": "AddPetCommand",        // PascalCase command name
    "namespace": "PetstoreApi.Commands",        // Project namespace + .Commands
    "responseType": "Pet",                       // IRequest<T> response type
    "hasResponseType": true,                     // false if void/Unit
    "allParams": [                               // All operation parameters
        {
            "paramName": "name",                 // camelCase parameter name
            "dataType": "string",                // C# type
            "required": true,
            "description": "Pet name"
        },
        {
            "paramName": "photoUrls",
            "dataType": "string[]",              // Arrays converted per Phase 5
            "required": true
        }
    ],
    "imports": [                                 // Required using statements
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
/// Command for {{operationId}} operation
/// </summary>
public record {{commandClassName}} : IRequest<{{responseType}}>
{
    {{#allParams}}
    {{#description}}
    /// <summary>
    /// {{description}}
    /// </summary>
    {{/description}}
    public {{dataType}} {{baseName}} { get; init; }{{^required}} = default!;{{/required}}
    
    {{/allParams}}
}
```

## Output Example

**Input**: POST /pet operation from petstore.yaml

**Output**: `Commands/AddPetCommand.cs`
```csharp
using MediatR;
using PetstoreApi.Models;

namespace PetstoreApi.Commands;

/// <summary>
/// Command for addPet operation
/// </summary>
public record AddPetCommand : IRequest<Pet>
{
    /// <summary>
    /// Pet name
    /// </summary>
    public string Name { get; init; }
    
    /// <summary>
    /// List of photo URLs
    /// </summary>
    public string[] PhotoUrls { get; init; }
    
    public Category? Category { get; init; } = default!;
    
    public Tag[]? Tags { get; init; } = default!;
    
    public PetStatus Status { get; init; }
}
```

## Validation Rules

1. **File name**: Must match `{commandClassName}.cs` pattern
2. **Namespace**: Must be `{projectName}.Commands`
3. **Record type**: Must use `record` keyword (immutable)
4. **IRequest<T>**: Must implement with correct response type
5. **Properties**: Must use `{ get; init; }` accessor
6. **Nullability**: Optional parameters get `= default!` assignment

## Generation Logic (MinimalApiServerCodegen.java)

```java
private void processCommand(CodegenOperation operation) {
    // Only for POST/PUT/PATCH/DELETE
    if (!isCommandOperation(operation)) {
        return;
    }
    
    Map<String, Object> commandModel = new HashMap<>();
    commandModel.put("commandClassName", getCommandClassName(operation));
    commandModel.put("namespace", packageName + ".Commands");
    commandModel.put("responseType", getMediatrResponseType(operation));
    commandModel.put("hasResponseType", operation.returnType != null);
    commandModel.put("allParams", operation.allParams);
    commandModel.put("imports", getRequiredImports(operation));
    
    String filename = "Commands/" + getCommandClassName(operation) + ".cs";
    supportingFiles.add(new SupportingFile("command.mustache", "", filename));
}

private String getCommandClassName(CodegenOperation operation) {
    // addPet → AddPetCommand
    return camelize(operation.operationId) + "Command";
}

private boolean isCommandOperation(CodegenOperation operation) {
    return operation.httpMethod.matches("POST|PUT|PATCH|DELETE");
}
```

## Regeneration Behavior

- **First Generation**: File is created
- **Subsequent Generations**: File is **ALWAYS regenerated** (commands are contract, not implementation)
- **Protection**: Commands are NOT protected by .openapi-generator-ignore (they're part of API contract)

**Rationale**: Commands define the API contract. If OpenAPI spec changes, commands should regenerate to match. Business logic lives in handlers (which ARE protected).

## Edge Cases

1. **No parameters**: Generate empty record with just IRequest<T>
2. **Complex types**: Reference Models namespace
3. **Array parameters**: Use `T[]` notation per Phase 5
4. **Header/Query params**: Include as properties alongside body properties
5. **File uploads**: Add `IFormFile` or `Stream` property types
