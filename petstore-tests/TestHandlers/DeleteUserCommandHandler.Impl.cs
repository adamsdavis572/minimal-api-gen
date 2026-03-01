using PetstoreApi.Commands;

namespace PetstoreApi.Handlers;

public partial class DeleteUserCommandHandler
{
    private async partial Task<bool> ExecuteAsync(DeleteUserCommand request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("DeleteUser is not implemented in the petstore test stub.");
    }
}
