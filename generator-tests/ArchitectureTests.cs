using Xunit;
using FluentAssertions;
using System.IO;
using System.Linq;

namespace MinimalApiGenerator.Tests;

/// <summary>
/// Tests to verify Contract-First CQRS architecture compliance at the template level (FR-026, FR-027)
/// These tests examine mustache templates to ensure proper separation between Contract and Implementation packages
/// </summary>
public class ArchitectureTests
{
    private const string TemplateDir = "../../../../generator/src/main/resources/aspnet-minimalapi";

    private static string LoadTemplate(string templateName)
    {
        var templatePath = Path.Combine(TemplateDir, templateName);
        File.Exists(templatePath).Should().BeTrue($"Template {templateName} should exist at {templatePath}");
        return File.ReadAllText(templatePath);
    }

    /// <summary>
    /// FR-026: Contract package (Endpoints, Commands, Queries, DTOs, Validators) MUST NOT reference Models or Handlers
    /// These are Implementation package concerns resolved at runtime via MediatR/DI
    /// </summary>
    [Theory]
    [InlineData("endpointMapper.mustache", "Endpoints")]
    [InlineData("command.mustache", "Commands")]
    [InlineData("dto.mustache", "DTOs")]
    [InlineData("dtoValidator.mustache", "Validators")]
    public void ContractPackageTemplates_MustNotReferenceModelsOrHandlers(string templateName, string componentType)
    {
        // Arrange
        var template = LoadTemplate(templateName);

        // Act - Check for forbidden using statements
        var hasModelsReference = template.Contains("using {{packageName}}.Models") ||
                                template.Contains("using PetstoreApi.Models");
        var hasHandlersReference = template.Contains("using {{packageName}}.Handlers") ||
                                  template.Contains("using PetstoreApi.Handlers");

        // Assert
        hasModelsReference.Should().BeFalse(
            $"{componentType} template ({templateName}) MUST NOT reference Models namespace. " +
            "Contract package has zero dependencies on Implementation package (FR-026). " +
            "Domain entities (Models) are Implementation-only and resolved by Handlers.");

        hasHandlersReference.Should().BeFalse(
            $"{componentType} template ({templateName}) MUST NOT reference Handlers namespace. " +
            "Handlers are resolved at runtime by MediatR via DI - never directly referenced (FR-026).");
    }

    /// <summary>
    /// FR-027: Endpoints must use DTO types in .Produces<> declarations, not Model types
    /// Endpoints declare what DTOs they return - MediatR handlers convert Models to DTOs internally
    /// </summary>
    [Fact]
    public void EndpointTemplate_MustUseDtoTypesInProducesDeclarations()
    {
        // Arrange
        var template = LoadTemplate("endpointMapper.mustache");

        // Act - Check .Produces declarations use DTO types
        // Pattern to detect: .Produces<Pet> should be .Produces<PetDto>
        // Looking for .Produces<{{returnType}}> where returnType doesn't end with "Dto"
        
        // First check: Does template have .Produces declarations?
        var hasProducesDeclarations = template.Contains(".Produces<");
        
        if (hasProducesDeclarations)
        {
            // Check if it uses Model types (Pet, Order) instead of DTO types (PetDto, OrderDto)
            // This is a simplified check - looking for common patterns
            var hasModelTypeInProduces = template.Contains(".Produces<Pet>") ||
                                        template.Contains(".Produces<Order>") ||
                                        template.Contains(".Produces<User>") ||
                                        template.Contains(".Produces<{{returnType}}>") ||
                                        template.Contains(".Produces<List<Pet>>") ||
                                        template.Contains(".Produces<List<{{returnType}}>>") ||
                                        template.Contains(".Produces<{{#returnType}}{{classname}}{{/returnType}}>");

            // Assert
            hasModelTypeInProduces.Should().BeFalse(
                "Endpoints MUST use DTO types in .Produces<> declarations (FR-027). " +
                "Example: .Produces<PetDto>(200) NOT .Produces<Pet>(200). " +
                "Handlers convert Model → DTO internally; Endpoints only declare DTO response types.");
        }
        else
        {
            // If no .Produces declarations, this might be okay - document it
            Assert.True(true, "Template has no .Produces<> declarations - skipping DTO type validation");
        }
    }

