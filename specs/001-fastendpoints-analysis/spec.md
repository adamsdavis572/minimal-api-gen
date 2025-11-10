# Feature Specification: FastEndpoints Analysis and Mapping

**Feature Branch**: `001-fastendpoints-analysis`  
**Created**: 2025-11-10  
**Status**: Draft  
**Input**: User description: "Analyze AspNetCoreServerCodegen to map FastEndpoints override points and templates"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Locate and Document Base Class Structure (Priority: P1)

As a generator developer, I need to understand the structure of `AspNetCoreServerCodegen.java` and locate all FastEndpoints-related conditional logic blocks so that I can identify which methods need to be overridden in the new generator.

**Why this priority**: This is the foundation for the entire project. Without understanding the base class structure and FastEndpoints conditional logic, we cannot proceed with creating the inheritance-based generator.

**Independent Test**: Can be fully tested by locating the source file, identifying all `if ("fastendpoints".equals(library))` blocks, and producing a documented list of conditional branches. Success is measured by having a complete map of FastEndpoints-specific code paths.

**Acceptance Scenarios**:

1. **Given** the openapi-generator repository at `~/scratch/git/openapi-generator3`, **When** I locate `AspNetCoreServerCodegen.java`, **Then** I can read and analyze its contents
2. **Given** the base class source code, **When** I search for FastEndpoints conditional blocks, **Then** I find all instances of `if ("fastendpoints".equals(library))`
3. **Given** the conditional blocks, **When** I document their locations, **Then** I have a complete list with method names and line ranges

---

### User Story 2 - Map Override Methods and Their Logic (Priority: P1)

As a generator developer, I need to create a detailed map of which methods contain FastEndpoints logic (processOpts, postProcessOperationsWithModels, apiTemplateFiles, supportingFiles) so that I know exactly what to override in the new MinimalApiServerCodegen class.

**Why this priority**: This map is the blueprint for Phase 2. Without knowing which methods to override and what logic they contain, we cannot create the scaffolding correctly.

**Independent Test**: Can be fully tested by examining each identified method, documenting its FastEndpoints-specific logic, and verifying the map is complete by cross-referencing with all FastEndpoints conditional blocks found in User Story 1.

**Acceptance Scenarios**:

1. **Given** the list of FastEndpoints conditional blocks, **When** I analyze `processOpts()`, **Then** I document all FastEndpoints-specific CliOptions (like `useMediatR`)
2. **Given** the method analysis, **When** I examine `apiTemplateFiles()`, **Then** I find the line `apiTemplateFiles.put("endpoint.mustache", ".cs")`
3. **Given** the method analysis, **When** I examine `postProcessOperationsWithModels()`, **Then** I document the imports and properties added for FastEndpoints
4. **Given** the method analysis, **When** I examine `supportingFiles()`, **Then** I document all FastEndpoints-specific supporting file additions

---

### User Story 3 - Identify and Catalog Core Templates (Priority: P2)

As a generator developer, I need to identify all Mustache templates used by FastEndpoints (operation templates, supporting files, model templates) and understand their relationships so that I know which templates to copy, modify, or reuse unchanged.

**Why this priority**: Templates are the output generation mechanism. Understanding which templates exist and their interdependencies is critical for Phase 2 scaffolding and Phase 4 refactoring.

**Independent Test**: Can be fully tested by listing all templates in the `resources/aspnetcore` directory that are referenced by FastEndpoints logic, categorizing them by type, and documenting their data model dependencies.

**Acceptance Scenarios**:

1. **Given** the `resources/aspnetcore` directory, **When** I locate operation templates, **Then** I find `endpoint.mustache` and `validator.mustache`
2. **Given** the template directory, **When** I locate supporting files, **Then** I find `program.cs.mustache`, `csproj.mustache`, and related templates
3. **Given** the template directory, **When** I locate model templates, **Then** I find `model.mustache`, `modelEnum.mustache`, and related templates
4. **Given** all identified templates, **When** I analyze their variable dependencies, **Then** I document which data model properties each template consumes (e.g., `{{vars}}`, `{{operations}}`)

---

### User Story 4 - Document Template Reusability Analysis (Priority: P2)

As a generator developer, I need to understand which templates are framework-agnostic (model templates) versus framework-specific (endpoint templates) so that I can plan which templates will remain unchanged and which will need refactoring in Phase 4.

**Why this priority**: This analysis determines the scope of work for Phase 4. Knowing that model templates are 99-100% reusable reduces implementation effort and validates the inheritance approach.

**Independent Test**: Can be fully tested by analyzing each template's content for framework-specific code (FastEndpoints base classes, specific method signatures) versus generic C# POCO structures, and producing a reusability matrix.

**Acceptance Scenarios**:

