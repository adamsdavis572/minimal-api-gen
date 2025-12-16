# Data Model: Configuration Options Fixes

**Date**: 2025-12-12  
**Feature**: 007-config-fixes

## Overview

This feature primarily deals with code generation templates and configuration flags rather than domain entities. However, there are key data structures in the generator and generated code.

---

## Generator Data Model (Java)

### Entity: MinimalApiServerCodegen Configuration

**Purpose**: Generator configuration options that control template processing

```java
public class MinimalApiServerCodegen extends AbstractCSharpCodegen {
    // Existing flags (no changes)
    private boolean useMediatr = false;
    private boolean useProblemDetails = false;
    private boolean useRecords = false;
    private boolean useAuthentication = false;
    private boolean useResponseCaching = false;
    private boolean useApiVersioning = false;
    
    // Modified behavior
    private boolean useValidators = false;        // NOW FUNCTIONAL: controls validator generation
    private boolean useGlobalExceptionHandler = true;  // NOW FUNCTIONAL: controls middleware
    
    // REMOVED (dead code)
    // private boolean useRouteGroups = true;     // DELETED: unused flag
}
```

**Relationships**:
- Inherits from AbstractCSharpCodegen (OpenAPI Generator framework)
- Populates additionalProperties map for template access
- Triggers validator file generation when useValidators=true

**Validation Rules**:
- All flags are boolean
- useGlobalExceptionHandler defaults to true (opt-out pattern)
- useValidators defaults to false (opt-in pattern)

**State Transitions**:
- Configuration → Template Processing → File Generation
- useValidators=true → validator.mustache invoked per operation
- useGlobalExceptionHandler=true → exception handler middleware added to program.mustache

---

## Generated Code Data Model (C#)

### Entity: Validator Class

**Purpose**: FluentValidation validator for request validation

```csharp
public class {{operationId}}RequestValidator : AbstractValidator<{{operationId}}Request>
{
    public {{operationId}}RequestValidator()
    {
        // Rules added per required parameter
        RuleFor(x => x.ParameterName).NotEmpty();
    }
}
```

**Properties**:
- Inherits from `AbstractValidator<TRequest>`
- One class per operation with parameters
- Rules based on OpenAPI schema constraints

**Relationships**:
- References request DTO class (e.g., `AddPetRequest`)
- Registered in DI container via `AddValidatorsFromAssemblyContaining<Program>()`
- Invoked by ASP.NET Core validation pipeline

**Validation Rules** (FluentValidation DSL):
- `.NotEmpty()` for required parameters
- `.Matches(pattern)` for regex patterns (future enhancement)
- `.Length(min, max)` for string length constraints (future enhancement)
- `.GreaterThanOrEqualTo(min)` for numeric minimums (future enhancement)

**File Location**: `{outputFolder}/Validators/{OperationId}RequestValidator.cs`

---

### Entity: Exception Handler Configuration

**Purpose**: ASP.NET Core middleware configuration for unhandled exceptions

```csharp
// In Program.cs
app.UseExceptionHandler(exceptionHandlerApp =>
{
    exceptionHandlerApp.Run(async context =>
    {
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        context.Response.ContentType = "application/problem+json";
        
        var problemDetails = new ProblemDetails
        {
            Status = 500,
            Title = "An error occurred",
            Detail = exception?.Message
        };
        
        await context.Response.WriteAsJsonAsync(problemDetails);
    });
});
```

**Properties**:
- Middleware lambda configuration
- Produces ProblemDetails response
- Sets HTTP 500 status code

**Relationships**:
- Integrates with useProblemDetails flag
- Catches unhandled exceptions from all endpoints
- Transforms to RFC 7807 format

**Validation Rules**:
- Always returns HTTP 500 for unhandled exceptions
- ContentType must be "application/problem+json" when useProblemDetails=true
- Detail field contains exception message (production may want to sanitize)

---

## Template Data Model (Mustache Context)

### Entity: Operation Context

**Purpose**: Data passed to mustache templates for each API operation

```javascript
{
    "operationId": "AddPet",
    "requiredParams": [
        {
            "paramName": "pet",
            "nameInPascalCase": "Pet",
            "isBodyParam": true,
            "isPathParam": false,
            "isQueryParam": false,
            "dataType": "Pet",
            "required": true
        }
    ],
    "allParams": [...],
    "useValidators": true,
    "useGlobalExceptionHandler": true
}
```

**Properties**:
- `operationId`: Unique operation name (e.g., "AddPet", "GetPetById")
- `requiredParams`: Array of required parameters (populated by OpenAPI Generator)
- `useValidators`: Boolean flag from configuration
- `useGlobalExceptionHandler`: Boolean flag from configuration

**Relationships**:
- Provided by OpenAPI Generator framework
- Consumed by validator.mustache template
- Consumed by program.mustache template

**Source**: AbstractCSharpCodegen.postProcessOperationsWithModels()

---

## Key Entities Summary

| Entity | Type | Purpose | Lifespan |
|--------|------|---------|----------|
| MinimalApiServerCodegen | Java Class | Generator configuration | Generator execution |
| {Operation}RequestValidator | C# Class | Validation logic | Generated code runtime |
| Exception Handler Config | C# Middleware | Error handling | Generated code runtime |
| Operation Context | Mustache Data | Template rendering | Template processing |

---

## Data Flow

```
OpenAPI Spec
    ↓
MinimalApiServerCodegen.processOperation()
    ↓ (populates requiredParams)
Operation Context Object
    ↓ (passed to template)
validator.mustache
    ↓ (renders to)
{Operation}RequestValidator.cs
    ↓ (compiled with)
Generated API Project
    ↓ (runtime: DI registers)
IServiceCollection.AddValidatorsFromAssemblyContaining()
    ↓ (runtime: endpoint calls)
AbstractValidator<TRequest>.ValidateAsync()
```

---

## Assumptions

1. **OpenAPI Generator Framework**: The `requiredParams` property is always populated correctly by AbstractCSharpCodegen
2. **FluentValidation**: Version 11.9.0 API remains stable for AbstractValidator<T> usage
3. **ASP.NET Core**: UseExceptionHandler API remains stable in .NET 8.0+
4. **Template Engine**: Mustache supports nested loops and conditional blocks as documented

---

## Edge Cases

### Validator Generation
- **No required parameters**: Generate empty validator class (allows consistency)
- **Multiple body parameters**: Loop handles all, generates RuleFor for each
- **Optional parameters**: Not included in validator (only requiredParams loop)

### Exception Handler
- **useProblemDetails=false**: Generate simple JSON error instead of ProblemDetails
- **useGlobalExceptionHandler=false**: Skip middleware registration entirely
- **Exception without message**: Use default "An error occurred" message

### Flag Removal
- **Existing projects with useRouteGroups=false**: Flag ignored (no error), MapGroup still used
- **Documentation references**: Updated to state "route groups required"
