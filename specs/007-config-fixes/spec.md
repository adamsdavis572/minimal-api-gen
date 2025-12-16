# Feature Specification: DTO Validation Architecture

**Feature Branch**: `007-config-fixes`  
**Created**: 2025-12-12  
**Updated**: 2025-12-16
**Status**: In Progress  
**Input**: Implement true CQRS with separate DTOs for API contracts, comprehensive FluentValidation on DTOs using OpenAPI schema constraints (minLength, maxLength, pattern, minimum, maximum, minItems, maxItems), and fix validation configuration options. This addresses technical debt from 006-mediatr-decoupling where Commands reference Models directly instead of DTOs.

## User Scenarios & Testing *(mandatory)*

### User Story 0 - Separate DTOs for CQRS (Priority: P0 - Critical Architectural Fix)

As a generator user implementing CQRS with MediatR, I want Commands and Queries to reference separate DTO classes (not domain Models directly), so that my API contracts are decoupled from my domain models and I can validate request data independently from domain logic.

**Why this priority**: P0 (blocking) because this is fundamental CQRS architecture that was missing from 006-mediatr-decoupling. Without DTOs, Commands are tightly coupled to Models, preventing proper separation of concerns and making validation ambiguous (validate Model or Command?).

**Independent Test**: Can be fully tested by generating code and verifying that: (1) DTOs/ directory exists with DTO classes, (2) AddPetCommand references AddPetDto (not Pet Model), (3) Handler can map DTO to Model independently.

**Acceptance Scenarios**:

1. **Given** OpenAPI operation with requestBody schema, **When** generator runs with `useMediatr=true`, **Then** DTO class is generated in DTOs/ directory matching requestBody schema
2. **Given** generated AddPetCommand, **When** examining class properties, **Then** Command contains AddPetDto property (not Pet Model property)
3. **Given** generated DTO classes, **When** examining properties, **Then** DTOs have identical structure to requestBody schema (property names, types, nullability)
4. **Given** generated Handler, **When** examining Handle method, **Then** Handler receives Command with DTO and is responsible for mapping DTO to Model
5. **Given** DTOs and Models, **When** comparing structures, **Then** they may differ (DTOs are API contract, Models are domain), allowing independent evolution

---

### User Story 1 - FluentValidation on DTOs (Priority: P1)

As a generator user, when I enable `useValidators=true` and my OpenAPI spec contains validation constraints (required fields, patterns, min/max values), I want the generator to automatically create FluentValidation validator classes for my DTO classes (not Models), so that API contract validation is separated from domain logic and enforced before reaching handlers.

**Why this priority**: This is the highest priority implementation task because FluentValidation infrastructure is currently always included (adding dependency overhead) but never used. Validators must target DTOs (not Models) to maintain CQRS separation. This delivers immediate value by providing working validation at the API boundary.

**Independent Test**: Can be fully tested by running the generator with `useValidators=true` on the petstore.yaml spec (which has required fields, patterns, and min/max constraints) and verifying that validator classes are generated (e.g., AddPetDtoValidator.cs with RuleFor rules), and that validation executes correctly when posting invalid data, returning 400 before reaching the Handler.

**Acceptance Scenarios**:

1. **Given** OpenAPI spec with required fields (e.g., Pet has required name and photoUrls), **When** generator runs with `useValidators=true`, **Then** DTO validator class is generated with NotEmpty() rules for required fields
2. **Given** OpenAPI spec with string constraints (minLength=1, maxLength=100), **When** generator runs with `useValidators=true`, **Then** DTO validator class is generated with Length(1, 100) rule
3. **Given** OpenAPI spec with pattern constraint (e.g., email regex), **When** generator runs with `useValidators=true`, **Then** DTO validator class is generated with Matches(pattern) rule
4. **Given** OpenAPI spec with numeric constraints (minimum=1, maximum=100), **When** generator runs with `useValidators=true`, **Then** DTO validator class is generated with GreaterThanOrEqualTo(1) and LessThanOrEqualTo(100) rules
5. **Given** OpenAPI spec with array constraints (minItems=1, maxItems=10), **When** generator runs with `useValidators=true`, **Then** DTO validator class is generated with Must(x => x.Count >= 1 && x.Count <= 10) rule
6. **Given** DTO with nested object property (e.g., AddPetDto.category is CategoryDto), **When** generator runs with `useValidators=true`, **Then** validator uses SetValidator(new CategoryDtoValidator()) to chain nested validation
7. **Given** generator runs with `useValidators=false`, **When** templates are processed, **Then** FluentValidation packages are NOT included in project.csproj and no validator registration in Program.cs
8. **Given** generated API with DTO validators, **When** client posts invalid data (missing required field), **Then** API returns 400 Bad Request with RFC 7807 ProblemDetails containing validation errors from DTO validator (not Model validator)
9. **Given** generated API with DTO validators, **When** validation passes, **Then** validated DTO is passed to Handler which maps it to Model for business logic

