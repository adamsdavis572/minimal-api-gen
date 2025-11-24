# Quickstart: Using MediatR with Generated Code

**Feature**: 006-mediatr-decoupling  
**Audience**: Developers using the Minimal API Generator  
**Purpose**: Quick examples of how to use the generated MediatR code

## Setup

### Option 1: Generate with MediatR (Production)

```bash
java -cp "generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar:~/.m2/repository/org/openapitools/openapi-generator-cli/7.17.0/openapi-generator-cli-7.17.0.jar" \
  org.openapitools.codegen.OpenAPIGenerator generate \
  -g aspnetcore-minimalapi \
  -i specs/petstore.yaml \
  -o output \
  --additional-properties useMediatr=true
```

### Option 2: Generate without MediatR (Quick Prototypes)

```bash
# Default behavior - useMediatr=false
java -cp "..." org.openapitools.codegen.OpenAPIGenerator generate \
  -g aspnetcore-minimalapi \
  -i specs/petstore.yaml \
  -o output
```

---

## Generated Structure (useMediatr=true)

```
output/
├── Commands/
│   ├── AddPetCommand.cs          # Command classes for POST/PUT/DELETE
│   └── UpdatePetCommand.cs
├── Queries/
│   ├── GetPetByIdQuery.cs        # Query classes for GET
│   └── GetAllPetsQuery.cs
├── Handlers/
│   ├── AddPetCommandHandler.cs   # Business logic goes here (USER EDITS)
│   └── GetPetByIdQueryHandler.cs
├── Features/
│   └── PetApiEndpoints.cs        # Clean endpoints (NO business logic)
├── Models/
│   └── Pet.cs                     # DTOs (unchanged from current generator)
└── Program.cs                     # MediatR auto-registered
```

---

## Example 1: Implementing a Command Handler

**Generated Scaffold** (`Handlers/AddPetCommandHandler.cs`):
```csharp
public class AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>
{
    // TODO: Add dependencies via constructor injection
    
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        // TODO: Implement addPet logic
        throw new NotImplementedException("Handler for addPet not yet implemented");
    }
}
```

**Your Implementation** (after first generation):
```csharp
public class AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<AddPetCommandHandler> _logger;
    
    public AddPetCommandHandler(
        ApplicationDbContext db,
        ILogger<AddPetCommandHandler> logger)
    {
        _db = db;
        _logger = logger;
    }
    
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("Creating new pet: {Name}", request.Name);
        
        var pet = new Pet
        {
            Name = request.Name,
            PhotoUrls = request.PhotoUrls,
            Category = request.Category,
            Tags = request.Tags,
            Status = request.Status
        };
        
        _db.Pets.Add(pet);
        await _db.SaveChangesAsync(cancellationToken);
        
        return pet;
    }
}
```

**Key Points**:
- Add dependencies via constructor
- Implement actual business logic
- File is protected - regenerating code won't overwrite this
- Endpoint automatically calls this handler via MediatR

---

## Example 2: Implementing a Query Handler

**Generated Scaffold** (`Handlers/GetPetByIdQueryHandler.cs`):
```csharp
public class GetPetByIdQueryHandler : IRequestHandler<GetPetByIdQuery, Pet>
{
    // TODO: Add dependencies via constructor injection
    
    public async Task<Pet> Handle(GetPetByIdQuery request, CancellationToken cancellationToken)
    {
        // TODO: Implement getPetById logic
        throw new NotImplementedException("Handler for getPetById not yet implemented");
    }
}
```

**Your Implementation**:
```csharp
public class GetPetByIdQueryHandler : IRequestHandler<GetPetByIdQuery, Pet>
{
    private readonly ApplicationDbContext _db;
    
    public GetPetByIdQueryHandler(ApplicationDbContext db)
    {
        _db = db;
    }
    
    public async Task<Pet> Handle(GetPetByIdQuery request, CancellationToken cancellationToken)
    {
        var pet = await _db.Pets
            .Include(p => p.Category)
            .Include(p => p.Tags)
            .FirstOrDefaultAsync(p => p.Id == request.PetId, cancellationToken);
        
        if (pet == null)
        {
            throw new KeyNotFoundException($"Pet with ID {request.PetId} not found");
        }
        
        return pet;
    }
}
```

