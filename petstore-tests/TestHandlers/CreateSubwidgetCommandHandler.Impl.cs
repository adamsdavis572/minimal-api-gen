using PetstoreApi.Commands;
using PetstoreApi.DTOs;

namespace PetstoreApi.Handlers;

public partial class CreateSubwidgetCommandHandler
{
    private async partial Task<WidgetDto> ExecuteAsync(CreateSubwidgetCommand request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        if (request.widget == null) return null;
        return MapDomainToDto(MapDtoToDomain(request.widget));
    }
}