---

### User Story 2 - Enhanced Petstore Schema for Validation Testing (Priority: P1)

As a generator developer, I want the petstore.yaml test spec to include comprehensive validation constraints (minLength, maxLength, pattern, minimum, maximum, minItems, maxItems) on multiple properties, so that I can verify that all FluentValidation rule types are generated correctly from OpenAPI schemas.

**Why this priority**: Same priority as P1 because we cannot test DTO validation without a spec that has diverse constraints. Current petstore.yaml only has basic 'required' fields, which is insufficient for testing minLength, pattern, etc.

**Independent Test**: Can be fully tested by examining petstore.yaml and confirming it contains at least one example of each constraint type (minLength, maxLength, pattern, minimum, maximum, minItems, maxItems), then generating validators and verifying each constraint appears as FluentValidation rule.

**Acceptance Scenarios**:

1. **Given** Pet schema in petstore.yaml, **When** examining 'name' property, **Then** it has minLength=1 and maxLength=100 constraints
2. **Given** Pet schema in petstore.yaml, **When** examining 'photoUrls' property, **Then** it has minItems=1 and maxItems=10 constraints
3. **Given** User schema in petstore.yaml, **When** examining 'email' property, **Then** it has pattern constraint with email regex
4. **Given** User schema in petstore.yaml, **When** examining 'username' property, **Then** it has minLength=3 and maxLength=50 constraints
5. **Given** Order schema in petstore.yaml, **When** examining 'quantity' property, **Then** it has minimum=1 and maximum=1000 constraints
6. **Given** Category schema in petstore.yaml, **When** examining 'name' property, **Then** it has minLength=1 constraint
7. **Given** enhanced petstore.yaml, **When** generator runs with `useValidators=true`, **Then** all 6+ constraint examples appear as FluentValidation rules in generated validators

---

### User Story 3 - Global Exception Handling (Priority: P2)

As a generator user, when I enable `useValidators=true` and my OpenAPI spec contains validation constraints (required fields, patterns, min/max values), I want the generator to automatically create FluentValidation validator classes for my request models, so that I get compile-time validation code without manual implementation.

**Why this priority**: This is the highest priority because FluentValidation infrastructure is currently always included (adding dependency overhead) but never used. This creates wasted dependencies and misleads users about validation capabilities. Fixing this delivers immediate value by either removing unused dependencies or providing working validation.

**Independent Test**: Can be fully tested by running the generator with `useValidators=true` on the petstore.yaml spec (which has required fields, patterns, and min/max constraints) and verifying that validator classes are generated (e.g., AddPetRequestValidator.cs with RuleFor rules), and that validation executes correctly when posting invalid data.

**Acceptance Scenarios**:

1. **Given** OpenAPI spec with required fields (e.g., Pet has required name and photoUrls), **When** generator runs with `useValidators=true`, **Then** validator class is generated with NotEmpty() rules for required fields
2. **Given** OpenAPI spec with pattern constraint (e.g., User login has regex pattern), **When** generator runs with `useValidators=true`, **Then** validator class is generated with Matches() rule for pattern validation
3. **Given** OpenAPI spec with min/max constraints (e.g., Order quantity between 1-5), **When** generator runs with `useValidators=true`, **Then** validator class is generated with GreaterThanOrEqualTo() and LessThanOrEqualTo() rules
4. **Given** generator runs with `useValidators=false`, **When** templates are processed, **Then** FluentValidation packages are NOT included in project.csproj and no validator registration in Program.cs
5. **Given** generated API with validators, **When** client posts invalid data (missing required field), **Then** API returns 400 Bad Request with RFC 7807 ProblemDetails containing validation errors

