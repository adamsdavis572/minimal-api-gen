#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;
using MediatR;
using PetstoreApi.Commands;
using PetstoreApi.Queries;

namespace PetstoreApi.Endpoints;

/// <summary>
/// Minimal API endpoints for PetApi operations
/// </summary>
public static class PetApiEndpoints
{
    /// <summary>
    /// Maps all PetApi endpoints to the route group
    /// </summary>
    public static RouteGroupBuilder MapPetApiEndpoints(this RouteGroupBuilder group)
    {
        // Post /pet - Add a new pet to the store
        group.MapPost("/pet", async (HttpContext httpContext, [FromBody] Pet pet) =>
        {
            // MediatR delegation
            var mediator = httpContext.RequestServices.GetRequiredService<IMediator>();
            var command = new AddPetCommand
            {
                pet = pet
            };
            var result = await mediator.Send(command);
            return Results.Ok(result);
        })
        .WithName("AddPet")
        .WithSummary("Add a new pet to the store")
        .Produces<Pet>(200)
        .ProducesProblem(400);

        // Delete /pet/{petId} - Deletes a pet
        group.MapDelete("/pet/{petId}", async (HttpContext httpContext, long petId, [FromHeader] string apiKey) =>
        {
            // MediatR delegation
            var mediator = httpContext.RequestServices.GetRequiredService<IMediator>();
            var command = new DeletePetCommand
            {
                petId = petId,
                apiKey = apiKey
            };
            var result = await mediator.Send(command);
            return Results.NoContent();
        })
        .WithName("DeletePet")
        .WithSummary("Deletes a pet")
        .ProducesProblem(400);

        // Get /pet/findByStatus - Finds Pets by status
        group.MapGet("/pet/findByStatus", async (HttpContext httpContext, [FromQuery] string[] status) =>
        {
            // MediatR delegation
            var mediator = httpContext.RequestServices.GetRequiredService<IMediator>();
            var query = new FindPetsByStatusQuery
            {
                status = status
            };
            var result = await mediator.Send(query);
            return Results.Ok(result);
        })
        .WithName("FindPetsByStatus")
        .WithSummary("Finds Pets by status")
        .Produces<List<Pet>>(200)
        .ProducesProblem(400);

        // Get /pet/findByTags - Finds Pets by tags
        group.MapGet("/pet/findByTags", async (HttpContext httpContext, [FromQuery] string[] tags) =>
        {
            // MediatR delegation
            var mediator = httpContext.RequestServices.GetRequiredService<IMediator>();
            var query = new FindPetsByTagsQuery
            {
                tags = tags
            };
            var result = await mediator.Send(query);
            return Results.Ok(result);
        })
        .WithName("FindPetsByTags")
        .WithSummary("Finds Pets by tags")
        .Produces<List<Pet>>(200)
        .ProducesProblem(400);

        // Get /pet/{petId} - Find pet by ID
        group.MapGet("/pet/{petId}", async (HttpContext httpContext, long petId) =>
        {
            // MediatR delegation
            var mediator = httpContext.RequestServices.GetRequiredService<IMediator>();
            var query = new GetPetByIdQuery
            {
                petId = petId
            };
            var result = await mediator.Send(query);
            return Results.Ok(result);
        })
        .WithName("GetPetById")
        .WithSummary("Find pet by ID")
        .Produces<Pet>(200)
        .ProducesProblem(400);

        // Put /pet - Update an existing pet
        group.MapPut("/pet", async (HttpContext httpContext, [FromBody] Pet pet) =>
        {
            // MediatR delegation
            var mediator = httpContext.RequestServices.GetRequiredService<IMediator>();
            var command = new UpdatePetCommand
            {
                pet = pet
            };
            var result = await mediator.Send(command);
            return Results.Ok(result);
        })
        .WithName("UpdatePet")
        .WithSummary("Update an existing pet")
        .Produces<Pet>(200)
        .ProducesProblem(400);

        return group;
    }
}
