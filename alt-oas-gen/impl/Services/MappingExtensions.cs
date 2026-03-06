using AlternativeApi.DTOs;
using AlternativeApi.Models;

namespace AlternativeApi.Services;

/// <summary>
/// Shared DTO ↔ Model mapping helpers used by handler impl files.
/// </summary>
internal static class MappingExtensions
{
    // ── Item ──────────────────────────────────────────────────────────────────

    internal static Item ToModel(this CreateItemDto dto) => new()
    {
        Sku = dto.Sku,
        Name = dto.Name,
        Description = dto.Description,
        Category = (ItemCategory)(int)dto.Category,
        Price = dto.Price != null ? new Money { Amount = dto.Price.Amount, Currency = dto.Price.Currency } : new Money(),
        StockQuantity = dto.StockQuantity ?? 0,
        Tags = dto.Tags?.ToList() ?? [],
        CreatedAt = DateTime.UtcNow,
    };

    internal static Item ApplyUpdate(this Item existing, UpdateItemDto dto)
    {
        existing.Name = dto.Name;
        existing.Description = dto.Description;
        existing.Category = (ItemCategory)(int)dto.Category;
        existing.Price = dto.Price != null ? new Money { Amount = dto.Price.Amount, Currency = dto.Price.Currency } : existing.Price;
        existing.StockQuantity = dto.StockQuantity ?? existing.StockQuantity;
        existing.Tags = dto.Tags?.ToList() ?? existing.Tags;
        existing.UpdatedAt = DateTime.UtcNow;
        return existing;
    }

    internal static ItemDto ToDto(this Item item) => new()
    {
        Id = item.Id,
        Sku = item.Sku,
        Name = item.Name,
        Description = item.Description,
        Category = (ItemCategoryDto)(int)item.Category,
        Price = item.Price != null ? new MoneyDto { Amount = item.Price.Amount, Currency = item.Price.Currency } : null!,
        StockQuantity = item.StockQuantity,
        Tags = item.Tags,
        Image = item.Image != null ? new ItemImageDto { Url = item.Image.Url, AltText = item.Image.AltText } : null,
        CreatedAt = item.CreatedAt,
        UpdatedAt = item.UpdatedAt == default ? null : item.UpdatedAt,
    };

    internal static ItemPageDto ToPageDto(
        this (IReadOnlyList<Item> Items, long TotalCount) result,
        int page, int pageSize) => new()
    {
        Items = result.Items.Select(ToDto).ToList(),
        TotalCount = result.TotalCount,
        Page = page,
        PageSize = pageSize,
        HasNextPage = result.TotalCount > (long)page * pageSize,
    };

    // ── Order ─────────────────────────────────────────────────────────────────

    internal static Order ToModel(this PlaceOrderDto dto) => new()
    {
        Lines = dto.Lines.Select(l => new OrderLine
        {
            ItemId = l.ItemId,
            Quantity = l.Quantity,
            UnitPrice = l.UnitPrice != null ? new Money { Amount = l.UnitPrice.Amount, Currency = l.UnitPrice.Currency } : new Money(),
        }).ToList(),
        ShippingAddress = dto.ShippingAddress != null ? new Address
        {
            Line1 = dto.ShippingAddress.Line1,
            Line2 = dto.ShippingAddress.Line2,
            City = dto.ShippingAddress.City,
            PostCode = dto.ShippingAddress.PostCode,
            Country = dto.ShippingAddress.Country,
        } : new Address(),
        Notes = dto.Notes,
        Status = OrderStatus.PendingEnum,
        Total = ComputeTotal(dto.Lines),
        PlacedAt = DateTime.UtcNow,
    };

    internal static OrderDto ToDto(this Order order) => new()
    {
        Id = order.Id,
        Status = (OrderStatusDto)(int)order.Status,
        Lines = order.Lines?.Select(l => new OrderLineDto
        {
            ItemId = l.ItemId,
            Quantity = l.Quantity,
            UnitPrice = l.UnitPrice != null ? new MoneyDto { Amount = l.UnitPrice.Amount, Currency = l.UnitPrice.Currency } : null!,
        }).ToList() ?? [],
        ShippingAddress = order.ShippingAddress != null ? new AddressDto
        {
            Line1 = order.ShippingAddress.Line1,
            Line2 = order.ShippingAddress.Line2,
            City = order.ShippingAddress.City,
            PostCode = order.ShippingAddress.PostCode,
            Country = order.ShippingAddress.Country,
        } : null!,
        Total = order.Total != null ? new MoneyDto { Amount = order.Total.Amount, Currency = order.Total.Currency } : null!,
        Notes = order.Notes,
        PlacedAt = order.PlacedAt,
        UpdatedAt = order.UpdatedAt == default ? null : order.UpdatedAt,
    };

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Money ComputeTotal(List<OrderLineDto> lines)
    {
        var currency = lines.FirstOrDefault()?.UnitPrice?.Currency ?? "USD";
        var amount = lines.Sum(l => (l.UnitPrice?.Amount ?? 0) * l.Quantity);
        return new Money { Amount = amount, Currency = currency };
    }
}
