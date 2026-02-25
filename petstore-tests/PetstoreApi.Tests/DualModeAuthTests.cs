using System.Net;
using System.Net.Http.Json;
using FluentAssertions;
using Microsoft.Extensions.DependencyInjection;
using PetstoreApi.DTOs;
using PetstoreApi.Filters;
using Xunit;
using PetstoreApi.Tests.TestAuthentication;

namespace PetstoreApi.Tests;

/// <summary>
/// Tests demonstrating dual-mode integration testing:
/// - Open Mode: Authorization is bypassed, all requests succeed
/// - Secure Mode: Authentication is enforced via HTTP headers
/// </summary>
public class DualModeAuthTests
{
    [Fact]
    public async Task OpenMode_AllowsRequestWithoutAuthHeaders()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Open
        };
        var client = factory.CreateClient();
        
        var newPet = new AddPetDto
        {
            Name = "TestPet",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act - No authentication headers provided
        var response = await client.PostAsJsonAsync("/v2/pet", newPet);

        // Assert - Should succeed in Open Mode
        response.StatusCode.Should().Be(HttpStatusCode.Created);
    }

    [Fact]
    public async Task SecureMode_AuthenticationSystemIsActive()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Secure
        };

        // Skip if no PermissionEndpointFilter is registered - auth is not configured in this generation mode
        var hasPermissionFilter = factory.Services.GetService<PermissionEndpointFilter>() != null;
        if (!hasPermissionFilter) return;

        var client = factory.CreateClient();
        
        var newPet = new AddPetDto
        {
            Name = "TestPet",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act - No authentication headers provided
        var response = await client.PostAsJsonAsync("/v2/pet", newPet);

        // Assert - In Secure Mode, authentication system is active

        response.StatusCode.Should().BeOneOf(
            HttpStatusCode.Unauthorized, // Endpoint requires authentication but none provided
            HttpStatusCode.Forbidden);   // Endpoint requires specific authorization
    }

    [Fact]
    public async Task SecureMode_SucceedsWithValidAuthHeaders()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Secure
        };
        var client = factory.CreateClient();
        
        var newPet = new AddPetDto
        {
            Name = "TestPet",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act - Provide authentication headers with write permission
        var request = new HttpRequestMessage(HttpMethod.Post, "/v2/pet")
        {
            Content = JsonContent.Create(newPet)
        };
        request.Headers.Add(MockAuthHandler.UserIdHeader, "test-user-123");
        request.Headers.Add(MockAuthHandler.RoleHeader, "Admin");
        request.Headers.Add(MockAuthHandler.PermissionHeader, "write");
        
        var response = await client.SendAsync(request);

        // Assert - Should succeed with valid auth headers and write permission
        response.StatusCode.Should().Be(HttpStatusCode.Created);
    }

    [Fact]
    public async Task SecureMode_UsesDefaultRoleWhenRoleHeaderMissing()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Secure
        };
        var client = factory.CreateClient();
        
        var newPet = new AddPetDto
        {
            Name = "TestPet",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act - Provide user ID and write permission but not role (should default to "User")
        var request = new HttpRequestMessage(HttpMethod.Post, "/v2/pet")
        {
            Content = JsonContent.Create(newPet)
        };
        request.Headers.Add(MockAuthHandler.UserIdHeader, "test-user-456");
        request.Headers.Add(MockAuthHandler.PermissionHeader, "write");
        // Note: X-Test-Role header is intentionally omitted
        
        var response = await client.SendAsync(request);

        // Assert - Should succeed with default role and write permission
        response.StatusCode.Should().Be(HttpStatusCode.Created);
    }

    [Fact]
    public async Task SecureMode_SupportsMultipleRoles()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Secure
        };
        var client = factory.CreateClient();
        
        var newPet = new AddPetDto
        {
            Name = "TestPet",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act - Provide different roles in different requests, both with write permission
        var requestAsUser = new HttpRequestMessage(HttpMethod.Post, "/v2/pet")
        {
            Content = JsonContent.Create(newPet)
        };
        requestAsUser.Headers.Add(MockAuthHandler.UserIdHeader, "user-123");
        requestAsUser.Headers.Add(MockAuthHandler.RoleHeader, "User");
        requestAsUser.Headers.Add(MockAuthHandler.PermissionHeader, "write");
        
        var requestAsAdmin = new HttpRequestMessage(HttpMethod.Post, "/v2/pet")
        {
            Content = JsonContent.Create(newPet)
        };
        requestAsAdmin.Headers.Add(MockAuthHandler.UserIdHeader, "admin-456");
        requestAsAdmin.Headers.Add(MockAuthHandler.RoleHeader, "Admin");
        requestAsAdmin.Headers.Add(MockAuthHandler.PermissionHeader, "read,write");
        
        var userResponse = await client.SendAsync(requestAsUser);
        var adminResponse = await client.SendAsync(requestAsAdmin);

        // Assert - Both should succeed with write permission
        userResponse.StatusCode.Should().Be(HttpStatusCode.Created);
        adminResponse.StatusCode.Should().Be(HttpStatusCode.Created);
    }

    [Fact]
    public async Task OpenMode_HealthCheckWorks()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Open
        };
        var client = factory.CreateClient();

        // Act
        var response = await client.GetAsync("/health");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }

    [Fact]
    public async Task SecureMode_HealthCheckWorks()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory
        {
            Mode = TestMode.Secure
        };
        var client = factory.CreateClient();

        // Act - Health check typically doesn't require auth
        var response = await client.GetAsync("/health");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }
}
