using PetstoreApi.Configurators;
using PetstoreApi.Extensions;

namespace PetstoreApi.Configurators;

/// <summary>
/// Registers application-specific services (IPetStore implementation, etc.).
/// Kept in petstore-tests/ as it wires up test/implementation dependencies
/// that are not part of the generated API contract.
/// </summary>
public class ApplicationServiceConfigurator : IServiceConfigurator
{
    public void ConfigureServices(IServiceCollection services, IConfiguration configuration, IHostEnvironment environment)
    {
        services.AddApplicationServices();
    }
}
