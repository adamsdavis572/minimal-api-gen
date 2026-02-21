# Quickstart: Endpoint Authorization Filter

**Feature**: 009-endpoint-auth-filter  
**Audience**: Developers implementing or testing authorization in generated Minimal APIs  
**Time to Complete**: 15-20 minutes

## Prerequisites

- .NET 8.0 SDK installed
- Generated Petstore API with NuGet packaging (`useNugetPackaging=true`)
- Basic understanding of ASP.NET Core authentication/authorization

## What You'll Build

An authorization filter that:
- ✅ Protects write operations (POST/PUT/DELETE) with `WriteAccess` policy
- ✅ Protects read operations (GET) with `ReadAccess` policy
- ✅ Uses endpoint name metadata for policy mapping
- ✅ Does NOT modify any generated Contract code
- ✅ Allows developers to choose: authorized or non-authorized endpoints

---

## Step 1: Generate Base API (5 minutes)

### 1.1 Clean and Generate
```bash
cd /Users/adam/scratch/git/minimal-api-gen

# Clean previous generation
devbox run task clean:generated

# Generate with NuGet packaging and authentication flag
devbox run task gen:petstore ADDITIONAL_PROPS="useNugetPackaging=true,useAuthentication=true"
```

**Expected Output**:
```
test-output/
├── Contract/
│   ├── Endpoints/
│   │   ├── PetEndpoints.cs
│   │   ├── StoreEndpoints.cs
│   │   └── UserEndpoints.cs
│   └── Extensions/
│       └── EndpointExtensions.cs  → AddApiEndpoints() method
└── Implementation/
    └── Program.cs  → Authentication/authorization middleware configured
```

### 1.2 Verify Endpoint Names
Check that endpoints have `.WithName()` metadata:

```bash
grep -n "WithName" test-output/Contract/Endpoints/PetEndpoints.cs
```

**Expected Output**:
```
15:.WithName("AddPet")
30:.WithName("UpdatePet")
45:.WithName("DeletePet")
60:.WithName("GetPetById")
75:.WithName("FindPetsByStatus")
90:.WithName("FindPetsByTags")
```

---

## Step 2: Create Authorization Test Artifacts (5 minutes)

### 2.1 Create Test Directory
```bash
mkdir -p petstore-tests/Auth
```

### 2.2 Create PermissionEndpointFilter.cs (Test Artifact)
```bash
cat > petstore-tests/Auth/PermissionEndpointFilter.cs << 'EOF'
using Microsoft.AspNetCore.Authorization;

namespace PetstoreApi.Filters;

/// <summary>
/// Endpoint filter that enforces authorization based on endpoint name to policy mapping
/// </summary>
public class PermissionEndpointFilter : IEndpointFilter
{
    private readonly IAuthorizationService _authorizationService;
    
    // Static mapping of endpoint names to policy names
    private static readonly Dictionary<string, string> EndpointPolicies = new()
    {
        // Pet endpoints
        { "AddPet", "WriteAccess" },
        { "UpdatePet", "WriteAccess" },
        { "DeletePet", "WriteAccess" },
        { "GetPetById", "ReadAccess" },
        { "FindPetsByStatus", "ReadAccess" },
        { "FindPetsByTags", "ReadAccess" },
        
        // Store endpoints
        { "PlaceOrder", "WriteAccess" },
        { "DeleteOrder", "WriteAccess" },
        { "GetOrderById", "ReadAccess" },
        { "GetInventory", "ReadAccess" },
        
        // User endpoints
        { "CreateUser", "WriteAccess" },
        { "UpdateUser", "WriteAccess" },
        { "DeleteUser", "WriteAccess" },
        { "GetUserByName", "ReadAccess" },
        { "LoginUser", "ReadAccess" },
        { "LogoutUser", "ReadAccess" }
    };
    
    public PermissionEndpointFilter(IAuthorizationService authorizationService)
    {
        _authorizationService = authorizationService ?? 
            throw new ArgumentNullException(nameof(authorizationService));
    }
    
    public async ValueTask<object?> InvokeAsync(
        EndpointFilterInvocationContext context, 
        EndpointFilterDelegate next)
    {
        var httpContext = context.HttpContext;
        
        // Retrieve endpoint name from metadata
        var endpointName = httpContext.GetEndpoint()?
            .Metadata
            .GetMetadata<EndpointNameMetadata>()?
            .EndpointName;
        
        // If no endpoint name or no policy mapping, allow request
        if (string.IsNullOrEmpty(endpointName) || 
            !EndpointPolicies.TryGetValue(endpointName, out var policyName))
        {
            return await next(context);
        }
        
        // Authorize based on policy
        var authorizationResult = await _authorizationService.AuthorizeAsync(
            httpContext.User, 
            resource: null, 
            policyName);
        
        // Return 403 Forbidden if authorization fails
        if (!authorizationResult.Succeeded)
        {
            return Results.Forbid();
        }
        
        // Authorization succeeded, continue to endpoint handler
        return await next(context);
    }
}
EOF
```