---

### User Story 3 - Global Exception Handling (Priority: P2)

As a generator user, when I enable `useGlobalExceptionHandler=true` (the default), I want the generated API to include ASP.NET Core's exception handler middleware that returns RFC 7807 ProblemDetails for unhandled exceptions (including ValidationException from DTO validators), so that my API has consistent error responses without manual configuration.

**Why this priority**: Second priority because the flag exists and defaults to true, but does nothing. This misleads users who expect exception handling to be configured. Implementing this provides production-ready error handling out of the box.

**Independent Test**: Can be fully tested by generating code with default settings (useGlobalExceptionHandler=true), intentionally throwing an exception in an endpoint handler, and verifying that the response is a properly formatted ProblemDetails JSON with status 500 instead of an unhandled exception.

**Acceptance Scenarios**:

1. **Given** generator runs with `useGlobalExceptionHandler=true`, **When** Program.cs is generated, **Then** it contains `app.UseExceptionHandler()` middleware configuration
2. **Given** generated API with exception handler, **When** an unhandled exception occurs in an endpoint, **Then** API returns 500 Internal Server Error with ProblemDetails format (type, title, status, detail fields)
3. **Given** generator runs with `useGlobalExceptionHandler=false`, **When** Program.cs is generated, **Then** no exception handler middleware is registered
4. **Given** `useProblemDetails=true` and `useGlobalExceptionHandler=true`, **When** exception handler runs, **Then** response follows RFC 7807 ProblemDetails schema

---

### User Story 4 - Clean Up Unused Configuration (Priority: P3)

As a generator maintainer, I want to remove the unused `useRouteGroups` flag from the codebase and document that route groups are the required architecture, so that the configuration surface is simpler and users aren't confused by flags that don't affect generation.

**Why this priority**: Lowest priority because this is technical debt cleanup that doesn't add new functionality. However, it reduces confusion and simplifies maintenance.

**Independent Test**: Can be fully tested by removing the flag from MinimalApiServerCodegen.java, verifying that generator still compiles and runs successfully, and confirming that generated code still uses route groups (MapGroup pattern) regardless of any flag value.

**Acceptance Scenarios**:

1. **Given** `useRouteGroups` flag is removed from code, **When** generator compiles, **Then** no compilation errors occur
2. **Given** flag is removed, **When** generator runs with or without old `useRouteGroups=false` property, **Then** generated code always uses MapGroup pattern
3. **Given** flag is removed, **When** `--help` output is generated, **Then** `useRouteGroups` is not listed in available options
4. **Given** flag is removed, **When** documentation is updated, **Then** CONFIGURATION.md states "Route groups (MapGroup) are required architecture, not configurable"

---

### Edge Cases

- What happens when OpenAPI spec has validation constraints but user sets `useValidators=false`? (Answer: No validators generated, validation not enforced - this is expected behavior)
- What happens when OpenAPI spec has no validation constraints but user sets `useValidators=true`? (Answer: Validator classes still generated but with empty rule sets - this is acceptable for consistency)
- What happens when user has existing code that sets `useRouteGroups=false` in CLI args? (Answer: Flag is ignored with no error - backward compatible but should log warning if possible)
- What happens when exception handler and problem details are both disabled? (Answer: Unhandled exceptions return default ASP.NET Core error page - this is expected)
- What happens with complex validation constraints not supported by FluentValidation directly? (Answer: Use reasonable mappings - e.g., minLength/maxLength → Length(min, max), unsupported constraints skipped with comment in generated code)
- What happens when requestBody schema is identical for multiple operations? (Answer: Generate single shared DTO class reused by multiple Commands to avoid duplication)
- What happens when DTO structure differs from Model structure? (Answer: Handler must manually map - this is expected in CQRS, allows DTOs to evolve independently)
- What happens when nested DTO references another DTO that also needs validation? (Answer: Generate validator for nested DTO and use SetValidator() to chain, validation cascades automatically)
- What happens when array property has constraints on both array (minItems/maxItems) AND items (pattern, minLength)? (Answer: Generate RuleForEach() for item validation plus Must() for array size validation)
- What happens when useMediatr=false? (Answer: No DTOs generated, endpoints use Models directly via [FromBody], validation targets Models - maintains backward compatibility)

