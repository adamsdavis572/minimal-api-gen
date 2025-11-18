using PetstoreApi.Endpoints;

namespace PetstoreApi.Extensions;

/// <summary>
/// Extension methods for registering all API endpoints
/// </summary>
public static class EndpointMapper
{
    /// <summary>
    /// Maps all API endpoint groups to the application
    /// </summary>
    public static IEndpointRouteBuilder MapAllEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/v2");
        group.MapDefaultApiEndpoints();
        group.MapFakeApiEndpoints();
        group.MapPetApiEndpoints();
        group.MapStoreApiEndpoints();
        group.MapUserApiEndpoints();

        return app;
    }
}
