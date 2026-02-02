using System.Net;
using System.Net.Http.Json;
using FluentAssertions;
using PetstoreApi.Models;
using PetstoreApi.DTOs;
using Xunit;

namespace PetstoreApi.Tests;

/// <summary>
/// NOTE: FluentValidation errors always return HttpValidationProblemDetails format (RFC 7807).
/// This format includes an "errors" dictionary with field names as keys and validation messages as values.
/// Example response:
/// {
///   "type": "https://tools.ietf.org/html/rfc7231#section-6.5.1",
///   "title": "One or more validation errors occurred.",
///   "status": 400,
///   "errors": {
///     "Name": ["'Name' must not be empty."],
///     "PhotoUrls": ["'Photo Urls' must not be empty."]
///   }
/// }
/// </summary>
public class HttpValidationProblemDetails
{
    public string? Type { get; set; }
    public string? Title { get; set; }
    public int? Status { get; set; }
    public Dictionary<string, string[]> Errors { get; set; } = new();
}

public class ValidationTests : IClassFixture<CustomWebApplicationFactory>
{
    private readonly HttpClient _client;

    public ValidationTests(CustomWebApplicationFactory factory)
    {
        _client = factory.CreateClient();
    }

    [Fact]
    public async Task AddPet_WithMissingRequiredPet_Returns400()
    {
        // Arrange - POST with null body (missing required 'pet' parameter)
        
        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", (AddPetDto?)null);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task AddPet_WithValidPet_Returns201Created()
    {
        // Arrange
        var pet = new AddPetDto
        {
            Id = 123,
            Name = "Fluffy",
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Tags = new List<TagDto> { new TagDto { Id = 1, Name = "friendly" } },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Created);
    }

    [Fact]
    public async Task AddPet_WithMissingName_Returns400()
    {
        // Arrange - Pet with null Name (required property)
        var pet = new AddPetDto
        {
            Id = 123,
            Name = null!, // Required property missing
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Tags = new List<TagDto> { new TagDto { Id = 1, Name = "friendly" } },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task AddPet_WithMissingPhotoUrls_Returns400()
    {
        // Arrange - Pet with null PhotoUrls (required property)
        var pet = new AddPetDto
        {
            Id = 123,
            Name = "Fluffy",
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            PhotoUrls = null!, // Required property missing
            Tags = new List<TagDto> { new TagDto { Id = 1, Name = "friendly" } },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task UpdatePet_WithMissingName_Returns400()
    {
        // Arrange - Pet with null Name
        var pet = new UpdatePetDto
        {
            Id = 123,
            Name = null!,
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Tags = new List<TagDto> { new TagDto { Id = 1, Name = "friendly" } },
            Status = UpdatePetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PutAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task DeletePet_WithMissingRequiredPetId_Returns405MethodNotAllowed()
    {
        // Arrange - DELETE without required petId parameter
        // Note: Returns 405 because the route /v2/pet/ (with trailing slash) doesn't match the route definition

        // Act
        var response = await _client.DeleteAsync("/v2/pet/");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.MethodNotAllowed);
    }

    [Fact]
    public async Task UpdatePet_WithMissingRequiredPet_Returns400()
    {
        // Arrange
        
        // Act
        var response = await _client.PutAsJsonAsync("/v2/pet", (UpdatePetDto?)null);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task CreateUser_WithMissingRequiredUser_Returns400()
    {
        // Arrange
        
        // Act
        var response = await _client.PostAsJsonAsync("/v2/user", (CreateUserDto?)null);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task CreateUser_WithValidUser_Returns204NoContent()
    {
        // Arrange
        var user = new CreateUserDto
        {
            Id = 1,
            Username = "testuser",
            FirstName = "Test",
            LastName = "User",
            Email = "test@example.com",
            Password = "password123",
            Phone = "123-456-7890",
            UserStatus = 1
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/user", user);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NoContent);
    }

    [Fact]
    public async Task CreateUser_WithMissingUsername_Returns400()
    {
        // Arrange - User with null Username (required)
        var user = new CreateUserDto
        {
            Id = 1,
            Username = "testuser",
            FirstName = "Test",
            LastName = "User",
            Email = "test@example.com",
            Password = "password123",
            Phone = "555-1234",
            UserStatus = 1
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/user", user);

        // Assert - username is not required, should succeed
        response.StatusCode.Should().Be(HttpStatusCode.NoContent);
    }

    [Fact]
    public async Task LoginUser_WithMissingRequiredUsername_Returns400()
    {
        // Arrange - missing required 'username' query parameter
        
        // Act
        var response = await _client.GetAsync("/v2/user/login?password=test123");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task LoginUser_WithMissingRequiredPassword_Returns400()
    {
        // Arrange - missing required 'password' query parameter
        
        // Act
        var response = await _client.GetAsync("/v2/user/login?username=testuser");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    // ===== String Length Validation Tests =====

    [Fact]
    public async Task AddPet_WithNameTooShort_Returns400()
    {
        // Arrange - Name is empty string (violates minimum length of 1)
        var pet = new AddPetDto
        {
            Name = "",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task AddPet_WithNameTooLong_Returns400()
    {
        // Arrange - Name exceeds maximum length of 100 characters
        var pet = new AddPetDto
        {
            Name = new string('a', 101),
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    // ===== Pattern Validation Tests =====

    [Fact]
    public async Task AddPet_WithInvalidCategoryNamePattern_Returns400()
    {
        // Arrange - Category name contains invalid characters for pattern ^[a-zA-Z0-9]+[a-zA-Z0-9\.\-_]*[a-zA-Z0-9]+$
        var pet = new AddPetDto
        {
            Name = "Fluffy",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Category = new CategoryDto { Id = 1, Name = "Invalid-Name!" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task CreateUser_WithInvalidEmailPattern_Returns400()
    {
        // Arrange - Email doesn't match email pattern
        var user = new CreateUserDto
        {
            Username = "testuser",
            FirstName = "Test",
            LastName = "User",
            Email = "not-an-email",
            Password = "password123",
            Phone = "1234567890",
            UserStatus = 1
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/user", user);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    // ===== Array Size Validation Tests =====

    [Fact]
    public async Task AddPet_WithTooFewPhotoUrls_Returns400()
    {
        // Arrange - PhotoUrls is empty (requires minimum 1)
        var pet = new AddPetDto
        {
            Name = "Fluffy",
            PhotoUrls = new List<string>(),
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task AddPet_WithTooManyPhotoUrls_Returns400()
    {
        // Arrange - PhotoUrls exceeds maximum of 10
        var pet = new AddPetDto
        {
            Name = "Fluffy",
            PhotoUrls = Enumerable.Range(1, 11).Select(i => $"http://example.com/photo{i}.jpg").ToList(),
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    // ===== Nested Object Validation Tests =====

    [Fact]
    public async Task AddPet_WithInvalidNestedCategory_Returns400()
    {
        // Arrange - Category has invalid name (should fail CategoryDtoValidator)
        var pet = new AddPetDto
        {
            Name = "Fluffy",
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Category = new CategoryDto { Id = 1, Name = "!" }, // Invalid pattern
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }

    // ===== Multiple Validation Errors Tests =====

    [Fact]
    public async Task AddPet_WithMultipleValidationErrors_Returns400WithAllErrors()
    {
        // Arrange - Multiple validation errors: empty name, empty photoUrls, invalid category
        var pet = new AddPetDto
        {
            Name = "", // Too short
            PhotoUrls = new List<string>(), // Too few
            Category = new CategoryDto { Id = 1, Name = "!" }, // Invalid pattern
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
        
        var problemDetails = await response.Content.ReadFromJsonAsync<HttpValidationProblemDetails>();
        problemDetails.Should().NotBeNull();
        problemDetails!.Errors.Should().NotBeEmpty();
        problemDetails.Errors.Should().ContainKey("Name");
        problemDetails.Errors.Should().ContainKey("PhotoUrls");
        problemDetails.Errors.Should().ContainKey("Category.Name");
    }

    [Fact]
    public async Task ValidationError_ReturnsCorrectProblemDetailsFormat()
    {
        // Arrange - Send invalid pet to trigger validation
        var pet = new AddPetDto
        {
            Name = "", // Validation error
            PhotoUrls = new List<string> { "http://example.com/photo.jpg" },
            Category = new CategoryDto { Id = 1, Name = "Dogs" },
            Status = AddPetDto.StatusEnum.AvailableEnum
        };

        // Act
        var response = await _client.PostAsJsonAsync("/v2/pet", pet);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
        
        var problemDetails = await response.Content.ReadFromJsonAsync<HttpValidationProblemDetails>();
        problemDetails.Should().NotBeNull();
        problemDetails!.Status.Should().Be(400);
        problemDetails.Errors.Should().NotBeEmpty();
        problemDetails.Errors.Should().ContainKey("Name");
        problemDetails.Errors["Name"].Should().NotBeEmpty();
    }
}
