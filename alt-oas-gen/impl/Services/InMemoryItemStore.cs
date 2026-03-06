using System.Collections.Concurrent;
using AlternativeApi.Models;

namespace AlternativeApi.Services;

public interface IItemStore
{
    Item Add(Item item);
    Item? GetById(Guid id);
    Item? Update(Guid id, Item item);
    bool Delete(Guid id);
    Item? AttachImage(Guid id, ItemImage image);
    (IReadOnlyList<Item> Items, long TotalCount) List(ItemCategory? category, bool? inStock, int page, int pageSize);
    IReadOnlyList<Item> GetAll();
}

public class InMemoryItemStore : IItemStore
{
    private readonly ConcurrentDictionary<Guid, Item> _items = new();

    public Item Add(Item item)
    {
        if (item.Id == Guid.Empty)
            item.Id = Guid.NewGuid();
        _items[item.Id] = item;
        return item;
    }

    public Item? GetById(Guid id) =>
        _items.TryGetValue(id, out var item) ? item : null;

    public Item? Update(Guid id, Item item)
    {
        if (!_items.ContainsKey(id)) return null;
        item.Id = id;
        _items[id] = item;
        return item;
    }

    public bool Delete(Guid id) => _items.TryRemove(id, out _);

    public Item? AttachImage(Guid id, ItemImage image)
    {
        if (!_items.TryGetValue(id, out var item)) return null;
        item.Image = image;
        return item;
    }

    public (IReadOnlyList<Item> Items, long TotalCount) List(
        ItemCategory? category, bool? inStock, int page, int pageSize)
    {
        var query = _items.Values.AsEnumerable();

        if (category.HasValue)
            query = query.Where(i => i.Category == category.Value);

        if (inStock.HasValue)
            query = query.Where(i => inStock.Value ? i.StockQuantity > 0 : i.StockQuantity == 0);

        var all = query.OrderBy(i => i.CreatedAt).ToList();
        var paged = all.Skip((page - 1) * pageSize).Take(pageSize).ToList();

        return (paged, all.Count);
    }

    public IReadOnlyList<Item> GetAll() => _items.Values.ToList();
}
