# Quickstart: NuGet API Contract Packaging

**Branch**: `008-nuget-api-contracts` | **Date**: 2026-01-27 | **Spec**: [spec.md](spec.md)

## Prerequisites

### Required Tools
- **devbox**: Development environment manager (project uses isolated toolchain)
- **.NET SDK 8.0+**: For building and packaging generated code
- **Maven 3.8.9+**: For building the OpenAPI generator (managed via devbox)
- **Java 11+**: For running the OpenAPI generator (managed via devbox)
- **Git**: For version control

### Verify Installation

```bash
# Check devbox is available
which devbox
# Expected: /usr/local/bin/devbox or similar

# Check .NET SDK (via devbox)
cd /Users/adam/scratch/git/minimal-api-gen
devbox run dotnet --version
# Expected: 8.0.x or higher

# Check Java (via devbox)
devbox run java -version
# Expected: Java 11 or higher
```

### Project Structure

```text
minimal-api-gen/
  generator/                  # OpenAPI generator source code
  test-output/               # Generated code output
  petstore-tests/            # Integration tests
  specs/008-nuget-api-contracts/  # This feature
```

## Workflow Overview

Feature 008 enables independent versioning of API contracts by generating two separate projects:

1. **Contracts Project** (`PetstoreApi.Contracts.csproj`): NuGet package containing Endpoints, DTOs, Validators
2. **Implementation Project** (`PetstoreApi.csproj`): Business logic containing Handlers, Models, Program.cs

**Key Benefit**: Update API surface independently from business logic implementations.

---

## Step 1: Build the Generator

Build the custom OpenAPI generator with NuGet packaging support:

```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run task build-generator
```

**Expected Output**:
```
[INFO] Building minimal-api-gen 1.0.0-SNAPSHOT
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

**Validation**:
- [ ] `generator/target/minimal-api-gen-openapi-generator-1.0.0-SNAPSHOT.jar` exists

---

## Step 2: Generate Code with NuGet Packaging

Generate Petstore API code with NuGet packaging enabled:

```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore,packageVersion=1.0.0,packageAuthors=Platform Team"
```

**CLI Options Explained**:
- `useNugetPackaging=true`: Enables dual-project generation (Contracts + Implementation)
- `packageId=MyCompany.Petstore`: NuGet package identifier (consumers use `dotnet add package MyCompany.Petstore`)
- `packageVersion=1.0.0`: Semantic version for NuGet feed
- `packageAuthors=Platform Team`: Author metadata visible in NuGet

**Expected Output**:
```
[INFO] Successfully generated code to /Users/adam/scratch/git/minimal-api-gen/test-output
```

---

## Step 3: Verify Generated Structure

Check that both projects were created with correct structure:

```bash
cd /Users/adam/scratch/git/minimal-api-gen
ls -la test-output/src/
```

**Expected Structure**:
```
test-output/
  PetstoreApi.sln                          # Solution file
  Generated/                               # Shared generated code
    Models/                                # DTOs (Pet, Order, User)
    Commands/                              # MediatR commands
    Queries/                               # MediatR queries
    Endpoints/                             # Endpoint definitions
    Validators/                            # FluentValidation validators
  src/
    PetstoreApi.Contracts/                 # NuGet package project
      PetstoreApi.Contracts.csproj         # Package definition
      Extensions/
        EndpointExtensions.cs              # AddApiEndpoints() method
        ValidatorExtensions.cs             # AddApiValidators() method
    PetstoreApi/                           # Implementation project
      PetstoreApi.csproj                   # Application definition
      Program.cs                           # Entry point
      Handlers/                            # Empty (user-owned)
      Models/                              # Empty (user-owned)
```

**Validation**:
- [ ] `test-output/src/PetstoreApi.Contracts/PetstoreApi.Contracts.csproj` exists
- [ ] `test-output/src/PetstoreApi/PetstoreApi.csproj` exists
- [ ] `test-output/Generated/` contains Models/, Commands/, Queries/, Endpoints/

---

## Step 4: Build Contracts Project

Build the NuGet package project to verify generated code compiles:

```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run dotnet build test-output/src/PetstoreApi.Contracts/ --verbosity minimal
```

**Expected Output**:
```
  PetstoreApi.Contracts -> /Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi.Contracts/bin/Debug/net8.0/PetstoreApi.Contracts.dll
Build succeeded.
    0 Warning(s)
    0 Error(s)
