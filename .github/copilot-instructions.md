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
- C# 11+ / .NET 8.0 (generated code target), Java 11 (generator build) (009-endpoint-auth-filter)

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

**Task Organization**: All tasks use consistent namespacing with `:` separator:
- `generator:*` - Build custom OpenAPI generator
- `gen:*` - Generate code from OpenAPI spec
- `build:*` - Compile generated code
- `test:*` - Unit & integration testing
- `api:*` - Manage test API server
- `bruno:*` - API test suites
- `docker:*` - Container management
- `clean:*` - Cleanup tasks

```bash
# See all available tasks (organized by namespace)
devbox run task --list

# ============================================================================
# COMMON WORKFLOWS (RECOMMENDED)
# ============================================================================

# DEFAULT: Test generated petstore with xUnit tests
devbox run task clean:generated gen:petstore test:unit

# DEFAULT: Test generated petstore with Bruno integration tests
devbox run task clean:generated gen:petstore test:integration

# REBUILD GENERATOR: When you modify generator templates or Java code
devbox run task clean:all generator:build

# CUSTOM GENERATION: Generate with specific additional properties
devbox run task clean:generated gen:petstore ADDITIONAL_PROPS="packageName=MyApi,useMediatr=true,useValidators=true"

# ============================================================================
# GENERATOR TASKS (generator:*)
# ============================================================================

devbox run task generator:build                # Build the custom OpenAPI generator JAR
devbox run task generator:download-cli         # Download OpenAPI Generator CLI from Maven

# ============================================================================
# CODE GENERATION TASKS (gen:*)
# ============================================================================

devbox run task gen:petstore                   # Generate server code (default: MediatR + validators enabled)
devbox run task gen:petstore ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true"  # Custom config
devbox run task gen:copy-test-stubs            # Copy test handler stubs into generated code

# ============================================================================
# BUILD TASKS (build:*)
# ============================================================================

# ONLY USE FOR NUGET PACKAGING - build step NOT required for testing
devbox run task build:all                      # Build entire solution (both projects)
devbox run task build:contracts-nuget          # Build Contracts project only (NuGet workflow)
devbox run task build:impl-nuget               # Build Implementation using NuGet Contracts

# ============================================================================
# TEST TASKS (test:*)
# ============================================================================

devbox run task test:unit                      # Run xUnit tests (45 tests)
devbox run task test:integration               # Full lifecycle: start API → Bruno tests → stop
devbox run task test:integration SUITE="main-suite"        # Run specific Bruno suite
devbox run task test:integration-single TEST='pet/add-pet.bru'  # Run single Bruno test

# ============================================================================
# API SERVER TASKS (api:*)
# ============================================================================

devbox run task api:run                        # Launch API in foreground (for manual testing)
devbox run task api:start                      # Start API in background (saves PID)
devbox run task api:wait                       # Wait until API is healthy
devbox run task api:stop                       # Stop background API

# ============================================================================
# BRUNO TEST TASKS (bruno:*)
# ============================================================================

devbox run task bruno:run                      # Run all Bruno tests
devbox run task bruno:run-main-suite           # Run main pet tests (6 tests - CRUD)
devbox run task bruno:run-validation-suite     # Run validation tests (13 tests)
devbox run task bruno:run-all-suites           # Run both suites (19 tests)
devbox run task bruno:run-single TEST='pet/add-pet.bru'  # Run specific test file

# ============================================================================
# DOCKER TASKS (docker:*)
# ============================================================================

devbox run task docker:build                   # Build Docker image with custom generator
devbox run task docker:push                    # Push image to registry
devbox run task docker:test                    # Test Docker image by generating code

# ============================================================================
# CLEANUP TASKS (clean:*)
# ============================================================================

devbox run task clean:generated                # Clean only generated code (keeps generator JAR)
devbox run task clean:all                      # Clean all code + generator artifacts

# ============================================================================
# LEGACY ALIASES (DEPRECATED - use namespaced versions above)
# ============================================================================

devbox run task build-generator                # [DEPRECATED] Use generator:build
devbox run task generate-petstore-minimal-api  # [DEPRECATED] Use gen:petstore
devbox run task copy-test-stubs                # [DEPRECATED] Use gen:copy-test-stubs
devbox run task quick-test                     # [DEPRECATED] Use test:unit
devbox run task test-ci                        # [DEPRECATED] Use test:integration
devbox run task run-petstore-api               # [DEPRECATED] Use api:run
devbox run task clean-generated-api            # [DEPRECATED] Use clean:generated
```

### Why This Matters
- **Exit code 127** = "command not found" → You tried to run a command that's only in devbox
- Tools are isolated in devbox environment for reproducible builds
- Task runner (go-task) coordinates multi-step workflows from Taskfile.yml
- Direct command execution will ALWAYS fail outside devbox

### Typical Development Workflows

**Testing Generated Petstore API** (default workflow):
```bash
cd /Users/adam/scratch/git/minimal-api-gen

# Unit tests (xUnit - 45 tests)
devbox run task clean:generated gen:petstore test:unit

# Integration tests (Bruno - 27 tests, 77 assertions)
devbox run task clean:generated gen:petstore test:integration
```

**Rebuilding the Generator** (after template/Java changes):
```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run task clean:all generator:build
```

**Custom Code Generation**:
```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run task gen:petstore ADDITIONAL_PROPS="packageName=MyApi,useMediatr=true,useValidators=true"
```

**INCORRECT** ❌: Never run commands directly
```bash
task generator:build                         # ❌ task not in PATH
mvn clean package                            # ❌ mvn not in PATH  
dotnet test test-output/                     # ❌ dotnet not in PATH
./run-generator.sh                           # ❌ Deprecated script
```

**Output Locations**:
- Generated code: `test-output/`
- Generator JAR: `generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar`
- Test results: Console output + test-output/tests/PetstoreApi.Tests/bin/
## Code Style

Java (OpenAPI Generator codebase) - version detection needed from repository: Follow standard conventions

## Recent Changes
- 009-endpoint-auth-filter: Added C# 11+ / .NET 8.0 (generated code target), Java 11 (generator build)
- 007-config-fixes: Added Java 11 (generator build), C# 11+ / .NET 8.0 (generated code)
- 006-mediatr-decoupling: Added Java 11 (generator), C# 11+ / .NET 8.0 (generated code)


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
