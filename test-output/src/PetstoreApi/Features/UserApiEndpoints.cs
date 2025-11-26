#pragma warning disable ASP0020 // Complex types as query parameters
using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using PetstoreApi.Models;
using MediatR;
using PetstoreApi.Commands;
using PetstoreApi.Queries;

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
        group.MapPost("/user", async (IMediator mediator, [FromBody] User user) =>
        {
            // MediatR delegation
            var command = new CreateUserCommand
            {
                user = user
            };
            var result = await mediator.Send(command);
            return Results.NoContent();
        })
        .WithName("CreateUser")
        .WithSummary("Create user")
        .ProducesProblem(400);

        // Post /user/createWithArray - Creates list of users with given input array
        group.MapPost("/user/createWithArray", async (IMediator mediator, [FromBody] List<User> user) =>
        {
            // MediatR delegation
            var command = new CreateUsersWithArrayInputCommand
            {
                user = user
            };
            var result = await mediator.Send(command);
            return Results.NoContent();
        })
        .WithName("CreateUsersWithArrayInput")
        .WithSummary("Creates list of users with given input array")
        .ProducesProblem(400);

        // Post /user/createWithList - Creates list of users with given input array
        group.MapPost("/user/createWithList", async (IMediator mediator, [FromBody] List<User> user) =>
        {
            // MediatR delegation
            var command = new CreateUsersWithListInputCommand
            {
                user = user
            };
            var result = await mediator.Send(command);
            return Results.NoContent();
        })
        .WithName("CreateUsersWithListInput")
        .WithSummary("Creates list of users with given input array")
        .ProducesProblem(400);

        // Delete /user/{username} - Delete user
        group.MapDelete("/user/{username}", async (IMediator mediator, string username) =>
        {
            // MediatR delegation
            var command = new DeleteUserCommand
            {
                username = username
            };
            var result = await mediator.Send(command);
            // DELETE with bool return - check if resource was found
            return result ? Results.NoContent() : Results.NotFound();
        })
        .WithName("DeleteUser")
        .WithSummary("Delete user")
        .Produces<bool>(200)
        .ProducesProblem(400);

        // Get /user/{username} - Get user by user name
        group.MapGet("/user/{username}", async (IMediator mediator, string username) =>
        {
            // MediatR delegation
            var query = new GetUserByNameQuery
            {
                username = username
            };
            var result = await mediator.Send(query);
            if (result == null) return Results.NotFound();
            return Results.Ok(result);
        })
        .WithName("GetUserByName")
        .WithSummary("Get user by user name")
        .Produces<User>(200)
        .ProducesProblem(400);

        // Get /user/login - Logs user into the system
        group.MapGet("/user/login", async (IMediator mediator, [FromQuery] string username, [FromQuery] string password) =>
        {
            // MediatR delegation
            var query = new LoginUserQuery
            {
                username = username,
                password = password
            };
            var result = await mediator.Send(query);
            if (result == null) return Results.NotFound();
            return Results.Ok(result);
        })
        .WithName("LoginUser")
        .WithSummary("Logs user into the system")
        .Produces<string>(200)
        .ProducesProblem(400);

        // Get /user/logout - Logs out current logged in user session
        group.MapGet("/user/logout", async (IMediator mediator) =>
        {
            // MediatR delegation
            var query = new LogoutUserQuery
            {
            };
            var result = await mediator.Send(query);
            return Results.Ok();
        })
        .WithName("LogoutUser")
        .WithSummary("Logs out current logged in user session")
        .ProducesProblem(400);

        // Put /user/{username} - Updated user
        group.MapPut("/user/{username}", async (IMediator mediator, string username, [FromBody] User user) =>
        {
            // MediatR delegation
            var command = new UpdateUserCommand
            {
                username = username,
                user = user
            };
            var result = await mediator.Send(command);
            return Results.NoContent();
        })
        .WithName("UpdateUser")
        .WithSummary("Updated user")
        .ProducesProblem(400);

        return group;
    }
}
