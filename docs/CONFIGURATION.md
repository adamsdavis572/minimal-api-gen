# Generator Configuration Reference

This page documents all configuration options available for the minimal-api-gen generator.

## Table of Contents

- [Overview](#overview)
- [Configuration Options](#configuration-options)
- [Usage Examples](#usage-examples)
- [Comparison with OpenAPI Generator](#comparison-with-openapi-generator)

---

## Overview

The minimal-api-gen generator provides 20+ configuration options through the `--additional-properties` flag (or `ADDITIONAL_PROPS` in Taskfile). These options control the features, structure, and packaging of the generated code.

**Setting Options:**
```bash
# Via Taskfile (recommended)
devbox run task gen:petstore ADDITIONAL_PROPS="key1=value1,key2=value2"

# Via Docker
docker run --rm -v $(pwd):/workspace adamsdavis/minimal-api-generator:latest \
  generate -g aspnetcore-minimalapi \
  -i /workspace/spec.yaml -o /workspace/output \
  --additional-properties key1=value1,key2=value2
```

**Default Configuration:**
The generator is opinionated toward modern ASP.NET Core patterns with sensible defaults for minimal APIs.

### Quick Reference

| Category | Options Count | Key Features |
|----------|---------------|--------------|
| **Core Features** | 8 | MediatR, Validators, Records, Problem Details, Auth, Caching, Versioning |
| **NuGet Packaging** | 7 | Separate contracts project, version, metadata, license, tags |
| **Routing** | 3 | Route prefix, versioning prefix, API version |
| **Project Config** | 4 | Namespace, solution GUIDs |

**Total: 22 configuration options**

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
| `useGlobalExceptionHandler` | boolean | `true` | Add application-wide exception handler middleware |

### NuGet Packaging

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `useNugetPackaging` | boolean | `false` | Generate separate Contracts project for NuGet distribution |
| `packageVersion` | string | OAS version or `"1.0.0"` | Package version (CLI overrides OpenAPI spec version) |
| `packageDescription` | string | OAS description or auto-generated | Package description for NuGet feed |
| `packageLicenseExpression` | string | `"Apache-2.0"` | SPDX license expression (e.g., Apache-2.0, MIT, GPL-3.0) |
| `packageRepositoryUrl` | string | `null` | Git repository URL (optional) |
| `packageProjectUrl` | string | `null` | Project homepage URL (optional) |
| `packageTags` | string | `"openapi;minimal-api;contracts"` | Semicolon-separated NuGet tags |

**Package Version Priority:**
1. CLI option `packageVersion=x.y.z` (highest priority)
2. OpenAPI spec `info.version`
3. Default `"1.0.0"`

**Example:**
```bash
devbox run task gen:petstore ADDITIONAL_PROPS="\
  useNugetPackaging=true,\
  packageVersion=2.5.0,\
  packageDescription=Petstore API contracts,\
  packageLicenseExpression=MIT,\
  packageRepositoryUrl=https://github.com/mycompany/petstore,\
  packageProjectUrl=https://petstore.example.com,\
  packageTags=petstore;api;microservices"
```

### Routing & Organization

> **Note**: Route groups (MapGroup) are the required architecture for this generator and are not configurable. All endpoints are automatically organized using `MapGroup` by OpenAPI tag.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `routePrefix` | string | `"api"` | Route prefix for all endpoints (e.g., `/api/pets`) |
| `versioningPrefix` | string | `"v"` | Version prefix when `useApiVersioning=true` (e.g., `v` → `/v1/...`) |
| `apiVersion` | string | `"1"` | API version string when `useApiVersioning=true` |

### Project Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `packageName` | string | `"Org.OpenAPITools"` | Root C# namespace for generated code |
| `solutionGuid` | string | auto-generated | GUID for .sln solution file |
| `projectConfigurationGuid` | string | auto-generated | GUID for project configuration |
| `contractsProjectGuid` | string | auto-generated | GUID for Contracts project (when useNugetPackaging=true) |

---

## Project Structure Variations

The generator produces different project structures based on the `useNugetPackaging` flag:

### Single Project (useNugetPackaging=false)

**Default structure** - all code in one project:

```
test-output/
├── src/Org.OpenAPITools/
│   ├── Converters/                    # JSON enum converters
│   ├── Extensions/
│   │   ├── EndpointMapper.cs          # Endpoint registration
│   │   └── ServiceCollectionExtensions.cs
│   ├── Features/
│   │   ├── PetApiEndpoints.cs         # Pet endpoints (MapGroup)
│   │   ├── StoreApiEndpoints.cs
│   │   └── UserApiEndpoints.cs
│   ├── Models/
│   │   ├── Pet.cs                     # DTOs
│   │   ├── Order.cs
│   │   └── User.cs
│   ├── Program.cs
│   └── Org.OpenAPITools.csproj
└── Org.OpenAPITools.sln
```

**Best for:** Simple APIs, single deployments, monolithic applications

### Dual Project (useNugetPackaging=true)

**Separated structure** - contracts split for NuGet distribution:

```
test-output/
├── Contract/                          # Source files (copied during build)
│   ├── Converters/                    # Enum converters (shared)
│   └── Endpoints/                     # Endpoint definitions (shared)
│       ├── PetApiEndpoints.cs
│       ├── StoreApiEndpoints.cs
│       └── UserApiEndpoints.cs
├── src/
│   ├── Org.OpenAPITools.Contracts/   # ← Publishable NuGet package
│   │   ├── Extensions/
│   │   │   └── EndpointExtensions.cs  # AddApiEndpoints()
│   │   ├── (Models linked from ../Org.OpenAPITools/Models/)
│   │   └── Org.OpenAPITools.Contracts.csproj
│   └── Org.OpenAPITools/             # ← Implementation project
│       ├── Extensions/
│       │   ├── HandlerExtensions.cs   # Handler DI
│       │   ├── ServiceCollectionExtensions.cs
│       │   └── EndpointMapper.cs
│       ├── Models/                    # DTOs (source location)
│       │   ├── Pet.cs
│       │   └── ...
│       ├── Program.cs
│       └── Org.OpenAPITools.csproj   # References Contracts package
└── Org.OpenAPITools.sln              # Multi-project solution
```

**Best for:** Microservices, client SDK distribution, API-first development

**NuGet Workflow:**
```bash
# 1. Generate with NuGet packaging
devbox run task gen:petstore ADDITIONAL_PROPS="useNugetPackaging=true,packageVersion=1.2.0"

# 2. Pack the Contracts project
dotnet pack test-output/src/Org.OpenAPITools.Contracts/ -c Release -o packages/

# 3. Push to NuGet feed
dotnet nuget push packages/Org.OpenAPITools.Contracts.1.2.0.nupkg --source https://api.nuget.org/v3/index.json

# 4. Consumers reference the package
dotnet add package Org.OpenAPITools.Contracts
```

---

## Usage Examples

### Basic Generation (Defaults)
```bash
devbox run task generate-petstore-minimal-api
```

### Enable NuGet Packaging
```bash
devbox run task gen:petstore ADDITIONAL_PROPS="useNugetPackaging=true"
```

**Generated structure:**
- `src/Org.OpenAPITools.Contracts/` - NuGet package project
- `src/Org.OpenAPITools/` - Implementation project
- Endpoints and models in Contracts project
- Handler implementations in main project

### NuGet with Custom Metadata
```bash
devbox run task gen:petstore ADDITIONAL_PROPS="\
  useNugetPackaging=true,\
  packageVersion=2.0.0,\
  packageDescription=Petstore API contracts for microservices,\
  packageLicenseExpression=MIT,\
  packageRepositoryUrl=https://github.com/mycompany/petstore-api,\
  packageProjectUrl=https://docs.petstore.com,\
  packageTags=petstore;api;microservices;contracts"
```

### Enable MediatR CQRS Pattern
```bash
devbox run task gen:petstore ADDITIONAL_PROPS="useMediatr=true"
```

**Generated structure:**
- Commands: `AddPetCommand`, `UpdatePetCommand`, `DeletePetCommand`
- Queries: `GetPetByIdQuery`, `ListPetsQuery`
- Handlers: `AddPetCommandHandler`, `GetPetByIdQueryHandler`, etc.

### Enable Multiple Features
```bash
devbox run task gen:petstore ADDITIONAL_PROPS="useMediatr=true,useProblemDetails=true,useRecords=true,useValidators=true"
```

### API Versioning
```bash
./run-generator.sh \
  --additional-properties useApiVersioning=true,routePrefix=api,versioningPrefix=v,apiVersion=1
```

**Result:** Endpoints at `/api/v1/pets`, `/api/v1/pets/{id}`, etc.

### Full-Featured Configuration with NuGet
```bash
devbox run task gen:petstore ADDITIONAL_PROPS="\
  useNugetPackaging=true,\
  useMediatr=true,\
  useProblemDetails=true,\
  useRecords=true,\
  useAuthentication=true,\
  useValidators=true,\
  useResponseCaching=true,\
  useApiVersioning=true,\
  packageName=MyCompany.PetstoreApi,\
  packageVersion=3.0.0,\
  packageDescription=Enterprise Petstore API,\
  packageLicenseExpression=MIT,\
  routePrefix=api,\
  versioningPrefix=v,\
  apiVersion=3"
```

**Result:** 
- Endpoints at `/api/v3/pets`
- Separate NuGet Contracts package
- MediatR CQRS with validators
- Records for DTOs
- Problem Details error responses
- Version 3.0.0 with MIT license

### Using Taskfile
```bash
# Generate with inline properties
devbox run task gen:petstore ADDITIONAL_PROPS="useMediatr=true,useValidators=true"

# Or edit Taskfile.yml to set defaults, then run:
devbox run task gen:petstore
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
- Many class/operation modifiers and enable/optOut switches

### Options Only in minimal-api-gen

- `useMediatr`: MediatR CQRS pattern (Commands, Queries, Handlers)
- `useNugetPackaging`: Separate Contracts project for NuGet distribution
- `packageVersion`: CLI override for package version (3-tier priority)
- `packageDescription`: NuGet description with smart defaults
- `packageLicenseExpression`: SPDX license expression
- `packageRepositoryUrl`: Git repository URL
- `packageProjectUrl`: Project homepage URL
- `packageTags`: NuGet discoverability tags
- `contractsProjectGuid`: GUID for Contracts project

### Key Takeaways

- **minimal-api-gen** is optimized for **ASP.NET Core Minimal APIs** with MediatR/CQRS patterns and NuGet contract distribution
- **ASPNETServer** is optimized for **Controller-based APIs** with extensive legacy support
- If migrating from ASPNETServer: Most core options map cleanly, but `useMediatr` and NuGet packaging features are minimal-api-gen exclusives

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
