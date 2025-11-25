#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;

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
        group.MapDelete("/store/order/{orderId}", (string orderId) =>
        {
        })
        .WithName("DeleteOrder")
        .WithSummary("Delete purchase order by ID")
        .ProducesProblem(400);

        // Get /store/inventory - Returns pet inventories by status
        group.MapGet("/store/inventory", () =>
        {
        })
        .WithName("GetInventory")
        .WithSummary("Returns pet inventories by status")
        .Produces<Dictionary<string, int>>(200)
        .ProducesProblem(400);

        // Get /store/order/{orderId} - Find purchase order by ID
        group.MapGet("/store/order/{orderId}", (long orderId) =>
        {
        })
        .WithName("GetOrderById")
        .WithSummary("Find purchase order by ID")
        .Produces<Order>(200)
        .ProducesProblem(400);

        // Post /store/order - Place an order for a pet
        group.MapPost("/store/order", async ([FromBody] Order order) =>
        {
        })
        .WithName("PlaceOrder")
        .WithSummary("Place an order for a pet")
        .Produces<Order>(200)
        .ProducesProblem(400);

        return group;
    }
}
