# DTO Validator Examples

**Feature**: 007-config-fixes | **Date**: 2025-12-16

## Purpose

Concrete examples of generated FluentValidation validator classes showing comprehensive constraint mapping from OpenAPI schemas to FluentValidation rules.

---

## Example 1: AddPetDtoValidator (Comprehensive)

**Validates**: AddPetDto  
**OpenAPI Constraints**: required, minLength, maxLength, minItems, maxItems, nested objects

**Generated File**: `Validators/AddPetDtoValidator.cs`

```csharp
using FluentValidation;
using PetstoreApi.DTOs;

namespace PetstoreApi.Validators;

/// <summary>
/// Validator for AddPetDto
/// Generated from OpenAPI schema constraints
/// </summary>
public class AddPetDtoValidator : AbstractValidator<AddPetDto>
{
    public AddPetDtoValidator()
    {
        // Required field: name
        RuleFor(x => x.Name)
            .NotEmpty()
            .WithMessage("Name is required");
        
        // String length constraints: name (1-100 chars)
        RuleFor(x => x.Name)
            .Length(1, 100)
            .WithMessage("Name must be between 1 and 100 characters");
        
        // Required collection: photoUrls
        RuleFor(x => x.PhotoUrls)
            .NotNull()
            .WithMessage("PhotoUrls is required");
        
        // Array size constraints: photoUrls (1-10 items)
        RuleFor(x => x.PhotoUrls)
            .Must(x => x.Count >= 1 && x.Count <= 10)
            .WithMessage("PhotoUrls must contain between 1 and 10 items");
        
        // Nested object validation: category
        RuleFor(x => x.Category)
            .SetValidator(new CategoryDtoValidator()!)
            .When(x => x.Category != null);
        
        // Nested array validation: tags
        RuleForEach(x => x.Tags)
            .SetValidator(new TagDtoValidator())
            .When(x => x.Tags != null);
    }
}
```

**Constraint Coverage**:
- ✅ required → NotEmpty()
- ✅ minLength/maxLength → Length(min, max)
- ✅ minItems/maxItems → Must(x => x.Count...)
- ✅ nested object → SetValidator()
- ✅ nested array items → RuleForEach()

---

## Example 2: CategoryDtoValidator (Simple)

**Validates**: CategoryDto  
**OpenAPI Constraints**: minLength

**Generated File**: `Validators/CategoryDtoValidator.cs`

```csharp
using FluentValidation;
using PetstoreApi.DTOs;

namespace PetstoreApi.Validators;

/// <summary>
/// Validator for CategoryDto
/// Generated from OpenAPI schema constraints
/// </summary>
public class CategoryDtoValidator : AbstractValidator<CategoryDto>
{
    public CategoryDtoValidator()
    {
        // String minimum length: name (at least 1 char)
        RuleFor(x => x.Name)
            .MinimumLength(1)
            .WithMessage("Name must be at least 1 character")
            .When(x => x.Name != null);
    }
}
```

**Key Feature**: Validation only when property is not null (optional field)

---

## Example 3: PlaceOrderDtoValidator (Numeric Constraints)

**Validates**: PlaceOrderDto  
**OpenAPI Constraints**: minimum, maximum

**Generated File**: `Validators/PlaceOrderDtoValidator.cs`

```csharp
using FluentValidation;
using PetstoreApi.DTOs;

namespace PetstoreApi.Validators;

/// <summary>
/// Validator for PlaceOrderDto
/// Generated from OpenAPI schema constraints
/// </summary>
public class PlaceOrderDtoValidator : AbstractValidator<PlaceOrderDto>
{
    public PlaceOrderDtoValidator()
    {
        // Numeric range: quantity (1-1000)
        RuleFor(x => x.Quantity)
            .GreaterThanOrEqualTo(1)
            .WithMessage("Quantity must be at least 1");
        
        RuleFor(x => x.Quantity)
            .LessThanOrEqualTo(1000)
            .WithMessage("Quantity must not exceed 1000");
    }
}
```

**Constraint Coverage**:
- ✅ minimum → GreaterThanOrEqualTo()
- ✅ maximum → LessThanOrEqualTo()

---

## Example 4: CreateUserDtoValidator (Pattern Matching)

**Validates**: CreateUserDto  
**OpenAPI Constraints**: pattern (regex), minLength, maxLength

**Generated File**: `Validators/CreateUserDtoValidator.cs`

```csharp
using FluentValidation;
using PetstoreApi.DTOs;

namespace PetstoreApi.Validators;

/// <summary>
/// Validator for CreateUserDto
/// Generated from OpenAPI schema constraints
/// </summary>
public class CreateUserDtoValidator : AbstractValidator<CreateUserDto>
{
    public CreateUserDtoValidator()
    {
        // String length constraints: username (3-50 chars)
        RuleFor(x => x.Username)
            .Length(3, 50)
            .WithMessage("Username must be between 3 and 50 characters")
            .When(x => x.Username != null);
        
        // Pattern constraint: email (regex validation)
        RuleFor(x => x.Email)
            .Matches(@"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")
            .WithMessage("Email must be a valid email address")
            .When(x => x.Email != null);
    }
}
```

**Constraint Coverage**:
- ✅ minLength/maxLength → Length(min, max)
- ✅ pattern → Matches(regex)

