#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;

namespace PetstoreApi.Endpoints;

/// <summary>
/// Minimal API endpoints for FakeApi operations
/// </summary>
public static class FakeApiEndpoints
{
    /// <summary>
    /// Maps all FakeApi endpoints to the route group
    /// </summary>
    public static RouteGroupBuilder MapFakeApiEndpoints(this RouteGroupBuilder group)
    {
        // Get /fake/nullable_example_test - Fake endpoint to test nullable example (object)
        group.MapGet("/fake/nullable_example_test", () =>
        {
        })
        .WithName("FakeNullableExampleTest")
        .WithSummary("Fake endpoint to test nullable example (object)")
        .Produces<TestNullable>(200)
        .ProducesProblem(400);

        // Get /fake/parameter_example_test - fake endpoint to test parameter example (object)
        group.MapGet("/fake/parameter_example_test", (HttpContext httpContext) =>
        {
            // Deserialize complex object from query parameter
            var dataJson = httpContext.Request.Query["data"].FirstOrDefault();
            if (string.IsNullOrEmpty(dataJson))
            {
                return Results.BadRequest("Missing required query parameter: data");
            }
            Pet? data = null;
            try
            {
                data = System.Text.Json.JsonSerializer.Deserialize<Pet>(dataJson);
            }
            catch (System.Text.Json.JsonException)
            {
                return Results.BadRequest("Invalid JSON in query parameter: data");
            }
            if (data == null)
            {
                return Results.BadRequest("Failed to deserialize query parameter: data");
            }
            
        })
        .WithName("FakeParameterExampleTest")
        .WithSummary("fake endpoint to test parameter example (object)")
        .ProducesProblem(400);

        return group;
    }
}
