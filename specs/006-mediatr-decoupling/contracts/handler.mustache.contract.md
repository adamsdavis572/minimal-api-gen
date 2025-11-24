# Handler Template Contract

**Template File**: `generator/src/main/resources/aspnet-minimalapi/handler.mustache`  
**Output Pattern**: `Handlers/{HandlerClassName}.cs`  
**Generated When**: `useMediatr=true` (one handler per command/query)

## Input Data Model

```java
{
    "handlerClassName": "AddPetCommandHandler",  // Handler class name
    "namespace": "PetstoreApi.Handlers",         // Project namespace + .Handlers
    "requestType": "AddPetCommand",              // Command or Query name
    "requestNamespace": "PetstoreApi.Commands",  // Full namespace of request
    "responseType": "Pet",                        // Return type
    "hasResponseType": true,                      // false for Unit/void
    "isUnit": false,                              // true if responseType is Unit
    "operationId": "addPet",                      // Original operation ID for TODO comment
    "imports": [
        "MediatR",
        "PetstoreApi.Commands",
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
/// Handler for {{requestType}}
/// </summary>
public class {{handlerClassName}} : IRequestHandler<{{requestType}}, {{responseType}}>
{
    // TODO: Add dependencies via constructor injection
    // Example:
    // private readonly IRepository<{{responseType}}> _repository;
    // public {{handlerClassName}}(IRepository<{{responseType}}> repository)
    // {
    //     _repository = repository;
    // }
    
    /// <summary>
    /// Handles the {{requestType}}
    /// </summary>
    public async Task<{{responseType}}> Handle({{requestType}} request, CancellationToken cancellationToken)
    {
        // TODO: Implement {{operationId}} logic
        {{#isUnit}}
        // Example: await _repository.DeleteAsync(request.Id);
        return Unit.Value;
        {{/isUnit}}
        {{^isUnit}}
        // Example: return await _repository.GetByIdAsync(request.Id);
        throw new NotImplementedException("Handler for {{operationId}} not yet implemented");
        {{/isUnit}}
    }
}
```

## Output Example

**Input**: AddPetCommand

**Output**: `Handlers/AddPetCommandHandler.cs`
```csharp
using MediatR;
using PetstoreApi.Commands;
using PetstoreApi.Models;

namespace PetstoreApi.Handlers;

/// <summary>
/// Handler for AddPetCommand
/// </summary>
public class AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>
{
    // TODO: Add dependencies via constructor injection
    // Example:
    // private readonly IRepository<Pet> _repository;
    // public AddPetCommandHandler(IRepository<Pet> repository)
    // {
    //     _repository = repository;
    // }
    
    /// <summary>
    /// Handles the AddPetCommand
    /// </summary>
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        // TODO: Implement addPet logic
        // Example: return await _repository.CreateAsync(request);
        throw new NotImplementedException("Handler for addPet not yet implemented");
    }
}
```

**Input**: GetPetByIdQuery

**Output**: `Handlers/GetPetByIdQueryHandler.cs`
```csharp
using MediatR;
using PetstoreApi.Queries;
using PetstoreApi.Models;

namespace PetstoreApi.Handlers;

/// <summary>
/// Handler for GetPetByIdQuery
/// </summary>
public class GetPetByIdQueryHandler : IRequestHandler<GetPetByIdQuery, Pet>
{
    // TODO: Add dependencies via constructor injection
    
    /// <summary>
    /// Handles the GetPetByIdQuery
    /// </summary>
    public async Task<Pet> Handle(GetPetByIdQuery request, CancellationToken cancellationToken)
    {
        // TODO: Implement getPetById logic
        throw new NotImplementedException("Handler for getPetById not yet implemented");
    }
}
```

**Input**: DeletePetCommand (returns Unit)

