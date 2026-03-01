# Petstore Tests: How the Test Infrastructure Works

## Overview

`test-output/` is **ephemeral** — it is wiped and regenerated every time `gen:petstore` runs. It cannot contain hand-written implementation code.

`petstore-tests/` is the **persistent source** of all test artifacts: handler implementations, DI configuration, xUnit tests, and auth infrastructure. These files are copied into `test-output/` at test time by the `gen:copy-test-stubs` family of tasks.

```
petstore-tests/         ← source of truth (committed, hand-written)
    ↓ gen:copy-test-stubs
test-output/            ← ephemeral generated + stitched-in code
```

---

## Directory Map

```text
petstore-tests/
├── petstore.yaml                        # OpenAPI spec used for all generation
├── TestHandlers/                        # MediatR handler implementations
│   ├── InMemoryPetStore.cs              # IPetStore + InMemoryPetStore impl
│   ├── AddPetCommandHandler.cs
│   ├── UpdatePetCommandHandler.cs
│   ├── DeletePetCommandHandler.cs
│   ├── GetPetByIdQueryHandler.cs
│   ├── FindPetsByStatusQueryHandler.cs
│   └── FindPetsByTagsQueryHandler.cs
├── Configurators/
│   ├── ApplicationServiceConfigurator.cs  # Registers IPetStore via AddApplicationServices()
│   └── SecurityConfigurator.cs            # JWT auth + authorization policies (auth only)
├── Auth/
│   └── PermissionEndpointFilter.cs        # IEndpointFilter enforcing ReadAccess/WriteAccess policies
├── PetstoreApi/
│   └── Extensions/
│       └── ServiceCollectionExtensions.cs # AddApplicationServices() — registers IPetStore singleton
└── PetstoreApi.Tests/
    ├── CustomWebApplicationFactory.cs     # WebApplicationFactory with Open/Secure mode switching
    ├── TestAuthentication/
    │   ├── BypassAuthHandler.cs           # Open mode: bypasses all auth checks
    │   ├── MockAuthHandler.cs             # Secure mode: reads X-Test-* headers as claims
    │   └── TestAuthenticationExtensions.cs
    ├── PetEndpointTests.cs
    ├── ValidationTests.cs
    ├── HealthEndpointTests.cs
    ├── DualModeAuthTests.cs
    └── GeneratedDtoTests.cs + GeneratedHandlerTests.cs
```

---

## The Copy-Stubs Mechanism

### `gen:copy-test-stubs` (base — always required)

Copies the minimum set of files needed to compile and run the generated API:

| Source (`petstore-tests/`) | Destination (`test-output/`) |
|---|---|
| `TestHandlers/*.cs` (handlers) | `src/PetstoreApi/Handlers/` |
| `TestHandlers/InMemoryPetStore.cs` | `src/PetstoreApi/Services/` |
| `Auth/PermissionEndpointFilter.cs` | `src/PetstoreApi/Filters/` |
| `Configurators/ApplicationServiceConfigurator.cs` | `src/PetstoreApi/Configurators/` |
| `PetstoreApi/Extensions/ServiceCollectionExtensions.cs` | `src/PetstoreApi/Extensions/` |
| `PetstoreApi.Tests/` | `tests/PetstoreApi.Tests/` |

> ⚠️ All `test:*` and `api:*` tasks call `gen:copy-test-stubs` automatically as a dependency. Files copied by this task **overwrite** the generated placeholders in `test-output/`. When debugging generator output, inspect files **before** running any test or API task, or compare directly against the Mustache templates.

### `gen:copy-test-stubs-with-auth` (adds JWT bearer auth)

Depends on `gen:copy-test-stubs` (so always runs base stubs first), then additionally:

1. Copies `Configurators/SecurityConfigurator.cs` → `test-output/src/PetstoreApi/Configurators/`
2. Runs `dotnet add package Microsoft.AspNetCore.Authentication.JwtBearer --version 8.0.0` against the generated `.csproj`

This is the only way to enable JWT auth in the generated project. There is no `useAuthentication` generator flag.

---

## How Auth Wiring Works

The generated `Program.cs` uses a **scanner pattern** to discover and invoke configurators at startup:

