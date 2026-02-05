# Taskfile Reorganization Summary

## Changes Applied

Reorganized Taskfile.yml with consistent namespacing for improved maintainability.

## New Namespace Structure

### 1. **generator:** - Build Custom OpenAPI Generator
- `generator:build` - Build the custom OpenAPI generator JAR
- `generator:download-cli` - Downloads main generator CLI from Maven

### 2. **gen:** - Code Generation from OpenAPI Spec
- `gen:petstore` - Generate server code from OpenAPI spec (with MediatR and optional properties)
- `gen:copy-test-stubs` - Copy test handlers and test project over generated stubs

### 3. **build:** - Compile Generated Code
- `build:all` - Build entire solution (both projects when NuGet packaging enabled)
- `build:contracts-nuget` - Build the Contracts project only (for NuGet packaging workflow)
- `build:impl-nuget` - Build Implementation project that uses Contracts via NuGet package reference

### 4. **test:** - Unit & Integration Testing
- `test:unit` - Run xUnit tests (assumes code already generated)
- `test:integration` - Full API lifecycle test (start → wait → Bruno tests → stop)
- `test:integration-single` - Run specific Bruno test(s) with full lifecycle

### 5. **api:** - Manage Test API Server
- `api:run` - Launch the generated Petstore API server (foreground)
- `api:start` - Start the .NET API in background and save PID
- `api:wait` - Wait until the API is healthy
- `api:stop` - Stop the background API using saved PID

### 6. **bruno:** - API Test Suites
- `bruno:run` - Run all Bruno API tests
- `bruno:run-main-suite` - Run main pet test suite (6 tests - CRUD operations)
- `bruno:run-validation-suite` - Run validation test suite (13 tests - FluentValidation)
- `bruno:run-all-suites` - Run both main + validation suites (19 tests total)
- `bruno:run-single` - Run one or more Bruno test files

### 7. **docker:** - Container Management
- `docker:build` - Build Docker image with custom OpenAPI generator
- `docker:push` - Push Docker image to registry
- `docker:test` - Test Docker image by generating code

### 8. **clean:** - Cleanup Tasks
- `clean:generated` - Clean only generated Minimal API code (keeps generator artifacts)
- `clean:all` - Clean all generated code and build artifacts

## Legacy Aliases (Backward Compatibility)

All old task names are maintained as aliases with `[DEPRECATED]` markers:

- `build-generator` → `generator:build`
- `generate-petstore-minimal-api` → `gen:petstore`
- `copy-test-stubs` → `gen:copy-test-stubs`
- `quick-test` → `test:unit`
- `test-ci` → `test:integration`
- `run-petstore-api` → `api:run`
- `clean-generated-api` → `clean:generated`

## Benefits

1. **Consistent Namespacing** - All tasks now use `:` separator
2. **Logical Grouping** - Tasks are organized by functional area
3. **Clear Semantics** - NuGet-specific tasks clearly identified (`build:contracts-nuget`, `build:impl-nuget`)
4. **Backward Compatible** - Existing scripts/docs continue to work via aliases
5. **Improved Discoverability** - `task --list` now shows organized structure
6. **Removed Dead Code** - Cleaned up commented-out tasks

## Verification

✅ All 37 tasks verified with `task --list`  
✅ `generator:build` successfully tested  
✅ `test:unit` successfully executed (45/45 tests passing)  
✅ Backward compatibility maintained

## Backup

Original Taskfile saved as `Taskfile.old.yml`
