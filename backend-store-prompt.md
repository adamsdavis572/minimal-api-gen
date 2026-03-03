I am using the `minimal-api-gen` OpenAPI Generator plugin to generate an ASP.NET Core Minimal API 
server. The generator has been run against my OpenAPI spec and has produced code in `test-output/`.

I need you to implement the two test-stub files that wire up the in-memory service layer:

---

### 1. In-memory store: `src/<ProjectName>/Services/InMemory<ResourceName>Store.cs`

Create a thread-safe in-memory implementation of `I<ResourceName>Store` (defined in the same file).
Model it on this reference implementation for the Petstore API:

```csharp
// Reference: petstore-tests/TestHandlers/InMemoryPetStore.cs
public interface IPetStore
{
    Pet Add(Pet pet);
    Pet? GetById(long id);
    Pet? Update(Pet pet);
    bool Delete(long id);
    IEnumerable<Pet> FindByStatus(IEnumerable<string> statuses);
    IEnumerable<Pet> FindByTags(IEnumerable<string> tags);
}

public class InMemoryPetStore : IPetStore
{
    private readonly ConcurrentDictionary<long, Pet> _pets = new();
    private long _nextId = 1;

    public Pet Add(Pet pet)
    {
        if (pet.Id <= 0)
            pet.Id = Interlocked.Increment(ref _nextId);
        _pets[pet.Id] = pet;
        return pet;
    }
    // ... GetById, Update, Delete, FindByStatus, FindByTags
    // Status enum values are read via JsonPropertyNameAttribute reflection
}
```

Key rules:
- Use `ConcurrentDictionary<TKey, TEntity>` for thread safety
- Use `Interlocked.Increment` for ID generation, only when ID is not provided (≤ 0)
- Use `JsonPropertyNameAttribute` reflection to compare enum values (see `GetEnumJsonValue` helper)
- Namespace: `<ProjectName>.Services`
- Use model types from `<ProjectName>.Models` (these are generated — do not redefine them)

---

### 2. MediatR handler stubs: `src/<ProjectName>/Handlers/<HandlerFile>.cs`

The generator produces one handler partial class per operation, e.g.:

```csharp
// Generated (do not modify):
// test-output/src/<ProjectName>/Commands/Add<Resource>Command.cs
public partial class Add<Resource>CommandHandler : IRequestHandler<Add<Resource>Command, IResult>
{
    public partial Task<IResult> Handle(Add<Resource>Command request, CancellationToken ct);
}
```

Create the implementation file that completes each partial handler by:
- Accepting `I<ResourceName>Store` via constructor injection
- Delegating to the store methods
- Returning appropriate `Results.*` values (e.g. `Results.Ok(...)`, `Results.NotFound()`, 
  `Results.Created(...)`, `Results.NoContent()`)
- Using `TypedResults` where the return type is known at compile time

Example pattern:
```csharp
public partial class Add<Resource>CommandHandler
{
    private readonly I<Resource>Store _store;

    public Add<Resource>CommandHandler(I<Resource>Store store) => _store = store;

    public partial Task<IResult> Handle(Add<Resource>Command request, CancellationToken ct)
    {
        var result = _store.Add(request.Body);
        return Task.FromResult(Results.Created($"/<resources>/{result.Id}", result) as IResult);
    }
}
```

---

### 3. DI registration: `src/<ProjectName>/Extensions/ServiceCollectionExtensions.cs`

Register the store as a singleton so all handlers share the same in-memory state:

```csharp
public static class ServiceCollectionExtensions
{
    public static IServiceCollection Add<ProjectName>Services(this IServiceCollection services)
    {
        services.AddSingleton<I<Resource>Store, InMemory<Resource>Store>();
        return services;
    }
}
```

---

### Context you must know

- **Generated model types** are records in `<ProjectName>.Models` namespace — do NOT redefine them
- **Generated command/query types** are in `<ProjectName>.Commands` / `<ProjectName>.Queries`
- **Handler partials** are in `test-output/src/<ProjectName>/Handlers/` — check these files first 
  to see the exact method signatures you need to implement
- **Namespace root** is `<ProjectName>` throughout
- **Target framework**: .NET 8, C# 11+, use records, primary constructors, file-scoped namespaces
- **Do not modify** anything under `test-output/src/<ProjectName>/` directly — those are generated 
  files. Your implementations go in the stub files listed above.

---

### My API

- **Project name**: `<ProjectName>`
- **Primary resource(s)**: `<Resource1>`, `<Resource2>` (describe each briefly)
- **Operations needed**: (e.g. Create, GetById, List, Update, Delete, FindByStatus)
- **OpenAPI spec excerpt**: (paste relevant paths/schemas here)
- **Generated handler files to implement**: (paste or list the partial class signatures from 
  `test-output/src/<ProjectName>/Handlers/`)