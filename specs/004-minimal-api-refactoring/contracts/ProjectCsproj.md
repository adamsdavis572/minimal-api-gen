# Contract: project.csproj.mustache → PetstoreApi.csproj

**Template**: `project.csproj.mustache` (MODIFY - Infrastructure)  
**Output**: `test-output/src/PetstoreApi/PetstoreApi.csproj`  
**Status**: MODIFY (80% framework-specific, 20% reusable)

---

## Transformation Specification

### FROM: FastEndpoints Dependencies
```xml
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <Nullable>enable</Nullable>
  </PropertyGroup>

  <ItemGroup>
    <PackageReference Include="FastEndpoints" Version="5.29.0" />
    <PackageReference Include="FastEndpoints.Swagger" Version="5.29.0" />
  </ItemGroup>
</Project>
```

### TO: Minimal API Dependencies
```xml
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
  </PropertyGroup>

  <ItemGroup>
    <PackageReference Include="Swashbuckle.AspNetCore" Version="6.5.0" />
    <PackageReference Include="FluentValidation" Version="11.9.0" />
    <PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />
    {{#useAuthentication}}
    <PackageReference Include="Microsoft.AspNetCore.Authentication.JwtBearer" Version="8.0.0" />
    {{/useAuthentication}}
  </ItemGroup>
</Project>
```

---

## Changes Required

### 1. Remove FastEndpoints Packages
```xml
<!-- DELETE -->
<PackageReference Include="FastEndpoints" Version="5.29.0" />
<PackageReference Include="FastEndpoints.Swagger" Version="5.29.0" />
```

### 2. Add Minimal API Packages
```xml
<!-- ADD -->
<PackageReference Include="Swashbuckle.AspNetCore" Version="6.5.0" />
<PackageReference Include="FluentValidation" Version="11.9.0" />
<PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />
```

### 3. Enable ImplicitUsings (Optional)
```xml
<PropertyGroup>
  <ImplicitUsings>enable</ImplicitUsings>
</PropertyGroup>
```

This reduces boilerplate `using` statements in generated files.

### 4. Conditional Authentication Package
```xml
{{#useAuthentication}}
<PackageReference Include="Microsoft.AspNetCore.Authentication.JwtBearer" Version="8.0.0" />
{{/useAuthentication}}
```

---

## Template Variables

| Variable | Type | Default | Example | Purpose |
|----------|------|---------|---------|---------|
| `{{packageName}}` | String | Required | `"PetstoreApi"` | Assembly name |
| `{{targetFramework}}` | String | `"net8.0"` | `"net8.0"` | .NET version |
| `{{useAuthentication}}` | Boolean | `false` | `false` | Include JWT package |
| `{{nullable}}` | String | `"enable"` | `"enable"` | Nullable reference types |
| `{{implicitUsings}}` | String | `"enable"` | `"enable"` | Implicit using statements |

---

## Package Version Constraints

| Package | Minimum Version | Recommended | Reason |
|---------|----------------|-------------|--------|
| `Swashbuckle.AspNetCore` | 6.5.0 | 6.5.0 | OpenAPI 3.0 support, .NET 8 compatibility |
| `FluentValidation` | 11.9.0 | 11.9.0 | Async validation, .NET 8 compatibility |
| `FluentValidation.DependencyInjectionExtensions` | 11.9.0 | 11.9.0 | `AddValidatorsFromAssemblyContaining<T>()` support |
| `Microsoft.AspNetCore.Authentication.JwtBearer` | 8.0.0 | 8.0.0 | .NET 8 framework alignment |

**CVE Check**: All packages validated via `appmod-validate-cve` tool (no known vulnerabilities).

---

## PropertyGroup Settings

```xml
<PropertyGroup>
  <TargetFramework>net8.0</TargetFramework>
  <Nullable>enable</Nullable>
  <ImplicitUsings>enable</ImplicitUsings>
  <OutputType>Exe</OutputType>
  <RootNamespace>{{packageName}}</RootNamespace>
</PropertyGroup>
```

**Key Settings**:
- `Nullable=enable`: Enforce null safety (matches Feature 003 baseline)
- `ImplicitUsings=enable`: Reduce boilerplate (System, System.Collections.Generic, etc.)
- `OutputType=Exe`: Executable application
- `RootNamespace`: Matches `{{packageName}}` variable

---

## Conditional Blocks

### Development-Only Packages
```xml
<ItemGroup Condition="'$(Configuration)' == 'Debug'">
  <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="8.0.0" />
</ItemGroup>
```

Not currently required but useful for OpenAPI tooling.

---

## Dependencies

**Framework SDKs**:
- `Microsoft.NET.Sdk.Web` (included with .NET 8.0 SDK)

**Implicit References** (via ImplicitUsings):
- `Microsoft.AspNetCore.Builder`
- `Microsoft.AspNetCore.Hosting`
- `Microsoft.AspNetCore.Http`
- `Microsoft.Extensions.DependencyInjection`

**Explicit References** (always required):
- `Swashbuckle.AspNetCore`
- `FluentValidation` + DI extensions

---

## Validation Rules

1. **Version Alignment**:
   - All `Microsoft.*` packages must use version `8.0.0`
   - FluentValidation packages must use same version (`11.9.0`)

2. **Package Compatibility**:
   - Swashbuckle 6.5.0 compatible with .NET 8.0
   - FluentValidation 11.9.0 supports async validation

3. **Build Validation**:
   - Must restore without errors: `devbox run dotnet restore`
   - Must build without warnings: `devbox run dotnet build`

---

## Expected Output

**File**: `test-output/src/PetstoreApi/PetstoreApi.csproj`

**Characteristics**:
- 20-30 lines (without authentication)
- Minimal API packages only (no FastEndpoints)
- .NET 8.0 target framework
- Nullable enabled
- ImplicitUsings enabled

**Example**:
```xml
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <RootNamespace>PetstoreApi</RootNamespace>
  </PropertyGroup>

  <ItemGroup>
    <PackageReference Include="Swashbuckle.AspNetCore" Version="6.5.0" />
    <PackageReference Include="FluentValidation" Version="11.9.0" />
    <PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />
  </ItemGroup>
</Project>
```

---

## TDD Verification

**Build Test**:
```bash
cd test-output/src/PetstoreApi
devbox run dotnet restore
devbox run dotnet build
```

Expected: ✅ Build succeeded, 0 warnings

**Package Test**:
```bash
devbox run dotnet list package
```

Expected:
- Swashbuckle.AspNetCore (6.5.0)
- FluentValidation (11.9.0)
- FluentValidation.DependencyInjectionExtensions (11.9.0)
- NO FastEndpoints packages

**Integration Test**:
```bash
cd test-output/tests/PetstoreApi.Tests
devbox run dotnet test
```

Expected: ✅ 7 tests passed (from Feature 003 baseline)

---

## Migration Notes

**Breaking Changes**:
- FastEndpoints' automatic validation removed → manual FluentValidation required
- FastEndpoints' Swagger removed → Swashbuckle Swagger required
- Endpoint classes removed → route group functions required

**Non-Breaking Changes**:
- Model classes unchanged (100% reusable)
- Validator classes unchanged (70% reusable with minor syntax changes)
- Test classes unchanged (100% reusable)
