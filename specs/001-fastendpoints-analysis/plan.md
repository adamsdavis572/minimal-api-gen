# Implementation Plan: FastEndpoints Analysis and Mapping

**Branch**: `001-fastendpoints-analysis` | **Date**: 2025-11-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-fastendpoints-analysis/spec.md`

**Note**: This is Phase 1 of the Test-Driven Refactoring workflow. This analysis phase produces documentation artifacts that will guide Phase 2 (Scaffolding).

## Summary

Analyze the AspNetCoreServerCodegen class in the OpenAPI Generator repository to identify all FastEndpoints-specific conditional logic blocks, method overrides, and Mustache templates. Produce comprehensive documentation including a method override map, template catalog with categorization (operation/supporting/model), and reusability matrix showing which templates are framework-agnostic versus framework-specific. This analysis establishes the blueprint for creating the MinimalApiServerCodegen inheritance structure in Phase 2.

## Technical Context

**Language/Version**: Java (OpenAPI Generator codebase) - version detection needed from repository  
**Primary Dependencies**: OpenAPI Generator framework, Java source code analysis tools (grep, IDE)  
**Storage**: N/A (analysis produces markdown documentation artifacts)  
**Testing**: N/A (this is analysis/documentation phase - no code implementation)  
**Target Platform**: macOS (development environment) analyzing Java codebase  
**Project Type**: Analysis/Documentation project (produces markdown files in specs directory)  
**Performance Goals**: Complete analysis within 30 minutes per success criteria SC-001  
**Constraints**: Must locate repository at ~/scratch/git/openapi-generator3, must identify all FastEndpoints conditional blocks  
**Scale/Scope**: Analyzing 1 Java class (AspNetCoreServerCodegen.java), ~10-15 Mustache templates in resources/aspnetcore directory

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Phase-Gated Progression (Principle IV)**: ✅ PASS
- This is Phase 1 (Analysis) as defined in the constitution
- No prior phase deliverables required (this is the starting phase)
- Deliverables clearly defined: method override map, template catalog, reusability matrix

**Inheritance-First Architecture (Principle I)**: ✅ PASS
- This analysis identifies the base class (AspNetCoreServerCodegen) to extend
- Maps override points for strategic method overriding
- No violation: analysis supports inheritance approach

**Test-Driven Refactoring (Principle II)**: ✅ PASS
- No tests required for analysis phase (documentation only)
- Analysis output will guide test creation in Phase 3
- No violation: analysis is prerequisite to TDD

**Template Reusability (Principle III)**: ✅ PASS
- Key objective: identify framework-agnostic vs framework-specific templates
- Reusability matrix directly supports this principle
- No violation: analysis validates reusability assumptions

**Build Tool Integration (Principle V)**: ✅ PASS
- No build commands in analysis phase (documentation only)
- Future phases will use `devbox run` as required
- No violation: principle not applicable to this phase

**Overall Gate Status**: ✅ **PASS** - All constitution principles satisfied or not applicable to analysis phase. May proceed to Phase 0 research.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (analysis targets)

```text
# Analysis reads from openapi-generator repository (external)
~/scratch/git/openapi-generator3/
└── modules/openapi-generator/
    └── src/main/java/org/openapitools/codegen/languages/
        └── AspNetCoreServerCodegen.java
    └── src/main/resources/aspnetcore/
        ├── endpoint.mustache
        ├── validator.mustache
        ├── program.cs.mustache
        ├── csproj.mustache
        ├── model.mustache
        ├── modelEnum.mustache
        └── [other templates]

# Analysis produces documentation in this repository
/Users/adam/scratch/git/minimal-api-gen/
└── specs/001-fastendpoints-analysis/
    ├── spec.md                     # Input (user stories & requirements)
    ├── plan.md                     # This file
    ├── research.md                 # Phase 0 output (tool/approach decisions)
    ├── method-override-map.md      # Phase 1 output (Java methods analysis)
    ├── template-catalog.md         # Phase 1 output (template inventory)
    └── reusability-matrix.md       # Phase 1 output (template classification)