```

**Validation**:
- [ ] Build completes with 0 errors
- [ ] `test-output/src/PetstoreApi.Contracts/bin/Debug/net8.0/PetstoreApi.Contracts.dll` exists

---

## Step 5: Build Implementation Project

Build the implementation project (requires handlers first):

```bash
cd /Users/adam/scratch/git/minimal-api-gen

# Copy test handlers from petstore-tests
devbox run task copy-test-stubs

# Build implementation project
devbox run dotnet build test-output/src/PetstoreApi/ --verbosity minimal
```

**Expected Output**:
```
  PetstoreApi -> /Users/adam/scratch/git/minimal-api-gen/test-output/src/PetstoreApi/bin/Debug/net8.0/PetstoreApi.dll
Build succeeded.
    0 Warning(s)
    0 Error(s)
```

**Validation**:
- [ ] Build completes with 0 errors
- [ ] `test-output/src/PetstoreApi/bin/Debug/net8.0/PetstoreApi.dll` exists
- [ ] `test-output/src/PetstoreApi/Handlers/` contains handler implementations

---

## Step 6: Pack NuGet Package

Create the distributable NuGet package from the Contracts project:

```bash
cd /Users/adam/scratch/git/minimal-api-gen
mkdir -p packages
devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ \
  --configuration Release \
  --output ./packages/
```

**Expected Output**:
```
Successfully created package '/Users/adam/scratch/git/minimal-api-gen/packages/MyCompany.Petstore.1.0.0.nupkg'.
```

**Validation**:
- [ ] `packages/MyCompany.Petstore.1.0.0.nupkg` exists
- [ ] Package size is under 500KB (typical for 20 operations)

---

## Step 7: Verify Package Contents

Inspect the NuGet package to ensure correct contents:

```bash
cd /Users/adam/scratch/git/minimal-api-gen

# Extract package to temporary location
unzip -l packages/MyCompany.Petstore.1.0.0.nupkg
```

**Expected Contents**:
```
Archive:  MyCompany.Petstore.1.0.0.nupkg
  Length      Name
---------  ----
     xxxx  lib/net8.0/PetstoreApi.Contracts.dll
     xxxx  MyCompany.Petstore.nuspec
     xxxx  [Content_Types].xml
```

**Validation**:
- [ ] Package contains `lib/net8.0/PetstoreApi.Contracts.dll`
- [ ] Package does NOT contain Handlers/, Models/, or Program.cs
- [ ] `.nuspec` metadata includes correct packageId, version, authors

---

## Step 8: Create Test Consumer Application

Create a new ASP.NET Core application to consume the NuGet package:

```bash
cd /Users/adam/scratch/git/minimal-api-gen
mkdir -p test-consumer
cd test-consumer
devbox run dotnet new web -n ConsumerApp -f net8.0
cd ConsumerApp
```

**Expected Output**:
```
The template "ASP.NET Core Empty" was created successfully.
```

**Validation**:
- [ ] `test-consumer/ConsumerApp/ConsumerApp.csproj` exists
- [ ] `test-consumer/ConsumerApp/Program.cs` exists

---

## Step 9: Add Local NuGet Package Reference

Add the generated NuGet package to the consumer application:

```bash
cd /Users/adam/scratch/git/minimal-api-gen/test-consumer/ConsumerApp

# Add local package source
devbox run dotnet add package MyCompany.Petstore \
  --version 1.0.0 \
  --source ../../packages/
```

**Expected Output**:
```
info : Adding PackageReference for package 'MyCompany.Petstore' into project '/Users/adam/scratch/git/minimal-api-gen/test-consumer/ConsumerApp/ConsumerApp.csproj'.
info : Package 'MyCompany.Petstore' is compatible with all the specified frameworks in project '/Users/adam/scratch/git/minimal-api-gen/test-consumer/ConsumerApp/ConsumerApp.csproj'.
info : PackageReference for package 'MyCompany.Petstore' version '1.0.0' added to file '/Users/adam/scratch/git/minimal-api-gen/test-consumer/ConsumerApp/ConsumerApp.csproj'.
```

**Verification**:
```bash
# Check PackageReference was added
cat ConsumerApp.csproj | grep MyCompany.Petstore
```

**Expected Line**:
```xml
<PackageReference Include="MyCompany.Petstore" Version="1.0.0" />
```

**Validation**:
- [ ] `ConsumerApp.csproj` contains PackageReference to MyCompany.Petstore
- [ ] `dotnet restore` completes successfully

---

## Step 10: Register Services in Program.cs

Configure the consumer application to use packaged endpoints and validators:

```bash
cd /Users/adam/scratch/git/minimal-api-gen/test-consumer/ConsumerApp
```

Edit `Program.cs`:

```csharp
using MyCompany.Petstore.Contracts.Extensions;  // From NuGet package
using MediatR;

