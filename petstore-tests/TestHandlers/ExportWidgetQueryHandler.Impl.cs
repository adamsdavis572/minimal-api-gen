using PetstoreApi.DTOs;
using PetstoreApi.Queries;

namespace PetstoreApi.Handlers;

public partial class ExportWidgetQueryHandler
{
    private async partial Task<FileDto> ExecuteAsync(ExportWidgetQuery request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        return new FileDto(Array.Empty<byte>(), "application/octet-stream", $"{request.widgetId}.bin");
    }
}