---

## OpenAPI Constraint Mapping Reference

| OpenAPI Constraint | FluentValidation Rule | Example |
|-------------------|----------------------|---------|
| **required: true** | `NotEmpty()` or `NotNull()` | `RuleFor(x => x.Name).NotEmpty()` |
| **minLength: N** | `MinimumLength(N)` or `Length(min, max)` | `RuleFor(x => x.Name).Length(1, 100)` |
| **maxLength: N** | `MaximumLength(N)` or `Length(min, max)` | `RuleFor(x => x.Name).Length(1, 100)` |
| **pattern: "regex"** | `Matches("regex")` | `RuleFor(x => x.Email).Matches("^[a-zA-Z0-9._%+-]+@...")` |
| **minimum: N** | `GreaterThanOrEqualTo(N)` | `RuleFor(x => x.Quantity).GreaterThanOrEqualTo(1)` |
| **maximum: N** | `LessThanOrEqualTo(N)` | `RuleFor(x => x.Quantity).LessThanOrEqualTo(1000)` |
| **minItems: N** | `Must(x => x.Count >= N)` | `RuleFor(x => x.PhotoUrls).Must(x => x.Count >= 1)` |
| **maxItems: N** | `Must(x => x.Count <= N)` | `RuleFor(x => x.PhotoUrls).Must(x => x.Count <= 10)` |
| **Nested object** | `SetValidator(new TValidator())` | `RuleFor(x => x.Category).SetValidator(new CategoryDtoValidator())` |
| **Array items** | `RuleForEach(x => x.Items).SetValidator(...)` | `RuleForEach(x => x.Tags).SetValidator(new TagDtoValidator())` |

---

## Validator Chaining Example

**Scenario**: Pet has nested Category, Category has nested Subcategory

**CategoryDto**:
```csharp
public record CategoryDto
{
    public string Name { get; init; }
    public SubcategoryDto? Subcategory { get; init; }
}
```

**CategoryDtoValidator**:
```csharp
public class CategoryDtoValidator : AbstractValidator<CategoryDto>
{
    public CategoryDtoValidator()
    {
        RuleFor(x => x.Name).NotEmpty();
        
        // Chain to nested validator
        RuleFor(x => x.Subcategory)
            .SetValidator(new SubcategoryDtoValidator()!)
            .When(x => x.Subcategory != null);
    }
}
```

**Result**: Validation cascades through object graph automatically

---

## Conditional Validation Example

**Scenario**: Validate email only if provided (optional field)

```csharp
public class CreateUserDtoValidator : AbstractValidator<CreateUserDto>
{
    public CreateUserDtoValidator()
    {
        // Only validate email format if email is provided
        RuleFor(x => x.Email)
            .Matches(@"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")
            .When(x => x.Email != null);  // Conditional validation
    }
}
```

**Pattern**: Use `.When()` for optional fields that have format constraints

---

## Array Item Validation Example

**Scenario**: Validate each URL in photoUrls array

```csharp
public class AddPetDtoValidator : AbstractValidator<AddPetDto>
{
    public AddPetDtoValidator()
    {
        // Validate array size
        RuleFor(x => x.PhotoUrls)
            .Must(x => x.Count >= 1 && x.Count <= 10);
        
        // Validate each item (if items have pattern constraint)
        RuleForEach(x => x.PhotoUrls)
            .Matches(@"^https?://")
            .WithMessage("Each photo URL must start with http:// or https://");
    }
}
```

**Key Feature**: `RuleForEach()` applies rule to every array element

---

## Empty Validator Example

**Scenario**: DTO has no validation constraints in OpenAPI schema

```csharp
public class SimpleDto
{
    public string? OptionalField { get; init; }
}
```

**Generated Validator**:
```csharp
public class SimpleDtoValidator : AbstractValidator<SimpleDto>
{
    public SimpleDtoValidator()
    {
        // No rules - all fields are optional with no constraints
    }
}
```

**Rationale**: Generate validator for consistency even if empty

---

## Validation Execution Flow

```
1. Client sends JSON request
       ↓
2. ASP.NET Core deserializes to DTO
       ↓
3. FluentValidation triggers (via DI)
       ↓
4. DtoValidator.Validate(dto)
       ↓
5a. ValidationResult.IsValid = true
    → DTO passed to Command → Handler executes
       ↓
5b. ValidationResult.IsValid = false
    → Exception → ValidationProblem returned (400)
       ↓
6. Client receives response (200 or 400)
```

---

## Validation Error Response Example

**Request**: POST /pet with invalid data
```json
{
  "name": "",
  "photoUrls": []
}
```

**Response**: 400 Bad Request
```json
{
  "type": "https://tools.ietf.org/html/rfc7231#section-6.5.1",
  "title": "One or more validation errors occurred.",
  "status": 400,
  "errors": {
    "Name": ["Name is required", "Name must be between 1 and 100 characters"],
    "PhotoUrls": ["PhotoUrls must contain between 1 and 10 items"]
  }
}
```

**Key Feature**: RFC 7807 ProblemDetails format with structured errors

---

**Status**: DTO validator examples complete. See [command-dto-integration.md](command-dto-integration.md) for Command/Query integration.
