# Data Model: NuGet API Contract Packaging

**Branch**: `008-nuget-api-contracts` | **Date**: 2026-01-27 | **Spec**: [spec.md](spec.md)

## Core Entities

### Entity 1: NuGet Package Project

**Purpose**: Separate `.csproj` file that packages API contracts (Endpoints, DTOs, Validators) for distribution via NuGet feeds. Enables independent versioning of the API surface layer without coupling to implementation details.

**Fields**:
- `projectPath`: string (absolute path to .csproj directory, e.g., `src/PetstoreApi.Contracts/`)
- `csprojFile`: string (relative path to .csproj, e.g., `PetstoreApi.Contracts.csproj`)
- `packageId`: string (NuGet package identifier, e.g., `PetstoreApi.Contracts`)
- `version`: string (SemVer 2.0.0 version, e.g., `1.2.0`)
- `authors`: string (package authors, e.g., `Platform Team`)
- `description`: string (package description for NuGet feed)
- `includeSymbols`: boolean (generate .snupkg for debugging, default: false)
- `outputPackage`: string (generated .nupkg file path, e.g., `bin/Release/PetstoreApi.Contracts.1.2.0.nupkg`)
- `targetFramework`: string (e.g., `net8.0`)
- `compileIncludes`: List<CompileInclude> (references to generated source files via <Compile Include>)

**Relationships**:
- Contains: Endpoints (via CompileInclude), DTOs (via CompileInclude), Validators (via CompileInclude, conditional), Extension Methods (AddApiEndpoints, AddApiValidators)
- Referenced by: Implementation Project (via ProjectReference during development)
- Produces: Package Metadata (embedded in .nupkg)
- Dependencies: MediatR (IRequest<>, IMediator), FluentValidation (IValidator<>, conditional), ASP.NET Core Minimal APIs (IEndpointRouteBuilder)

**Validation Rules**:
- **FR-002**: MUST include only Endpoints/, DTOs/, and Validators/ directories (exclude Handlers/, Models/, Program.cs)
- **FR-003**: MUST set `<GeneratePackageOnBuild>false</GeneratePackageOnBuild>` (require explicit `dotnet pack`)
- **FR-004**: MUST include package dependencies with correct version constraints (MediatR, FluentValidation, ASP.NET Core)
- **FR-017**: MUST compile successfully with `dotnet build` (exit code 0)
- **FR-018**: MUST pack successfully with `dotnet pack`, producing valid `.nupkg` file
- **SC-002**: Package size MUST be under 500KB for typical OpenAPI spec (20 operations, 10 DTOs)
- **SC-009**: Build time MUST be under 10 seconds on standard hardware

**State Transitions**:
```
[Generated] --build--> [Compiled] --pack--> [Packaged] --push--> [Published]
                         ↓                      ↓
                    [Build Errors]         [.nupkg + .snupkg]
```

**Examples**:

```xml
<!-- PetstoreApi.Contracts.csproj (generated) -->
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
    
    <!-- NuGet Package Metadata (FR-016) -->
    <PackageId>PetstoreApi.Contracts</PackageId>
    <Version>1.2.0</Version>
    <Authors>Platform Team</Authors>
    <Company>Platform Team</Company>
    <Description>API contracts (DTOs, Endpoints, Validators) for PetstoreApi generated from OpenAPI specification</Description>
    
    <!-- Packaging Settings (FR-003) -->
    <GeneratePackageOnBuild>false</GeneratePackageOnBuild>
    
    <!-- Symbol Packages (FR-015, US5) -->
    <IncludeSymbols>true</IncludeSymbols>
    <SymbolPackageFormat>snupkg</SymbolPackageFormat>
    
    <!-- Repository Metadata -->
    <RepositoryUrl>https://github.com/org/petstore-api</RepositoryUrl>
    <RepositoryType>git</RepositoryType>
    <PackageProjectUrl>https://github.com/org/petstore-api</PackageProjectUrl>
    <PackageTags>openapi;minimal-api;contracts;petstore</PackageTags>
    <PackageLicenseExpression>Apache-2.0</PackageLicenseExpression>
  </PropertyGroup>

  <!-- Dependencies (FR-004) -->
  <ItemGroup>
    <PackageReference Include="MediatR" Version="12.2.0" />
    <PackageReference Include="FluentValidation" Version="11.9.0" />
    <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="8.0.0" />
  </ItemGroup>

  <!-- Unified Source References (RQ-001, FR-002) -->
  <ItemGroup>
    <!-- DTOs from Generated/ directory -->
    <Compile Include="..\..\Generated\Models\*.cs" Link="Models\%(Filename)%(Extension)" />
    
    <!-- Endpoints from Generated/ directory -->
    <Compile Include="..\..\Generated\Endpoints\*.cs" Link="Endpoints\%(Filename)%(Extension)" />
    
    <!-- Validators from Generated/ directory (conditional, FR-022) -->
    <Compile Include="..\..\Generated\Validators\*.cs" Link="Validators\%(Filename)%(Extension)" Condition="'$(UseValidators)' == 'true'" />
    
    <!-- Extension Methods (generated in project directory) -->
    <Compile Include="Extensions\EndpointExtensions.cs" />
    <Compile Include="Extensions\ValidatorExtensions.cs" Condition="'$(UseValidators)' == 'true'" />
  </ItemGroup>
</Project>
```