## Assumptions *(optional)*

- OpenAPI Generator framework provides access to all schema constraint properties (minLength, maxLength, pattern, etc.) via CodegenProperty objects
- FluentValidation 11.x supports all constraint types we need (NotEmpty, Length, Matches, GreaterThan, SetValidator, RuleForEach)
- Existing test suite can be updated to work with DTOs instead of Models without major rewrites
- DTO-to-Model mapping in Handlers is manual (no auto-mapping library like AutoMapper assumed)
- petstore.yaml can be updated with validation constraints without breaking existing functionality
- Validation occurs at API boundary before MediatR pipeline (not in MediatR behavior)
- Exception handler can distinguish ValidationException from other exceptions to return 400 vs 500
- DTOs will use C# record types (like Commands/Queries) for immutability and value semantics

## Requirements *(mandatory)*

### Functional Requirements

**DTO Architecture (P0)**:

- **FR-000**: Generator MUST create DTOs/ directory for Data Transfer Object classes separate from Models/ directory
- **FR-001**: Generator MUST generate DTO classes (e.g., AddPetDto.cs) for each requestBody schema in OpenAPI spec when `useMediatr=true`
- **FR-002**: DTO properties MUST exactly match requestBody schema properties (names, types, nullability, structure)
- **FR-003**: Command/Query classes MUST reference DTO types (not Model types) for body parameters when `useMediatr=true`
- **FR-004**: Example: AddPetCommand MUST have `public AddPetDto pet { get; init; }` (not `public Pet pet`)
- **FR-005**: Generator MUST preserve Models/ directory for domain model classes (unchanged from current behavior)
- **FR-006**: Handlers MUST receive Commands with DTOs and be responsible for DTO→Model mapping
- **FR-007**: DTOs and Models MAY have different structures to allow independent evolution of API contract vs domain

**FluentValidation Generation (P1)**:

- **FR-008**: Generator MUST read validation constraints from OpenAPI schema (required, pattern, minLength, maxLength, minimum, maximum, minItems, maxItems, format)
- **FR-009**: Generator MUST generate validator classes (e.g., AddPetDtoValidator.cs) inheriting from AbstractValidator<TDto> when `useValidators=true`
- **FR-010**: Validators MUST target DTO classes (not Model classes) to validate API contract before business logic
- **FR-011**: Validator classes MUST contain FluentValidation rules mapped from OpenAPI constraints:
  - required → NotEmpty() or NotNull()
  - minLength/maxLength → Length(min, max)
  - pattern → Matches(regex)
  - minimum/maximum → GreaterThanOrEqualTo(min) and LessThanOrEqualTo(max)
  - minItems/maxItems → Must(x => x.Count >= min && x.Count <= max)
- **FR-012**: For nested DTOs, validators MUST use SetValidator(new NestedDtoValidator()) to chain validation
- **FR-013**: Generator MUST conditionally include FluentValidation NuGet packages in project.csproj only when `useValidators=true`
- **FR-014**: Generator MUST conditionally register validators in Program.cs (AddValidatorsFromAssemblyContaining) only when `useValidators=true`
- **FR-015**: Generated validators MUST integrate with ASP.NET Core's validation pipeline to return ValidationProblem responses
- **FR-016**: Validation MUST occur before MediatR handler execution (at API boundary)

**Enhanced Petstore Schema (P1)**:

- **FR-017**: petstore.yaml MUST include minLength and maxLength on Pet.name property (e.g., minLength=1, maxLength=100)
- **FR-018**: petstore.yaml MUST include minItems and maxItems on Pet.photoUrls property (e.g., minItems=1, maxItems=10)
- **FR-019**: petstore.yaml MUST include pattern constraint on User.email property (email regex)
- **FR-020**: petstore.yaml MUST include minLength and maxLength on User.username property (e.g., minLength=3, maxLength=50)
- **FR-021**: petstore.yaml MUST include minimum and maximum on Order.quantity property (e.g., minimum=1, maximum=1000)
- **FR-022**: petstore.yaml MUST include minLength on Category.name property (e.g., minLength=1)
- **FR-023**: Enhanced constraints MUST be backward compatible (not breaking existing tests)

**Global Exception Handler (P2)**:

