# Example: Generated Validator Class with useValidators=true

**File**: Validators/AddPetRequestValidator.cs  
**Generated From**: POST /pet operation with required body parameter

```csharp
using FluentValidation;
using PetstoreApi.Models;

namespace PetstoreApi.Validators;

public class AddPetRequestValidator : AbstractValidator<AddPetRequest>
{
    public AddPetRequestValidator()
    {
        RuleFor(x => x.pet).NotEmpty();
    }
}
```

---

**File**: Validators/DeletePetRequestValidator.cs  
**Generated From**: DELETE /pet/{petId} operation with required path parameter

```csharp
using FluentValidation;
using PetstoreApi.Models;

namespace PetstoreApi.Validators;

public class DeletePetRequestValidator : AbstractValidator<DeletePetRequest>
{
    public DeletePetRequestValidator()
    {
        RuleFor(x => x.PetId).NotEmpty();
    }
}
```

---

**File**: Validators/FindPetsByStatusRequestValidator.cs  
**Generated From**: GET /pet/findByStatus with required query parameter

```csharp
using FluentValidation;
using PetstoreApi.Models;

namespace PetstoreApi.Validators;

public class FindPetsByStatusRequestValidator : AbstractValidator<FindPetsByStatusRequest>
{
    public FindPetsByStatusRequestValidator()
    {
        RuleFor(x => x.Status).NotEmpty();
    }
}
```

---

## Modified Files When useValidators=true

**File**: Program.cs (with useValidators=true)

```csharp
using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using FluentValidation;                                    // ← Added
using PetstoreApi;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddValidatorsFromAssemblyContaining<Program>();  // ← Added
builder.Services.AddProblemDetails();

var app = builder.Build();

app.UseExceptionHandler();
app.MapAllEndpoints();

app.Run();
```

**File**: PetstoreApi.csproj (with useValidators=true)

```xml
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
  </PropertyGroup>

  <ItemGroup>
    <PackageReference Include="FluentValidation" Version="11.9.0" />
    <PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.9.0" />
  </ItemGroup>
</Project>
```

---

## Contrast: Generated Code with useValidators=false

**File**: Program.cs (with useValidators=false)

```csharp
using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
// No FluentValidation import
using PetstoreApi;

var builder = WebApplication.CreateBuilder(args);

// No validator registration
builder.Services.AddProblemDetails();

var app = builder.Build();

app.UseExceptionHandler();
app.MapAllEndpoints();

app.Run();
```

**File**: PetstoreApi.csproj (with useValidators=false)

```xml
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
  </PropertyGroup>

  <ItemGroup>
    <!-- No FluentValidation packages -->
  </ItemGroup>
</Project>
```

**Validators/ Directory**: Does not exist (no validator classes generated)
