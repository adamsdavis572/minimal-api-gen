using Microsoft.AspNetCore.Authorization;

namespace PetstoreApi.Filters;

/// <summary>
/// Endpoint filter that enforces authorization based on endpoint name to policy mapping
/// </summary>
public class PermissionEndpointFilter : IEndpointFilter
{
    private readonly IAuthorizationService _authorizationService;
    
    // Static mapping of endpoint names to policy names
    private static readonly Dictionary<string, string> EndpointPolicies = new()
    {
        // Pet endpoints
        { "AddPet", "WriteAccess" },
        { "UpdatePet", "WriteAccess" },
        { "DeletePet", "WriteAccess" },
        { "GetPetById", "ReadAccess" },
        { "FindPetsByStatus", "ReadAccess" },
        { "FindPetsByTags", "ReadAccess" },
        
        // Store endpoints
        { "PlaceOrder", "WriteAccess" },
        { "DeleteOrder", "WriteAccess" },
        { "GetOrderById", "ReadAccess" },
        { "GetInventory", "ReadAccess" },
        
        // User endpoints
        { "CreateUser", "WriteAccess" },
        { "UpdateUser", "WriteAccess" },
        { "DeleteUser", "WriteAccess" },
        { "GetUserByName", "ReadAccess" },
        { "LoginUser", "ReadAccess" },
        { "LogoutUser", "ReadAccess" }
    };
    
    public PermissionEndpointFilter(IAuthorizationService authorizationService)
    {
        _authorizationService = authorizationService ?? 
            throw new ArgumentNullException(nameof(authorizationService));
    }
    
    public async ValueTask<object?> InvokeAsync(
        EndpointFilterInvocationContext context, 
        EndpointFilterDelegate next)
    {
        var httpContext = context.HttpContext;
        
        // Retrieve endpoint name from metadata
        var endpointName = httpContext.GetEndpoint()?
            .Metadata
            .GetMetadata<EndpointNameMetadata>()?
            .EndpointName;
        
        // If no endpoint name or no policy mapping, allow request
        if (string.IsNullOrEmpty(endpointName) || 
            !EndpointPolicies.TryGetValue(endpointName, out var policyName))
        {
            return await next(context);
        }
        
        // Authorize based on policy
        var authorizationResult = await _authorizationService.AuthorizeAsync(
            httpContext.User, 
            resource: null, 
            policyName);
        
        // Return 403 Forbidden if authorization fails
        if (!authorizationResult.Succeeded)
        {
            return Results.Forbid();
        }
        
        // Authorization succeeded, continue to endpoint handler
        return await next(context);
    }
}
