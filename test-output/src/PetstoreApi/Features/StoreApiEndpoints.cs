#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;
using MediatR;
using PetstoreApi.Commands;
using PetstoreApi.Queries;

namespace PetstoreApi.Endpoints;

/// <summary>
/// Minimal API endpoints for StoreApi operations
/// </summary>
public static class StoreApiEndpoints
{
    /// <summary>
    /// Maps all StoreApi endpoints to the route group
    /// </summary>
    public static RouteGroupBuilder MapStoreApiEndpoints(this RouteGroupBuilder group)
    {
        // Delete /store/order/{orderId} - Delete purchase order by ID
        group.MapDelete("/store/order/{orderId}", async (IMediator mediator, string orderId) =>
        {
            // MediatR delegation
            var command = new DeleteOrderCommand
            {
                orderId = orderId
            };
            var result = await mediator.Send(command);
            // DELETE with bool return - check if resource was found
            return result ? Results.NoContent() : Results.NotFound();
        })
        .WithName("DeleteOrder")
        .WithSummary("Delete purchase order by ID")
        .Produces<bool>(200)
        .ProducesProblem(400);

        // Get /store/inventory - Returns pet inventories by status
        group.MapGet("/store/inventory", async (IMediator mediator) =>
        {
            // MediatR delegation
            var query = new GetInventoryQuery
            {
            };
            var result = await mediator.Send(query);
            if (result == null) return Results.NotFound();
            return Results.Ok(result);
        })
        .WithName("GetInventory")
        .WithSummary("Returns pet inventories by status")
        .Produces<Dictionary<string, int>>(200)
        .ProducesProblem(400);

        // Get /store/order/{orderId} - Find purchase order by ID
        group.MapGet("/store/order/{orderId}", async (IMediator mediator, long orderId) =>
        {
            // MediatR delegation
            var query = new GetOrderByIdQuery
            {
                orderId = orderId
            };
            var result = await mediator.Send(query);
            if (result == null) return Results.NotFound();
            return Results.Ok(result);
        })
        .WithName("GetOrderById")
        .WithSummary("Find purchase order by ID")
        .Produces<Order>(200)
        .ProducesProblem(400);

        // Post /store/order - Place an order for a pet
        group.MapPost("/store/order", async (IMediator mediator, [FromBody] Order order) =>
        {
            // MediatR delegation
            var command = new PlaceOrderCommand
            {
                order = order
            };
            var result = await mediator.Send(command);
            if (result == null) return Results.NotFound();
            return Results.Created($"/v2/store/order", result);
        })
        .WithName("PlaceOrder")
        .WithSummary("Place an order for a pet")
        .Produces<Order>(200)
        .ProducesProblem(400);

        return group;
    }
}
