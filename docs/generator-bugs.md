# Generator Bugs — WTW Object Management API

Bugs discovered when building the generated output (`alt-oas-gen/output/`) against the
`WTW-ICT-ObjectManagementApi-1.0.0.yaml` spec. All were fixed in-place on the output;
the root causes lie in the generator templates and should be addressed there.

---

## BUG-001 — Kebab-case path params in string interpolations

**Files:** `Features/ObjectVersionsApiEndpoints.cs`, `Features/ObjectPropertiesApiEndpoints.cs`,
`Features/ObjectReferencesApiEndpoints.cs`

**Symptom:** `CS1525 Invalid expression term 'object'`

**Cause:** The generator emits the raw OpenAPI path parameter names (kebab-case) directly into
C# string interpolations. `{object-id}` is parsed by the compiler as `{object - id}` where
`object` is a reserved keyword.

```csharp
// Generated (broken)
return Results.Created($"/objects/{object-id}/versions", result);

// Fixed
return Results.Created($"/objects/{objectId}/versions", result);
```

**Fix:** Replace `{object-id}` → `{objectId}` and `{version-id}` → `{versionId}` in all
`Results.Created(...)` location strings.

---

## BUG-002 — `System.IO.StreamDto` used as a return type

**Files:** `Queries/ObjectFileGetQuery.cs`, `Handlers/ObjectFileGetQueryHandler.cs`,
`Features/ObjectFilesApiEndpoints.cs`

**Symptom:** `CS0246 The type or namespace name 'StreamDto' could not be found` (in namespace
`System.IO`)

**Cause:** The generator failed to resolve the schema for the file-download endpoint (an
OpenAPI `format: binary` / `type: string` response) and emitted the nonsensical type
`System.IO.StreamDto` instead of a real DTO type.

```csharp
// Generated (broken)
public record ObjectFileGetQuery : IRequest<System.IO.StreamDto>

// Fixed
public record ObjectFileGetQuery : IRequest<FileDto>
```

**Fix:** Replace `System.IO.StreamDto` with `FileDto` throughout the three affected files.

---

## BUG-003 — Missing `using AlternativeApi.Models` in query and feature files

**Files:** `Queries/ObjectsListQuery.cs`, `Queries/ObjectGetQuery.cs`,
`Features/ObjectsApiEndpoints.cs`

**Symptom:** `CS0246 The type or namespace name 'ObjectType' could not be found`

**Cause:** These files reference the `ObjectType` domain enum (from the Models namespace) in
property declarations and endpoint route handlers, but the generator only emitted
`using AlternativeApi.DTOs;` and omitted `using AlternativeApi.Models;`.

**Fix:** Add `using AlternativeApi.Models;` to the affected files.

---

## BUG-004 — Inline LINQ initialisers use wrong list types (`Children`, `Enablements`)

**Files:** `Handlers/ObjectCreateCommandHandler.cs`, `Handlers/ObjectGetQueryHandler.cs`,
`Handlers/ObjectUpdateCommandHandler.cs`, `Handlers/ObjectsListQueryHandler.cs`,
`Handlers/ObjectVersionsListQueryHandler.cs`

**Symptom:** `CS0029 Cannot implicitly convert type 'List<ObjectItem>' to 'List<ObjectItemDto>'`
(and similar for `VersionEnablement` / `VersionEnablementDto`)

**Cause:** The generator produces flattened inline LINQ projections for list properties inside
mapping methods. When projecting a domain model to a DTO (or vice-versa), nested list properties
(`Children`, `Enablements`) are copied verbatim without conversion, causing type mismatches.

```csharp
// Generated (broken) — copies the domain List<ObjectItem> into a DTO property
// that expects List<ObjectItemDto>
new ObjectItemDto { ..., Children = o.Children, Enablements = o.Enablements }

// Fixed (stub — correct recursive mapping would require deeper template changes)
new ObjectItemDto { ..., Children = null, Enablements = null }
```

**Fix:** Replace `Children = o.Children` and `Enablements = *.Enablements` with `null` in
the inline initialisers. A proper fix in the generator would require recursive projection
helpers for nested list types.

---

## BUG-005 — Unqualified `(ReferenceTypeEnum)` / `(ExternalSystemEnum)` casts

**Files:** `Handlers/ObjectReferencesListQueryHandler.cs`,
`Handlers/ObjectVersionReferencesListQueryHandler.cs`

**Symptom:** `CS0246 The type or namespace name 'ReferenceTypeEnum' could not be found`

**Cause:** The inline projection inside `MapDomainToDto` casts enum values using unqualified
names. These nested enums are defined inside `ObjectItemReferenceDto` and require a fully
qualified cast.

```csharp
// Generated (broken)
ReferenceType = (ReferenceTypeEnum)(int)o.ReferenceType,
ExternalSystem = (ExternalSystemEnum)(int)o.ExternalSystem,

// Fixed
ReferenceType = (ObjectItemReferenceDto.ReferenceTypeEnum)(int)o.ReferenceType,
ExternalSystem = (ObjectItemReferenceDto.ExternalSystemEnum)(int)o.ExternalSystem,
```

**Fix:** Qualify all enum casts with their parent DTO type in the affected handlers.

---

## BUG-006 — `.HasValue` / `.Value` called on a non-nullable enum

**File:** `Handlers/ObjectReferencesCreateCommandHandler.cs`

**Symptom:** `CS1061 'ReferenceTypeEnum' does not contain a definition for 'HasValue'`

**Cause:** The generated `ObjectReferencesCreateDto` declares `ReferenceType` and
`ExternalSystem` as non-nullable enums, but the `MapDtoToDomain` method in the handler
treats them as `Nullable<T>` and calls `.HasValue` / `.Value`.

```csharp
// Generated (broken)
ReferenceType = dto.ReferenceType.HasValue
    ? MapReferenceTypeDtoToModel(dto.ReferenceType.Value)
    : ObjectItemReference.ReferenceTypeEnum.PythonModelDeploymentEnum,

// Fixed
ReferenceType = MapReferenceTypeDtoToModel(dto.ReferenceType),
```

The helper method `MapReferenceTypeDtoToModel` was also generated with a nullable parameter
signature (`ReferenceTypeEnum?`) despite the DTO property being non-nullable; it handles
`null` internally so passing the non-nullable value is safe.

**Fix:** Remove the null-guard ternary expressions and call the mapper directly.

---

## BUG-007 — `FolderHierarchyItemDto.Children` assigned from wrong type in mapping

**File:** `Handlers/FolderHierarchyGetQueryHandler.cs`

**Symptom:** `CS0029 Cannot implicitly convert type 'List<FolderHierarchyItem>'` (domain) `to 'List<FolderHierarchyItemDto>'`

**Cause:** The generated shallow mapper copies `f.Children` (a `List<FolderHierarchyItem>`)
directly into the `Children` property of `FolderHierarchyItemDto` (which expects
`List<FolderHierarchyItemDto>`).

```csharp
// Generated (broken)
Children = model.Children?.Select(f =>
    new FolderHierarchyItemDto { Id = f.Id, Name = f.Name, Children = f.Children }).ToList(),

// Fixed in generated file (stub)
Children = model.Children?.Select(f =>
    new FolderHierarchyItemDto { Id = f.Id, Name = f.Name }).ToList(),
```

The `FolderHierarchyGetQueryHandler.Impl.cs` companion file works around this by
implementing a recursive `MapRecursive` helper instead of relying on the generated
`MapDomainToDto` method at all.

**Fix (generated file):** Omit the `Children` assignment in the inline initialiser.  
**Fix (impl file):** Use a hand-written `MapRecursive` that correctly projects the full tree.