- **FR-024**: Generator MUST register ASP.NET Core's exception handler middleware in Program.cs when `useGlobalExceptionHandler=true`
- **FR-025**: Exception handler MUST catch ValidationException from DTO validators and return 400 with validation errors
- **FR-026**: Exception handler MUST return RFC 7807 ProblemDetails format for unhandled exceptions (500 status)
- **FR-027**: Exception handler MUST integrate with `useProblemDetails` flag for consistent error response formatting
- **FR-028**: Generator MUST omit exception handler middleware when `useGlobalExceptionHandler=false`

**Configuration Cleanup (P3)**:

- **FR-029**: Generator MUST remove `useRouteGroups` boolean flag from MinimalApiServerCodegen.java
- **FR-030**: Generator MUST remove `setUseRouteGroups()` method and related code
- **FR-031**: Documentation MUST state that route groups (MapGroup) are required architecture
- **FR-032**: Generator MUST continue using MapGroup pattern in all generated code
- **FR-033**: CLI help output MUST not list `useRouteGroups` as an available option

### Key Entities

- **DTO (Data Transfer Object)**: Auto-generated C# record class in DTOs/ directory representing API request contract from OpenAPI requestBody schema, properties match schema exactly, decoupled from domain Models, one DTO per unique requestBody schema
- **Model**: Existing domain model class in Models/ directory representing business entities, may differ from DTOs to allow independent evolution, unchanged from current implementation
- **Command/Query**: MediatR request class that references DTO (not Model) for body parameters, responsible for carrying validated DTO to Handler
- **DTO Validator Class**: Auto-generated C# class inheriting from AbstractValidator<TDto>, contains FluentValidation rules mapped from OpenAPI schema constraints, validates API contract before Handler execution, one validator per DTO
- **Validation Rule**: FluentValidation RuleFor() expression mapping OpenAPI constraint to C# validation logic (e.g., required → NotEmpty(), pattern → Matches(), min/max → range checks, nested DTO → SetValidator())
- **Exception Handler Configuration**: ASP.NET Core middleware setup that catches unhandled exceptions (including ValidationException) and transforms them to RFC 7807 ProblemDetails responses
- **Configuration Flag**: Boolean option in generator that controls conditional code generation (useValidators, useGlobalExceptionHandler, useMediatr)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: DTOs separate from Models - verified by confirming DTOs/ directory exists with 5+ DTO classes (AddPetDto, UpdatePetDto, etc.) and Commands reference DTOs (not Models)
- **SC-002**: DTO validators generated with comprehensive rules - verified by running petstore generation with `useValidators=true` and confirming 5+ DTO validator files exist with rules for all constraint types (NotEmpty, Length, Matches, GreaterThan, SetValidator)
- **SC-003**: Enhanced petstore.yaml has diverse constraints - verified by confirming schema contains at least 6 examples of different constraint types (minLength, maxLength, pattern, minimum, maximum, minItems, maxItems)
- **SC-004**: DTO validation rejects invalid requests - verified by posting Pet with name exceeding maxLength and receiving 400 ValidationProblem response within 100ms with specific error message
- **SC-005**: Nested DTO validation works - verified by posting Pet with invalid nested Category and receiving 400 with errors from CategoryDtoValidator chained via SetValidator
- **SC-006**: FluentValidation packages excluded when disabled - verified by checking project.csproj contains zero FluentValidation package references when `useValidators=false`
- **SC-007**: Exception handler catches validation errors - verified by triggering ValidationException and confirming response is 400 ProblemDetails (not 500)
- **SC-008**: Exception handler returns RFC 7807 for unhandled errors - verified by triggering exception in Handler and confirming response has type, title, status, detail fields matching RFC 7807 schema
- **SC-009**: Configuration surface reduced by 1 unused option - verified by confirming `useRouteGroups` is removed from code, CLI help, and documentation
- **SC-010**: All existing tests pass after DTO refactoring - verified by running baseline test suite and confirming 100% pass rate (tests updated to expect DTOs in Commands)
- **SC-011**: Generated code compiles in all configurations - verified by testing 8 configuration matrix combinations (validators on/off, exception handler on/off, problem details on/off, mediatr on/off)
- **SC-012**: DTO-to-Model mapping is Handler responsibility - verified by examining generated Handler code and confirming it receives Command with DTO and must manually map to Model

