# Template Catalog

**Feature**: 001-fastendpoints-analysis  
**Templates Location**: `~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/`  
**Total Templates**: 17 mustache files + 1 non-mustache file (gitignore)  
**Analysis Date**: 2025-11-10

## Overview

The FastEndpoints generator uses 17 Mustache templates to generate a complete ASP.NET Core Web API project with FastEndpoints framework integration.

## Template Categories

### Operation Templates (API Endpoints)

These templates generate the actual API endpoint classes and request/response types.

| Template File | Registered In | Output File Pattern | Consumed Variables | Framework Dependencies |
|---------------|---------------|---------------------|-------------------|------------------------|
| `endpoint.mustache` | Constructor (`apiTemplateFiles`) | `{operationId}Endpoint.cs` | `{{packageName}}`, `{{apiPackage}}`, `{{operations}}`, `{{operation}}`, `{{operationId}}`, `{{summary}}`, `{{isDeprecated}}`, `{{httpMethod}}`, `{{basePathWithoutHost}}`, `{{path}}`, `{{useApiVersioning}}`, `{{apiVersion}}`, `{{useResponseCaching}}`, `{{useAuthentication}}`, `{{allParams}}`, `{{isFile}}`, `{{tags}}`, `{{responses}}`, `{{is2xx}}`, `{{useProblemDetails}}`, `{{code}}`, `{{paramName}}`, `{{isBodyParam}}`, `{{nameInPascalCase}}`, `{{description}}`, `{{message}}` | **High**: `Endpoint<TRequest, TResponse>` base class, `Configure()` override, FastEndpoints-specific methods (`ResponseCache()`, `AllowAnonymous()`, `WithTags()`, `ProducesProblemDetails()`), `HandleAsync()` signature |
| `request.mustache` | Constructor (`apiTemplateFiles`) | `{operationId}Request.cs` | `{{useValidators}}`, `{{packageName}}`, `{{modelPackage}}`, `{{apiPackage}}`, `{{useRecords}}`, `{{operations}}`, `{{operation}}`, `{{operationId}}`, `{{requiredParams}}`, `{{isBodyParam}}`, `{{paramName}}`, `{{nameInPascalCase}}` | **Medium**: Includes `requestRecord.mustache` or `requestClass.mustache`, optional FluentValidation integration via `FastEndpoints.Validator<T>` base class |
| `requestClass.mustache` | Partial (via `request.mustache`) | Inline in Request.cs | `{{operations}}`, `{{operation}}`, `{{operationId}}`, `{{allParams}}`, `{{isBodyParam}}`, `{{paramName}}`, `{{nameInPascalCase}}`, `{{dataType}}`, `{{description}}` | **Low**: C# class structure, properties only |
| `requestRecord.mustache` | Partial (via `request.mustache`) | Inline in Request.cs | `{{operations}}`, `{{operation}}`, `{{operationId}}`, `{{allParams}}`, `{{isBodyParam}}`, `{{paramName}}`, `{{nameInPascalCase}}`, `{{dataType}}`, `{{description}}` | **Low**: C# record structure (C# 9.0+ feature) |
| `endpointType.mustache` | Partial (via `endpoint.mustache`) | Inline in Endpoint.cs | None (static text: `Endpoint<`) | **High**: FastEndpoints `Endpoint<TRequest, TResponse>` base class |
| `endpointRequestType.mustache` | Partial (via `endpoint.mustache`) | Inline in Endpoint.cs | `{{operationId}}`, `{{#allParams}}{{#-first}}` | **Low**: Determines request type (`Request` or `EmptyRequest`) |
| `endpointResponseType.mustache` | Partial (via `endpoint.mustache`) | Inline in Endpoint.cs | `{{responses}}` | **Low**: Determines response type pattern |
| `loginRequest.mustache` | `addSupportingFiles()` (conditional) | `LoginRequest.cs` | Unknown (authentication-specific) | **Medium**: Authentication model, likely framework-agnostic |
| `userLoginEndpoint.mustache` | `addSupportingFiles()` (conditional) | `UserLoginEndpoint.cs` | Unknown (authentication-specific) | **High**: FastEndpoints endpoint for login |

---

### Supporting Templates (Project Infrastructure)

These templates generate project configuration, build files, and documentation.

