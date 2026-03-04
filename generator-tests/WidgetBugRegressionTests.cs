using Xunit;
using FluentAssertions;
using System.IO;

namespace MinimalApiGenerator.Tests;

/// <summary>
/// Regression tests that exercise known generator bugs using the Widget resource
/// added to petstore.yaml specifically for this purpose.
///
/// Each test asserts the CORRECT output pattern. Tests currently FAIL because
/// the bugs are present in the generator. When a bug is fixed, its test passes.
///
/// Pre-requisite: run `task gen:petstore` to populate test-output/ before
/// running these tests (e.g. via `task regress:full-petstore-validators-problemdetails-nuget`).
///
/// Bug reference: docs/generator-bug-report.md
/// </summary>
public class WidgetBugRegressionTests
{
    private const string TestOutputDir = "../../../../test-output";

    private static string LoadGenerated(string relativePath)
    {
        var fullPath = Path.Combine(TestOutputDir, relativePath);
        File.Exists(fullPath).Should().BeTrue(
            $"Generated file '{relativePath}' must exist — run `task gen:petstore` first.");
        return File.ReadAllText(fullPath);
    }

    // -------------------------------------------------------------------------
    // Bug 1 — Hyphenated path parameter used in Results.Created() URI string
    //
    // Generator emits:  Results.Created($"/v2/widget/{widget-id}/subwidgets", result)
    // C# cannot use a hyphenated identifier in string interpolation — compile error.
    // Correct:          Results.Created($"/widget/{widgetId}/subwidgets", result)
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug1_HyphenatedPathParam_ShouldNotAppearInResultsCreatedUri()
    {
        var content = LoadGenerated("Contract/Endpoints/WidgetApiEndpoints.cs");

        content.Should().NotContain("{widget-id}",
            "Bug 1: generator must not emit hyphenated path parameter '{widget-id}' " +
            "in Results.Created() URI — it is not a valid C# interpolation identifier");
    }

    // -------------------------------------------------------------------------
    // Bug 2 — System.IO.StreamDto used as IRequest<T> return type in Query record
    //
    // Generator emits:  public record ExportWidgetQuery : IRequest<System.IO.StreamDto>
    // System.IO.StreamDto does not exist anywhere in the .NET BCL or this codebase.
    // Correct:          IRequest<FileDto>  (or another real response DTO)
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug2_StreamDto_ShouldNotAppearInQueryRecord()
    {
        var content = LoadGenerated("Contract/Queries/ExportWidgetQuery.cs");

        content.Should().NotContain("System.IO.StreamDto",
            "Bug 2: generator must not emit 'System.IO.StreamDto' as the IRequest<T> " +
            "return type — this type does not exist");
    }

    // -------------------------------------------------------------------------
    // Bug 4 — SetValidator() called on an enum property in generated validator
    //
    // Generator emits:  .SetValidator(new WidgetTypeDtoValidator())
    //                   on the WidgetType property of WidgetDtoValidator.
    // FluentValidation's SetValidator() requires a class/record type, not an enum.
    // Correct:          remove SetValidator for enum properties entirely.
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug4_SetValidatorOnEnumProperty_ShouldNotAppearInValidator()
    {
        var content = LoadGenerated("Contract/Validators/WidgetDtoValidator.cs");

        content.Should().NotContain("SetValidator(new WidgetTypeDtoValidator",
            "Bug 4: generator must not emit SetValidator() for an enum property — " +
            "FluentValidation's SetValidator requires a class/record type, not an enum");
    }

    // -------------------------------------------------------------------------
    // Bug 5 — Nested type path used in typeof() inside validator Must() clause
    //
    // Generator emits:  typeof(WidgetDto.WidgetTypeDto)
    // WidgetTypeDto is a top-level enum DTO, not a nested type inside WidgetDto.
    // Correct:          typeof(WidgetTypeDto)
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug5_NestedTypePathInValidator_ShouldNotAppearInMustClause()
    {
        var content = LoadGenerated("Contract/Validators/WidgetDtoValidator.cs");

        content.Should().NotContain("WidgetDto.WidgetTypeDto",
            "Bug 5: generator must not emit 'WidgetDto.WidgetTypeDto' in typeof() — " +
            "WidgetTypeDto is a top-level DTO, not a nested type inside WidgetDto");
    }

    // -------------------------------------------------------------------------
    // Bug 5 (inline enum) — Nested type path for inline enum Status property
    //
    // Generator emits:  typeof(WidgetDto.StatusEnum)  inside Must() clause.
    // StatusEnum is defined as a nested type inside WidgetDto — but the Must()
    // clause should reference it as WidgetDto.StatusEnum only when called from
    // outside; inside WidgetDtoValidator the full path adds noise and is fragile.
    // Correct:          typeof(WidgetDto.StatusEnum) is acceptable for inline
    //                   enums — this test confirms the inline enum path is valid
    //                   (non-breaking variant).
    //
    // NOTE: This test intentionally passes now — it documents the contrast with
    // Bug 5 above where the type path is WRONG (WidgetDto.WidgetTypeDto for a
    // top-level enum), vs this where the path is correct (WidgetDto.StatusEnum
    // for a genuinely nested inline enum).
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug5_InlineEnumNested_StatusEnum_PathIsAcceptable()
    {
        var content = LoadGenerated("Contract/Validators/WidgetDtoValidator.cs");

        // StatusEnum IS inline inside WidgetDto, so WidgetDto.StatusEnum is correct.
        content.Should().Contain("WidgetDto.StatusEnum",
            "The inline 'Status' enum nested type reference is acceptable — " +
            "StatusEnum is genuinely defined inside WidgetDto (contrast with Bug 5 " +
            "where WidgetTypeDto is top-level but referenced as WidgetDto.WidgetTypeDto)");
    }

