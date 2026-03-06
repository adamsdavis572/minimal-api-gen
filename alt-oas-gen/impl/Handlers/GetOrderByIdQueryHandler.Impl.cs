using AlternativeApi.DTOs;
using AlternativeApi.Queries;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class GetOrderByIdQueryHandler
{
    private readonly IOrderStore _store;

    public GetOrderByIdQueryHandler(IOrderStore store) => _store = store;

    private partial Task<OrderDto> ExecuteAsync(GetOrderByIdQuery request, CancellationToken cancellationToken)
    {
        var order = _store.GetById(request.orderId);
        return Task.FromResult(order?.ToDto()!);
    }
}
