using PetstoreApi.DTOs;
using PetstoreApi.Queries;
using PetstoreApi.Services;

namespace PetstoreApi.Handlers;

public partial class FindPetsByTagsQueryHandler
{
    private readonly IPetStore _petStore;

    public FindPetsByTagsQueryHandler(IPetStore petStore)
    {
        _petStore = petStore;
    }

    private async partial Task<IEnumerable<PetDto>> ExecuteAsync(FindPetsByTagsQuery request, CancellationToken cancellationToken)
    {
        var pets = _petStore.FindByTags(request.tags);
        return await Task.FromResult(pets.Select(MapDomainToDto));
    }
}
