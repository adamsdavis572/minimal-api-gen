using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Routing;
using PetstoreApi.Endpoints;
using PetstoreApi.Filters;

namespace PetstoreApi.Extensions;

/// <summary>
/// Extension methods for registering API endpoints WITH authorization filtering
/// </summary>
public static class AuthorizedEndpointExtensions
{
    /// <summary>
    /// Registers all API endpoints with authorization filter applied.
    /// Delegates to generated endpoint registration methods.
    /// </summary>
    /// <param name="endpoints">The endpoint route builder</param>
    /// <returns>The endpoint route builder for chaining</returns>
    public static IEndpointRouteBuilder AddAuthorizedApiEndpoints(this IEndpointRouteBuilder endpoints)
    {
        // Create route group with authorization filter
        var group = endpoints.MapGroup("/v2")
            .AddEndpointFilter<PermissionEndpointFilter>();
        
        // Delegate to generated endpoint registration methods
        PetApiEndpoints.MapPetApiEndpoints(group);
        StoreApiEndpoints.MapStoreApiEndpoints(group);
        UserApiEndpoints.MapUserApiEndpoints(group);
        
        return endpoints;
    }
}
