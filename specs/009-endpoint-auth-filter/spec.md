# Feature Specification: Endpoint Authentication & Authorization Filter

**Feature Branch**: `009-endpoint-auth-filter`  
**Created**: 2026-02-14  
**Status**: Draft  
**Input**: User description: "Inject Authentication and Authorization into generated Minimal API endpoints without modifying generated code using IEndpointFilter pattern"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Secure Write Operations (Priority: P1)

API consumers with write permissions can successfully create, update, and delete resources (pets, orders, users), while consumers without write permissions receive a 403 Forbidden response when attempting these operations.

**Why this priority**: Protecting write operations is the most critical security requirement. Unauthorized modifications could corrupt data, violate business rules, or compromise system integrity.

**Independent Test**: Can be fully tested by configuring a user with only read claims, attempting a POST/PUT/DELETE request, and verifying authorization failure. Then test with write claims and verify success.

**Acceptance Scenarios**:

1. **Given** a client authenticated with `permission: write` claim, **When** they POST to `/v2/pet` (AddPet), **Then** the request succeeds with 200/201 status
2. **Given** a client authenticated with only `permission: read` claim, **When** they POST to `/v2/pet` (AddPet), **Then** the request fails with 403 Forbidden
3. **Given** a client authenticated with `permission: write` claim, **When** they PUT to `/v2/pet` (UpdatePet), **Then** the request succeeds
4. **Given** a client authenticated with only `permission: read` claim, **When** they DELETE to `/v2/pet/{id}` (DeletePet), **Then** the request fails with 403 Forbidden

---

### User Story 2 - Secure Read Operations (Priority: P2)

API consumers with read permissions can successfully retrieve resources (pets, orders, users), while unauthenticated or unauthorized consumers receive a 403 Forbidden response.

**Why this priority**: Read operations are important for data privacy and compliance, but less critical than write operations since they don't modify state.

**Independent Test**: Can be fully tested by configuring a user without read claims, attempting a GET request, and verifying authorization failure. Then test with read claims and verify success.

**Acceptance Scenarios**:

1. **Given** a client authenticated with `permission: read` claim, **When** they GET `/v2/pet/{id}` (GetPetById) **Then** the request succeeds with 200 status
2. **Given** an unauthenticated client, **When** they GET `/v2/pet/{id}`, **Then** the request fails with 401 Unauthorized
3. **Given** a client authenticated without any permission claims, **When** they GET `/v2/store/inventory`, **Then** the request fails with 403 Forbidden
4. **Given** a client authenticated with `permission: read` claim, **When** they GET `/v2/user/{username}` (GetUserByName), **Then** the request succeeds

---

### User Story 3 - Generator Integration Without Code Modification (Priority: P1)

Developers can add authorization filtering to generated endpoints without modifying any files in `test-output/Contract/` directory. The authorization logic is injected via route group filters.

**Why this priority**: This is the core constraint of the feature - preserving generated code immutability is essential for maintainability and regeneration workflows.

**Independent Test**: Can be tested by regenerating the API (running `task gen:petstore` with NuGet packaging), verifying that `test-output/Contract/*.cs` files remain unchanged, yet authorization still works correctly.

**Acceptance Scenarios**:

1. **Given** generated endpoint files exist in `test-output/Contract/Endpoints/`, **When** the generator runs again, **Then** those files contain no authentication/authorization code
2. **Given** authorization is configured in `PermissionEndpointFilter`, **When** requests are made to secured endpoints, **Then** authorization is enforced without modifying generated files
3. **Given** a new endpoint is added to the OpenAPI spec, **When** the code is regenerated, **Then** authorization can be applied by updating only the filter's endpoint mapping dictionary

---

### Edge Cases

