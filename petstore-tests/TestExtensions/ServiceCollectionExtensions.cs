using PetstoreApi.Services;

namespace PetstoreApi.Extensions;

/// <summary>
/// Extension methods for registering application services
/// </summary>
public static class ServiceCollectionExtensions
{
    /// <summary>
    /// Registers custom application services
    /// Add your service registrations here - this file is generated once and not overwritten
    /// </summary>
    public static IServiceCollection AddApplicationServices(this IServiceCollection services)
    {
        // Register IPetStore service as singleton
        services.AddSingleton<IPetStore, InMemoryPetStore>();
        
        return services;
    }
}
