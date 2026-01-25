# Data Model: DTO Validation Architecture

**Date**: 2025-12-12 | **Updated**: 2025-12-16  
**Feature**: 007-config-fixes

## Overview

This feature implements true CQRS with separate DTOs for API contracts and comprehensive FluentValidation. The data model includes DTOs (API contract layer), DTO Validators (validation layer), Commands/Queries referencing DTOs, Handlers mapping DTOs to Models, and unchanged Models (domain layer).

---

## Generator Data Model (Java)

### Entity: MinimalApiServerCodegen Configuration

**Purpose**: Generator configuration options that control template processing and DTO generation

```java
public class MinimalApiServerCodegen extends AbstractCSharpCodegen {
    // Existing flags (from 006)
    private boolean useMediatr = false;           // REQUIRED for DTOs
    private boolean useProblemDetails = false;
    private boolean useRecords = false;
    private boolean useAuthentication = false;
    private boolean useResponseCaching = false;
    private boolean useApiVersioning = false;
    
    // Modified behavior (007)
    private boolean useValidators = false;        // NOW FUNCTIONAL: controls DTO validator generation
    private boolean useGlobalExceptionHandler = true;  // NOW FUNCTIONAL: controls middleware
    
    // REMOVED (007)
    // private boolean useRouteGroups = true;     // DELETED: unused flag
}
```

**Relationships**:
- Inherits from AbstractCSharpCodegen (OpenAPI Generator framework)
- Populates additionalProperties map for template access
- Triggers DTO and validator file generation when useMediatr=true and useValidators=true

**Validation Rules**:
- All flags are boolean
- useGlobalExceptionHandler defaults to true (opt-out pattern)
- useValidators defaults to false (opt-in pattern)
- useMediatr must be true for DTOs to be generated

**State Transitions**:
- Configuration → Template Processing → DTO/Validator File Generation
- useMediatr=true → dto.mustache invoked per requestBody schema
- useValidators=true → dtoValidator.mustache invoked per DTO
- useGlobalExceptionHandler=true → exception handler middleware added to program.mustache

---

## Template Data Model (Mustache Context)

---

## Entity Catalog

### 1. DTO (Data Transfer Object)

**Purpose**: API contract representation for request bodies, separate from domain Models

**Properties**:
- C# record type (immutable by default)
- Properties match OpenAPI requestBody schema exactly
- Located in `DTOs/` directory
- One DTO per unique requestBody schema

**Example Classes**:
- `AddPetDto` - for POST /pet requestBody
- `UpdatePetDto` - for PUT /pet requestBody
- `CategoryDto` - nested object in AddPetDto
- `TagDto` - nested object in AddPetDto
- `PlaceOrderDto` - for POST /store/order requestBody

**Relationships**:
- Referenced by Command/Query classes (composition)
- Validated by corresponding DTO Validator
- Mapped to Model by Handler

**Lifecycle**: Created from JSON deserialization → Validated by FluentValidation → Passed to Handler → Mapped to Model

**File Location**: `{outputFolder}/DTOs/{DtoClassName}.cs`

---

### 2. DTO Validator

**Purpose**: FluentValidation validator for DTO classes, enforces OpenAPI constraints at API boundary

**Properties**:
- Inherits from `AbstractValidator<TDto>`
- Contains RuleFor() expressions for each property constraint
- Located in `Validators/` directory
- Registered in DI container via AddValidatorsFromAssemblyContaining

**Example Classes**:
- `AddPetDtoValidator` - validates AddPetDto
- `CategoryDtoValidator` - validates CategoryDto (chained via SetValidator)
- `PlaceOrderDtoValidator` - validates PlaceOrderDto

**Validation Rules** (mapped from OpenAPI):
- required → `RuleFor(x => x.Name).NotEmpty()`
- minLength/maxLength → `RuleFor(x => x.Name).Length(1, 100)`
- pattern → `RuleFor(x => x.Email).Matches("^[a-zA-Z0-9._%+-]+@...")`
- minimum/maximum → `RuleFor(x => x.Quantity).GreaterThanOrEqualTo(1).LessThanOrEqualTo(1000)`
- minItems/maxItems → `RuleFor(x => x.PhotoUrls).Must(x => x.Count >= 1 && x.Count <= 10)`
- nested object → `RuleFor(x => x.Category).SetValidator(new CategoryDtoValidator())`

