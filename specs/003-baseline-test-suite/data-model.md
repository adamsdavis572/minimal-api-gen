# Data Model: Baseline Test Suite Entities

**Feature**: 003-baseline-test-suite  
**Date**: 2025-11-13  
**Status**: Design Complete

## Overview

This document defines the test entities and their relationships for the baseline test suite that validates generated FastEndpoints projects using xUnit and `WebApplicationFactory`.

---

## Entity Definitions

### Entity 1: `CustomWebApplicationFactory`

**Purpose**: Test host factory for hosting the generated FastEndpoints application in-memory during integration tests

**Type**: C# class extending `WebApplicationFactory<Program>`

**Properties**:
| Property | Type | Description |
|----------|------|-------------|
| _(inherited from base)_ | `WebApplicationFactory<Program>` | Provides `CreateClient()`, `CreateDefaultClient()`, etc. |

**Methods**:
| Method | Signature | Description |
|--------|-----------|-------------|
| `ConfigureWebHost` | `protected override void ConfigureWebHost(IWebHostBuilder builder)` | Override point for test-specific service configuration (unused in baseline) |

**Lifecycle**:
- Created once per test class via xUnit's `IClassFixture<CustomWebApplicationFactory>`
- Shared across all test methods in the class
- Disposed after all tests in the class complete

**Relationships**:
- **Used by**: All test classes (e.g., `PetEndpointTests`) via constructor injection
- **Creates**: `HttpClient` instances for making HTTP requests to in-memory app

**Example Usage**:
```csharp
public class CustomWebApplicationFactory : WebApplicationFactory<Program>
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        // No overrides needed for baseline tests
        // Future: could override database, auth, etc.
    }
}

public class PetEndpointTests : IClassFixture<CustomWebApplicationFactory>
{
    private readonly HttpClient _client;

    public PetEndpointTests(CustomWebApplicationFactory factory)
    {
        _client = factory.CreateClient();
    }
}
```

**Notes**:
- Hosts full ASP.NET Core pipeline (middleware, routing, validation) in-memory
- No external Kestrel server or port binding required
- Configuration identical to production except for test overrides

---

### Entity 2: `TestCase`

**Purpose**: Logical unit of validation representing one HTTP operation test

