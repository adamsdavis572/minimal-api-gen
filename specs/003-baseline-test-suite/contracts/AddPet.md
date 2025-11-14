# POST /api/pet - Add Pet

**Operation**: Create a new pet in the store

**Endpoint**: `POST /api/pet`

**Content-Type**: `application/json`

---

## Happy Path: Valid Pet Data

### Request

**HTTP Request**:
```http
POST /api/pet HTTP/1.1
Host: localhost
Content-Type: application/json
Content-Length: 152

{
  "name": "Fluffy",
  "photoUrls": [
    "https://example.com/photos/fluffy.jpg"
  ],
  "status": "available"
}
```

**Request Body Schema**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | ✅ Yes | Pet name (must not be empty) |
| `photoUrls` | `string[]` | ✅ Yes | Array of photo URLs |
| `status` | `string` | ❌ No | Pet status (available, pending, sold) |
| `id` | `long` | ❌ No | Ignored (server generates ID) |
| `category` | `object` | ❌ No | Pet category (optional) |
| `tags` | `object[]` | ❌ No | Array of tags (optional) |

**cURL Example**:
```bash
curl -X POST http://localhost:5000/api/pet \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Fluffy",
    "photoUrls": ["https://example.com/photos/fluffy.jpg"],
    "status": "available"
  }'
```

### Response

**HTTP Response**:
```http
HTTP/1.1 201 Created
Location: /api/pet/1
Content-Type: application/json; charset=utf-8
Date: Thu, 13 Nov 2025 10:00:00 GMT
Content-Length: 178

{
  "id": 1,
  "name": "Fluffy",
  "photoUrls": [
    "https://example.com/photos/fluffy.jpg"
  ],
  "status": "available",
  "category": null,
  "tags": null
}
```

**Response Details**:
- **Status Code**: `201 Created`
- **Location Header**: `/api/pet/1` (URL of created resource)
- **Body**: Created Pet object with server-generated `id`

**Response Body Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `id` | `long` | Server-generated unique ID (always >0) |
| `name` | `string` | Pet name (echoed from request) |
| `photoUrls` | `string[]` | Photo URLs (echoed from request) |
| `status` | `string` | Pet status (echoed from request) |
| `category` | `object?` | Pet category (null if not provided) |
| `tags` | `object[]?` | Tags (null if not provided) |

**Test Assertions**:
```csharp
response.StatusCode.Should().Be(HttpStatusCode.Created); // 201
response.Headers.Location.Should().NotBeNull();
response.Headers.Location.ToString().Should().Contain("/api/pet/");

var createdPet = await response.Content.ReadFromJsonAsync<Pet>();
createdPet.Id.Should().BeGreaterThan(0);
createdPet.Name.Should().Be("Fluffy");
createdPet.Status.Should().Be("available");
```

---

## Unhappy Path: Missing Required Name Field

### Request

**HTTP Request**:
```http
POST /api/pet HTTP/1.1
Host: localhost
Content-Type: application/json
Content-Length: 97

{
  "photoUrls": [
    "https://example.com/photos/unnamed.jpg"
  ],
  "status": "available"
}
```

**Request Body** (name field missing):
```json
{
  "photoUrls": ["https://example.com/photos/unnamed.jpg"],
  "status": "available"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:5000/api/pet \
  -H "Content-Type: application/json" \
  -d '{
    "photoUrls": ["https://example.com/photos/unnamed.jpg"],
    "status": "available"
  }'
```

### Response

**HTTP Response**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json; charset=utf-8
Date: Thu, 13 Nov 2025 10:00:00 GMT
Content-Length: 245

{
  "errors": {
    "Name": [
      "'Name' must not be empty."
    ]
  },
  "type": "https://tools.ietf.org/html/rfc7231#section-6.5.1",
  "title": "One or more validation errors occurred.",
  "status": 400,
  "traceId": "00-abc123..."
}
```

**Response Details**:
- **Status Code**: `400 Bad Request`
- **Content-Type**: `application/problem+json` (FastEndpoints/FluentValidation error format)
- **Body**: Validation error details

**Response Body Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `errors` | `object` | Dictionary of field names → error messages |
| `errors.Name` | `string[]` | Array of validation errors for `Name` field |
| `type` | `string` | RFC 7231 problem type URL |
| `title` | `string` | Human-readable error title |
| `status` | `int` | HTTP status code (400) |
| `traceId` | `string` | Correlation ID for debugging |

**Test Assertions**:
```csharp
response.StatusCode.Should().Be(HttpStatusCode.BadRequest); // 400

var error = await response.Content.ReadFromJsonAsync<ValidationProblemDetails>();
error.Status.Should().Be(400);
error.Errors.Should().ContainKey("Name");
error.Errors["Name"].Should().Contain("'Name' must not be empty.");
```

---

## Implementation Notes

**FluentValidation Rules** (from AddPetRequestValidator):
```csharp
public class AddPetRequestValidator : AbstractValidator<AddPetRequest>
{
    public AddPetRequestValidator()
    {
        RuleFor(x => x.Pet.Name)
            .NotEmpty()
            .WithMessage("'Name' must not be empty.");
        
        RuleFor(x => x.Pet.PhotoUrls)
            .NotEmpty()
            .WithMessage("'PhotoUrls' must not be empty.");
    }
}
```

**In-Memory Storage Logic** (AddPetEndpoint.HandleAsync):
```csharp
private static readonly Dictionary<long, Pet> PetStore = new();
private static long _nextId = 1;

public override async Task HandleAsync(AddPetRequest req, CancellationToken ct)
{
    var pet = req.Pet with { Id = _nextId++ };
    PetStore[pet.Id] = pet;
    
    await SendCreatedAtAsync<GetPetEndpoint>(
        new { id = pet.Id },
        pet,
        cancellation: ct
    );
}
```

**Key FastEndpoints APIs Used**:
- `SendCreatedAtAsync<TEndpoint>()`: Returns 201 with Location header
- Automatic FluentValidation integration (returns 400 if validation fails)

---

## References

- [FastEndpoints: Post-Processor](https://fast-endpoints.com/docs/post-processors)
- [FastEndpoints: Validation](https://fast-endpoints.com/docs/validation)
- [RFC 7231 Section 6.3.2: 201 Created](https://tools.ietf.org/html/rfc7231#section-6.3.2)
- [RFC 7231 Section 6.5.1: 400 Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)
