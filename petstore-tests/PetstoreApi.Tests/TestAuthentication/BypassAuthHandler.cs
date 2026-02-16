using Microsoft.AspNetCore.Authorization;

namespace PetstoreApi.Tests.TestAuthentication;

/// <summary>
/// Authorization handler that bypasses all authorization requirements.
/// Used in Open Mode to allow all requests regardless of authorization policies.
/// </summary>
public class BypassAuthHandler : IAuthorizationHandler
{
    public Task HandleAsync(AuthorizationHandlerContext context)
    {
        // Succeed all pending requirements
        foreach (var requirement in context.PendingRequirements.ToList())
        {
            context.Succeed(requirement);
        }

        return Task.CompletedTask;
    }
}
