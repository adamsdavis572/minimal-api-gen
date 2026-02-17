using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using PetstoreApi.Filters;
using System.IdentityModel.Tokens.Jwt;

namespace PetstoreApi.Extensions;

/// <summary>
/// Extension methods for configuring API security (authentication and authorization)
/// </summary>
public static class SecurityExtensions
{
    /// <summary>
    /// Configures JWT authentication and permission-based authorization for the API.
    /// In development: Uses test secret for JWT validation.
    /// In production: Uses Auth:Authority and Auth:Audience from configuration.
    /// </summary>
    public static IServiceCollection AddApiSecurity(
        this IServiceCollection services, 
        IConfiguration configuration,
        IHostEnvironment environment)
    {
        // Clear default claim type mappings to preserve original JWT claim names
        JwtSecurityTokenHandler.DefaultInboundClaimTypeMap.Clear();

        // === AUTHENTICATION ===
        // Well-known test secret for JWT signing (must match bruno/generate-test-tokens.js)
        const string TestSecret = "this-is-a-test-secret-key-for-petstore-api-dev-only-min-32-bytes!";

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.MapInboundClaims = false; // Preserve original JWT claim names (e.g. "sub" not "http://schemas...")
                
                // For testing: Validate tokens signed with known test secret
                if (environment.IsDevelopment())
                {
                    options.RequireHttpsMetadata = false;
                    options.TokenValidationParameters = new TokenValidationParameters
                    {
                        ValidateIssuer = false,
                        ValidateAudience = false,
                        ValidateLifetime = false,
                        ValidateIssuerSigningKey = true,
                        IssuerSigningKey = new SymmetricSecurityKey(
                            System.Text.Encoding.UTF8.GetBytes(TestSecret))
                    };
                }
                else
                {
                    options.Authority = configuration["Auth:Authority"];
                    options.Audience = configuration["Auth:Audience"];
                }
            });

        // === AUTHORIZATION ===
        services.AddAuthorization(options =>
        {
            // Authorization policies for endpoint filter
            options.AddPolicy("ReadAccess", policy => 
                policy.RequireClaim("permission", "read"));
            options.AddPolicy("WriteAccess", policy => 
                policy.RequireClaim("permission", "write"));
        });

        // Register the permission filter
        services.AddSingleton<PermissionEndpointFilter>();

        return services;
    }
}
