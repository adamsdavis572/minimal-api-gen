using AlternativeApi.DTOs;
using AlternativeApi.Models;
using AlternativeApi.Queries;
using AlternativeApi.Services;

namespace AlternativeApi.Handlers;

public partial class GetSummaryReportQueryHandler
{
    private readonly IItemStore _itemStore;
    private readonly IOrderStore _orderStore;

    public GetSummaryReportQueryHandler(IItemStore itemStore, IOrderStore orderStore)
    {
        _itemStore = itemStore;
        _orderStore = orderStore;
    }

    private partial Task<SummaryReportDto> ExecuteAsync(GetSummaryReportQuery request, CancellationToken cancellationToken)
    {
        var orders = _orderStore.GetAll().AsEnumerable();

        if (request.from.HasValue)
            orders = orders.Where(o => o.PlacedAt >= request.from.Value);
        if (request.to.HasValue)
            orders = orders.Where(o => o.PlacedAt <= request.to.Value);

        var matchedOrders = orders.ToList();

        var totalRevenueCurrency = matchedOrders
            .SelectMany(o => o.Lines ?? [])
            .Select(l => l.UnitPrice?.Currency)
            .FirstOrDefault(c => c != null) ?? "USD";

        var totalRevenueAmount = matchedOrders
            .SelectMany(o => o.Lines ?? [])
            .Sum(l => (l.UnitPrice?.Amount ?? 0) * l.Quantity);

        var avgOrderValue = matchedOrders.Count > 0
            ? totalRevenueAmount / matchedOrders.Count
            : (double?)null;

        // Build category breakdown by looking up each ordered item's category
        var categoryBreakdown = matchedOrders
            .SelectMany(o => o.Lines ?? [])
            .GroupBy(l =>
            {
                var item = _itemStore.GetById(l.ItemId);
                return item?.Category ?? ItemCategory.OtherEnum;
            })
            .Select(g => new CategoryBreakdownDto
            {
                Category = (ItemCategoryDto)(int)g.Key,
                ItemCount = g.Count(),
                TotalRevenue = new MoneyDto
                {
                    Amount = g.Sum(l => (l.UnitPrice?.Amount ?? 0) * l.Quantity),
                    Currency = totalRevenueCurrency,
                },
            })
            .ToList();

        var report = new SummaryReportDto
        {
            TotalOrders = matchedOrders.Count,
            TotalRevenue = new MoneyDto { Amount = totalRevenueAmount, Currency = totalRevenueCurrency },
            AverageOrderValue = avgOrderValue.HasValue
                ? new MoneyDto { Amount = avgOrderValue.Value, Currency = totalRevenueCurrency }
                : null,
            CategoryBreakdown = categoryBreakdown,
            GeneratedAt = DateTime.UtcNow,
        };

        return Task.FromResult(report);
    }
}