---

### Entity 2: Implementation Project

**Purpose**: Separate `.csproj` file containing business logic (Handlers, Models, Program.cs) that consumes the NuGet Package Project. Provides service registration and handler implementations while remaining decoupled from API surface layer.

**Fields**:
- `projectPath`: string (absolute path to .csproj directory, e.g., `src/PetstoreApi/`)
- `csprojFile`: string (relative path to .csproj, e.g., `PetstoreApi.csproj`)
- `contractsReference`: ProjectReference | PackageReference (reference type to Contracts project)
- `targetFramework`: string (e.g., `net8.0`)
- `userCode`: List<string> (user-owned files: Handlers/, Models/, Program.cs)

**Relationships**:
- References: NuGet Package Project (via ProjectReference or PackageReference)
- Contains: Handlers (IRequestHandler implementations), Models (business entities), Program.cs (service registration)
- Consumes: Extension Methods from NuGet Package Project (AddApiEndpoints, AddApiValidators, optional AddApiHandlers)
- Dependencies: All dependencies from NuGet Package Project (transitive), plus user-added dependencies (DbContext, cache, etc.)

**Validation Rules**:
- **FR-005**: MUST reference NuGet package project via `<ProjectReference>` during local development
- **FR-006**: MUST include Handlers/, Models/, Program.cs, and service registration extensions
- **FR-017**: MUST compile successfully with Contracts reference (exit code 0)
- **FR-019**: MUST support backward-compatible OpenAPI changes without breaking compilation
- **FR-020**: MUST fail compilation for non-backward-compatible OpenAPI changes (renamed properties, type changes)
- **FR-021**: Handler registration MUST use `AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))` to auto-discover handlers
- **SC-004**: Updating package with backward-compatible changes requires zero Handler code changes
- **SC-005**: Updating package with breaking changes causes compilation errors within 1 second
- **SC-009**: Build time MUST be under 10 seconds on standard hardware

**State Transitions**:
```
[Development Mode]                    [Published Mode]
      ↓                                      ↓
ProjectReference → Contracts.csproj    PackageReference → NuGet Feed
      ↓                                      ↓
  Local Build                            Restore + Build
      ↓                                      ↓
  Run/Debug                              Run/Deploy
```

**Examples**:

```xml
<!-- PetstoreApi.csproj (generated template, user customizes) -->
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>

  <!-- Development Mode: Reference Contracts project locally (FR-005) -->
  <ItemGroup>
    <ProjectReference Include="..\PetstoreApi.Contracts\PetstoreApi.Contracts.csproj" />
  </ItemGroup>

  <!-- Published Mode (uncomment when consuming published package):
  <ItemGroup>
    <PackageReference Include="PetstoreApi.Contracts" Version="1.2.0" />
  </ItemGroup>
  -->

  <!-- Additional Dependencies (user-added) -->
  <ItemGroup>
    <PackageReference Include="MediatR" Version="12.2.0" />
    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
    <PackageReference Include="Serilog.AspNetCore" Version="8.0.0" />
  </ItemGroup>

  <!-- User-Owned Code (FR-006) -->
  <ItemGroup>
    <Compile Include="Program.cs" />
    <Compile Include="Handlers\**\*.cs" />
    <Compile Include="Models\**\*.cs" />
  </ItemGroup>
</Project>
```

```csharp
// Program.cs (generated template, user customizes)
using PetstoreApi.Contracts.Extensions;

var builder = WebApplication.CreateBuilder(args);

// User registers custom services first (e.g., DbContext, cache)
builder.Services.AddDbContext<PetStoreDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

// Register MediatR handlers from this assembly (FR-021, RQ-003)
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

// Register validators from Contracts assembly (FR-008, RQ-004)
builder.Services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly);

var app = builder.Build();

// Register API endpoints from Contracts package (FR-007)
app.AddApiEndpoints();

app.Run();
```

