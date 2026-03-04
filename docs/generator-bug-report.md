# Generator Bug Report

Bugs identified in the output of the `aspnet-minimalapi` OpenAPI Generator during manual implementation of the `WTW.ObjectManager` in-memory service layer. All bugs below were found in generated files under `generated/` and required manual correction to achieve a clean build.

---

## Summary

| # | Severity | Area | File(s) |
|---|---|---|---|
| 1 | Error | Contracts / Endpoints | `ObjectPropertiesApiEndpoints.cs`, `ObjectReferencesApiEndpoints.cs`, `ObjectVersionsApiEndpoints.cs` |
| 2 | Error | Contracts / Queries | `ObjectFileGetQuery.cs` |
| 3 | Error | Contracts / Queries + Endpoints | `ObjectsListQuery.cs`, `ObjectGetQuery.cs`, `ObjectsApiEndpoints.cs`, `ObjectFilesApiEndpoints.cs` |
| 4 | Error | Contracts / Validators | `VersionEnablementDtoValidator.cs` |
| 5 | Error | Contracts / Validators | `ObjectCreateDtoValidator.cs`, `ObjectItemDtoValidator.cs` |
| 6 | Error | Contracts / Validators | `ObjectPropertyCreateDtoValidator.cs`, `ObjectPropertyUpdateDtoValidator.cs` |
| 7 | Error | Contracts / Validators | `ObjectReferencesCreateDtoValidator.cs` |
| 8 | Error | Handler Scaffolds | `ObjectFileGetQueryHandler.cs` |
| 9 | Error | Handler Scaffolds | Multiple ObjectItem handlers (Children / CurrentVersion inline mapping) |
| 10 | Error | Handler Scaffolds | Multiple ObjectItem handlers (ObjectType enum cross-namespace cast) |
| 11 | Error | Handler Scaffolds | Multiple ObjectItem handlers (ProductEnablement enum cross-namespace cast) |
| 12 | Error | Handler Scaffolds | Multiple ObjectItem handlers (PropertyType non-nullable struct) |
| 13 | Error | Handler Scaffolds | `ObjectReferencesCreateCommandHandler.cs`, `ObjectVersionReferencesListQueryHandler.cs` (ReferenceType / ExternalSystem enum casts) |

---

## Detailed Bug Descriptions

---

### Bug 1 — Hyphenated path parameters used in `Results.Created()` string interpolation

**Files:**
- `generated/Contract/Endpoints/ObjectPropertiesApiEndpoints.cs`
- `generated/Contract/Endpoints/ObjectReferencesApiEndpoints.cs`
- `generated/Contract/Endpoints/ObjectVersionsApiEndpoints.cs`

**Description:**  
The generator emits kebab-case route parameter names (e.g. `{object-id}`, `{version-id}`) inside `Results.Created(...)` location URI strings. These do not match the camelCase lambda parameter names (`objectId`, `versionId`) actually bound from the route. The interpolated variables therefore resolve to `0` or `null` at runtime, producing incorrect `Location` response headers.

**Example (broken):**
```csharp
return TypedResults.Created($"/objects/{object-id}/versions/{result.Id}", result);
```

**Fix:** Replace hyphenated tokens with camelCase variable names:
```csharp
return TypedResults.Created($"/objects/{objectId}/versions/{result.Id}", result);
```

---

### Bug 2 — `System.IO.StreamDto` used as a return type (non-existent type)

**Files:**
- `generated/Contract/Queries/ObjectFileGetQuery.cs`
- `generated/Contract/Endpoints/ObjectFilesApiEndpoints.cs`
- `generated/src/WTW.ObjectManager/Handlers/ObjectFileGetQueryHandler.cs`

**Description:**  
The generator emits `System.IO.StreamDto` as the MediatR `IRequest<T>` return type and as the `.Produces<T>()` type for the file-download endpoint — a type that does not exist. The correct generated DTO is `FileDto`.

**Example (broken):**
```csharp
public record ObjectFileGetQuery(...) : IRequest<System.IO.StreamDto>;
```

**Fix:**
```csharp
public record ObjectFileGetQuery(...) : IRequest<FileDto>;
```

