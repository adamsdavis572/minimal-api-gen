# minimal-api-gen Development Guidelines

## Stack
- **Generator**: Java 11, Maven 3.8.9+, OpenAPI Generator (AbstractCSharpCodegen), Mustache templates
- **Generated code target**: C# 11+ / .NET 8.0 (ASP.NET Core Minimal API, MediatR, FluentValidation)
- **Test tooling**: xUnit, Bruno (bru CLI), devbox (isolated tool environment)

## Project Structure

```text
generator/          # Java OpenAPI Generator plugin (builds JAR)
  src/main/java/    # Codegen logic (MinimalApiServerCodegen.java)
  src/main/resources/aspnet-minimalapi/  # Mustache templates
petstore-tests/     # Test artifacts (handlers, stubs, specs) - copied into test-output
  TestHandlers/     # IPetStore handlers (InMemoryPetStore, AddPetCommandHandler, etc.)
  Configurators/    # ApplicationServiceConfigurator, SecurityConfigurator
  PetstoreApi/Extensions/  # ServiceCollectionExtensions.cs (registers IPetStore)
  Auth/             # PermissionEndpointFilter.cs
  PetstoreApi.Tests/  # xUnit test project
  petstore.yaml     # OpenAPI spec used for generation
test-output/        # Generated code (ephemeral - wiped by clean:generated)
```

---

## ⛔ CRITICAL: ALWAYS USE `devbox run task`

**ALL tools** (`mvn`, `dotnet`, `java`, `bru`, `task`) are **only available inside devbox**.  
**NEVER** run them directly. **ALWAYS** prefix with `devbox run task`.

```bash
# ✅ CORRECT
cd /Users/adam/scratch/git/minimal-api-gen
devbox run task generator:build
devbox run task gen:petstore
devbox run task test:petstore-unit

# ❌ WRONG - these will all fail with "command not found" (exit 127)
task generator:build
mvn clean package
dotnet test test-output/
java -cp ...
bru run ...
```

**Exit code 127** = command not found → you ran something outside devbox.

---

## Task Reference

```bash
devbox run task --list    # show all tasks
```

### generator:* — Build the OpenAPI Generator plugin

```bash
devbox run task generator:build            # Build JAR (incremental - skips if sources unchanged)
devbox run task generator:build-test       # Build JAR + run generator unit tests
```

> Run `generator:build` any time you change Java source or Mustache templates.

### gen:* — Generate code from OpenAPI spec

```bash
# Default: MediatR + validators + problem details + NuGet packaging
devbox run task gen:petstore

# Custom properties
devbox run task gen:petstore ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true,useValidators=true,useProblemDetails=true,useNugetPackaging=true"
devbox run task gen:petstore ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true,useValidators=true,useProblemDetails=true,useAuthentication=true,useNugetPackaging=true"
devbox run task gen:petstore ADDITIONAL_PROPS="packageName=PetstoreApi,useMediatr=true,useValidators=true,useProblemDetails=true,useNugetPackaging=false"

# Copy test handlers/stubs/configurators into test-output (called automatically by test tasks)
devbox run task gen:copy-test-stubs
```

`gen:copy-test-stubs` copies from `petstore-tests/` into `test-output/`:
- `TestHandlers/` → `test-output/src/PetstoreApi/Handlers/` + `Services/`
- `Auth/PermissionEndpointFilter.cs` → `test-output/src/PetstoreApi/Filters/`
- `Configurators/ApplicationServiceConfigurator.cs` (+ `SecurityConfigurator.cs` if JWT detected) → `test-output/src/PetstoreApi/Configurators/`
- `PetstoreApi/Extensions/ServiceCollectionExtensions.cs` → `test-output/src/PetstoreApi/Extensions/`

> ⚠️ **`gen:copy-test-stubs` runs after generation and overwrites files in `test-output/`.** All `test:*` and `api:*` tasks call it automatically. When debugging generated output, inspect files **before** running any test/api task, or compare against the Mustache templates directly — files in `test-output/` may not reflect pure generator output.

### build:* — Compile generated code (NuGet packaging workflow only)

```bash
devbox run task build:all                  # Build entire solution
devbox run task build:contracts-nuget      # Build Contracts project only
devbox run task build:impl-nuget           # Build Implementation (depends on contracts-nuget)
```

> build tasks are **not required** for `test:petstore-unit` or `test:petstore-integration` — dotnet test handles compilation.

### test:* — Testing

```bash
# Generator template unit tests
devbox run task test:generator

# xUnit tests for generated petstore (auto-runs gen:copy-test-stubs first)
devbox run task test:petstore-unit

# Full integration lifecycle: start API → wait healthy → Bruno tests → stop
devbox run task test:petstore-integration
devbox run task test:petstore-integration SUITE="all-suites"            # main + validation (19 tests)
devbox run task test:petstore-integration SUITE="all-suites-with-auth"  # main + validation + auth (23 tests)

# Run a single Bruno test file with full lifecycle
devbox run task test:petstore-integration-single TEST='pet/add-pet.bru'
```

### regress:* — Full regression (clean → generate → unit test → integration test)

```bash
# No NuGet packaging (single-project output)
devbox run task regress:full-petstore-validators-problemdetails

# NuGet packaging (dual-project: Contracts + Implementation)
devbox run task regress:full-petstore-validators-problemdetails-nuget

# NuGet packaging + JWT authentication
devbox run task regress:full-petstore-validators-problemdetails-nuget-auth
```

### api:* — Manage the running API server

```bash
devbox run task api:run      # Foreground (blocks terminal - use for manual testing)
devbox run task api:start    # Background (saves PID to .server.pid)
devbox run task api:wait     # Poll http://localhost:5198/health until ready
devbox run task api:stop     # Kill background API via saved PID
```

### bruno:* — Bruno API test suites (requires API running)

```bash
devbox run task bruno:run-main-suite           # 6 tests  - CRUD operations
devbox run task bruno:run-validation-suite     # 13 tests - FluentValidation
devbox run task bruno:run-auth-suite           # 4 tests  - JWT auth (009 feature)
devbox run task bruno:run-all-suites           # 19 tests - main + validation
devbox run task bruno:run-all-suites-with-auth # 23 tests - main + validation + auth
devbox run task bruno:run-single TEST='pet/add-pet.bru'  # single file
```

### clean:* — Cleanup

```bash
devbox run task clean:generated   # Delete test-output/ only (keeps generator JAR)
devbox run task clean:all         # Delete test-output/ + generator build artifacts
```

### docker:* — Container

```bash
devbox run task docker:build      # Build image (uses podman)
devbox run task docker:push       # Push to registry
devbox run task docker:test       # Smoke-test image by generating code
```

---

## Common Workflows

**After changing a Mustache template or Java codegen:**
```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run task clean:all generator:build
```

**Quick unit test cycle (default props):**
```bash
devbox run task clean:generated gen:petstore test:petstore-unit
```

**Full integration test cycle:**
```bash
devbox run task clean:generated gen:petstore test:petstore-integration SUITE="all-suites"
```

**Full regression with auth:**
```bash
devbox run task regress:full-petstore-validators-problemdetails-nuget-auth
```

**Output locations:**
- Generated code: `test-output/`
- Generator JAR: `generator/target/aspnet-minimalapi-openapi-generator.jar`
- Test results: console output + `test-output/tests/PetstoreApi.Tests/bin/`

---

## Code Style

- **Java** (generator): standard Java conventions, match surrounding OpenAPI Generator codebase style
- **C#** (templates/generated): C# 11+ idioms, .NET 8 patterns, record types for DTOs

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
