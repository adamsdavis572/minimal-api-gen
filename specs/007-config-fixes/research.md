# Research: Configuration Options Fixes

**Date**: 2025-12-12  
**Feature**: 007-config-fixes  
**Purpose**: Document template reuse strategy from aspnet-fastendpoints generator

## Phase 0: Template Analysis & Reuse Strategy

### Decision 1: Validator Class Generation Approach

**Context**: Need to generate FluentValidation validator classes from OpenAPI schema constraints.

**Options Evaluated**:
1. **Inline Validator Injection** (current broken approach)
   - Injects `IValidator<T>` as endpoint parameter
   - Requires `hasValidation` flag detection logic
   - Not standard FluentValidation pattern

2. **Validator Classes** (aspnet-fastendpoints approach) ✅ SELECTED
   - Generates separate validator class per operation request
   - Inherits from `FastEndpoints.Validator<T>` or `AbstractValidator<T>`
   - Uses `{{#requiredParams}}` from OpenAPI Generator framework
   - FastEndpoints auto-registers validators (Minimal APIs require manual registration)

**Rationale**: 
- aspnet-fastendpoints template proven and working
- `requiredParams` property already populated by OpenAPI Generator base class
- Validator classes are framework-agnostic (just inherit from different base)
- Minimal changes needed: change base class from `FastEndpoints.Validator<T>` to `AbstractValidator<T>`

**Alternatives Considered**:
- Custom constraint detection: Would require parsing OpenAPI schema manually, reinventing wheel
- DataAnnotations: Doesn't match FluentValidation approach already partially implemented

---

### Decision 2: Template File Reuse from aspnet-fastendpoints

**Source Template**: `modules/openapi-generator/src/main/resources/aspnet-fastendpoints/request.mustache`

**Key Sections to Adopt**:

```mustache
{{#useValidators}}using FluentValidation;{{/useValidators}}

{{#useValidators}}
{{#operations}}{{#operation}}
public class {{operationId}}RequestValidator : FastEndpoints.Validator<{{operationId}}Request>
{
    public {{operationId}}RequestValidator()
    {
    {{#requiredParams}}
        RuleFor(x => x.{{#isBodyParam}}{{paramName}}{{/isBodyParam}}{{^isBodyParam}}{{nameInPascalCase}}{{/isBodyParam}}).NotEmpty();
    {{/requiredParams}}
    }
}
{{/operation}}{{/operations}}
{{/useValidators}}
```

**Minimal APIs Adaptation**:
```mustache
{{#useValidators}}using FluentValidation;{{/useValidators}}

{{#useValidators}}
{{#operations}}{{#operation}}
public class {{operationId}}RequestValidator : AbstractValidator<{{operationId}}Request>
{
    public {{operationId}}RequestValidator()
    {
    {{#requiredParams}}
        RuleFor(x => x.{{#isBodyParam}}{{paramName}}{{/isBodyParam}}{{^isBodyParam}}{{nameInPascalCase}}{{/isBodyParam}}).NotEmpty();
    {{/requiredParams}}
    }
}
{{/operation}}{{/operations}}
{{/useValidators}}
```

**Changes Required**:
- Line 11: Change `FastEndpoints.Validator<T>` → `AbstractValidator<T>`
- That's it! Everything else can be reused as-is.

**File Location**: Create new `generator/src/main/resources/aspnet-minimalapi/validator.mustache`

---

### Decision 3: Exception Handler Middleware

**Context**: Need to implement UseExceptionHandler middleware for unhandled exceptions.

**ASP.NET Core Pattern** (from official docs):
```csharp
app.UseExceptionHandler(exceptionHandlerApp =>
{
    exceptionHandlerApp.Run(async context =>
    {
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        context.Response.ContentType = "application/problem+json";
        
        var exceptionHandlerPathFeature = context.Features.Get<IExceptionHandlerPathFeature>();
        var exception = exceptionHandlerPathFeature?.Error;
        
        var problemDetails = new ProblemDetails
        {
            Status = StatusCodes.Status500InternalServerError,
            Title = "An error occurred",
            Detail = exception?.Message
        };
        
        await context.Response.WriteAsJsonAsync(problemDetails);
    });
});
```

**Integration with useProblemDetails**:
- When `useProblemDetails=true` AND `useGlobalExceptionHandler=true`: Use ProblemDetails format
- When `useProblemDetails=false` AND `useGlobalExceptionHandler=true`: Use simple JSON error
- When `useGlobalExceptionHandler=false`: No middleware registered

