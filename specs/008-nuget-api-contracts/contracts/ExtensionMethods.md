# Contract: DI Extension Methods for Service Registration

**Feature**: 008-nuget-api-contracts  
**Status**: Draft  
**Purpose**: Document the public API surface for registering Endpoints, Validators, and Handlers in the DI container

---

## Overview

Feature 008 exposes three extension methods to simplify service registration in consuming applications:

1. **AddApiEndpoints()** - Registers all endpoint routes (Contracts.dll) - **REQUIRED**
2. **AddApiValidators()** - Registers FluentValidation validators (Contracts.dll) - **RECOMMENDED**
3. **AddApiHandlers()** - Registers MediatR handlers (Implementation.dll) - **OPTIONAL**

**Design Philosophy** (from FR-007, FR-008, FR-009):
- AddApiEndpoints is REQUIRED because there's no standard ASP.NET Core equivalent for bulk endpoint registration
- AddApiValidators is RECOMMENDED because validators are in a different assembly (cross-assembly registration)
- AddApiHandlers is OPTIONAL because developers can use standard MediatR: `services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))`

---

## Extension Method 1: AddApiEndpoints()

### Signature
```csharp
namespace {{packageName}}.Contracts.Extensions;

public static class EndpointExtensions
{
    /// <summary>
    /// Registers all API endpoints from the Contracts package.
    /// </summary>
    /// <param name="endpoints">The endpoint route builder.</param>
    /// <returns>The endpoint route builder for chaining.</returns>
    public static IEndpointRouteBuilder AddApiEndpoints(this IEndpointRouteBuilder endpoints)
}
```

---

### Behavior

**Purpose** (FR-007): Provides a single entry point to register all generated endpoint routes in a consuming application.

**What it does**:
1. Creates route groups per OpenAPI tag (e.g., `/pet`, `/store`, `/user`)
2. Maps each endpoint with HTTP verb, route pattern, and handler delegate
3. Applies metadata (OpenAPI annotations, request/response types)
4. Returns `IEndpointRouteBuilder` for fluent chaining

**Related FRs**: FR-007 (Required for Contracts package), US2 (Inject Services)

---

### Assembly Location

**Package**: `PetstoreApi.Contracts.dll` (distributed in NuGet package)  
**Namespace**: `PetstoreApi.Contracts.Extensions`  
**File**: `Extensions/EndpointExtensions.cs` (generated in Contracts project)

**Why in Contracts** (from FR-022):
- Endpoints are part of the API contract (what the API exposes)
- Must be in same assembly as Endpoint definitions
- Consumers need one-line registration: `app.AddApiEndpoints()`

---

### Usage Example

```csharp
// Program.cs in consuming application (PetstoreApi/Program.cs)
using PetstoreApi.Contracts.Extensions;

var builder = WebApplication.CreateBuilder(args);

// Register services (MediatR, validators, custom services)
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
builder.Services.AddApiValidators();
builder.Services.AddSingleton<IDbContext, InMemoryDbContext>();

var app = builder.Build();

// Register all API endpoints (REQUIRED)
app.AddApiEndpoints();

app.Run();
```

**Result**: All endpoints from Contracts package (PetEndpoints, StoreEndpoints, UserEndpoints) are registered and callable via HTTP.

---

### Implementation Strategy

**Generated code structure** (EndpointExtensions.cs):
```csharp
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Routing;

namespace {{packageName}}.Contracts.Extensions;

public static class EndpointExtensions
{
    public static IEndpointRouteBuilder AddApiEndpoints(this IEndpointRouteBuilder endpoints)
    {
        // Group endpoints by OpenAPI tag
        {{#operationsByTag}}
        var {{tagCamelCase}}Group = endpoints.MapGroup("/{{tagLowerCase}}")
            .WithTags("{{tag}}")
            .WithOpenApi();

        // Register endpoints for this tag
        {{#operations}}
        {{tagCamelCase}}Group.Map{{httpMethod}}("{{path}}", {{packageName}}.Contracts.Endpoints.{{tagPascalCase}}Endpoints.{{operationId}})
            .WithName("{{operationId}}")
            .Produces<{{returnType}}>({{successStatusCode}})
            {{#errorResponses}}
            .Produces<ProblemDetails>({{statusCode}})
            {{/errorResponses}};
        {{/operations}}

        {{/operationsByTag}}

        return endpoints;
    }
}
```

