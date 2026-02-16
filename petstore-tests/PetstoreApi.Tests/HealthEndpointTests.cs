using System.Net;
using System.Net.Http.Json;
using FluentAssertions;
using Xunit;

namespace PetstoreApi.Tests;

public class HealthEndpointTests
{
    [Fact]
    public async Task Health_Returns200OK()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory { Mode = TestMode.Open };
        var client = factory.CreateClient();
        
        // Act
        var response = await client.GetAsync("/health");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }

    [Fact]
    public async Task Health_ReturnsHealthyStatus()
    {
        // Arrange
        var factory = new CustomWebApplicationFactory { Mode = TestMode.Open };
        var client = factory.CreateClient();
        
        // Act
        var response = await client.GetAsync("/health");
        var content = await response.Content.ReadFromJsonAsync<HealthResponse>();

        // Assert
        content.Should().NotBeNull();
        content!.Status.Should().Be("healthy");
    }
}

public class HealthResponse
{
    public string Status { get; set; } = string.Empty;
}
