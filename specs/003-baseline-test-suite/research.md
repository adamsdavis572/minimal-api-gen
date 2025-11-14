# Research: Baseline Test Suite and Validation Framework

**Feature**: 003-baseline-test-suite  
**Date**: 2025-11-13  
**Status**: Complete

## Overview

This document captures research findings for building an xUnit-based integration test framework using `WebApplicationFactory` to validate FastEndpoints output. All research questions from Technical Context have been resolved.

## Decisions and Rationale

### Decision 1: xUnit as Test Framework

**Decision**: Use xUnit 2.x as the primary test framework

**Rationale**:
- Industry standard for .NET testing with excellent ASP.NET Core integration
- Native support for `WebApplicationFactory` via `Microsoft.AspNetCore.Mvc.Testing`
- Parallel test execution by default (faster test runs)
- Clean, minimal syntax without unnecessary ceremony
- FluentAssertions integrates seamlessly for readable assertions

**Alternatives Considered**:
- **NUnit**: More verbose syntax, less common in modern .NET projects
- **MSTest**: Less expressive, fewer community extensions
- **Rejected because**: xUnit is the de facto standard for ASP.NET Core testing with best documentation

**References**:
- [xUnit.net Documentation](https://xunit.net/)
- [Microsoft: Integration tests in ASP.NET Core](https://learn.microsoft.com/en-us/aspnet/core/test/integration-tests)

---

### Decision 2: WebApplicationFactory for Integration Testing

**Decision**: Use `Microsoft.AspNetCore.Mvc.Testing.WebApplicationFactory<TEntryPoint>` to host the generated FastEndpoints application in-memory

**Rationale**:
- Official Microsoft pattern for testing ASP.NET Core apps without external servers
- Hosts the application in-memory with full middleware pipeline
- Creates real `HttpClient` instances for actual HTTP requests
- No need for ports, no race conditions, no external dependencies
- Works identically for FastEndpoints and Minimal API (framework-agnostic)

**Alternatives Considered**:
- **TestServer directly**: Lower-level API, more boilerplate required
- **External Kestrel server**: Requires port management, slower, harder to debug
- **Rejected because**: `WebApplicationFactory` is the recommended pattern with minimal setup

**Implementation Pattern**:
```csharp
public class CustomWebApplicationFactory : WebApplicationFactory<Program>
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            // Override services for testing if needed
        });
    }
}
```

**References**:
- [Microsoft: WebApplicationFactory](https://learn.microsoft.com/en-us/aspnet/core/test/integration-tests#basic-tests-with-the-default-webapplicationfactory)

---

### Decision 3: FluentAssertions for Assertion Syntax

**Decision**: Use FluentAssertions library for all test assertions

**Rationale**:
- Highly readable assertion syntax: `result.Should().Be(expected)`
- Better error messages than xUnit's `Assert` methods
- Specialized assertions for HTTP responses, JSON, collections
- Industry standard in .NET testing community
- Makes tests self-documenting

**Alternatives Considered**:
- **xUnit Assert methods**: Less readable, cryptic error messages
- **Shouldly**: Less popular, smaller ecosystem
- **Rejected because**: FluentAssertions is the most mature and widely adopted

**Example Usage**:
```csharp
response.StatusCode.Should().Be(HttpStatusCode.OK);
pet.Name.Should().Be("Fluffy");
pets.Should().HaveCount(3);
```

**References**:
- [FluentAssertions Documentation](https://fluentassertions.com/)

---

### Decision 4: Test Scope - Pet API Only (Minimum Viable)

**Decision**: Focus test suite on Pet API operations only (AddPet, GetPet, UpdatePet, DeletePet) for baseline. Store and User APIs are out of scope for Feature 003.

**Rationale**:
- Pet API demonstrates full CRUD pattern (sufficient for validation)
- Reduces scope to meet "independently testable" principle
- Store/User APIs have identical patterns - no new validation value
- Can add more APIs later if needed (not blocking Feature 004 refactoring)
- Aligns with success criteria: "at least 8 test cases" (4 operations × 2 scenarios = 8)

**Alternatives Considered**:
- **Test all APIs**: 3× more test code, longer test execution, same validation confidence
- **Rejected because**: Diminishing returns - Pet API proves the pattern works

---

### Decision 5: TDD RED-GREEN Pattern with Stubbed Implementations

**Decision**: Write tests first (RED phase), then implement minimal HandleAsync logic in generated endpoints until tests pass (GREEN phase)

**Rationale**:
- Follows Constitution Principle II (Test-Driven Refactoring)
- Ensures tests fail for the right reasons before implementation
- Proves tests are actually validating behavior (not false positives)
- Documents expected API contract through failing tests
- Provides confidence that passing tests mean correct implementation

**Implementation Approach**:
1. Write test expecting `POST /api/pet` to return 201 with created pet
2. Run test → fails with 500 or stub response (RED)
3. Edit generated `AddPetEndpoint.cs` HandleAsync to return mocked pet
4. Run test → passes (GREEN)
5. Repeat for GetPet, UpdatePet, DeletePet

**References**:
- Constitution Principle II: Test-Driven Refactoring

---

### Decision 6: Test Data Strategy - In-Memory Collections

**Decision**: Use static in-memory dictionaries in endpoint handlers for test data storage (no real database)

**Rationale**:
- Simplest approach for baseline validation
- No database setup/teardown overhead
- Tests run faster (no I/O)
- Sufficient to prove HTTP/FastEndpoints/Validation patterns work
- Database integration is out of scope for generator validation

**Implementation Example**:
```csharp
private static readonly Dictionary<long, Pet> PetStore = new();

public override async Task HandleAsync(AddPetRequest req, CancellationToken ct)
{
    var id = PetStore.Count + 1;
    var pet = req.pet with { Id = id };
    PetStore[id] = pet;
    await SendCreatedAtAsync<GetPetEndpoint>(
        new { id },
        pet,
        ct
    );
}
```

**Alternatives Considered**:
- **Real database**: Overkill, slower, requires connection management
- **In-memory EF Core**: Still requires EF setup, unnecessary complexity
- **Rejected because**: Static collections prove the API contract works without database noise

---

### Decision 7: Validation Testing - FluentValidation Integration

**Decision**: Test both happy paths AND unhappy paths where FluentValidation returns 400 Bad Request with error details

**Rationale**:
- Validates FluentValidation is properly registered in generated Program.cs
- Proves request validation works (a key FastEndpoints feature)
- Ensures error responses follow FastEndpoints conventions
- Tests framework integration, not just HTTP routing

**Test Scenarios**:
- Happy: Valid pet → 201 Created
- Unhappy: Pet missing required `name` field → 400 Bad Request with validation errors JSON

**Expected 400 Response**:
```json
{
  "errors": {
    "Name": ["'Name' must not be empty."]
  },
  "type": "https://tools.ietf.org/html/rfc7231#section-6.5.1",
  "title": "One or more validation errors occurred.",
  "status": 400
}
```

**References**:
- [FastEndpoints: Validation](https://fast-endpoints.com/docs/validation)

---

### Decision 8: Test Organization - One Test Class Per API Resource

**Decision**: Create `PetEndpointTests.cs` containing all Pet operation tests (Add, Get, Update, Delete)

**Rationale**:
- Logical grouping by resource (Pet, Store, User)
- Each test class can share setup code (WebApplicationFactory, test data)
- xUnit creates one instance per test method, so shared state is safe
- Easier to navigate test results

**Class Structure**:
```csharp
public class PetEndpointTests : IClassFixture<CustomWebApplicationFactory>
{
    private readonly HttpClient _client;
    
    public PetEndpointTests(CustomWebApplicationFactory factory)
    {
        _client = factory.CreateClient();
    }
    
    [Fact]
    public async Task AddPet_WithValidData_Returns201Created() { ... }
    
    [Fact]
    public async Task AddPet_WithMissingName_Returns400BadRequest() { ... }
    
    // ... more tests
}
```

**Alternatives Considered**:
- **One test class per operation**: Too granular, duplicated setup code
- **One giant test class**: Hard to navigate, slower test discovery
- **Rejected because**: Resource-based grouping balances organization and reuse

---

## Best Practices Applied

### 1. WebApplicationFactory Best Practices

**Pattern**: Use `IClassFixture<CustomWebApplicationFactory>` for test class sharing

**Rationale**:
- xUnit creates one factory instance per test class
- Factory is reused across all test methods in the class
- Application hosted once, multiple HTTP requests via same client
- Faster test execution than creating factory per test

**Anti-pattern to avoid**: Creating factory in test method (slow, wasteful)

**Reference**: [xUnit: Shared Context](https://xunit.net/docs/shared-context)

---

### 2. HTTP Client Management

**Pattern**: Create `HttpClient` in constructor, reuse across tests

**Rationale**:
- `HttpClient` is expensive to create
- `WebApplicationFactory.CreateClient()` configures base URL automatically
- Reusing client is safe because factory is reset between test classes

**Code**:
```csharp
private readonly HttpClient _client;

public PetEndpointTests(CustomWebApplicationFactory factory)
{
    _client = factory.CreateClient();
}
```

---

### 3. Test Naming Convention

**Pattern**: `[Method]_[Scenario]_[ExpectedBehavior]`

**Examples**:
- `AddPet_WithValidData_Returns201Created`
- `AddPet_WithMissingName_Returns400BadRequest`
- `GetPet_WithExistingId_ReturnsPet`
- `GetPet_WithNonExistentId_Returns404NotFound`

**Rationale**:
- Self-documenting test names
- Readable in test explorer
- Clearly communicates intent

---

### 4. Arrange-Act-Assert Pattern

**Pattern**: Structure every test method with clear AAA sections

**Example**:
```csharp
[Fact]
public async Task AddPet_WithValidData_Returns201Created()
{
    // Arrange
    var pet = new Pet { Name = "Fluffy", Status = "available" };
    var content = new StringContent(JsonSerializer.Serialize(pet), Encoding.UTF8, "application/json");
    
    // Act
    var response = await _client.PostAsync("/api/pet", content);
    
    // Assert
    response.StatusCode.Should().Be(HttpStatusCode.Created);
    var createdPet = await response.Content.ReadFromJsonAsync<Pet>();
    createdPet.Name.Should().Be("Fluffy");
    createdPet.Id.Should().BeGreaterThan(0);
}
```

---

### 5. Async All The Way

**Pattern**: All test methods are `async Task`, all I/O is `await`ed

**Rationale**:
- HTTP requests are inherently async
- xUnit supports async tests natively
- Avoids blocking threads, enables parallel execution
- Matches real-world usage patterns

---

## Technology Stack Summary

| Technology | Version | Purpose |
|-----------|---------|---------|
| xUnit | 2.x | Test framework |
| Microsoft.AspNetCore.Mvc.Testing | 8.0.x | WebApplicationFactory |
| FluentAssertions | 6.x | Assertion library |
| .NET SDK | 8.0 | Runtime (via devbox) |
| FastEndpoints | 5.29.0 | Generated project dependency |
| OpenAPI Generator | 7.x | Code generation (from Feature 002) |
| devbox | latest | Build environment |

---

## Implementation Sequence

Based on research, the implementation order is:

1. **Generate FastEndpoints Project**
   - Build generator: `devbox run mvn clean package`
   - Run generation: `java -cp ... generate -g aspnetcore-minimalapi -i petstore.oas -o test-output`
   - Compile: `cd test-output/src/PetstoreApi && devbox run dotnet build`

2. **Create Test Project**
   - `devbox run dotnet new xunit -n PetstoreApi.Tests -o test-output/tests/PetstoreApi.Tests`
   - Add packages: `Microsoft.AspNetCore.Mvc.Testing`, `FluentAssertions`
   - Add project reference to generated PetstoreApi project

3. **Create CustomWebApplicationFactory**
   - Extend `WebApplicationFactory<Program>`
   - Make Program class accessible (partial class with public visibility if needed)

4. **Write Pet Tests (RED Phase)**
   - `AddPet_WithValidData_Returns201Created` → FAIL
   - `AddPet_WithMissingName_Returns400BadRequest` → FAIL
   - `GetPet_WithExistingId_ReturnsPet` → FAIL
   - `GetPet_WithNonExistentId_Returns404NotFound` → FAIL
   - `UpdatePet_WithValidData_Returns200OK` → FAIL
   - `UpdatePet_WithNonExistentId_Returns404NotFound` → FAIL
   - `DeletePet_WithExistingId_Returns204NoContent` → FAIL
   - `DeletePet_WithNonExistentId_Returns404NotFound` → FAIL

5. **Implement Endpoint Logic (GREEN Phase)**
   - Edit generated `PetApiEndpoint.cs` to add in-memory storage logic
   - Run tests → should pass
   - Document pass/fail status for each test

6. **Validate Success Criteria**
   - Build time <1 minute ✓
   - Test setup <2 minutes ✓
   - 8+ test cases ✓
   - All tests pass ✓
   - Test execution <30 seconds ✓

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Generated Program.cs not accessible | Tests can't reference it | Make Program class public via partial class |
| FluentValidation not registered | Validation tests fail | Verify Program.cs has `AddValidatorsFromAssemblyContaining<Program>()` |
| Generated endpoints missing HandleAsync | Tests fail | Confirm Feature 002 templates include HandleAsync stubs |
| Test execution slow | Poor developer experience | Use WebApplicationFactory (in-memory), avoid external I/O |

---

## Open Questions (None Remaining)

All NEEDS CLARIFICATION items from Technical Context have been resolved:
- ✅ Testing framework: xUnit
- ✅ Integration testing approach: WebApplicationFactory
- ✅ Assertion library: FluentAssertions
- ✅ Test scope: Pet API only (CRUD operations)
- ✅ Data storage strategy: In-memory static collections
- ✅ TDD pattern: RED (tests first) → GREEN (implement) → Document

---

## References

1. [xUnit.net Documentation](https://xunit.net/)
2. [Microsoft: Integration tests in ASP.NET Core](https://learn.microsoft.com/en-us/aspnet/core/test/integration-tests)
3. [Microsoft: WebApplicationFactory](https://learn.microsoft.com/en-us/aspnet/core/test/integration-tests#basic-tests-with-the-default-webapplicationfactory)
4. [FluentAssertions Documentation](https://fluentassertions.com/)
5. [FastEndpoints: Validation](https://fast-endpoints.com/docs/validation)
6. [xUnit: Shared Context](https://xunit.net/docs/shared-context)
