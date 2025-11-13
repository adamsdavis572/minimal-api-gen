# Method Override Map

**Feature**: 001-fastendpoints-analysis  
**Source File**: `AspnetFastendpointsServerCodegen.java`  
**Base Class**: `AbstractCSharpCodegen`  
**Total Lines**: 222  
**Analysis Date**: 2025-11-10

## Discovery Note

**Important**: The original assumption was that FastEndpoints was implemented as conditional logic within `AspNetCoreServerCodegen`. However, analysis revealed that FastEndpoints is implemented as a **standalone generator** (`AspnetFastendpointsServerCodegen`) that extends `AbstractCSharpCodegen` directly.

**File Location**: `~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AspnetFastendpointsServerCodegen.java`

**Templates Location**: `~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/`

## Override Methods Analysis

| Method Name | Line Range | Signature | Logic Summary | CLI Options / Templates | Override Purpose |
|-------------|------------|-----------|---------------|------------------------|------------------|
| `AspnetFastendpointsServerCodegen()` | 57-81 | `public AspnetFastendpointsServerCodegen()` | Constructor that configures template paths, registers template files, and adds CLI options | Templates: `model.mustache`, `endpoint.mustache`, `request.mustache`<br>CLI Options: `useProblemDetails`, `useRecords`, `useAuthentication`, `useValidators`, `useResponseCaching`, `useApiVersioning`, `routePrefix`, `versioningPrefix`, `apiVersion`, `solutionGuid`, `projectConfigurationGuid` | Sets up FastEndpoints-specific template files and configuration options |
| `processOpts()` | 82-102 | `public void processOpts()` | Processes all CLI options by calling setter methods, then calls parent `processOpts()`, finally adds supporting files | Calls: `setUseProblemDetails()`, `setUseRecordForRequest()`, `setUseAuthentication()`, `setUseValidators()`, `setUseResponseCaching()`, `setUseApiVersioning()`, `setRoutePrefix()`, `setVersioningPrefix()`, `setApiVersion()`, `setSolutionGuid()`, `setProjectConfigurationGuid()`, `addSupportingFiles()` | Orchestrates option processing and file generation |
| `addSupportingFiles()` | 103-125 | `private void addSupportingFiles()` | Adds all supporting template files including conditional authentication files, readme, project files, and configuration | Supporting Files: `loginRequest.mustache`, `userLoginEndpoint.mustache` (conditional), `readme.mustache`, `gitignore`, `solution.mustache`, `project.csproj.mustache`, `launchSettings.json`, `appsettings.json`, `appsettings.Development.json`, `program.mustache` | Registers all project infrastructure files |
| `processOperation()` | 126-132 | `protected void processOperation(CodegenOperation operation)` | Overrides parent to convert HTTP method to PascalCase (e.g., PUT → Put) | Transforms `operation.httpMethod` | Formats HTTP methods for FastEndpoints configuration syntax |
| `setUseProblemDetails()` | 133-140 | `private void setUseProblemDetails()` | Reads `useProblemDetails` CLI option and stores in additional properties | Sets `additionalProperties.put(USE_PROBLEM_DETAILS, ...)` | Enables RFC 7807/9457 error responses |
| `setUseRecordForRequest()` | 141-148 | `private void setUseRecordForRequest()` | Reads `useRecords` CLI option and stores in additional properties | Sets `additionalProperties.put(USE_RECORDS, ...)` | Enables C# record types for requests/responses |
| `setUseAuthentication()` | 149-156 | `private void setUseAuthentication()` | Reads `useAuthentication` CLI option and stores in additional properties | Sets `additionalProperties.put(USE_AUTHENTICATION, ...)` | Enables FastEndpoints authentication |
| `setUseValidators()` | 157-164 | `private void setUseValidators()` | Reads `useValidators` CLI option and stores in additional properties | Sets `additionalProperties.put(USE_VALIDATORS, ...)` | Enables FluentValidation validators |
| `setUseResponseCaching()` | 165-172 | `private void setUseResponseCaching()` | Reads `useResponseCaching` CLI option and stores in additional properties | Sets `additionalProperties.put(USE_RESPONSE_CACHING, ...)` | Enables response caching |
| `setUseApiVersioning()` | 173-180 | `private void setUseApiVersioning()` | Reads `useApiVersioning` CLI option and stores in additional properties | Sets `additionalProperties.put(USE_API_VERSIONING, ...)` | Enables API versioning |
| `setRoutePrefix()` | 181-188 | `private void setRoutePrefix()` | Reads `routePrefix` CLI option and stores in additional properties | Sets `additionalProperties.put(ROUTE_PREFIX, ...)` | Configures route prefix |
| `setVersioningPrefix()` | 189-196 | `private void setVersioningPrefix()` | Reads `versioningPrefix` CLI option and stores in additional properties | Sets `additionalProperties.put(VERSIONING_PREFIX, ...)` | Configures versioning prefix |
| `setApiVersion()` | 197-204 | `private void setApiVersion()` | Reads `apiVersion` CLI option and stores in additional properties | Sets `additionalProperties.put(API_VERSION, ...)` | Sets API version |
| `setSolutionGuid()` | 205-213 | `private void setSolutionGuid()` | Reads or generates `solutionGuid` and stores in additional properties | Sets `additionalProperties.put(SOLUTION_GUID, ...)` | Generates/sets solution GUID |
| `setProjectConfigurationGuid()` | 214-222 | `private void setProjectConfigurationGuid()` | Reads or generates `projectConfigurationGuid` and stores in additional properties | Sets `additionalProperties.put(PROJECT_CONFIGURATION_GUID, ...)` | Generates/sets project configuration GUID |

