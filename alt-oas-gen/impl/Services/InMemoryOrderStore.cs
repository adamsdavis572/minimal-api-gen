using System.Collections.Concurrent;
using AlternativeApi.Models;

namespace AlternativeApi.Services;

public interface IOrderStore
{
    Order Place(Order order);
    Order? GetById(Guid id);
    Order? UpdateStatus(Guid id, OrderStatus status);
    IReadOnlyList<Order> GetAll();
}

public class InMemoryOrderStore : IOrderStore
{
    private readonly ConcurrentDictionary<Guid, Order> _orders = new();

    public Order Place(Order order)
    {
        if (order.Id == Guid.Empty)
            order.Id = Guid.NewGuid();
        _orders[order.Id] = order;
        return order;
    }

    public Order? GetById(Guid id) =>
        _orders.TryGetValue(id, out var order) ? order : null;

    public Order? UpdateStatus(Guid id, OrderStatus status)
    {
        if (!_orders.TryGetValue(id, out var order)) return null;
        order.Status = status;
        return order;
    }

    public IReadOnlyList<Order> GetAll() => _orders.Values.ToList();
}