---

### Entity 3: Generator CLI Options

**Purpose**: Command-line configuration properties that control NuGet packaging behavior. Enables developers to customize package metadata and generation mode via OpenAPI Generator CLI flags.

**Fields**:
- `useNugetPackaging`: boolean (enable dual-project structure, default: false)
- `packageId`: string (NuGet package identifier, default: derived from OpenAPI spec title)
- `packageVersion`: string (SemVer version, default: `1.0.0`)
- `packageAuthors`: string (package authors, default: `Generated by OpenAPI Generator`)
- `packageDescription`: string (package description, default: derived from OpenAPI spec description)
- `includeSymbols`: boolean (generate .snupkg, default: false)
- `packageLicenseExpression`: string (SPDX license identifier, default: `Apache-2.0`)
- `packageRepositoryUrl`: string (Git repository URL, default: empty)
- `packageProjectUrl`: string (project homepage URL, default: empty)
- `packageTags`: string (comma-separated tags, default: `openapi,minimal-api,contracts`)

**Relationships**:
- Configures: NuGet Package Project (via Package Metadata)
- Configures: Implementation Project (via project reference type)
- Processed by: Generator Class (MinimalApiServerCodegen.processOpts())
- Stored in: additionalProperties map (Mustache template context)

**Validation Rules**:
- **FR-011**: Generator MUST support `packageId` property (default: sanitized OpenAPI spec title)
- **FR-012**: Generator MUST support `packageVersion` property following SemVer 2.0.0 (default: 1.0.0)
- **FR-013**: Generator MUST support `packageAuthors` property (default: generator name)
- **FR-014**: Generator MUST support `packageDescription` property (default: OpenAPI spec description)
- **FR-015**: Generator MUST support `includeSymbols` property (default: false)
- **SC-006**: `packageVersion` MUST follow SemVer conventions (major.minor.patch)
- **SC-007**: CLI options MUST enable publishing via `dotnet nuget push` without manual .csproj edits

**Examples**:

```bash
# Minimal usage (US1)
openapi-generator-cli generate \
  -i petstore.yaml \
  -g aspnetcore-minimalapi \
  -o ./output \
  --additional-properties=useNugetPackaging=true

# Full metadata configuration (US4)
openapi-generator-cli generate \
  -i petstore.yaml \
  -g aspnetcore-minimalapi \
  -o ./output \
  --additional-properties=\
useNugetPackaging=true,\
packageId=MyCompany.PetStore.Contracts,\
packageVersion=2.1.0,\
packageAuthors=Platform Team,\
packageDescription=Petstore API contracts for microservices integration,\
packageLicenseExpression=MIT,\
packageRepositoryUrl=https://github.com/mycompany/petstore,\
packageProjectUrl=https://github.com/mycompany/petstore,\
packageTags=petstore;api;microservices,\
includeSymbols=true
```

```java
// MinimalApiServerCodegen.java (processing CLI options)
@Override
public void processOpts() {
    super.processOpts();
    
    // FR-011: packageId
    String packageId = (String) additionalProperties.getOrDefault(
        "packageId", 
        sanitizeName(openAPI.getInfo().getTitle()) + ".Contracts"
    );
    additionalProperties.put("packageId", packageId);
    
    // FR-012: packageVersion (SemVer validation)
    String packageVersion = (String) additionalProperties.getOrDefault("packageVersion", "1.0.0");
    if (!packageVersion.matches("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$")) {
        throw new IllegalArgumentException("packageVersion must follow SemVer format: " + packageVersion);
    }
    additionalProperties.put("packageVersion", packageVersion);
    
    // FR-013: packageAuthors
    String packageAuthors = (String) additionalProperties.getOrDefault(
        "packageAuthors", 
        "Generated by OpenAPI Generator"
    );
    additionalProperties.put("packageAuthors", packageAuthors);
    
    // FR-014: packageDescription
    String packageDescription = (String) additionalProperties.getOrDefault(
        "packageDescription",
        openAPI.getInfo().getDescription() != null 
            ? openAPI.getInfo().getDescription() 
            : "API contracts for " + openAPI.getInfo().getTitle()
    );
    additionalProperties.put("packageDescription", packageDescription);
    
    // FR-015: includeSymbols
    boolean includeSymbols = Boolean.parseBoolean(
        (String) additionalProperties.getOrDefault("includeSymbols", "false")
    );
    additionalProperties.put("includeSymbols", includeSymbols);
}
```

