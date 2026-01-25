using System.Net;
using System.Net.Http.Json;
using FluentAssertions;
using Xunit;

namespace PetstoreApi.Tests;

public class HealthEndpointTests : IClassFixture<CustomWebApplicationFactory>
{
    private readonly HttpClient _client;

    public HealthEndpointTests(CustomWebApplicationFactory factory)
    {
        _client = factory.CreateClient();
    }

    [Fact]
    public async Task Health_Returns200OK()
    {
        // Act
        var response = await _client.GetAsync("/health");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }

    [Fact]
    public async Task Health_ReturnsHealthyStatus()
    {
        // Act
        var response = await _client.GetAsync("/health");
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