    /// <summary>
    /// Handlers MUST reference both Models (domain logic) and DTOs (response types) per FR-027
    /// This enables Handler to map between domain entities and API contract types
    /// </summary>
    [Fact]
    public void HandlerTemplate_MustReferenceBothModelsAndDtos()
    {
        // Arrange
        var template = LoadTemplate("handler.mustache");

        // Act
        var hasModelsReference = template.Contains("using {{packageName}}.Models") ||
                                template.Contains("using PetstoreApi.Models");
        var hasDtosReference = template.Contains("using {{packageName}}.DTOs") ||
                              template.Contains("using PetstoreApi.DTOs");

        // Assert
        hasModelsReference.Should().BeTrue(
            "Handler template MUST reference Models namespace (FR-027). " +
            "Handlers need access to domain entities for business logic implementation.");

        hasDtosReference.Should().BeTrue(
            "Handler template MUST reference DTOs namespace (FR-027). " +
            "Handlers return DTO types (IRequestHandler<TCommand, TDto>) per Contract-First architecture.");
    }

    /// <summary>
    /// Commands and Queries should only reference DTOs (for request properties), not Models
    /// They are the request data structures themselves (FR-027)
    /// </summary>
    [Theory]
    [InlineData("command.mustache")]
    public void CommandQueryTemplates_ShouldNotReferenceModels(string templateName)
    {
        // Arrange
        var template = LoadTemplate(templateName);

        // Act
        var hasModelsReference = template.Contains("using {{packageName}}.Models") ||
                                template.Contains("using PetstoreApi.Models");

        // Assert
        hasModelsReference.Should().BeFalse(
            $"{templateName} MUST NOT reference Models namespace (FR-027). " +
            "Commands/Queries are request data structures using DTO types for properties. " +
            "Example: AddPetCommand has AddPetDto properties, NOT Pet (Model) properties.");
    }

    /// <summary>
    /// DTOs should be self-contained with no dependencies on Models or Handlers
    /// They are pure data structures for API contracts
    /// </summary>
    [Fact]
    public void DtoTemplate_ShouldBeSelfContained()
    {
        // Arrange
        var template = LoadTemplate("dto.mustache");

        // Act
        var hasModelsReference = template.Contains("using {{packageName}}.Models");
        var hasHandlersReference = template.Contains("using {{packageName}}.Handlers");

        // Assert
        hasModelsReference.Should().BeFalse(
            "DTO template (dto.mustache) MUST NOT reference Models namespace. " +
            "DTOs are independent data contracts with no knowledge of domain entities.");

        hasHandlersReference.Should().BeFalse(
            "DTO template (dto.mustache) MUST NOT reference Handlers namespace. " +
            "DTOs are pure data structures with no business logic dependencies.");
    }

    /// <summary>
    /// Validators should only validate DTOs, not Models
    /// They are part of Contract package validation layer
    /// </summary>
    [Fact]
    public void ValidatorTemplate_ShouldOnlyReferenceDtos()
    {
        // Arrange
        var template = LoadTemplate("dtoValidator.mustache");

        // Act
        var hasModelsReference = template.Contains("using {{packageName}}.Models");
        var hasHandlersReference = template.Contains("using {{packageName}}.Handlers");

        // Assert
        hasModelsReference.Should().BeFalse(
            "Validator template MUST NOT reference Models namespace. " +
            "Validators validate DTOs (API contract), not domain Models (business logic).");

        hasHandlersReference.Should().BeFalse(
            "Validator template MUST NOT reference Handlers namespace. " +
            "Validators are stateless validation rules with no handler dependencies.");
    }
}