var builder = WebApplication.CreateBuilder(args);

// Register MediatR for handling Commands/Queries
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

// Register validators from NuGet package (RECOMMENDED)
builder.Services.AddApiValidators();

// Add custom services (e.g., database, cache, repositories)
// builder.Services.AddSingleton<IDbContext, InMemoryDbContext>();

var app = builder.Build();

// Register all API endpoints from NuGet package (REQUIRED)
app.AddApiEndpoints();

app.Run();
```

**Key Points**:
- `AddApiValidators()`: Registers FluentValidation validators from Contracts.dll
- `AddApiEndpoints()`: Registers all HTTP endpoints (GET /pet/{id}, POST /pet, etc.)
- `AddMediatR()`: Discovers handler implementations from consumer assembly

**Validation**:
- [ ] Program.cs imports `MyCompany.Petstore.Contracts.Extensions`
- [ ] Program.cs calls `AddApiValidators()` before `AddApiEndpoints()`
- [ ] MediatR is registered before building the app

---

## Step 11: Implement Handlers

Create handler implementations for MediatR commands and queries:

```bash
cd /Users/adam/scratch/git/minimal-api-gen/test-consumer/ConsumerApp
mkdir Handlers
```

Create `Handlers/AddPetHandler.cs`:

```csharp
using MediatR;
using MyCompany.Petstore.Contracts.Commands;  // From NuGet package
using MyCompany.Petstore.Contracts.Models;     // From NuGet package

namespace ConsumerApp.Handlers;

public class AddPetHandler : IRequestHandler<AddPetCommand, PetDto>
{
    private readonly ILogger<AddPetHandler> _logger;

    public AddPetHandler(ILogger<AddPetHandler> logger)
    {
        _logger = logger;
    }

    public Task<PetDto> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("Adding pet: {Name}", request.Name);
        
        // Custom business logic here
        var pet = new PetDto 
        { 
            Id = new Random().Next(1000, 9999),
            Name = request.Name,
            Status = request.Status
        };
        
        return Task.FromResult(pet);
    }
}
```

Create `Handlers/GetPetByIdHandler.cs`:

```csharp
using MediatR;
using MyCompany.Petstore.Contracts.Queries;   // From NuGet package
using MyCompany.Petstore.Contracts.Models;     // From NuGet package

namespace ConsumerApp.Handlers;

public class GetPetByIdHandler : IRequestHandler<GetPetByIdQuery, PetDto?>
{
    private readonly ILogger<GetPetByIdHandler> _logger;

    public GetPetByIdHandler(ILogger<GetPetByIdHandler> logger)
    {
        _logger = logger;
    }

    public Task<PetDto?> Handle(GetPetByIdQuery request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("Getting pet by ID: {PetId}", request.PetId);
        
        // Custom business logic (database lookup, caching, etc.)
        var pet = new PetDto 
        { 
            Id = request.PetId,
            Name = "Sample Pet",
            Status = "available"
        };
        
        return Task.FromResult<PetDto?>(pet);
    }
}
```

**Key Points**:
- Handlers implement `IRequestHandler<TRequest, TResponse>` from MediatR
- Commands/Queries (`AddPetCommand`, `GetPetByIdQuery`) come from NuGet package
- DTOs (`PetDto`) come from NuGet package
- Business logic is fully custom (database, caching, validation, etc.)

**Validation**:
- [ ] Handler classes implement correct `IRequestHandler<T, R>` interface
- [ ] Handlers import types from `MyCompany.Petstore.Contracts` namespace
- [ ] Handler constructors request services via dependency injection

---

## Step 12: Run Consumer Application

Build and run the consumer application:

```bash
cd /Users/adam/scratch/git/minimal-api-gen/test-consumer/ConsumerApp
devbox run dotnet run
```

**Expected Output**:
```
info: Microsoft.Hosting.Lifetime[14]
      Now listening on: http://localhost:5000
info: Microsoft.Hosting.Lifetime[0]
      Application started. Press Ctrl+C to shut down.
```

**Test Endpoints**:

```bash
# In another terminal
cd /Users/adam/scratch/git/minimal-api-gen

# Create a pet
curl -X POST http://localhost:5000/pet \
  -H "Content-Type: application/json" \
  -d '{"name": "Fluffy", "status": "available"}'