---

### Entity 4: Extension Methods

**Purpose**: Public static methods that encapsulate DI registration logic for API components. Provides clean integration points between NuGet package (Contracts) and host application (Implementation), following ASP.NET Core extension method patterns.

**Fields**:
- `methodName`: string (e.g., `AddApiEndpoints`, `AddApiValidators`, `AddApiHandlers`)
- `returnType`: string (e.g., `IEndpointRouteBuilder`, `IServiceCollection`)
- `parameterType`: string (e.g., `this IEndpointRouteBuilder endpoints`, `this IServiceCollection services`)
- `assemblyLocation`: string (Contracts.dll for AddApiEndpoints/AddApiValidators, Implementation.dll for AddApiHandlers)
- `isRequired`: boolean (true for AddApiEndpoints, false for others)
- `condition`: string (e.g., `useValidators=true` for AddApiValidators)

**Types**:

1. **AddApiEndpoints** (Required, FR-007):
   - **Purpose**: Register all endpoint routes with ASP.NET Core routing
   - **Location**: NuGet Package (Contracts.dll)
   - **Signature**: `public static IEndpointRouteBuilder AddApiEndpoints(this IEndpointRouteBuilder endpoints)`
   - **Rationale**: No standard ASP.NET Core method for bulk endpoint registration
   
2. **AddApiValidators** (Recommended, FR-008):
   - **Purpose**: Register FluentValidation validators from Contracts assembly
   - **Location**: NuGet Package (Contracts.dll)
   - **Signature**: `public static IServiceCollection AddApiValidators(this IServiceCollection services)`
   - **Rationale**: Validators in different assembly than Program.cs (requires explicit assembly reference)
   - **Alternative**: `services.AddValidatorsFromAssembly(typeof(SomeDto).Assembly)`

3. **AddApiHandlers** (Optional, FR-009):
   - **Purpose**: Register MediatR handlers for API consistency
   - **Location**: Implementation Project (Implementation.dll)
   - **Signature**: `public static IServiceCollection AddApiHandlers(this IServiceCollection services)`
   - **Rationale**: Optional convenience method; developers can use standard MediatR registration
   - **Alternative**: `services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))`

**Relationships**:
- Defined in: NuGet Package Project (AddApiEndpoints, AddApiValidators)
- Defined in: Implementation Project (AddApiHandlers, optional)
- Called from: Program.cs (host application)
- Registers: Services in DI Container (IServiceCollection)
- Registers: Endpoints in Routing (IEndpointRouteBuilder)

**Validation Rules**:
- **FR-007**: AddApiEndpoints MUST be public static method returning IEndpointRouteBuilder
- **FR-008**: AddApiValidators SHOULD be provided when `useValidators=true`
- **FR-009**: AddApiHandlers MAY be provided for API consistency (optional)
- **FR-010**: Extension methods MUST use constructor injection compatible with ASP.NET Core DI
- **SC-003**: Host application MUST register all components with 2-3 method calls (target: 2-3 lines)

**Examples**:

```csharp
// EndpointExtensions.cs (in Contracts project, FR-007)
namespace PetstoreApi.Contracts.Extensions;

public static class EndpointExtensions
{
    /// <summary>
    /// Registers all API endpoint routes from the Contracts assembly.
    /// Required extension method (no ASP.NET Core equivalent for bulk registration).
    /// </summary>
    public static IEndpointRouteBuilder AddApiEndpoints(this IEndpointRouteBuilder endpoints)
    {
        // Register Pet endpoints
        PetEndpoints.MapPetEndpoints(endpoints);
        
        // Register Store endpoints
        StoreEndpoints.MapStoreEndpoints(endpoints);
        
        // Register User endpoints
        UserEndpoints.MapUserEndpoints(endpoints);
        
        return endpoints;
    }
}
```

```csharp
// ValidatorExtensions.cs (in Contracts project, FR-008, conditional on useValidators=true)
namespace PetstoreApi.Contracts.Extensions;

public static class ValidatorExtensions
{
    /// <summary>
    /// Registers all FluentValidation validators from the Contracts assembly.
    /// Recommended extension method (validators in different assembly than Program.cs).
    /// Alternative: services.AddValidatorsFromAssembly(typeof(Pet).Assembly)
    /// </summary>
    public static IServiceCollection AddApiValidators(this IServiceCollection services)
    {
        // Scan Contracts assembly for all AbstractValidator<T> implementations
        services.AddValidatorsFromAssembly(typeof(ValidatorExtensions).Assembly);
        return services;
    }
}
```

