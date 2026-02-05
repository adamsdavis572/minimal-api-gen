# Feature Specification: NuGet API Contract Packaging

**Feature Branch**: `008-nuget-api-contracts`  
**Created**: 2026-01-26  
**Status**: Draft  
**Input**: Create NuGet package capability for Endpoints, DTOs, and Validators while keeping Handlers and Models separate for flexible versioning

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Package API Contracts for Distribution (Priority: P0 - Critical)

As an API developer, when I generate code from an OpenAPI specification, I want the generator to produce a NuGet package containing only the API contract layer (Endpoints, DTOs, Validators), so that I can distribute and version the API surface independently from business logic implementations (Handlers, Models).

**Why this priority**: P0 (blocking) because this is the foundational capability that enables the entire workflow. Without the ability to package API contracts separately, none of the versioning or decoupling benefits can be realized. This changes the generator's output model fundamentally.

**Independent Test**: Can be fully tested by generating code with `useNugetPackaging=true`, verifying that a `.csproj` file for the NuGet package is created with only Endpoints/, DTOs/, and Validators/ included, and successfully building the NuGet package with `dotnet pack`.

**Acceptance Scenarios**:

1. **Given** OpenAPI spec with operations, **When** generator runs with `useNugetPackaging=true`, **Then** two project files are generated: one for NuGet package (API contracts) and one for implementation (Handlers, Models, Program.cs)
2. **Given** generated NuGet package project, **When** examining included files, **Then** only Endpoints/, DTOs/, and Validators/ directories are packaged, excluding Handlers/ and Models/
3. **Given** generated NuGet package project, **When** running `dotnet pack`, **Then** a valid `.nupkg` file is created with correct metadata (package ID, version, dependencies)
4. **Given** generated implementation project, **When** examining project references, **Then** it references the NuGet package project locally via `ProjectReference`
5. **Given** generated implementation project, **When** examining included files, **Then** it contains Handlers/, Models/, Program.cs, and service registration code

---

### User Story 2 - Inject Services and Handlers from Host Application (Priority: P0 - Critical)

As an API developer, when I consume a NuGet package containing generated Endpoints and Commands/Queries (which use DTO types), I want generated Handler scaffolds with DTO-to-Domain mapping code, so that I can customize business logic while the packaged Endpoints delegate to my Handler implementations without recompiling the package.

**Why this priority**: P0 (blocking) because without service injection, the packaged Endpoints cannot execute business logic. This is the critical integration point between the NuGet package (API surface) and the host application (business logic). This must be designed correctly from the start to avoid breaking changes.

**Independent Test**: Can be fully tested by consuming a generated NuGet package in a test host application, registering custom Handler implementations via an extension method (e.g., `services.AddPetstoreHandlers()`), running the API, and verifying that Endpoints successfully invoke the registered Handlers.

**Acceptance Scenarios**:

1. **Given** NuGet package with Endpoints, **When** examining package public API, **Then** an extension method `AddApiEndpoints()` is provided to register all endpoint routes
2. **Given** NuGet package with Validators, **When** examining package public API, **Then** an extension method `AddApiValidators()` is provided to register all FluentValidation validators
3. **Given** implementation project with custom Handlers, **When** examining Program.cs, **Then** an extension method `AddApiHandlers()` is provided to register all MediatR handlers with dependency injection
4. **Given** implementation project needing custom services (e.g., IDbContext), **When** registering services in Program.cs, **Then** developer registers custom services in DI container first, then calls `AddApiHandlers()` which resolves services from DI when constructing handlers
5. **Given** Endpoint receiving HTTP request, **When** executing via MediatR, **Then** the request flows to the custom Handler implementation registered in the host application
6. **Given** breaking change in OpenAPI spec (new required property), **When** NuGet package is updated, **Then** host application compilation fails with clear error indicating Handler signature mismatch, forcing explicit update
7. **Given** generated Handler implementation, **When** examining the Handle() method, **Then** it includes scaffolded mapping methods (MapCommandToDomain, MapDomainToDto, MapEnumToDomain) with TODO comments indicating customization points, providing a starting point for developer to customize business logic

---

