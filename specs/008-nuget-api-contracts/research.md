# Research: NuGet API Contract Packaging

**Branch**: `008-nuget-api-contracts` | **Date**: 2026-01-27 | **Spec**: [spec.md](spec.md)

## Research Questions

### RQ-001: How should .csproj files reference shared source files in unified generation approach?

**Status**: Resolved

**Context**: Feature 008 uses a "unified generation" approach where source files are generated once and referenced by multiple .csproj files (Contracts.csproj and Implementation.csproj). This avoids code duplication and ensures consistency. Need to determine the best MSBuild pattern for cross-project file references.

**Options Considered**:

1. **`<Compile Include>` with relative paths (Link-style)**:
   - Pros: Standard MSBuild pattern, widely supported, no file duplication on disk
   - Cons: Files appear in different logical location in Solution Explorer (requires Link metadata)
   - Complexity: Low
   - Example:
   ```xml
   <ItemGroup>
     <Compile Include="..\Shared\**\*.cs" Link="%(RecursiveDir)%(Filename)%(Extension)" />
   </ItemGroup>
   ```

2. **`<Compile Include>` with wildcards and LinkBase**:
   - Pros: Preserves directory structure in Solution Explorer, clean logical organization
   - Cons: Slightly more complex metadata, LinkBase may not be understood by all tools
   - Complexity: Medium
   - Example:
   ```xml
   <ItemGroup>
     <Compile Include="..\Generated\Contracts\**\*.cs" LinkBase="Contracts" />
   </ItemGroup>
   ```

3. **Directory.Build.props with conditional includes**:
   - Pros: Centralized configuration, DRY principle
   - Cons: Less explicit per-project, harder to understand at glance
   - Complexity: Medium-High
   - Example:
   ```xml
   <!-- Directory.Build.props -->
   <ItemGroup Condition="'$(IsContractsProject)' == 'true'">
     <Compile Include="$(MSBuildThisFileDirectory)Generated\Contracts\**\*.cs" />
   </ItemGroup>
   ```

4. **File copying with MSBuild targets**:
   - Pros: Files physically present in each project
   - Cons: Duplication on disk, sync issues, build complexity
   - Complexity: High
   - NOT RECOMMENDED

**Decision**: Use Option 1 (`<Compile Include>` with relative paths) for simplicity and standard MSBuild practices

**Rationale**: 
- Microsoft documentation explicitly recommends `<Compile Include>` with relative paths for cross-project file sharing
- Standard pattern used throughout .NET ecosystem (e.g., shared projects)
- Link metadata allows correct Solution Explorer display without physical file duplication
- Low complexity, well-understood by all MSBuild tooling
- No dependency on advanced MSBuild features