```csharp
// HandlerExtensions.cs (in Implementation project, FR-009, optional)
namespace PetstoreApi.Extensions;

public static class HandlerExtensions
{
    /// <summary>
    /// Registers all MediatR handlers from the Implementation assembly.
    /// Optional extension method for API consistency.
    /// Alternative: services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))
    /// </summary>
    public static IServiceCollection AddApiHandlers(this IServiceCollection services)
    {
        services.AddMediatR(cfg => 
            cfg.RegisterServicesFromAssembly(typeof(HandlerExtensions).Assembly));
        return services;
    }
}
```

---

### Entity 5: Package Metadata

**Purpose**: Configuration data embedded in NuGet package .csproj that defines package identity, versioning, licensing, and discoverability. Visible in NuGet feeds and consumed by package managers.

**Fields**:
- `packageId`: string (unique package identifier, e.g., `MyCompany.PetStore.Contracts`)
- `version`: string (SemVer version, e.g., `2.1.0`)
- `authors`: string (comma-separated authors, e.g., `Platform Team, API Team`)
- `company`: string (organization name, defaults to authors)
- `description`: string (package description, shown on nuget.org)
- `repositoryUrl`: string (Git repository URL, enables GitHub integration)
- `repositoryType`: string (VCS type, e.g., `git`)
- `projectUrl`: string (project homepage/documentation URL)
- `licenseExpression`: string (SPDX license identifier, e.g., `Apache-2.0`, `MIT`)
- `tags`: string (semicolon-separated tags, e.g., `openapi;minimal-api;contracts`)
- `includeSymbols`: boolean (generate .snupkg for debugging)
- `symbolPackageFormat`: string (`snupkg` for standard format, legacy: `symbols.nupkg`)

**Relationships**:
- Embedded in: NuGet Package .csproj (PropertyGroup)
- Configured by: Generator CLI Options (via Mustache template variables)
- Visible in: NuGet feeds (nuget.org, Azure Artifacts, MyGet)
- Consumed by: Package managers (dotnet restore, Visual Studio, Rider)

**Validation Rules**:
- **FR-016**: NuGet package project MUST have `<PackageId>`, `<Version>`, `<Authors>`, `<Description>` elements
- **RQ-002**: PackageId and Version are REQUIRED (minimum viable metadata)
- **RQ-002**: Description, LicenseExpression, RepositoryUrl are STRONGLY RECOMMENDED (nuget.org best practices)
- **SC-006**: Version MUST follow SemVer conventions (major.minor.patch[-prerelease])
- **US4**: Metadata enables correct package identification and discoverability

**SemVer Guidelines** (SC-006):
- **Major version** (1.0.0 → 2.0.0): Breaking changes (renamed properties, removed endpoints, type changes)
- **Minor version** (1.0.0 → 1.1.0): Backward-compatible changes (new optional properties, new endpoints)
- **Patch version** (1.0.0 → 1.0.1): Bug fixes, internal changes (no API surface changes)
- **Prerelease** (1.0.0-beta.1): Unstable versions for testing

**Examples**:

```xml
<!-- Minimum viable metadata (RQ-002, no errors but poor UX) -->
<PropertyGroup>
  <PackageId>PetstoreApi.Contracts</PackageId>
  <Version>1.0.0</Version>
  <Authors>Generated by OpenAPI Generator</Authors>
</PropertyGroup>
```

```xml
<!-- Recommended metadata (RQ-002, production-ready) -->
<PropertyGroup>
  <!-- Identity -->
  <PackageId>MyCompany.PetStore.Contracts</PackageId>
  <Version>2.1.0</Version>
  <Authors>Platform Team, API Team</Authors>
  <Company>MyCompany</Company>
  
  <!-- Description -->
  <Description>API contracts (DTOs, Endpoints, Validators) for Petstore microservice. Supports CRUD operations on pets, orders, and users. Generated from OpenAPI 3.0 specification.</Description>
  
  <!-- Licensing (OSS compliance) -->
  <PackageLicenseExpression>Apache-2.0</PackageLicenseExpression>
  
  <!-- Repository (GitHub integration) -->
  <RepositoryUrl>https://github.com/mycompany/petstore-api</RepositoryUrl>
  <RepositoryType>git</RepositoryType>
  
  <!-- Discoverability -->
  <PackageProjectUrl>https://github.com/mycompany/petstore-api</PackageProjectUrl>
  <PackageTags>openapi;minimal-api;contracts;petstore;microservices</PackageTags>
  
  <!-- Debugging (US5) -->
  <IncludeSymbols>true</IncludeSymbols>
  <SymbolPackageFormat>snupkg</SymbolPackageFormat>
</PropertyGroup>
```

