#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;

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
        group.MapPost("/pet", async ([FromBody] Pet pet) =>
        {
        })
        .WithName("AddPet")
        .WithSummary("Add a new pet to the store")
        .Produces<Pet>(200)
        .ProducesProblem(400);

        // Delete /pet/{petId} - Deletes a pet
        group.MapDelete("/pet/{petId}", (long petId, [FromHeader] string apiKey) =>
        {
        })
        .WithName("DeletePet")
        .WithSummary("Deletes a pet")
        .ProducesProblem(400);

        // Get /pet/findByStatus - Finds Pets by status
        group.MapGet("/pet/findByStatus", ([FromQuery] string[] status) =>
        {
        })
        .WithName("FindPetsByStatus")
        .WithSummary("Finds Pets by status")
        .Produces<List<Pet>>(200)
        .ProducesProblem(400);

        // Get /pet/findByTags - Finds Pets by tags
        group.MapGet("/pet/findByTags", ([FromQuery] string[] tags) =>
        {
        })
        .WithName("FindPetsByTags")
        .WithSummary("Finds Pets by tags")
        .Produces<List<Pet>>(200)
        .ProducesProblem(400);

        // Get /pet/{petId} - Find pet by ID
        group.MapGet("/pet/{petId}", (long petId) =>
        {
        })
        .WithName("GetPetById")
        .WithSummary("Find pet by ID")
        .Produces<Pet>(200)
        .ProducesProblem(400);

        // Put /pet - Update an existing pet
        group.MapPut("/pet", async ([FromBody] Pet pet) =>
        {
        })
        .WithName("UpdatePet")
        .WithSummary("Update an existing pet")
        .Produces<Pet>(200)
        .ProducesProblem(400);

        return group;
    }
}
