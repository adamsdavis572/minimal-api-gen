# Quickstart: Configuration Options Fixes

**Feature**: 007-config-fixes  
**Target Users**: Developers using minimal-api-gen to generate ASP.NET Core Minimal APIs

## Overview

This feature fixes three configuration issues:
1. **FluentValidation** now generates working validator classes when `useValidators=true`
2. **Exception Handling** now properly configured when `useGlobalExceptionHandler=true`
3. **Route Groups** - removed unused `useRouteGroups` flag (always enabled)

---

## Quick Start: Enable FluentValidation

### Step 1: Generate Code with Validators

```bash
cd generator
./run-generator.sh --additional-properties useValidators=true
```

### Step 2: Verify Generated Files

Check that validator classes were created:

```bash
ls -la test-output/src/PetstoreApi/Validators/
# Expected output:
# AddPetRequestValidator.cs
# DeletePetRequestValidator.cs
# FindPetsByStatusRequestValidator.cs
# GetPetByIdRequestValidator.cs
# ... (one per operation)
```

### Step 3: Build and Run

```bash
cd test-output
devbox run dotnet build
devbox run dotnet run
```

### Step 4: Test Validation

Send invalid request (missing required field):

```bash
curl -X POST http://localhost:5000/v2/pet \
  -H "Content-Type: application/json" \
  -d '{}'
```

Expected response (HTTP 400):
```json
{
  "type": "https://tools.ietf.org/html/rfc7231#section-6.5.1",
  "title": "One or more validation errors occurred",
  "status": 400,
  "errors": {
    "pet": ["'pet' must not be empty."]
  }
}
```

---

## Quick Start: Enable Exception Handling

### Step 1: Generate with Exception Handler

```bash
cd generator
./run-generator.sh --additional-properties useGlobalExceptionHandler=true,useProblemDetails=true
```

### Step 2: Trigger an Exception

Modify a generated handler to throw an exception:

```csharp
// In test-output/src/PetstoreApi/Handlers/GetPetByIdHandler.cs
public class GetPetByIdHandler : IRequestHandler<GetPetByIdQuery, Pet?>
{
    public Task<Pet?> Handle(GetPetByIdQuery request, CancellationToken cancellationToken)
    {
        throw new InvalidOperationException("Test exception");
    }
}
```

### Step 3: Test Exception Response

```bash
curl -X GET http://localhost:5000/v2/pet/1
```

Expected response (HTTP 500):
```json
{
  "type": "https://tools.ietf.org/html/rfc7231#section-6.6.1",
  "title": "An error occurred while processing your request",
  "status": 500,
  "detail": "Test exception"
}
```

---

## Configuration Matrix

| useValidators | useGlobalExceptionHandler | useProblemDetails | Result |
|---------------|---------------------------|-------------------|--------|
| false | false | false | No validation, no exception handler |
| true | false | false | Validators generated, no exception handler |
| false | true | false | No validation, simple JSON errors |
| false | true | true | No validation, ProblemDetails errors |
| **true** | **true** | **true** | **Full featured** ✅ Recommended |

---

## Migrating Existing Projects

### Before (Broken Validation)

```bash
# Old command (validation infrastructure included but not working)
./run-generator.sh
```

**Result**: FluentValidation packages included, but no validator classes generated, no validation occurs.

### After (Working Validation)

```bash
# New command (validation fully functional)
./run-generator.sh --additional-properties useValidators=true
```

**Result**: Validator classes generated, validation occurs automatically on requests.

**OR** to exclude FluentValidation entirely:

```bash
# Explicitly disable (removes packages)
./run-generator.sh --additional-properties useValidators=false
```

**Result**: No FluentValidation packages, no validator classes, no validation (clean, minimal output).

---

## Advanced: Customize Validator Rules

Currently, validators only check for required parameters using `.NotEmpty()`. To add custom rules:

### Option 1: Extend Generated Validators (Partial Classes)

```csharp
// Create: Validators/AddPetRequestValidator.Custom.cs
using FluentValidation;

namespace PetstoreApi.Validators;

// Extend the generated partial class
public partial class AddPetRequestValidator
{
    partial void CustomRules()
    {
        RuleFor(x => x.pet.Name)
            .Length(1, 50)
            .WithMessage("Pet name must be 1-50 characters");
            
        RuleFor(x => x.pet.Status)
            .Must(status => status == "available" || status == "pending" || status == "sold")
            .WithMessage("Invalid status value");
    }
}
```

### Option 2: Create Custom Validators

```csharp
// Create: Validators/PetValidator.cs
using FluentValidation;
using PetstoreApi.Models;

namespace PetstoreApi.Validators;

public class PetValidator : AbstractValidator<Pet>
{
    public PetValidator()
    {
        RuleFor(x => x.Name)
            .NotEmpty()
            .Length(1, 50);
            
        RuleFor(x => x.PhotoUrls)
            .NotEmpty()
            .Must(urls => urls.Count > 0);
    }
}

// Then inject into request validator:
public class AddPetRequestValidator : AbstractValidator<AddPetRequest>
{
    public AddPetRequestValidator()
    {
        RuleFor(x => x.pet).NotEmpty();
        RuleFor(x => x.pet).SetValidator(new PetValidator());  // ← Add nested validation
    }
}
```

---

## Troubleshooting

### Validators Not Executing

**Symptom**: Invalid requests return 200 OK instead of 400 Bad Request

**Diagnosis**:
1. Check that `useValidators=true` was used during generation
2. Verify `Program.cs` contains `builder.Services.AddValidatorsFromAssemblyContaining<Program>();`
3. Verify FluentValidation packages in .csproj
4. Check validator classes exist in Validators/ directory

**Solution**: Regenerate with `--additional-properties useValidators=true`

---

### Exception Handler Not Working

**Symptom**: Exceptions return HTML error page instead of JSON

**Diagnosis**:
1. Check that `useGlobalExceptionHandler=true` was used
2. Verify `Program.cs` contains `app.UseExceptionHandler(...)`
3. Check middleware order (UseExceptionHandler must come before MapAllEndpoints)

**Solution**: Regenerate with `--additional-properties useGlobalExceptionHandler=true,useProblemDetails=true`

---

### Route Groups Flag Removed

**Symptom**: Old command line includes `useRouteGroups=false` and warnings appear

**Diagnosis**: The flag was removed as dead code (route groups are always enabled)

**Solution**: Remove `useRouteGroups` from your command line arguments. It's ignored but may cause confusion.

---

## Next Steps

- See [validator-examples.md](contracts/validator-examples.md) for generated code samples
- See [exception-handler-examples.md](contracts/exception-handler-examples.md) for middleware examples
- See [CONFIGURATION.md](../../../docs/CONFIGURATION.md) for all configuration options
- See baseline test suite (feature 003) for validation test patterns