**Evidence**: 
- [MSBuild Common Items documentation](https://learn.microsoft.com/en-us/visualstudio/msbuild/common-msbuild-project-items) shows Compile item with Link metadata
- [.NET SDK Overview](https://learn.microsoft.com/en-us/dotnet/core/project-sdk/overview) documents standard file inclusion patterns
- Feature 002 research.md (line 104): LinkBase metadata explanation shows this is standard practice

**Implementation Notes**:
- **Contracts.csproj references**:
  ```xml
  <ItemGroup>
    <!-- DTOs -->
    <Compile Include="..\Generated\**\Models\*.cs" Link="Models\%(Filename)%(Extension)" />
    
    <!-- Endpoints -->
    <Compile Include="..\Generated\**\Endpoints\*.cs" Link="Endpoints\%(Filename)%(Extension)" />
    
    <!-- Validators (if enabled) -->
    <Compile Include="..\Generated\**\Validators\*.cs" Link="Validators\%(Filename)%(Extension)" Condition="'$(UseValidators)' == 'true'" />
  </ItemGroup>
  ```

- **Implementation.csproj references**:
  ```xml
  <ItemGroup>
    <!-- Reference Contracts project -->
    <ProjectReference Include="..\PetstoreApi.Contracts\PetstoreApi.Contracts.csproj" />
    
    <!-- Handler implementations (NOT generated) -->
    <Compile Include="Handlers\**\*.cs" />
    
    <!-- Program.cs (NOT generated, user-owned) -->
    <Compile Include="Program.cs" />
  </ItemGroup>
  ```

- **Generator output structure**:
  ```
  src/
    PetstoreApi.Contracts/
      PetstoreApi.Contracts.csproj   (references ../Generated/...)
    PetstoreApi/
      PetstoreApi.csproj             (references Contracts project + user code)
      Program.cs                     (user-owned, NOT generated)
      Handlers/                      (user-owned, NOT generated)
        AddPetCommandHandler.cs
        ...
  Generated/
    Models/
      Pet.cs
      Order.cs
    Endpoints/
      PetEndpoints.cs
      StoreEndpoints.cs
    Validators/
      PetValidator.cs               (if useValidators=true)
  ```

- Use `Link` metadata to preserve logical directory structure in IDE
- Use `%(RecursiveDir)` to maintain subdirectory structure automatically
- Wildcards (`**\*.cs`) enable adding new files without .csproj edits

---

### RQ-002: What NuGet package metadata properties are REQUIRED vs OPTIONAL in .csproj?

**Status**: Resolved

**Context**: Feature 008 generates .csproj files that will be packed into NuGet packages using `dotnet pack`. Need to understand minimum viable metadata to prevent errors, plus recommended metadata for professional packages on nuget.org.

**Options Considered**:

1. **Minimal viable metadata (no errors)**:
   - Pros: Quick setup, no friction
   - Cons: Poor user experience, warnings from nuget.org, unprofessional
   - Properties: `PackageId`, `Version`, `Authors`
   - Complexity: Low

2. **Recommended metadata (nuget.org best practices)**:
   - Pros: Professional appearance, better discoverability, license compliance
   - Cons: More template complexity, some properties may be redundant
   - Properties: All of minimal + Description, RepositoryUrl, PackageTags, PackageLicenseExpression, PackageProjectUrl
   - Complexity: Medium

3. **Full metadata (kitchen sink)**:
   - Pros: Complete information
   - Cons: Information overload, some metadata rarely useful
   - Properties: All of recommended + PackageIcon, PackageReadmeFile, PackageReleaseNotes, Copyright, etc.
   - Complexity: High

**Decision**: Use Option 2 (Recommended metadata) with sensible defaults from OpenAPI spec

**Rationale**:
- Microsoft docs state only `PackageId` and `Version` are strictly required to avoid errors
- `Authors` defaults to AssemblyName if not specified (acceptable default)
- nuget.org strongly recommends Description, License, ProjectUrl for discoverability
- PackageTags improve search ranking on nuget.org
- RepositoryUrl enables GitHub integration (source link, issues)
- Balance between completeness and template simplicity

**Evidence**:
- [NuGet package creation docs](https://learn.microsoft.com/en-us/nuget/create-packages/creating-a-package-dotnet-cli) list required vs optional properties
- [MSBuild props reference](https://learn.microsoft.com/en-us/dotnet/core/project-sdk/msbuild-props#package-properties) documents PackageId, Version, Authors behavior
- nuget.org best practices guide emphasizes Description, License, Tags

**Implementation Notes**:

**Minimum viable .csproj (no errors)**:
```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <PackageId>PetstoreApi.Contracts</PackageId>
    <Version>1.0.0</Version>
    <Authors>Generated by OpenAPI Generator</Authors>
  </PropertyGroup>
</Project>
```

**Recommended .csproj (production-ready)**:
```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
    
    <!-- NuGet Package Metadata -->
    <PackageId>{{packageName}}.Contracts</PackageId>
    <Version>{{packageVersion}}</Version>
    <Authors>{{packageAuthors}}</Authors>
    <Company>{{packageAuthors}}</Company>
    <Description>API contracts (DTOs, Endpoints, Validators) for {{packageName}} generated from OpenAPI specification</Description>
    
    <!-- Licensing -->
    <PackageLicenseExpression>{{packageLicenseExpression}}</PackageLicenseExpression>
    
    <!-- Repository -->
    <RepositoryUrl>{{packageRepositoryUrl}}</RepositoryUrl>
    <RepositoryType>git</RepositoryType>
    
    <!-- Additional metadata -->
    <PackageProjectUrl>{{packageProjectUrl}}</PackageProjectUrl>
    <PackageTags>openapi;minimal-api;contracts;{{additionalTags}}</PackageTags>
    
    <!-- Symbol packages for debugging -->
    <IncludeSymbols>true</IncludeSymbols>
    <SymbolPackageFormat>snupkg</SymbolPackageFormat>
  </PropertyGroup>
</Project>
```

**Mustache template mapping** (generator configuration):
```java
// In MinimalApiServerCodegen.java
additionalProperties.put("packageName", "PetstoreApi");
additionalProperties.put("packageVersion", "1.0.0");
additionalProperties.put("packageAuthors", "Generated by OpenAPI Generator");
additionalProperties.put("packageLicenseExpression", "Apache-2.0");
additionalProperties.put("packageRepositoryUrl", "https://github.com/org/repo");
additionalProperties.put("packageProjectUrl", "https://github.com/org/repo");
additionalProperties.put("additionalTags", "petstore;api");
```

**Property explanations**:
- **Required**: `PackageId` (unique identifier), `Version` (SemVer 2.0.0)
- **Strongly Recommended**: `Description` (shows on nuget.org), `PackageLicenseExpression` (OSS compliance)
- **Recommended**: `RepositoryUrl` (source code link), `PackageTags` (discoverability), `PackageProjectUrl` (documentation)
- **Optional but useful**: `IncludeSymbols` + `SymbolPackageFormat` (debugging support)
- **Defaults**: `Authors` → `AssemblyName`, `Company` → `Authors` if not specified

**SymbolPackageFormat values**:
- `snupkg`: Standard format, smaller, portable symbol packages (RECOMMENDED)
- `symbols.nupkg`: Legacy format, larger, deprecated

---

### RQ-003: How does MediatR's AddMediatR(cfg => cfg.RegisterServicesFromAssembly()) work?

**Status**: Resolved

**Context**: Feature 006 introduced MediatR for decoupling handlers. Feature 008 splits contracts (requests) and implementations (handlers) into separate assemblies. Need to understand if MediatR's assembly scanning can discover handlers in a different assembly than the requests.

**Options Considered**:

1. **Single assembly registration (scan only one assembly)**:
   - Pros: Simple, explicit control
   - Cons: Won't work for Feature 008 (handlers in different assembly than requests)
   - Code: `cfg.RegisterServicesFromAssembly(typeof(AddPetCommandHandler).Assembly)`
   - Complexity: Low

2. **Multiple assembly registration (scan both assemblies)**:
   - Pros: Explicitly scans all assemblies, works across boundaries
   - Cons: Requires knowing both assembly types at startup
   - Code: 
   ```csharp
   cfg.RegisterServicesFromAssemblies(
       typeof(AddPetCommand).Assembly,    // Contracts
       typeof(AddPetCommandHandler).Assembly  // Implementation
   )
   ```
   - Complexity: Medium

3. **Assembly.Load() by name (dynamic loading)**:
   - Pros: No compile-time dependency on handler assembly
   - Cons: Runtime errors if assembly not found, less type-safe
   - Code: `cfg.RegisterServicesFromAssembly(Assembly.Load("PetstoreApi.Handlers"))`
   - Complexity: Medium-High

4. **Assembly scanning all loaded assemblies**:
   - Pros: Automatically discovers all handlers
   - Cons: Performance impact, may register unwanted handlers
   - Code: `cfg.RegisterServicesFromAssemblies(AppDomain.CurrentDomain.GetAssemblies())`
   - Complexity: Low

**Decision**: Use Option 2 (Multiple assembly registration) for explicit control and type safety

**Rationale**:
- MediatR 12.x supports cross-assembly handler resolution
- `RegisterServicesFromAssembly()` scans for ALL `IRequestHandler<,>` implementations in specified assembly
- Each assembly must be registered explicitly - scanning ONE assembly does NOT find handlers in OTHER assemblies
- MediatR matches requests to handlers by interface signature, not by assembly location
- Performance: Assembly scanning happens once at startup, negligible cost
- Type safety: Using typeof() ensures compile-time checking vs Assembly.Load(string)

**Evidence**:
- [MediatR Wiki](https://github.com/jbogard/MediatR/wiki) Setup section: "For each assembly registered, the AddMediatR method will scan those assemblies for MediatR types"
- MediatR source code: `RegisterServicesFromAssembly()` uses reflection to find all `IRequestHandler<,>` implementations
- MediatR 12.x documentation: "cfg.RegisterServicesFromAssembly(typeof(Program).Assembly)" - implies each assembly needs explicit registration

**Implementation Notes**:

**How RegisterServicesFromAssembly() works**:
```csharp
// Pseudocode of MediatR internals
public void RegisterServicesFromAssembly(Assembly assembly)
{
    // 1. Scan assembly for all types
    var allTypes = assembly.GetExportedTypes();
    
    // 2. Filter for IRequestHandler<,> implementations
    var handlerTypes = allTypes.Where(t => 
        t.GetInterfaces().Any(i => 
            i.IsGenericType && 
            i.GetGenericTypeDefinition() == typeof(IRequestHandler<,>)
        )
    );
    
    // 3. Register each handler as transient
    foreach (var handlerType in handlerTypes)
    {
        var interfaces = handlerType.GetInterfaces()
            .Where(i => i.IsGenericType && 
                        i.GetGenericTypeDefinition() == typeof(IRequestHandler<,>));
        
        foreach (var handlerInterface in interfaces)
        {
            services.AddTransient(handlerInterface, handlerType);
        }
    }
    
    // 4. Also registers INotificationHandler<>, IStreamRequestHandler<>, etc.
}
```

**Cross-assembly scenario for Feature 008**:
```csharp
// Program.cs in PetstoreApi project (Implementation)
var builder = WebApplication.CreateBuilder(args);

// Register MediatR with BOTH assemblies
builder.Services.AddMediatR(cfg => {
    // Scan Contracts assembly for IRequest<> definitions
    cfg.RegisterServicesFromAssembly(typeof(AddPetCommand).Assembly);
    
    // Scan Implementation assembly for IRequestHandler<,> implementations
    cfg.RegisterServicesFromAssembly(typeof(AddPetCommandHandler).Assembly);
});

// At runtime, MediatR matches:
// AddPetCommand : IRequest<Pet>  (from Contracts.dll)
// ↓ matched to ↓
// AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>  (from Implementation.dll)
```

**Performance implications**:
- Assembly scanning happens ONCE at startup (DI container registration)
- Typical scanning cost: < 50ms for 100 types (negligible)
- Runtime request dispatch: O(1) dictionary lookup, ~100ns overhead
- No performance penalty compared to manual registration

**Alternative if only Implementation assembly matters**:
```csharp
// Simpler: Only scan Implementation assembly (handlers are what matter for DI)
builder.Services.AddMediatR(cfg => {
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly);
});

// This works because:
// - IRequest<> types (contracts) don't need to be registered in DI
// - Only IRequestHandler<,> implementations need to be in DI container
// - MediatR uses typeof(TRequest) from Send<TRequest>() call to find handler
```

**Recommendation for Feature 008**:
- Only scan Implementation assembly: `typeof(Program).Assembly`
- Contracts assembly (IRequest<> types) doesn't need scanning - no services to register
- Keep Program.cs simple and focused on what actually needs DI registration

---

### RQ-004: How does FluentValidation's AddValidatorsFromAssembly() work?

**Status**: Resolved

**Context**: Feature 008 includes validators in the Contracts package when `useValidators=true`. Need to understand how FluentValidation discovers validators and whether it works across assembly boundaries (validators in Contracts.dll, registration in Implementation.dll).

**Options Considered**:

1. **`AddValidatorsFromAssembly(Assembly)` with typeof().Assembly**:
   - Pros: Type-safe, compile-time checking, standard pattern
   - Cons: Requires reference to type in target assembly
   - Code: `services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly)`
   - Complexity: Low

2. **`AddValidatorsFromAssembly(Assembly)` with Assembly.Load()**:
   - Pros: No compile-time dependency
   - Cons: Runtime errors if assembly not found, string-based (fragile)
   - Code: `services.AddValidatorsFromAssembly(Assembly.Load("PetstoreApi.Contracts"))`
   - Complexity: Medium

3. **`AddValidatorsFromAssemblies()` multiple assemblies**:
   - Pros: Explicitly scans multiple assemblies if needed
   - Cons: More verbose if only one assembly has validators
   - Code: `services.AddValidatorsFromAssemblies(assembly1, assembly2)`
   - Complexity: Medium

4. **Manual registration per validator**:
   - Pros: Explicit control, no scanning overhead
   - Cons: High maintenance burden, easy to forget validators
   - Code: `services.AddScoped<IValidator<Pet>, PetValidator>()`
   - Complexity: High

**Decision**: Use Option 1 (typeof().Assembly) with reference to Contracts assembly

**Rationale**:
- FluentValidation 11.9.x provides `AddValidatorsFromAssembly()` for automatic discovery
- Scanning finds ALL types inheriting from `AbstractValidator<T>` in specified assembly
- Uses reflection to register each validator as `IValidator<T>` → `TValidator` mapping
- Type-safe approach (typeof()) is standard .NET pattern, recommended by FluentValidation docs
- Assembly.Load(string) is fragile and discouraged (no compile-time checking)
- Performance: Scanning happens once at startup, negligible cost (<100ms for typical apps)

**Evidence**:
- [FluentValidation GitHub](https://github.com/FluentValidation/FluentValidation) README shows `AddValidatorsFromAssembly(typeof(T).Assembly)` pattern
- FluentValidation.DependencyInjectionExtensions source code: `AddValidatorsFromAssembly()` uses reflection to find validators
- Official docs recommend typeof() over Assembly.Load() for type safety

**Implementation Notes**:

**How AddValidatorsFromAssembly() works**:
```csharp
// Pseudocode of FluentValidation internals
public static IServiceCollection AddValidatorsFromAssembly(
    this IServiceCollection services, 
    Assembly assembly, 
    ServiceLifetime lifetime = ServiceLifetime.Scoped)
{
    // 1. Scan assembly for all types
    var allTypes = assembly.GetExportedTypes();
    
    // 2. Filter for AbstractValidator<T> descendants
    var validatorTypes = allTypes.Where(t => 
        t.IsClass && 
        !t.IsAbstract && 
        t.BaseType != null &&
        t.BaseType.IsGenericType &&
        t.BaseType.GetGenericTypeDefinition() == typeof(AbstractValidator<>)
    );
    
    // 3. Register each validator as IValidator<T> → ConcreteValidator
    foreach (var validatorType in validatorTypes)
    {
        var baseType = validatorType.BaseType;
        var modelType = baseType.GetGenericArguments()[0]; // Extract T from AbstractValidator<T>
        
        var interfaceType = typeof(IValidator<>).MakeGenericType(modelType);
        
        // Register with specified lifetime (default: Scoped)
        services.Add(new ServiceDescriptor(interfaceType, validatorType, lifetime));
    }
    
    return services;
}
```

**Cross-assembly scenario for Feature 008**:
```csharp
// Program.cs in PetstoreApi project (Implementation)
var builder = WebApplication.CreateBuilder(args);

// Reference Contracts assembly where validators live
// (Already have ProjectReference to PetstoreApi.Contracts)
builder.Services.AddValidatorsFromAssembly(typeof(PetValidator).Assembly);

// At runtime, FluentValidation resolves:
// IValidator<Pet> → PetValidator (from Contracts.dll)
// IValidator<Order> → OrderValidator (from Contracts.dll)
```

**ServiceLifetime options**:
- **Scoped** (DEFAULT): Validator created per HTTP request, shared within request
  - Use case: Validators with dependencies on scoped services (e.g., DbContext)
  - Recommended for web APIs
- **Transient**: New validator instance per resolution
  - Use case: Stateless validators, no dependencies
  - Slightly higher allocation overhead
- **Singleton**: Single validator instance for app lifetime
  - Use case: Pure validators with no dependencies
  - Best performance, but DANGEROUS if validator has state

**Performance implications**:
- Assembly scanning: One-time cost at startup, ~50-200ms for typical apps
- Runtime validation: No overhead from assembly scanning (DI container lookup is O(1))
- Recommendation: Always use Scoped lifetime for validators (best balance)

**Alternative: Manual registration (NOT recommended)**:
```csharp
// Feature 008 should NOT do this - defeats purpose of auto-discovery
services.AddScoped<IValidator<Pet>, PetValidator>();
services.AddScoped<IValidator<Order>, OrderValidator>();
services.AddScoped<IValidator<User>, UserValidator>();
// ... manual registration for every validator (maintenance burden)
```

**Recommendation for Feature 008 Program.cs template**:
```csharp
{{#useValidators}}
// Register FluentValidation validators from Contracts assembly
builder.Services.AddValidatorsFromAssembly(typeof({{packageName}}.Contracts.{{firstModel}}Validator).Assembly);
{{/useValidators}}
```

**Why typeof(FirstModelValidator).Assembly**:
- Need ANY type from Contracts assembly to get Assembly reference
- Validators are in Contracts assembly (alongside DTOs)
- FirstModelValidator is guaranteed to exist if useValidators=true
- Alternative: Use ANY DTO type from Contracts (e.g., typeof(Pet).Assembly)

**Additional AddValidatorsFromAssembly() options**:
```csharp
// With custom lifetime
services.AddValidatorsFromAssembly(assembly, ServiceLifetime.Transient);

// With filter predicate (advanced)
services.AddValidatorsFromAssembly(assembly, lifetime: ServiceLifetime.Scoped, 
    filter: result => result.ValidatorType.Namespace?.StartsWith("PetstoreApi") == true);

// Include internal validators (default: only public)
services.AddValidatorsFromAssembly(assembly, includeInternalTypes: true);
```

**Best practice for Feature 008**:
- Use default Scoped lifetime (web API best practice)
- No filter needed (all validators in Contracts assembly are relevant)
- Include only public validators (default behavior)

---

### RQ-005: What is the "unified source generation" approach and how should it work?

**Status**: Resolved

**Context**: Feature 008 introduces a "unified source generation" approach where the generator creates files once and multiple .csproj files reference them, avoiding file duplication. This is different from traditional multi-project generation where files are duplicated per project. Need to determine best directory structure and reference patterns.

**Options Considered**:

1. **Generate to separate project directories (file duplication)**:
   - Pros: Simple, each project self-contained, no shared dependencies
   - Cons: File duplication, sync issues if regenerated, wasted disk space
   - Structure:
   ```
   src/
     PetstoreApi.Contracts/
       Models/Pet.cs
       Endpoints/PetEndpoints.cs
     PetstoreApi/
       Models/Pet.cs          (DUPLICATE)
       Endpoints/PetEndpoints.cs  (DUPLICATE)
       Handlers/...
   ```
   - Complexity: Low
   - REJECTED: Violates DRY principle

2. **Generate to src/Shared/, reference via relative paths**:
   - Pros: Single source of truth, no duplication, clean separation
   - Cons: Requires <Compile Include> with Link metadata, extra directory
   - Structure:
   ```
   src/
     Shared/
       Models/Pet.cs
       Endpoints/PetEndpoints.cs
       Validators/PetValidator.cs
     PetstoreApi.Contracts/
       PetstoreApi.Contracts.csproj  (references ../Shared/...)
     PetstoreApi/
       PetstoreApi.csproj
   ```
   - Complexity: Medium

3. **Generate to dedicated Generated/ directory at solution level**:
   - Pros: Clear separation (user code vs generated), single source, supports .gitignore
   - Cons: Requires relative path references, slightly longer paths
   - Structure:
   ```
   Generated/
     Models/Pet.cs
     Endpoints/PetEndpoints.cs
     Validators/PetValidator.cs
   src/
     PetstoreApi.Contracts/
       PetstoreApi.Contracts.csproj  (references ../../Generated/...)
     PetstoreApi/
       PetstoreApi.csproj
   ```
   - Complexity: Medium
   - RECOMMENDED

4. **Generate to Contracts project, Implementation uses <Compile Include>**:
   - Pros: Contracts project owns the files physically
   - Cons: Mixing generated and project files, Contracts not truly standalone
   - Structure:
   ```
   src/
     PetstoreApi.Contracts/
       Models/Pet.cs
       Endpoints/PetEndpoints.cs
       PetstoreApi.Contracts.csproj
     PetstoreApi/
       PetstoreApi.csproj  (uses <Compile Include="../Contracts/Models/**/*.cs">)
   ```
   - Complexity: Medium-Low

**Decision**: Use Option 3 (Generate to Generated/ directory at solution level)

**Rationale**:
- Clear separation between generated code (throw away, regenerate anytime) and user code (version controlled, hand-edited)
- Supports .gitignore for Generated/ directory (common pattern)
- Single source of truth - files generated once, referenced by multiple projects
- Follows .NET SDK pattern: generated files often go in obj/ or separate directories
- Easy to clean: delete Generated/ directory
- No mixing of generated and hand-written code in same directory
- Explicit that these files are not user-owned

**Evidence**:
- [.NET SDK Overview](https://learn.microsoft.com/en-us/dotnet/core/project-sdk/overview) shows SDK generates files to obj/ directory (separate from user code)
- MSBuild documentation recommends separating generated artifacts from source
- Common pattern in Roslyn source generators: output to separate directory
- OpenAPI Generator itself uses separate output directories by default

**Implementation Notes**:

**Directory structure**:
```
minimal-api-gen/
  Generated/                          # ← Generator output (can be .gitignored)
    Models/
      Pet.cs
      Order.cs
      User.cs
    Endpoints/
      PetEndpoints.cs
      StoreEndpoints.cs
      UserEndpoints.cs
    Validators/                       # Only if useValidators=true
      PetValidator.cs
      OrderValidator.cs
  src/
    PetstoreApi.Contracts/
      PetstoreApi.Contracts.csproj    # References ../../Generated/**/*.cs
    PetstoreApi/
      PetstoreApi.csproj              # ProjectReference to Contracts
      Program.cs                      # User-owned, NOT generated
      appsettings.json               # User-owned, NOT generated
      Handlers/                       # User-owned, NOT generated
        AddPetCommandHandler.cs
        GetPetByIdQueryHandler.cs
  PetstoreApi.sln
```

**PetstoreApi.Contracts.csproj** (references generated files):
```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
    
    <!-- NuGet metadata -->
    <PackageId>PetstoreApi.Contracts</PackageId>
    <Version>1.0.0</Version>
    <Authors>Generated by OpenAPI Generator</Authors>
    <Description>API contracts for PetstoreApi</Description>
  </PropertyGroup>

  <ItemGroup>
    <!-- Reference generated models -->
    <Compile Include="..\..\Generated\Models\**\*.cs" Link="Models\%(RecursiveDir)%(Filename)%(Extension)" />
    
    <!-- Reference generated endpoints -->
    <Compile Include="..\..\Generated\Endpoints\**\*.cs" Link="Endpoints\%(RecursiveDir)%(Filename)%(Extension)" />
    
    <!-- Reference generated validators (conditional) -->
    <Compile Include="..\..\Generated\Validators\**\*.cs" Link="Validators\%(RecursiveDir)%(Filename)%(Extension)" Condition="Exists('..\..\Generated\Validators')" />
  </ItemGroup>

  <ItemGroup>
    <!-- Dependencies needed for contracts -->
    <PackageReference Include="MediatR.Contracts" Version="2.0.1" />
    <PackageReference Include="FluentValidation" Version="11.9.0" Condition="Exists('..\..\Generated\Validators')" />
  </ItemGroup>
</Project>
```

**PetstoreApi.csproj** (Implementation project):
```xml
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>

  <ItemGroup>
    <!-- Reference Contracts package -->
    <ProjectReference Include="..\PetstoreApi.Contracts\PetstoreApi.Contracts.csproj" />
  </ItemGroup>

  <ItemGroup>
    <!-- Dependencies for implementation -->
    <PackageReference Include="MediatR" Version="12.2.0" />
    <PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />
  </ItemGroup>
</Project>
```

**Generator configuration** (Java code):
```java
// In MinimalApiServerCodegen.java
@Override
public void processOpts() {
    super.processOpts();
    
    // Set output directory for generated files
    String generatedOutputPath = outputFolder + "/Generated";
    
    // Set supporting files output (Projects, solutions)
    String projectOutputPath = outputFolder + "/src";
    
    // Configure model output
    modelTemplateFiles.put("model.mustache", ".cs");
    setModelPackage("Models");
    modelOutputDir = generatedOutputPath + "/Models";
    
    // Configure API output
    apiTemplateFiles.put("endpoint.mustache", ".cs");
    setApiPackage("Endpoints");
    apiOutputDir = generatedOutputPath + "/Endpoints";
    
    // Configure validator output (if enabled)
    if (additionalProperties.containsKey("useValidators")) {
        additionalProperties.put("validatorOutputDir", generatedOutputPath + "/Validators");
    }
    
    // Supporting files (projects, solution)
    supportingFiles.add(new SupportingFile("Contracts.csproj.mustache", 
        projectOutputPath + "/{{packageName}}.Contracts", 
        "{{packageName}}.Contracts.csproj"));
    
    supportingFiles.add(new SupportingFile("Implementation.csproj.mustache", 
        projectOutputPath + "/{{packageName}}", 
        "{{packageName}}.csproj"));
}
```

**.gitignore pattern**:
```gitignore
# Generated code (can be regenerated from OpenAPI spec)
Generated/

# But keep solution structure
!src/
!*.sln
```

**Advantages of this approach**:
1. **Clear ownership**: Generated/ = generator-owned, src/ = user-owned
2. **Easy regeneration**: Delete Generated/, re-run generator, no conflicts
3. **Version control**: Can choose to gitignore Generated/ or commit it
4. **IDE support**: Link metadata makes files appear in correct logical location
5. **Maintainability**: Single source of truth for generated code
6. **Scalability**: Easy to add more projects that reference Generated/ files

**Comparison to alternatives**:

| Approach | File Duplication | Clean Regeneration | User Code Safety | Complexity |
|----------|------------------|-------------------|------------------|------------|
| Separate dirs (Option 1) | ❌ Yes | ❌ Hard (conflicts) | ⚠️ Medium | Low |
| src/Shared/ (Option 2) | ✅ No | ✅ Easy | ✅ High | Medium |
| **Generated/ (Option 3)** | **✅ No** | **✅ Easy** | **✅ High** | **Medium** |
| In Contracts/ (Option 4) | ✅ No | ⚠️ Medium | ⚠️ Medium | Low |

**Recommendation**: Generated/ at solution level (Option 3) is the best balance of clarity, safety, and maintainability

---

## Summary of Decisions

### Cross-Project File Sharing (RQ-001)
**Decision**: Use `<Compile Include>` with relative paths and Link metadata

**Key Points**:
- Standard MSBuild pattern, widely supported
- No file duplication on disk
- Link metadata preserves logical structure in Solution Explorer
- Example: `<Compile Include="..\..\Generated\**\*.cs" Link="Models\%(RecursiveDir)%(Filename)%(Extension)" />`

### NuGet Package Metadata (RQ-002)
**Decision**: Use recommended metadata set (not just minimal)

**Required Properties**: PackageId, Version
**Recommended Properties**: Authors, Description, PackageLicenseExpression, RepositoryUrl, PackageTags
**Optional but Useful**: IncludeSymbols, SymbolPackageFormat

**Key Points**:
- Only PackageId and Version are strictly required to avoid errors
- Description, License, Tags strongly recommended for nuget.org discoverability
- Symbol packages (snupkg) enable debugging support

### MediatR Assembly Scanning (RQ-003)
**Decision**: Scan Implementation assembly only (handlers), Contracts assembly doesn't need scanning

**Key Points**:
- `RegisterServicesFromAssembly(typeof(Program).Assembly)` is sufficient
- MediatR finds all IRequestHandler<,> implementations via reflection
- Requests (IRequest<>) don't need DI registration, only handlers do
- Assembly scanning happens once at startup (~50ms), negligible performance cost
- Cross-assembly handler resolution works automatically (handlers can live in different assembly than requests)

### FluentValidation Assembly Scanning (RQ-004)
**Decision**: Use `AddValidatorsFromAssembly(typeof(FirstValidator).Assembly)` with Scoped lifetime

**Key Points**:
- Scans for all AbstractValidator<T> descendants in assembly
- Registers as IValidator<T> → ConcreteValidator in DI
- Use typeof() over Assembly.Load() for type safety
- Default Scoped lifetime is best for web APIs
- Assembly scanning happens once at startup (~50-200ms)

### Unified Source Generation (RQ-005)
**Decision**: Generate to `Generated/` directory at solution level, projects reference via <Compile Include>

**Key Points**:
- Clear separation: Generated/ = throw away, src/ = user-owned
- Single source of truth, no file duplication
- Supports .gitignore for generated files
- Easy to regenerate: delete Generated/, re-run generator
- Directory structure:
  ```
  Generated/
    Models/
    Endpoints/
    Validators/
  src/
    PetstoreApi.Contracts/  (references ../../Generated/)
    PetstoreApi/            (references Contracts project)
  ```

### Implementation Strategy
All decisions are mutually compatible and form a cohesive approach:
1. Generator outputs to `Generated/` directory (RQ-005)
2. Contracts.csproj uses `<Compile Include>` to reference generated files (RQ-001)
3. Implementation.csproj uses `<ProjectReference>` to Contracts.csproj (RQ-001)
4. Both .csproj files include recommended NuGet metadata (RQ-002)
5. Program.cs scans Implementation assembly for handlers (RQ-003)
6. Program.cs scans Contracts assembly for validators (RQ-004)

This architecture supports the goals of Feature 008:
- ✅ Contracts publishable as NuGet package
- ✅ Clear separation of concerns (contracts vs implementation)
- ✅ No code duplication
- ✅ Easy regeneration without conflicts
- ✅ Type-safe, compile-time validation
- ✅ Professional NuGet package metadata
