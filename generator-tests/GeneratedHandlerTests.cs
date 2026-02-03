using Xunit;
using FluentAssertions;
using System.IO;

namespace MinimalApiGenerator.Tests;

/// <summary>
/// Tests to verify handler.mustache template complies with Contract-First CQRS architecture (FR-029)
/// These are pure unit tests that validate template design without requiring code generation
/// </summary>
public class HandlerTemplateTests
{
    private const string TemplateDir = "../../../../generator/src/main/resources/aspnet-minimalapi";

    private static string LoadTemplate(string templateName)
    {
        var templatePath = Path.Combine(TemplateDir, templateName);
        File.Exists(templatePath).Should().BeTrue($"Template {templateName} should exist at {templatePath}");
        return File.ReadAllText(templatePath);
    }

    [Fact(Skip = "Handler scaffolding not yet implemented - template generates minimal stub")]
    public void HandlerTemplate_ShouldGenerateScaffoldedMappingMethods()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");
        
        // Assert - Check for mapping method scaffolds (FR-029)
        // When implemented, template should generate:
        // - MapCommandToDomain or MapDtoToDomain method
        // - MapDomainToDto or MapModelToDto method  
        // - Enum mapping methods
        // - TODO comments for customization guidance
        template.Should().Contain("MapCommandToDomain",
            "Handler template should scaffold MapCommandToDomain or similar mapping method (FR-029)");
        template.Should().Contain("TODO",
            "Scaffolded methods should include TODO comments for customization guidance (FR-029)");
    }

    [Fact]
    public void HandlerTemplate_MustReturnDtoType()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");
        
        // Assert - Handler should implement IRequestHandler<TCommand, TDto>
        // Template should use {{returnDtoType}} or similar for response type
        template.Should().Contain("IRequestHandler",
            "Handler template should implement IRequestHandler interface (MediatR pattern)");
        
        // Verify template doesn't hardcode Model types as return values
        template.Should().NotContain("IRequestHandler<{{operationIdCamelCase}}Command, {{returnType}}>",
            "Handler should not use {{returnType}} (Model) - should use DTO type instead (FR-027)");
    }

    [Fact]
    public void HandlerTemplate_MustReferenceModelsNamespace()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");
        
        // Assert - Handler needs access to domain Models for mapping
        template.Should().Contain("using {{packageName}}.Models;",
            "Handler template should reference Models namespace for domain entity mapping");
    }

    [Fact]
    public void HandlerTemplate_MustReferenceDtosNamespace()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");
        
        // Assert
        template.Should().Contain("using {{packageName}}.DTOs;",
            "Handler template should reference DTOs namespace for response type");
    }

    [Fact]
    public void HandlerTemplate_MustUseHandlersNamespace()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");
        
        // Assert
        template.Should().Contain("namespace {{packageName}}.Handlers;",
            "Handler template should use Handlers namespace");
    }

    [Fact]
    public void HandlerTemplate_MustHaveHandleMethod()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");
        
        // Assert - Verify Handle method signature exists
        template.Should().Contain("Handle(",
            "Handler template should define Handle method (MediatR pattern)");
        template.Should().Contain("CancellationToken",
            "Handle method should accept CancellationToken parameter");
    }
}