### User Story 3 - Version API Contracts Independently (Priority: P1)

As an API maintainer, when a new version of the OpenAPI specification is released with backward-compatible changes (new optional fields, new endpoints), I want to update the NuGet package version and have consuming applications adopt it without requiring Handler changes, so that API evolution doesn't force cascading code updates across all implementations.

**Why this priority**: P1 (high value) because this delivers the key benefit of the separation - independent versioning. This enables faster API iteration and reduces deployment coupling. However, the packaging infrastructure (P0) must exist first.

**Independent Test**: Can be fully tested by generating v1.0.0 NuGet package from an OpenAPI spec, then generating v1.1.0 from a spec with an additional optional property on an existing DTO, updating the NuGet reference in a host application, and verifying that the application compiles and runs without modifying any Handlers.

**Acceptance Scenarios**:

1. **Given** OpenAPI spec v1.0.0 with Pet DTO (name, status), **When** generating NuGet package, **Then** package version is 1.0.0
2. **Given** OpenAPI spec v1.1.0 with Pet DTO (name, status, category - optional), **When** generating NuGet package, **Then** package version is 1.1.0 and Pet DTO includes new optional property
3. **Given** host application using NuGet v1.0.0, **When** updating to v1.1.0, **Then** existing Handlers (using name, status) continue to work without modification
4. **Given** OpenAPI spec v2.0.0 with breaking change (name renamed to petName), **When** generating NuGet package, **Then** package version is 2.0.0 and consuming applications fail to compile with clear errors
5. **Given** host application needing to adopt v2.0.0, **When** updating package, **Then** compiler errors guide developer to update Handler implementations to use new DTO property names

---

### User Story 4 - Configure Package Metadata (Priority: P2)

As an API maintainer, when generating a NuGet package, I want to specify package metadata (package ID, version, authors, description, repository URL) via generator options or a configuration file, so that the published package has correct identification and discoverability in NuGet feeds.

**Why this priority**: P2 (important but not blocking) because package metadata is essential for production NuGet distribution, but development and testing can proceed with default metadata. This can be added after core packaging works.

**Independent Test**: Can be fully tested by generating code with `packageId=MyCompany.PetStore.Contracts`, `packageVersion=2.1.0`, `packageAuthors=MyTeam`, and other metadata properties, then verifying that the generated `.csproj` contains correct `<PackageId>`, `<Version>`, `<Authors>`, etc., and that `dotnet pack` produces a `.nupkg` with this metadata.

**Acceptance Scenarios**:

1. **Given** generator runs with `packageId=MyCompany.PetStore.Contracts`, **When** examining generated NuGet project, **Then** `.csproj` contains `<PackageId>MyCompany.PetStore.Contracts</PackageId>`
2. **Given** generator runs with `packageVersion=2.1.0`, **When** examining generated NuGet project, **Then** `.csproj` contains `<Version>2.1.0</Version>`
3. **Given** generator runs with `packageAuthors=Platform Team`, **When** examining generated NuGet project, **Then** `.csproj` contains `<Authors>Platform Team</Authors>`
4. **Given** generator runs with `packageDescription=API contracts for Petstore`, **When** running `dotnet pack`, **Then** generated `.nupkg` metadata contains description visible in NuGet explorer
5. **Given** generator runs without package metadata properties, **When** generating projects, **Then** sensible defaults are used (packageId from spec title, version 1.0.0, authors from generator name)

---

### User Story 5 - Symbol Package for Debugging (Priority: P3)

As an API consumer, when debugging issues in Endpoints or Validators packaged in NuGet, I want to include a symbol package (`.snupkg`) with source files and debugging symbols, so that I can step into packaged code during debugging sessions.

**Why this priority**: P3 (nice to have) because debugging capability improves developer experience but isn't required for core functionality. Most debugging happens in Handlers (which are local source code), not in packaged Endpoints.

**Independent Test**: Can be fully tested by generating code with `includeSymbols=true`, running `dotnet pack` with symbols configuration, verifying that a `.snupkg` file is created alongside the `.nupkg`, publishing both to a local NuGet feed, and confirming that Visual Studio can step into Endpoint code during debugging.

