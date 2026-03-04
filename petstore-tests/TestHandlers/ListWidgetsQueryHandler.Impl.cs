using PetstoreApi.DTOs;
using PetstoreApi.Queries;

namespace PetstoreApi.Handlers;

public partial class ListWidgetsQueryHandler
{
    private async partial Task<WidgetCollectionDto> ExecuteAsync(ListWidgetsQuery request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        return new WidgetCollectionDto();
    }
}
