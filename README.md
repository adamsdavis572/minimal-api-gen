# ASP.NET Core Minimal API Generator

A custom OpenAPI Generator for creating clean, modern ASP.NET Core Minimal APIs with optional MediatR support. This generator produces production-ready code following best practices including CQRS patterns, proper HTTP status codes, and comprehensive test coverage.

## Features

- **ASP.NET Core Minimal APIs**: Generates lightweight, performant endpoint definitions
- **MediatR Integration**: Optional CQRS pattern with Commands, Queries, and Handlers
- **Proper HTTP Semantics**: 
  - POST returns 201 Created with Location header
  - DELETE returns 204 NoContent (success) or 404 NotFound
  - GET/PUT return 404 for missing resources
- **Type-Safe Models**: Uses C# records for immutable DTOs
- **FluentValidation**: Request validation infrastructure
- **Swagger/OpenAPI**: Automatic API documentation
- **Test Infrastructure**: Complete test suite with WebApplicationFactory

## Prerequisites

- Java 11+ (for building the generator)
- .NET 8.0 SDK (for generated code)
- Maven 3.8.9+ (managed via devbox)
- [Devbox](https://www.jetpack.io/devbox) (for reproducible builds)

## Quick Start

### 1. Build the Generator

```bash
cd generator
devbox run mvn clean package
```

### 2. Generate Your API

```bash
# Basic generation (without MediatR)
./run-generator.sh

# With MediatR/CQRS support
./run-generator.sh --additional-properties useMediatr=true
```

Generated code appears in the `test-output/` directory.

### 3. Run Tests

```bash
# Quick test (assumes code already generated)
./test.sh

# Complete regeneration workflow with tests
./regenerate.sh --test
```

## Generator Usage

### Command Line Options

```bash
./run-generator.sh [--additional-properties key=value,key2=value2]
```

**Available Properties:**

- `useMediatr=true|false` - Enable MediatR/CQRS pattern (default: true)
- `packageName=YourApi` - Set the root namespace (default: PetstoreApi)

### Example Commands

```bash
# Generate with custom namespace
./run-generator.sh --additional-properties packageName=MyShopApi

# Generate with MediatR enabled
./run-generator.sh --additional-properties useMediatr=true

# Multiple properties
./run-generator.sh --additional-properties useMediatr=true,packageName=InventoryApi
```

## Workflow Scripts

### `regenerate.sh`

Complete regeneration from scratch:

```bash
# Regenerate without running tests
./regenerate.sh

# Regenerate and run tests
./regenerate.sh --test
```

**What it does:**
1. Deletes old generated code
2. Runs the generator
3. Copies test project from master location
4. Copies test handlers from master location
5. Optionally runs tests

### `test.sh`

Quick test execution for iterative development:

```bash
./test.sh
```

**What it does:**
1. Copies test handlers over stub handlers
2. Runs the test suite

Use this when you've made changes to handlers and want to re-test without regenerating everything.

### `copy-test-handlers.sh`

Internal script that copies test handler implementations over generated stubs. Called by other scripts, not typically used directly.

## Project Structure

```
minimal-api-gen/
├── generator/                          # OpenAPI Generator implementation
│   ├── src/
│   │   └── main/
│   │       ├── java/                   # Generator Java code
│   │       └── resources/
│   │           └── aspnet-minimalapi/  # Mustache templates
│   ├── tests/                          # Master copies (persist across regenerations)
│   │   ├── TestHandlers/               # Handler implementations
│   │   │   ├── AddPetCommandHandler.cs
│   │   │   ├── GetPetByIdQueryHandler.cs
│   │   │   ├── UpdatePetCommandHandler.cs
│   │   │   ├── DeletePetCommandHandler.cs
│   │   │   └── InMemoryPetStore.cs
│   │   └── PetstoreApi.Tests/          # Test project
│   │       ├── PetEndpointTests.cs
│   │       ├── CustomWebApplicationFactory.cs
│   │       └── PetstoreApi.Tests.csproj
│   ├── run-generator.sh                # Generate code
│   ├── regenerate.sh                   # Complete workflow
│   ├── test.sh                         # Quick test
│   └── copy-test-handlers.sh           # Copy handlers
└── test-output/                        # Generated code (ephemeral)
    ├── src/PetstoreApi/
    │   ├── Commands/                   # MediatR commands
    │   ├── Queries/                    # MediatR queries
    │   ├── Handlers/                   # Handler stubs (overwritten by tests/TestHandlers/)
    │   ├── Models/                     # DTOs
    │   ├── Features/                   # Endpoint definitions
    │   └── Program.cs
    └── tests/PetstoreApi.Tests/        # Copied from generator/tests/
```

## Generated Code Structure

### With MediatR Enabled

```
src/PetstoreApi/
├── Commands/           # Write operations (POST, PUT, DELETE)
│   ├── AddPetCommand.cs
│   ├── UpdatePetCommand.cs
│   └── DeletePetCommand.cs
├── Queries/            # Read operations (GET)
│   └── GetPetByIdQuery.cs
├── Handlers/           # Business logic implementations
│   ├── AddPetCommandHandler.cs
│   ├── UpdatePetCommandHandler.cs
│   ├── DeletePetCommandHandler.cs
│   └── GetPetByIdQueryHandler.cs
├── Models/             # DTOs
│   └── Pet.cs
├── Features/           # Endpoint definitions
│   └── PetApiEndpoints.cs
└── Program.cs          # App configuration
```

### DELETE Operation Example

The generator properly handles DELETE operations with boolean returns:

```csharp
// Command
public record DeletePetCommand : IRequest<bool>
{
    public long petId { get; init; }
}

// Handler
public class DeletePetCommandHandler : IRequestHandler<DeletePetCommand, bool>
{
    public async Task<bool> Handle(DeletePetCommand request, CancellationToken cancellationToken)
    {
        var pet = _petStore.GetById(request.petId);
        if (pet == null)
            return false; // Not found
            
        _petStore.Delete(request.petId);
        return true; // Deleted successfully
    }
}

// Endpoint
group.MapDelete("/pet/{petId}", async (IMediator mediator, long petId) =>
{
    var command = new DeletePetCommand { petId = petId };
    var result = await mediator.Send(command);
    return result ? Results.NoContent() : Results.NotFound();
});
```

## Testing

The generator includes a complete test suite with 7 baseline tests:

```bash
cd generator
./regenerate.sh --test
```

**Tests cover:**
- ✅ POST: Add pet returns 201 Created
- ✅ GET: Retrieve existing pet returns 200 OK
- ✅ GET: Non-existent pet returns 404 NotFound
- ✅ PUT: Update existing pet returns 200 OK
- ✅ PUT: Update non-existent pet returns 404 NotFound
- ✅ DELETE: Delete existing pet returns 204 NoContent
- ✅ DELETE: Delete non-existent pet returns 404 NotFound

### Test Architecture

Tests use:
- **xUnit** - Test framework
- **FluentAssertions** - Readable assertions
- **WebApplicationFactory** - Integration testing
- **In-Memory Store** - Test data isolation

Each test run uses a fresh in-memory data store, ensuring test isolation.

## Customizing the Generator

### Modifying Templates

Templates are in `generator/src/main/resources/aspnet-minimalapi/`:

- `api.mustache` - Endpoint definitions
- `command.mustache` - MediatR commands
- `query.mustache` - MediatR queries
- `handler.mustache` - Handler stubs
- `model.mustache` - DTO models
- `program.mustache` - Application setup

After modifying templates:

```bash
cd generator
devbox run mvn clean package
./regenerate.sh --test
```

### Extending the Java Code

The generator implementation is in:
```
generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java
```

Key methods:
- `getMediatrResponseType()` - Determines MediatR response types
- `fromOperation()` - Processes OpenAPI operations
- `postProcessOperationsWithModels()` - Adds template variables

## Development Workflow

### Adding New Features

1. Modify templates or Java code
2. Rebuild generator: `devbox run mvn clean package`
3. Test generation: `./regenerate.sh --test`
4. Verify all tests pass
5. Commit changes

### Debugging

View full generation output:
```bash
./run-generator.sh --additional-properties useMediatr=true
```

Build and test manually:
```bash
cd test-output
devbox run dotnet build
devbox run dotnet test tests/PetstoreApi.Tests/PetstoreApi.Tests.csproj
```

## OpenAPI Specification

Place your OpenAPI spec at:
```
generator/petstore.yaml
```

The generator uses this spec to create your API. Modify it to match your domain.

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make changes and ensure tests pass: `./regenerate.sh --test`
4. Commit with clear messages
5. Push and create a pull request

## License

[Your License Here]

## Acknowledgments

Built on [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) framework.
