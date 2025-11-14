# PUT /api/pet - Update Pet

**Operation**: Update an existing pet in the store

**Endpoint**: `PUT /api/pet`

**Content-Type**: `application/json`

---

## Happy Path: Valid Pet Update

### Request

**HTTP Request**:
```http
PUT /api/pet HTTP/1.1
Host: localhost
Content-Type: application/json
Content-Length: 172

{
  "id": 1,
  "name": "Fluffy Updated",
  "photoUrls": [
    "https://example.com/photos/fluffy-updated.jpg"
  ],
  "status": "sold"
}
```

**Request Body Schema**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `long` | ✅ Yes | Pet ID (must exist in store) |
| `name` | `string` | ✅ Yes | Updated pet name |
| `photoUrls` | `string[]` | ✅ Yes | Updated photo URLs |
| `status` | `string` | ❌ No | Updated status (available, pending, sold) |
| `category` | `object` | ❌ No | Updated category (optional) |
| `tags` | `object[]` | ❌ No | Updated tags (optional) |

**cURL Example**:
```bash
curl -X PUT http://localhost:5000/api/pet \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "Fluffy Updated",
    "photoUrls": ["https://example.com/photos/fluffy-updated.jpg"],
    "status": "sold"
  }'
```

### Response

**HTTP Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Date: Thu, 13 Nov 2025 10:00:00 GMT
Content-Length: 188

{
  "id": 1,
  "name": "Fluffy Updated",
  "photoUrls": [
    "https://example.com/photos/fluffy-updated.jpg"
  ],
  "status": "sold",
  "category": null,
  "tags": null
}
```

**Response Details**:
- **Status Code**: `200 OK`
- **Body**: Updated Pet object

**Response Body Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `id` | `long` | Pet ID (unchanged) |
| `name` | `string` | Updated pet name |
| `photoUrls` | `string[]` | Updated photo URLs |
| `status` | `string` | Updated status |
| `category` | `object?` | Updated category (null if not provided) |
| `tags` | `object[]?` | Updated tags (null if not provided) |

**Test Assertions**:
```csharp
response.StatusCode.Should().Be(HttpStatusCode.OK); // 200

var updatedPet = await response.Content.ReadFromJsonAsync<Pet>();
updatedPet.Id.Should().Be(1);
updatedPet.Name.Should().Be("Fluffy Updated");
updatedPet.Status.Should().Be("sold");
updatedPet.PhotoUrls.Should().Contain("https://example.com/photos/fluffy-updated.jpg");
```

---

## Unhappy Path: Update Non-Existent Pet

### Request

**HTTP Request**:
```http
PUT /api/pet HTTP/1.1
Host: localhost
Content-Type: application/json
Content-Length: 165

{
  "id": 999999,
  "name": "Ghost Pet",
  "photoUrls": [
    "https://example.com/photos/ghost.jpg"
  ],
  "status": "available"
}
```

**Request Body** (ID does not exist in store):
```json
{
  "id": 999999,
  "name": "Ghost Pet",
  "photoUrls": ["https://example.com/photos/ghost.jpg"],
  "status": "available"
}
```

**cURL Example**:
```bash
curl -X PUT http://localhost:5000/api/pet \
  -H "Content-Type: application/json" \
  -d '{
    "id": 999999,
    "name": "Ghost Pet",
    "photoUrls": ["https://example.com/photos/ghost.jpg"],
    "status": "available"
  }'
```

### Response

**HTTP Response**:
```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json; charset=utf-8
Date: Thu, 13 Nov 2025 10:00:00 GMT
Content-Length: 123

{
  "type": "https://tools.ietf.org/html/rfc7231#section-6.5.4",
  "title": "Not Found",
  "status": 404,
  "traceId": "00-ghi789..."
}
```

**Response Details**:
- **Status Code**: `404 Not Found`
- **Content-Type**: `application/problem+json` (FastEndpoints error format)
- **Body**: Problem details (cannot update non-existent pet)

**Response Body Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `type` | `string` | RFC 7231 problem type URL |
| `title` | `string` | Human-readable error title ("Not Found") |
| `status` | `int` | HTTP status code (404) |
| `traceId` | `string` | Correlation ID for debugging |

**Test Assertions**:
```csharp
response.StatusCode.Should().Be(HttpStatusCode.NotFound); // 404

var error = await response.Content.ReadFromJsonAsync<ProblemDetails>();
error.Status.Should().Be(404);
error.Title.Should().Be("Not Found");
```

---

## Implementation Notes

**FluentValidation Rules** (from UpdatePetRequestValidator):
```csharp
public class UpdatePetRequestValidator : AbstractValidator<UpdatePetRequest>
{
    public UpdatePetRequestValidator()
    {
        RuleFor(x => x.Pet.Id)
            .GreaterThan(0)
            .WithMessage("'Id' must be greater than 0.");
        
        RuleFor(x => x.Pet.Name)
            .NotEmpty()
            .WithMessage("'Name' must not be empty.");
        
        RuleFor(x => x.Pet.PhotoUrls)
            .NotEmpty()
            .WithMessage("'PhotoUrls' must not be empty.");
    }
}
```

**In-Memory Storage Logic** (UpdatePetEndpoint.HandleAsync):
```csharp
private static readonly Dictionary<long, Pet> PetStore = new();

public override async Task HandleAsync(UpdatePetRequest req, CancellationToken ct)
{
    if (!PetStore.ContainsKey(req.Pet.Id))
    {
        await SendNotFoundAsync(ct);
        return;
    }
    
    PetStore[req.Pet.Id] = req.Pet;
    await SendOkAsync(req.Pet, ct);
}
```

**Key FastEndpoints APIs Used**:
- `SendOkAsync(object)`: Returns 200 with updated JSON body
- `SendNotFoundAsync()`: Returns 404 with problem details

**Test Setup Pattern**:
```csharp
[Fact]
public async Task UpdatePet_WithValidData_Returns200OK()
{
    // Arrange - create a pet first
    var pet = new { name = "Max", photoUrls = new[] { "..." }, status = "available" };
    var createResponse = await _client.PostAsync("/api/pet", ...);
    var createdPet = await createResponse.Content.ReadFromJsonAsync<Pet>();
    var petId = createdPet.Id;
    
    // Act - update the pet
    var updatedPet = new { id = petId, name = "Max Updated", photoUrls = new[] { "..." }, status = "sold" };
    var updateContent = new StringContent(JsonSerializer.Serialize(updatedPet), Encoding.UTF8, "application/json");
    var response = await _client.PutAsync("/api/pet", updateContent);
    
    // Assert - verify update succeeded
    response.StatusCode.Should().Be(HttpStatusCode.OK);
    var result = await response.Content.ReadFromJsonAsync<Pet>();
    result.Name.Should().Be("Max Updated");
    result.Status.Should().Be("sold");
}
```

---

## References

- [FastEndpoints: Sending Responses](https://fast-endpoints.com/docs/responses)
- [RFC 7231 Section 6.3.1: 200 OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)
- [RFC 7231 Section 6.5.4: 404 Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)
