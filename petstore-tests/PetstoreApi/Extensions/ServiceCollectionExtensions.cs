using PetstoreApi.Services;

namespace PetstoreApi.Extensions;

/// <summary>
/// Extension methods for registering application services
/// </summary>
public static class ServiceCollectionExtensions
{
    /// <summary>
    /// Registers custom application services
    /// </summary>
    public static IServiceCollection AddApplicationServices(this IServiceCollection services)
    {
        // Register IPetStore implementation
        services.AddSingleton<IPetStore, InMemoryPetStore>();

        return services;
    }
}
