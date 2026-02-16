# Dual-Mode Integration Testing Infrastructure

## Overview

The petstore-tests infrastructure now supports **dual-mode integration testing** for ASP.NET Core Minimal API applications. This allows tests to run in two modes:

1. **Open Mode** - Authorization is bypassed, allowing all requests regardless of security configuration
2. **Secure Mode** - Authentication is enforced via HTTP headers, simulating authenticated users with roles

## Quick Start

### Open Mode (Default)

Use Open Mode for basic functionality testing where authorization is not the focus:

```csharp
public class MyTests
{
    [Fact]
    public async Task Test_BasicFunctionality()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Open  // Authorization bypassed
        };
        var client = factory.CreateClient();

        // Act & Assert - No auth headers needed
        var response = await client.GetAsync("/api/endpoint");
        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }
}
```

### Secure Mode

Use Secure Mode to test authentication and authorization:

```csharp
public class MySecureTests
{
    [Fact]
    public async Task Test_WithAuthentication()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Secure  // Authentication enforced
        };
        var client = factory.CreateClient();

        // Act - Provide authentication headers
        var request = new HttpRequestMessage(HttpMethod.Get, "/api/secure-endpoint");
        request.Headers.Add("X-Test-UserId", "user123");
        request.Headers.Add("X-Test-Role", "Admin");
        
        var response = await client.SendAsync(request);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }
}
```

## Architecture

### TestMode Enum

```csharp
public enum TestMode
{
    Open,    // Authorization bypassed
    Secure   // Authentication via headers
}
```

### CustomWebApplicationFactory

The factory now accepts a `Mode` property to configure test behavior:

```csharp
var factory = new CustomWebApplicationFactory
{
    Mode = TestMode.Open  // or TestMode.Secure
};
```

### Authentication Handlers

#### MockAuthHandler (Secure Mode)

`MockAuthHandler` is an `AuthenticationHandler<AuthenticationSchemeOptions>` that:
- Reads user claims from HTTP headers
- **X-Test-UserId** (required): Maps to `NameIdentifier` and `Name` claims
- **X-Test-Role** (optional): Maps to `Role` claim (defaults to "User")
- Returns authentication success/failure based on headers

**Header Requirements:**
- `X-Test-UserId`: **Required** - Authentication fails if missing or empty
- `X-Test-Role`: **Optional** - Defaults to "User" if not provided

#### BypassAuthHandler (Open Mode)

`BypassAuthHandler` is an `IAuthorizationHandler` that:
- Automatically satisfies all authorization requirements
- Iterates through `context.PendingRequirements`
- Calls `context.Succeed(requirement)` for each requirement

## Usage Patterns

### Testing Without Authentication (Open Mode)

```csharp
[Fact]
public async Task OpenMode_AllowsRequestWithoutAuthHeaders()
{
    var factory = new CustomWebApplicationFactory { Mode = TestMode.Open };
    var client = factory.CreateClient();
    
    // No auth headers needed - authorization bypassed
    var response = await client.PostAsJsonAsync("/v2/pet", newPet);
    
    response.StatusCode.Should().Be(HttpStatusCode.Created);
}
```

### Testing With Default User Role (Secure Mode)

```csharp
[Fact]
public async Task SecureMode_UsesDefaultRole()
{
    var factory = new CustomWebApplicationFactory { Mode = TestMode.Secure };
    var client = factory.CreateClient();
    
    var request = new HttpRequestMessage(HttpMethod.Post, "/v2/pet")
    {
        Content = JsonContent.Create(newPet)
    };
    request.Headers.Add("X-Test-UserId", "user456");
    // X-Test-Role omitted - will default to "User"
    
    var response = await client.SendAsync(request);
    response.StatusCode.Should().Be(HttpStatusCode.Created);
}
```

### Testing Multiple Roles (Secure Mode)

```csharp
[Fact]
public async Task SecureMode_SupportsMultipleRoles()
{
    var factory = new CustomWebApplicationFactory { Mode = TestMode.Secure };
    var client = factory.CreateClient();
    
    // Test as regular user
    var userRequest = new HttpRequestMessage(HttpMethod.Post, "/v2/pet")
    {
        Content = JsonContent.Create(newPet)
    };
    userRequest.Headers.Add("X-Test-UserId", "user123");
    userRequest.Headers.Add("X-Test-Role", "User");
    
    // Test as admin
    var adminRequest = new HttpRequestMessage(HttpMethod.Post, "/v2/pet")
    {
        Content = JsonContent.Create(newPet)
    };
    adminRequest.Headers.Add("X-Test-UserId", "admin456");
    adminRequest.Headers.Add("X-Test-Role", "Admin");
    
    var userResponse = await client.SendAsync(userRequest);
    var adminResponse = await client.SendAsync(adminRequest);
    
    // Both should succeed (role-specific behavior tested separately)
    userResponse.StatusCode.Should().Be(HttpStatusCode.Created);
    adminResponse.StatusCode.Should().Be(HttpStatusCode.Created);
}
```