    // -------------------------------------------------------------------------
    // Bug 8 — System.IO.StreamDto used throughout the handler scaffold
    //
    // Generator emits:  IRequestHandler<ExportWidgetQuery, System.IO.StreamDto>
    //                   Task<System.IO.StreamDto> Handle(...)
    //                   private partial Task<System.IO.StreamDto> ExecuteAsync(...)
    // System.IO.StreamDto does not exist. Correct: use the real response DTO.
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug8_StreamDto_ShouldNotAppearInHandlerScaffold()
    {
        var content = LoadGenerated("src/PetstoreApi/Handlers/ExportWidgetQueryHandler.cs");

        content.Should().NotContain("System.IO.StreamDto",
            "Bug 8: generator must not emit 'System.IO.StreamDto' anywhere in the " +
            "handler scaffold — this type does not exist");
    }

    // -------------------------------------------------------------------------
    // Bug 9 — Inline Select lambda assigns List<Widget> to List<WidgetDto>
    //         (MapDtoToDomain and MapDomainToDto)
    //
    // Generator emits:  Children = w.Children
    //                   inside a Select(w => new Widget/WidgetDto { ... }) projection.
    // List<WidgetDto> ≠ List<Widget> — incompatible types, compile error.
    // Correct:          Children = null  (or a recursive mapping call)
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug9_Children_ShouldNotAssignRawListAcrossNamespaceBoundary()
    {
        var content = LoadGenerated("src/PetstoreApi/Handlers/CreateSubwidgetCommandHandler.cs");

        content.Should().NotContain("Children = w.Children",
            "Bug 9: generator must not emit 'Children = w.Children' in an inline " +
            "Select projection — List<Widget> and List<WidgetDto> are incompatible types");
    }

    // -------------------------------------------------------------------------
    // Bug 9 (MapDtoToDomain) — WidgetType assigned across namespace boundary inside Select
    //
    // Generator emits:  WidgetType = w.WidgetType
    //                   inside Select(w => new Widget { ... }) in MapDtoToDomain.
    // w.WidgetType is WidgetTypeDto; Widget.WidgetType is WidgetType — incompatible.
    // Correct:          cast  (WidgetType)(int)w.WidgetType
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug9_InlineSelectWidgetType_ShouldNotAssignRawEnumAcrossNamespace_MapDtoToDomain()
    {
        var content = LoadGenerated("src/PetstoreApi/Handlers/CreateSubwidgetCommandHandler.cs");

        // The raw uncast assignment WidgetType = w.WidgetType appears in the inline
        // Select inside MapDtoToDomain where w is a WidgetDto item and the target is Widget.
        content.Should().NotContain("WidgetType = w.WidgetType",
            "Bug 9 (variant): generator must not emit 'WidgetType = w.WidgetType' inside " +
            "an inline Select — WidgetTypeDto and WidgetType are different types requiring a cast");
    }

    // -------------------------------------------------------------------------
    // Bug 10 — Enum property wrongly materialised via struct-initialiser in MapDomainToDto
    //
    // Generator emits:  WidgetType = model.WidgetType != null ? new WidgetTypeDto {  } : null,
    // Constructing an enum via object-initialiser syntax is invalid C#.
    // Correct:          WidgetType = (WidgetTypeDto)(int)model.WidgetType,
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug10_StructInitEnum_ShouldNotAppearInMapDomainToDto()
    {
        var content = LoadGenerated("src/PetstoreApi/Handlers/CreateSubwidgetCommandHandler.cs");

        content.Should().NotContain("new WidgetTypeDto {  }",
            "Bug 10: generator must not emit 'new WidgetTypeDto { }' — enums cannot be " +
            "constructed with object-initialiser syntax; use (WidgetTypeDto)(int)model.WidgetType");
    }

    // -------------------------------------------------------------------------
    // Bug 12 — Enum property wrongly materialised via struct-initialiser in MapDtoToDomain
    //
    // Generator emits:  WidgetType = dto.WidgetType != null ? new WidgetType {  } : null,
    // Same pattern as Bug 10 but in the reverse mapping direction.
    // Correct:          WidgetType = (WidgetType)(int)dto.WidgetType,
    // -------------------------------------------------------------------------

    [Fact]
    public void Bug12_StructInitEnum_ShouldNotAppearInMapDtoToDomain()
    {
        var content = LoadGenerated("src/PetstoreApi/Handlers/CreateSubwidgetCommandHandler.cs");

        content.Should().NotContain("new WidgetType {  }",
            "Bug 12: generator must not emit 'new WidgetType { }' — enums cannot be " +
            "constructed with object-initialiser syntax; use (WidgetType)(int)dto.WidgetType");
    }
}
