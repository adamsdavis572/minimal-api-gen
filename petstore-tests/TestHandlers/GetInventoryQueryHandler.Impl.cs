using PetstoreApi.Queries;

namespace PetstoreApi.Handlers;

public partial class GetInventoryQueryHandler
{
    private async partial Task<Dictionary<string, int>> ExecuteAsync(GetInventoryQuery request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("GetInventory is not implemented in the petstore test stub.");
    }
}