**Key techniques**:
- Uses `MapGroup()` to organize endpoints by tag (e.g., `/pet`, `/store`)
- Each endpoint maps to a static delegate method in Endpoints/ classes
- Metadata applied via `.WithName()`, `.Produces<>()`, `.WithOpenApi()`
- Returns `IEndpointRouteBuilder` for fluent chaining

**Example output** (PetEndpoints.cs):
```csharp
namespace PetstoreApi.Contracts.Endpoints;

public static class PetEndpoints
{
    public static async Task<IResult> AddPet(
        [FromBody] AddPetCommand command,
        [FromServices] IMediator mediator,
        CancellationToken cancellationToken)
    {
        var result = await mediator.Send(command, cancellationToken);
        return Results.Ok(result);
    }

    public static async Task<IResult> GetPetById(
        [FromRoute] long petId,
        [FromServices] IMediator mediator,
        CancellationToken cancellationToken)
    {
        var query = new GetPetByIdQuery { PetId = petId };
        var result = await mediator.Send(query, cancellationToken);
        return Results.Ok(result);
    }
}
```

---

### Why REQUIRED (vs Optional)

**Rationale** (from FR-007):
- ASP.NET Core has NO built-in equivalent for bulk endpoint registration
- Without this method, consumers would need to manually call 100+ `Map*()` methods
- Endpoints are code, not types (cannot use assembly scanning like validators/handlers)
- Provides clean abstraction: "Register everything from this package"

**Alternative approaches** (all worse):
1. **Manual registration**: `app.MapGet("/pet/{id}", PetEndpoints.GetPetById)` × 50 operations = maintenance nightmare
2. **Reflection**: Possible but slow, complex, fragile (method signatures vary)
3. **Source generators**: Over-engineered for this use case

---

## Extension Method 2: AddApiValidators()

### Signature
```csharp
namespace {{packageName}}.Contracts.Extensions;

public static class ValidatorExtensions
{
    /// <summary>
    /// Registers all FluentValidation validators from the Contracts package.
    /// Only available when useValidators=true.
    /// </summary>
    /// <param name="services">The service collection.</param>
    /// <returns>The service collection for chaining.</returns>
    public static IServiceCollection AddApiValidators(this IServiceCollection services)
}
```

---

### Behavior

**Purpose** (FR-008): Registers FluentValidation validators from the Contracts assembly with proper lifetime (Scoped).

**What it does**:
1. Scans Contracts.dll for all `AbstractValidator<T>` descendants
2. Registers each as `IValidator<T>` → `ConcreteValidator` in DI container
3. Uses `ServiceLifetime.Scoped` (recommended for web APIs)
4. Returns `IServiceCollection` for fluent chaining

**Related FRs**: FR-008 (Recommended extension), FR-022 (Cross-assembly registration)  
**Related RQs**: RQ-004 (FluentValidation assembly scanning)

---

### Assembly Location

**Package**: `PetstoreApi.Contracts.dll` (distributed in NuGet package)  
**Namespace**: `PetstoreApi.Contracts.Extensions`  
**File**: `Extensions/ValidatorExtensions.cs` (generated in Contracts project, conditional on `useValidators=true`)

**Why in Contracts** (from FR-022):
- Validators are in Contracts.dll (alongside DTOs)
- Must use `typeof(Validator).Assembly` to reference Contracts assembly
- If in Implementation.dll, would need cross-assembly type reference (ugly)

---

### Usage Example