**Template Implementation**: Modify `program.mustache` to add conditional middleware registration.

---

### Decision 4: Configuration Flag Cleanup

**useRouteGroups Flag Analysis**:
- **Current State**: Flag exists, defaults to true, never checked in templates
- **Usage Search**: No `{{#useRouteGroups}}` or `{{^useRouteGroups}}` found in any template
- **Conclusion**: Dead code, always uses MapGroup pattern

**Removal Strategy**:
1. Remove from `MinimalApiServerCodegen.java`:
   - Line 52: `private boolean useRouteGroups = true;`
   - Lines 246-252: `setUseRouteGroups()` method
   - Line ~70: CLI option declaration
2. Update docs/CONFIGURATION.md to state "Route groups required architecture"
3. Update docs/CONFIGURATION_ANALYSIS.md to mark issue resolved
4. No template changes needed (already using MapGroup unconditionally)

---

## Findings Summary

### Template Reuse Matrix

| Component | Source | Adaptation | Complexity |
|-----------|--------|------------|------------|
| Validator Classes | aspnet-fastendpoints/request.mustache | Change base class name | **LOW** - 1 line change |
| FluentValidation Import | aspnet-fastendpoints/request.mustache | Direct copy | **TRIVIAL** - exact reuse |
| RuleFor Logic | aspnet-fastendpoints/request.mustache | Direct copy | **TRIVIAL** - exact reuse |
| requiredParams Loop | OpenAPI Generator built-in | No change needed | **NONE** - framework provides |
| Exception Handler | ASP.NET Core pattern | New code | **MEDIUM** - middleware config |
| Flag Removal | N/A | Delete code | **TRIVIAL** - removal only |

### Risk Assessment

**LOW RISK**:
- Validator template reuse: Proven pattern, minimal adaptation
- requiredParams property: Provided by framework, well-documented
- Flag removal: Dead code, no impact

**MEDIUM RISK**:
- Exception handler: New code, requires testing for ProblemDetails integration
- Validator registration: Minimal APIs require manual DI registration vs FastEndpoints auto-registration

**MITIGATION**:
- Comprehensive test coverage for exception scenarios
- Verify validator registration in Program.cs generates correctly
- Test both `useValidators=true` and `useValidators=false` paths

---

## OpenAPI Generator Framework Support

### Built-in Properties Available

From OpenAPI Generator's AbstractCSharpCodegen and CodegenOperation:

```java
// Already populated by framework:
operation.requiredParams      // List of required parameters
operation.allParams          // All parameters
param.isBodyParam           // Boolean flag
param.paramName             // Parameter name (camelCase)
param.nameInPascalCase      // Parameter name (PascalCase)
param.isPathParam           // Boolean flag
param.isQueryParam          // Boolean flag
param.required              // Boolean flag
```

**Key Insight**: We don't need to implement constraint detection logic. OpenAPI Generator already parses the schema and populates `requiredParams`. The aspnet-fastendpoints template simply loops over this existing property.

---

## Implementation Sequence

Based on dependencies and risk:

1. **Phase 1** (Lowest Risk): Remove useRouteGroups flag
   - Pure deletion, no generation impact
   - Validates build system works

2. **Phase 2** (Medium Risk): Implement validator generation
   - Create validator.mustache template
   - Modify MinimalApiServerCodegen.java to generate validator files
   - Make FluentValidation packages conditional
   - Test with petstore spec

3. **Phase 3** (Highest Risk): Implement exception handler
   - Modify program.mustache for middleware
   - Test exception scenarios
   - Verify ProblemDetails integration

This order allows incremental validation and easier debugging if issues arise.

---

## References

- aspnet-fastendpoints request.mustache: `modules/openapi-generator/src/main/resources/aspnet-fastendpoints/request.mustache`
- Generated validator example: `samples/server/petstore/aspnet/fastendpoints-useValidators/src/Org.OpenAPITools/Features/PetApiRequest.cs`
- ASP.NET Core Exception Handling: https://learn.microsoft.com/en-us/aspnet/core/fundamentals/error-handling
- FluentValidation Docs: https://docs.fluentvalidation.net/
- OpenAPI Generator AbstractCSharpCodegen: `modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AbstractCSharpCodegen.java`
