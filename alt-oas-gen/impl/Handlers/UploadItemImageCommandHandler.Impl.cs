using AlternativeApi.Commands;
using AlternativeApi.DTOs;
using AlternativeApi.Models;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class UploadItemImageCommandHandler
{
    private readonly IItemStore _store;

    public UploadItemImageCommandHandler(IItemStore store) => _store = store;

    private partial Task<ItemImageDto> ExecuteAsync(UploadItemImageCommand request, CancellationToken cancellationToken)
    {
        var image = new ItemImage
        {
            Url = $"/items/{request.itemId}/image",
            AltText = request.altText,
        };

        var item = _store.AttachImage(request.itemId, image);
        if (item is null) return Task.FromResult<ItemImageDto>(null!);

        return Task.FromResult(new ItemImageDto
        {
            Url = image.Url,
            AltText = image.AltText,
        });
    }
}
