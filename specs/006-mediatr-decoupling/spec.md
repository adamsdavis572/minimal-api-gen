# Feature Specification: MediatR Implementation Decoupling

**Feature Branch**: `006-mediatr-decoupling`  
**Created**: 2025-11-19  
**Status**: Draft  
**Input**: User description: "Decouple implementation from generated minimal api server stubs using mediatr library"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate Clean API Endpoints (Priority: P1)

As a developer using the OpenAPI Generator, I want the generated Minimal API endpoints to be clean stubs that delegate to MediatR handlers, so that my business logic is decoupled from the generated code and I can modify implementations without touching generated files.

**Why this priority**: This is the core value proposition - enabling clean separation of concerns and making the generator production-ready by removing business logic from generated code.

**Independent Test**: Can be fully tested by generating code from an OpenAPI spec and verifying that endpoint files contain only MediatR.Send() calls without any implementation logic (no dictionaries, no CRUD operations, no vendor extensions).

**Acceptance Scenarios**:

1. **Given** an OpenAPI specification with POST /pet operation, **When** code is generated, **Then** the PetApiEndpoints.cs contains only a stub that creates and sends an AddPetCommand to MediatR
2. **Given** an OpenAPI specification with GET /pet/{id} operation, **When** code is generated, **Then** the PetApiEndpoints.cs contains only a stub that creates and sends a GetPetByIdQuery to MediatR
3. **Given** an OpenAPI specification with PUT /pet operation, **When** code is generated, **Then** the PetApiEndpoints.cs contains only a stub that creates and sends an UpdatePetCommand to MediatR
4. **Given** an OpenAPI specification with DELETE /pet/{id} operation, **When** code is generated, **Then** the PetApiEndpoints.cs contains only a stub that creates and sends a DeletePetCommand to MediatR

---

### User Story 2 - Generate MediatR Commands/Queries (Priority: P1)

As a developer using the OpenAPI Generator, I want MediatR command and query classes automatically generated for each operation, so that I have a clear contract for implementing my business logic.

**Why this priority**: Commands and queries are essential infrastructure for the MediatR pattern - without them, developers cannot implement handlers. This must be delivered alongside US1.

**Independent Test**: Can be fully tested by generating code and verifying that Commands/ and Queries/ directories contain proper MediatR request classes with correct properties matching OpenAPI operation parameters and return types.

**Acceptance Scenarios**:

1. **Given** an OpenAPI POST operation with request body, **When** code is generated, **Then** a command class is created with properties matching the request body schema and implementing IRequest<TResponse>
2. **Given** an OpenAPI GET operation with path/query parameters, **When** code is generated, **Then** a query class is created with properties matching all parameters and implementing IRequest<TResponse>
3. **Given** an OpenAPI operation returning a Pet object, **When** code is generated, **Then** the command/query implements IRequest<Pet>
4. **Given** an OpenAPI operation with no return type (204), **When** code is generated, **Then** the command implements IRequest<Unit>

---

### User Story 3 - Generate Handler Scaffolds (Priority: P2)

As a developer using the OpenAPI Generator, I want MediatR handler scaffolds automatically generated with TODO comments, so that I have a clear starting point for implementing my business logic.

**Why this priority**: While not strictly necessary (developers can create handlers manually), this significantly improves developer experience by providing scaffolding and reducing boilerplate.

**Independent Test**: Can be fully tested by generating code and verifying that Handlers/ directory contains handler classes with Handle() methods returning NotImplementedException or TODO comments.

**Acceptance Scenarios**:

1. **Given** an AddPetCommand exists, **When** code is generated, **Then** an AddPetCommandHandler class exists implementing IRequestHandler<AddPetCommand, Pet> with a Handle method containing TODO comment
2. **Given** a GetPetByIdQuery exists, **When** code is generated, **Then** a GetPetByIdQueryHandler class exists with implementation scaffold
3. **Given** handlers already exist from previous generation, **When** code is regenerated, **Then** existing handler files are NOT overwritten (preserve manual implementations)

---

### User Story 4 - MediatR Service Registration (Priority: P1)

As a developer using the OpenAPI Generator, I want MediatR automatically registered in the DI container, so that my application works out of the box without manual configuration.

**Why this priority**: Without proper DI registration, the generated code won't work at runtime. This is essential infrastructure that must be delivered with US1 and US2.

**Independent Test**: Can be fully tested by generating code, building the project, and running tests that verify MediatR is registered and can resolve handlers.

**Acceptance Scenarios**:

1. **Given** generated code, **When** the application starts, **Then** MediatR is registered in the DI container via builder.Services.AddMediatR()
2. **Given** MediatR is registered, **When** an endpoint receives a request, **Then** it can successfully resolve and execute the corresponding handler
3. **Given** generated code, **When** examining Program.cs or extensions, **Then** MediatR registration is automatic and requires no manual developer intervention

---

### User Story 5 - Remove Technical Debt (Priority: P1)

As a maintainer of the OpenAPI Generator, I want to remove all Pet-specific vendor extensions and CRUD implementations from api.mustache, so that the template is truly general-purpose and works with any OpenAPI specification.

