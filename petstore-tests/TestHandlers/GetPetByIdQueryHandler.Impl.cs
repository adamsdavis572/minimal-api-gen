using PetstoreApi.DTOs;
using PetstoreApi.Queries;
using PetstoreApi.Services;

namespace PetstoreApi.Handlers;

public partial class GetPetByIdQueryHandler
{
    private readonly IPetStore _petStore;

    public GetPetByIdQueryHandler(IPetStore petStore)
    {
        _petStore = petStore;
    }

    private async partial Task<PetDto> ExecuteAsync(GetPetByIdQuery request, CancellationToken cancellationToken)
    {
        var pet = _petStore.GetById(request.petId);
        if (pet == null) return null;
        return await Task.FromResult(MapDomainToDto(pet));
    }
}
