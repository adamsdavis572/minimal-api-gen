# Test Fixtures for MediatR Handler Implementations

**Purpose**: Contains concrete MediatR handler implementations used to drive integration tests after technical debt removal from templates.

## Structure

This directory will contain:

```
Fixtures/
├── README.md                        # This file
├── InMemoryPetStore.cs              # Shared in-memory storage service
├── AddPetCommandHandler.cs          # Handler for POST /pet
├── GetPetByIdQueryHandler.cs        # Handler for GET /pet/{id}
├── UpdatePetCommandHandler.cs       # Handler for PUT /pet
└── DeletePetCommandHandler.cs       # Handler for DELETE /pet/{id}
```

## Purpose

After removing Pet-specific business logic from the `api.mustache` template (Phase 4, User Story 5), the 7 baseline tests will fail because endpoints become empty stubs. These handler implementations restore the test-passing behavior by:

1. **Extracting** business logic from template to proper C# classes
2. **Demonstrating** the MediatR pattern works correctly
3. **Validating** that endpoints can delegate to handlers successfully
4. **Preserving** exact same behavior as embedded template logic

## Migration Timeline

- **Before US5**: Tests pass with logic embedded in api.mustache template
- **After US5**: Tests FAIL (RED) - templates now generate clean stubs
- **After TDD Phase**: Tests PASS (GREEN) - handlers implement business logic

## Handler Implementation Notes

### InMemoryPetStore.cs
- Singleton service registered in test DI container
- Contains Dictionary<long, Pet>, _nextId counter, _lock object
- Provides thread-safe CRUD operations
- Exact same logic as removed template code

### Handler Classes
- Implement IRequestHandler<TRequest, TResponse>
- Inject InMemoryPetStore via constructor
- Delegate to store for actual operations
- Return appropriate HTTP result types (Pet, Unit)

## Registration

Handlers and store are registered in test WebApplicationFactory:

```csharp
services.AddSingleton<InMemoryPetStore>();
services.AddTransient<AddPetCommandHandler>();
services.AddTransient<GetPetByIdQueryHandler>();
services.AddTransient<UpdatePetCommandHandler>();
services.AddTransient<DeletePetCommandHandler>();
```

## Status

- [x] T003: Directory structure created
- [ ] T074: Create InMemoryPetStore.cs
- [ ] T075: Implement AddPetCommandHandler.cs
- [ ] T076: Implement GetPetByIdQueryHandler.cs
- [ ] T077: Implement UpdatePetCommandHandler.cs
- [ ] T078: Implement DeletePetCommandHandler.cs
- [ ] T079: Register handlers in test WebApplicationFactory
- [ ] T080: Register InMemoryPetStore in test DI
- [ ] T081: Copy handlers to generated project
- [ ] T082: Build with test handlers
- [ ] T083: Run tests - expect GREEN

Created: 2025-11-24