```

**Structure Decision**: This is an analysis-only feature producing documentation artifacts. No source code will be created in this phase. Analysis reads from the external openapi-generator repository and produces structured markdown documentation in the specs directory that will guide Phase 2 implementation.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Status**: ✅ No violations - Complexity tracking not required.

All constitution principles are satisfied for this analysis phase. No complexity justification needed.

## Phase Outputs Summary

### Phase 0: Research (Complete)
- ✅ **research.md**: Tool selection decisions (grep, VS Code, markdown tables)
- ✅ **research.md**: Categorization approach (3-tier classification system)
- ✅ **research.md**: Analysis workflow documented

### Phase 1: Design & Contracts (Complete)
- ✅ **data-model.md**: Entity definitions (AspNetCoreServerCodegen, OverrideMethod, MustacheTemplate, etc.)
- ✅ **quickstart.md**: Step-by-step analysis guide with validation checklist
- ✅ **Agent Context**: Updated .github/copilot-instructions.md with Java + OpenAPI Generator stack

### Phase 1: Deliverables (Created During Implementation - 2025-11-10)
- ✅ **method-override-map.md**: 15 methods documented (exceeded target of 4), including constructor, processOpts, addSupportingFiles, processOperation, and 11 setter methods
- ✅ **template-catalog.md**: Complete inventory of 17 .mustache files with categorization (Operation: 9, Supporting: 4, Model: 4)
- ✅ **reusability-matrix.md**: Classification of all 17 templates (Reuse Unchanged: 4, Modify: 3, Replace: 10) with Constitution Principle III validation

## Post-Phase 1 Constitution Re-Check

**Phase-Gated Progression**: ✅ PASS
- Phase 1 (Analysis) deliverables defined and documented
- Clear exit criteria established (method map, template catalog, reusability matrix)
- Phase 2 cannot begin until all three deliverables are created and validated

**Template Reusability**: ✅ PASS  
- Reusability matrix design explicitly validates Principle III
- "Reuse Unchanged" category targets model templates
- Framework-agnostic verification built into analysis workflow

**Documentation Requirements**: ✅ PASS
- All outputs are markdown files (version-controlled, human-readable)
- Structured formats chosen (tables for scannability)
- Inline comments in quickstart guide explain analysis steps

## Next Steps

1. **Execute Analysis** (Developer task):
   - Follow quickstart.md steps 1-10
   - Create the three deliverable files (method-override-map, template-catalog, reusability-matrix)
   - Validate completeness per quickstart Step 8 checklist
   - Commit artifacts to 001-fastendpoints-analysis branch

2. **Phase Gate Validation**:
   - Verify at least 4 methods documented in override map
   - Confirm all .mustache files listed in template catalog
   - Validate model templates marked as "Reuse Unchanged"
   - Ensure analysis completed within SC-001 target (30 minutes)

3. **Transition to Feature 002**:
   - Use method override map to guide MinimalApiServerCodegen class creation
   - Use template catalog to copy templates into new generator project
   - Reference reusability matrix for Phase 4 planning

## Implementation Notes

**Manual vs Automated**: This analysis is intentionally manual (grep + inspection) rather than automated parsing. Rationale:
- Pattern matching is straightforward (`if ("fastendpoints".equals(library))`)
- Java class is single file (~1000-2000 lines estimated)
- Human inspection provides qualitative assessment (framework dependencies, reusability)
- One-time analysis doesn't justify automation tooling investment

**Success Criteria Alignment**: 
- SC-001 (30 min): Achievable with grep + grep piping
- SC-002 (4 methods): Expect to find processOpts, apiTemplateFiles, postProcessOperationsWithModels, supportingFiles
- SC-003 (100% template coverage): `find` command provides complete file list
- SC-004 (model templates framework-agnostic): Visual inspection of model.mustache content
- SC-007 (version-controlled artifacts): All .md files in specs/ directory tracked by Git

**Risk Mitigation**:
- If repository path incorrect: Fast failure at quickstart Step 1
- If fewer than 4 methods found: Re-examine with broader search (possible missed conditional patterns)
- If templates have unexpected dependencies: Document in reusability matrix rationale column