```csharp
// Program.cs in consuming application
using PetstoreApi.Contracts.Extensions;

var builder = WebApplication.CreateBuilder(args);

// Register validators from Contracts package (RECOMMENDED)
builder.Services.AddApiValidators();

// MediatR will automatically discover validators via DI
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

var app = builder.Build();
app.AddApiEndpoints();
app.Run();
```

**Result**: All validators (PetValidator, OrderValidator) registered as `IValidator<T>` in DI container.

---

### Implementation Strategy

**Generated code structure** (ValidatorExtensions.cs):
```csharp
using FluentValidation;
using Microsoft.Extensions.DependencyInjection;
using System.Reflection;

namespace {{packageName}}.Contracts.Extensions;

public static class ValidatorExtensions
{
    public static IServiceCollection AddApiValidators(this IServiceCollection services)
    {
        // Use FluentValidation's built-in assembly scanning
        services.AddValidatorsFromAssembly(
            Assembly.GetExecutingAssembly(), // Contracts.dll (this assembly)
            ServiceLifetime.Scoped,           // Scoped lifetime (web API best practice)
            includeInternalTypes: false       // Only public validators
        );

        return services;
    }
}
```

**Key techniques**:
- Uses `AddValidatorsFromAssembly()` from FluentValidation.DependencyInjectionExtensions
- `Assembly.GetExecutingAssembly()` gets Contracts.dll (where validators live)
- `ServiceLifetime.Scoped` ensures validators created per HTTP request
- Automatically discovers all `AbstractValidator<T>` descendants

**How FluentValidation scanning works** (from RQ-004):
```csharp
// Pseudocode of FluentValidation internals
foreach (var type in assembly.GetExportedTypes())
{
    if (type is AbstractValidator<T> && !type.IsAbstract)
    {
        services.AddScoped(typeof(IValidator<T>), type);
    }
}
```

**Example validators** (in Contracts package):
```csharp
namespace PetstoreApi.Contracts.Validators;

public class PetValidator : AbstractValidator<Pet>
{
    public PetValidator()
    {
        RuleFor(x => x.Name).NotEmpty().MaximumLength(100);
        RuleFor(x => x.Status).IsInEnum();
    }
}

public class OrderValidator : AbstractValidator<Order>
{
    public OrderValidator()
    {
        RuleFor(x => x.PetId).GreaterThan(0);
        RuleFor(x => x.Quantity).InclusiveBetween(1, 100);
    }
}
```

---

### Why RECOMMENDED (not Required)

**Rationale** (from FR-008):
- Validators are in **different assembly** than Program.cs (cross-assembly registration)
- Without this method, consumers need verbose syntax:
  ```csharp
  services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly);
  ```
- Provides clean API and encapsulates assembly scanning logic

**Alternative approaches** (all acceptable):
1. **Manual registration** (works but verbose):
   ```csharp
   services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly);
   ```
2. **Use DTO type instead of validator**:
   ```csharp
   services.AddValidatorsFromAssembly(typeof(Pet).Assembly);
   ```

**Why not REQUIRED**:
- Developers familiar with FluentValidation can use standard registration
- Not all APIs enable validators (`useValidators=false`)
- FluentValidation already provides good extension methods

---

## Extension Method 3: AddApiHandlers()

### Signature
```csharp
namespace {{packageName}}.Extensions;

public static class HandlerExtensions
{
    /// <summary>
    /// Registers all MediatR handlers from the Implementation assembly.
    /// OPTIONAL: Developers can use standard MediatR registration instead.
    /// </summary>
    /// <param name="services">The service collection.</param>
    /// <returns>The service collection for chaining.</returns>
    public static IServiceCollection AddApiHandlers(this IServiceCollection services)
}
```

---

### Behavior

**Purpose** (FR-009): Provides API consistency by wrapping MediatR's standard registration.

**What it does**:
1. Calls `AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))`
2. Auto-discovers all `IRequestHandler<,>` implementations in Implementation.dll
3. Registers each handler with `ServiceLifetime.Transient` (MediatR default)
4. Returns `IServiceCollection` for fluent chaining