---

### Bug 3 — `ObjectType` (Models enum) referenced in the Contracts project

**Files:**
- `generated/Contract/Queries/ObjectsListQuery.cs`
- `generated/Contract/Queries/ObjectGetQuery.cs`
- `generated/Contract/Endpoints/ObjectsApiEndpoints.cs`
- `generated/Contract/Endpoints/ObjectFilesApiEndpoints.cs`

**Description:**  
Query records and endpoint lambda parameters use `ObjectType[]?` (from the `WTW.ObjectManager.Models` namespace), but the Contracts project has no reference to the implementation project and cannot resolve that type. The correct type in the Contracts layer is `ObjectTypeDto[]?` from `WTW.ObjectManager.DTOs`.

**Example (broken):**
```csharp
public record ObjectsListQuery(ObjectType[]? objectType, ...) : IRequest<ObjectCollectionDto>;
```

**Fix:**
```csharp
public record ObjectsListQuery(ObjectTypeDto[]? objectType, ...) : IRequest<ObjectCollectionDto>;
```

---

### Bug 4 — Validator uses nested type path and broken `SetValidator` for `VersionEnablementDto`

**File:** `generated/Contract/Validators/VersionEnablementDtoValidator.cs`

**Description:**  
The generated validator references `VersionEnablementDto.ProductEnablementDto` (a non-existent nested type) instead of the top-level `ProductEnablementDto`. It also emits a `SetValidator(new ProductEnablementDtoValidator())` call for an enum property, which is invalid (enums cannot use `SetValidator`).

**Example (broken):**
```csharp
RuleFor(x => x.EnabledFor)
    .IsInEnum()
    .SetValidator(new VersionEnablementDto.ProductEnablementDtoValidator());
```

**Fix:** Correct the type reference and remove the `SetValidator` call:
```csharp
RuleFor(x => x.EnabledFor)
    .IsInEnum();
```

---

### Bug 5 — Validators use nested type path and broken `.Value` on non-nullable enum for `ObjectType`

**Files:**
- `generated/Contract/Validators/ObjectCreateDtoValidator.cs`
- `generated/Contract/Validators/ObjectItemDtoValidator.cs`

**Description:**  
Same pattern as Bug 4. The validator emits `ObjectCreateDto.ObjectTypeDto` (non-existent nested type) and calls `.Must(value => value.Value != ...)` on a non-nullable enum (which has no `.Value` property). A broken `SetValidator` call is also emitted.

**Example (broken):**
```csharp
RuleFor(x => x.ObjectType)
    .Must(value => value.Value != ObjectCreateDto.ObjectTypeDto.NumberMinus1)
    .SetValidator(new ObjectCreateDto.ObjectTypeDtoValidator());
```

**Fix:**
```csharp
RuleFor(x => x.ObjectType)
    .Must(value => value != ObjectTypeDto.NumberMinus1);
```

---

### Bug 6 — Validators use nested type path and broken `.Value` on non-nullable enum for `PropertyType`

**Files:**
- `generated/Contract/Validators/ObjectPropertyCreateDtoValidator.cs`
- `generated/Contract/Validators/ObjectPropertyUpdateDtoValidator.cs`

**Description:**  
Same pattern as Bug 5, but for `PropertyTypeDto`. The generated code references `ObjectPropertyCreateDto.PropertyTypeDto` and calls `.Value` on a non-nullable enum.

**Fix:**
```csharp
RuleFor(x => x.Type)
    .Must(value => value != PropertyTypeDto.NumberMinus1);
```

---

### Bug 7 — Validator calls `.Value` on non-nullable enums for `ReferenceType` / `ExternalSystem`

**File:** `generated/Contract/Validators/ObjectReferencesCreateDtoValidator.cs`

**Description:**  
The validator emits `.Must(value => value.Value != ...)` for `ReferenceType` and `ExternalSystem` enum properties that are non-nullable in their DTO — so `.Value` does not compile.

**Fix:** Remove `.Value`:
```csharp
.Must(value => value != ObjectReferencesCreateDto.ReferenceTypeEnum.NumberMinus1)
```