```mustache
<!-- Mustache template: Contracts.csproj.mustache -->
<PropertyGroup>
  <!-- NuGet Package Metadata -->
  <PackageId>{{packageId}}</PackageId>
  <Version>{{packageVersion}}</Version>
  <Authors>{{packageAuthors}}</Authors>
  <Company>{{packageAuthors}}</Company>
  <Description>{{packageDescription}}</Description>
  
  {{#packageLicenseExpression}}
  <PackageLicenseExpression>{{packageLicenseExpression}}</PackageLicenseExpression>
  {{/packageLicenseExpression}}
  
  {{#packageRepositoryUrl}}
  <RepositoryUrl>{{packageRepositoryUrl}}</RepositoryUrl>
  <RepositoryType>git</RepositoryType>
  {{/packageRepositoryUrl}}
  
  {{#packageProjectUrl}}
  <PackageProjectUrl>{{packageProjectUrl}}</PackageProjectUrl>
  {{/packageProjectUrl}}
  
  {{#packageTags}}
  <PackageTags>{{packageTags}}</PackageTags>
  {{/packageTags}}
  
  {{#includeSymbols}}
  <IncludeSymbols>true</IncludeSymbols>
  <SymbolPackageFormat>snupkg</SymbolPackageFormat>
  {{/includeSymbols}}
</PropertyGroup>
```

---

### Entity 6: Assembly Separation

**Purpose**: Architectural pattern that separates API surface layer (Contracts.dll) from business logic layer (Implementation.dll) at assembly boundaries. Enables independent versioning, distribution, and deployment while maintaining compile-time safety.

**Fields**:
- `contractsAssembly`: string (e.g., `PetstoreApi.Contracts.dll`)
- `implementationAssembly`: string (e.g., `PetstoreApi.dll`)
- `contractsContains`: List<string> (types in Contracts.dll)
- `implementationContains`: List<string> (types in Implementation.dll)

**Contracts.dll Contents** (API Surface):
- **Endpoints**: ASP.NET Core Minimal API endpoint definitions
- **DTOs**: Data Transfer Objects (requests/responses)
- **Validators**: FluentValidation validators (`AbstractValidator<T>`)
- **Commands**: MediatR command definitions (`IRequest<T>`)
- **Queries**: MediatR query definitions (`IRequest<T>`)
- **Extension Methods**: AddApiEndpoints(), AddApiValidators()

**Implementation.dll Contents** (Business Logic):
- **Handlers**: MediatR handler implementations (`IRequestHandler<TRequest, TResponse>`)
- **Models**: Domain/business entities (not exposed via API)
- **Program.cs**: Application entry point and service registration
- **Extension Methods**: AddApiHandlers() (optional)

**Relationships**:
- Contracts.dll: Produced by NuGet Package Project
- Implementation.dll: Produced by Implementation Project
- Implementation.dll → References → Contracts.dll (via ProjectReference or PackageReference)
- Separation enables: Independent versioning (US3), NuGet distribution (US1), MediatR decoupling (Feature 006)

**Validation Rules**:
- **FR-022**: Generator MUST separate components by assembly (Contracts vs Implementation)
- **FR-008**: Validators in Contracts.dll require special registration (different assembly than Program.cs)
- **FR-009**: Handlers in Implementation.dll can use standard MediatR registration (same assembly as Program.cs)
- **RQ-003**: MediatR cross-assembly handler resolution requires explicit assembly registration
- **RQ-004**: FluentValidation cross-assembly validator discovery requires explicit assembly reference

**Why This Separation Matters**:

1. **Validators Need Special Registration** (FR-008):
   - Validators live in Contracts.dll (packaged for distribution)
   - Program.cs lives in Implementation.dll
   - `services.AddValidatorsFromAssembly(typeof(Program).Assembly)` would NOT find validators (wrong assembly)
   - Solution: `services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly)` or extension method

2. **Handlers Use Standard Registration** (FR-009):
   - Handlers live in Implementation.dll (same as Program.cs)
   - `services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))` finds all handlers
   - No special assembly reference needed

3. **Independent Versioning** (US3):
   - Update Contracts.dll (v1.0 → v1.1) without changing Implementation.dll
   - Backward-compatible API changes don't require recompiling handlers
   - Breaking API changes cause compile errors (type safety preserved)

**Examples**:

