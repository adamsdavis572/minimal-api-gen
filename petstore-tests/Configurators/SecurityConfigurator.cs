using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using PetstoreApi.Configurators;
using PetstoreApi.Filters;
using System.IdentityModel.Tokens.Jwt;

namespace PetstoreApi.Configurators;

/// <summary>
/// Configures JWT authentication, authorization policies, and the permission endpoint filter.
/// Implements both IServiceConfigurator and IApplicationConfigurator so a single class
/// owns all auth concerns — service registration and middleware pipeline.
/// Kept in petstore-tests/ as auth setup is environment/test-specific.
/// </summary>
public class SecurityConfigurator : IServiceConfigurator, IApplicationConfigurator
{
    /// <summary>
    /// Auth middleware must run before endpoints (default Order=0) but after exception handling.
    /// </summary>
    public int Order => 10;

    public void ConfigureServices(IServiceCollection services, IConfiguration configuration, IHostEnvironment environment)
    {
        // Clear default claim type mappings to preserve original JWT claim names
        JwtSecurityTokenHandler.DefaultInboundClaimTypeMap.Clear();

        // Well-known test secret for JWT signing (must match bruno/generate-test-tokens.js)
        const string TestSecret = "this-is-a-test-secret-key-for-petstore-api-dev-only-min-32-bytes!";

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.MapInboundClaims = false;

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

        services.AddAuthorization(options =>
        {
            options.AddPolicy("ReadAccess", policy =>
                policy.RequireClaim("permission", "read"));
            options.AddPolicy("WriteAccess", policy =>
                policy.RequireClaim("permission", "write"));
        });

        // Register as IEndpointFilter so AddApiEndpoints() picks it up automatically from DI
        services.AddSingleton<IEndpointFilter, PermissionEndpointFilter>();
        services.AddSingleton<PermissionEndpointFilter>();
    }

    public void Configure(WebApplication app, IHostEnvironment environment)
    {
        app.UseAuthentication();
        app.UseAuthorization();
    }
}