**Acceptance Scenarios**:

1. **Given** generator runs with `includeSymbols=true`, **When** examining generated NuGet project, **Then** `.csproj` contains `<IncludeSymbols>true</IncludeSymbols>` and `<SymbolPackageFormat>snupkg</SymbolPackageFormat>`
2. **Given** generated NuGet project with symbols enabled, **When** running `dotnet pack`, **Then** both `.nupkg` and `.snupkg` files are created
3. **Given** symbols package published to NuGet feed, **When** debugging in Visual Studio, **Then** developer can step into Endpoint code and see original source lines
4. **Given** generator runs with `includeSymbols=false` or by default, **When** running `dotnet pack`, **Then** only `.nupkg` is created (no symbol package)

---

### Edge Cases

- What happens when a non-backward-compatible change is made to the OpenAPI spec (e.g., removing a property, changing type)? → NuGet package version should follow SemVer (major version bump), consuming applications fail to compile with clear errors pointing to Handler signature mismatches where DTO properties no longer exist or have changed types
- What happens when an Endpoint depends on a service (e.g., ILogger, IConfiguration) that must be injected? → NuGet package Endpoints should use constructor injection for framework services (ILogger, IMediator), host application provides these via DI container
- What happens when developer wants to publish to a private NuGet feed? → Package metadata includes `<RepositoryUrl>` and developer uses `dotnet nuget push` with feed URL and API key
- What happens when consuming application uses different .NET version than package targets? → NuGet package should target `net8.0` (lowest supported version), multi-targeting can be added later if needed
- What happens when Endpoint needs a custom policy or middleware? → Implementation project (Program.cs) can apply middleware before calling `AddApiEndpoints()`, Endpoints remain middleware-agnostic
- What happens when two different OpenAPI specs generate conflicting endpoint routes? → Package naming (packageId) should include API name/version to avoid conflicts, e.g., `MyCompany.PetStore.V1.Contracts` vs `MyCompany.Inventory.V1.Contracts`
- What happens when developer wants to add custom handlers (not generated from OpenAPI) in the implementation project? → Developer implements IRequestHandler<TRequest, TResponse> in Handlers/ directory; MediatR assembly scanning auto-discovers and registers custom handlers alongside generated ones without requiring code regeneration

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Generator MUST create two separate `.csproj` files when `useNugetPackaging=true`: one for NuGet package (API contracts) and one for implementation (business logic)
- **FR-002**: NuGet package project MUST include only Endpoints/, Commands/, Queries/, DTOs/, Validators/, and Converters/ directories (exclude Handlers/, Domain Entities/, Program.cs)
- **FR-003**: NuGet package project MUST generate a valid `.csproj` with `<GeneratePackageOnBuild>false</GeneratePackageOnBuild>` (require explicit `dotnet pack`)
- **FR-004**: NuGet package project MUST include dependencies on MediatR, FluentValidation, and ASP.NET Core Minimal APIs packages with correct version constraints
- **FR-005**: Implementation project MUST reference NuGet package project via `<ProjectReference>` during local development
- **FR-006**: Implementation project MUST include Handlers/ (with scaffolded mapping code), Models/ (domain entities - future: rename to Domain/), Program.cs, and service registration extensions
- **FR-007**: NuGet package MUST expose a public extension method `AddApiEndpoints(this IEndpointRouteBuilder endpoints)` to register all endpoint routes. This is the only required extension method as there is no standard ASP.NET Core equivalent for bulk endpoint registration.
- **FR-008**: NuGet package SHOULD provide an extension method `AddApiValidators(this IServiceCollection services)` to register FluentValidation validators (when `useValidators=true`). This is recommended because validators are in the NuGet package assembly (different from Program.cs). Alternatives: `services.AddValidatorsFromAssembly(Assembly.Load("PackageName"))` or `services.AddValidatorsFromAssembly(typeof(SomeDto).Assembly)`.
- **FR-009**: Implementation project MAY provide an extension method `AddApiHandlers(this IServiceCollection services)` for API consistency, but this is optional. Handlers are in the same assembly as Program.cs, so developers can use standard MediatR registration: `services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly))`.
- **FR-010**: Endpoints in NuGet package MUST use constructor injection for required services (IMediator, ILogger) compatible with ASP.NET Core DI
- **FR-011**: Generator MUST support `packageId` property to specify NuGet package identifier (default: derived from OpenAPI spec title)
- **FR-012**: Generator MUST support `packageVersion` property to specify SemVer version (default: 1.0.0)
- **FR-013**: Generator MUST support `packageAuthors` property to specify package authors (default: generator name)
- **FR-014**: Generator MUST support `packageDescription` property to specify package description (default: derived from OpenAPI spec description)
- **FR-015**: Generator MUST support `includeSymbols` property to enable symbol package generation (default: false)
- **FR-016**: Generator MUST support `packageLicenseExpression` property to specify SPDX license identifier (e.g., "Apache-2.0", "MIT") for NuGet package metadata (default: "Apache-2.0")
- **FR-017**: Generator MUST support `packageRepositoryUrl` property to specify source code repository URL for NuGet package metadata (optional, no default - only included if provided)
- **FR-018**: Generator MUST support `packageProjectUrl` property to specify project homepage URL for NuGet package metadata (optional, no default - only included if provided)
- **FR-019**: Generator MUST support `packageTags` property to specify semicolon-separated tags for NuGet package discoverability (default: "openapi;minimal-api;contracts")
- **FR-020**: NuGet package project MUST have correct `<PackageId>`, `<Version>`, `<Authors>`, `<Description>` elements based on generator properties
- **FR-021**: Generator MUST produce buildable projects: `dotnet build` succeeds for both NuGet package project and implementation project
- **FR-022**: Generator MUST produce packable NuGet project: `dotnet pack` succeeds and produces valid `.nupkg` file
- **FR-023**: Generated code MUST support backward-compatible OpenAPI changes (new optional properties, new endpoints) without breaking consuming applications
- **FR-024**: Generated code MUST cause compilation errors for non-backward-compatible OpenAPI changes (renamed properties, removed properties, type changes) to force explicit Handler updates
- **FR-025**: Implementation project's `AddApiHandlers()` extension method MUST use `services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(...))` to auto-register all IRequestHandler implementations, supporting both generated handlers and developer-added custom handlers without code regeneration
- **FR-026**: When `useNugetPackaging=true`, generator MUST separate components by assembly: NuGet package (Contracts.dll) contains Endpoints/DTOs/Validators/Commands/Queries, Implementation project (Implementation.dll) contains Handlers/Models/Program.cs. This separation explains why validators need special registration (different assembly) while handlers can use `typeof(Program).Assembly` (same assembly).
- **FR-027**: Commands and Queries MUST be the request data structures themselves and MUST use DTO types for response types (e.g., `AddPetCommand : IRequest<PetDto>` with request properties on the Command, not `IRequest<Pet>`) to ensure Contract package has zero dependencies on Implementation project and maintains true Contract-First architecture where domain models never leak into API contract. All OpenAPI operation parameters map to Command/Query properties: path parameters (e.g., `/pets/{petId}` → `long PetId`), query parameters (e.g., `?status=available` → `StatusEnum? Status`), and request body schema properties (e.g., `{"name":"Fluffy"}` → `string Name`) become properties with C# types derived from OpenAPI schema types. Note: For complex nested request structures, a Request DTO MAY be used as a property on the Command/Query (e.g., `AddPetCommand { AddPetRequestDto Pet }`) when the OpenAPI request body schema contains nested objects that warrant their own type definition.
- **FR-028**: ALL enum properties in generated DTOs (response types defined in OpenAPI schemas) MUST include `[JsonConverter(typeof(EnumMemberJsonConverter<EnumType>))]` attributes to enable strict type serialization (string-to-enum mapping) at the API boundary, using the existing `EnumMemberJsonConverter<T>` implementation that supports `JsonPropertyName` attributes on enum members. This applies to all enums whether defined inline in schemas (e.g., `status: { type: string, enum: [available, pending] }`) or referenced via `$ref` (e.g., `status: { $ref: '#/components/schemas/StatusEnum' }`).
- **FR-029**: Generated Handlers MUST include scaffolded mapping methods (MapCommandToDomain for input mapping, MapDomainToDto for response mapping, MapEnumToDomain/MapEnumToDto for enum conversions) as private methods with TODO comments indicating customization points. Scaffolds provide starting point structure but require developer implementation for production use. Examples of customization: error handling (throw exceptions for invalid states), null checking (handle optional properties), validation (check business rules before mapping), and business logic (compute derived fields). Each TODO comment should guide developers toward specific implementation needs relevant to that mapping step.

