# ASP.NET Core Minimal API Generator

[![CI Status](https://github.com/adamsdavis572/minimal-api-gen/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/adamsdavis572/minimal-api-gen/actions/workflows/ci.yml)

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

All tools are managed via [Devbox](https://www.jetpack.io/devbox). Prefix every task with `devbox run task`:

```bash
# 1. Build the custom generator JAR
devbox run task generator:build

# 2. Generate API code from the petstore spec
devbox run task gen:petstore

# 3. Run unit tests for the generated API
devbox run task test:petstore-unit

# 4. Run full integration tests (start API → Bruno tests → stop)
devbox run task test:petstore-integration SUITE="all-suites"

# 5. Run the generated API server manually
devbox run task api:run
```

Generated code appears in the `test-output/` directory.

### Docker (For CI/CD and Distribution)

The generator is also distributed as a Docker image — no local Java or Maven required. See [docker/README.md](docker/README.md) for build, usage, and CI/CD integration instructions.

## Generator Usage

### Local Development with Taskfile (Recommended)

For local development and testing, use Taskfile commands directly:

```bash
devbox run task gen:petstore
```

**Key Tasks:**

```bash
# Build the generator JAR
devbox run task generator:build

# Generate API code from spec
devbox run task gen:petstore

# Copy test handlers and stubs into test-output/
devbox run task gen:copy-test-stubs

# Run xUnit unit tests
devbox run task test:petstore-unit

# Full integration test lifecycle
devbox run task test:petstore-integration SUITE="all-suites"

# Run the API server (foreground)
devbox run task api:run

# List all available tasks
devbox run task --list
```

**Generator Configuration:**

The generator supports 20+ configuration options for features, structure, and NuGet packaging. See the [Configuration Reference](docs/CONFIGURATION.md) for complete details.

**Quick Examples:**

```bash
# Enable MediatR CQRS pattern
devbox run task gen:petstore ADDITIONAL_PROPS="useMediatr=true"

# Enable NuGet packaging with custom metadata
devbox run task gen:petstore ADDITIONAL_PROPS="useNugetPackaging=true,packageVersion=2.0.0,packageDescription=My API Contracts"

# Multiple features
devbox run task gen:petstore ADDITIONAL_PROPS="useMediatr=true,useValidators=true,useRecords=true"

# Custom namespace
devbox run task gen:petstore ADDITIONAL_PROPS="packageName=MyCompany.ShopApi"
```

**Popular Options:**
- `useMediatr` - Enable MediatR/CQRS pattern (default: false)
- `useNugetPackaging` - Generate separate Contracts NuGet package (default: false)
- `useValidators` - Add FluentValidation validators (default: false)
- `useProblemDetails=true|false` - Use RFC 7807 format for errors (default: false)
- `useGlobalExceptionHandler=true|false` - Enable exception handling middleware (default: true)
- `useRecords` - Use C# records for DTOs (default: false)
- `packageName` - Root namespace (default: Org.OpenAPITools)
- `packageVersion` - NuGet package version (default: OAS version or 1.0.0)
- `packageDescription` - NuGet description (default: from OAS)
- `packageLicenseExpression` - SPDX license (default: Apache-2.0)

See [CONFIGURATION.md](docs/CONFIGURATION.md) for all 20+ options.

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

## Task Reference

All tasks must be invoked as `devbox run task <name>`. Run `devbox run task --list` to see all available tasks.

### `generator:*` — Build the OpenAPI Generator plugin

| Task | Description |
|---|---|
| `generator:build` | Build the custom OpenAPI generator JAR (incremental) |
| `generator:build-test` | Build JAR and run generator unit tests |

### `gen:*` — Generate code from OpenAPI spec

| Task | Description |
|---|---|
| `gen:petstore` | Generate server code from the petstore spec (default: MediatR + validators + problem details + NuGet) |
| `gen:copy-test-stubs` | Copy hand-written handlers, tests, and configurators into `test-output/` |
| `gen:copy-test-stubs-with-auth` | Same as above, plus JWT Bearer auth (`SecurityConfigurator` + `JwtBearer` NuGet package) |

Pass custom properties with `ADDITIONAL_PROPS`:
```bash
devbox run task gen:petstore ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true,useValidators=true,useProblemDetails=true,useNugetPackaging=false"
```

### `build:*` — Compile generated code (NuGet packaging workflow only)

| Task | Description |
|---|---|
| `build:all` | Build the entire solution |
| `build:contracts-nuget` | Build the Contracts project only |
| `build:impl-nuget` | Build the Implementation project (depends on `contracts-nuget`) |

### `test:*` — Testing

| Task | Description |
|---|---|
| `test:generator` | Run generator unit tests — validates Mustache templates directly, no code generation needed |
| `test:petstore-unit` | Run xUnit tests for the generated petstore API |
| `test:petstore-integration` | Full lifecycle: start API → wait healthy → Bruno tests → stop |
| `test:petstore-integration-single` | Same lifecycle for a single Bruno test file |

```bash
devbox run task test:petstore-integration SUITE="all-suites"             # 19 tests
devbox run task test:petstore-integration SUITE="all-suites-with-auth"   # 23 tests
devbox run task test:petstore-integration-single TEST='pet/add-pet.bru'
```

### `regress:*` — Full regression suites

Run the complete cycle (clean → generate → unit test → integration test) with a specific configuration. Use these before releases or after significant changes to generator logic or templates.

| Task | Description |
|---|---|
| `regress:full-petstore-validators-problemdetails` | Single-project output (`useNugetPackaging=false`) |
| `regress:full-petstore-validators-problemdetails-nuget` | Dual-project NuGet packaging |
| `regress:full-petstore-validators-problemdetails-nuget-auth` | Dual-project + JWT auth |

### `api:*` — Manage the test API server

| Task | Description |
|---|---|
| `api:run` | Start the API in the foreground (blocks terminal) |
| `api:start` | Start the API in the background (saves PID) |
| `api:wait` | Poll `http://localhost:5198/health` until ready |
| `api:stop` | Stop the background API |

### `bruno:*` — Bruno integration test suites (API must be running)

| Task | Description |
|---|---|
| `bruno:run-main-suite` | 6 tests — CRUD operations |
| `bruno:run-validation-suite` | 13 tests — FluentValidation |
| `bruno:run-auth-suite` | 4 tests — JWT auth |
| `bruno:run-all-suites` | 19 tests — main + validation |
| `bruno:run-all-suites-with-auth` | 23 tests — main + validation + auth |
| `bruno:run-single TEST='...'` | Run a single `.bru` file |
| `bruno:debug-single TEST='...'` | Run single test, dump full request/response JSON |

### `clean:*`

| Task | Description |
|---|---|
| `clean:generated` | Delete `test-output/` only (keeps generator JAR) |
| `clean:all` | Delete `test-output/` and all generator build artifacts |

## Project Structure

```
minimal-api-gen/
├── Taskfile.yml                        # Build automation
├── generator/                          # OpenAPI Generator implementation
│   ├── src/
│   │   └── main/
│   │       ├── java/                   # Generator Java code
│   │       │   └── org/openapitools/codegen/languages/
│   │       │       └── MinimalApiServerCodegen.java
│   │       └── resources/
│   │           └── aspnet-minimalapi/  # Mustache templates
│   │               ├── api.mustache
│   │               ├── model.mustache
│   │               ├── command.mustache
│   │               ├── query.mustache
│   │               ├── handler.mustache
│   │               ├── validator.mustache
│   │               ├── program.mustache
│   │               └── nuget-*.mustache
│   └── pom.xml                         # Maven configuration
├── petstore-tests/                     # Master test copies
│   ├── TestHandlers/                   # Handler implementations
│   ├── PetstoreApi.Tests/              # Test project
│   └── petstore.yaml                   # OpenAPI specification
├── bruno/                              # API integration tests (Bruno)
│   ├── pet/                            # Pet endpoint tests
│   ├── validation/                     # Validation tests
│   └── bruno.json                      # Collection config
├── specs/                              # Feature specifications
│   └── 008-nuget-api-contracts/        # Current feature work
├── docker/                             # Docker build files
│   ├── Dockerfile
│   └── README.md
├── docs/                               # Documentation
│   ├── CONFIGURATION.md                # Generator options reference
│   └── *.md                            # Additional guides
└── test-output/                        # Generated code (ephemeral)
    └── (see "Generated Code Structure" below)
```

## Generated Code Structure

The output structure varies depending on which generator properties you enable (`useNugetPackaging`, `useMediatr`, `useValidators`, etc.). See [docs/generated-structure.md](docs/generated-structure.md) for full directory trees and a description of key differences.

## Testing

There are three distinct layers of testing.

### Generator unit tests (`test:generator`)

Validate Mustache template logic and codegen behaviour directly — **no code generation or running API required**. These live in `generator-tests/` and are fast to run after any change to Java codegen or templates:

```bash
devbox run task generator:build
devbox run task test:generator
```

### Petstore xUnit tests (`test:petstore-unit`)

xUnit tests for the _generated_ C# code, using `WebApplicationFactory` and an in-memory pet store. These live in `petstore-tests/PetstoreApi.Tests/` and are copied into `test-output/` at test time by `gen:copy-test-stubs`:

```bash
devbox run task gen:petstore
devbox run task test:petstore-unit
```

The tests verify correct HTTP semantics (201 Created, 404 NotFound, 204 NoContent, validation 400s, etc.) and support two auth modes — open (all checks bypassed) and secure (JWT claims via test headers). See [docs/petstore-tests.md](docs/petstore-tests.md) for a full description of the test infrastructure, the copy-stubs mechanism, and auth wiring.

### Bruno integration tests (`test:petstore-integration`)

End-to-end API tests using the [Bruno](https://www.usebruno.com/) CLI. Run against a live API server:

```bash
devbox run task test:petstore-integration SUITE="all-suites"            # 19 tests
devbox run task test:petstore-integration SUITE="all-suites-with-auth"  # 23 tests
```

## Docker

The generator is published as a Docker image (`adamsdavis/minimal-api-generator:latest`) for use in CI/CD pipelines and other environments where local Java/Maven setup is not desirable. See [docker/README.md](docker/README.md) for build instructions, `docker run` usage examples, CI/CD integration, and troubleshooting.

## Releases

### Creating a Release

To publish a new version of the generator:

1. Create and push a tag:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. Create a GitHub Release:
   - Go to the repository's [Releases page](https://github.com/adamsdavis572/minimal-api-gen/releases)
   - Click "Draft a new release"
   - Select the tag you just created (e.g., `v1.0.0`)
   - Fill in the release title and description
   - Click "Publish release"

3. The release workflow automatically:
   - Checks out the release tag
   - Builds the generator JAR using the repository tooling (Devbox/Nix)
   - Uploads `aspnet-minimalapi-openapi-generator.jar` as a release asset

### Downloading a Release

The generator JAR is available as an asset on each GitHub Release:

```bash
# Download the latest release JAR
curl -L -o aspnet-minimalapi-openapi-generator.jar \
  https://github.com/adamsdavis572/minimal-api-gen/releases/latest/download/aspnet-minimalapi-openapi-generator.jar

# Download a specific version
curl -L -o aspnet-minimalapi-openapi-generator.jar \
  https://github.com/adamsdavis572/minimal-api-gen/releases/download/v1.0.0/aspnet-minimalapi-openapi-generator.jar
```

### Using a Release JAR

```bash
# Generate code using a released JAR
java -cp aspnet-minimalapi-openapi-generator.jar:openapi-generator-cli.jar \
  org.openapitools.codegen.OpenAPIGenerator generate \
  -g aspnetcore-minimalapi \
  -i your-api-spec.yaml \
  -o ./generated \
  --additional-properties packageName=YourApi,useMediatr=true
```

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
devbox run task generator:build
devbox run task gen:petstore
devbox run task test:petstore-unit
```

**Docker:**
```bash
devbox run task docker:build
devbox run task docker:test
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
2. Rebuild generator: `devbox run task generator:build`
3. Generate and test: `devbox run task gen:petstore test:petstore-unit`
4. Run full integration tests: `devbox run task test:petstore-integration SUITE="all-suites"`
5. Commit changes

### Debugging

View full generation output:
```bash
devbox run task clean:generated gen:petstore
```

Run the API server for manual testing:
```bash
devbox run task api:run
```

Debug a single Bruno test with full request/response output:
```bash
devbox run task bruno:debug-single TEST='pet/add-pet.bru'
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
3. Make changes and ensure tests pass: `devbox run task gen:petstore test:petstore-unit`
4. Run full regression: `devbox run task regress:full-petstore-validators-problemdetails-nuget`
5. Test Docker build if applicable: `devbox run task docker:build && devbox run task docker:test`
5. Commit with clear messages
6. Push and create a pull request

## License

[Your License Here]

## Acknowledgments

Built on [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) framework.

## Documentation

- **[Configuration Reference](docs/CONFIGURATION.md)** - All 20+ generator options with examples
- **[Petstore Test Infrastructure](docs/petstore-tests.md)** - How the test stubs, copy mechanism, and auth wiring work
- **[Generated Code Structure](docs/generated-structure.md)** - Directory trees for each combination of generator flags
- **[Docker Usage](docker/README.md)** - Building, distributing, and using the generator image
- [OpenAPI Generator Docs](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/customization.md) - Upstream documentation
