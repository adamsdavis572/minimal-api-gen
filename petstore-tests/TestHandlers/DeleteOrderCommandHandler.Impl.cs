using PetstoreApi.Commands;

namespace PetstoreApi.Handlers;

public partial class DeleteOrderCommandHandler
{
    private async partial Task<bool> ExecuteAsync(DeleteOrderCommand request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("DeleteOrder is not implemented in the petstore test stub.");
    }
}
