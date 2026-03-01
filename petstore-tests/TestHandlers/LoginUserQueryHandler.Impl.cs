using PetstoreApi.Queries;

namespace PetstoreApi.Handlers;

public partial class LoginUserQueryHandler
{
    private async partial Task<string> ExecuteAsync(LoginUserQuery request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("LoginUser is not implemented in the petstore test stub.");
    }
}