# Expected: {"id": 1234, "name": "Fluffy", "status": "available"}

# Get pet by ID
curl http://localhost:5000/pet/123

# Expected: {"id": 123, "name": "Sample Pet", "status": "available"}
```

**Validation**:
- [ ] Application starts without errors
- [ ] POST /pet endpoint returns created pet with generated ID
- [ ] GET /pet/{id} endpoint returns pet data
- [ ] Handlers are successfully invoked via MediatR

---

## Step 13: Publish to NuGet Feed (Optional)

Publish the NuGet package to a public or private feed:

### Public NuGet.org

```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run dotnet nuget push packages/MyCompany.Petstore.1.0.0.nupkg \
  --source https://api.nuget.org/v3/index.json \
  --api-key YOUR_API_KEY
```

### Private Azure Artifacts Feed

```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run dotnet nuget push packages/MyCompany.Petstore.1.0.0.nupkg \
  --source https://pkgs.dev.azure.com/YOUR_ORG/_packaging/YOUR_FEED/nuget/v3/index.json \
  --api-key YOUR_PAT
```

### Private GitHub Packages

```bash
cd /Users/adam/scratch/git/minimal-api-gen
devbox run dotnet nuget push packages/MyCompany.Petstore.1.0.0.nupkg \
  --source https://nuget.pkg.github.com/YOUR_USERNAME/index.json \
  --api-key YOUR_GITHUB_TOKEN
```

**Expected Output**:
```
Pushing MyCompany.Petstore.1.0.0.nupkg to 'https://api.nuget.org/v3/index.json'...
  PUT https://api.nuget.org/v3/index.json
  OK https://api.nuget.org/v3/index.json 2134ms
Your package was pushed.
```

**Validation**:
- [ ] Package appears in NuGet feed
- [ ] Package metadata (version, authors, description) is visible
- [ ] Other developers can install via `dotnet add package MyCompany.Petstore`

---

## Step 14: Update Package Version

Regenerate with a new version after API changes:

### Backward-Compatible Change (Minor Version)

**Scenario**: Added optional `category` field to `Pet` DTO in OpenAPI spec

```bash
cd /Users/adam/scratch/git/minimal-api-gen

# Regenerate with new version
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore,packageVersion=1.1.0,packageAuthors=Platform Team"

# Build and pack new version
devbox run dotnet build test-output/src/PetstoreApi.Contracts/
devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ \
  --configuration Release \
  --output ./packages/
```

**Expected Behavior**:
- Consumer applications using v1.0.0 handlers continue to work without changes
- New field (`category`) is optional, so existing handlers don't need updates
- Consumers can upgrade: `dotnet add package MyCompany.Petstore --version 1.1.0`

### Breaking Change (Major Version)

**Scenario**: Renamed `name` to `petName` in `Pet` DTO (breaking change)

```bash
cd /Users/adam/scratch/git/minimal-api-gen

# Regenerate with major version bump
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore,packageVersion=2.0.0,packageAuthors=Platform Team"

# Build and pack new version
devbox run dotnet build test-output/src/PetstoreApi.Contracts/
devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ \
  --configuration Release \
  --output ./packages/
```

**Expected Behavior**:
- Consumer applications upgrading to v2.0.0 will have **compilation errors**
- Handlers referencing `request.Name` will fail (property no longer exists)
- Developers must update handlers to use `request.PetName`
- This is **intentional** - forces explicit handling of breaking changes

**Validation**:
- [ ] Minor version updates don't break existing handlers
- [ ] Major version updates cause compilation errors for breaking changes
- [ ] Multiple package versions can coexist in NuGet feed

---

## Step 15: Advanced - Symbol Package for Debugging

Enable debugging into packaged code with symbol packages:

```bash
cd /Users/adam/scratch/git/minimal-api-gen

# Regenerate with symbols enabled
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore,packageVersion=1.0.0,includeSymbols=true"

# Pack with symbol package
devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ \
  --configuration Release \
  --output ./packages/ \
  --include-symbols \
  --include-source
```

**Expected Output**:
```
Successfully created package '/Users/adam/scratch/git/minimal-api-gen/packages/MyCompany.Petstore.1.0.0.nupkg'.
Successfully created package '/Users/adam/scratch/git/minimal-api-gen/packages/MyCompany.Petstore.1.0.0.snupkg'.
```

**Publish Both Packages**:

```bash
# Publish main package
devbox run dotnet nuget push packages/MyCompany.Petstore.1.0.0.nupkg \
  --source https://api.nuget.org/v3/index.json \
  --api-key YOUR_API_KEY

