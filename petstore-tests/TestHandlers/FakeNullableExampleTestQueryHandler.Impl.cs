using PetstoreApi.DTOs;
using PetstoreApi.Queries;

namespace PetstoreApi.Handlers;

public partial class FakeNullableExampleTestQueryHandler
{
    private async partial Task<TestNullableDto> ExecuteAsync(FakeNullableExampleTestQuery request, CancellationToken cancellationToken)
    {
        await Task.CompletedTask;
        throw new NotImplementedException("FakeNullableExampleTest is not implemented in the petstore test stub.");
    }
}