**Output**: `Handlers/DeletePetCommandHandler.cs`
```csharp
using MediatR;
using PetstoreApi.Commands;

namespace PetstoreApi.Handlers;

/// <summary>
/// Handler for DeletePetCommand
/// </summary>
public class DeletePetCommandHandler : IRequestHandler<DeletePetCommand, Unit>
{
    // TODO: Add dependencies via constructor injection
    
    /// <summary>
    /// Handles the DeletePetCommand
    /// </summary>
    public async Task<Unit> Handle(DeletePetCommand request, CancellationToken cancellationToken)
    {
        // TODO: Implement deletePet logic
        // Example: await _repository.DeleteAsync(request.PetId);
        return Unit.Value;
    }
}
```

## Validation Rules

1. **File name**: Must match `{handlerClassName}.cs` pattern
2. **Namespace**: Must be `{projectName}.Handlers`
3. **Class type**: Must use `class` (not record - handlers are stateful)
4. **IRequestHandler<TRequest, TResponse>**: Must implement with correct types
5. **Handle method**: 
   - Must be async
   - Must return `Task<TResponse>`
   - Must accept `CancellationToken`
6. **TODO comments**: Must include helpful examples

## Generation Logic (MinimalApiServerCodegen.java)

```java
private void processHandler(CodegenOperation operation) {
    if (!useMediatr) {
        return;
    }
    
    Map<String, Object> handlerModel = new HashMap<>();
    handlerModel.put("handlerClassName", getHandlerClassName(operation));
    handlerModel.put("namespace", packageName + ".Handlers");
    
    // Determine if command or query
    boolean isCommand = isCommandOperation(operation);
    String requestType = isCommand ? 
        getCommandClassName(operation) : 
        getQueryClassName(operation);
    String requestNamespace = isCommand ? 
        packageName + ".Commands" : 
        packageName + ".Queries";
    
    handlerModel.put("requestType", requestType);
    handlerModel.put("requestNamespace", requestNamespace);
    
    // Response type handling
    String responseType = getMediatrResponseType(operation);
    boolean isUnit = "Unit".equals(responseType);
    
    handlerModel.put("responseType", responseType);
    handlerModel.put("hasResponseType", operation.returnType != null);
    handlerModel.put("isUnit", isUnit);
    handlerModel.put("operationId", operation.operationId);
    handlerModel.put("imports", getRequiredImports(operation));
    
    String filename = "Handlers/" + getHandlerClassName(operation) + ".cs";
    
    // CRITICAL: Check if file exists before adding to supportingFiles
    // This implements the regeneration protection
    File handlerFile = new File(outputFolder, filename);
    if (!handlerFile.exists()) {
        supportingFiles.add(new SupportingFile("handler.mustache", "", filename));
        LOGGER.info("Generating handler scaffold: " + filename);
    } else {
        LOGGER.info("Skipping existing handler: " + filename);
    }
}

private String getHandlerClassName(CodegenOperation operation) {
    // addPet → AddPetCommandHandler
    // getPetById → GetPetByIdQueryHandler
    boolean isCommand = isCommandOperation(operation);
    String requestName = isCommand ? 
        getCommandClassName(operation) : 
        getQueryClassName(operation);
    return requestName + "Handler";
}
```

## Regeneration Behavior ⚠️ CRITICAL

- **First Generation**: File is created with TODO scaffold
- **Subsequent Generations**: File is **SKIPPED** if it exists
- **Protection Method**: File-level check in `processHandler()` method

**Rationale**: Handlers contain user-implemented business logic. Once generated, they become user-owned code and must never be overwritten. This is the key difference from commands/queries which ARE regenerated.

## Implementation Notes

1. **File Check**: Use `File.exists()` check in generator BEFORE adding to supportingFiles
2. **Logging**: Log INFO when skipping existing handlers for transparency
3. **Force Regeneration**: User can manually delete handler file to regenerate scaffold
4. **No .openapi-generator-ignore**: We use programmatic check instead of ignore file for better control

## Edge Cases

1. **Unit return type**: Use `return Unit.Value;` pattern
2. **Collection return type**: Return `IEnumerable<T>` as promised by interface
3. **Complex operations**: Include helpful TODO comments with common patterns
4. **Async requirements**: Always use async/await pattern even if synchronous today (future-proof)