---

### Bug 8 — Handler scaffold references `System.IO.StreamDto` (see also Bug 2)

**File:** `generated/src/WTW.ObjectManager/Handlers/ObjectFileGetQueryHandler.cs`

**Description:**  
The handler scaffold for the file-download endpoint uses `System.IO.StreamDto` as the return type in method signatures and mapping methods — consistent with Bug 2, but located in the implementation project rather than Contracts.

**Fix:** Replace all `System.IO.StreamDto` references with `FileDto`.

---

### Bug 9 — Inline `Children` / `CurrentVersion` mappings use wrong types in Select lambdas

**Files (MapDomainToDto and/or MapDtoToDomain):**
- `ObjectCreateCommandHandler.cs`
- `ObjectGetQueryHandler.cs`
- `ObjectUpdateCommandHandler.cs`
- `ObjectsListQueryHandler.cs`
- `FolderHierarchyGetQueryHandler.cs`

**Description:**  
Handler scaffold methods that project children collections inline (e.g. `model.Children?.Select(o => new ObjectItemDto { ... Children = o.Children, CurrentVersion = o.CurrentVersion ... })`) assign the source (`ObjectItem`) types directly to the destination DTO properties (`ObjectItemDto`). These types are incompatible and do not compile. Because recursively mapping children is non-trivial, the correct fix for an in-memory implementation is to set `Children = null` in the inline projection.

Similarly, `CurrentVersion = o.CurrentVersion` assigns an `ObjectItemVersion` (model) where an `ObjectItemVersionDto` is expected, or vice versa.

**Example (broken):**
```csharp
Children = model.Children?.Select(o => new ObjectItemDto {
    ...
    Children = o.Children,          // List<ObjectItem> ≠ List<ObjectItemDto>
    CurrentVersion = o.CurrentVersion, // ObjectItemVersion ≠ ObjectItemVersionDto
    ...
}).ToList(),
```

**Fix:**
```csharp
Children = null,
CurrentVersion = null,
```

---

### Bug 10 — `ObjectType` enum not cast between model and DTO namespaces in handler scaffolds

**Files:**
- `ObjectCreateCommandHandler.cs` (MapDtoToDomain and MapDomainToDto)
- `ObjectGetQueryHandler.cs`
- `ObjectUpdateCommandHandler.cs`
- `ObjectsListQueryHandler.cs`
- Various inline Select lambdas

**Description:**  
Handler scaffolds emit `ObjectType = model.ObjectType` or `ObjectType = o.ObjectType` when the source and destination use enums from different namespaces (`WTW.ObjectManager.Models.ObjectType` vs `WTW.ObjectManager.DTOs.ObjectTypeDto`). Even though both enums share the same underlying integer values, there is no implicit conversion.

The generator also emits the broken pattern `ObjectType = dto.ObjectType != null ? new ObjectType { } : null` — attempting to instantiate an enum as a struct with an object initializer, which is invalid C#.

**Example (broken):**
```csharp
ObjectType = dto.ObjectType != null ? new ObjectType {  } : null,
```

**Fix:**
```csharp
ObjectType = (ObjectType)(int)dto.ObjectType,   // MapDtoToDomain
ObjectType = (ObjectTypeDto)(int)model.ObjectType, // MapDomainToDto
```

---

### Bug 11 — `ProductEnablement` enum not cast between model and DTO namespaces in handler scaffolds

**Files:**
- `ObjectVersionsCreateCommandHandler.cs` (MapDtoToDomain and MapDomainToDto)
- `ObjectVersionGetQueryHandler.cs`
- `ObjectVersionsListQueryHandler.cs`
- `ObjectGetQueryHandler.cs` (via `CurrentVersion.Enablements`)
- `ObjectCreateCommandHandler.cs` (via `CurrentVersion.Enablements`)

**Description:**  
Inside `VersionEnablement` / `VersionEnablementDto` inline projections, the generator emits `EnabledFor = v.EnabledFor` directly, crossing namespace boundaries between `WTW.ObjectManager.Models.ProductEnablement` and `WTW.ObjectManager.DTOs.ProductEnablementDto`.

