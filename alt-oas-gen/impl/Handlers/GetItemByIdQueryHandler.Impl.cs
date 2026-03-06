using AlternativeApi.DTOs;
using AlternativeApi.Queries;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class GetItemByIdQueryHandler
{
    private readonly IItemStore _store;

    public GetItemByIdQueryHandler(IItemStore store) => _store = store;

    private partial Task<ItemDto> ExecuteAsync(GetItemByIdQuery request, CancellationToken cancellationToken)
    {
        var item = _store.GetById(request.itemId);
        return Task.FromResult(item?.ToDto()!);
    }
}