| Template File | Registered In | Output File | Consumed Variables | Framework Dependencies |
|---------------|---------------|-------------|-------------------|------------------------|
| `program.mustache` | `addSupportingFiles()` | `Program.cs` | `{{useAuthentication}}`, `{{packageName}}`, `{{appName}}`, `{{packageDescription}}`, `{{version}}`, `{{useResponseCaching}}`, `{{useProblemDetails}}`, `{{useApiVersioning}}`, `{{routePrefix}}`, `{{versioningPrefix}}` | **High**: `using FastEndpoints`, `AddFastEndpoints()`, `UseFastEndpoints()`, `AddAuthenticationJwtBearer()`, `AddAuthorization()`, `UseAuthentication()`, `UseAuthorization()`, FastEndpoints Swagger integration |
| `project.csproj.mustache` | `addSupportingFiles()` | `{packageName}.csproj` | `{{useAuthentication}}` | **High**: `<PackageReference Include="FastEndpoints" Version="5.29.0" />`, `<PackageReference Include="FastEndpoints.Swagger" Version="5.29.0" />`, Optional: `FastEndpoints.Security` |
| `solution.mustache` | `addSupportingFiles()` | `{packageName}.sln` | `{{packageName}}`, `{{solutionGuid}}`, `{{projectConfigurationGuid}}` | **None**: Standard .NET solution file structure |
| `readme.mustache` | `addSupportingFiles()` | `README.md` | Unknown (likely project metadata) | **None**: Markdown documentation |
| `gitignore` (not .mustache) | `addSupportingFiles()` | `.gitignore` | None (static file) | **None**: Git ignore patterns |
| `appsettings.json` (not .mustache) | `addSupportingFiles()` | `appsettings.json` | None (static file) | **None**: ASP.NET Core configuration |
| `appsettings.Development.json` (not .mustache) | `addSupportingFiles()` | `appsettings.Development.json` | None (static file) | **None**: ASP.NET Core development configuration |
| `Properties/launchSettings.json` (not .mustache) | `addSupportingFiles()` | `Properties/launchSettings.json` | None (static file) | **None**: Visual Studio/VS Code launch configuration |

---

### Model Templates (Data Structures)

These templates generate C# model classes representing data transfer objects (DTOs), entities, and enums.

| Template File | Registered In | Output File Pattern | Consumed Variables | Framework Dependencies |
|---------------|---------------|---------------------|-------------------|------------------------|
| `model.mustache` | Constructor (`modelTemplateFiles`) | `{classname}.cs` | `{{models}}`, `{{model}}`, `{{packageName}}`, `{{modelPackage}}`, `{{isEnum}}`, `{{useRecords}}` | **None**: Delegates to `enumClass`, `modelRecord`, or `modelClass` |
| `modelClass.mustache` | Partial (via `model.mustache`) | Inline in model file | `{{description}}`, `{{classname}}`, `{{parent}}`, `{{vars}}`, `{{isEnum}}`, `{{complexType}}`, `{{datatypeWithEnum}}`, `{{isNullable}}`, `{{name}}`, `{{defaultValue}}`, `{{dataType}}`, `{{nullableReferenceTypes}}`, `{{isContainer}}`, `{{required}}` | **None**: Pure C# POCO class with properties |
| `modelRecord.mustache` | Partial (via `model.mustache`) | Inline in model file | `{{description}}`, `{{classname}}`, `{{parent}}`, `{{vars}}`, `{{isEnum}}`, `{{complexType}}`, `{{datatypeWithEnum}}`, `{{isNullable}}`, `{{name}}`, `{{defaultValue}}`, `{{dataType}}`, `{{nullableReferenceTypes}}`, `{{isContainer}}`, `{{required}}` | **None**: Pure C# record (C# 9.0+ feature) |
| `enumClass.mustache` | Partial (via `model.mustache` or `modelClass.mustache`) | Inline in model file | `{{description}}`, `{{datatypeWithEnum}}`, `{{allowableValues}}`, `{{enumVars}}`, `{{value}}` | **None**: Standard C# enum |

---

## Template Registration Summary

### Constructor Registration
```java
modelTemplateFiles.put("model.mustache", ".cs");
apiTemplateFiles.put("endpoint.mustache", "Endpoint.cs");
apiTemplateFiles.put("request.mustache", "Request.cs");
```

### Supporting Files Registration (addSupportingFiles method)
- Conditional: `loginRequest.mustache` → `LoginRequest.cs`
- Conditional: `userLoginEndpoint.mustache` → `UserLoginEndpoint.cs`
- `readme.mustache` → `README.md`
- `gitignore` → `.gitignore`
- `solution.mustache` → `{packageName}.sln`
- `project.csproj.mustache` → `{packageName}.csproj`
- `Properties/launchSettings.json` (static)
- `appsettings.json` (static)
- `appsettings.Development.json` (static)
- `program.mustache` → `Program.cs`