**Relationships**:
- Validates corresponding DTO
- Chained via SetValidator for nested DTOs
- Triggered before MediatR handler execution

**Lifecycle**: Registered at startup → Invoked on request → Returns ValidationResult → Converted to 400 response if failed

**File Location**: `{outputFolder}/Validators/{DtoClassName}Validator.cs`

---

### 3. Command (Modified)

**Purpose**: MediatR request carrying validated DTO to Handler

**Properties**:
- References DTO type (NOT Model type) for body parameters
- Immutable record type
- Implements IRequest<TResponse>

**Example**: AddPetCommand
```csharp
public record AddPetCommand : IRequest<Pet>
{
    public AddPetDto pet { get; init; }  // Changed from Pet to AddPetDto
}
```

**Key Change from 006**: Body parameter type changed from Model to DTO

**Relationships**:
- Contains DTO (composition)
- Handled by corresponding Handler
- Sent via MediatR pipeline

**File Location**: `{outputFolder}/Commands/{OperationId}Command.cs`

---

### 4. Query (Modified)

**Purpose**: MediatR request for read operations (may include DTO for filtering)

**Properties**:
- References DTO type for body parameters (if applicable)
- Immutable record type
- Implements IRequest<TResponse>

**Example**: FindPetsByStatusQuery (no DTO, query params only)
```csharp
public record FindPetsByStatusQuery : IRequest<List<Pet>>
{
    public List<string>? status { get; init; }
}
```

**Note**: Most queries won't have DTOs (only GET operations with request bodies, which are rare)

**File Location**: `{outputFolder}/Queries/{OperationId}Query.cs`

---

### 5. Handler (Modified)

**Purpose**: MediatR handler that maps DTO to Model and executes business logic

**Properties**:
- Receives Command/Query with DTO
- Responsible for DTO→Model mapping
- Contains business logic operating on Models

**Example**: AddPetHandler
```csharp
public class AddPetHandler : IRequestHandler<AddPetCommand, Pet>
{
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        // Map DTO to Model
        var pet = new Pet
        {
            Id = request.pet.Id,
            Name = request.pet.Name,
            PhotoUrls = request.pet.PhotoUrls,
            Category = request.pet.Category != null ? new Category
            {
                Id = request.pet.Category.Id,
                Name = request.pet.Category.Name
            } : null,
            // ... map all properties
        };
        
        // TODO: Implement business logic with Model
        return pet;
    }
}
```

**Key Change from 006**: Added DTO→Model mapping responsibility

**Relationships**:
- Handles Command/Query
- Maps DTO to Model
- Returns Model (not DTO)

**File Location**: `{outputFolder}/Handlers/{OperationId}Handler.cs`

---

### 6. Model (Unchanged)

**Purpose**: Domain entity representing business concepts from OpenAPI components/schemas

**Properties**:
- Plain C# class
- Located in `Models/` directory
- May differ from DTOs (independent evolution)

**Example**: Pet
```csharp
public class Pet
{
    public long Id { get; set; }
    public string Name { get; set; }
    public Category? Category { get; set; }
    public List<string> PhotoUrls { get; set; }
    public List<Tag>? Tags { get; set; }
    public string? Status { get; set; }
}
```

**Note**: No [Required] attributes, no validation logic - this is domain model only

**Relationships**:
- Created by Handler from DTO
- Used in business logic
- Returned as response

**File Location**: `{outputFolder}/Models/{ModelName}.cs`

---

## Entity Relationships Diagram

```
[JSON Request Body]
       ↓
   [DTO Class] ← validated by → [DTO Validator]
       ↓
 [Command/Query] ← referenced by
       ↓
    [Handler] ← maps DTO to → [Model]
       ↓
 [Business Logic]
       ↓
  [Model Response]
```

---

## DTO vs Model Comparison

| Aspect | DTO | Model |
|--------|-----|-------|
| **Purpose** | API contract | Domain entity |
| **Location** | DTOs/ directory | Models/ directory |
| **Validation** | FluentValidation rules | None (domain invariants in logic) |
| **Immutability** | record type (init-only) | class (mutable setters) |
| **Schema Source** | OpenAPI requestBody | OpenAPI components/schemas |
| **Referenced By** | Command/Query | Handler business logic |
| **Lifetime** | Request scope | Business logic scope |
| **Can Differ?** | Yes - API and domain may evolve independently | Yes |

---

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