**Key Points**:
- Queries typically read from database
- Can include related entities
- Throwing exceptions allows endpoint to return proper HTTP status codes

---

## Example 3: Handler with Unit Return (Delete)

**Generated Scaffold** (`Handlers/DeletePetCommandHandler.cs`):
```csharp
public class DeletePetCommandHandler : IRequestHandler<DeletePetCommand, Unit>
{
    public async Task<Unit> Handle(DeletePetCommand request, CancellationToken cancellationToken)
    {
        // TODO: Implement deletePet logic
        return Unit.Value;
    }
}
```

**Your Implementation**:
```csharp
public class DeletePetCommandHandler : IRequestHandler<DeletePetCommand, Unit>
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<DeletePetCommandHandler> _logger;
    
    public DeletePetCommandHandler(
        ApplicationDbContext db,
        ILogger<DeletePetCommandHandler> logger)
    {
        _db = db;
        _logger = logger;
    }
    
    public async Task<Unit> Handle(DeletePetCommand request, CancellationToken cancellationToken)
    {
        var pet = await _db.Pets.FindAsync(request.PetId);
        
        if (pet == null)
        {
            throw new KeyNotFoundException($"Pet with ID {request.PetId} not found");
        }
        
        _db.Pets.Remove(pet);
        await _db.SaveChangesAsync(cancellationToken);
        
        _logger.LogInformation("Deleted pet {PetId}", request.PetId);
        
        return Unit.Value; // MediatR's "void" equivalent
    }
}
```

**Key Points**:
- Use `Unit.Value` for operations with no return value
- Still async for database operations
- Exceptions bubble up to endpoint for error handling

---

## Example 4: Testing Your Handlers

```csharp
public class AddPetCommandHandlerTests
{
    [Fact]
    public async Task Handle_ValidCommand_CreatesPet()
    {
        // Arrange
        var options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseInMemoryDatabase(databaseName: "TestDb")
            .Options;
        var db = new ApplicationDbContext(options);
        var logger = new Mock<ILogger<AddPetCommandHandler>>();
        var handler = new AddPetCommandHandler(db, logger.Object);
        
        var command = new AddPetCommand
        {
            Name = "Fluffy",
            PhotoUrls = new[] { "http://example.com/fluffy.jpg" },
            Status = PetStatus.Available
        };
        
        // Act
        var result = await handler.Handle(command, CancellationToken.None);
        
        // Assert
        Assert.NotNull(result);
        Assert.Equal("Fluffy", result.Name);
        Assert.True(result.Id > 0); // Database assigned ID
    }
}
```

---

## Example 5: Adding Validation

Validation remains in endpoints (per design decision Q5:A):

**Generated Endpoint** (when `useValidators=true` and `useMediatr=true`):
```csharp
group.MapPost("/pet", async (
    IMediator mediator,
    [FromBody] AddPetCommand command,
    IValidator<AddPetCommand> validator) =>
{
    var validationResult = await validator.ValidateAsync(command);
    if (!validationResult.IsValid)
    {
        return Results.ValidationProblem(validationResult.ToDictionary());
    }
    
    var result = await mediator.Send(command, cancellationToken);
    return Results.Created($"/pet/{result.Id}", result);
})
```

**Your Validator** (`Validators/AddPetCommandValidator.cs` - user creates):
```csharp
public class AddPetCommandValidator : AbstractValidator<AddPetCommand>
{
    public AddPetCommandValidator()
    {
        RuleFor(x => x.Name)
            .NotEmpty()
            .MaximumLength(100);
        
        RuleFor(x => x.PhotoUrls)
            .NotEmpty()
            .Must(urls => urls.All(url => Uri.IsWellFormedUriString(url, UriKind.Absolute)))
            .WithMessage("All photo URLs must be valid URLs");
    }
}
```

---

## Example 6: Simple Stub Mode (useMediatr=false)

**Generated Endpoint** (default behavior):
```csharp
group.MapPost("/pet", ([FromBody] Pet pet) =>
{
    // TODO: Implement addPet logic
    return Results.Ok(pet);
})
.WithName("AddPet")
.Produces<Pet>(201);
```