**Related FRs**: FR-009 (Optional extension), FR-021 (Auto-register handlers)  
**Related RQs**: RQ-003 (MediatR assembly scanning)

---

### Assembly Location

**Package**: `PetstoreApi.dll` (Implementation project, NOT in NuGet package)  
**Namespace**: `PetstoreApi.Extensions`  
**File**: `Extensions/HandlerExtensions.cs` (generated in Implementation project)

**Why in Implementation** (from FR-022):
- Handlers are in Implementation.dll (same assembly as Program.cs)
- Can use `typeof(Program).Assembly` without cross-assembly reference
- Not distributed in NuGet package (internal to application)

---

### Usage Example (Option 1: Extension Method)

```csharp
// Program.cs in Implementation project
using PetstoreApi.Extensions;

var builder = WebApplication.CreateBuilder(args);

// Use convenience extension method (cleaner)
builder.Services.AddApiHandlers();

// Register validators and endpoints from Contracts
builder.Services.AddApiValidators();

var app = builder.Build();
app.AddApiEndpoints();
app.Run();
```

---

### Usage Example (Option 2: Standard MediatR - RECOMMENDED)

```csharp
// Program.cs in Implementation project
var builder = WebApplication.CreateBuilder(args);

// Use standard MediatR registration (explicit, standard)
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

// Register validators and endpoints from Contracts
builder.Services.AddApiValidators();

var app = builder.Build();
app.AddApiEndpoints();
app.Run();
```

**Result**: Identical behavior, Option 2 is more explicit and uses standard MediatR patterns.

---

### Implementation Strategy

**Generated code structure** (HandlerExtensions.cs):
```csharp
using MediatR;
using Microsoft.Extensions.DependencyInjection;

namespace {{packageName}}.Extensions;

public static class HandlerExtensions
{
    public static IServiceCollection AddApiHandlers(this IServiceCollection services)
    {
        // Register MediatR with handler discovery from this assembly
        services.AddMediatR(cfg => 
            cfg.RegisterServicesFromAssembly(typeof(HandlerExtensions).Assembly));

        return services;
    }
}
```

**Key techniques**:
- Wraps MediatR's `AddMediatR()` for API consistency
- Uses `typeof(HandlerExtensions).Assembly` to get Implementation.dll
- Alternatively: `typeof(Program).Assembly` (both are same assembly)

**How MediatR scanning works** (from RQ-003):
```csharp
// Pseudocode of MediatR internals
foreach (var type in assembly.GetExportedTypes())
{
    foreach (var iface in type.GetInterfaces())
    {
        if (iface is IRequestHandler<TRequest, TResponse>)
        {
            services.AddTransient(iface, type);
        }
    }
}
```

**Example handlers** (in Implementation project):
```csharp
namespace PetstoreApi.Handlers;

public class AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>
{
    private readonly IDbContext _db;
    
    public AddPetCommandHandler(IDbContext db) => _db = db;
    
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken ct)
    {
        var pet = new Pet { Id = request.Id, Name = request.Name };
        await _db.Pets.AddAsync(pet, ct);
        return pet;
    }
}
```

---

### Why OPTIONAL (not Required or Recommended)

**Rationale** (from FR-009):
- Handlers are in **same assembly** as Program.cs (no cross-assembly complexity)
- MediatR's standard registration is well-known: `AddMediatR(cfg => cfg.RegisterServicesFromAssembly(...))`
- Extension method provides API consistency but no functional benefit
- Most .NET developers prefer explicit MediatR registration (clearer intent)

**When to use**:
- Prefer consistency with `AddApiEndpoints()` / `AddApiValidators()` naming
- Hide MediatR implementation detail from consuming code
- Team convention prefers `Add*()` pattern

