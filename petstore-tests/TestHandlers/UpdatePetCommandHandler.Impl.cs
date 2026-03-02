using PetstoreApi.Commands;
using PetstoreApi.DTOs;
using PetstoreApi.Services;

namespace PetstoreApi.Handlers;

public partial class UpdatePetCommandHandler
{
    private readonly IPetStore _petStore;

    public UpdatePetCommandHandler(IPetStore petStore)
    {
        _petStore = petStore;
    }

    private async partial Task<PetDto> ExecuteAsync(UpdatePetCommand request, CancellationToken cancellationToken)
    {
        var domain = MapDtoToDomain(request.pet);
        var result = _petStore.Update(domain);
        if (result == null) return null;
        return await Task.FromResult(MapDomainToDto(result));
    }
}
