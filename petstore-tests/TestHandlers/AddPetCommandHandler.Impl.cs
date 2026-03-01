using PetstoreApi.Commands;
using PetstoreApi.DTOs;
using PetstoreApi.Services;

namespace PetstoreApi.Handlers;

public partial class AddPetCommandHandler
{
    private readonly IPetStore _petStore;

    public AddPetCommandHandler(IPetStore petStore)
    {
        _petStore = petStore;
    }

    private async partial Task<PetDto> ExecuteAsync(AddPetCommand request, CancellationToken cancellationToken)
    {
        var domain = MapDtoToDomain(request.pet);
        var result = _petStore.Add(domain);
        return await Task.FromResult(MapDomainToDto(result));
    }
}
