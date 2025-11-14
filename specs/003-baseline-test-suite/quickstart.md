# Quickstart: Baseline Test Suite

**Feature**: 003-baseline-test-suite  
**Last Updated**: 2025-11-13

This guide walks you through building the OpenAPI Generator, generating a FastEndpoints project, creating a test suite, and running TDD validation (RED-GREEN workflow).

---

## Prerequisites

**Required Tools**:
- **devbox**: Development environment manager ([install guide](https://www.jetpack.io/devbox/docs/installing_devbox/))
- **Git**: Version control (for cloning repository)

**Packages Installed via devbox** (no manual installation needed):
- Java 11 (JDK)
- Maven 3.8.9+
- .NET SDK 8.0

**Verify devbox Installation**:
```bash
devbox version
# Output: devbox 0.x.x
```

---

## Step 1: Clone Repository and Navigate to Project

```bash
# Clone the repository
git clone https://github.com/your-org/minimal-api-gen.git
cd minimal-api-gen

# Checkout Feature 003 branch
git checkout 003-baseline-test-suite
```

---

## Step 2: Build OpenAPI Generator

**Location**: `generator/` directory

**Command**:
```bash
cd generator
devbox run mvn clean package
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  2.4 s
[INFO] Finished at: 2025-11-13T10:00:00Z
```

**Success Criteria**: 
- ✅ Build completes in <1 minute (SC-001)
- ✅ JAR file created: `target/aspnet-minimalapi-openapi-generator-1.0.0.jar`

**Troubleshooting**:
- If `mvn: command not found`: Ensure you ran `devbox run mvn` (not direct `mvn`)
- If Java version error: devbox will automatically use JDK 11 from `devbox.json`

---

## Step 3: Generate FastEndpoints Project

**Input**: OpenAPI specification for Petstore API  
**Output**: FastEndpoints project in `test-output/src/PetstoreApi/`

**Command** (from repository root):
```bash
cd ..  # back to repository root
java -cp generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar:$HOME/.m2/repository/org/openapitools/openapi-generator-cli/7.0.1/openapi-generator-cli-7.0.1.jar \
  org.openapitools.codegen.OpenAPIGenerator generate \
  -g aspnetcore-minimalapi \
  -i https://raw.githubusercontent.com/openapitools/openapi-generator/master/modules/openapi-generator/src/test/resources/3_0/petstore.yaml \
  -o test-output \
  --additional-properties=packageName=PetstoreApi
```

**Expected Output**:
```
Successfully generated code to /path/to/test-output
Models written to test-output/src/PetstoreApi/Models
Endpoints written to test-output/src/PetstoreApi/Endpoints
...
```

**Generated Files** (key files):
```
test-output/
└── src/
    └── PetstoreApi/
        ├── PetstoreApi.csproj
        ├── Program.cs
        ├── Models/
        │   └── Pet.cs
        ├── Endpoints/
        │   └── PetApiEndpoint.cs
        └── Validators/
            └── AddPetRequestValidator.cs
```

---

## Step 4: Compile Generated FastEndpoints Project

**Location**: `test-output/src/PetstoreApi/`

**Command**:
```bash
cd test-output/src/PetstoreApi
devbox run dotnet build
```

**Expected Output**:
```
Build succeeded.
    0 Warning(s)
    0 Error(s)

Time Elapsed 00:00:03.45
```

**Success Criteria**:
- ✅ Build succeeds with 0 errors (SC-003)
- ✅ Warnings are acceptable (FastEndpoints may emit nullable warnings)
- ✅ DLL created: `bin/Debug/net8.0/PetstoreApi.dll`

**Troubleshooting**:
- If `dotnet: command not found`: Ensure you ran `devbox run dotnet` (not direct `dotnet`)
- If target framework error: Verify `devbox.json` includes `dotnet-sdk_8` package

---

## Step 5: Make Program Class Accessible for Testing

**Why**: `WebApplicationFactory<Program>` needs to reference the `Program` class

**File**: `test-output/src/PetstoreApi/Program.cs`

**Edit**: Add the following at the bottom of `Program.cs`:
```csharp
// Make Program class accessible for integration tests
public partial class Program { }
```

**Verification**:
```bash
# Rebuild to ensure no syntax errors
devbox run dotnet build
```

---

## Step 6: Create xUnit Test Project

**Location**: `test-output/tests/PetstoreApi.Tests/`

**Command** (from repository root):
```bash
cd ../../..  # back to repository root
devbox run dotnet new xunit -n PetstoreApi.Tests -o test-output/tests/PetstoreApi.Tests
```

**Expected Output**:
```
The template "xUnit Test Project" was created successfully.
```

**Generated Files**:
```
test-output/tests/PetstoreApi.Tests/
├── PetstoreApi.Tests.csproj
├── UnitTest1.cs  (delete this later)
└── Usings.cs
```

---

## Step 7: Add NuGet Packages

**Required Packages**:
1. `Microsoft.AspNetCore.Mvc.Testing` - provides `WebApplicationFactory`
2. `FluentAssertions` - provides readable assertions

**Commands** (from repository root):
```bash
cd test-output/tests/PetstoreApi.Tests

# Add WebApplicationFactory package
devbox run dotnet add package Microsoft.AspNetCore.Mvc.Testing --version 8.0.0

# Add FluentAssertions package
devbox run dotnet add package FluentAssertions --version 6.12.0
```

**Expected Output** (per package):
```
info : PackageReference for 'Microsoft.AspNetCore.Mvc.Testing' version '8.0.0' added to file '...csproj'.
```

**Verification**:
```bash
# View installed packages
devbox run dotnet list package
```

---

## Step 8: Add Project Reference to PetstoreApi

**Why**: Test project needs to reference the FastEndpoints project to access `Program` class

**Command** (from `test-output/tests/PetstoreApi.Tests/`):
```bash
devbox run dotnet add reference ../../src/PetstoreApi/PetstoreApi.csproj
```

**Expected Output**:
```
Reference `../../src/PetstoreApi/PetstoreApi.csproj` added to the project.
```

---

## Step 9: Create CustomWebApplicationFactory

**File**: `test-output/tests/PetstoreApi.Tests/CustomWebApplicationFactory.cs`

**Content**:
```csharp
using Microsoft.AspNetCore.Mvc.Testing;

namespace PetstoreApi.Tests;

public class CustomWebApplicationFactory : WebApplicationFactory<Program>
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        // No service overrides needed for baseline tests
        // Future: can override database, auth, logging, etc.
    }
}
```

**Explanation**:
- Extends `WebApplicationFactory<Program>` from `Microsoft.AspNetCore.Mvc.Testing`
- Hosts the FastEndpoints app in-memory for testing
- `ConfigureWebHost` allows test-specific service registration (unused here)

---

## Step 10: Delete Default Test File

**Command**:
```bash
rm UnitTest1.cs
```

**Why**: We'll create `PetEndpointTests.cs` with actual test cases

---

## Step 11: Create Pet Endpoint Tests

**File**: `test-output/tests/PetstoreApi.Tests/PetEndpointTests.cs`

**Content** (copy from `specs/003-baseline-test-suite/contracts/*.md` test code):

```csharp
using System.Net;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using FluentAssertions;
using Xunit;

namespace PetstoreApi.Tests;

public class PetEndpointTests : IClassFixture<CustomWebApplicationFactory>
{
    private readonly HttpClient _client;

    public PetEndpointTests(CustomWebApplicationFactory factory)
    {
        _client = factory.CreateClient();
    }

    // ==================== AddPet Tests ====================

    [Fact]
    public async Task AddPet_WithValidData_Returns201Created()
    {
        // Arrange
        var pet = new { name = "Fluffy", photoUrls = new[] { "http://example.com/fluffy.jpg" }, status = "available" };
        var content = new StringContent(JsonSerializer.Serialize(pet), Encoding.UTF8, "application/json");

        // Act
        var response = await _client.PostAsync("/api/pet", content);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Created);
        var createdPet = await response.Content.ReadFromJsonAsync<JsonElement>();
        createdPet.GetProperty("name").GetString().Should().Be("Fluffy");
        createdPet.GetProperty("id").GetInt64().Should().BeGreaterThan(0);
    }

    [Fact]
    public async Task AddPet_WithMissingName_Returns400BadRequest()
    {
        // Arrange
        var pet = new { photoUrls = new[] { "http://example.com/photo.jpg" }, status = "available" };
        var content = new StringContent(JsonSerializer.Serialize(pet), Encoding.UTF8, "application/json");

        // Act
        var response = await _client.PostAsync("/api/pet", content);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
        var error = await response.Content.ReadFromJsonAsync<JsonElement>();
        error.GetProperty("errors").GetProperty("Name").GetArrayLength().Should().BeGreaterThan(0);
    }

    // ==================== GetPet Tests ====================

    [Fact]
    public async Task GetPet_WithExistingId_ReturnsPet()
    {
        // Arrange - first create a pet
        var pet = new { name = "Buddy", photoUrls = new[] { "http://example.com/buddy.jpg" }, status = "available" };
        var createResponse = await _client.PostAsync("/api/pet", new StringContent(JsonSerializer.Serialize(pet), Encoding.UTF8, "application/json"));
        var createdPet = await createResponse.Content.ReadFromJsonAsync<JsonElement>();
        var petId = createdPet.GetProperty("id").GetInt64();

        // Act
        var response = await _client.GetAsync($"/api/pet/{petId}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var retrievedPet = await response.Content.ReadFromJsonAsync<JsonElement>();
        retrievedPet.GetProperty("id").GetInt64().Should().Be(petId);
        retrievedPet.GetProperty("name").GetString().Should().Be("Buddy");
    }

    [Fact]
    public async Task GetPet_WithNonExistentId_Returns404NotFound()
    {
        // Arrange
        var nonExistentId = 999999;

        // Act
        var response = await _client.GetAsync($"/api/pet/{nonExistentId}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }

    // ==================== UpdatePet Tests ====================

    [Fact]
    public async Task UpdatePet_WithValidData_Returns200OK()
    {
        // Arrange - create a pet first
        var pet = new { name = "Max", photoUrls = new[] { "http://example.com/max.jpg" }, status = "available" };
        var createResponse = await _client.PostAsync("/api/pet", new StringContent(JsonSerializer.Serialize(pet), Encoding.UTF8, "application/json"));
        var createdPet = await createResponse.Content.ReadFromJsonAsync<JsonElement>();
        var petId = createdPet.GetProperty("id").GetInt64();

        // Act - update the pet
        var updatedPet = new { id = petId, name = "Max Updated", photoUrls = new[] { "http://example.com/max.jpg" }, status = "sold" };
        var updateContent = new StringContent(JsonSerializer.Serialize(updatedPet), Encoding.UTF8, "application/json");
        var response = await _client.PutAsync("/api/pet", updateContent);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var result = await response.Content.ReadFromJsonAsync<JsonElement>();
        result.GetProperty("name").GetString().Should().Be("Max Updated");
        result.GetProperty("status").GetString().Should().Be("sold");
    }

    [Fact]
    public async Task UpdatePet_WithNonExistentId_Returns404NotFound()
    {
        // Arrange
        var nonExistentPet = new { id = 999999, name = "Ghost", photoUrls = new[] { "http://example.com/ghost.jpg" }, status = "available" };
        var content = new StringContent(JsonSerializer.Serialize(nonExistentPet), Encoding.UTF8, "application/json");

        // Act
        var response = await _client.PutAsync("/api/pet", content);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }

    // ==================== DeletePet Tests ====================

    [Fact]
    public async Task DeletePet_WithExistingId_Returns204NoContent()
    {
        // Arrange - create a pet first
        var pet = new { name = "Rex", photoUrls = new[] { "http://example.com/rex.jpg" }, status = "available" };
        var createResponse = await _client.PostAsync("/api/pet", new StringContent(JsonSerializer.Serialize(pet), Encoding.UTF8, "application/json"));
        var createdPet = await createResponse.Content.ReadFromJsonAsync<JsonElement>();
        var petId = createdPet.GetProperty("id").GetInt64();

        // Act
        var response = await _client.DeleteAsync($"/api/pet/{petId}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NoContent);
    }

    [Fact]
    public async Task DeletePet_WithNonExistentId_Returns404NotFound()
    {
        // Arrange
        var nonExistentId = 999999;

        // Act
        var response = await _client.DeleteAsync($"/api/pet/{nonExistentId}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }
}
```

**Test Count**: 8 tests (meets SC-004: ≥8 test cases)
- 4 Happy Path tests (valid input → success)
- 4 Unhappy Path tests (invalid input → error)

---

## Step 12: Build Test Project

**Command** (from `test-output/tests/PetstoreApi.Tests/`):
```bash
devbox run dotnet build
```

**Expected Output**:
```
Build succeeded.
    0 Error(s)

Time Elapsed 00:00:02.12
```

**Success Criteria**:
- ✅ Test project builds without errors (SC-002: setup <2 minutes)
- ✅ Test discovery finds 8 tests

---

## Step 13: Run Tests - RED Phase (Expect Failures)

**Why**: Tests should fail initially because endpoints are stubbed (TDD RED phase)

**Command** (from `test-output/tests/PetstoreApi.Tests/`):
```bash
devbox run dotnet test --logger "console;verbosity=detailed"
```

**Expected Output** (all tests fail):
```
Failed AddPet_WithValidData_Returns201Created [10 ms]
  Expected status code 201, but got 500
Failed AddPet_WithMissingName_Returns400BadRequest [8 ms]
  Expected status code 400, but got 500
... (6 more failures)

Failed!  - Failed:     8, Passed:     0, Skipped:     0, Total:     8
Time Elapsed 00:00:10.234s
```

**Success Criteria**:
- ✅ All 8 tests **FAIL** (proves tests are validating behavior)
- ✅ Typical failures: 500 Internal Server Error or NotImplementedException
- ✅ Execution time <30 seconds (SC-007)

**Key Insight**: Failing tests prove validation works (not false positives)

---

## Step 14: Implement Endpoint Logic - GREEN Phase

**Location**: `test-output/src/PetstoreApi/Endpoints/PetApiEndpoint.cs` (or similar)

**Add In-Memory Storage** (at class level):
```csharp
private static readonly Dictionary<long, Pet> PetStore = new();
private static long _nextId = 1;
```

**Implement AddPet HandleAsync**:
```csharp
public override async Task HandleAsync(AddPetRequest req, CancellationToken ct)
{
    var pet = req.Pet with { Id = _nextId++ };
    PetStore[pet.Id] = pet;
    
    await SendCreatedAtAsync<GetPetEndpoint>(
        new { id = pet.Id },
        pet,
        cancellation: ct
    );
}
```

**Implement GetPet HandleAsync**:
```csharp
public override async Task HandleAsync(GetPetByIdRequest req, CancellationToken ct)
{
    if (!PetStore.TryGetValue(req.PetId, out var pet))
    {
        await SendNotFoundAsync(ct);
        return;
    }
    
    await SendOkAsync(pet, ct);
}
```

**Implement UpdatePet HandleAsync**:
```csharp
public override async Task HandleAsync(UpdatePetRequest req, CancellationToken ct)
{
    if (!PetStore.ContainsKey(req.Pet.Id))
    {
        await SendNotFoundAsync(ct);
        return;
    }
    
    PetStore[req.Pet.Id] = req.Pet;
    await SendOkAsync(req.Pet, ct);
}
```

**Implement DeletePet HandleAsync**:
```csharp
public override async Task HandleAsync(DeletePetRequest req, CancellationToken ct)
{
    if (!PetStore.Remove(req.PetId))
    {
        await SendNotFoundAsync(ct);
        return;
    }
    
    await SendNoContentAsync(ct);
}
```

**Rebuild PetstoreApi**:
```bash
cd ../../src/PetstoreApi
devbox run dotnet build
```

---

## Step 15: Run Tests - GREEN Phase (Expect Success)

**Command** (from `test-output/tests/PetstoreApi.Tests/`):
```bash
cd ../../tests/PetstoreApi.Tests
devbox run dotnet test --logger "console;verbosity=detailed"
```

**Expected Output** (all tests pass):
```
Passed AddPet_WithValidData_Returns201Created [45 ms]
Passed AddPet_WithMissingName_Returns400BadRequest [32 ms]
Passed GetPet_WithExistingId_ReturnsPet [28 ms]
Passed GetPet_WithNonExistentId_Returns404NotFound [15 ms]
Passed UpdatePet_WithValidData_Returns200OK [38 ms]
Passed UpdatePet_WithNonExistentId_Returns404NotFound [18 ms]
Passed DeletePet_WithExistingId_Returns204NoContent [42 ms]
Passed DeletePet_WithNonExistentId_Returns404NotFound [12 ms]

Passed!  - Failed:     0, Passed:     8, Skipped:     0, Total:     8
Time Elapsed 00:00:05.432s
```

**Success Criteria**:
- ✅ All 8 tests **PASS** (SC-005: 100% pass rate)
- ✅ Execution time <30 seconds (SC-007: typically 5-10s)

---

## Step 16: Validate Success Criteria

**SC-001: Generator builds in <1 minute**
- Measured in Step 2: `time (cd generator && devbox run mvn clean package)`
- Expected: <60 seconds

**SC-002: Test project setup <2 minutes**
- Measured from Step 6 (create test project) through Step 12 (build test project)
- Expected: <120 seconds

**SC-003: Generated project compiles**
- Verified in Step 4: `dotnet build` with 0 errors
- Expected: ✅ Pass

**SC-004: At least 8 test cases**
- Count tests in Step 11: `PetEndpointTests.cs` has 8 `[Fact]` methods
- Expected: ≥8 ✅

**SC-005: 100% test pass rate**
- Verified in Step 15: 8/8 tests passed
- Expected: 100% ✅

**SC-006: All CRUD operations covered**
- AddPet (POST): 2 tests ✅
- GetPet (GET): 2 tests ✅
- UpdatePet (PUT): 2 tests ✅
- DeletePet (DELETE): 2 tests ✅
- Expected: All 4 operations ✅

**SC-007: Test execution <30 seconds**
- Measured in Step 15: `Time Elapsed 00:00:05.432s`
- Expected: <30s ✅

---

## Summary

**What You Built**:
1. ✅ OpenAPI Generator (custom aspnetcore-minimalapi generator)
2. ✅ Generated FastEndpoints project (PetstoreApi)
3. ✅ xUnit test project with WebApplicationFactory
4. ✅ 8 integration tests (RED → GREEN TDD workflow)
5. ✅ In-memory CRUD implementation for Pet API

**TDD Workflow Proven**:
- **RED Phase**: All 8 tests failed (proves validation works)
- **GREEN Phase**: All 8 tests passed (proves implementation correct)

**Performance**:
- Generator build: <1 minute
- Test setup: <2 minutes
- Test execution: <30 seconds

**Next Steps** (Future Features):
- Add Store and User API tests
- Add database integration (Entity Framework Core)
- Add authentication/authorization tests
- Add performance/load tests

---

## Troubleshooting

### Issue: `devbox: command not found`
**Solution**: Install devbox: `curl -fsSL https://get.jetpack.io/devbox | bash`

### Issue: `dotnet: command not found` (even with devbox run)
**Solution**: Verify `generator/devbox.json` includes `"dotnet-sdk_8"` in packages array

### Issue: Tests fail with "Program class not found"
**Solution**: Ensure Step 5 completed (add `public partial class Program {}` to Program.cs)

### Issue: Tests fail with FluentValidation errors not returned
**Solution**: Verify `Program.cs` has `builder.Services.AddValidatorsFromAssemblyContaining<Program>();`

### Issue: Test execution >30 seconds
**Solution**: Run tests with `--no-build` flag: `devbox run dotnet test --no-build`

---

## References

- [devbox Documentation](https://www.jetpack.io/devbox/docs/)
- [xUnit Documentation](https://xunit.net/)
- [Microsoft: Integration tests in ASP.NET Core](https://learn.microsoft.com/en-us/aspnet/core/test/integration-tests)
- [FastEndpoints Documentation](https://fast-endpoints.com/docs)
- [FluentAssertions Documentation](https://fluentassertions.com/)
