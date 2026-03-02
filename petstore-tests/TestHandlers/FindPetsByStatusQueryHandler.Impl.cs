using PetstoreApi.DTOs;
using PetstoreApi.Queries;
using PetstoreApi.Services;

namespace PetstoreApi.Handlers;

public partial class FindPetsByStatusQueryHandler
{
    private readonly IPetStore _petStore;

    public FindPetsByStatusQueryHandler(IPetStore petStore)
    {
        _petStore = petStore;
    }

    private async partial Task<IEnumerable<PetDto>> ExecuteAsync(FindPetsByStatusQuery request, CancellationToken cancellationToken)
    {
        var pets = _petStore.FindByStatus(request.status);
        return await Task.FromResult(pets.Select(MapDomainToDto));
    }
}
