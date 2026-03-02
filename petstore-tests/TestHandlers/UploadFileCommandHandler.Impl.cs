using PetstoreApi.Commands;
using PetstoreApi.DTOs;

namespace PetstoreApi.Handlers;

public partial class UploadFileCommandHandler
{
    private async partial Task<ApiResponseDto> ExecuteAsync(UploadFileCommand request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("UploadFile is not implemented in the petstore test stub.");
    }
}