```
┌─────────────────────────────────────────────────────┐
│ Contracts.dll (NuGet Package)                       │
│                                                     │
│ ┌─────────────┐  ┌─────────────┐  ┌──────────────┐│
│ │  Endpoints  │  │    DTOs     │  │  Validators  ││
│ │             │  │             │  │              ││
│ │ PetEndpoints│  │ Pet         │  │ PetValidator ││
│ │ StoreEndpts │  │ Order       │  │ OrderValid.  ││
│ └─────────────┘  └─────────────┘  └──────────────┘│
│                                                     │
│ ┌─────────────┐  ┌─────────────┐                  │
│ │  Commands   │  │   Queries   │                  │
│ │             │  │             │                  │
│ │ AddPetCmd   │  │ GetPetQuery │                  │
│ │ DeletePetCmd│  │ FindPetsQ   │                  │
│ └─────────────┘  └─────────────┘                  │
│                                                     │
│ ┌────────────────────────────────────────────────┐ │
│ │ Extension Methods                              │ │
│ │ • AddApiEndpoints() [Required]                 │ │
│ │ • AddApiValidators() [Recommended]             │ │
│ └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
                        ↑
                        │ References (ProjectReference or PackageReference)
                        │
┌─────────────────────────────────────────────────────┐
│ Implementation.dll (Host Application)               │
│                                                     │
│ ┌─────────────┐  ┌─────────────┐  ┌──────────────┐│
│ │  Handlers   │  │   Models    │  │  Program.cs  ││
│ │             │  │             │  │              ││
│ │ AddPetHndlr │  │ PetEntity   │  │ builder...   ││
│ │ DeletePetH. │  │ OrderEntity │  │ app.Run()    ││
│ └─────────────┘  └─────────────┘  └──────────────┘│
│                                                     │
│ ┌────────────────────────────────────────────────┐ │
│ │ Extension Methods (Optional)                   │ │
│ │ • AddApiHandlers() [Convenience]               │ │
│ └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

```csharp
// Program.cs - Cross-Assembly Service Registration

// WRONG: Would only scan Implementation.dll (miss validators)
// services.AddValidatorsFromAssembly(typeof(Program).Assembly); ❌

// CORRECT: Explicitly reference Contracts.dll for validators
services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly); ✅

// OR use extension method (abstracts assembly reference)
services.AddApiValidators(); ✅

// Handlers: Standard registration (same assembly as Program.cs)
services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly)); ✅
```

---

## Entity Relationships

```
┌──────────────────────────────────────────────────────────────────┐
│                     Generator CLI Options                        │
│  (useNugetPackaging, packageId, packageVersion, authors, etc.)   │
└───────────────────────┬──────────────────────────────────────────┘
                        │ Configures
                        ↓
        ┌───────────────────────────────────────┐
        │      MinimalApiServerCodegen          │
        │  (processes options, generates files) │
        └───────────┬───────────────────────────┘
                    │ Generates
        ┌───────────┴───────────┐
        ↓                       ↓
┌───────────────────┐   ┌───────────────────────┐
│  NuGet Package    │   │  Implementation       │
│     Project       │   │     Project           │
│   (.Contracts)    │   │                       │
├───────────────────┤   ├───────────────────────┤
│ • Endpoints       │◄──┤ • Handlers            │
│ • DTOs            │   │ • Models              │
│ • Validators      │   │ • Program.cs          │
│ • Commands        │   │                       │
│ • Queries         │   │                       │
│ • Extensions      │   │                       │
└───────┬───────────┘   └───────────────────────┘
        │ Produces              ↑
        ↓                       │ References
┌───────────────────┐           │ (ProjectReference)
│  Package Metadata │           │
│  (PackageId, etc.)│───────────┘
└───────────────────┘
        │ Embedded in
        ↓
┌───────────────────┐
│    .nupkg File    │
│  (NuGet Package)  │
└───────────────────┘
        │ Consumed by
        ↓
┌───────────────────┐
│   Host Apps       │
│  (Microservices)  │
└───────────────────┘

Extension Methods Bridge:
┌────────────────────────────────────────┐
│ NuGet Package (Contracts.dll)          │
│ • AddApiEndpoints()     [Required]     │
│ • AddApiValidators()    [Recommended]  │
└───────────┬────────────────────────────┘
            │ Called from
            ↓
┌────────────────────────────────────────┐
│ Program.cs (Implementation.dll)        │
│ builder.Services.AddApiValidators();   │
│ app.AddApiEndpoints();                 │
└────────────────────────────────────────┘