# Publish symbol package
devbox run dotnet nuget push packages/MyCompany.Petstore.1.0.0.snupkg \
  --source https://api.nuget.org/v3/index.json \
  --api-key YOUR_API_KEY
```

**Debugging in Visual Studio/Rider**:
1. Enable "Enable Source Link support" in debugger settings
2. Set breakpoint in consumer handler
3. Step into endpoint call (F11)
4. Debugger downloads symbols and shows original Endpoint source code

**Validation**:
- [ ] `.snupkg` file created alongside `.nupkg`
- [ ] Visual Studio can step into Endpoint code during debugging
- [ ] Source code displayed in debugger matches generated templates

---

## Troubleshooting

### Build Errors: .NET SDK Version

**Symptom**: `error NETSDK1045: The current .NET SDK does not support targeting .NET 8.0.`

**Solution**:
```bash
# Check .NET version
devbox run dotnet --version
# Must be 8.0.0 or higher

# If outdated, update devbox.json and reinstall
cat devbox.json | grep dotnet
devbox install
```

### Pack Errors: Missing Package Metadata

**Symptom**: `warning NU5125: The 'licenseUrl' element will be deprecated. Consider using the 'license' element instead.`

**Solution**: Metadata warnings are informational only. Package still builds successfully. To suppress:

```bash
# Regenerate with complete metadata
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore,packageVersion=1.0.0,packageAuthors=Platform Team,packageDescription=Petstore API Contracts"
```

### Runtime Error: Assembly Not Found

**Symptom**: `System.IO.FileNotFoundException: Could not load file or assembly 'PetstoreApi.Contracts, Version=1.0.0.0'`

**Solution**: Verify package reference and restore:

```bash
cd test-consumer/ConsumerApp

# Check package is referenced
cat ConsumerApp.csproj | grep MyCompany.Petstore

# Restore packages
devbox run dotnet restore

# Verify package installed
ls ~/.nuget/packages/mycompany.petstore/
```

### Runtime Error: Validators Not Registered

**Symptom**: Validation fails silently or throws `NullReferenceException` on validator invocation

**Solution**: Ensure `AddApiValidators()` is called in Program.cs:

```csharp
// CORRECT order:
builder.Services.AddApiValidators();        // Register validators FIRST
builder.Services.AddMediatR(cfg => ...);    // Register MediatR second

var app = builder.Build();
app.AddApiEndpoints();                      // Register endpoints last
```

### Runtime Error: Handler Not Found

**Symptom**: `System.InvalidOperationException: Handler was not found for request of type MyCompany.Petstore.Contracts.Commands.AddPetCommand`

**Root Cause**: MediatR cannot find handler implementation

**Solutions**:

1. **Verify handler implements correct interface**:
```csharp
// CORRECT:
public class AddPetHandler : IRequestHandler<AddPetCommand, PetDto>

// WRONG (generic type mismatch):
public class AddPetHandler : IRequestHandler<AddPetCommand, Pet>
```

2. **Verify MediatR assembly scanning**:
```csharp
// Ensure handlers are in same assembly as Program.cs
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
```

3. **Verify handler is in correct namespace**:
```bash
# Check handler exists
ls ConsumerApp/Handlers/AddPetHandler.cs

# Verify namespace matches
grep "namespace" ConsumerApp/Handlers/AddPetHandler.cs
# Expected: namespace ConsumerApp.Handlers;
```

### Build Warning: ProjectReference vs PackageReference

**Symptom**: `warning NU1608: Detected package version outside of dependency constraint`

**Context**: During local development, Implementation project uses `<ProjectReference>` to Contracts project. When published, consumers use `<PackageReference>`.

**Solution**: This is expected behavior. To test with PackageReference locally:

1. Comment out ProjectReference in `PetstoreApi.csproj`:
```xml
<!-- <ProjectReference Include="../PetstoreApi.Contracts/PetstoreApi.Contracts.csproj" /> -->
<PackageReference Include="MyCompany.Petstore" Version="1.0.0" />
```

2. Add local package source:
```bash
devbox run dotnet nuget add source ./packages/ --name local-packages
```

3. Rebuild:
```bash
devbox run dotnet restore test-output/src/PetstoreApi/
devbox run dotnet build test-output/src/PetstoreApi/
```

### CLI Error: Exit Code 127 (Command Not Found)

**Symptom**: `-bash: mvn: command not found` or `-bash: dotnet: command not found`

**Root Cause**: Tools are not in global PATH - they're isolated in devbox environment

**Solution**: **ALWAYS** use `devbox run` prefix:

```bash
# WRONG:
mvn clean package                    # ❌ mvn not in PATH
dotnet build test-output/            # ❌ dotnet not in PATH
task build-generator                 # ❌ task not in PATH