## Code Excerpts

### Constructor - Template Registration

```java
public AspnetFastendpointsServerCodegen() {
    super();

    outputFolder = "generated-code" + File.separator + "aspnet-fastendpoints";
    embeddedTemplateDir = templateDir = "aspnet-fastendpoints";

    modelTemplateFiles.put("model.mustache", ".cs");
    apiTemplateFiles.put("endpoint.mustache", "Endpoint.cs");
    apiTemplateFiles.put("request.mustache", "Request.cs");

    addSwitch(USE_PROBLEM_DETAILS, "Enable RFC compatible error responses...", useProblemDetails);
    addSwitch(USE_RECORDS, "Use record instead of class for the requests and response.", useRecords);
    addSwitch(USE_AUTHENTICATION, "Enable authentication...", useAuthentication);
    addSwitch(USE_VALIDATORS, "Enable request validators...", useValidators);
    addSwitch(USE_RESPONSE_CACHING, "Enable response caching...", useResponseCaching);
    addSwitch(USE_API_VERSIONING, "Enable API versioning...", useApiVersioning);
    // ... additional options
}
```

### processOpts - Option Processing

```java
public void processOpts() {
    setPackageDescription(openAPI.getInfo().getDescription());

    setUseProblemDetails();
    setUseRecordForRequest();
    setUseAuthentication();
    setUseValidators();
    setUseResponseCaching();
    setUseApiVersioning();
    setRoutePrefix();
    setVersioningPrefix();
    setApiVersion();
    setSolutionGuid();
    setProjectConfigurationGuid();

    super.processOpts();

    addSupportingFiles();
}
```

### addSupportingFiles - Supporting File Registration

```java
private void addSupportingFiles() {
    apiPackage = "Features";
    modelPackage = "Models";
    String packageFolder = sourceFolder + File.separator + packageName;
    
    if (useAuthentication) {
        supportingFiles.add(new SupportingFile("loginRequest.mustache", 
            packageFolder + File.separator + apiPackage, "LoginRequest.cs"));
        supportingFiles.add(new SupportingFile("userLoginEndpoint.mustache", 
            packageFolder + File.separator + apiPackage, "UserLoginEndpoint.cs"));
    }

    supportingFiles.add(new SupportingFile("readme.mustache", "", "README.md"));
    supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
    supportingFiles.add(new SupportingFile("solution.mustache", "", packageName + ".sln"));
    supportingFiles.add(new SupportingFile("project.csproj.mustache", 
        packageFolder, packageName + ".csproj"));
    // ... additional files
}
```

### processOperation - HTTP Method Formatting

```java
protected void processOperation(CodegenOperation operation) {
    super.processOperation(operation);

    // Converts, for example, PUT to Put for endpoint configuration
    operation.httpMethod = operation.httpMethod.charAt(0) + 
        operation.httpMethod.substring(1).toLowerCase(Locale.ROOT);
}
```

## Inheritance Strategy Implications

Since `AspnetFastendpointsServerCodegen` is a **standalone generator** extending `AbstractCSharpCodegen`:

1. **No Conditional Logic to Extract**: There are no `if ("fastendpoints".equals(library))` blocks because this is a dedicated generator
2. **Direct Inheritance Path**: MinimalApiServerCodegen should also extend `AbstractCSharpCodegen` directly (similar pattern)
3. **Complete Rewrite Needed**: Cannot simply override methods - must implement similar structure from scratch
4. **Template Comparison**: Can compare FastEndpoints templates with desired Minimal API templates to identify similarities

## Next Steps for Feature 002

1. Create `MinimalApiServerCodegen` extending `AbstractCSharpCodegen`
2. Implement similar method structure: constructor, `processOpts()`, `addSupportingFiles()`, `processOperation()`
3. Replace FastEndpoints-specific CLI options with Minimal API equivalents
4. Replace FastEndpoints templates with Minimal API templates
5. Maintain framework-agnostic model templates (high reusability as planned)

## Summary

- **Total Methods Documented**: 15 methods
- **Key Override Points**: Constructor (template registration), processOpts (option processing), addSupportingFiles (file registration), processOperation (HTTP method formatting)
- **CLI Options**: 11 options (useProblemDetails, useRecords, useAuthentication, useValidators, useResponseCaching, useApiVersioning, routePrefix, versioningPrefix, apiVersion, solutionGuid, projectConfigurationGuid)
- **Template Files Registered**: 3 in constructor (model, endpoint, request) + ~10 in addSupportingFiles()
- **Base Class**: AbstractCSharpCodegen (NOT AspNetCoreServerCodegen as originally assumed)
