# minimal-api-gen Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-11-10

## Active Technologies
- Java 8+ (OpenAPI Generator compatibility), C# 11+ (generated code target) + OpenAPI Generator framework (AbstractCSharpCodegen base class), Maven 3.8.9+, Mustache template engine (002-generator-scaffolding)
- File system (template resources in JAR, generated code output to disk) (002-generator-scaffolding)
- C# 11+ (.NET 8.0 SDK via devbox), Java 11 (for generator build via devbox) (003-baseline-test-suite)
- N/A (in-memory test data) (003-baseline-test-suite)
- In-memory Dictionary for baseline tests (from Feature 003), N/A for generator itself (004-minimal-api-refactoring)
- Java 11 (generator), C# 11+ / .NET 8.0 (generated code) (006-mediatr-decoupling)
- In-memory (for baseline tests only - production uses handler implementations) (006-mediatr-decoupling)

- Java (OpenAPI Generator codebase) - version detection needed from repository + OpenAPI Generator framework, Java source code analysis tools (grep, IDE) (001-fastendpoints-analysis)

## Project Structure

```text
src/
tests/
```

## Commands

⚠️ **CRITICAL**: All build tools MUST be run via devbox wrapper:
- Maven: `devbox run mvn` (never direct `mvn`)
- dotnet: `devbox run dotnet` (never direct `dotnet`)
- Example: `cd /path/to/project && devbox run dotnet build`
- devbox is installed in ~/scratch/git/minimal-api-gen/generator

### Running the Custom OpenAPI Generator

**Build**: `cd generator && devbox run mvn clean package`

**Generate Code**: `cd generator && ./run-generator.sh [--additional-properties key=value]`
- Uses OpenAPI Generator CLI + custom generator on classpath
- Reference: https://github.com/OpenAPITools/openapi-generator/blob/master/docs/customization.md#use-your-new-generator-with-the-cli
- Examples:
  - Default: `./run-generator.sh`
  - With MediatR: `./run-generator.sh --additional-properties useMediatr=true`
  - Multiple flags: `./run-generator.sh --additional-properties useMediatr=true,useAuthentication=false`

**Output**: Generated code appears in `test-output/` directory
## Code Style

Java (OpenAPI Generator codebase) - version detection needed from repository: Follow standard conventions

## Recent Changes
- 006-mediatr-decoupling: Added Java 11 (generator), C# 11+ / .NET 8.0 (generated code)
- 004-minimal-api-refactoring: Added In-memory Dictionary for baseline tests (from Feature 003), N/A for generator itself
- 003-baseline-test-suite: Added C# 11+ (.NET 8.0 SDK via devbox), Java 11 (for generator build via devbox)


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
