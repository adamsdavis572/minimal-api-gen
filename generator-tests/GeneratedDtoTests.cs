using Xunit;
using FluentAssertions;
using System.IO;

namespace MinimalApiGenerator.Tests;

/// <summary>
/// Tests to verify dto.mustache template complies with Contract-First CQRS architecture (FR-028)
/// These are pure unit tests that validate template design without requiring code generation
/// </summary>
public class DtoTemplateTests
{
    private const string TemplateDir = "../../../../generator/src/main/resources/aspnet-minimalapi";

    private static string LoadTemplate(string templateName)
    {
        var templatePath = Path.Combine(TemplateDir, templateName);
        File.Exists(templatePath).Should().BeTrue($"Template {templateName} should exist at {templatePath}");
        return File.ReadAllText(templatePath);
    }

    [Fact]
    public void DtoTemplate_ShouldGenerateJsonConverterOnEnumProperties()
    {
        // Arrange
        var template = LoadTemplate("dto.mustache");
        
        // Assert - Check for JsonConverter attribute generation pattern (FR-028)
        // Template should generate: [System.Text.Json.Serialization.JsonConverter(typeof(...EnumMemberJsonConverter<T>))]
        template.Should().Contain("JsonConverter",
            "DTO template should generate [JsonConverter] attributes on enum properties (FR-028)");
        template.Should().Contain("EnumMemberJsonConverter",
            "DTO template should use EnumMemberJsonConverter for enum serialization (FR-028)");
    }

    [Fact]
    public void DtoTemplate_ShouldGenerateJsonPropertyNameOnEnumMembers()
    {
        // Arrange
        var template = LoadTemplate("dto.mustache");
        
        // Assert - Check for JsonPropertyName attribute generation on enum members (FR-028)
        // Template should generate: [System.Text.Json.Serialization.JsonPropertyName("value")]
        template.Should().Contain("JsonPropertyName",
            "DTO template should generate [JsonPropertyName] attributes on enum members for string-to-enum mapping (FR-028)");
    }

    [Fact]
    public void DtoTemplate_ShouldUseDtosNamespace()
    {
        // Arrange
        var template = LoadTemplate("dto.mustache");
        
        // Assert
        template.Should().Contain("namespace {{packageName}}.DTOs;",
            "DTO template should use DTOs namespace");
    }

    [Fact]
    public void DtoTemplate_ShouldIncludeSerializationUsings()
    {
        // Arrange
        var template = LoadTemplate("dto.mustache");
        
        // Assert - DTOs need System.Text.Json.Serialization for attributes
        template.Should().Contain("System.Text.Json.Serialization",
            "DTO template should reference System.Text.Json.Serialization for JsonConverter/JsonPropertyName attributes");
    }

    [Fact]
    public void DtoTemplate_ShouldNotReferenceModelsOrHandlers()
    {
        // Arrange
        var template = LoadTemplate("dto.mustache");
        
        // Assert - DTOs are Contract package, must not reference Implementation package
        template.Should().NotContain("using {{packageName}}.Models",
            "DTO template must not reference Models namespace (Contract-First architecture)");
        template.Should().NotContain("using {{packageName}}.Handlers",
            "DTO template must not reference Handlers namespace (Contract-First architecture)");
    }
}