**Why this priority**: This addresses the critical technical debt identified in Phase 5 - the template currently contains hardcoded Petstore logic that makes it unsuitable for production use with other APIs.

**Independent Test**: Can be fully tested by code review of api.mustache verifying: no vendor extensions (x-isAddPet, x-isGetPetById, etc.), no Dictionary<long, Pet> declarations, no CRUD implementations, only MediatR delegation logic.

**Acceptance Scenarios**:

1. **Given** api.mustache template, **When** examining the file, **Then** there are no vendor extension checks like {{#vendorExtensions.x-isAddPet}}
2. **Given** api.mustache template, **When** examining the file, **Then** there are no in-memory data structures like Dictionary<long, Pet>
3. **Given** api.mustache template, **When** examining the file, **Then** there are no CRUD implementations (only MediatR.Send() calls)
4. **Given** MinimalApiServerCodegen.java, **When** examining addOperationToGroup method, **Then** it does not add x-isPetApi or operation-specific vendor extensions

---

### User Story 6 - Configure MediatR Usage (Priority: P1)

As a developer using the OpenAPI Generator, I want to control whether MediatR pattern is used via a configuration flag, so that I can choose between simple TODO stubs (for quick prototyping) or MediatR architecture (for production applications).

**Why this priority**: Not all projects need the complexity of MediatR - some developers want simple stubs to implement directly. This flexibility makes the generator useful for both quick prototypes and production systems.

**Independent Test**: Can be fully tested by generating code twice (once with `useMediatr=false`, once with `useMediatr=true`) and verifying the outputs differ correctly: plain stubs with TODOs vs MediatR commands/queries/handlers.

**Acceptance Scenarios**:

1. **Given** generator invoked with `--additional-properties useMediatr=false`, **When** code is generated, **Then** endpoints contain only TODO comments (no MediatR references, no commands/queries generated)
2. **Given** generator invoked with `--additional-properties useMediatr=true`, **When** code is generated, **Then** endpoints delegate to MediatR and commands/queries/handlers are generated
3. **Given** generator invoked without specifying `useMediatr`, **When** code is generated, **Then** it defaults to `useMediatr=false` (plain stubs for backward compatibility)
4. **Given** `useMediatr=false`, **When** examining generated csproj, **Then** MediatR package reference is not included
5. **Given** `useMediatr=true`, **When** examining generated csproj, **Then** MediatR package reference is included

---

### Edge Cases

- What happens when an OpenAPI operation has complex query parameters (arrays, objects) - how are these mapped to command/query properties?
- How does the system handle operations with file uploads (multipart/form-data) in MediatR pattern?
- What happens when an OpenAPI spec changes and code is regenerated - how are existing handler implementations preserved?
- How does the system handle operations with multiple response types (200, 201, 400, 404) in MediatR pattern?
- What happens when an operation has no request body and no parameters (e.g., GET /health) - is an empty query still generated?
- How does validation (FluentValidation) integrate with MediatR pipeline vs inline endpoint validation?
- What happens if `useMediatr` is changed between generations (false → true or true → false) - does regeneration handle the transition correctly?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST generate endpoint files that contain ONLY MediatR delegation logic (no business logic implementations)
- **FR-002**: System MUST generate a MediatR command class for each POST, PUT, PATCH, DELETE operation in the OpenAPI spec
- **FR-003**: System MUST generate a MediatR query class for each GET operation in the OpenAPI spec
- **FR-004**: Command/Query classes MUST include properties for all operation parameters (path, query, header, request body)
- **FR-005**: Command/Query classes MUST implement IRequest<TResponse> where TResponse matches the OpenAPI operation response type
- **FR-006**: System MUST generate handler scaffold classes implementing IRequestHandler<TRequest, TResponse> for each command/query
- **FR-007**: System MUST preserve existing handler implementations when code is regenerated (handlers marked as non-regeneratable)
- **FR-008**: System MUST automatically register MediatR in the DI container (Program.cs or extension method)
- **FR-009**: System MUST remove all vendor extensions (x-isAddPet, x-isGetPetById, x-isUpdatePet, x-isDeletePet) from MinimalApiServerCodegen.java
- **FR-010**: System MUST remove all in-memory data structures and CRUD implementations from api.mustache template
- **FR-011**: System MUST convert array-type query parameters to native arrays (T[]) as established in Phase 5
- **FR-012**: System MUST use HttpContext-based JSON deserialization for complex object query parameters as established in Phase 5
- **FR-013**: System MUST maintain automatic basePath extraction from OpenAPI server URL as established in Phase 5
- **FR-014**: Endpoint methods MUST inject IMediator via parameter and call SendAsync() with the generated command/query
- **FR-015**: System MUST organize generated files into logical folders: Features/{Tag}/ for endpoints, Commands/ and Queries/ for requests, Handlers/ for handler scaffolds
- **FR-016**: System MUST support a configuration option `useMediatr` (boolean) to toggle between MediatR pattern and plain minimal API stubs
- **FR-017**: When `useMediatr=false`, system MUST generate plain endpoint stubs with TODO comments (no MediatR dependencies)
- **FR-018**: When `useMediatr=true`, system MUST generate MediatR commands/queries/handlers and configure DI registration
- **FR-019**: Configuration option MUST be settable via OpenAPI Generator CLI (e.g., `--additional-properties useMediatr=true`)
- **FR-020**: Default value for `useMediatr` MUST be `false` to maintain backward compatibility

### Key Entities *(include if feature involves data)*

- **Command**: Represents a request to mutate state (POST, PUT, PATCH, DELETE operations). Contains properties for operation parameters and request body. Implements IRequest<TResponse>.
- **Query**: Represents a request to retrieve state (GET operations). Contains properties for operation parameters. Implements IRequest<TResponse>.
- **Handler**: Processes a command or query. Implements IRequestHandler<TRequest, TResponse>. Contains business logic (manually implemented, not generated after first scaffold).
- **Endpoint**: Minimal API route definition. Receives HTTP request, maps to command/query, sends via MediatR, returns HTTP response.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Generated endpoint files contain zero lines of business logic (only parameter mapping and MediatR.Send() calls when useMediatr=true, or TODO comments when useMediatr=false)
- **SC-002**: Code generation creates exactly one command/query class per OpenAPI operation when useMediatr=true
- **SC-003**: Regenerating code preserves 100% of existing handler implementations (no overwrites)
- **SC-004**: Applications built with generated code compile successfully without additional manual MediatR configuration
- **SC-005**: All 7 existing baseline tests pass with both useMediatr=true and useMediatr=false configurations
- **SC-006**: api.mustache template contains zero vendor extension conditionals
- **SC-007**: MinimalApiServerCodegen.java addOperationToGroup method contains zero vendor extension assignments
- **SC-008**: Code review confirms zero Pet-specific or Petstore-specific logic in any template file
- **SC-009**: Generated code with useMediatr=false contains zero MediatR package references
- **SC-010**: Configuration defaults to useMediatr=false when not specified (backward compatibility verified)

## Scope & Boundaries *(mandatory)*

### In Scope

- Configuration option `useMediatr` to toggle between MediatR pattern and plain stubs
- Generation of MediatR command classes for mutating operations (POST, PUT, PATCH, DELETE) when useMediatr=true
- Generation of MediatR query classes for read operations (GET) when useMediatr=true
- Generation of MediatR handler scaffolds with TODO comments (initial generation only) when useMediatr=true
- Generation of plain endpoint stubs with TODO comments when useMediatr=false
- Automatic MediatR DI registration in generated application when useMediatr=true
- Conditional inclusion of MediatR NuGet package based on useMediatr setting
- Removal of all vendor extensions from code generator
- Removal of all implementation logic from api.mustache template
- Preservation of existing handler implementations during regeneration
- Integration with existing FluentValidation support
- Maintaining all Phase 5 improvements (array conversion, complex query params, basePath)
- Backward compatibility (useMediatr defaults to false)

### Out of Scope

- Implementing actual business logic in handlers (remains developer responsibility)
- Generating integration tests for MediatR handlers (tests remain as-is, testing endpoints)
- Adding MediatR pipeline behaviors (logging, validation middleware) beyond basic registration
- Supporting MediatR notifications (INotification) - only IRequest patterns
- Generating repository or database access patterns
- Auto-detection of whether to generate commands vs queries (based on HTTP method convention only)
- Supporting non-standard MediatR patterns (custom request types)

## Assumptions *(mandatory)*

- MediatR library version 12.x will be used (latest stable as of late 2024) when useMediatr=true
- Developers will manually implement handler logic after initial scaffold generation (when useMediatr=true)
- Developers will manually implement endpoint logic in TODO sections (when useMediatr=false)
- Existing test suite verifies endpoint behavior, not handler behavior (handlers tested separately if needed)
- OpenAPI specifications follow standard conventions (GET = read, POST/PUT/DELETE = write)
- Developers choosing useMediatr=true understand MediatR pattern and CQRS concepts
- Handler scaffolds generated only once - subsequent regenerations skip existing handlers
- FluentValidation remains inline in endpoints (not moved to MediatR pipeline behaviors)
- Generated code targets .NET 8.0 LTS (same as current baseline)
- Default behavior (useMediatr=false) provides simplest possible stubs for quick prototyping
- Projects can switch from useMediatr=false to useMediatr=true but manual migration of existing implementations required

## Dependencies *(mandatory)*

### External Dependencies

- MediatR NuGet package (12.x) - conditionally added to generated project dependencies only when useMediatr=true
- Microsoft.Extensions.DependencyInjection.Abstractions (already present in .NET 8)

### Internal Dependencies

- Phase 4 work: Template structure and Mustache generation system
- Phase 5 work: Array type conversion, complex query parameter handling, basePath extraction
- OpenAPI Generator framework: AbstractCSharpCodegen base class, CodegenOperation/CodegenParameter models

### Constraints

- Must maintain backward compatibility with OpenAPI Generator 7.17.0 framework
- Must preserve all existing functionality from Phases 1-5 (no regressions)
- Generated handler files must be marked non-regeneratable to prevent overwrites
- MediatR registration must not conflict with existing DI registrations