### Key Entities

- **NuGet Package Project (Contracts)**: Separate `.csproj` containing API contracts (Endpoints, Commands, Queries, DTOs, Validators, Converters) intended for distribution via NuGet feed. Outputs `.nupkg` file via `dotnet pack`. This assembly defines the API surface and has zero dependencies on Implementation.
- **Implementation Project**: Separate `.csproj` containing business logic (Handlers with mapping code, Domain Entities, Program.cs) that consumes NuGet package locally during development and provides service/handler registration. Contains the executable application.
- **DTO (Data Transfer Object)**: API contract type representing response data structures defined in OpenAPI schema. Generated with enum types and JsonConverter attributes. DTOs are framework-agnostic POCOs in the Contract package used for operation responses. Examples: `PetDto`, `CategoryDto`, `OrderDto`. Note: Request data is represented by Commands/Queries themselves, not separate input DTOs.
- **Domain Entity**: Business logic model in Implementation project's Models/ folder (e.g., `Pet`, `Order`, `User`) with business rules and relationships. Generated as `partial` class scaffolds allowing developer customization. Domain Entities are NOT in Contract package as they represent internal business concepts, not API surface. Current folder: Models/ (future enhancement: rename to Domain/ for clearer semantics).
- **Command/Query**: MediatR request types in Contract package (e.g., `AddPetCommand : IRequest<PetDto>`, `GetPetByIdQuery : IRequest<PetDto>`) that ARE the request data structures themselves with operation parameters as properties. Commands/Queries define the input contract, DTOs define the output contract. No separate input DTO is needed - the Command/Query IS the input DTO.
- **Handler**: MediatR handler in Implementation project (e.g., `AddPetCommandHandler : IRequestHandler<AddPetCommand, PetDto>`) that executes business logic. Generated with scaffolded mapping methods to convert Command/Query (input) → Domain Entity → DTO (response).
- **Package Metadata**: Configuration data (packageId, packageVersion, packageAuthors, packageDescription) embedded in NuGet package `.csproj` and visible in NuGet feeds.
- **Extension Methods**: Public static methods (`AddApiEndpoints`, `AddApiValidators`, `AddApiHandlers`) that encapsulate registration logic for DI container and endpoint routing.
- **Dependency Injection Container**: ASP.NET Core's `IServiceCollection` used to register services, validators, and handlers; IEndpointRouteBuilder used to register endpoint routes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developer can generate a valid NuGet package by running generator with `useNugetPackaging=true` followed by `dotnet pack` (both commands complete successfully)
- **SC-002**: Generated NuGet package size is under 500KB for typical OpenAPI spec (20 operations, 10 DTOs) to ensure fast download and restore
- **SC-003**: Host application can register all API components with 2-3 method calls in Program.cs: `AddApiEndpoints()` (required), `AddApiValidators()` (recommended), and optionally `AddApiHandlers()` or standard `AddMediatR()`. Custom service registration not counted toward this metric.
- **SC-004**: Updating NuGet package with backward-compatible API changes (new optional properties) requires zero changes to existing Handler implementations (verified by successful compilation)
- **SC-005**: Updating NuGet package with breaking API changes (renamed properties) causes compilation errors in Handler implementations within 1 second (immediate feedback during `dotnet build`)
- **SC-006**: Generated NuGet package follows SemVer conventions: patch version for bug fixes, minor version for backward-compatible changes, major version for breaking changes
- **SC-007**: Developer can publish generated NuGet package to public or private NuGet feed using standard `dotnet nuget push` command without manual `.csproj` edits
- **SC-008**: Generated code supports debugging: developer can set breakpoints in Endpoint code when consuming NuGet package with symbols (when `includeSymbols=true`)
- **SC-009**: Generator produces two buildable projects: `dotnet build` completes in under 10 seconds for both NuGet package project and implementation project on standard hardware
- **SC-010**: API endpoint routing performance is identical (within 5% latency) whether using NuGet-packaged Endpoints or inline Endpoints (no performance penalty for packaging)