**Your Implementation** (inline - good for prototypes):
```csharp
group.MapPost("/pet", ([FromBody] Pet pet) =>
{
    // Quick prototype implementation
    pet.Id = _nextId++;
    _inMemoryStore.Add(pet.Id, pet);
    return Results.Created($"/pet/{pet.Id}", pet);
})
.WithName("AddPet")
.Produces<Pet>(201);
```

---

## Regeneration Behavior

### What Happens on Regeneration?

| File Type | Regenerated? | Rationale |
|-----------|--------------|-----------|
| Commands/*.cs | ✅ YES | API contract - should match spec |
| Queries/*.cs | ✅ YES | API contract - should match spec |
| Handlers/*.cs | ❌ NO | User business logic - protected |
| Features/*Endpoints.cs | ✅ YES | Generated glue code |
| Models/*.cs | ✅ YES | DTOs from spec |
| Program.cs | ✅ YES | Infrastructure setup |

### Safe Workflow

1. **First Generation**: Run generator, creates all files
2. **Implement Handlers**: Edit handler files with your business logic
3. **Update Spec**: Modify OpenAPI specification
4. **Regenerate**: Run generator again
   - Commands/Queries updated to match new spec
   - Handlers preserved (your code safe)
   - Endpoints updated for new operations

---

## Common Patterns

### Pattern 1: Repository Pattern

```csharp
public class AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>
{
    private readonly IPetRepository _repository;
    
    public AddPetCommandHandler(IPetRepository repository)
    {
        _repository = repository;
    }
    
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        return await _repository.CreateAsync(request, cancellationToken);
    }
}
```

### Pattern 2: Mapping with AutoMapper

```csharp
public class AddPetCommandHandler : IRequestHandler<AddPetCommand, Pet>
{
    private readonly IMapper _mapper;
    private readonly ApplicationDbContext _db;
    
    public async Task<Pet> Handle(AddPetCommand request, CancellationToken cancellationToken)
    {
        var pet = _mapper.Map<Pet>(request);
        _db.Pets.Add(pet);
        await _db.SaveChangesAsync(cancellationToken);
        return pet;
    }
}
```

### Pattern 3: External API Calls

```csharp
public class GetWeatherQueryHandler : IRequestHandler<GetWeatherQuery, Weather>
{
    private readonly HttpClient _httpClient;
    
    public async Task<Weather> Handle(GetWeatherQuery request, CancellationToken cancellationToken)
    {
        var response = await _httpClient.GetAsync($"/weather/{request.City}", cancellationToken);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<Weather>(cancellationToken);
    }
}
```

---

## Migration Guide

### Switching from useMediatr=false to useMediatr=true

1. **Backup your implementations**:
   ```bash
   cp -r Features Features.backup
   ```

2. **Regenerate with MediatR**:
   ```bash
   # Add useMediatr=true flag
   java -cp "..." generate ... --additional-properties useMediatr=true
   ```

3. **Move logic to handlers**:
   - Copy business logic from old endpoints
   - Paste into generated handler scaffolds
   - Add necessary dependencies

4. **Test thoroughly**:
   - All tests should still pass
   - Handlers now contain logic
   - Endpoints are thin wrappers

---

## Troubleshooting

### Issue: Handler not found at runtime

**Symptom**: `InvalidOperationException: Handler was not found for request of type 'AddPetCommand'`

**Solution**: Ensure MediatR scans correct assembly:
```csharp
builder.Services.AddMediatR(cfg => 
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
```

### Issue: Validation not working

**Symptom**: Invalid data passes through

**Solution**: Ensure validator is registered:
```csharp
builder.Services.AddValidatorsFromAssembly(typeof(Program).Assembly);
```

### Issue: Handler regenerated and overwrote my code

**Symptom**: Business logic lost after regeneration

**Solution**: This shouldn't happen! File check should prevent it. If it does:
1. Restore from git
2. Report bug with reproduction steps
3. Verify MinimalApiServerCodegen.java has file existence check

---

## Next Steps

- Implement your first handler
- Add database context to handlers
- Create unit tests for handlers
- Configure logging and monitoring
- Add custom MediatR pipeline behaviors (optional, advanced)
