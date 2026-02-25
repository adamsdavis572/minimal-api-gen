using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using PetstoreApi.Services;
using PetstoreApi.Tests.TestAuthentication;

namespace PetstoreApi.Tests;

/// <summary>
/// Test mode for integration tests.
/// </summary>
public enum TestMode
{
    /// <summary>
    /// Open mode - authorization is bypassed for all requests.
    /// </summary>
    Open,
    
    /// <summary>
    /// Secure mode - authentication is enforced via header-based MockAuthHandler.
    /// </summary>
    Secure
}

public class CustomWebApplicationFactory : WebApplicationFactory<Program>
{
    /// <summary>
    /// Gets or sets the test mode for authentication/authorization.
    /// Default is Open mode.
    /// </summary>
    public TestMode Mode { get; set; } = TestMode.Open;

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            // Replace IPetStore registered by ApplicationServiceConfigurator with the
            // in-memory test implementation. Using Replace() avoids double-registration.
            services.Replace(ServiceDescriptor.Singleton<IPetStore, InMemoryPetStore>());
            
            // Configure authentication/authorization based on test mode
            switch (Mode)
            {
                case TestMode.Open:
                    services.AddOpenModeAuth();
                    break;
                    
                case TestMode.Secure:
                    services.AddSecureModeAuth();
                    break;
            }
        });
    }
}
