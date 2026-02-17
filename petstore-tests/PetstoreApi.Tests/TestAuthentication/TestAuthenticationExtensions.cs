using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authorization;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

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
        
        // Note: No need to call AddAuthorization() - policies already registered by Program.cs
        // Note: No authentication registration needed - BypassAuthHandler works at authorization level
        
        return services;
    }

    /// <summary>
    /// Configures authentication and authorization for Secure Mode (header-based authentication).
    /// Removes production authentication (JwtBearer) and replaces with MockAuthHandler.
    /// Authentication reads user claims from HTTP headers:
    /// - X-Test-UserId: User identifier for NameIdentifier claim
    /// - X-Test-Role: User role (defaults to "User" if not present)
    /// - X-Test-Permission: Comma-separated permissions (read, write)
    /// </summary>
    public static IServiceCollection AddSecureModeAuth(this IServiceCollection services)
    {
        // Remove existing authentication services to avoid conflicts with JwtBearer from Program.cs
        services.RemoveAll<IAuthenticationService>();
        services.RemoveAll<IAuthenticationSchemeProvider>();
        services.RemoveAll<IAuthenticationHandlerProvider>();
        
        // Add ONLY test authentication with MockAuthHandler
        services.AddAuthentication(MockAuthHandler.AuthenticationScheme)
            .AddScheme<AuthenticationSchemeOptions, MockAuthHandler>(
                MockAuthHandler.AuthenticationScheme,
                options => { });
        
        // Note: No need to call AddAuthorization() - policies already registered by Program.cs
        // The ReadAccess and WriteAccess policies will work with MockAuthHandler's permission claims
        
        return services;
    }
}