### Testing Authentication Failures (Secure Mode)

```csharp
[Fact]
public async Task SecureMode_FailsWithoutUserId()
{
    var factory = new CustomWebApplicationFactory { Mode = TestMode.Secure };
    var client = factory.CreateClient();
    
    // No headers - authentication should fail
    var response = await client.PostAsJsonAsync("/v2/pet", newPet);
    
    // Status code depends on endpoint authorization
    response.StatusCode.Should().BeOneOf(
        HttpStatusCode.Created,      // If endpoint doesn't require auth
        HttpStatusCode.Unauthorized, // If endpoint requires auth
        HttpStatusCode.Forbidden     // If authorization fails
    );
}
```

## Implementation Files

- **`TestAuthentication/MockAuthHandler.cs`**: Header-based authentication handler for Secure Mode
- **`TestAuthentication/BypassAuthHandler.cs`**: Authorization bypass handler for Open Mode  
- **`TestAuthentication/TestAuthenticationExtensions.cs`**: Extension methods to configure modes
- **`CustomWebApplicationFactory.cs`**: Updated factory with TestMode support
- **`DualModeAuthTests.cs`**: Example tests demonstrating both modes

## Extension Methods

### AddOpenModeAuth

```csharp
services.AddOpenModeAuth();
```

Configures services for Open Mode:
- Registers `BypassAuthHandler` as `IAuthorizationHandler`
- Adds authorization services

### AddSecureModeAuth

```csharp
services.AddSecureModeAuth();
```

Configures services for Secure Mode:
- Registers authentication with `TestScheme`
- Configures `MockAuthHandler` to read from headers
- Adds authorization services

## Best Practices

1. **Use Open Mode for basic functionality tests** - Focus on business logic without auth complexity
2. **Use Secure Mode for authorization tests** - Test role-based access control and user-specific behavior
3. **Always provide X-Test-UserId in Secure Mode** - Authentication will fail without it
4. **Use descriptive user IDs** - E.g., "admin123", "user456" for clarity
5. **Test both with and without roles** - Verify default role behavior and role-specific logic

## Migration Guide

### Updating Existing Tests

Existing tests continue to work with the default `TestMode.Open`:

```csharp
// Old code - still works
var factory = new CustomWebApplicationFactory();
var client = factory.CreateClient();

// Equivalent to
var factory = new CustomWebApplicationFactory { Mode = TestMode.Open };
var client = factory.CreateClient();
```

### Adding Authentication Tests

To test authenticated endpoints:

```csharp
// Old approach - not possible
// ❌ No way to provide authentication

// New approach - Secure Mode
✅ var factory = new CustomWebApplicationFactory { Mode = TestMode.Secure };
var client = factory.CreateClient();
var request = new HttpRequestMessage(HttpMethod.Get, "/api/secure");
request.Headers.Add("X-Test-UserId", "testuser");
```

## Examples

See `DualModeAuthTests.cs` for complete examples including:
- Open Mode without auth headers
- Secure Mode with auth headers
- Default role behavior
- Multiple role testing
- Health check endpoints in both modes

## Troubleshooting

### Tests fail with "Missing X-Test-UserId header"

**Cause**: Using Secure Mode without providing the required user ID header.

**Solution**: Add the header or switch to Open Mode:
```csharp
request.Headers.Add("X-Test-UserId", "testuser");
```

### Authorization still enforced in Open Mode

**Cause**: Factory mode not set correctly.

**Solution**: Explicitly set mode:
```csharp
var factory = new CustomWebApplicationFactory { Mode = TestMode.Open };
```

### Authentication not working in Secure Mode

**Cause**: Missing authentication middleware or incorrect header format.

**Solution**: Ensure headers are added to request, not client:
```csharp
// ❌ Wrong - headers on client
client.DefaultRequestHeaders.Add("X-Test-UserId", "user");

// ✅ Right - headers on request
var request = new HttpRequestMessage(HttpMethod.Get, "/api/endpoint");
request.Headers.Add("X-Test-UserId", "user");
```
