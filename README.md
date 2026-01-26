# ASP.NET Core Minimal API Generator

A custom OpenAPI Generator for creating clean, modern ASP.NET Core Minimal APIs with optional MediatR support. This generator produces production-ready code following best practices including CQRS patterns, proper HTTP status codes, and comprehensive test coverage.

## Features

- **ASP.NET Core Minimal APIs**: Generates lightweight, performant endpoint definitions
- **True CQRS with DTOs**: Separate Data Transfer Objects from domain Models for clean API boundaries
- **MediatR Integration**: Optional CQRS pattern with Commands, Queries, and Handlers
- **Comprehensive Validation**: FluentValidation with full OpenAPI constraint support (7 constraint types)
- **Production Error Handling**: Global exception handler with RFC 7807 ProblemDetails or simple JSON
- **Proper HTTP Semantics**: 
  - POST returns 201 Created with Location header
  - DELETE returns 204 NoContent (success) or 404 NotFound
  - GET/PUT return 404 for missing resources
  - Validation errors return 400 BadRequest
- **Type-Safe Models**: Uses C# records for immutable DTOs and Models
- **Swagger/OpenAPI**: Automatic API documentation
- **Test Infrastructure**: Complete test suite with WebApplicationFactory

## Prerequisites

### Local Development
- Java 11+ (for building the generator)
- .NET 8.0 SDK (for generated code)
- Maven 3.8.9+ (managed via devbox)
- [Devbox](https://www.jetpack.io/devbox) (for reproducible builds)

### Docker (Alternative)
- Docker (no other dependencies needed)

## Quick Start

### Local Development (Recommended)

Use Taskfile commands for all local development and testing:

```bash
# Complete workflow: build generator, generate code, and run tests
devbox run task regenerate

# Or step-by-step:

# 1. Build the custom generator
devbox run task build-generator

# 2. Generate API code with MediatR/CQRS support
devbox run task generate-petstore-minimal-api

# 3. Run tests
devbox run task test-server-stubs

# 4. Run the generated API server
devbox run task run-petstore-api
```

Generated code appears in the `test-output/` directory.

### Docker Distribution (For CI/CD and External Systems)

Package the generator as a Docker image for use in CI/CD pipelines or other systems:

```bash
# Build the Docker image
devbox run task docker-build

# Test the Docker image
devbox run task docker-test

# Push to registry for distribution
devbox run task docker-push
```

## Generator Usage

### Local Development with Taskfile (Recommended)

For local development and testing, use Taskfile commands directly:

```bash
devbox run task generate-petstore-minimal-api
```

**Available Tasks:**

```bash
# Build the generator
devbox run task build-generator

# Generate API code with MediatR support
devbox run task generate-petstore-minimal-api

# Copy test handlers and stubs
devbox run task copy-test-stubs

# Run tests
devbox run task test-server-stubs

# Complete workflow (generate + test)
devbox run task regenerate

# Quick test (no regeneration)
devbox run task quick-test

# Run the API server
devbox run task run-petstore-api

# List all available tasks
devbox run task --list
```

**Generator Properties:**

The generator supports various configuration options. See the [Configuration Reference](docs/CONFIGURATION.md) for complete details.

Key configuration options:
- `useMediatr=true|false` - Enable MediatR/CQRS with separate DTOs (default: false)
- `useValidators=true|false` - Enable FluentValidation on DTOs (default: false)
- `useGlobalExceptionHandler=true|false` - Enable exception handling middleware (default: true)
- `useProblemDetails=true|false` - Use RFC 7807 format for errors (default: false)
- `packageName=YourApi` - Set the root namespace (default: PetstoreApi)
- `useApiVersioning=true` - Enable API versioning (default: false)

**Architecture Patterns:**

When `useMediatr=true`, the generator creates a true CQRS architecture:
- **DTOs** (`DTOs/`): API contracts matching OpenAPI requestBody schemas
- **Commands/Queries** (`Commands/`, `Queries/`): Reference DTOs (not Models)
- **Handlers** (`Handlers/`): Business logic that maps DTOs to domain Models
- **Models** (`Models/`): Domain entities separate from API contracts

When `useMediatr=false`, the generator uses traditional Minimal API patterns with Models directly in endpoints.

### Docker Usage (For CI/CD and Distribution)

Use the Docker image to integrate the generator into CI/CD pipelines or other external systems:

**Basic Usage:**

```bash
# Mount your project directory (must contain your OpenAPI spec)
docker run --rm -v $(pwd):/workspace adamsdavis/minimal-api-generator:latest \
  generate \
  -g aspnetcore-minimalapi \
  -i /workspace/path/to/your-spec.yaml \
  -o /workspace/output
```

**Example Commands:**

```bash
# Generate with MediatR support
docker run --rm -v $(pwd):/workspace adamsdavis/minimal-api-generator:latest \
  generate -g aspnetcore-minimalapi \
  -i /workspace/specs/petstore.yaml \
  -o /workspace/output \
  --additional-properties useMediatr=true

# Generate with custom namespace
docker run --rm -v $(pwd):/workspace adamsdavis/minimal-api-generator:latest \
  generate -g aspnetcore-minimalapi \
  -i /workspace/specs/petstore.yaml \
  -o /workspace/output \
  --additional-properties packageName=MyShopApi

# Multiple properties
docker run --rm -v $(pwd):/workspace adamsdavis/minimal-api-generator:latest \
  generate -g aspnetcore-minimalapi \
  -i /workspace/specs/petstore.yaml \
  -o /workspace/output \
  --additional-properties useMediatr=true,packageName=InventoryApi
```

**CI/CD Integration:**

```yaml
# GitHub Actions example
- name: Generate API Code
  run: |
    docker run --rm -v ${{ github.workspace }}:/workspace adamsdavis/minimal-api-generator:latest \
      generate -g aspnetcore-minimalapi \
      -i /workspace/specs/api-spec.yaml \
      -o /workspace/generated \
      --additional-properties useMediatr=true
```

## Taskfile Workflows

### Complete Regeneration

```bash
devbox run task regenerate
```

**What it does:**
1. Builds the generator (if needed)
2. Generates server code from OpenAPI spec
3. Copies test handlers and test project
4. Runs the test suite

### Quick Test

For iterative development when code is already generated:

```bash
devbox run task quick-test
```

**What it does:**
1. Copies test handlers over stub handlers
2. Runs the test suite

Use this when you've made changes to handlers and want to re-test without regenerating everything.

### Individual Tasks

```bash
# Just copy test files
devbox run task copy-test-stubs

# Just run tests
devbox run task test-server-stubs

# Clean generated code
devbox run task clean-generated-api

# Clean everything including build artifacts
devbox run task clean-all
```

## Project Structure

```
minimal-api-gen/
├── Taskfile.yml                        # Build automation (replaces bash scripts)
├── generator/                          # OpenAPI Generator implementation
│   ├── src/
│   │   └── main/
│   │       ├── java/                   # Generator Java code
│   │       └── resources/
│   │           └── aspnet-minimalapi/  # Mustache templates
│   └── pom.xml                         # Maven configuration
├── petstore-tests/                     # Master copies (persist across regenerations)
│   ├── TestHandlers/                   # Handler implementations
│   │   ├── AddPetCommandHandler.cs
│   │   ├── GetPetByIdQueryHandler.cs
│   │   ├── UpdatePetCommandHandler.cs
│   │   ├── DeletePetCommandHandler.cs
│   │   └── InMemoryPetStore.cs
│   ├── PetstoreApi.Tests/              # Test project
│   │   ├── PetEndpointTests.cs
│   │   ├── CustomWebApplicationFactory.cs
│   │   └── PetstoreApi.Tests.csproj
│   └── petstore.yaml                   # OpenAPI specification
├── docker/                             # Docker build files
│   ├── Dockerfile
│   └── README.md
└── test-output/                        # Generated code (ephemeral)
    ├── src/PetstoreApi/
    │   ├── Commands/                   # MediatR commands
    │   ├── Queries/                    # MediatR queries
    │   ├── Handlers/                   # Handler stubs (overwritten by petstore-tests/TestHandlers/)
    │   ├── Models/                     # DTOs
    │   ├── Features/                   # Endpoint definitions
    │   ├── Extensions/                 # ServiceCollectionExtensions
    │   └── Program.cs
    └── tests/PetstoreApi.Tests/        # Copied from petstore-tests/
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
devbox run task regenerate
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

## Docker Distribution

The Docker image packages the generator for distribution to CI/CD pipelines, team members, or other systems that need to generate code without local Java/Maven setup.

### Building and Publishing

```bash
# Build the Docker image
devbox run task docker-build

# Test the image works correctly
devbox run task docker-test

# Push to registry for distribution
devbox run task docker-push
```

**Build Arguments:**

- `ARG_OPENAPI_GENERATOR_VERSION` - OpenAPI Generator CLI version (default: 7.10.0)

**Custom Build:**

```bash
podman build \
  --build-arg ARG_OPENAPI_GENERATOR_VERSION=7.10.0 \
  -t adamsdavis/minimal-api-generator:latest \
  -f docker/Dockerfile \
  .
```

### Image Architecture

The Docker image:
1. Uses OpenJDK 11 JRE (slim)
2. Downloads OpenAPI Generator CLI
3. Includes the custom generator JAR
4. Combines both on classpath via ENTRYPOINT

**Source Attribution:**
Based on [Stack Overflow answer](https://stackoverflow.com/q/78887848) by Arturo Martínez Díaz (CC BY-SA 4.0).

### Use Cases

- **CI/CD Pipelines**: Generate code as part of automated builds
- **Team Distribution**: Share generator without Java/Maven dependencies
- **External Systems**: Integrate code generation into other tools
- **Consistent Environments**: Ensure same generator version across systems

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

**Local Development:**
```bash
devbox run task build-generator
devbox run task regenerate
```

**Docker:**
```bash
devbox run task docker-build
devbox run task docker-test
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
2. Rebuild generator: `devbox run task build-generator`
3. Test generation: `devbox run task regenerate`
4. Verify all tests pass
5. Commit changes

### Debugging

View full generation output:
```bash
devbox run task generate-petstore-minimal-api
```

Run the API server for manual testing:
```bash
devbox run task run-petstore-api
```

Build and test manually:
```bash
cd test-output
devbox run dotnet build src/PetstoreApi/PetstoreApi.csproj
devbox run dotnet test tests/PetstoreApi.Tests/PetstoreApi.Tests.csproj
```

## OpenAPI Specification

The petstore example spec is located at:
```
petstore-tests/petstore.yaml
```

To use your own OpenAPI spec, update the `PETSTORE_SPEC` variable in `Taskfile.yml` to point to your specification file.

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make changes and ensure tests pass: `devbox run task regenerate`
4. Test Docker build if applicable: `devbox run task docker-build && devbox run task docker-test`
5. Commit with clear messages
6. Push and create a pull request

## License

[Your License Here]

## Acknowledgments

Built on [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) framework.

## Documentation

- **[Configuration Reference](docs/CONFIGURATION.md)** - Complete guide to all generator options and comparison with OpenAPI Generator
- [OpenAPI Generator Docs](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/customization.md) - Upstream documentation