```csharp
// Scans the assembly for IServiceConfigurator implementations and calls each one
var serviceConfigurators = typeof(Program).Assembly.GetTypes()
    .Where(t => typeof(IServiceConfigurator).IsAssignableFrom(t) && !t.IsInterface && !t.IsAbstract)
    ...
foreach (var configurator in serviceConfigurators)
    configurator.ConfigureServices(builder.Services, builder.Configuration, builder.Environment);

// Same pattern for IApplicationConfigurator (ordered middleware)
```

This means:
- **Without auth stubs**: only `ApplicationServiceConfigurator` is present → no auth middleware registered
- **With auth stubs** (`gen:copy-test-stubs-with-auth`): `SecurityConfigurator` is also present → JWT auth + authorization policies are registered automatically

`SecurityConfigurator` implements both `IServiceConfigurator` (registers `AddAuthentication`/`AddAuthorization`) and `IApplicationConfigurator` (calls `UseAuthentication`/`UseAuthorization` in the middleware pipeline at `Order = 10`).

### Authorization Policies

`SecurityConfigurator` registers two policies:

| Policy | Required claim |
|---|---|
| `ReadAccess` | `permission: read` |
| `WriteAccess` | `permission: write` |

`PermissionEndpointFilter` (always present) maps endpoint names to policies at request time:

| Endpoints | Policy |
|---|---|
| AddPet, UpdatePet, DeletePet, PlaceOrder, DeleteOrder, CreateUser, UpdateUser, DeleteUser | `WriteAccess` |
| GetPetById, FindPetsByStatus, FindPetsByTags, GetOrderById, GetInventory, GetUserByName, LoginUser, LogoutUser | `ReadAccess` |

---

## xUnit Test Modes

`CustomWebApplicationFactory` supports two modes controlled by its `Mode` property:

| Mode | Behaviour |
|---|---|
| `TestMode.Open` (default) | `BypassAuthHandler` — all auth checks pass, no credentials needed |
| `TestMode.Secure` | `MockAuthHandler` — reads `X-Test-UserId` / `X-Test-Permissions` headers as JWT claims |

```csharp
// Open mode (default) — no auth headers needed
var factory = new CustomWebApplicationFactory(); // Mode = TestMode.Open
var client = factory.CreateClient();

// Secure mode — supply X-Test-Permissions header
var factory = new CustomWebApplicationFactory { Mode = TestMode.Secure };
var client = factory.CreateClient();
var request = new HttpRequestMessage(HttpMethod.Post, "/pet");
request.Headers.Add("X-Test-Permissions", "write");
```

See [DUAL_MODE_TESTING.md](../petstore-tests/DUAL_MODE_TESTING.md) for full details.

---

## Bruno Auth Tests

The `bruno/OpenAPI_Petstore/auth-suite/` folder contains 4 integration tests that require a **running API with auth stubs**:

| Test | Expects |
|---|---|
| `add-pet-authorized.bru` | `201` — valid `writeToken` |
| `add-pet-unauthorized.bru` | `403` — missing token |
| `delete-pet-unauthorized.bru` | `403` — missing token |
| `update-pet-authorized.bru` | `200` — valid `writeToken` |

Tokens are pre-generated JWTs signed with the test secret (`this-is-a-test-secret-key-for-petstore-api-dev-only-min-32-bytes!`) defined in `SecurityConfigurator`. Generate them with:

```bash
node bruno/generate-test-tokens.js
```

---

## Common Workflows

### Run tests without auth

```bash
devbox run task clean:generated gen:petstore test:petstore-unit
devbox run task clean:generated gen:petstore test:petstore-integration SUITE="all-suites"
```

### Run tests with auth

```bash
devbox run task clean:generated gen:petstore gen:copy-test-stubs-with-auth test:petstore-unit
devbox run task clean:generated gen:petstore gen:copy-test-stubs-with-auth test:petstore-integration SUITE="all-suites-with-auth"
```

Note: `test:petstore-unit` and `test:petstore-integration` call `gen:copy-test-stubs` as a dep, which is idempotent. It does **not** remove `SecurityConfigurator.cs` or the JwtBearer package reference already placed by `gen:copy-test-stubs-with-auth`.

### Full auth regression

```bash
devbox run task regress:full-petstore-validators-problemdetails-nuget-auth
```

This runs: `clean:generated` → `gen:petstore` → `gen:copy-test-stubs-with-auth` → `test:petstore-unit` → `test:petstore-integration SUITE=all-suites-with-auth`
