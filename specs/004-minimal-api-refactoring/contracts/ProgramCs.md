# Contract: program.mustache → Program.cs

**Template**: `program.mustache` (MODIFY - Infrastructure)  
**Output**: `test-output/src/PetstoreApi/Program.cs`  
**Status**: MODIFY (70% framework-specific, 30% reusable)

---

## Transformation Specification

### FROM: FastEndpoints Pattern
```csharp
using FastEndpoints;
using FastEndpoints.Swagger;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddFastEndpoints();
builder.Services.SwaggerDocument();

var app = builder.Build();

app.UseFastEndpoints();
app.UseSwaggerGen();

app.Run();
```

### TO: Minimal API Pattern
```csharp
using FluentValidation;
using PetstoreApi.Extensions;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddProblemDetails();
builder.Services.AddValidatorsFromAssemblyContaining<Program>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseRouting();

app.MapAllEndpoints();

app.Run();
```

---

## Changes Required

### 1. Package References
**REMOVE**:
- `FastEndpoints`
- `FastEndpoints.Swagger`

**ADD**:
- `Swashbuckle.AspNetCore` (6.5.0+)
- `FluentValidation.DependencyInjectionExtensions` (11.9.0+)

### 2. Service Registration
**REMOVE**:
```csharp
builder.Services.AddFastEndpoints();
builder.Services.SwaggerDocument();
```

**ADD**:
```csharp
builder.Services.AddEndpointsApiExplorer();  // Required for Swagger
builder.Services.AddSwaggerGen();            // Swashbuckle
builder.Services.AddProblemDetails();        // RFC 9457 errors
builder.Services.AddValidatorsFromAssemblyContaining<Program>(); // FluentValidation DI
```

### 3. Middleware Pipeline
**REMOVE**:
```csharp
app.UseFastEndpoints();
app.UseSwaggerGen();
```

**ADD**:
```csharp
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseRouting();

app.MapAllEndpoints(); // Extension method from EndpointMapper.cs
```

### 4. Using Statements
**REMOVE**:
- `using FastEndpoints;`
- `using FastEndpoints.Swagger;`

**ADD**:
- `using FluentValidation;`
- `using {{packageName}}.Extensions;` (for MapAllEndpoints)

---

## Template Variables

| Variable | Type | Default | Example | Purpose |
|----------|------|---------|---------|---------|
| `{{packageName}}` | String | Required | `"PetstoreApi"` | Root namespace |
| `{{useAuthentication}}` | Boolean | `false` | `false` | Include JWT middleware |
| `{{useResponseCaching}}` | Boolean | `false` | `false` | Include caching middleware |
| `{{useProblemDetails}}` | Boolean | `true` | `true` | Use RFC 9457 errors |
| `{{routePrefix}}` | String | `""` | `"/v2"` | API version prefix |

---

## Conditional Blocks

### Authentication (Optional)
```csharp
{{#useAuthentication}}
using Microsoft.AspNetCore.Authentication.JwtBearer;

// In ConfigureServices:
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options => {
        options.Authority = builder.Configuration["Auth:Authority"];
        options.Audience = builder.Configuration["Auth:Audience"];
    });

// In Pipeline:
app.UseAuthentication();
app.UseAuthorization();
{{/useAuthentication}}
```

### Response Caching (Optional)
```csharp
{{#useResponseCaching}}
// In ConfigureServices:
builder.Services.AddResponseCaching();

// In Pipeline:
app.UseResponseCaching();
{{/useResponseCaching}}
```

### Route Prefix (Optional)
```csharp
{{#routePrefix}}
app.MapGroup("{{routePrefix}}").MapAllEndpoints();
{{/routePrefix}}
{{^routePrefix}}
app.MapAllEndpoints();
{{/routePrefix}}
```

---

## Dependencies

**Generated Files** (must exist):
- `Extensions/EndpointMapper.cs` (provides `MapAllEndpoints()` extension method)

**External Packages**:
- `Swashbuckle.AspNetCore`
- `FluentValidation.DependencyInjectionExtensions`

**Framework APIs**:
- `WebApplicationBuilder.Services` (DI container)
- `WebApplication.MapGroup()` (route groups)
- `IEndpointRouteBuilder` (endpoint mapping)

---

## Validation Rules

1. **Service Registration Order**:
   - `AddEndpointsApiExplorer()` BEFORE `AddSwaggerGen()`
   - `AddValidatorsFromAssemblyContaining<T>()` after builder creation

2. **Middleware Order**:
   - `UseSwagger()` before `UseSwaggerUI()`
   - `UseAuthentication()` before `UseAuthorization()`
   - `UseRouting()` before endpoint mapping
   - `MapAllEndpoints()` as final mapping call

3. **Environment Checks**:
   - Swagger UI only in Development environment
   - HTTPS redirection configurable per environment

---

## Expected Output

**File**: `test-output/src/PetstoreApi/Program.cs`

**Characteristics**:
- 30-40 lines (without authentication)
- No endpoint definitions (delegated to TagEndpoints classes)
- Single `MapAllEndpoints()` call
- Swagger configured for all environments
- FluentValidation registered via DI

**Signature**:
```csharp
namespace PetstoreApi;

public class Program
{
    public static void Main(string[] args)
    {
        var builder = WebApplication.CreateBuilder(args);
        
        // Services...
        
        var app = builder.Build();
        
        // Middleware...
        
        app.Run();
    }
}
```

---

## TDD Verification

**Tests Must Pass** (from Feature 003):
- `PetEndpointTests.AddPet_ReturnsCreated()`
- `PetEndpointTests.GetPetById_ReturnsOk()`
- `PetEndpointTests.UpdatePet_ReturnsOk()`
- `PetEndpointTests.DeletePet_WithApiKey_ReturnsNoContent()`

**Validation**:
1. Generate code with new template
2. Run `devbox run dotnet build` → must succeed
3. Run `devbox run dotnet test` → all 7 tests GREEN
4. If RED → analyze test output, fix template, regenerate, repeat