- What happens when an endpoint name doesn't exist in the filter's mapping dictionary? (Filter should deny access to the request)
- How does the system handle requests when `IAuthorizationService` is not registered? (Should fail with clear error message)
- What happens if a client sends a valid JWT token but with no permission claims? (Should return 403 Forbidden)
- How does the system behave if the same endpoint name exists in multiple API groups? (Filter should use first match in dictionary)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST define two authorization policies: "ReadAccess" requiring claim `permission: read`, and "WriteAccess" requiring claim `permission: write`
- **FR-002**: System MUST implement an `IEndpointFilter` that maintains a mapping of endpoint names to policy names
- **FR-003**: System MUST evaluate authorization for each incoming request based on the endpoint's name metadata
- **FR-004**: Users MUST receive 403 Forbidden response when authorization fails
- **FR-005**: System MUST allow requests to proceed normally when authorization succeeds or when endpoint has no policy mapping
- **FR-006**: System MUST apply authorization filter to all endpoints within the `/v2` route group
- **FR-007**: System MUST wire authentication and authorization middleware in correct order (authentication before authorization)
- **FR-008**: Generated endpoint files in `test-output/Contract/` MUST remain unmodified - no authentication/authorization code in those files
- **FR-009**: System MUST map Pet endpoints as follows: AddPet, UpdatePet, DeletePet require WriteAccess; GetPetById, FindPetsByStatus, FindPetsByTags require ReadAccess
- **FR-010**: System MUST map Store endpoints as follows: PlaceOrder, DeleteOrder require WriteAccess; GetOrderById, GetInventory require ReadAccess
- **FR-011**: System MUST map User endpoints as follows: CreateUser, UpdateUser, DeleteUser require WriteAccess; GetUserByName, LoginUser, LogoutUser require ReadAccess
- **FR-012**: System MUST provide an extension method `AddAuthorizedApiEndpoints` IN ADDITION to the generated `AddApiEndpoints` method. Both methods coexist - developers toggle between them in Program.cs by commenting/uncommenting the desired call. Using `AddApiEndpoints()` adds endpoints without authorization; using `AddAuthorizedApiEndpoints()` applies the permission filter to all endpoints.
- **FR-013**: Filter MUST resolve `IAuthorizationService` from dependency injection to perform authorization checks
- **FR-014**: Filter MUST retrieve endpoint name via `EndpointNameMetadata` from the HTTP context

### Key Entities

- **Authorization Policy**: Represents a security rule with a name (e.g., "ReadAccess") and required claims (e.g., `permission: read`)
- **Endpoint Name**: String identifier assigned to each API endpoint via metadata (e.g., "AddPet", "GetPetById")
- **Permission Claim**: Security claim attached to authenticated user indicating their access level (values: "read" or "write")

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of write operations (POST/PUT/DELETE) reject requests without `permission: write` claim with 403 Forbidden
- **SC-002**: 100% of read operations (GET) reject requests without `permission: read` claim with 403 Forbidden
- **SC-003**: All Bruno integration tests pass after implementing authorization (27 tests, 77 assertions) when run with valid credentials
- **SC-004**: Full petstore regression test with NuGet packaging completes successfully with generated endpoint files remaining unchanged
- **SC-005**: Developers can add authorization to new endpoints by updating only a configuration file, without touching generated code
- **SC-006**: Authorization checks add less than 5ms latency to request processing time

## Assumptions & Dependencies

### Assumptions

- Testing will use the existing Petstore API generated with NuGet packaging enabled
- Bruno integration tests will be updated to include authentication headers with proper claims
- The authorization approach will be implemented as test artifacts first, then evaluated for generator integration if successful
- Authentication mechanism (JWT, cookies, etc.) is out of scope - only authorization (checking permissions) is included

### Dependencies

- Existing Petstore test infrastructure (Bruno tests, xUnit tests, test handlers)
- .NET 8.0 SDK and ASP.NET Core authentication/authorization middleware
- NuGet-enabled code generation workflow
- OpenAPI Generator template structure for potential future generator integration

### Constraints

- **Immutability**: Generated files in Contract directory cannot be modified
- **Separation**: Authorization logic must be injectable without touching generated code
- **Testability**: Solution must pass all existing tests plus new authorization tests
- **Performance**: Authorization overhead must be minimal (< 5ms per request)

## Implementation Notes

**Note on Specification Approach**: This specification intentionally includes implementation-specific details (.NET interfaces, file names, directory structure) because:

1. **Context**: This is an internal tool development project for a code generator, not a customer-facing product
2. **Audience**: The primary stakeholders are developers working on the generator
3. **Requirements Origin**: The user explicitly requested specific implementation patterns (IEndpointFilter, specific file structure)
4. **Value**: The constraint of not modifying generated code IS the core business requirement, which necessitates discussing specific technical approaches

The implementation details serve to clarify the "do not modify generated code" constraint rather than prescribing unnecessary technical decisions.
