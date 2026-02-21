# Contract: Program.cs Configuration

**Status**: NEW - Test artifact (static file with builder methods, NOT generated)  
**Source**: `petstore-tests/PetstoreApi/Program.cs`  
**Copied To**: `test-output/src/PetstoreApi/Program.cs` (via gen:copy-test-stubs)  
**Purpose**: Test Program.cs with builder methods that can be commented/uncommented to enable/disable authentication and authorization

**Generator Integration**: NONE - Program.cs remains a test artifact. Only AuthorizedEndpointExtensions.cs gets minimal template

---

## Purpose

Configure the ASP.NET Core application to:
1. Register authentication services (JWT Bearer)
2. Register authorization services with policy definitions
3. Add authentication/authorization middleware to request pipeline
4. Call `AddAuthorizedApiEndpoints()` instead of `AddApiEndpoints()`

**Pattern**: Extend existing `program.mustache` template with authorization configuration blocks

---

## Required Changes to program.mustache

### Change 1: Add Policy Definitions (Extend Existing Block)

**Current Template** (lines 32-38):
```mustache
{{#useAuthentication}}
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = builder.Configuration["Auth:Authority"];
        options.Audience = builder.Configuration["Auth:Audience"];
    });
builder.Services.AddAuthorization();{{/useAuthentication}}
```

**Updated Template** (add policy configuration):
```mustache
{{#useAuthentication}}
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = builder.Configuration["Auth:Authority"];
        options.Audience = builder.Configuration["Auth:Audience"];
    });
builder.Services.AddAuthorization(options =>
{
    // Authorization policies for endpoint filter
    options.AddPolicy("ReadAccess", policy => 
        policy.RequireClaim("permission", "read"));
    options.AddPolicy("WriteAccess", policy => 
        policy.RequireClaim("permission", "write"));
});{{/useAuthentication}}
```

**Explanation**: Changed from `.AddAuthorization()` to `.AddAuthorization(options => { /* policies */ })` to define authorization policies.

---

### Change 2: Keep Existing Middleware Wiring

**Current Template** (appears later in file after `var app = builder.Build();`):
```mustache
{{#useAuthentication}}
app.UseAuthentication();
app.UseAuthorization();{{/useAuthentication}}
```

**No Change Required**: Middleware wiring already correct (authentication before authorization).

---

### Change 3: NO CHANGE to Endpoint Registration

**Current Template** (existing endpoint registration):
```mustache
{{#useNugetPackaging}}
app.AddApiEndpoints();  // From Contract package
{{/useNugetPackaging}}
{{^useNugetPackaging}}
app.MapAllEndpoints();  // Direct registration
{{/useNugetPackaging}}
```

**Important**: We do NOT modify the template to call `AddAuthorizedApiEndpoints()`. Instead:
- Test artifacts will manually call `AddAuthorizedApiEndpoints()` in Program.cs
- Developers choose which extension method to use
- Generated Contract code remains unchanged

**Rationale**: Satisfies FR-012 requirement that `AddApiEndpoints()` must still work without authorization.

---

## Generated Program.cs Structure (with useAuthentication=true)

```csharp
using FluentValidation;
using PetstoreApi.Extensions;
using PetstoreApi.Contracts.Extensions;
using Microsoft.AspNetCore.Authentication.JwtBearer;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new PetstoreApi.Converters.EnumMemberJsonConverterFactory());
    options.SerializerOptions.PropertyNameCaseInsensitive = true;
});
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new Microsoft.OpenApi.Models.OpenApiInfo
    {
        Title = "Petstore API",
        Description = "Petstore sample API",
        Version = "v1"
    });
});

// Validators (existing)
builder.Services.AddApiValidators();

// Handlers (existing)
builder.Services.AddApiHandlers();

// Application services (existing)
builder.Services.AddApplicationServices();

// 🆕 Authentication & Authorization (NEW)
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = builder.Configuration["Auth:Authority"];
        options.Audience = builder.Configuration["Auth:Audience"];
    });
builder.Services.AddAuthorization(options =>
{
    // Authorization policies for endpoint filter
    options.AddPolicy("ReadAccess", policy => 
        policy.RequireClaim("permission", "read"));
    options.AddPolicy("WriteAccess", policy => 
        policy.RequireClaim("permission", "write"));
});

var app = builder.Build();

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

// 🆕 Authentication & Authorization Middleware (NEW)
app.UseAuthentication();
app.UseAuthorization();

// 🆕 MANUAL CHANGE: Call authorized extension method (test artifact only)
// app.AddApiEndpoints();           // Original: No authorization
app.AddAuthorizedApiEndpoints();    // New: With authorization filter

app.Run();

// Make Program class accessible to tests
public partial class Program { }
```

---

## Configuration File (appsettings.json)

### Development Configuration
```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "Auth": {
    "Authority": "https://localhost:5001",
    "Audience": "petstore-api"
  },
  "AllowedHosts": "*"
}
```

### Test Configuration (appsettings.Test.json)
```json
{
  "Auth": {
    "Authority": "https://test-auth-server",
    "Audience": "petstore-api-test"
  }
}
```

**Note**: For testing, we'll use test authentication handler (`TestAuthHandler`) instead of real JWT validation.

---

## Validation Rules

