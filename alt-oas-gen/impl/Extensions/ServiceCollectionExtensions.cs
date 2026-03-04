using AlternativeApi.Services;

namespace AlternativeApi.Extensions;

public static class ServiceCollectionExtensions
{
    public static IServiceCollection AddApplicationServices(this IServiceCollection services)
    {
        services.AddSingleton<IItemStore, InMemoryItemStore>();
        services.AddSingleton<IOrderStore, InMemoryOrderStore>();
        return services;
    }
}
