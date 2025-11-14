# DELETE /api/pet/{id} - Delete Pet

**Operation**: Delete a pet from the store by ID

**Endpoint**: `DELETE /api/pet/{id}`

**Path Parameters**: `id` (long, required) - Pet ID to delete

---

## Happy Path: Delete Existing Pet

### Request

**HTTP Request**:
```http
DELETE /api/pet/1 HTTP/1.1
Host: localhost
```

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `long` | ✅ Yes | Pet ID to delete (must exist in store) |

**cURL Example**:
```bash
curl -X DELETE http://localhost:5000/api/pet/1
```

### Response

**HTTP Response**:
```http
HTTP/1.1 204 No Content
Date: Thu, 13 Nov 2025 10:00:00 GMT
```

**Response Details**:
- **Status Code**: `204 No Content`
- **Body**: Empty (no response body)

**Test Assertions**:
```csharp
response.StatusCode.Should().Be(HttpStatusCode.NoContent); // 204
```

---

## Unhappy Path: Delete Non-Existent Pet

### Request

**HTTP Request**:
```http
DELETE /api/pet/999999 HTTP/1.1
Host: localhost
```

**Path Parameters** (ID does not exist in store):
```
id = 999999
```

**cURL Example**:
```bash
curl -X DELETE http://localhost:5000/api/pet/999999
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
  "traceId": "00-jkl012..."
}
```

**Response Details**:
- **Status Code**: `404 Not Found`
- **Content-Type**: `application/problem+json` (FastEndpoints error format)
- **Body**: Problem details (cannot delete non-existent pet)

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

**In-Memory Storage Logic** (DeletePetEndpoint.HandleAsync):
```csharp
private static readonly Dictionary<long, Pet> PetStore = new();

public override async Task HandleAsync(DeletePetRequest req, CancellationToken ct)
{
    if (!PetStore.Remove(req.PetId))
    {
        await SendNotFoundAsync(ct);
        return;
    }
    
    await SendNoContentAsync(ct);
}
```

**Key FastEndpoints APIs Used**:
- `SendNoContentAsync()`: Returns 204 with empty body
- `SendNotFoundAsync()`: Returns 404 with problem details

**Dictionary.Remove Behavior**:
- Returns `true` if key existed and was removed
- Returns `false` if key did not exist (no removal)

**Test Setup Pattern**:
```csharp
[Fact]
public async Task DeletePet_WithExistingId_Returns204NoContent()
{
    // Arrange - create a pet first
    var pet = new { name = "Rex", photoUrls = new[] { "..." }, status = "available" };
    var createResponse = await _client.PostAsync("/api/pet", ...);
    var createdPet = await createResponse.Content.ReadFromJsonAsync<Pet>();
    var petId = createdPet.Id;
    
    // Act - delete the pet
    var response = await _client.DeleteAsync($"/api/pet/{petId}");
    
    // Assert - verify deletion succeeded
    response.StatusCode.Should().Be(HttpStatusCode.NoContent);
    
    // Optional: verify pet is gone
    var getResponse = await _client.GetAsync($"/api/pet/{petId}");
    getResponse.StatusCode.Should().Be(HttpStatusCode.NotFound);
}
```

---

## References

- [FastEndpoints: Sending Responses](https://fast-endpoints.com/docs/responses)
- [RFC 7231 Section 6.3.5: 204 No Content](https://tools.ietf.org/html/rfc7231#section-6.3.5)
- [RFC 7231 Section 6.5.4: 404 Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)
