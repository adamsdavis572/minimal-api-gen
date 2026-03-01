using PetstoreApi.Commands;
using PetstoreApi.DTOs;

namespace PetstoreApi.Handlers;

public partial class PlaceOrderCommandHandler
{
    private async partial Task<OrderDto> ExecuteAsync(PlaceOrderCommand request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("PlaceOrder is not implemented in the petstore test stub.");
    }
}
