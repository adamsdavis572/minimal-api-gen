using AlternativeApi.DTOs;
using AlternativeApi.Models;
using AlternativeApi.Queries;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class ListItemsQueryHandler
{
    private readonly IItemStore _store;

    public ListItemsQueryHandler(IItemStore store) => _store = store;

    private partial Task<ItemPageDto> ExecuteAsync(ListItemsQuery request, CancellationToken cancellationToken)
    {
        var page = request.page ?? 1;
        var pageSize = request.pageSize ?? 20;
        ItemCategory? category = request.category.HasValue
            ? (ItemCategory)(int)request.category.Value
            : null;

        var result = _store.List(category, request.inStock, page, pageSize);
        return Task.FromResult(result.ToPageDto(page, pageSize));
    }
}