### 1. Service Registration Order
```csharp
// ✅ CORRECT ORDER
builder.Services.AddAuthentication(...);  // 1. Authentication first
builder.Services.AddAuthorization(...);   // 2. Authorization second

// ❌ INCORRECT - order doesn't matter for services, but convention is auth before authz
```

### 2. Middleware Order (CRITICAL)
```csharp
// ✅ CORRECT ORDER
app.UseAuthentication();   // 1. MUST come before UseAuthorization
app.UseAuthorization();    // 2. MUST come after UseAuthentication

// ❌ INCORRECT - authorization will fail because user is not authenticated
app.UseAuthorization();
app.UseAuthentication();
```

**Why**: `UseAuthentication()` populates `HttpContext.User` with claims. `UseAuthorization()` checks those claims. If authorization runs first, `User` is null/anonymous.

### 3. Policy Naming
```csharp
// ✅ CORRECT - policy names match filter mappings
options.AddPolicy("ReadAccess", ...);   // Used in: EndpointPolicies["GetPetById"] = "ReadAccess"
options.AddPolicy("WriteAccess", ...);  // Used in: EndpointPolicies["AddPet"] = "WriteAccess"

// ❌ INCORRECT - mismatch will cause authorization to fail
options.AddPolicy("READ_ACCESS", ...);  // Name doesn't match filter mapping
```

### 4. Claim Requirements
```csharp
// ✅ CORRECT - claim type and value match JWT tokens
policy.RequireClaim("permission", "read");   // Matches: JWT.permission = "read"

// ❌ INCORRECT - case mismatch
policy.RequireClaim("Permission", "Read");   // Claims are case-sensitive
```

---

## Testing Strategy

### Unit Tests (Configuration Validation)
```csharp
[Fact]
public void Program_WithAuthentication_RegistersPolicies()
{
    // Arrange
    var builder = WebApplication.CreateBuilder();
    
    // Simulate useAuthentication=true configuration
    builder.Services.AddAuthorization(options =>
    {
        options.AddPolicy("ReadAccess", policy => policy.RequireClaim("permission", "read"));
        options.AddPolicy("WriteAccess", policy => policy.RequireClaim("permission", "write"));
    });
    
    var app = builder.Build();
    
    // Act
    var authOptions = app.Services.GetService<IOptions<AuthorizationOptions>>();
    
    // Assert
    authOptions.Value.GetPolicy("ReadAccess").Should().NotBeNull();
    authOptions.Value.GetPolicy("WriteAccess").Should().NotBeNull();
}
```

### Integration Tests (End-to-End)
```csharp
public class AuthorizedWebApplicationFactory : WebApplicationFactory<Program>
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            // Replace JWT authentication with test authentication
            services.AddAuthentication("Test")
                .AddScheme<AuthenticationSchemeOptions, TestAuthHandler>("Test", options => { });
        });
        
        builder.ConfigureAppConfiguration((context, config) =>
        {
            // Override Auth settings for testing
            config.AddInMemoryCollection(new Dictionary<string, string>
            {
                ["Auth:Authority"] = "https://test-auth",
                ["Auth:Audience"] = "test-api"
            });
        });
    }
}
```

---

## Edge Cases

### 1. Missing appsettings.json Configuration
**Problem**: `Auth:Authority` or `Auth:Audience` not configured

**Behavior**:
```csharp
options.Authority = builder.Configuration["Auth:Authority"];  // Returns null
```

**Solution**: Add validation in startup:
```csharp
if (string.IsNullOrEmpty(builder.Configuration["Auth:Authority"]))
{
    throw new InvalidOperationException("Auth:Authority configuration is required when useAuthentication=true");
}
```

### 2. IAuthorizationService Not Registered
**Problem**: `.AddAuthorization()` not called

**Behavior**: `PermissionEndpointFilter` constructor fails with DI exception

**Solution**: Ensured by template - `{{#useAuthentication}}` block includes `.AddAuthorization(...)`

### 3. Middleware Order Incorrect
**Problem**: `UseAuthorization()` called before `UseAuthentication()`

**Behavior**: All requests fail authorization (User is anonymous)

**Solution**: Template enforces correct order:
```mustache
{{#useAuthentication}}
app.UseAuthentication();
app.UseAuthorization();{{/useAuthentication}}
```

---

## Generator Integration (Future)

### CLI Flag
```bash
java -jar generator.jar generate \
  -g aspnetcore-minimalapi \
  -i petstore.yaml \
  -o output/ \
  --additional-properties useAuthentication=true,useMediatr=true
```

### Template Modifications
Only ONE change needed: Update policy configuration block in `program.mustache`

**Before**:
```mustache
builder.Services.AddAuthorization();
```

**After**:
```mustache
builder.Services.AddAuthorization(options =>
{
    options.AddPolicy("ReadAccess", policy => policy.RequireClaim("permission", "read"));
    options.AddPolicy("WriteAccess", policy => policy.RequireClaim("permission", "write"));
});
```

**Total Changes**: 5 lines in one template file

---

## Rollback Strategy

If authorization causes issues, revert by:
1. Change `app.AddAuthorizedApiEndpoints()` to `app.AddApiEndpoints()`
2. Remove `useAuthentication=true` flag from generation command
3. Regenerate code

Generated Contract code remains unchanged, so rollback is safe.

---

**Status**: Complete - Ready for implementation in test artifacts
