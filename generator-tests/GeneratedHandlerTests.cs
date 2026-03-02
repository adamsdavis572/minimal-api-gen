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

    [Fact]
    public void HandlerTemplate_ShouldGenerateScaffoldedMappingMethods()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");

        // Assert - Check for mapping method scaffolds (FR-029)
        // Template generates full DTO↔Model mapping using pre-rendered code injected by the Java codegen
        template.Should().Contain("MapDtoToDomain",
            "Handler template should scaffold MapDtoToDomain method for DTO→Model conversion (FR-029)");
        template.Should().Contain("MapDomainToDto",
            "Handler template should scaffold MapDomainToDto method for Model→DTO conversion (FR-029)");
        // Note: No TODO comments — the partial class + ExecuteAsync pattern is the extension point (replaces TODO scaffolds)
    }

    [Fact]
    public void HandlerTemplate_ShouldUsePartialClass()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");

        // Assert - Generated handlers are partial so business logic can be added in a companion .Impl.cs file
        template.Should().Contain("public partial class {{handlerClassName}}",
            "Handler should be a partial class to support companion .Impl.cs business logic files");
        template.Should().NotContain("public class {{handlerClassName}}",
            "Handler must be partial — the companion .Impl.cs pattern requires it");
    }

    [Fact]
    public void HandlerTemplate_ShouldDeclarePartialExecuteAsyncMethod()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");

        // Assert - partial ExecuteAsync is the extension point for business logic
        template.Should().Contain("private partial Task",
            "Handler template should declare a private partial ExecuteAsync method for business logic injection");
        template.Should().Contain("ExecuteAsync(",
            "Handler template should declare ExecuteAsync as the extension point for business logic");
    }

    [Fact]
    public void HandlerTemplate_ShouldNotHaveDuplicateUsingMediatR()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");

        // Count occurrences of "using MediatR;"
        int count = 0;
        int idx = 0;
        while ((idx = template.IndexOf("using MediatR;", idx)) >= 0)
        {
            count++;
            idx++;
        }

        count.Should().Be(1, "handler.mustache should have exactly one 'using MediatR;' declaration");
    }

    [Fact]
    public void HandlerTemplate_ShouldInjectPreRenderedMappingBodies()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");

        // Assert - Template uses triple-brace variables for pre-rendered mapping code from Java codegen
        template.Should().Contain("{{{dtoToModelBody}}}",
            "Template should output pre-rendered DTO→Model mapping body from Java codegen");
        template.Should().Contain("{{{modelToDtoBody}}}",
            "Template should output pre-rendered Model→DTO mapping body from Java codegen");
        template.Should().Contain("{{{enumMappingMethods}}}",
            "Template should output pre-rendered enum switch methods from Java codegen");
    }

    [Fact]
    public void HandlerTemplate_ShouldNotHaveEmptyConstructor()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");

        // Assert - Constructor belongs in the companion .Impl.cs, not the generated file
        template.Should().NotContain("public {{handlerClassName}}()",
            "Handler template must not generate an empty constructor — the companion .Impl.cs owns the constructor");
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
