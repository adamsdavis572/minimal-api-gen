# DTO Examples

**Feature**: 007-config-fixes | **Date**: 2025-12-16

## Purpose

Concrete examples of generated DTO classes showing structure, naming, and property mapping from OpenAPI requestBody schemas.

---

## Example 1: AddPetDto

**OpenAPI Source**: POST /pet requestBody with Pet schema

**Generated File**: `DTOs/AddPetDto.cs`

```csharp
namespace PetstoreApi.DTOs;

/// <summary>
/// DTO for Add Pet operation
/// Generated from OpenAPI requestBody schema
/// </summary>
public record AddPetDto
{
    /// <summary>
    /// Pet ID
    /// </summary>
    public long? Id { get; init; }
    
    /// <summary>
    /// Pet name (required, 1-100 chars)
    /// </summary>
    public string Name { get; init; } = string.Empty;
    
    /// <summary>
    /// Category information (nested DTO)
    /// </summary>
    public CategoryDto? Category { get; init; }
    
    /// <summary>
    /// Photo URLs (required, 1-10 items)
    /// </summary>
    public List<string> PhotoUrls { get; init; } = new();
    
    /// <summary>
    /// Tags (optional)
    /// </summary>
    public List<TagDto>? Tags { get; init; }
    
    /// <summary>
    /// Pet status (available, pending, sold)
    /// </summary>
    public string? Status { get; init; }
}
```

**Key Features**:
- C# record type (immutable with init-only properties)
- Nullable properties for optional fields
- Nested DTOs (CategoryDto, TagDto) instead of Models
- Summary comments from OpenAPI description
- Default initializers for required collections

---

## Example 2: CategoryDto

**OpenAPI Source**: Nested object in Pet schema

**Generated File**: `DTOs/CategoryDto.cs`

```csharp
namespace PetstoreApi.DTOs;

/// <summary>
/// DTO for Category
/// Generated from OpenAPI schema
/// </summary>
public record CategoryDto
{
    /// <summary>
    /// Category ID
    /// </summary>
    public long? Id { get; init; }
    
    /// <summary>
    /// Category name (minimum 1 char)
    /// </summary>
    public string? Name { get; init; }
}
```

**Key Features**:
- Separate class for nested object
- Can be reused by multiple DTOs
- Simple structure with 2 properties

---

## Example 3: TagDto

**OpenAPI Source**: Nested array item in Pet schema

**Generated File**: `DTOs/TagDto.cs`

```csharp
namespace PetstoreApi.DTOs;

/// <summary>
/// DTO for Tag
/// Generated from OpenAPI schema
/// </summary>
public record TagDto
{
    /// <summary>
    /// Tag ID
    /// </summary>
    public long? Id { get; init; }
    
    /// <summary>
    /// Tag name
    /// </summary>
    public string? Name { get; init; }
}
```

---

## Example 4: UpdatePetDto

**OpenAPI Source**: PUT /pet requestBody with Pet schema

**Generated File**: `DTOs/UpdatePetDto.cs`

```csharp
namespace PetstoreApi.DTOs;

/// <summary>
/// DTO for Update Pet operation
/// Generated from OpenAPI requestBody schema
/// </summary>
public record UpdatePetDto
{
    /// <summary>
    /// Pet ID (required for update)
    /// </summary>
    public long Id { get; init; }
    
    /// <summary>
    /// Pet name (required, 1-100 chars)
    /// </summary>
    public string Name { get; init; } = string.Empty;
    
    /// <summary>
    /// Category information (nested DTO)
    /// </summary>
    public CategoryDto? Category { get; init; }
    
    /// <summary>
    /// Photo URLs (required, 1-10 items)
    /// </summary>
    public List<string> PhotoUrls { get; init; } = new();
    
    /// <summary>
    /// Tags (optional)
    /// </summary>
    public List<TagDto>? Tags { get; init; }
    
    /// <summary>
    /// Pet status
    /// </summary>
    public string? Status { get; init; }
}
```