---

## Step 3: Create Authorized Extension Method (3 minutes)

### 3.1 Create AuthorizedEndpointExtensions.cs (Test Artifact)
```bash
cat > petstore-tests/Auth/AuthorizedEndpointExtensions.cs << 'EOF'
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Routing;
using PetstoreApi.Endpoints;
using PetstoreApi.Filters;

namespace PetstoreApi.Extensions;

/// <summary>
/// Extension methods for registering API endpoints WITH authorization filtering
/// </summary>
public static class AuthorizedEndpointExtensions
{
    /// <summary>
    /// Registers all API endpoints with authorization filter applied.
    /// Delegates to generated endpoint registration methods.
    /// </summary>
    public static IEndpointRouteBuilder AddAuthorizedApiEndpoints(this IEndpointRouteBuilder endpoints)
    {
        // Create route group with authorization filter
        var group = endpoints.MapGroup("/v2")
            .AddEndpointFilter<PermissionEndpointFilter>();
        
        // Delegate to generated endpoint registration methods
        PetEndpoints.MapPetEndpoints(group);
        StoreEndpoints.MapStoreEndpoints(group);
        UserEndpoints.MapUserEndpoints(group);
        
        return endpoints;
    }
}
EOF
```

---

## Step 4: Create Test Program.cs (5 minutes)

### 4.1 Create Program.cs with Builder Methods
```bash
mkdir -p petstore-tests/PetstoreApi
cat > petstore-tests/PetstoreApi/Program.cs << 'EOF'
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
builder.Services.AddSwaggerGen();
builder.Services.AddApiValidators();
builder.Services.AddApiHandlers();
builder.Services.AddApplicationServices();

// ========== AUTHORIZATION CONFIGURATION (UNCOMMENT TO ENABLE) ==========
// ConfigureAuthServices(builder.Services, builder.Configuration);
// ========================================================================

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

// ========== AUTHORIZATION MIDDLEWARE (UNCOMMENT TO ENABLE) ==========
// app.UseAuthentication();
// app.UseAuthorization();
// =====================================================================

// ========== ENDPOINT REGISTRATION (CHOOSE ONE) ==========
app.AddApiEndpoints();              // No authorization
// app.AddAuthorizedApiEndpoints();    // With authorization filter
// =========================================================

app.Run();

// ========== BUILDER METHODS ==========

void ConfigureAuthServices(IServiceCollection services, IConfiguration configuration)
{
    services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
        .AddJwtBearer(options =>
        {
            options.Authority = configuration["Auth:Authority"];
            options.Audience = configuration["Auth:Audience"];
        });
    
    services.AddAuthorization(options =>
    {
        options.AddPolicy("ReadAccess", policy => 
            policy.RequireClaim("permission", "read"));
        options.AddPolicy("WriteAccess", policy => 
            policy.RequireClaim("permission", "write"));
    });
}

public partial class Program { }
EOF
```

---

## Step 5: Update Taskfile.yml to Copy Auth Artifacts (3 minutes)

### 5.1 Add Auth Copy Steps

Edit `Taskfile.yml` and find the `gen:copy-test-stubs` task (around line 80-110).

Add these lines AFTER the existing ServiceCollectionExtensions copy:

```yaml
      - echo "Copying authorization test artifacts..."
      - mkdir -p {{.TEST_OUTPUT_DIR}}/src/PetstoreApi/Filters
      - cp petstore-tests/Auth/PermissionEndpointFilter.cs {{.TEST_OUTPUT_DIR}}/src/PetstoreApi/Filters/
      - cp petstore-tests/Auth/AuthorizedEndpointExtensions.cs {{.OUTPUT_EXTENSIONS_DIR}}/
      - echo "Copying test Program.cs..."
      - cp petstore-tests/PetstoreApi/Program.cs {{.TEST_OUTPUT_DIR}}/src/PetstoreApi/
      - echo "✓ Authorization artifacts copied successfully"
```

---

## Step 6: Enable Authorization in Program.cs (2 minutes)

### 6.1 Uncomment Authorization Configuration