## Assumptions

1. **Target .NET Version**: NuGet packages will target .NET 8.0 as the baseline framework. Multi-targeting (e.g., net6.0;net8.0) can be added in future enhancements.
2. **MediatR Dependency**: This feature assumes `useMediatr=true` is enabled. NuGet packaging without MediatR (direct endpoint handlers) is out of scope for initial implementation.
3. **FluentValidation Optional**: Validators are included in NuGet package only when `useValidators=true`. Package adapts to configuration flags.
4. **Local Development First**: Initial implementation uses `<ProjectReference>` for local development. Publishing implementation project to NuGet (for distribution) is a future enhancement.
5. **SemVer Responsibility**: Generator does not automatically determine SemVer version from OpenAPI changes. Developer must specify `packageVersion` based on change type (patch/minor/major).
6. **NuGet Feed Agnostic**: Generator produces packages compatible with any NuGet feed (NuGet.org, Azure Artifacts, MyGet). Feed configuration is developer's responsibility.
7. **No Multi-API Packaging**: Each OpenAPI specification generates one NuGet package. Combining multiple specs into one package is out of scope.
8. **Standard .NET Project Structure**: NuGet package uses standard .NET project layout. Custom packaging (e.g., Paket, custom MSBuild) is not supported.
9. **Authentication/Authorization**: Packaged Endpoints support standard ASP.NET Core auth attributes (`[Authorize]`, policies). Auth configuration remains in host application Program.cs.
10. **Error Handling**: Global exception handler remains in implementation project (Program.cs), not in NuGet package, allowing host application to customize error responses.

