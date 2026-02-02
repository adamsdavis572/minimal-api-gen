using System.Reflection;
using System.Text.Json.Serialization;
using FluentAssertions;
using PetstoreApi.DTOs;
using PetstoreApi.Converters;
using Xunit;

namespace PetstoreApi.Tests;

/// <summary>
/// Tests for validating DTO code generation compliance with FR-028
/// (DTOs have enum types with JsonConverter attributes)
/// </summary>
public class GeneratedDtoTests
{
    [Theory]
    [InlineData(typeof(AddPetDto), "Status")]
    [InlineData(typeof(UpdatePetDto), "Status")]
    [InlineData(typeof(PlaceOrderDto), "Status")]
    public void DtosHaveJsonConverterOnEnumProperties(Type dtoType, string enumPropertyName)
    {
        // Arrange
        var property = dtoType.GetProperty(enumPropertyName);
        
        // Assert - Property should exist
        property.Should().NotBeNull($"{dtoType.Name} should have a {enumPropertyName} property");
        
        // Assert - Property should be an enum type
        var propertyType = Nullable.GetUnderlyingType(property!.PropertyType) ?? property.PropertyType;
        propertyType.IsEnum.Should().BeTrue($"{enumPropertyName} should be an enum type");
        
        // Assert - Property should have JsonConverter attribute
        var converterAttribute = property.GetCustomAttribute<JsonConverterAttribute>();
        converterAttribute.Should().NotBeNull($"{enumPropertyName} should have [JsonConverter] attribute");
        
        // Assert - Converter should be EnumMemberJsonConverter<T>
        var converterType = converterAttribute!.ConverterType;
        converterType.Should().NotBeNull("JsonConverter should specify a converter type");
        converterType!.IsGenericType.Should().BeTrue("Converter should be a generic type");
        converterType.GetGenericTypeDefinition().Should().Be(typeof(EnumMemberJsonConverter<>),
            "Converter should be EnumMemberJsonConverter<T>");
    }

    [Fact]
    public void AddPetDto_StatusEnum_HasJsonPropertyNameAttributes()
    {
        // Arrange
        var statusEnumType = typeof(AddPetDto).GetNestedType("StatusEnum");
        
        // Assert - Enum type should exist
        statusEnumType.Should().NotBeNull("AddPetDto should have a nested StatusEnum type");
        statusEnumType!.IsEnum.Should().BeTrue("StatusEnum should be an enum");
        
        // Get all enum values
        var enumValues = Enum.GetValues(statusEnumType);
        
        // Assert - Each enum value should have JsonPropertyName attribute
        foreach (var enumValue in enumValues)
        {
            var memberInfo = statusEnumType.GetMember(enumValue.ToString()!)[0];
            var jsonPropertyAttribute = memberInfo.GetCustomAttribute<JsonPropertyNameAttribute>();
            
            jsonPropertyAttribute.Should().NotBeNull(
                $"Enum value {enumValue} should have [JsonPropertyName] attribute for proper serialization");
        }
    }

    [Fact]
    public void PlaceOrderDto_StatusEnum_HasJsonPropertyNameAttributes()
    {
        // Arrange
        var statusEnumType = typeof(PlaceOrderDto).GetNestedType("StatusEnum");
        
        // Assert - Enum type should exist
        statusEnumType.Should().NotBeNull("PlaceOrderDto should have a nested StatusEnum type");
        statusEnumType!.IsEnum.Should().BeTrue("StatusEnum should be an enum");
        
        // Get all enum values
        var enumValues = Enum.GetValues(statusEnumType);
        
        // Assert - Each enum value should have JsonPropertyName attribute
        foreach (var enumValue in enumValues)
        {
            var memberInfo = statusEnumType.GetMember(enumValue.ToString()!)[0];
            var jsonPropertyAttribute = memberInfo.GetCustomAttribute<JsonPropertyNameAttribute>();
            
            jsonPropertyAttribute.Should().NotBeNull(
                $"Enum value {enumValue} should have [JsonPropertyName] attribute for proper serialization");
        }
    }

    [Fact]
    public void AllDtosWithEnumProperties_HaveProperJsonConverterAttributes()
    {
        // Arrange - Find all DTOs in the assembly
        var dtoAssembly = typeof(AddPetDto).Assembly;
        var dtoTypes = dtoAssembly.GetTypes()
            .Where(t => t.Namespace == "PetstoreApi.DTOs" && t.IsClass && !t.IsNested)
            .ToList();

        // Act - Find all enum properties in DTOs
        var enumPropertiesWithoutConverter = new List<string>();
        
        foreach (var dtoType in dtoTypes)
        {
            var properties = dtoType.GetProperties(BindingFlags.Public | BindingFlags.Instance);
            
            foreach (var property in properties)
            {
                var propertyType = Nullable.GetUnderlyingType(property.PropertyType) ?? property.PropertyType;
                
                // Check if it's an enum (including nested enums)
                if (propertyType.IsEnum)
                {
                    var converterAttribute = property.GetCustomAttribute<JsonConverterAttribute>();
                    
                    if (converterAttribute == null)
                    {
                        enumPropertiesWithoutConverter.Add($"{dtoType.Name}.{property.Name}");
                    }
                    else
                    {
                        // Verify it's the correct converter type
                        var converterType = converterAttribute.ConverterType;
                        if (converterType == null || 
                            !converterType.IsGenericType || 
                            converterType.GetGenericTypeDefinition() != typeof(EnumMemberJsonConverter<>))
                        {
                            enumPropertiesWithoutConverter.Add($"{dtoType.Name}.{property.Name} (wrong converter)");
                        }
                    }
                }
            }
        }

        // Assert - All enum properties should have JsonConverter
        enumPropertiesWithoutConverter.Should().BeEmpty(
            "All enum properties in DTOs should have [JsonConverter(typeof(EnumMemberJsonConverter<T>))] attribute. " +
            "Missing or incorrect converters found on: " + string.Join(", ", enumPropertiesWithoutConverter));
    }
}