**Type**: Conceptual entity (not a C# class, but represented by xUnit `[Fact]` methods)

**Properties**:
| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `TestName` | `string` | xUnit method name following convention `[Method]_[Scenario]_[ExpectedBehavior]` | `"AddPet_WithValidData_Returns201Created"` |
| `HttpMethod` | `HttpMethod` | HTTP verb for the request | `HttpMethod.Post` |
| `Endpoint` | `string` | URL path relative to base address | `"/api/pet"` |
| `RequestBody` | `object?` | JSON payload (nullable for GET/DELETE) | `{ "name": "Fluffy", "photoUrls": [...], "status": "available" }` |
| `ExpectedStatus` | `HttpStatusCode` | Expected HTTP status code | `HttpStatusCode.Created` (201) |
| `ExpectedResponseBody` | `object?` | Expected JSON response (nullable for 204) | `{ "id": 1, "name": "Fluffy", ... }` |
| `Scenario` | `string` | Test scenario type | `"Happy Path"` or `"Unhappy Path"` |

**Test Case Categories**:

**Happy Path Tests** (valid input → success response):
| Test Name | HTTP | Endpoint | Status | Validates |
|-----------|------|----------|--------|-----------|
| `AddPet_WithValidData_Returns201Created` | POST | `/api/pet` | 201 | Pet creation with valid data |
| `GetPet_WithExistingId_ReturnsPet` | GET | `/api/pet/{id}` | 200 | Pet retrieval with valid ID |
| `UpdatePet_WithValidData_Returns200OK` | PUT | `/api/pet` | 200 | Pet update with valid data |
| `DeletePet_WithExistingId_Returns204NoContent` | DELETE | `/api/pet/{id}` | 204 | Pet deletion with valid ID |

**Unhappy Path Tests** (invalid input → error response):
| Test Name | HTTP | Endpoint | Status | Validates |
|-----------|------|----------|--------|-----------|
| `AddPet_WithMissingName_Returns400BadRequest` | POST | `/api/pet` | 400 | FluentValidation rejects missing required field |
| `GetPet_WithNonExistentId_Returns404NotFound` | GET | `/api/pet/{id}` | 404 | Resource not found handling |
| `UpdatePet_WithNonExistentId_Returns404NotFound` | PUT | `/api/pet` | 404 | Update non-existent resource handling |
| `DeletePet_WithNonExistentId_Returns404NotFound` | DELETE | `/api/pet/{id}` | 404 | Delete non-existent resource handling |

**Lifecycle**:
- Defined at design time in this document and contracts/
- Implemented as xUnit `[Fact]` methods in `PetEndpointTests.cs`
- Executed during `dotnet test` command
- Results aggregated in test output (pass/fail/skip)

**Relationships**:
- **Belongs to**: `GoldenStandardTestSuite` (8 test cases compose the suite)
- **Uses**: `HttpClient` from `CustomWebApplicationFactory`
- **Validates**: Generated FastEndpoints endpoints (`AddPetEndpoint`, `GetPetEndpoint`, etc.)

---

### Entity 3: `GoldenStandardTestSuite`

**Purpose**: Complete set of integration tests that validate the generated FastEndpoints project meets quality standards

**Type**: Conceptual entity (not a C# class, but represented by test project + test classes)

**Properties**:
| Property | Type | Description | Value |
|----------|------|-------------|-------|
| `TestProjectName` | `string` | Name of xUnit test project | `"PetstoreApi.Tests"` |
| `PetTests` | `List<TestCase>` | All Pet API test cases | 8 test cases (see TestCase entity) |
| `TotalTests` | `int` | Count of all test cases | 8 (minimum per SC-004) |
| `PassedTests` | `int` | Number of tests passing after GREEN phase | 8 (target 100% per SC-005) |
| `FailedTests` | `int` | Number of tests failing in RED phase | 8 (all fail initially, then 0 after GREEN) |
| `ExecutionTime` | `TimeSpan` | Total test run duration | <30 seconds (SC-007) |

**Test Execution Phases**:

**RED Phase** (tests written, endpoints stubbed):
```
Total tests: 8
Passed: 0
Failed: 8
Duration: ~10s
```

**GREEN Phase** (endpoints implemented):
```
Total tests: 8
Passed: 8
Failed: 0
Duration: <30s
```

**Test Coverage**:
| API Operation | Happy Path Test | Unhappy Path Test | Total |
|---------------|-----------------|-------------------|-------|
| AddPet (POST) | ✅ | ✅ | 2 |
| GetPet (GET) | ✅ | ✅ | 2 |
| UpdatePet (PUT) | ✅ | ✅ | 2 |
| DeletePet (DELETE) | ✅ | ✅ | 2 |
| **Total** | **4** | **4** | **8** |

**Lifecycle**:
1. **Design**: Test cases defined in data-model.md and contracts/
2. **RED Phase**: Tests written in PetEndpointTests.cs, all fail (proves validation works)
3. **Implementation**: Endpoint logic added to generated PetApiEndpoint.cs
4. **GREEN Phase**: All tests pass (proves implementation correct)
5. **Validation**: Success criteria checked (build time, test execution, pass rate)

**Relationships**:
- **Contains**: 8 `TestCase` entities (Pet CRUD operations × 2 scenarios)
- **Uses**: `CustomWebApplicationFactory` for in-memory app hosting
- **Validates**: Generated PetstoreApi project (FastEndpoints application)
- **Depends on**: `Microsoft.AspNetCore.Mvc.Testing`, `FluentAssertions`, xUnit

**File Structure**:
```
test-output/tests/PetstoreApi.Tests/
├── PetstoreApi.Tests.csproj       # Test project file with NuGet refs
├── CustomWebApplicationFactory.cs  # Extends WebApplicationFactory<Program>
└── PetEndpointTests.cs            # Contains all 8 TestCase [Fact] methods
```

**Success Criteria Mapping**:
| Success Criterion | Measured By | Target | Entity Property |
|-------------------|-------------|--------|-----------------|
| SC-001: Build time | `mvn clean package` duration | <1 min | _(not TestSuite property)_ |
| SC-002: Test setup | `dotnet new xunit` through `dotnet build` | <2 min | _(not TestSuite property)_ |
| SC-003: Compiles | `dotnet build` exit code | 0 errors | _(not TestSuite property)_ |
| SC-004: Test count | `dotnet test` summary | ≥8 | `TotalTests = 8` |
| SC-005: Pass rate | `dotnet test` pass/fail | 100% | `PassedTests / TotalTests = 8/8` |
| SC-006: CRUD coverage | Test case analysis | All 4 ops | 4 operations × 2 scenarios |
| SC-007: Execution time | `dotnet test` duration | <30s | `ExecutionTime < 30s` |

---

## Entity Relationships Diagram

```
┌─────────────────────────────────────┐
│ CustomWebApplicationFactory         │
│ (Test Host)                         │
│                                     │
│ - Hosts FastEndpoints app in-memory│
│ - Creates HttpClient instances     │
└───────────┬─────────────────────────┘
            │ provides HttpClient
            │
            ▼
┌─────────────────────────────────────┐
│ GoldenStandardTestSuite             │
│ (PetstoreApi.Tests project)         │
│                                     │
│ - TotalTests: 8                     │
│ - PassedTests: 8 (GREEN phase)      │
│ - ExecutionTime: <30s               │
└───────────┬─────────────────────────┘
            │ contains
            │
            ▼
┌─────────────────────────────────────┐
│ TestCase (8 instances)              │
│ (xUnit [Fact] methods)              │
│                                     │
│ 1. AddPet_WithValidData_...         │
│ 2. AddPet_WithMissingName_...       │
│ 3. GetPet_WithExistingId_...        │
│ 4. GetPet_WithNonExistentId_...     │
│ 5. UpdatePet_WithValidData_...      │
│ 6. UpdatePet_WithNonExistentId_...  │
│ 7. DeletePet_WithExistingId_...     │
│ 8. DeletePet_WithNonExistentId_...  │
└───────────┬─────────────────────────┘
            │ validates
            │
            ▼
┌─────────────────────────────────────┐
│ Generated FastEndpoints Project     │
│ (PetstoreApi)                       │
│                                     │
│ - AddPetEndpoint.HandleAsync(...)   │
│ - GetPetEndpoint.HandleAsync(...)   │
│ - UpdatePetEndpoint.HandleAsync(...)│
│ - DeletePetEndpoint.HandleAsync(...)│
└─────────────────────────────────────┘
```

---

## Data Flow: Test Execution

**Arrange Phase**:
```
TestCase (PetEndpointTests method)
    └── uses HttpClient from CustomWebApplicationFactory
        └── prepares HTTP request (method, endpoint, body)
```

**Act Phase**:
```
HttpClient sends request
    └── to in-memory app hosted by WebApplicationFactory
        └── request hits FastEndpoints routing
            └── routes to appropriate endpoint (e.g., AddPetEndpoint)
                └── FluentValidation runs (if POST/PUT)
                    └── HandleAsync logic executes
                        └── returns HTTP response
```

**Assert Phase**:
```
TestCase receives response
    └── FluentAssertions validates
        ├── response.StatusCode.Should().Be(...)
        ├── response.Content.ReadFromJsonAsync<T>()
        └── (deserialized object).Property.Should().Be(...)
```

---

## Technology Dependencies

| Entity | Depends On | Why |
|--------|------------|-----|
| `CustomWebApplicationFactory` | `Microsoft.AspNetCore.Mvc.Testing` | Base class `WebApplicationFactory<T>` |
| `TestCase` | xUnit, FluentAssertions | `[Fact]` attribute, `.Should()` assertions |
| `GoldenStandardTestSuite` | xUnit, `CustomWebApplicationFactory`, generated PetstoreApi project | Test runner, test host, system under test |

---

## Notes

**Why 8 Tests?**
- Minimum viable coverage of Pet CRUD operations
- Each operation has happy path (valid input) and unhappy path (invalid/not found)
- Proves all FastEndpoints patterns work (routing, validation, handlers, responses)
- Meets SC-004 requirement (≥8 test cases)

**Why Pet API Only?**
- Store and User APIs follow identical patterns (no new validation value)
- Pet API demonstrates full CRUD (sufficient for generator validation)
- Reduces implementation scope while meeting all success criteria

**Why In-Memory Storage?**
- Simplest approach for baseline validation
- No database setup/teardown overhead
- Tests run faster (no I/O)
- Database integration is out of scope for generator validation

**Extension Points** (Future Features):
- Add Store/User API tests (same pattern, more test cases)
- Add database integration tests (replace in-memory storage)
- Add authentication/authorization tests (JWT, OAuth)
- Add performance tests (stress testing, load testing)
- Add contract testing (Pact, Spring Cloud Contract)