## Dependencies

- **Feature 006 (MediatR Decoupling)**: NuGet packaging builds on MediatR architecture (Commands, Queries, Handlers). Feature 006 must be complete and stable.
- **Feature 007 (DTO Validation Architecture)**: NuGet package includes DTOs and Validators introduced in Feature 007. Packaging structure depends on this architecture.
- **.NET SDK 8.0+**: NuGet package generation requires .NET SDK for `dotnet pack` command and MSBuild targets.
- **NuGet CLI Tools**: Developer must have `dotnet nuget` tools available for publishing (not required for generation).

**Constitution Compliance Note (Principle III - Template Reusability)**: JsonConverter attributes on DTOs (FR-028) represent minimal framework coupling acceptable for correct API serialization behavior. While System.Text.Json is technically framework-specific, JSON serialization attributes are considered part of the data contract (similar to DataAnnotations) and necessary for API correctness. This does not violate the spirit of framework-agnostic DTOs as the POCOs remain usable across different API frameworks.

## Out of Scope

- **Automatic SemVer Detection**: Generator will not analyze OpenAPI spec changes to automatically determine semantic version (patch/minor/major). Developer specifies `packageVersion` explicitly.
- **Multi-Targeting**: Initial implementation targets `net8.0` only. Supporting multiple target frameworks (e.g., net6.0;net8.0) is deferred.
- **Publishing Implementation Project**: Host application (Handlers, Models, Program.cs) is not packaged or published. Only API contract layer is distributed as NuGet.
- **Custom Packaging Formats**: Only standard NuGet `.nupkg` format is supported. Other distribution formats (ZIP, Docker image, source tarball) are out of scope.
- **Breaking Change Detection**: Generator will not validate if OpenAPI changes are backward-compatible. Compilation errors in consuming apps are the validation mechanism.
- **Monorepo/Multi-API Support**: Each generator invocation produces one NuGet package for one OpenAPI spec. Aggregating multiple specs into one package is out of scope.
- **Package Signing**: Code signing for NuGet packages (Authenticode, strong naming) is not implemented. Developer can sign manually if required.
- **README/Documentation Generation**: NuGet package will not include auto-generated README.md or API documentation. OpenAPI spec serves as documentation source.
