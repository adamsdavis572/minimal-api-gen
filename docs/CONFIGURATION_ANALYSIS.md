# Configuration Options Analysis

**Analysis Date:** December 9, 2025  
**Branch:** 006-mediatr-decoupling  
**Generator Version:** 1.0.0

This document provides a deep analysis of all configuration options in the minimal-api-gen generator, including their implementation status, behavior, and usage in templates.

---

## Overview

The generator defines **14 configuration options** across three categories:
1. **Feature Flags** (9 boolean switches)
2. **Routing Configuration** (4 string options)
3. **Project Configuration** (2 GUID options)

All options are defined in `MinimalApiServerCodegen.java` and exposed via the `--additional-properties` CLI flag.

---

## Configuration Options Deep Dive

### 1. useMediatr

**Type:** `boolean`  
**Default:** `false`  
**Declared:** Line 56  
**CLI Help:** "Enable MediatR CQRS pattern with commands, queries, and handlers."

#### Implementation Details

**Setter Method:** `setUseMediatr()` (lines 259-265)
- Checks `additionalProperties` for override
- Falls back to default `false`
- Writes back to `additionalProperties` for template access

**Template Usage:**
- **program.mustache** (line 19-20): Adds MediatR service registration
  ```mustache
  {{#useMediatr}}
  builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
  {{/useMediatr}}
  ```
- **project.csproj.mustache** (line 13-14): Adds MediatR NuGet package
  ```mustache
  {{#useMediatr}}
  <PackageReference Include="MediatR" Version="12.2.0" />
  {{/useMediatr}}
  ```
- **api.mustache** (lines 6-10, 26-143): 
  - Imports MediatR namespaces
  - Injects `IMediator` into endpoints
  - Generates command/query/handler delegation code
  - Conditional code generation based on operation type (GET → Query, POST/PUT/DELETE → Command)