**When NOT to use**:
- Team familiar with MediatR (use standard registration)
- Prefer explicit dependencies (clearer what's happening)
- Following .NET convention (use framework-provided methods)

---

## Assembly Scanning Details

### Cross-Assembly Scenario (FR-022)

Feature 008 splits components across two assemblies:

| Component | Assembly | Registration Method | Why Special Handling? |
|-----------|----------|-------------------|----------------------|
| **Endpoints** | Contracts.dll | `AddApiEndpoints()` | No equivalent in ASP.NET Core, custom extension REQUIRED |
| **Validators** | Contracts.dll | `AddApiValidators()` | Different assembly than Program.cs, cross-assembly reference RECOMMENDED |
| **Handlers** | Implementation.dll | `AddApiHandlers()` or standard MediatR | Same assembly as Program.cs, standard registration works OPTIONAL |

---

### Validators: Why Cross-Assembly Registration Matters (RQ-004)

**Problem**: Validators are in Contracts.dll, but registration happens in Program.cs (Implementation.dll).

**Solution 1: Extension method in Contracts** (RECOMMENDED):
```csharp
// In Contracts.dll
public static IServiceCollection AddApiValidators(this IServiceCollection services)
{
    services.AddValidatorsFromAssembly(Assembly.GetExecutingAssembly()); // Contracts.dll
    return services;
}

// In Program.cs (Implementation.dll)
builder.Services.AddApiValidators(); // Clean, no cross-assembly type reference
```

**Solution 2: Manual registration with typeof** (ACCEPTABLE):
```csharp
// In Program.cs (Implementation.dll)
builder.Services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly); // Cross-assembly reference
```

**Why Solution 1 is better**:
- No need for consuming code to know about validator types
- Encapsulates assembly scanning logic in Contracts package
- Cleaner API surface

---

### Handlers: Why Same-Assembly Registration is Simple (RQ-003)

**Problem**: Handlers are in Implementation.dll, same assembly as Program.cs.

**Solution 1: Standard MediatR** (RECOMMENDED):
```csharp
// In Program.cs (Implementation.dll)
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly)); // Same assembly
```

**Solution 2: Extension method** (OPTIONAL):
```csharp
// In HandlerExtensions.cs (Implementation.dll)
public static IServiceCollection AddApiHandlers(this IServiceCollection services)
{
    services.AddMediatR(cfg => 
        cfg.RegisterServicesFromAssembly(typeof(HandlerExtensions).Assembly));
    return services;
}

// In Program.cs
builder.Services.AddApiHandlers();
```

**Why Solution 1 is simpler**:
- No extra extension method needed
- Uses well-known MediatR pattern
- Clearer intent (explicitly showing MediatR registration)

---

### Supporting Custom Handlers (FR-021)

**Feature 008 design goal**: Users can add custom handlers (not from OpenAPI) without regeneration.

**Example scenario**: Developer adds custom business logic handler:
```csharp
// Custom handler (user-added, not generated)
public class SendEmailCommandHandler : IRequestHandler<SendEmailCommand, bool>
{
    public async Task<bool> Handle(SendEmailCommand request, CancellationToken ct)
    {
        // Custom business logic
        await EmailService.SendAsync(request.To, request.Subject, request.Body);
        return true;
    }
}
```

**How it works** (from FR-021):
- MediatR's `RegisterServicesFromAssembly()` scans for ALL `IRequestHandler<,>` implementations
- Finds both generated handlers (AddPetCommandHandler) and custom handlers (SendEmailCommandHandler)
- Registers both automatically without code regeneration
- No distinction between "generated" and "custom" handlers at runtime

**Benefits**:
- Extend API with custom operations without touching generator
- Mix generated and hand-written code seamlessly
- No maintenance burden when adding new handlers

---

## Program.cs Complete Example

### Full Working Example (All Features Enabled)

```csharp
using PetstoreApi.Contracts.Extensions; // AddApiEndpoints, AddApiValidators
using MediatR;
using FluentValidation;

var builder = WebApplication.CreateBuilder(args);

// ============ SERVICE REGISTRATION ============

// 1. Register MediatR handlers from THIS assembly (Implementation.dll)
//    Discovers all IRequestHandler<,> implementations in Handlers/ directory
//    (Standard MediatR registration - RECOMMENDED over AddApiHandlers)
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

// 2. Register FluentValidation validators from Contracts assembly
//    Uses extension method from Contracts package to avoid cross-assembly typeof()
//    (When useValidators=true)
builder.Services.AddApiValidators();

// 3. Register custom user services (domain-specific)
builder.Services.AddSingleton<IDbContext, InMemoryDbContext>();
builder.Services.AddScoped<IEmailService, SendGridEmailService>();
builder.Services.AddMemoryCache();

// 4. Add standard ASP.NET Core services
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// ============ MIDDLEWARE PIPELINE ============

var app = builder.Build();

// Configure Swagger (development only)
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Apply standard middleware
app.UseHttpsRedirection();

// 5. Register all API endpoints from Contracts package
//    This is the ONLY way to register endpoints - no ASP.NET Core equivalent
app.AddApiEndpoints();

// Start the application
app.Run();
```

---

### Minimal Example (No Validators)

```csharp
using PetstoreApi.Contracts.Extensions;
using MediatR;

var builder = WebApplication.CreateBuilder(args);

// Register handlers from this assembly
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

var app = builder.Build();

// Register endpoints from Contracts package
app.AddApiEndpoints();

app.Run();
```

---

### Alternative: Using AddApiHandlers (Optional Style)

```csharp
using PetstoreApi.Contracts.Extensions;
using PetstoreApi.Extensions; // For AddApiHandlers

var builder = WebApplication.CreateBuilder(args);

// Use extension methods for consistency (all Add* pattern)
builder.Services.AddApiHandlers();    // MediatR (Implementation.dll)
builder.Services.AddApiValidators();  // FluentValidation (Contracts.dll)

var app = builder.Build();

app.AddApiEndpoints(); // Endpoints (Contracts.dll)

app.Run();
```

**Team preference**: Choose one style and use consistently across projects.

---

## Method Comparison Summary

| Method | Assembly | Required? | Rationale | Alternatives |
|--------|----------|-----------|-----------|-------------|
| **AddApiEndpoints()** | Contracts.dll | ✅ REQUIRED | No ASP.NET Core equivalent, endpoints are code not types | None practical |
| **AddApiValidators()** | Contracts.dll | ⭐ RECOMMENDED | Cross-assembly registration, cleaner than typeof() | `services.AddValidatorsFromAssembly(typeof(Validator).Assembly)` |
| **AddApiHandlers()** | Implementation.dll | ❓ OPTIONAL | Same-assembly registration, standard MediatR works fine | `services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))` |

---

## Testing Checklist

**Extension method generation**:
- [ ] `AddApiEndpoints()` generated in Contracts/Extensions/EndpointExtensions.cs
- [ ] `AddApiValidators()` generated when `useValidators=true`
- [ ] `AddApiHandlers()` generated in Implementation/Extensions/HandlerExtensions.cs (optional)

**Runtime behavior**:
- [ ] `AddApiEndpoints()` registers all endpoints (verify via Swagger UI)
- [ ] `AddApiValidators()` registers validators (verify via DI resolution)
- [ ] `AddApiHandlers()` registers handlers (verify MediatR sends work)

**Error handling**:
- [ ] Calling `AddApiEndpoints()` before service registration fails gracefully
- [ ] Missing handler for request returns clear error (MediatR exception)
- [ ] Missing validator does not block execution (FluentValidation optional)

---

## Related Documentation

- [spec.md](../spec.md) - FR-007 (AddApiEndpoints), FR-008 (AddApiValidators), FR-009 (AddApiHandlers)
- [research.md](../research.md) - RQ-003 (MediatR scanning), RQ-004 (FluentValidation scanning)
- [data-model.md](../data-model.md) - Extension Methods section
- [CLI-Options.md](./CLI-Options.md) - Generator CLI options
- [CsprojStructure.md](./CsprojStructure.md) - Assembly structure and references
