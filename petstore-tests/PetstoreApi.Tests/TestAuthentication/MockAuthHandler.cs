using System.Security.Claims;
using System.Text.Encodings.Web;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace PetstoreApi.Tests.TestAuthentication;

/// <summary>
/// Mock authentication handler for testing secure mode.
/// Reads user claims from HTTP headers (X-Test-UserId, X-Test-Role, X-Test-Permission)
/// and builds a ClaimsPrincipal for authentication.
/// </summary>
public class MockAuthHandler : AuthenticationHandler<AuthenticationSchemeOptions>
{
    public const string AuthenticationScheme = "TestScheme";
    public const string UserIdHeader = "X-Test-UserId";
    public const string RoleHeader = "X-Test-Role";
    public const string PermissionHeader = "X-Test-Permission";
    public const string DefaultRole = "User";

    public MockAuthHandler(
        IOptionsMonitor<AuthenticationSchemeOptions> options,
        ILoggerFactory logger,
        UrlEncoder encoder)
        : base(options, logger, encoder)
    {
    }

    protected override Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        // Extract user ID from header
        if (!Request.Headers.TryGetValue(UserIdHeader, out var userIdValues))
        {
            // No user ID header - authentication fails
            return Task.FromResult(AuthenticateResult.Fail($"Missing {UserIdHeader} header"));
        }

        var userId = userIdValues.ToString();
        if (string.IsNullOrEmpty(userId))
        {
            return Task.FromResult(AuthenticateResult.Fail($"{UserIdHeader} header is empty"));
        }

        // Extract role from header (default to "User" if not present)
        var role = DefaultRole;
        if (Request.Headers.TryGetValue(RoleHeader, out var roleValues))
        {
            var roleValue = roleValues.ToString();
            if (!string.IsNullOrEmpty(roleValue))
            {
                role = roleValue;
            }
        }

        // Build claims
        var claims = new List<Claim>
        {
            new Claim(ClaimTypes.NameIdentifier, userId),
            new Claim(ClaimTypes.Name, userId),
            new Claim(ClaimTypes.Role, role)
        };

        // Extract permission claims from header (comma-separated: "read,write")
        if (Request.Headers.TryGetValue(PermissionHeader, out var permissionValues))
        {
            var permissions = permissionValues.ToString().Split(',', StringSplitOptions.RemoveEmptyEntries);
            foreach (var permission in permissions)
            {
                claims.Add(new Claim("permission", permission.Trim().ToLowerInvariant()));
            }
        }

        var identity = new ClaimsIdentity(claims, AuthenticationScheme);
        var principal = new ClaimsPrincipal(identity);
        var ticket = new AuthenticationTicket(principal, AuthenticationScheme);

        return Task.FromResult(AuthenticateResult.Success(ticket));
    }
}
