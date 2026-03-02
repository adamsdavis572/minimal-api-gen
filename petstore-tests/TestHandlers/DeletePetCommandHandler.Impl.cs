using PetstoreApi.Commands;
using PetstoreApi.Services;

namespace PetstoreApi.Handlers;

public partial class DeletePetCommandHandler
{
    private readonly IPetStore _petStore;

    public DeletePetCommandHandler(IPetStore petStore)
    {
        _petStore = petStore;
    }

    private async partial Task<bool> ExecuteAsync(DeletePetCommand request, CancellationToken cancellationToken)
    {
        var existing = _petStore.GetById(request.petId);
        if (existing == null) return await Task.FromResult(false);
        _petStore.Delete(request.petId);
        return await Task.FromResult(true);
    }
}
