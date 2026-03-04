using AlternativeApi.Commands;
using AlternativeApi.DTOs;
using AlternativeApi.Models;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class UpdateOrderStatusCommandHandler
{
    private readonly IOrderStore _store;

    public UpdateOrderStatusCommandHandler(IOrderStore store) => _store = store;

    private partial Task<OrderDto> ExecuteAsync(UpdateOrderStatusCommand request, CancellationToken cancellationToken)
    {
        var status = (OrderStatus)(int)request.updateOrderStatusRequest.Status;
        var order = _store.UpdateStatus(request.orderId, status);
        return Task.FromResult(order?.ToDto()!);
    }
}
