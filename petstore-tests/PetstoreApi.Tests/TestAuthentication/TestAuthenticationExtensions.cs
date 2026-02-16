using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authorization;
using Microsoft.Extensions.DependencyInjection;

namespace PetstoreApi.Tests.TestAuthentication;

/// <summary>
/// Extension methods for configuring test authentication and authorization modes.
/// </summary>
public static class TestAuthenticationExtensions
{
    /// <summary>
    /// Configures authentication and authorization for Open Mode (authorization bypassed).
    /// All authorization requirements are automatically satisfied.
    /// </summary>
    public static IServiceCollection AddOpenModeAuth(this IServiceCollection services)
    {
        // Register bypass authorization handler to satisfy all requirements
        services.AddSingleton<IAuthorizationHandler, BypassAuthHandler>();
        
        // Add authorization services (required for authorization middleware)
        services.AddAuthorization();
        
        return services;
    }

    /// <summary>
    /// Configures authentication and authorization for Secure Mode (header-based authentication).
    /// Authentication reads user claims from HTTP headers:
    /// - X-Test-UserId: User identifier for NameIdentifier claim
    /// - X-Test-Role: User role (defaults to "User" if not present)
    /// </summary>
    public static IServiceCollection AddSecureModeAuth(this IServiceCollection services)
    {
        // Add authentication with custom TestScheme
        services.AddAuthentication(MockAuthHandler.AuthenticationScheme)
            .AddScheme<AuthenticationSchemeOptions, MockAuthHandler>(
                MockAuthHandler.AuthenticationScheme,
                options => { });
        
        // Add authorization services
        services.AddAuthorization();
        
        return services;
    }
}
