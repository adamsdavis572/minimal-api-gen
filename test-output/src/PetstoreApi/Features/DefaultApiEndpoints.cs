#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;

namespace PetstoreApi.Endpoints;

/// <summary>
/// Minimal API endpoints for DefaultApi operations
/// </summary>
public static class DefaultApiEndpoints
{
    /// <summary>
    /// Maps all DefaultApi endpoints to the route group
    /// </summary>
    public static RouteGroupBuilder MapDefaultApiEndpoints(this RouteGroupBuilder group)
    {
        // Get /test - Test API
        group.MapGet("/test", ([FromQuery] TestEnum testQuery) =>
        {
            // TODO: Implement TestGet logic
            return Results.NoContent();
        })
        .WithName("TestGet")
        .WithSummary("Test API")
        .ProducesProblem(400);

        return group;
    }
}
