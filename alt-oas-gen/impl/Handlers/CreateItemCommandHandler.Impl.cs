using AlternativeApi.Commands;
using AlternativeApi.DTOs;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class CreateItemCommandHandler
{
    private readonly IItemStore _store;

    public CreateItemCommandHandler(IItemStore store) => _store = store;

    private partial Task<ItemDto> ExecuteAsync(CreateItemCommand request, CancellationToken cancellationToken)
    {
        var item = request.createItemRequest.ToModel();
        var created = _store.Add(item);
        return Task.FromResult(created.ToDto());
    }
}