Assembly Separation:
┌─────────────────────────────────────────┐
│ Contracts.dll (API Surface)             │
│ ├─ Endpoints/                           │
│ ├─ DTOs/                                │
│ ├─ Validators/  ← Cross-assembly lookup │
│ ├─ Commands/    ← MediatR IRequest<T>   │
│ └─ Queries/                             │
└──────────────┬──────────────────────────┘
               │ Referenced by
               ↓
┌─────────────────────────────────────────┐
│ Implementation.dll (Business Logic)     │
│ ├─ Handlers/    ← MediatR IRequestHandler│
│ ├─ Models/                              │
│ └─ Program.cs   ← DI registration       │
└─────────────────────────────────────────┘
```

---

## State Transition Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                   Generator Execution                           │
└──────────────────────┬──────────────────────────────────────────┘
                       ↓
              [OpenAPI Spec Input]
                       ↓
        ┌──────────────────────────┐
        │ Process CLI Options      │
        │ (useNugetPackaging=true) │
        └──────────┬────────────────┘
                   ↓
        ┌──────────────────────────┐
        │ Generate Source Files    │
        │ (Endpoints, DTOs, etc.)  │
        └──────────┬────────────────┘
                   ↓
        ┌──────────────────────────┐
        │ Generate .csproj Files   │
        │ (Contracts + Impl)       │
        └──────────┬────────────────┘
                   ↓
              [Generated]
                   ↓
        ┌──────────────────────────┐
        │  dotnet build            │
        │  (Contracts project)     │
        └──────────┬────────────────┘
                   ↓
              [Compiled] ──Error──> [Build Failure]
                   ↓ Success
        ┌──────────────────────────┐
        │  dotnet pack             │
        │  (Contracts project)     │
        └──────────┬────────────────┘
                   ↓
              [Packaged]
                   ↓
          ┌────────┴────────┐
          ↓                 ↓
    [.nupkg File]     [.snupkg File]
                       (if includeSymbols=true)
          ↓
    ┌─────────────────────────┐
    │ dotnet nuget push       │
    │ (to NuGet feed)         │
    └─────────┬───────────────┘
              ↓
         [Published]
              ↓
    ┌──────────────────────────┐
    │ Host Apps consume via    │
    │ PackageReference         │
    └──────────────────────────┘

Implementation Project Lifecycle:
┌─────────────────────────────────┐
│ Development Mode                │
│ (ProjectReference to Contracts) │
└──────────┬──────────────────────┘
           ↓
   [Local Development]
           ↓
   ┌───────────────────┐
   │ dotnet build      │
   └───────┬───────────┘
           ↓
      [Compiled]
           ↓
   ┌───────────────────┐
   │ dotnet run        │
   └───────┬───────────┘
           ↓
      [Running]
           ↓
   ┌───────────────────────────────┐
   │ Switch to PackageReference    │
   │ (consume published package)   │
   └───────────────────────────────┘
```

---

## Summary

The NuGet API Contract Packaging data model introduces **6 core entities** that work together to enable independent versioning and distribution of API contracts:

1. **NuGet Package Project**: Packages API surface layer (Endpoints, DTOs, Validators) for distribution, with comprehensive metadata and symbol support
2. **Implementation Project**: Contains business logic (Handlers, Models) and references Contracts via ProjectReference or PackageReference
3. **Generator CLI Options**: Configures package metadata and generation mode via command-line flags
4. **Extension Methods**: Provides clean DI registration points (AddApiEndpoints, AddApiValidators, AddApiHandlers)
5. **Package Metadata**: Defines package identity, versioning, licensing, and discoverability in NuGet feeds
6. **Assembly Separation**: Architectural pattern separating Contracts.dll (API surface) from Implementation.dll (business logic)

**Key Architectural Decisions**:
- **Unified Source Generation** (RQ-001): Generated files created once in `Generated/` directory, referenced by multiple .csproj files via `<Compile Include>` to avoid duplication
- **Cross-Assembly Service Registration** (RQ-003, RQ-004): MediatR and FluentValidation require explicit assembly references for cross-assembly component discovery
- **SemVer Versioning** (SC-006): Package versions follow semantic versioning for backward-compatible vs breaking changes
- **Extension Method Strategy**: Required (AddApiEndpoints), Recommended (AddApiValidators), Optional (AddApiHandlers) based on technical necessity

**Success Metrics**:
- Package size < 500KB (SC-002)
- Build time < 10 seconds (SC-009)
- 2-3 method calls for full integration (SC-003)
- Zero Handler changes for backward-compatible updates (SC-004)
- Compilation errors within 1 second for breaking changes (SC-005)