Edit the COPIED `test-output/src/PetstoreApi/Program.cs`:

**Change 1** - Uncomment auth services (line ~18):
```csharp
// Before:
// ConfigureAuthServices(builder.Services, builder.Configuration);

// After:
ConfigureAuthServices(builder.Services, builder.Configuration);
```

**Change 2** - Uncomment auth middleware (line ~32):
```csharp
// Before:
// app.UseAuthentication();
// app.UseAuthorization();

// After:
app.UseAuthentication();
app.UseAuthorization();
```

**Change 3** - Switch to authorized endpoints (line ~36):
```csharp
// Before:
app.AddApiEndpoints();              // No authorization
// app.AddAuthorizedApiEndpoints();    // With authorization filter

// After:
// app.AddApiEndpoints();              // No authorization
app.AddAuthorizedApiEndpoints();    // With authorization filter
```

---

## Step 7: Build and Test (5 minutes)

### 7.1 Build the Implementation Project
```bash
devbox run task build:impl-nuget
```

**Expected Output**:
```
Copying authorization test artifacts...
✓ Authorization artifacts copied successfully
Build succeeded.
    0 Warning(s)
    0 Error(s)
```

### 7.2 Run Existing Unit Tests
```bash
devbox run task test:unit
```

**Expected Result**: All 45 tests should pass (they use test authentication).

### 5.3 Manual Test - Start API
```bash
devbox run task api:run
```

**Expected Output**:
```
info: Microsoft.Hosting.Lifetime[14]
      Now listening on: http://localhost:5000
```

### 5.4 Test Without Authentication (401 Expected)
```bash
curl -X POST http://localhost:5000/v2/pet \
  -H "Content-Type: application/json" \
  -d '{"name":"Fluffy","photoUrls":[]}'
```

**Expected Response**: `401 Unauthorized` (no authentication header)

### 5.5 Test With Wrong Permission (403 Expected)
Create a test JWT with only "read" permission, then:
```bash
curl -X POST http://localhost:5000/v2/pet \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_WITH_READ_PERMISSION>" \
  -d '{"name":"Fluffy","photoUrls":[]}'
```

**Expected Response**: `403 Forbidden` (has read but needs write)

---

## Step 8: Create Test Authentication (Optional - For Testing)

For testing without real JWT tokens, create a test authentication handler:

### 6.1 Create TestAuthHandler.cs
```bash
cat > petstore-tests/TestExtensions/TestAuthHandler.cs << 'EOF'
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using System.Security.Claims;
using System.Text.Encodings.Web;

namespace PetstoreApi.Tests;

public class TestAuthHandler : AuthenticationHandler<AuthenticationSchemeOptions>
{
    public const string SchemeName = "TestScheme";
    
    public TestAuthHandler(
        IOptionsMonitor<AuthenticationSchemeOptions> options,
        ILoggerFactory logger,
        UrlEncoder encoder)
        : base(options, logger, encoder)
    {
    }
    
    protected override Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        // Check for test header
        if (!Context.Request.Headers.TryGetValue("X-Test-Permission", out var permissionValue))
        {
            return Task.FromResult(AuthenticateResult.NoResult());
        }
        
        var claims = new[] { new Claim("permission", permissionValue.ToString()) };
        var identity = new ClaimsIdentity(claims, SchemeName);
        var principal = new ClaimsPrincipal(identity);
        var ticket = new AuthenticationTicket(principal, SchemeName);
        
        return Task.FromResult(AuthenticateResult.Success(ticket));
    }
}
EOF
```

### 6.2 Update CustomWebApplicationFactory.cs
```csharp
public class AuthorizedWebApplicationFactory : WebApplicationFactory<Program>
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            services.AddSingleton<IPetStore, InMemoryPetStore>();
            
            // Replace JWT authentication with test authentication
            services.AddAuthentication(TestAuthHandler.SchemeName)
                .AddScheme<AuthenticationSchemeOptions, TestAuthHandler>(
                    TestAuthHandler.SchemeName, 
                    options => { });
        });
    }
}
```

