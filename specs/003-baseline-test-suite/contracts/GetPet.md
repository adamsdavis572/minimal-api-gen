# GET /api/pet/{id} - Get Pet by ID

**Operation**: Retrieve a pet by its ID

**Endpoint**: `GET /api/pet/{id}`

**Path Parameters**: `id` (long, required) - Pet ID to retrieve

---

## Happy Path: Existing Pet ID

### Request

**HTTP Request**:
```http
GET /api/pet/1 HTTP/1.1
Host: localhost
Accept: application/json
```

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `long` | ✅ Yes | Pet ID (must be >0) |

**cURL Example**:
```bash
curl -X GET http://localhost:5000/api/pet/1 \
  -H "Accept: application/json"
```

### Response

**HTTP Response**:
```http
HTTP/1.1 200 OK
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
- **Status Code**: `200 OK`
- **Body**: Pet object matching the requested ID

**Response Body Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `id` | `long` | Pet ID (matches path parameter) |
| `name` | `string` | Pet name |
| `photoUrls` | `string[]` | Array of photo URLs |
| `status` | `string` | Pet status (available, pending, sold) |
| `category` | `object?` | Pet category (null if not set) |
| `tags` | `object[]?` | Tags (null if not set) |

**Test Assertions**:
```csharp
response.StatusCode.Should().Be(HttpStatusCode.OK); // 200

var pet = await response.Content.ReadFromJsonAsync<Pet>();
pet.Id.Should().Be(1);
pet.Name.Should().Be("Fluffy");
pet.PhotoUrls.Should().Contain("https://example.com/photos/fluffy.jpg");
pet.Status.Should().Be("available");
```

---

## Unhappy Path: Non-Existent Pet ID

### Request

**HTTP Request**:
```http
GET /api/pet/999999 HTTP/1.1
Host: localhost
Accept: application/json
```

**Path Parameters** (ID does not exist in store):
```
id = 999999
```

**cURL Example**:
```bash
curl -X GET http://localhost:5000/api/pet/999999 \
  -H "Accept: application/json"
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
  "traceId": "00-def456..."
}
```

**Response Details**:
- **Status Code**: `404 Not Found`
- **Content-Type**: `application/problem+json` (FastEndpoints error format)
- **Body**: Problem details (no pet data)

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

**In-Memory Storage Logic** (GetPetEndpoint.HandleAsync):
```csharp
private static readonly Dictionary<long, Pet> PetStore = new();

public override async Task HandleAsync(GetPetByIdRequest req, CancellationToken ct)
{
    if (!PetStore.TryGetValue(req.PetId, out var pet))
    {
        await SendNotFoundAsync(ct);
        return;
    }
    
    await SendOkAsync(pet, ct);
}
```

**Key FastEndpoints APIs Used**:
- `SendOkAsync(object)`: Returns 200 with JSON body
- `SendNotFoundAsync()`: Returns 404 with problem details

**Test Setup Pattern**:
```csharp
[Fact]
public async Task GetPet_WithExistingId_ReturnsPet()
{
    // Arrange - create a pet first
    var pet = new { name = "Buddy", photoUrls = new[] { "..." }, status = "available" };
    var createResponse = await _client.PostAsync("/api/pet", ...);
    var createdPet = await createResponse.Content.ReadFromJsonAsync<Pet>();
    var petId = createdPet.Id;
    
    // Act - retrieve the pet
    var response = await _client.GetAsync($"/api/pet/{petId}");
    
    // Assert - verify same pet returned
    response.StatusCode.Should().Be(HttpStatusCode.OK);
    var retrievedPet = await response.Content.ReadFromJsonAsync<Pet>();
    retrievedPet.Id.Should().Be(petId);
    retrievedPet.Name.Should().Be("Buddy");
}
```

---

## References

- [FastEndpoints: Sending Responses](https://fast-endpoints.com/docs/responses)
- [RFC 7231 Section 6.3.1: 200 OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)
- [RFC 7231 Section 6.5.4: 404 Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)
