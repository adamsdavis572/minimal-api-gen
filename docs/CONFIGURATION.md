# Generator Configuration Reference

This page documents all configuration options available for the minimal-api-gen generator.

## Table of Contents

- [Overview](#overview)
- [Configuration Options](#configuration-options)
- [Usage Examples](#usage-examples)
- [Comparison with OpenAPI Generator](#comparison-with-openapi-generator)

---

## Overview

The minimal-api-gen generator provides configuration options through the `--additional-properties` flag. These options control the features and patterns used in the generated code.

**Setting Options:**
```bash
--additional-properties key1=value1,key2=value2
```

**Default Configuration:**
The generator is opinionated toward modern ASP.NET Core patterns with sensible defaults for minimal APIs.

---

## Configuration Options

### Core Feature Flags

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `useMediatr` | boolean | `false` | ✅ **IMPLEMENTED** - Enable MediatR CQRS pattern with separate DTOs, commands, queries, and handlers |
| `useProblemDetails` | boolean | `false` | ✅ **IMPLEMENTED** - Enable RFC 7807 compliant ProblemDetails error responses |
| `useRecords` | boolean | `false` | Use C# `record` types for request/response models |
| `useAuthentication` | boolean | `false` | Enable JWT authentication wiring |
| `useValidators` | boolean | `false` | ✅ **IMPLEMENTED** - Enable [FluentValidation](https://fluentvalidation.net/) for DTO validation with 7 constraint types |
| `useResponseCaching` | boolean | `false` | Enable ASP.NET Core response caching support |
| `useApiVersioning` | boolean | `false` | Enable API versioning |

### Routing & Organization

> **Note**: Route groups (MapGroup) are the required architecture for this generator and are not configurable. All endpoints are automatically organized using `MapGroup` by OpenAPI tag.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `routePrefix` | string | `"api"` | Route prefix for all endpoints (e.g., `/api/pets`) |
| `versioningPrefix` | string | `"v"` | Version prefix when `useApiVersioning=true` (e.g., `v` → `/v1/...`) |
| `apiVersion` | string | `"1"` | API version string when `useApiVersioning=true` |

### Error Handling

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `useGlobalExceptionHandler` | boolean | `true` | ✅ **IMPLEMENTED** - Add application-wide exception handler middleware with ValidationException, BadHttpRequestException, and JsonException handling |

### Project Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `packageName` | string | `"PetstoreApi"` | Root C# namespace for generated code |
| `solutionGuid` | string | `null` | GUID for .sln solution file (auto-generated if omitted) |
| `projectConfigurationGuid` | string | `null` | GUID for project configuration (auto-generated if omitted) |

---

## Usage Examples

### Basic Generation (Defaults)
```bash
devbox run task generate-petstore-minimal-api
```

### Enable MediatR CQRS Pattern
```bash
devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useMediatr=true"
```

**Generated structure:**
- Commands: `AddPetCommand`, `UpdatePetCommand`, `DeletePetCommand`
- Queries: `GetPetByIdQuery`, `ListPetsQuery`
- Handlers: `AddPetCommandHandler`, `GetPetByIdQueryHandler`, etc.

### Enable Multiple Features
```bash
devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useMediatr=true,useProblemDetails=true,useRecords=true"
```

### API Versioning
```bash
devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useApiVersioning=true,routePrefix=api,versioningPrefix=v,apiVersion=1"
```

**Result:** Endpoints at `/api/v1/pets`, `/api/v1/pets/{id}`, etc.

### Full-Featured Configuration
```bash
devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useMediatr=true,useProblemDetails=true,useRecords=true,useAuthentication=true,useValidators=true,useResponseCaching=true,useApiVersioning=true,packageName=MyApi"
```

### Using Taskfile

For local development, use Taskfile commands with the `ADDITIONAL_PROPS` parameter:

```bash
# Generate with specific properties:
devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useMediatr=true,useProblemDetails=true"

# Or use the complete workflow (build + generate + test):
devbox run task regenerate
```

**Note**: The `regenerate` task uses properties defined in `Taskfile.yml`. To customize, either:
- Pass `ADDITIONAL_PROPS` directly to `generate-petstore-minimal-api` task
- Edit the `ADDITIONAL_PROPS` variable in `Taskfile.yml` for persistent changes

---

## Comparison with OpenAPI Generator

### minimal-api-gen vs ASPNETServer Generator

The minimal-api-gen generator uses a **leaner, opinionated option set** tailored for Minimal APIs and CQRS/MediatR patterns, with configuration options mostly shared with the newer [aspnet-fastendpoints](https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AspnetFastendpointsServerCodegen.java) generator in OpenAPI Generator core.

The **ASPNETServer** generator (`aspnetcore`) in OpenAPI Generator provides a **much broader superset** of configuration options supporting both legacy and new ASP.NET Core features.

### Option Comparison Matrix

| Option | minimal-api-gen | OpenAPI Generator ASPNETServer |
|--------|:---------------:|:------------------------------:|
| `useMediatr` | ✅ | ❌ (not in main ASPNETServer) |
| `useProblemDetails` | ✅ | ✅ |
| `useRecords` | ✅ | ✅ (FastEndpoints only) |
| `useAuthentication` | ✅ | ✅ (more advanced controls) |
| `useValidators` | ✅ | ✅ (FastEndpoints) |
| `useResponseCaching` | ✅ | ✅ (FastEndpoints) |
| `useApiVersioning` | ✅ | ✅ |
| `useGlobalExceptionHandler` | ✅ | ❌ (custom solution per user) |
| `routePrefix` | ✅ | ✅ |
| `versioningPrefix` | ✅ | ✅ |
| `apiVersion` | ✅ | ✅ |
| `solutionGuid` | ✅ | ✅ |
| `projectConfigurationGuid` | ✅ | ✅ |
| `packageName` | ✅ | ✅ |
| **Additional C# Metadata** | Basic | Extensive |
| **Swashbuckle/Swagger** | ❌ | ✅ |
| **Framework/Serialization** | ❌ | ✅ (many options) |
| **Output Style** | Endpoints.cs | Controllers.cs |

### Options Only in ASPNETServer

The ASPNETServer generator includes additional options not present in minimal-api-gen:

- `aspnetCoreVersion`: Specify .NET platform version (2.2, 3.1, 5.0, 6.0, 8.0)
- `swashbuckleVersion`, `useSwashbuckle`: Control Swagger integration
- `pocoModels`: Use POCOs instead of DTO records
- `useFrameworkReference`, `useNewtonsoft`, `newtonsoftVersion`: Serialization specifics
- `buildTarget`: "program" for standalone, "library" for reusable library
- Package metadata: `packageDescription`, `packageCopyright`, `packageAuthors`
- Many class/operation modifiers and enable/optOut switches

### Key Takeaways

- **minimal-api-gen** is optimized for **ASP.NET Core Minimal APIs** with MediatR/CQRS patterns
- **ASPNETServer** is optimized for **Controller-based APIs** with extensive legacy support
- If migrating from ASPNETServer: Most core options map cleanly, but `useMediatr` is a minimal-api-gen exclusive

---

## References

### Source Code
- [MinimalApiServerCodegen.java](https://github.com/adamsdavis572/minimal-api-gen/blob/main/generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java) - Configuration fields and option definitions
- [AspNetServerCodegen.java](https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AspNetServerCodegen.java) - OpenAPI Generator ASPNETServer options
- [AspnetFastendpointsServerCodegen.java](https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AspnetFastendpointsServerCodegen.java) - FastEndpoints generator (most similar to minimal-api-gen)

### Documentation
- [OpenAPI Generator ASPNetCore Docs](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/aspnetcore.md)
- [OpenAPI Generator Customization Guide](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/customization.md)

---

## Recommended Configurations

### Configuration Matrix (Tested)

These 6 configurations have been verified to compile and function correctly:

| Configuration | useMediatr | useValidators | useGlobalExceptionHandler | useProblemDetails | Use Case | Status |
|---------------|:----------:|:-------------:|:-------------------------:|:-----------------:|----------|:------:|
| **Backward Compatible** | false | false | false | false | Legacy APIs, no MediatR | ✅ 0 errors, 47 warnings |
| **Basic CQRS** | true | false | false | false | MediatR without validation | ✅ 0 errors |
| **CQRS + Validation** | true | true | false | false | Validated APIs without global handler | ✅ 0 errors |
| **CQRS + Errors** | true | false | true | false | Exception handling without validation | ✅ 0 errors |
| **Full Stack (RFC 7807)** ⭐ | true | true | true | true | **RECOMMENDED** - Production APIs with standards-compliant errors | ✅ 30/30 tests |
| **Full Stack (Simple)** | true | true | true | false | Production APIs with simple JSON errors | ✅ 0 errors, 67 warnings |

⭐ **Recommended Default**: `useMediatr=true,useValidators=true,useGlobalExceptionHandler=true,useProblemDetails=true`

### Feature Combinations

**DTOs and Validation** (`useMediatr=true,useValidators=true`):
- Generates separate DTO classes in `DTOs/` directory
- Commands/Queries reference DTOs (not Models)
- FluentValidation validators in `Validators/` directory
- Supports 7 OpenAPI constraint types:
  1. **Required fields**: `NotEmpty()` rules
  2. **String length**: `Length(min, max)` rules
  3. **String patterns**: `Matches(regex)` rules
  4. **Numeric ranges**: `GreaterThanOrEqualTo()`, `LessThanOrEqualTo()` rules
  5. **Array sizes**: `Must(x => x.Count >= min && x.Count <= max)` rules
  6. **Enum validation**: Automatic with C# enum types
  7. **Nested DTOs**: `SetValidator(new NestedDtoValidator())` chaining
- Validation occurs at API boundary before MediatR handlers
- Handlers map validated DTOs to domain Models

**Error Handling** (`useGlobalExceptionHandler=true`):
- Catches ValidationException (FluentValidation) → 400 BadRequest
- Catches BadHttpRequestException → 400 BadRequest
- Catches JsonException → 400 BadRequest
- Catches all other exceptions → 500 InternalServerError
- When `useProblemDetails=true`: Returns RFC 7807 ProblemDetails format
- When `useProblemDetails=false`: Returns simple `{error, message, errors}` JSON

**Examples:**

```bash
# Recommended production config (Feature 007 default)
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="packageName=MyApi,useMediatr=true,useValidators=true,useGlobalExceptionHandler=true,useProblemDetails=true"

# Simple validation without RFC 7807
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="packageName=MyApi,useMediatr=true,useValidators=true,useProblemDetails=false"

# Basic CQRS without validation
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="packageName=MyApi,useMediatr=true,useValidators=false"

# Traditional Minimal API (no MediatR)
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="packageName=MyApi,useMediatr=false"
```

---

## Contributing

To add new configuration options:

1. Add the option in `MinimalApiServerCodegen.java` using `addSwitch()` or `addOption()`
2. Add corresponding logic in `processOpts()` to set template variables
3. Update templates to use the new variable
4. Add tests in the baseline test suite
5. Document the option in this file

Example:
```java
// In MinimalApiServerCodegen.java
public MinimalApiServerCodegen() {
    // ...
    addSwitch(USE_MY_FEATURE, "Enable my feature", this.useMyFeature);
}

@Override
public void processOpts() {
    // ...
    if (additionalProperties.containsKey(USE_MY_FEATURE)) {
        this.setUseMyFeature(convertPropertyToBooleanAndWriteBack(USE_MY_FEATURE));
    }
    additionalProperties.put(USE_MY_FEATURE, this.useMyFeature);
}
```