# CORRECT:
devbox run task build-generator      # ✅ Use task runner via devbox
devbox run dotnet build test-output/ # ✅ Use dotnet via devbox
```

---

## Next Steps

### Integration with CI/CD

Add NuGet packaging to your build pipeline:

```yaml
# GitHub Actions example
- name: Build Generator
  run: devbox run task build-generator

- name: Generate Code
  run: devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true,packageVersion=${{ github.ref_name }}"

- name: Pack NuGet Package
  run: devbox run dotnet pack test-output/src/PetstoreApi.Contracts/ --configuration Release --output ./packages/

- name: Publish to NuGet
  run: devbox run dotnet nuget push packages/*.nupkg --source ${{ secrets.NUGET_SOURCE }} --api-key ${{ secrets.NUGET_API_KEY }}
```

### Multi-Environment Configuration

Use different package IDs for environments:

```bash
# Development
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore.Dev,packageVersion=1.0.0-dev"

# Staging
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore.Staging,packageVersion=1.0.0-rc"

# Production
devbox run task generate-petstore-minimal-api \
  ADDITIONAL_PROPS="useNugetPackaging=true,packageId=MyCompany.Petstore,packageVersion=1.0.0"
```

### Add Custom Handlers

Extend generated code with custom business logic:

```csharp
// ConsumerApp/Handlers/CustomReportHandler.cs
using MediatR;

namespace ConsumerApp.Handlers;

// Custom command not in OpenAPI spec
public record GenerateReportCommand : IRequest<byte[]>
{
    public DateTime StartDate { get; init; }
    public DateTime EndDate { get; init; }
}

// Custom handler auto-discovered by MediatR
public class GenerateReportHandler : IRequestHandler<GenerateReportCommand, byte[]>
{
    public Task<byte[]> Handle(GenerateReportCommand request, CancellationToken ct)
    {
        // Custom business logic
        return Task.FromResult(Array.Empty<byte>());
    }
}
```

MediatR's `RegisterServicesFromAssembly()` automatically discovers both generated and custom handlers without requiring code regeneration.

### Migrate Existing Projects

Upgrade from monolithic project to NuGet packaging:

1. **Backup existing handlers**:
```bash
cp -r test-output/Handlers/ ~/backup-handlers/
```

2. **Regenerate with packaging**:
```bash
devbox run task generate-petstore-minimal-api ADDITIONAL_PROPS="useNugetPackaging=true"
```

3. **Restore handlers**:
```bash
cp -r ~/backup-handlers/* test-output/src/PetstoreApi/Handlers/
```

4. **Update Program.cs imports**:
```csharp
// OLD: using PetstoreApi.Endpoints;
// NEW: using MyCompany.Petstore.Contracts.Extensions;
```

5. **Build and test**:
```bash
devbox run dotnet build test-output/
devbox run task test-server-stubs
```

---

## Summary

You've successfully:

✅ Generated code with NuGet packaging enabled  
✅ Built separate Contracts and Implementation projects  
✅ Packed a distributable NuGet package  
✅ Consumed the package in a test application  
✅ Implemented custom handlers for business logic  
✅ Published to NuGet feed (optional)  
✅ Updated package versions for API evolution  

**Key Takeaways**:
- **Contracts Project** = API surface (Endpoints, DTOs, Validators) → Versioned independently
- **Implementation Project** = Business logic (Handlers, Models) → Decoupled from API changes
- **Extension Methods** = Clean integration points (`AddApiEndpoints()`, `AddApiValidators()`)
- **SemVer Versioning** = Minor versions for backward-compatible changes, major versions for breaking changes

**Documentation**:
- [spec.md](spec.md) - Complete feature requirements
- [contracts/CLI-Options.md](contracts/CLI-Options.md) - All generator options
- [contracts/CsprojStructure.md](contracts/CsprojStructure.md) - Project file structure
- [contracts/ExtensionMethods.md](contracts/ExtensionMethods.md) - Extension method API

**Get Help**:
- Check [Troubleshooting](#troubleshooting) section above
- Review integration tests: `petstore-tests/PetstoreApi.Tests/`
- Examine generated code: `test-output/src/`
