using PetstoreApi.DTOs;
using PetstoreApi.Queries;

namespace PetstoreApi.Handlers;

public partial class GetUserByNameQueryHandler
{
    private async partial Task<UserDto> ExecuteAsync(GetUserByNameQuery request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("GetUserByName is not implemented in the petstore test stub.");
    }
}
