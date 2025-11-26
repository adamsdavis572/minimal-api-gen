#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;
using MediatR;
using PetstoreApi.Commands;
using PetstoreApi.Queries;

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
        group.MapGet("/test", async (IMediator mediator, [FromQuery] TestEnum testQuery) =>
        {
            // MediatR delegation
            var query = new TestGetQuery
            {
                testQuery = testQuery
            };
            var result = await mediator.Send(query);
            return Results.Ok();
        })
        .WithName("TestGet")
        .WithSummary("Test API")
        .ProducesProblem(400);

        return group;
    }
}
