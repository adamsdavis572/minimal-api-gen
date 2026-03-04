using AlternativeApi.Commands;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class DeleteItemCommandHandler
{
    private readonly IItemStore _store;

    public DeleteItemCommandHandler(IItemStore store) => _store = store;

    private partial Task<bool> ExecuteAsync(DeleteItemCommand request, CancellationToken cancellationToken) =>
        Task.FromResult(_store.Delete(request.itemId));
}
