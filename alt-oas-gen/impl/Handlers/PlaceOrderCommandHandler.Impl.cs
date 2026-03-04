using AlternativeApi.Commands;
using AlternativeApi.DTOs;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class PlaceOrderCommandHandler
{
    private readonly IOrderStore _store;

    public PlaceOrderCommandHandler(IOrderStore store) => _store = store;

    private partial Task<OrderDto> ExecuteAsync(PlaceOrderCommand request, CancellationToken cancellationToken)
    {
        var order = request.placeOrderRequest.ToModel();
        var placed = _store.Place(order);
        return Task.FromResult(placed.ToDto());
    }
}
