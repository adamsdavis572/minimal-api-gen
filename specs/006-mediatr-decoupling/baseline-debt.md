# Technical Debt Baseline: Current api.mustache Implementation

**Date**: 2025-11-24  
**Feature**: 006-mediatr-decoupling  
**Purpose**: Document Pet-specific logic currently embedded in templates that must be extracted to handler implementations

## Overview

The current `api.mustache` template contains hardcoded Pet-specific business logic that makes it unsuitable for production use with other OpenAPI specifications. This document catalogs all technical debt that will be removed and migrated to test handler implementations.

## Technical Debt Items

### 1. Vendor Extensions for Pet Operations

**Location**: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java` (addOperationToGroup method)

**Current Code**:
```java
// Lines that set vendor extensions based on operationId
vendorExtensions.put("x-isPetApi", true);
vendorExtensions.put("x-isAddPet", operationId.equals("addPet"));
vendorExtensions.put("x-isGetPetById", operationId.equals("getPetById"));
vendorExtensions.put("x-isUpdatePet", operationId.equals("updatePet"));
vendorExtensions.put("x-isDeletePet", operationId.equals("deletePet"));
```

**Problem**: Hardcodes assumptions about Petstore API structure into generator code.

**Impact**: Makes generator unusable for non-Petstore APIs without modification.

---

### 2. In-Memory Pet Storage Data Structures

**Location**: `generator/src/main/resources/aspnet-minimalapi/api.mustache` (lines 14-19)

**Current Code**:
```mustache
{{#operations}}{{#operation}}{{#-first}}{{#vendorExtensions.x-isPetApi}}
// In-memory Pet storage
private static readonly Dictionary<long, Pet> _petStore = new();
private static long _nextId = 1;
private static readonly object _lock = new();
{{/vendorExtensions.x-isPetApi}}{{/-first}}{{/operation}}{{/operations}}
```

**Problem**: 
- Template contains domain-specific data structures (Dictionary<long, Pet>)
- Assumes all APIs need in-memory storage
- Hardcodes Pet model name

**Impact**: Generated code contains Pet-specific logic that doesn't generalize to other APIs.

---

### 3. AddPet Operation Implementation

**Location**: `generator/src/main/resources/aspnet-minimalapi/api.mustache` (lines 70-80)

**Current Code**:
```mustache
{{#vendorExtensions.x-isAddPet}}
// Assign ID and store pet
lock (_lock)
{
    {{{paramName}}}.Id = _nextId++;
    _petStore[{{{paramName}}}.Id] = {{{paramName}}};
    return Results.Created($"{{baseName}}/pet/{{{paramName}}}.Id}", {{{paramName}}});
}
{{/vendorExtensions.x-isAddPet}}
```

**Logic Implemented**:
1. Thread-safe lock acquisition
2. ID assignment using auto-increment counter
3. Storage in dictionary using ID as key
4. Return 201 Created with location header

**Problem**: Business logic embedded in template, not configurable or replaceable.

---

### 4. GetPetById Operation Implementation

**Location**: `generator/src/main/resources/aspnet-minimalapi/api.mustache` (lines 81-93)

**Current Code**:
```mustache
{{#vendorExtensions.x-isGetPetById}}
// Retrieve pet from storage
lock (_lock)
{
    if (_petStore.TryGetValue(petId, out var pet))
    {
        return Results.Ok(pet);
    }
    return Results.NotFound();
}
{{/vendorExtensions.x-isGetPetById}}
```

**Logic Implemented**:
1. Thread-safe dictionary lookup
2. Return 200 OK with Pet object if found
3. Return 404 Not Found if not found

**Problem**: Hardcoded CRUD logic in template.

---

### 5. UpdatePet Operation Implementation

**Location**: `generator/src/main/resources/aspnet-minimalapi/api.mustache` (lines 94-107)

**Current Code**:
```mustache
{{#vendorExtensions.x-isUpdatePet}}
// Update pet in storage
lock (_lock)
{
    if (_petStore.ContainsKey({{{paramName}}}.Id))
    {
        _petStore[{{{paramName}}}.Id] = {{{paramName}}};
        return Results.Ok({{{paramName}}});
    }
    return Results.NotFound();
}
{{/vendorExtensions.x-isUpdatePet}}
```

**Logic Implemented**:
1. Check if Pet exists by ID
2. Update dictionary entry
3. Return 200 OK with updated Pet
4. Return 404 Not Found if ID doesn't exist

**Problem**: Update logic hardcoded in template.

---

### 6. DeletePet Operation Implementation

**Location**: `generator/src/main/resources/aspnet-minimalapi/api.mustache` (lines 108-120)

**Current Code**:
```mustache
{{#vendorExtensions.x-isDeletePet}}
// Delete pet from storage
lock (_lock)
{
    if (_petStore.Remove(petId))
    {
        return Results.NoContent();
    }
    return Results.NotFound();
}
{{/vendorExtensions.x-isDeletePet}}
```

**Logic Implemented**:
1. Thread-safe removal from dictionary
2. Return 204 No Content if removed successfully
3. Return 404 Not Found if ID doesn't exist

**Problem**: Delete logic hardcoded in template.

---

### 7. Fallback TODO Comments

**Location**: `generator/src/main/resources/aspnet-minimalapi/api.mustache` (line 121)

**Current Code**:
```mustache
{{^vendorExtensions.x-isAddPet}}{{^vendorExtensions.x-isGetPetById}}{{^vendorExtensions.x-isUpdatePet}}{{^vendorExtensions.x-isDeletePet}}
// TODO: Implement {{operationId}} logic
return Results.Ok(new {{returnType}}());
{{/vendorExtensions.x-isDeletePet}}{{/vendorExtensions.x-isUpdatePet}}{{/vendorExtensions.x-isGetPetById}}{{/vendorExtensions.x-isAddPet}}
```

**Logic**: For non-Pet operations, generates TODO stub returning default instance.

**Problem**: Nested conditionals create complex template logic.

---

## Migration Strategy

### Phase 1: Remove Technical Debt (US5)
1. Remove vendor extension assignments from MinimalApiServerCodegen.java
2. Remove ALL vendor extension conditionals from api.mustache
3. Remove in-memory data structures (Dictionary, _nextId, _lock)
4. Remove all CRUD implementation logic
5. Replace with simple TODO stubs for useMediatr=false case
6. **Expected Result**: Tests will FAIL (RED) - endpoints are now empty stubs

### Phase 2: Implement MediatR Pattern (US1, US2, US4)
1. Generate Command/Query classes for operations
2. Modify endpoints to delegate to IMediator.Send()
3. Register MediatR in DI container
4. **Expected Result**: Tests still FAIL - no handler implementations yet

### Phase 3: Migrate Logic to Test Handlers (TDD Validation)
1. Create InMemoryPetStore service in test fixtures
2. Implement AddPetCommandHandler with same logic as removed template code
3. Implement GetPetByIdQueryHandler with same logic
4. Implement UpdatePetCommandHandler with same logic
5. Implement DeletePetCommandHandler with same logic
6. Register handlers and store in test DI container
7. **Expected Result**: Tests PASS (GREEN) - business logic now in handlers

---

## Success Criteria

After migration:
- ✅ **SC-006**: api.mustache contains zero vendor extension conditionals
- ✅ **SC-007**: MinimalApiServerCodegen.java addOperationToGroup contains zero vendor extension assignments
- ✅ **SC-008**: No Pet-specific or Petstore-specific logic in any template file
- ✅ **SC-005**: All 7 baseline tests pass with useMediatr=true (using handler implementations)
- ✅ **Principle II**: Pet-specific logic extracted from template to proper handler implementations

---

## Baseline Test Requirements

The following 7 tests currently pass with embedded template logic and MUST continue to pass after migration to handlers:

1. `AddPet_WithValidData_Returns201Created` - POST /pet creates Pet, assigns ID, returns 201
2. `GetPet_WithExistingId_ReturnsPet` - GET /pet/{id} retrieves stored Pet, returns 200
3. `GetPet_WithNonExistentId_Returns404NotFound` - GET /pet/{id} with invalid ID returns 404
4. `UpdatePet_WithValidData_Returns200OK` - PUT /pet updates existing Pet, returns 200
5. `UpdatePet_WithNonExistentId_Returns404NotFound` - PUT /pet with invalid ID returns 404
6. `DeletePet_WithExistingId_Returns204NoContent` - DELETE /pet/{id} removes Pet, returns 204
7. `DeletePet_WithNonExistentId_Returns404NotFound` - DELETE /pet/{id} with invalid ID returns 404

---

## Code Extraction Mapping

| Template Location | Target Handler | Business Logic |
|-------------------|----------------|----------------|
| api.mustache lines 14-19 | InMemoryPetStore.cs | Dictionary, _nextId, _lock |
| api.mustache lines 70-80 | AddPetCommandHandler.cs | ID assignment, storage, Created response |
| api.mustache lines 81-93 | GetPetByIdQueryHandler.cs | Dictionary lookup, Ok/NotFound response |
| api.mustache lines 94-107 | UpdatePetCommandHandler.cs | Existence check, update, Ok/NotFound response |
| api.mustache lines 108-120 | DeletePetCommandHandler.cs | Remove operation, NoContent/NotFound response |

---

## Timeline

- **Phase 1 (US5)**: Task T017-T025 - Remove technical debt (RED phase)
- **Phase 2 (US1-US4)**: Task T026-T059 - Implement MediatR pattern (still RED)
- **Phase 3 (TDD)**: Task T074-T085 - Extract logic to handlers (GREEN phase)

Current Status: **Baseline Documented** ✅ (T002 complete)
