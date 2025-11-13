# Research: FastEndpoints Analysis Tools and Approach

**Feature**: 001-fastendpoints-analysis  
**Created**: 2025-11-10  
**Status**: Complete

## Research Questions

### Q1: Which tools should be used for Java source code analysis?

**Decision**: Combination of `grep`, `find`, and IDE (VS Code with Java extensions)

**Rationale**:
- `grep` with regex patterns efficiently locates conditional blocks: `grep -n 'if.*"fastendpoints".*equals' AspNetCoreServerCodegen.java`
- `find` command locates template files in directory tree: `find ~/scratch/git/openapi-generator3 -name "*.mustache"`
- VS Code provides syntax highlighting and navigation for understanding method context
- No specialized Java analysis tools needed for this straightforward pattern matching task

**Alternatives considered**:
- JavaParser or Spoon (Java AST parsing libraries): Overkill for simple pattern matching; adds complexity
- IntelliJ IDEA: More capable Java IDE but not required given simple analysis needs
- Manual reading only: Error-prone without search tools; misses edge cases

### Q2: What is the best format for documenting the override method map?

**Decision**: Markdown tables with method signature, location, logic summary, and code excerpt columns

**Rationale**:
- Tables provide structured, scannable format perfect for reference documentation
- Markdown is version-controlled, diff-able, and readable in VS Code/GitHub
- Code excerpts in tables show actual FastEndpoints logic without full file dumps
- Phase 2 developers can quickly find what to override without re-analyzing source

**Alternatives considered**:
- JSON format: Machine-readable but less human-friendly for manual review
- Plain text lists: Lacks structure, harder to scan for specific methods
- UML diagrams: Over-engineered for simple method mapping; maintenance burden

### Q3: How should templates be categorized for the reusability matrix?

**Decision**: Three-tier classification system: "Reuse Unchanged", "Modify for Minimal API", "Replace Completely"

**Rationale**:
- Aligns with Constitution Principle III (Template Reusability)
- "Reuse Unchanged": Model templates proven framework-agnostic by analysis
- "Modify for Minimal API": Supporting files (program.cs, csproj) need dependency changes
- "Replace Completely": Operation templates (endpoint.mustache) tightly coupled to FastEndpoints
- Clear actionable categories for Phase 4 refactoring decisions

**Alternatives considered**:
- Binary "reusable/not-reusable": Too coarse; loses nuance of modification vs replacement
- Five-tier scale (0-100% reusable): Over-precise given analysis is qualitative inspection
- Framework dependency enumeration: Too detailed; classification serves planning needs better

### Q4: Where is the openapi-generator repository expected to be located?

**Decision**: `~/scratch/git/openapi-generator3` (absolute path as specified in analysis.md and spec)

**Rationale**:
- Explicitly stated in project analysis document
- Assumption documented in spec.md
- Edge case handling: If not found, analysis tasks will fail fast with clear error

**Alternatives considered**:
- Environment variable: Adds configuration complexity unnecessary for single-developer project
- Relative path from current repo: Fragile; breaks if repo is moved
- Prompt user for location: Requires interactive script; analysis can be manual

### Q5: How should template variable dependencies be documented?

**Decision**: For each template, list consumed Mustache variables (e.g., `{{vars}}`, `{{operations}}`) with descriptions

**Rationale**:
- Documents data model contract between Java code and templates
- Helps Phase 4 refactoring by showing what data templates expect
- Simple list format sufficient; full data model not needed until Phase 2

**Alternatives considered**:
- Full OpenAPI Generator data model schema: Too comprehensive; most fields irrelevant to FastEndpoints
- Example data snippets: Helpful but time-consuming to create; variable names sufficient
- Dependency graph diagram: Visual appeal but harder to maintain as text

## Technology Stack Summary

| Component | Technology | Version | Notes |
|-----------|-----------|---------|-------|
| Source Analysis | grep, find | System default | macOS command-line tools |
| IDE | VS Code | Current | Java extension pack for syntax highlighting |
| Documentation Format | Markdown | N/A | Version-controlled in Git |
| Repository Access | File system | N/A | Direct read access to ~/scratch/git/openapi-generator3 |

## Analysis Workflow

1. **Locate Base Class**: Navigate to `AspNetCoreServerCodegen.java` in openapi-generator repo
2. **Search Conditional Blocks**: Run `grep -n 'if.*"fastendpoints".*equals' AspNetCoreServerCodegen.java`
3. **Document Methods**: For each match, extract method name, line range, and logic summary
4. **Locate Templates**: Run `find ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnetcore -name "*.mustache"`
5. **Categorize Templates**: Open each template, inspect for FastEndpoints/framework-specific code
6. **Build Reusability Matrix**: Classify each template as Reuse/Modify/Replace
7. **Document Relationships**: Note which templates reference models (e.g., endpoint.mustache → model.mustache)
8. **Validate Completeness**: Cross-reference all conditional blocks found with methods documented

## Next Phase Prerequisites

Phase 1 (Design & Contracts) can begin when:
- [ ] All FastEndpoints conditional blocks identified and documented
- [ ] Method override map completed with at least 4 methods (processOpts, apiTemplateFiles, postProcessOperationsWithModels, supportingFiles)
- [ ] All templates located and categorized
- [ ] Reusability matrix completed showing model templates are framework-agnostic

## References

- OpenAPI Generator Repository: `~/scratch/git/openapi-generator3`
- Minimal API Generator Analysis: `../../analysis.md`
- Constitution (Phase-Gated Progression): `.specify/memory/constitution.md`