### 6.3 Write Authorization Tests
```csharp
[Fact]
public async Task AddPet_WithWritePermission_ReturnsCreated()
{
    // Arrange
    var factory = new AuthorizedWebApplicationFactory();
    var client = factory.CreateClient();
    client.DefaultRequestHeaders.Add("X-Test-Permission", "write");
    
    var newPet = new Pet { Name = "Fluffy", PhotoUrls = new List<string>() };
    
    // Act
    var response = await client.PostAsJsonAsync("/v2/pet", newPet);
    
    // Assert
    response.StatusCode.Should().Be(HttpStatusCode.Created);
}

[Fact]
public async Task AddPet_WithoutWritePermission_ReturnsForbidden()
{
    // Arrange
    var factory = new AuthorizedWebApplicationFactory();
    var client = factory.CreateClient();
    client.DefaultRequestHeaders.Add("X-Test-Permission", "read");  // Only read
    
    var newPet = new Pet { Name = "Fluffy", PhotoUrls = new List<string>() };
    
    // Act
    var response = await client.PostAsJsonAsync("/v2/pet", newPet);
    
    // Assert
    response.StatusCode.Should().Be(HttpStatusCode.Forbidden);
}
```

---

## Step 9: Bruno Integration Tests (Optional)

### 7.1 Create Authorized Test Files
```bash
# Create test with write permission
cat > petstore-tests/bruno/pet/add-pet-authorized.bru << 'EOF'
meta {
  name: Add Pet (Authorized)
  type: http
  seq: 2
}

post {
  url: http://localhost:5000/v2/pet
  body: json
  auth: none
}

headers {
  X-Test-Permission: write
}

body:json {
  {
    "name": "Fluffy",
    "photoUrls": []
  }
}

assert {
  res.status: eq 201
}
EOF

# Create test with insufficient permission
cat > petstore-tests/bruno/pet/add-pet-unauthorized.bru << 'EOF'
meta {
  name: Add Pet (Unauthorized)
  type: http
  seq: 3
}

post {
  url: http://localhost:5000/v2/pet
  body: json
  auth: none
}

headers {
  X-Test-Permission: read
}

body:json {
  {
    "name": "Fluffy",
    "photoUrls": []
  }
}

assert {
  res.status: eq 403
}
EOF
```

### 7.2 Run Bruno Tests
```bash
devbox run task bruno:run-main-suite
```

**Expected Result**: 8 tests (6 existing + 2 new authorization tests)

---

## Common Issues

### Issue 1: "IAuthorizationService not registered"
**Symptom**: Filter constructor fails with DI exception

**Solution**: Ensure `builder.Services.AddAuthorization()` is called in Program.cs

---

### Issue 2: All requests return 403
**Symptom**: Even requests with correct permissions fail

**Possible Causes**:
1. Middleware order wrong (UseAuthorization before UseAuthentication)
2. Policy names don't match filter mappings
3. Claim type/value case mismatch

**Solution**: Verify middleware order and policy configuration

---

### Issue 3: Filter not executing
**Symptom**: Requests succeed even without authentication

**Possible Cause**: Using `app.AddApiEndpoints()` instead of `app.AddAuthorizedApiEndpoints()`

**Solution**: Change to `app.AddAuthorizedApiEndpoints()` in Program.cs

---

## Verification Checklist

- [ ] Generated code in `Contract/` directory is unchanged
- [ ] `PermissionEndpointFilter.cs` exists in `Implementation/Filters/`
- [ ] `AuthorizedEndpointExtensions.cs` exists in `Implementation/Extensions/`
- [ ] Program.cs calls `AddAuthorizedApiEndpoints()` instead of `AddApiEndpoints()`
- [ ] Authorization policies defined: `ReadAccess`, `WriteAccess`
- [ ] Middleware order correct: `UseAuthentication()` before `UseAuthorization()`
- [ ] All 45 existing unit tests pass
- [ ] POST requests without write permission return 403
- [ ] GET requests without read permission return 403

---

## Next Steps

### Generator Integration (Future)
1. Create `permissionEndpointFilter.mustache` template
2. Create `authorizedEndpointExtensions.mustache` template
3. Update `program.mustache` to add policy definitions
4. Add CLI flag: `useAuthorization=true`

### Enhancements
1. External configuration (JSON file for endpoint-to-policy mappings)
2. Wildcard policy patterns (e.g., `*Pet*` matches all pet endpoints)
3. Multiple policies per endpoint (OR/AND logic)
4. Audit logging for authorization failures

---

## Summary

You've successfully:
- ✅ Created an authorization filter using `IEndpointFilter`
- ✅ Mapped endpoint names to authorization policies
- ✅ Applied filter to route group without modifying generated code
- ✅ Tested authorization with different permission levels

**Total Files Created**: 2 (PermissionEndpointFilter.cs, AuthorizedEndpointExtensions.cs)  
**Total Files Modified**: 1 (Program.cs)  
**Generated Files Changed**: 0 (immutability preserved)

The solution is production-ready and can be integrated into the generator for automatic generation in future releases.
