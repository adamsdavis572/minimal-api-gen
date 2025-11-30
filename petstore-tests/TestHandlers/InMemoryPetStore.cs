// In-memory Pet storage service for testing/demo purposes
// This provides a thread-safe singleton storage for Pet entities

using PetstoreApi.Models;
using System.Collections.Concurrent;

namespace PetstoreApi.Services;

public interface IPetStore
{
    Pet Add(Pet pet);
    Pet? GetById(long id);
    Pet? Update(Pet pet);
    bool Delete(long id);
    IEnumerable<Pet> FindByStatus(IEnumerable<string> statuses);
}

public class InMemoryPetStore : IPetStore
{
    private readonly ConcurrentDictionary<long, Pet> _pets = new();
    private long _nextId = 1;

    public Pet Add(Pet pet)
    {
        // Assign new ID
        pet.Id = Interlocked.Increment(ref _nextId);
        
        _pets[pet.Id] = pet;
        return pet;
    }

    public Pet? GetById(long id)
    {
        return _pets.TryGetValue(id, out var pet) ? pet : null;
    }

    public Pet? Update(Pet pet)
    {
        // Check if pet exists
        if (!_pets.ContainsKey(pet.Id))
        {
            return null;
        }

        _pets[pet.Id] = pet;
        return pet;
    }

    public bool Delete(long id)
    {
        return _pets.TryRemove(id, out _);
    }

    public IEnumerable<Pet> FindByStatus(IEnumerable<string> statuses)
    {
        var statusSet = new HashSet<string>(statuses, StringComparer.OrdinalIgnoreCase);
        
        return _pets.Values
            .Where(p => p.Status != null && statusSet.Contains(p.Status.ToString()))
            .ToList();
    }
}