**Note**: Identical structure to AddPetDto in this case, but could differ if requestBody schemas are different.

---

## Example 5: PlaceOrderDto

**OpenAPI Source**: POST /store/order requestBody with Order schema

**Generated File**: `DTOs/PlaceOrderDto.cs`

```csharp
namespace PetstoreApi.DTOs;

/// <summary>
/// DTO for Place Order operation
/// Generated from OpenAPI requestBody schema
/// </summary>
public record PlaceOrderDto
{
    /// <summary>
    /// Order ID
    /// </summary>
    public long? Id { get; init; }
    
    /// <summary>
    /// Pet ID being ordered
    /// </summary>
    public long? PetId { get; init; }
    
    /// <summary>
    /// Order quantity (1-1000)
    /// </summary>
    public int? Quantity { get; init; }
    
    /// <summary>
    /// Ship date
    /// </summary>
    public DateTime? ShipDate { get; init; }
    
    /// <summary>
    /// Order status (placed, approved, delivered)
    /// </summary>
    public string? Status { get; init; }
    
    /// <summary>
    /// Completion status
    /// </summary>
    public bool? Complete { get; init; }
}
```

**Key Features**:
- DateTime property mapped from OpenAPI date-time format
- Boolean property for complete flag
- Integer with validation constraints (1-1000)

---

## Example 6: CreateUserDto

**OpenAPI Source**: POST /user requestBody with User schema

**Generated File**: `DTOs/CreateUserDto.cs`

```csharp
namespace PetstoreApi.DTOs;

/// <summary>
/// DTO for Create User operation
/// Generated from OpenAPI requestBody schema
/// </summary>
public record CreateUserDto
{
    /// <summary>
    /// User ID
    /// </summary>
    public long? Id { get; init; }
    
    /// <summary>
    /// Username (3-50 chars)
    /// </summary>
    public string? Username { get; init; }
    
    /// <summary>
    /// First name
    /// </summary>
    public string? FirstName { get; init; }
    
    /// <summary>
    /// Last name
    /// </summary>
    public string? LastName { get; init; }
    
    /// <summary>
    /// Email (validated by regex pattern)
    /// </summary>
    public string? Email { get; init; }
    
    /// <summary>
    /// Password
    /// </summary>
    public string? Password { get; init; }
    
    /// <summary>
    /// Phone number
    /// </summary>
    public string? Phone { get; init; }
    
    /// <summary>
    /// User status
    /// </summary>
    public int? UserStatus { get; init; }
}
```

**Key Features**:
- String properties with length constraints (username)
- Email property with pattern validation
- Integer status code

---

## DTO Naming Convention

| Operation | Request Body Schema | Generated DTO Name |
|-----------|-------------------|-------------------|
| POST /pet | Pet | AddPetDto |
| PUT /pet | Pet | UpdatePetDto |
| POST /store/order | Order | PlaceOrderDto |
| POST /user | User | CreateUserDto |
| PUT /user/{username} | User | UpdateUserDto |

**Pattern**: `{OperationId}Dto` or `{Verb}{SchemaName}Dto`

---

## DTO vs Model Comparison

### AddPetDto (API Contract)
```csharp
public record AddPetDto
{
    public string Name { get; init; } = string.Empty;  // Required, validated
    public CategoryDto? Category { get; init; }        // Nested DTO
}
```

### Pet (Domain Model)
```csharp
public class Pet
{
    public string Name { get; set; }           // Mutable
    public Category? Category { get; set; }    // Nested Model (not DTO)
}
```

**Key Differences**:
1. DTO is record (immutable), Model is class (mutable)
2. DTO references nested DTOs, Model references nested Models
3. DTO has validation metadata (enforced by validator), Model does not
4. DTO evolves with API contract, Model evolves with domain logic

---

**Status**: DTO examples complete. See [dto-validator-examples.md](dto-validator-examples.md) for validation rules.