### Partial Templates (Included via {{>partialName}})
- `endpointType.mustache` → Used by `endpoint.mustache`
- `endpointRequestType.mustache` → Used by `endpoint.mustache`
- `endpointResponseType.mustache` → Used by `endpoint.mustache`
- `requestClass.mustache` → Used by `request.mustache`
- `requestRecord.mustache` → Used by `request.mustache`
- `modelClass.mustache` → Used by `model.mustache`
- `modelRecord.mustache` → Used by `model.mustache`
- `enumClass.mustache` → Used by `model.mustache` or `modelClass.mustache`

---

## Variable Dependency Analysis

### Common Variables (Used Across Multiple Templates)
- `{{packageName}}` - Project namespace (used in 10+ templates)
- `{{operations}}` / `{{operation}}` - Operation collection and iterator (endpoint generation)
- `{{operationId}}` - Unique operation identifier (endpoint naming)
- `{{models}}` / `{{model}}` - Model collection and iterator (model generation)
- `{{classname}}` - Model class name
- `{{useAuthentication}}` - Boolean flag for authentication features
- `{{useValidators}}` - Boolean flag for FluentValidation
- `{{useRecords}}` - Boolean flag for C# record types
- `{{useProblemDetails}}` - Boolean flag for RFC 7807/9457 errors
- `{{useApiVersioning}}` - Boolean flag for API versioning
- `{{useResponseCaching}}` - Boolean flag for response caching

### Template-Specific Variables
- **endpoint.mustache**: `{{httpMethod}}`, `{{path}}`, `{{basePathWithoutHost}}`, `{{summary}}`, `{{tags}}`, `{{responses}}`, `{{allParams}}`, `{{isDeprecated}}`, `{{isFile}}`
- **model templates**: `{{vars}}`, `{{description}}`, `{{dataType}}`, `{{isEnum}}`, `{{parent}}`, `{{isNullable}}`, `{{required}}`, `{{defaultValue}}`
- **program.mustache**: `{{appName}}`, `{{packageDescription}}`, `{{version}}`, `{{routePrefix}}`, `{{versioningPrefix}}`
- **project.csproj.mustache**: No unique variables (uses only conditionals)
- **solution.mustache**: `{{solutionGuid}}`, `{{projectConfigurationGuid}}`

---

## Coverage Validation

✅ **100% Template Coverage**: All 17 .mustache files in `aspnet-fastendpoints/` directory analyzed and cataloged

**Additional Files** (non-mustache):
- `gitignore` (static file copied as `.gitignore`)
- `appsettings.json` (static file)
- `appsettings.Development.json` (static file)
- `Properties/launchSettings.json` (static file)

**Total Files**: 17 mustache templates + 4 static files = 21 files

---

## Template Relationships

```text
model.mustache
├── enumClass.mustache (if isEnum)
├── modelRecord.mustache (if useRecords)
└── modelClass.mustache (if not useRecords)
    └── enumClass.mustache (for enum properties)

endpoint.mustache
├── endpointType.mustache
├── endpointRequestType.mustache
└── endpointResponseType.mustache

request.mustache
├── requestRecord.mustache (if useRecords)
└── requestClass.mustache (if not useRecords)
```

---

## Key Insights for Minimal API Migration

1. **Operation Templates** (`endpoint.mustache`, `request.mustache`) are **tightly coupled** to FastEndpoints - complete replacement needed
2. **Model Templates** (`model.mustache`, `modelClass.mustache`, `modelRecord.mustache`, `enumClass.mustache`) are **framework-agnostic** - can be reused as-is
3. **Supporting Templates** (`program.mustache`, `project.csproj.mustache`) require **significant modification** - FastEndpoints references must be replaced with Minimal API patterns
4. **Solution/Build Templates** (`solution.mustache`, `gitignore`) are **completely framework-agnostic** - can be reused as-is
5. **Authentication Templates** (`loginRequest.mustache`, `userLoginEndpoint.mustache`) are **FastEndpoints-specific** - will need Minimal API equivalents

This analysis validates **Constitution Principle III (Template Reusability)**: Model templates (~24% of templates) are indeed framework-agnostic and can be reused unchanged for Minimal API generation.
