# Generated Code Structure

The structure of the generated `test-output/` depends on which generator properties are enabled.

---

## With `useNugetPackaging=false` (default)

Single-project structure — all code in one assembly:

```
test-output/
├── src/
│   └── PetstoreApi/                   # Main API project
│       ├── Converters/                 # JSON converters
│       ├── Extensions/                 # Dependency injection
│       ├── Features/                   # Endpoint definitions (MapGroup)
│       ├── Models/                     # DTOs & data contracts
│       ├── Properties/                 # Launch settings
│       ├── Program.cs                  # App entry point
│       ├── appsettings.json
│       └── PetstoreApi.csproj
├── tests/
│   └── PetstoreApi.Tests/              # xUnit test project (copied from petstore-tests/)
├── PetstoreApi.sln
└── README.md
```

---

## With `useNugetPackaging=true`

Two-project structure — contracts separated for NuGet distribution:

```
test-output/
├── Contract/                           # Shared contract files (source)
│   ├── Converters/                     # Enum converters
│   └── Endpoints/                      # Endpoint definitions
├── src/
│   ├── PetstoreApi.Contracts/          # NuGet package project
│   │   ├── Extensions/                 # EndpointExtensions.cs
│   │   ├── (Models linked from ../PetstoreApi/Models/)
│   │   └── PetstoreApi.Contracts.csproj  # NuGet metadata
│   └── PetstoreApi/                    # Implementation project
│       ├── Extensions/                 # HandlerExtensions, ServiceCollection
│       ├── Models/                     # DTOs (original location)
│       ├── Properties/
│       ├── Program.cs
│       └── PetstoreApi.csproj          # References Contracts package
├── tests/
│   └── PetstoreApi.Tests/
├── PetstoreApi.sln                     # Solution with both projects
└── README.md
```

**Key differences from single-project:**
- Endpoints live in `Contract/Endpoints/` rather than `Features/`
- Models are shared via file links into the Contracts project
- A separate `PetstoreApi.Contracts.csproj` is generated for NuGet publishing

---

## With `useMediatr=true`

Adds CQRS pattern files inside the implementation project:

```
src/PetstoreApi/
├── Commands/           # Write operations (POST, PUT, DELETE)
│   ├── AddPetCommand.cs
│   ├── UpdatePetCommand.cs
│   └── DeletePetCommand.cs
├── Queries/            # Read operations (GET)
│   ├── GetPetByIdQuery.cs
│   └── FindPetsByStatusQuery.cs
├── Handlers/           # Stub handler implementations (overwritten by test stubs)
│   ├── AddPetCommandHandler.cs
│   ├── GetPetByIdQueryHandler.cs
│   └── ...
└── ...
```

---

## With `useValidators=true`

Adds FluentValidation validators:

```
src/PetstoreApi/
├── Validators/
│   ├── AddPetCommandValidator.cs
│   ├── UpdatePetCommandValidator.cs
│   └── ...
└── ...
```

---

## With `useProblemDetails=true`

Configures the global exception handler to return [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) `application/problem+json` responses instead of plain JSON error objects.

---

## Note on `test-output/` vs generator output

`gen:copy-test-stubs` (and its variants) overwrites several subdirectories — `Handlers/`, `Services/`, `Filters/`, `Configurators/`, `Extensions/`, and the entire `tests/` tree — with the hand-written stubs from `petstore-tests/`. If you need to inspect the raw generator output, examine the files **before** running any `test:*` or `api:*` task, or compare directly against the Mustache templates in `generator/src/main/resources/aspnet-minimalapi/`.
