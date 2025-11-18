#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;

namespace PetstoreApi.Endpoints;

/// <summary>
/// Minimal API endpoints for UserApi operations
/// </summary>
public static class UserApiEndpoints
{
    

    /// <summary>
    /// Maps all UserApi endpoints to the route group
    /// </summary>
    public static RouteGroupBuilder MapUserApiEndpoints(this RouteGroupBuilder group)
    {
        // Post /user - Create user
        group.MapPost("/user", async ([FromBody] User user) =>
        {
            
            // TODO: Implement CreateUser logic
            return Results.NoContent();
            
        })
        .WithName("CreateUser")
        .WithSummary("Create user")
        .ProducesProblem(400);

        // Post /user/createWithArray - Creates list of users with given input array
        group.MapPost("/user/createWithArray", async ([FromBody] List<User> user) =>
        {
            
            // TODO: Implement CreateUsersWithArrayInput logic
            return Results.NoContent();
            
        })
        .WithName("CreateUsersWithArrayInput")
        .WithSummary("Creates list of users with given input array")
        .ProducesProblem(400);

        // Post /user/createWithList - Creates list of users with given input array
        group.MapPost("/user/createWithList", async ([FromBody] List<User> user) =>
        {
            
            // TODO: Implement CreateUsersWithListInput logic
            return Results.NoContent();
            
        })
        .WithName("CreateUsersWithListInput")
        .WithSummary("Creates list of users with given input array")
        .ProducesProblem(400);

        // Delete /user/{username} - Delete user
        group.MapDelete("/user/{username}", (string username) =>
        {
            
            // TODO: Implement DeleteUser logic
            return Results.NoContent();
            
        })
        .WithName("DeleteUser")
        .WithSummary("Delete user")
        .ProducesProblem(400);

        // Get /user/{username} - Get user by user name
        group.MapGet("/user/{username}", (string username) =>
        {
            
            // TODO: Implement GetUserByName logic
            User result = default;
            return Results.Ok(result);
            
        })
        .WithName("GetUserByName")
        .WithSummary("Get user by user name")
        .Produces<User>(200)
        .ProducesProblem(400);

        // Get /user/login - Logs user into the system
        group.MapGet("/user/login", ([FromQuery] string username, [FromQuery] string password) =>
        {
            
            // TODO: Implement LoginUser logic
            string result = default;
            return Results.Ok(result);
            
        })
        .WithName("LoginUser")
        .WithSummary("Logs user into the system")
        .Produces<string>(200)
        .ProducesProblem(400);

        // Get /user/logout - Logs out current logged in user session
        group.MapGet("/user/logout", () =>
        {
            
            // TODO: Implement LogoutUser logic
            return Results.NoContent();
            
        })
        .WithName("LogoutUser")
        .WithSummary("Logs out current logged in user session")
        .ProducesProblem(400);

        // Put /user/{username} - Updated user
        group.MapPut("/user/{username}", async ([FromBody] User user) =>
        {
            
            // TODO: Implement UpdateUser logic
            return Results.NoContent();
            
        })
        .WithName("UpdateUser")
        .WithSummary("Updated user")
        .ProducesProblem(400);

        return group;
    }
}