For `MapDtoToDomain`, the generator additionally emits nullable guard boilerplate (`v.EnabledFor.HasValue ? ... : default`) even though `VersionEnablementDto.EnabledFor` is typed as `ProductEnablementDto?`, causing a `.HasValue` / `.Value` pattern that may not match the actual nullability.

**Example (broken):**
```csharp
Enablements = dto.Enablements?.Select(v => new VersionEnablement {
    EnabledFor = v.EnabledFor,   // ProductEnablementDto? ≠ ProductEnablement
    ...
}).ToList(),
```

**Fix:**
```csharp
EnabledFor = v.EnabledFor.HasValue ? (ProductEnablement)(int)v.EnabledFor.Value : default,  // MapDtoToDomain
EnabledFor = (ProductEnablementDto)(int)v.EnabledFor,  // MapDomainToDto
```

---

### Bug 12 — `PropertyType` emitted as nullable struct with object initializer in handler scaffolds

**Files:**
- `ObjectPropertyCreateCommandHandler.cs` (MapDtoToDomain)
- `ObjectPropertyUpdateCommandHandler.cs` (MapDtoToDomain)

**Description:**  
The generator emits `Type = dto.Type != null ? new PropertyType { } : null` in the `MapDtoToDomain` method. `PropertyType` is a non-nullable enum (value type), so the ternary returning `null` does not compile, and constructing an enum via `new PropertyType { }` is invalid C#.

**Example (broken):**
```csharp
Type = dto.Type != null ? new PropertyType {  } : null,
```

**Fix:**
```csharp
Type = (PropertyType)(int)dto.Type,
```

---

### Bug 13 — `ReferenceType` / `ExternalSystem` guard pattern on non-nullable enums in handler scaffolds

**Files:**
- `ObjectReferencesCreateCommandHandler.cs` (MapDtoToDomain)
- `ObjectVersionReferencesListQueryHandler.cs` (MapDomainToDto)

**Description:**  
In `MapDtoToDomain`, the scaffold emits a `.HasValue ? Map...(dto.ReferenceType.Value) : DefaultValue` pattern, treating `ReferenceType` and `ExternalSystem` as nullable. In the actual DTO these are non-nullable enums, so `.HasValue` and `.Value` do not exist. A call to private helper methods (`MapReferenceTypeDtoToModel`, `MapExternalSystemDtoToModel`) is also emitted but those methods are never generated.

In `MapDomainToDto` (ObjectVersionReferencesListQueryHandler), `ReferenceType = o.ReferenceType` and `ExternalSystem = o.ExternalSystem` cross the model ↔ DTO namespace boundary without a cast (same pattern as Bugs 10 and 11).

**Example (broken — MapDtoToDomain):**
```csharp
ReferenceType = dto.ReferenceType.HasValue
    ? MapReferenceTypeDtoToModel(dto.ReferenceType.Value)
    : ObjectItemReference.ReferenceTypeEnum.PythonModelDeploymentEnum,
```

**Fix:**
```csharp
ReferenceType = (ObjectItemReference.ReferenceTypeEnum)(int)dto.ReferenceType,
ExternalSystem = (ObjectItemReference.ExternalSystemEnum)(int)dto.ExternalSystem,
```

---

## Root Cause Analysis

The bugs fall into four broad generator defects:

| Category | Root Cause |
|---|---|
| **Wrong type references in Contracts** | Generator does not distinguish which namespace is visible in each project; emits model types (`ObjectType`, `System.IO.Stream`) into the Contracts project that cannot reference them. |
| **Broken enum mapping** | Generator emits identity assignment (`= o.EnumProp`) across namespace boundaries, emits `new EnumType { }` struct-init syntax for enums, and emits nullable guard patterns (`.HasValue`) for non-nullable enum properties. |
| **Recursive collection mapping** | Generator attempts to recursively map `Children` and `CurrentVersion` inline but produces type-incompatible assignments rather than recursive calls or nulls. |
| **Hyphenated route params in URI strings** | Generator uses kebab-case parameter names from the OpenAPI spec in C# string interpolation, where the bound lambda parameters are camelCase. |
