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
| `useMediatr` | boolean | `false` | Enable MediatR CQRS pattern with commands, queries, and handlers |
| `useProblemDetails` | boolean | `false` | Enable RFC 7807 compliant error responses |
| `useRecords` | boolean | `false` | Use C# `record` types for request/response models |
| `useAuthentication` | boolean | `false` | Enable JWT authentication wiring |
| `useValidators` | boolean | `false` | Enable [FluentValidation](https://fluentvalidation.net/) for request validation |
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
| `useGlobalExceptionHandler` | boolean | `true` | Add application-wide exception handler middleware |

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
./run-generator.sh --additional-properties useMediatr=true
```

**Generated structure:**
- Commands: `AddPetCommand`, `UpdatePetCommand`, `DeletePetCommand`
- Queries: `GetPetByIdQuery`, `ListPetsQuery`
- Handlers: `AddPetCommandHandler`, `GetPetByIdQueryHandler`, etc.

### Enable Multiple Features
```bash
./run-generator.sh \
  --additional-properties useMediatr=true,useProblemDetails=true,useRecords=true
```

### API Versioning
```bash
./run-generator.sh \
  --additional-properties useApiVersioning=true,routePrefix=api,versioningPrefix=v,apiVersion=1
```

**Result:** Endpoints at `/api/v1/pets`, `/api/v1/pets/{id}`, etc.

### Full-Featured Configuration
```bash
./run-generator.sh \
  --additional-properties \
    useMediatr=true,\
    useProblemDetails=true,\
    useRecords=true,\
    useAuthentication=true,\
    useValidators=true,\
    useResponseCaching=true,\
    useApiVersioning=true,\
    packageName=MyApi
```

### Using Taskfile
```bash
# Edit Taskfile.yml to add additional properties:
ADDITIONAL_PROPS: "useMediatr=true,useProblemDetails=true"

# Then run:
devbox run task regenerate
```

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
