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
- Java 11 (generator build), C# 11+ / .NET 8.0 (generated code) (007-config-fixes)
- File system (templates in JAR, generated code output to disk) (007-config-fixes)

- Java (OpenAPI Generator codebase) - version detection needed from repository + OpenAPI Generator framework, Java source code analysis tools (grep, IDE) (001-fastendpoints-analysis)

## Project Structure

```text
src/
tests/
```

## Commands

⚠️ **CRITICAL - ALWAYS USE TASK RUNNER VIA DEVBOX**:

### Command Execution Rules (MUST FOLLOW)
1. **NEVER run commands directly**: `mvn`, `dotnet`, `task`, generator scripts are NOT available globally
2. **ALWAYS use devbox task runner**: `devbox run task <task-name>` (task runner is ONLY available inside devbox)
3. **Task runner wraps all tools**: The Taskfile.yml already handles devbox for mvn/dotnet/java - don't add extra nesting
4. **Use absolute paths**: Always `cd` to project root first: `cd /Users/adam/scratch/git/minimal-api-gen`

### Available Task Commands
```bash
# See all available tasks
devbox run task --list

# Generator Build - must be done before generating code
devbox run task build-generator                    # Build the generator JAR

# Generates 
devbox run task generate-petstore-minimal-api      # Generate code with default config

    # Generate with custom configuration
    devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true"
    devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true,useValidators=true"


devbox run task copy-test-stubs # Copy test handler stubs into generated code for testing - this is needed for quicktest 

devbox run task quick-test                     # Quick test run (assumes code already generated) - xUnit tests run from source location

devbox run task test-ci                          # Full test cycle: Start API -> Wait -> Run Bruno tests -> Stop API

# Building Nuget Package Solution Generated Code ONLY USE FOR BUILDING NUGET Package - the build step is not required for non-NuGet scenarios
devbox run task build-all                          # Build entire solution (both projects)
devbox run task build-contracts                    # Build the Contracts project only
devbox run task build-implementation-using-contracts  # Build Implementation using NuGet package

# Bruno Tests
devbox run task bruno:run-all-suites              # Run both main + validation suites (19 tests)
devbox run task bruno:run-main-suite              # Run main pet test suite (6 tests)
devbox run task bruno:run-validation-suite        # Run validation test suite (13 tests)
devbox run task single-bruno-test TEST='pet/add-pet.bru'  # Run specific Bruno test(s) usually not needed but  required for certain investigations

# Cleanup
devbox run task clean-all                          # Clean all generated code and build artifacts
devbox run task clean-generated-api                # Clean only generated Minimal API code, this should be us

# API Server
devbox run task run-petstore-api                   # Launch the generated   
                      

```

### Why This Matters
- **Exit code 127** = "command not found" → You tried to run a command that's only in devbox
- Tools are isolated in devbox environment for reproducible builds
- Task runner (go-task) coordinates multi-step workflows from Taskfile.yml
- Direct command execution will ALWAYS fail outside devbox

### Running the Custom OpenAPI Generator

**CORRECT**: Use task commands (recommended)
```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run task build-generator
devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true"
```

**INCORRECT** ❌: Never do these
```bash
task build-generator                          # ❌ task not in PATH
mvn clean package                            # ❌ mvn not in PATH  
cd generator && ./run-generator.sh           # ❌ Deprecated script
dotnet build test-output/                   # ❌ dotnet not in PATH
dotnet test test-output/                     # ❌ Use task test-server-stubs instead
dotnet pack test-output/                     # ❌ Use task build-contracts instead
```

**Output**: Generated code appears in `test-output/` directory

**Building Generated Code**: Always use task commands
```bash
# Build both projects (Contracts + Implementation)
devbox run task build-all

# Build only the Contracts project (for NuGet packaging)
devbox run task build-contracts

# Build Implementation using packaged Contracts
devbox run task build-implementation-using-contracts
```
## Code Style

Java (OpenAPI Generator codebase) - version detection needed from repository: Follow standard conventions

## Recent Changes
- 007-config-fixes: Added Java 11 (generator build), C# 11+ / .NET 8.0 (generated code)
- 006-mediatr-decoupling: Added Java 11 (generator), C# 11+ / .NET 8.0 (generated code)


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
