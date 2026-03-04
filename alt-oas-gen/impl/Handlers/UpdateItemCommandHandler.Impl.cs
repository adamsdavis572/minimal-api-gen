using AlternativeApi.Commands;
using AlternativeApi.DTOs;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class UpdateItemCommandHandler
{
    private readonly IItemStore _store;

    public UpdateItemCommandHandler(IItemStore store) => _store = store;

    private partial Task<ItemDto> ExecuteAsync(UpdateItemCommand request, CancellationToken cancellationToken)
    {
        var existing = _store.GetById(request.itemId);
        if (existing is null) return Task.FromResult<ItemDto>(null!);

        existing.ApplyUpdate(request.updateItemRequest);
        var updated = _store.Update(request.itemId, existing);
        return Task.FromResult(updated?.ToDto()!);
    }
}
