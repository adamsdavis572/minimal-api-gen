using PetstoreApi.DTOs;
using PetstoreApi.Queries;

namespace PetstoreApi.Handlers;

public partial class GetOrderByIdQueryHandler
{
    private async partial Task<OrderDto> ExecuteAsync(GetOrderByIdQuery request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("GetOrderById is not implemented in the petstore test stub.");
    }
}