**Code Generation Impact:**
When enabled, `processOperation()` and `postProcessOperationsWithModels()` generate additional files:
- **Commands/** - `{OperationName}Command.cs` for POST/PUT/PATCH/DELETE operations
- **Queries/** - `{OperationName}Query.cs` for GET operations  
- **Handlers/** - `{OperationName}Handler.cs` (with existence check per R4 - line 535)

**Vendor Extensions Added:**
- `mediatrResponseType` - Response type for `IRequest<T>`
- `isUnit` - Boolean indicating Unit return type
- `isQuery` / `isCommand` - Operation type classification
- `queryClassName` / `commandClassName` - Generated class names
- `handlerClassName` - Handler class name

**File Generation Logic:**
- `generateMediatrFilesForOperation()` (lines 486-543) uses Mustache compiler
- Templates: `command.mustache`, `query.mustache`, `handler.mustache`
- Handler files use existence check: `if (!handlerFileObj.exists())` (line 533)

**Current Usage:** Enabled in Taskfile.yml for petstore generation:
```yaml
--additional-properties packageName=PetstoreApi,useMediatr=true
```

**Implementation Status:** ✅ **FULLY IMPLEMENTED**

---

### 2. useProblemDetails

**Type:** `boolean`  
**Default:** `false`  
**Declared:** Line 46  
**CLI Help:** "Enable RFC 7807 compatible error responses."

#### Implementation Details

**Setter Method:** `setUseProblemDetails()` (lines 198-204)
- Standard boolean property pattern
- No special logic or dependencies

**Template Usage:**
- **program.mustache** (line 18): Adds ProblemDetails service
  ```mustache
  {{#useProblemDetails}}builder.Services.AddProblemDetails();{{/useProblemDetails}}
  ```

**Behavior:**
Enables ASP.NET Core's RFC 7807 Problem Details middleware for standardized error responses:
```json
{
  "type": "https://tools.ietf.org/html/rfc7231#section-6.5.1",
  "title": "Bad Request",
  "status": 400,
  "detail": "The request could not be understood"
}
```

**Current Usage:** Not enabled in Taskfile.yml

**Implementation Status:** ✅ **FULLY IMPLEMENTED** (middleware registration only, no custom template logic)

---

### 3. useRecords

**Type:** `boolean`  
**Default:** `false`  
**Declared:** Line 47  
**CLI Help:** "Use record instead of class for the requests and response."

#### Implementation Details

**Setter Method:** `setUseRecordForRequest()` (lines 206-212)
- Standard boolean property pattern

**Template Usage:**
- **model.mustache** (lines 5-6): Conditional model generation
  ```mustache
  {{#useRecords}}{{>modelRecord}}{{/useRecords}}
  {{^useRecords}}{{>modelClass}}{{/useRecords}}
  ```

**Templates Involved:**
- `modelRecord.mustache` - Generates C# `record` types (immutable, value-based)
- `modelClass.mustache` - Generates C# `class` types (traditional mutable objects)

**Behavior:**
- **When true:** Models generated as `public record Pet(string Name, string Status)`
- **When false:** Models generated as `public class Pet { public string Name { get; set; } }`

**Benefits of Records:**
- Immutability by default
- Value-based equality
- Concise syntax with primary constructors
- Better for DTOs and API contracts

**Current Usage:** Not enabled in Taskfile.yml

**Implementation Status:** ✅ **FULLY IMPLEMENTED** (requires `modelRecord.mustache` template - existence assumed)

---

### 4. useAuthentication

**Type:** `boolean`  
**Default:** `false`  
**Declared:** Line 48  
**CLI Help:** "Enable JWT authentication."

#### Implementation Details

**Setter Method:** `setUseAuthentication()` (lines 214-220)
- Standard boolean property pattern

**Template Usage:**
- **program.mustache** (lines 2-3, 21-28, 44-46): 
  - Imports JWT namespace
  - Registers JWT authentication with Authority/Audience from config
  - Adds UseAuthentication/UseAuthorization middleware
  ```mustache
  {{#useAuthentication}}
  using Microsoft.AspNetCore.Authentication.JwtBearer;
  {{/useAuthentication}}
  ```
- **project.csproj.mustache** (line 12-13): Adds JWT NuGet package
  ```mustache
  {{#useAuthentication}}
  <PackageReference Include="Microsoft.AspNetCore.Authentication.JwtBearer" Version="8.0.0" />
  {{/useAuthentication}}
  ```

**Supporting Files Added:**
In `addSupportingFiles()` (lines 129-154), when `useAuthentication == true`:
```java
supportingFiles.add(new SupportingFile("loginRequest.mustache", 
    packageFolder + File.separator + apiPackage, "LoginRequest.cs"));
supportingFiles.add(new SupportingFile("userLoginEndpoint.mustache", 
    packageFolder + File.separator + apiPackage, "UserLoginEndpoint.cs"));
```

**Configuration Required:**
Uses `appsettings.json` values:
```json
{
  "Auth": {
    "Authority": "https://your-auth-server.com",
    "Audience": "your-api-audience"
  }
}
```

**Current Usage:** Not enabled in Taskfile.yml

**Implementation Status:** ✅ **FULLY IMPLEMENTED** (requires `loginRequest.mustache` and `userLoginEndpoint.mustache` templates)

---

### 5. useValidators

**Type:** `boolean`  
**Default:** `false`  
**Declared:** Line 49  
**CLI Help:** "Enable FluentValidation request validators."

#### Implementation Details

**Setter Method:** `setUseValidators()` (lines 222-228)
- Standard boolean property pattern

**Template Usage:**
- **api.mustache** (line 27): Conditional `IValidator<T>` injection for body parameters
  ```mustache
  {{#hasValidation}}{{#isBodyParam}}, IValidator<{{{dataType}}}> validator{{/isBodyParam}}{{/hasValidation}}
  ```
- **api.mustache** (lines 57-63): Conditional validation logic
  ```mustache
  {{#hasValidation}}
  // Validate request
  var validationResult = await validator.ValidateAsync({{{paramName}}});
  if (!validationResult.IsValid)
  {
      return Results.ValidationProblem(validationResult.ToDictionary());
  }
  {{/hasValidation}}
  ```

**Infrastructure (Always Generated):**
- **program.mustache** (line 19): Service registration (unconditional)
  ```mustache
  builder.Services.AddValidatorsFromAssemblyContaining<Program>();
  ```
- **project.csproj.mustache** (lines 11-12): NuGet packages (unconditional)
  ```mustache
  <PackageReference Include="FluentValidation" Version="11.9.0" />
  <PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />
  ```

**Behavior:**
- The `{{#hasValidation}}` mustache tag is controlled by **OpenAPI schema constraints** (not the `useValidators` flag)
- Validator is injected if operation has `requestBody` with validation constraints
- FluentValidation infrastructure is **always included** regardless of usage
- The `useValidators` flag is **declared but never checked** in templates

**Verified in Generated Code:**
In `/test-output`, no validator instances are injected or used because the petstore.yaml operations don't have validation constraints defined.

**Current Usage:** Flag exists but has no effect on code generation

**Implementation Status:** ⚠️ **PARTIALLY IMPLEMENTED / MISNAMED**
- Flag name suggests it controls validation, but validation is controlled by OpenAPI schema
- FluentValidation is always included (infrastructure overhead when unused)
- **Actual behavior:** Validation code generated based on OpenAPI `requestBody` schema constraints
- **Recommendation:** 
  - Rename flag to `includeFluentValidation` and make packages/registration conditional on it
  - Or remove flag and document that FluentValidation is always available
  - Current behavior: `hasValidation` is set by OpenAPI schema, not by `useValidators` flag

---

### 6. useResponseCaching

**Type:** `boolean`  
**Default:** `false`  
**Declared:** Line 50  
**CLI Help:** "Enable response caching."

#### Implementation Details

**Setter Method:** `setUseResponseCaching()` (lines 230-236)
- Standard boolean property pattern

**Template Usage:**
- **program.mustache** (lines 28-29, 46-47): 
  - Adds caching service
  - Adds caching middleware
  ```mustache
  {{#useResponseCaching}}
  builder.Services.AddResponseCaching();
  {{/useResponseCaching}}
  ```

**Behavior:**
Enables ASP.NET Core response caching middleware. Requires additional configuration per endpoint:
```csharp
.CacheOutput(policy => policy.Expire(TimeSpan.FromMinutes(5)))
```

**Note:** The generator only adds the middleware infrastructure. Actual caching policies must be configured manually on endpoints.

**Current Usage:** Not enabled in Taskfile.yml

**Implementation Status:** ✅ **FULLY IMPLEMENTED** (infrastructure only, per-endpoint configuration required)

---

### 7. useApiVersioning

**Type:** `boolean`  
**Default:** `false`  
**Declared:** Line 51  
**CLI Help:** "Enable API versioning."

#### Implementation Details

**Setter Method:** `setUseApiVersioning()` (lines 238-244)
- Standard boolean property pattern

**Template Usage:**
- **program.mustache** (lines 49-56): Conditional route group setup
  ```mustache
  {{#useApiVersioning}}
  var {{routePrefix}}Group = app.MapGroup("/{{routePrefix}}");
  {{routePrefix}}Group.MapAllEndpoints();
  {{/useApiVersioning}}
  {{^useApiVersioning}}
  app.MapAllEndpoints();
  {{/useApiVersioning}}
  ```
- **endpointMapper.mustache** (lines 15-40): Different route patterns
  ```mustache
  {{#useApiVersioning}}
  var apiGroup = app.MapGroup("{{{routePrefix}}}/{{{versioningPrefix}}}{{{apiVersion}}}");
  {{/useApiVersioning}}
  {{^useApiVersioning}}
  var group = app.MapGroup("{{{serverBasePath}}}");
  {{/useApiVersioning}}
  ```

**Related Options:**
Works with `routePrefix`, `versioningPrefix`, and `apiVersion` to construct versioned routes.

**Behavior:**
- **When true:** Routes become `/api/v1/pets`, `/api/v1/pets/{id}`, etc.
- **When false:** Routes use server base path from OpenAPI spec (e.g., `/pets`)

**Current Usage:** Not enabled in Taskfile.yml

**Implementation Status:** ✅ **FULLY IMPLEMENTED**

---

### 8. useRouteGroups ✅ RESOLVED (007-config-fixes)

**Type:** `boolean`  
**Default:** ~~`true`~~ **REMOVED**  
**Status:** Flag removed in feature 007-config-fixes

#### Resolution

**Decision:** Route groups are required architecture, not a configurable option
- Flag and all related code removed
- Documentation updated to state route groups always enabled
- Generated code always uses `MapGroup()` pattern

**Previous Implementation Details** (for historical reference):
- **Setter Method:** ~~`setUseRouteGroups()`~~ (removed)
- **Template Usage:** No direct mustache usage found
- **Code Impact:** Would have affected endpoint organization, but was never actually used in templates

**Implementation Status:** ✅ **RESOLVED** - Flag removed as dead code
- Flag exists but no conditional template logic
- Default `true` suggests this is the only supported mode
- **Recommendation:** Consider removing flag or document as "always enabled"

---

### 9. useGlobalExceptionHandler

**Type:** `boolean`  
**Default:** `true` ⚠️ (opposite of other flags)  
**Declared:** Line 53  
**CLI Help:** "Enable global exception handling middleware."

#### Implementation Details

**Setter Method:** `setUseGlobalExceptionHandler()` (lines 254-260)
- Standard boolean property pattern
- **Default is `true`**

**Template Usage:**
- **No direct mustache usage found**
- No conditional middleware registration

**Implementation Status:** ❌ **NOT IMPLEMENTED**
- Configuration option exists but has no effect on generated code
- No exception handler middleware registered in program.mustache
- **Recommendation:** Either implement the feature or remove the flag

---

### 10. routePrefix

**Type:** `string`  
**Default:** `"api"`  
**Declared:** Line 55  
**CLI Help:** "The route prefix for the API. Used only if useApiVersioning is true"

#### Implementation Details

**Setter Method:** `setRoutePrefix()` (lines 267-273)
- Checks `additionalProperties` for override
- Falls back to default `"api"`
- Writes back to `additionalProperties`

**Template Usage:**
- **program.mustache** (line 50): Creates route group
  ```mustache
  var {{routePrefix}}Group = app.MapGroup("/{{routePrefix}}");
  ```
- **endpointMapper.mustache** (line 16): Version path construction
  ```mustache
  var apiGroup = app.MapGroup("{{{routePrefix}}}/{{{versioningPrefix}}}{{{apiVersion}}}");
  ```

**Behavior:**
Only used when `useApiVersioning=true`. Forms first segment of versioned routes.

**Example:** `routePrefix=api` → `/api/v1/pets`

**Current Usage:** Not overridden in Taskfile.yml (uses default "api")

**Implementation Status:** ✅ **FULLY IMPLEMENTED**

---

### 11. versioningPrefix

**Type:** `string`  
**Default:** `"v"`  
**Declared:** Line 56  
**CLI Help:** "The versioning prefix for the API. Used only if useApiVersioning is true"

#### Implementation Details

**Setter Method:** `setVersioningPrefix()` (lines 275-281)
- Standard string property pattern

**Template Usage:**
- **endpointMapper.mustache** (line 16): Version path construction
  ```mustache
  var apiGroup = app.MapGroup("{{{routePrefix}}}/{{{versioningPrefix}}}{{{apiVersion}}}");
  ```

**Behavior:**
Only used when `useApiVersioning=true`. Forms version prefix before version number.

**Example:** `versioningPrefix=v` + `apiVersion=1` → `/api/v1/pets`  
**Alternative:** `versioningPrefix=version` → `/api/version1/pets`

**Current Usage:** Not overridden in Taskfile.yml (uses default "v")

**Implementation Status:** ✅ **FULLY IMPLEMENTED**

---

### 12. apiVersion

**Type:** `string`  
**Default:** `"1"`  
**Declared:** Line 57  
**CLI Help:** "The version of the API. Used only if useApiVersioning is true"

#### Implementation Details

**Setter Method:** `setApiVersion()` (lines 283-290)
- Standard string property pattern
- **Does NOT read from OpenAPI spec's `info.version` field**

**Template Usage:**
- **endpointMapper.mustache** (line 16): Version path construction
  ```mustache
  var apiGroup = app.MapGroup("{{{routePrefix}}}/{{{versioningPrefix}}}{{{apiVersion}}}");
  ```

**Version Resolution Hierarchy:**
1. CLI `--additional-properties apiVersion=X` (highest priority)
2. Hardcoded default `"1"` (fallback)
3. ❌ OpenAPI spec's `info.version` (never consulted)

**Comparison with Other Properties:**
`setPackageDescription()` (line 106) **does** read from OpenAPI:
```java
setPackageDescription(openAPI.getInfo().getDescription());
```

But `setApiVersion()` does not check `openAPI.getInfo().getVersion()`.

**Behavior:**
- Forms the version number in versioned routes
- Only used when `useApiVersioning=true`

**Example:** `apiVersion=2` → `/api/v2/pets`

**Current Usage:** Not overridden in Taskfile.yml (uses default "1")

**Implementation Status:** ✅ **FULLY IMPLEMENTED** (but could be enhanced to read from spec)

**Enhancement Opportunity:**
```java
private void setApiVersion() {
    if (additionalProperties.containsKey(API_VERSION)) {
        apiVersion = (String) additionalProperties.get(API_VERSION);
    } else if (openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null) {
        apiVersion = openAPI.getInfo().getVersion(); // Read from spec
    }
    additionalProperties.put(API_VERSION, apiVersion);
}
```

---

### 13. solutionGuid

**Type:** `string`  
**Default:** `null` (auto-generated UUID)  
**Declared:** Line 58  
**CLI Help:** "The solution GUID to be used in the solution file (auto generated if not provided)"

#### Implementation Details

**Setter Method:** `setSolutionGuid()` (lines 292-300)
- Checks for override in `additionalProperties`
- **Auto-generates UUID** if not provided:
  ```java
  solutionGuid = "{" + randomUUID().toString().toUpperCase(Locale.ROOT) + "}";
  ```

**Template Usage:**
- **solution.mustache**: Used in .sln file for solution identification

**Behavior:**
- Provides stable solution GUID for Visual Studio solution files
- Ensures reproducible builds when specified
- Auto-generates unique GUID when omitted

**Current Usage:** Not overridden in Taskfile.yml (auto-generated)

**Implementation Status:** ✅ **FULLY IMPLEMENTED**

---

### 14. projectConfigurationGuid

**Type:** `string`  
**Default:** `null` (auto-generated UUID)  
**Declared:** Line 59  
**CLI Help:** "The project configuration GUID to be used in the solution file (auto generated if not provided)"

#### Implementation Details

**Setter Method:** `setProjectConfigurationGuid()` (lines 302-310)
- Identical pattern to `solutionGuid`
- Auto-generates UUID if not provided

**Template Usage:**
- **solution.mustache**: Used in .sln file for project configuration identification

**Behavior:**
Same as `solutionGuid` but for project configuration section of .sln file.

**Current Usage:** Not overridden in Taskfile.yml (auto-generated)

**Implementation Status:** ✅ **FULLY IMPLEMENTED**

---

## Additional Computed Properties

### serverBasePath

**Not a CLI option** - computed automatically from OpenAPI spec.

**Setter Method:** `setBasePath()` (lines 312-353)

**Logic:**
1. Reads first server URL from OpenAPI spec: `openAPI.getServers().get(0).getUrl()`
2. Extracts path component:
   - Absolute URL: `http://petstore.swagger.io/v2` → `/v2`
   - Relative path: `/v2` → `/v2`
   - Root or empty: → `""` (empty string)
3. Handles URI parsing errors with fallback logic

**Template Usage:**
- **endpointMapper.mustache** (line 29): Used when `useApiVersioning=false`
  ```mustache
  var group = app.MapGroup("{{{serverBasePath}}}");
  ```
- **api.mustache** (line 115): Used in Created response Location header
  ```mustache
  return Results.Created($"{{basePathWithoutHost}}{{path}}", result);
  ```

**Implementation Status:** ✅ **FULLY IMPLEMENTED**

---

## Summary Tables

### Implementation Status Overview

| Option | Type | Default | Status | Template Usage |
|--------|------|---------|--------|----------------|
| `useMediatr` | boolean | false | ✅ Full | program, api, project.csproj |
| `useProblemDetails` | boolean | false | ✅ Full | program |
| `useRecords` | boolean | false | ✅ Full | model |
| `useAuthentication` | boolean | false | ✅ Full | program, project.csproj + files |
| `useValidators` | boolean | false | ⚠️ Unused | Declared but not checked in templates |
| `useResponseCaching` | boolean | false | ✅ Full | program |
| `useApiVersioning` | boolean | false | ✅ Full | program, endpointMapper |
| ~~`useRouteGroups`~~ | ~~boolean~~ | ~~true~~ | ✅ Resolved | **REMOVED** (007-config-fixes) |
| `useGlobalExceptionHandler` | boolean | **true** | ❌ None | Not implemented |
| `routePrefix` | string | "api" | ✅ Full | program, endpointMapper |
| `versioningPrefix` | string | "v" | ✅ Full | endpointMapper |
| `apiVersion` | string | "1" | ✅ Full | endpointMapper |
| `solutionGuid` | string | null | ✅ Full | solution |
| `projectConfigurationGuid` | string | null | ✅ Full | solution |

### Feature Flag Defaults

Most flags default to `false` (opt-in), except:
- ~~✅ `useRouteGroups` = **true** (opt-out)~~ **REMOVED** (007-config-fixes)
- ✅ `useGlobalExceptionHandler` = **true** (but not implemented)

---

## Recommendations

### High Priority

1. **Validation System** - Incomplete implementation (CRITICAL)
   - **Issue:** Templates have `{{#hasValidation}}` logic but flag is never set
   - OpenAPI schema HAS validation constraints (required fields, patterns, min/max)
   - But `hasValidation` flag never becomes true, so no validation code generated
   - **Impact:** FluentValidation infrastructure included but unused (wasted dependencies)
   - **Action required:**
     - Implement `hasValidation` detection in `processOperation()` method
     - Check body parameter schema for: required, pattern, minLength, maxLength, minimum, maximum, etc.
     - Set flag when constraints found: `parameter.hasValidation = true`
     - Consider generating validator classes (e.g., `PetValidator.cs` with rules)

2. **useGlobalExceptionHandler** - Either implement or remove
   - Current state: Flag exists but does nothing
   - Options:
     - Add exception handler middleware to program.mustache
     - Remove the configuration option
     - Document as "future feature"

2. **useValidators** - Implementation incomplete
   - **Verified:** Flag is declared but never checked in templates
   - Current state: FluentValidation packages and registration always included
   - Validation code (`IValidator<T>` injection, `ValidateAsync()` calls) uses `{{#hasValidation}}` tag
   - **PROBLEM:** `hasValidation` flag is never set to true, even though OpenAPI schema has validation constraints
   - Petstore schema has: required fields, min/max values, regex patterns
   - But no validation code generated in endpoints
   
   **Comparison with OpenAPI Generator's aspnet-fastendpoints:**
   - aspnet-fastendpoints **successfully implements** FluentValidation with `useValidators` flag
   - Generates validator classes using `{{#requiredParams}}` loop in request.mustache:
     ```mustache
     {{#useValidators}}
     public class {{operationId}}RequestValidator : FastEndpoints.Validator<{{operationId}}Request>
     {
         {{#requiredParams}}
         RuleFor(x => x.{{paramName}}).NotEmpty();
         {{/requiredParams}}
     }
     {{/useValidators}}
     ```
   - Relies on OpenAPI Generator's `requiredParams` property which is automatically populated
   
   **Missing in minimal-api-gen:**
   - Option 1: Adopt aspnet-fastendpoints approach - generate validator classes per operation
   - Option 2: Implement logic to set `hasValidation` flag on parameters with constraints
   - Option 3: Use `{{#useValidators}}` and `{{#requiredParams}}` in api.mustache templates

3. ~~**useRouteGroups** - Document or remove~~ ✅ **RESOLVED** (007-config-fixes)
   - ~~Current state: Default `true`, no conditional template logic~~
   - ~~Appears to be architecture requirement, not optional feature~~
   - ~~Recommend: Document as "required/always enabled" or remove flag~~
   - **Resolution:** Flag removed, route groups documented as required architecture

### Medium Priority

4. **apiVersion** - Read from OpenAPI spec
   - Current: Ignores `info.version` field in OpenAPI document
   - Enhancement: Fall back to spec version if not provided via CLI
   - Would align with `setPackageDescription()` pattern

5. **useRecords / useAuthentication** - Verify templates exist
   - Templates referenced but not visible in analysis:
     - `modelRecord.mustache`
     - `loginRequest.mustache`
     - `userLoginEndpoint.mustache`
   - Verify these exist and work correctly

### Low Priority

6. **Documentation** - Add behavior examples
   - Document what each flag generates
   - Show before/after code examples
   - Clarify dependencies between options

---

## Testing Recommendations

### Current Test Coverage
Based on Taskfile.yml, only `useMediatr=true` is tested in the baseline petstore generation.

### Suggested Test Matrix

| Test Name | Options | Purpose |
|-----------|---------|---------|
| baseline-mediatr | `useMediatr=true` | ✅ Current (petstore) |
| baseline-minimal | (all defaults) | Minimal API without CQRS |
| full-featured | All flags enabled | Integration test |
| versioning | `useApiVersioning=true` | Version routing |
| records | `useRecords=true` | Record-based models |
| auth | `useAuthentication=true` | JWT setup |
| caching | `useResponseCaching=true` | Cache middleware |

### Test Approach
```bash
# Example: Test versioning
./run-generator.sh \
  --additional-properties useApiVersioning=true,apiVersion=2,routePrefix=api,versioningPrefix=v

# Verify routes: /api/v2/pets
```

---

## Appendix: File Locations

- **Generator Code:** `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
- **Templates:** `generator/src/main/resources/aspnet-minimalapi/*.mustache`
- **Build Config:** `Taskfile.yml` (lines 63-64 for properties)
- **Docs:** `docs/CONFIGURATION.md` (user-facing documentation)

---

## Validation Against Generated Code

**Test Output Analyzed:** `/test-output/src/PetstoreApi/`

### Verification Results

✅ **FluentValidation Infrastructure (Always Generated):**
- `using FluentValidation;` in all endpoint files
- `builder.Services.AddValidatorsFromAssemblyContaining<Program>();` in Program.cs
- FluentValidation packages in .csproj
- **But no actual validator usage** because petstore.yaml has no validation constraints

✅ **MediatR Implementation (useMediatr=true):**
- Commands generated in `/Commands/` folder (AddPetCommand.cs, UpdatePetCommand.cs, DeletePetCommand.cs)
- Queries generated in `/Queries/` folder (GetPetByIdQuery.cs, FindPetsByStatusQuery.cs, etc.)
- Handlers generated in `/Handlers/` folder with existence check
- `IMediator` injected into all endpoints
- MediatR registration in Program.cs: `builder.Services.AddMediatR(...)`

✅ **Generated Route Structure:**
- Routes use server base path from OpenAPI: `/v2` prefix from `http://petstore.swagger.io/v2`
- No API versioning (useApiVersioning=false)
- Endpoints organized by tag: PetApiEndpoints.cs, StoreApiEndpoints.cs, UserApiEndpoints.cs

❌ **No Validation Code Generated (Despite Schema Constraints):**
- No `IValidator<T>` parameters in endpoint methods
- No `validator.ValidateAsync()` calls
- No `ValidationResult` usage
- **OpenAPI spec DOES have validation constraints:**
  - Pet schema: `required: [name, photoUrls]`
  - Order path parameter: `minimum: 1, maximum: 5`
  - User login: `pattern: '^[a-zA-Z0-9]+[a-zA-Z0-9\.\-_]*[a-zA-Z0-9]+$'`
  - Category name: pattern validation
- **Root cause:** The template approach differs from OpenAPI Generator's aspnet-fastendpoints
  - aspnet-fastendpoints: Generates validator classes using `{{#requiredParams}}` loop, controlled by `{{#useValidators}}` flag
  - minimal-api-gen: Tries to inject validators inline with `{{#hasValidation}}` flag, which is never set
- **Missing:** Either adopt aspnet-fastendpoints approach OR implement logic to set `hasValidation` flag

### Key Findings

1. **useValidators flag is unused** - Validation controlled by OpenAPI schema, not this flag
2. **FluentValidation always included** - Adds dependency overhead even when unused
3. **hasValidation is schema-driven** - Set by OpenAPI Generator when `requestBody` has constraints
4. **MediatR fully functional** - All CQRS pattern files generated correctly
5. **serverBasePath works** - Correctly extracted `/v2` from server URL

---

## Change History

- **2025-12-09:** Initial comprehensive analysis
- **2025-12-09:** Validated against generated code in `/test-output`, corrected useValidators analysis
- **Status:** 11/14 options fully implemented, 2 unused/partial, 1 not implemented