1. **Given** the model templates, **When** I analyze their content, **Then** I confirm they generate framework-agnostic C# POCOs with no FastEndpoints dependencies
2. **Given** the operation templates, **When** I analyze `endpoint.mustache`, **Then** I identify FastEndpoints-specific patterns like `Endpoint<TRequest, TResponse>` base class
3. **Given** the validator templates, **When** I analyze `validator.mustache`, **Then** I identify that FluentValidation patterns are framework-agnostic and can be reused for Minimal APIs
4. **Given** all templates, **When** I create a reusability matrix, **Then** I categorize each as "Reuse Unchanged", "Modify for Minimal API", or "Replace Completely"

---

### Edge Cases

- What happens when the openapi-generator repository is not at the expected path `~/scratch/git/openapi-generator3`?
- How does the system handle multiple versions of AspNetCoreServerCodegen if the repository has multiple branches?
- What happens if new FastEndpoints conditional logic is added to the base class after the analysis is complete?
- How does the system handle templates that are referenced dynamically (not through static conditional blocks)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST locate the `AspNetCoreServerCodegen.java` file in the openapi-generator repository
- **FR-002**: System MUST identify all conditional blocks where `if ("fastendpoints".equals(library))` appears
- **FR-003**: System MUST document the methods containing FastEndpoints logic: `processOpts()`, `postProcessOperationsWithModels()`, `apiTemplateFiles()`, `supportingFiles()`
- **FR-004**: System MUST extract and document the specific logic within each FastEndpoints conditional block
- **FR-005**: System MUST locate all Mustache templates in the `resources/aspnetcore` directory
- **FR-006**: System MUST identify which templates are referenced by FastEndpoints logic
- **FR-007**: System MUST categorize templates into: operation templates, supporting files, and model templates
- **FR-008**: System MUST analyze each template's content to determine framework dependencies
- **FR-009**: System MUST produce a reusability matrix showing which templates can be reused unchanged versus which require modification
- **FR-010**: System MUST document the data model variables each template consumes (e.g., `{{vars}}`, `{{operations}}`, `{{models}}`)
- **FR-011**: System MUST identify the relationship between templates (e.g., endpoint.mustache references models from model.mustache)
- **FR-012**: All analysis results MUST be documented in a structured format (markdown tables, lists, or JSON)

### Key Entities

- **Base Class (AspNetCoreServerCodegen)**: The OpenAPI Generator class that contains FastEndpoints implementation logic; Key attributes: methods with conditional logic, template file mappings, CLI options
- **Override Method**: A Java method in the base class that contains FastEndpoints-specific logic; Key attributes: method name, FastEndpoints conditional blocks, logic description, parameters
- **Mustache Template**: A template file that generates code output; Key attributes: file name, file path, template type (operation/supporting/model), framework dependencies, reusability classification
- **Conditional Logic Block**: A section of Java code within an `if ("fastendpoints".equals(library))` statement; Key attributes: containing method, logic description, line range
- **Template Variable**: A data model property consumed by templates; Key attributes: variable name (e.g., `{{vars}}`), consuming templates, data type, source in data model

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developer can locate AspNetCoreServerCodegen.java and all FastEndpoints conditional blocks within 30 minutes
- **SC-002**: Complete method override map produced documenting at least 4 key methods (processOpts, apiTemplateFiles, postProcessOperationsWithModels, supportingFiles)
- **SC-003**: All Mustache templates identified and categorized into 3 groups (operation, supporting, model) with 100% coverage
- **SC-004**: Reusability matrix produced showing that model templates are 99-100% framework-agnostic and can be reused unchanged
- **SC-005**: Template relationship diagram created showing how endpoint.mustache and validator.mustache consume models from model.mustache
- **SC-006**: Analysis documentation is complete enough that Phase 2 can begin without referring back to the base class source code
- **SC-007**: All analysis artifacts (method map, template catalog, reusability matrix) are stored in version-controlled markdown files in the specs directory

## Assumptions

- The openapi-generator repository is available at `~/scratch/git/openapi-generator3` and contains the AspNetCoreServerCodegen class
- The FastEndpoints library implementation is stable and no major changes to conditional logic patterns occur during analysis
- The `resources/aspnetcore` directory structure is consistent and all templates follow standard Mustache syntax
- The base class follows standard OpenAPI Generator patterns for template registration and method overrides
- All FastEndpoints-specific logic is isolated within conditional blocks checking for library name equality
- Model templates generate standard C# POCO classes without framework-specific attributes or base classes

## Out of Scope

- Implementing any code changes to the generator (this is analysis only)
- Creating the MinimalApiServerCodegen class (Phase 2)
- Writing test suites (Phase 3)
- Refactoring templates to Minimal API patterns (Phase 4)
- Performance optimization of the analysis process
- Automated tooling to extract analysis data (manual analysis is acceptable)
- Analysis of non-FastEndpoints library implementations in the base class
