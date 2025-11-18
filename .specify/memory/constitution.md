<!--
Sync Impact Report:
Version Change: None → 1.0.0
Modified Principles: N/A (initial creation)
Added Sections: All sections are new
Removed Sections: None
Templates Status:
  ✅ plan-template.md - Reviewed, aligns with constitution
  ✅ spec-template.md - Reviewed, aligns with constitution  
  ✅ tasks-template.md - Reviewed, aligns with constitution
  ✅ commands/*.md - No agent-specific references found
Follow-up TODOs: None
-->

# Minimal API Generator Constitution

## Core Principles

### I. Inheritance-First Architecture
The generator MUST extend `AspNetCoreServerCodegen` rather than reimplementing from scratch. All functionality is achieved through strategic method overrides. This ensures:
- Compatibility with the base OpenAPI Generator framework
- Reuse of battle-tested model generation logic (DTOs remain framework-agnostic)
- Clear separation between inherited behavior and custom Minimal API logic
- Maintainability through minimal surface area of customization

**Rationale**: Inheritance reduces code duplication, leverages existing stable functionality, and focuses development effort only on Minimal API-specific concerns.

### II. Test-Driven Refactoring (NON-NEGOTIABLE)
Development follows a strict Red-Green-Refactor cycle with a "Golden Standard" test suite:

1. **Baseline Phase**: Create a complete xUnit test suite that validates FastEndpoints output (the starting point)
2. **Refactor Phase**: Modify generator and templates to produce Minimal API code
3. **Validation Phase**: The SAME test suite MUST pass against the new Minimal API output

Tests are written first, must fail (RED), then implementation proceeds until tests pass (GREEN), then code is refactored while keeping tests green. The test suite acts as a contract proving functional equivalence between FastEndpoints and Minimal API implementations.

**Rationale**: TDD with a baseline ensures no regression, proves correctness through executable specifications, and provides confidence that refactoring preserves behavior.

### III. Template Reusability
Model templates (`model.mustache`, `modelEnum.mustache`, etc.) MUST remain framework-agnostic and unchanged. These templates generate C# POCOs (DTOs) that are consumed identically by both FastEndpoints and Minimal APIs.

Only operation-level templates (e.g., `endpoint.mustache` → `TagEndpoints.cs.mustache`) and supporting files (`program.cs.mustache`, `csproj.mustache`) require modification.

**Rationale**: C# data contracts are framework-independent. Reusing model templates eliminates unnecessary work and ensures consistency.

### IV. Phase-Gated Progression
Development MUST proceed through strict phases, each with validation gates:

- **Phase 1 (Analysis)**: Map FastEndpoints logic in base class and templates
- **Phase 2 (Scaffolding)**: Create inheritance structure that replicates FastEndpoints
- **Phase 3 (Baseline Validation)**: Build passing xUnit test suite for FastEndpoints output
- **Phase 4 (Refactoring)**: Iteratively modify generator until Minimal API output passes tests
- **Phase 5 (Finalization)**: Document, clean up, validate completeness

No phase may begin until the previous phase's deliverables are complete and validated.

**Rationale**: Gated phases prevent premature optimization, ensure solid foundations, and provide clear checkpoints for progress verification.

### V. Build Tool Integration
All build commands (Java, Maven, .NET) MUST be executed through `devbox run <command>`. This ensures:
- Consistent, reproducible development environment across machines
- Isolation of dependencies
- Version-controlled tool configurations

**Rationale**: Environment consistency eliminates "works on my machine" issues and ensures all team members and CI/CD pipelines use identical toolchains.

## Technical Standards

### Code Generation Quality
Generated code MUST:
- Compile without errors or warnings
- Follow modern C# conventions (e.g., nullable reference types, file-scoped namespaces where appropriate)
- Include FluentValidation support with proper validator registration
- Use ASP.NET Core Minimal APIs route grouping patterns (e.g., `MapPetEndpoints()`)
- Generate one endpoint class per OpenAPI tag (not one per operation)

### Testing Requirements
All generator changes MUST be validated through:
- **Contract Tests**: Verify generated code structure matches expected patterns
- **Integration Tests**: Use `Microsoft.AspNetCore.Mvc.Testing` to validate HTTP behavior
- **Happy Path Tests**: Validate successful request/response flows
- **Unhappy Path Tests**: Validate validation failures return appropriate 400 responses

Test projects MUST use xUnit, FluentAssertions, and `CustomWebApplicationFactory` for hosting.

### Template Development Standards
Mustache templates MUST:
- Use descriptive variable names matching OpenAPI Generator's data model
- Include comments explaining complex logic
- Loop efficiently over collections (`operationsByTag`, `vars`, etc.)
- Generate properly indented C# code (template whitespace matters)
- Avoid hardcoding; use generator CLI options for configuration

## Development Workflow

### Generator Modification Cycle
1. Identify the Java method override or template requiring change
2. Make the modification in `MinimalApiServerCodegen.java` or `.mustache` file
3. Build the generator: `mvn clean package`
4. Regenerate test output: `java -jar target/generator.jar generate -g aspnetcore-minimalapi -i petstore.oas -o ./test-output`
5. Run xUnit test suite against regenerated code
6. Iterate until tests pass (RED → GREEN)
7. Refactor for clarity while maintaining GREEN tests
8. Commit with descriptive message linking to phase/task

### Version Control Practices
- Generator source code lives in this repository
- Generated test projects are tracked for validation purposes
- Use feature branches with descriptive names (e.g., `phase-4-minimal-api-refactor`)
- Commits should reference specific phase tasks or test failures addressed

### Documentation Requirements
All changes MUST be accompanied by:
- Updated README sections if user-facing behavior changes
- Inline code comments for non-obvious logic
- Template comments explaining data model expectations
- Updated CLI option documentation for new generator parameters

## Governance

This constitution supersedes all ad-hoc development practices. Any deviation from these principles requires:
1. Documented justification (captured in `analysis.md` or relevant spec)
2. Explicit approval (via PR review discussion)
3. Plan for compliance restoration if deviation is temporary

All code reviews MUST verify:
- Tests written before implementation (RED phase)
- Tests passing after implementation (GREEN phase)
- Phase gates respected (no skipping phases)
- Build commands use `devbox run`
- Generated code quality meets standards

Complexity or architectural changes MUST be justified by referencing specific OpenAPI Generator constraints or Minimal API requirements.

For runtime development guidance during implementation, refer to `.specify/templates/commands/*.md` and the phase-specific instructions in `analysis.md`.

### Amendment Process
Constitution changes require:
- MAJOR version bump: Removal of core principles or fundamental architecture change
- MINOR version bump: Addition of new principles or significant workflow modifications
- PATCH version bump: Clarifications, typo fixes, or non-semantic improvements

All amendments MUST include a Sync Impact Report documenting affected templates and follow-up work.

**Version**: 1.0.0 | **Ratified**: 2025-11-10 | **Last Amended**: 2025-11-10
