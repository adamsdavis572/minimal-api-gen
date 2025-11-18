# Feature Specification: Generator Finalization and Documentation

**Feature Branch**: `005-finalization-docs`  
**Created**: 2025-11-10  
**Status**: Draft  
**Input**: User description: "Finalize generator with cleanup, documentation, and CLI options reference"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Cleanup Unused Templates and Code (Priority: P1)

As a generator developer, I need to delete all unused FastEndpoints templates (endpoint.mustache) and remove any dead code from Java classes so that the generator codebase is clean and maintainable.

**Why this priority**: Code cleanup prevents confusion and technical debt. Removing unused artifacts ensures the codebase accurately reflects current functionality.

**Independent Test**: Can be fully tested by searching for unused template references, deleting files, rebuilding the generator, and verifying it still works correctly.

**Acceptance Scenarios**:

1. **Given** the template directory, **When** I identify endpoint.mustache as unused, **Then** I delete it from src/main/resources/aspnetcore-minimalapi/
2. **Given** the Java code, **When** I search for commented-out or dead code, **Then** I remove it
3. **Given** the cleaned codebase, **When** I rebuild with devbox run mvn clean package, **Then** build succeeds
4. **Given** the rebuilt generator, **When** I run against petstore.oas, **Then** output is identical to pre-cleanup

---

### User Story 2 - Write Comprehensive README Documentation (Priority: P1)

As a generator user, I need comprehensive README documentation explaining the inheritance architecture, how to build and use the generator, and what output it produces so that I can understand and use the generator effectively.

**Why this priority**: Documentation is essential for usability. Without clear instructions, users cannot adopt or contribute to the generator.

**Independent Test**: Can be fully tested by following the README instructions from scratch on a clean machine and successfully generating a Minimal API project.

**Acceptance Scenarios**:

1. **Given** a new README.md file, **When** I write the Overview section, **Then** it explains the inheritance from AspNetCoreServerCodegen
2. **Given** the README, **When** I write the Build Instructions, **Then** they include devbox run mvn clean package
3. **Given** the README, **When** I write the Usage section, **Then** it shows the complete java -jar command with -g aspnetcore-minimalapi
4. **Given** the README, **When** I write the Output Structure section, **Then** it describes the generated project layout with TagEndpoints classes

---

### User Story 3 - Document CLI Options Reference (Priority: P2)

As a generator user, I need complete documentation of all CLI options (useRouteGroups, useGlobalExceptionHandler, etc.) with descriptions and examples so that I can customize generator behavior for my use case.

**Why this priority**: CLI options are power-user features. Clear documentation enables advanced customization and helps users understand available configuration.

**Independent Test**: Can be fully tested by reading the options documentation, trying each option, and verifying the behavior matches the documented description.

**Acceptance Scenarios**:

1. **Given** the CLI options implemented in processOpts(), **When** I document useRouteGroups, **Then** I explain it groups endpoints by tag
2. **Given** the options documentation, **When** I document useGlobalExceptionHandler, **Then** I explain it adds centralized error handling
3. **Given** each option, **When** I provide examples, **Then** I show the command-line syntax (e.g., -p useRouteGroups=true)
4. **Given** the complete options reference, **When** I add it to README or separate OPTIONS.md, **Then** all options from processOpts() are documented

---

### Edge Cases

- What happens when the README instructions are followed but devbox is not installed?
- How does the documentation handle users unfamiliar with OpenAPI Generator conventions?
- What happens if CLI option names change but documentation is not updated?
- How does the generator communicate deprecation of old options?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST delete endpoint.mustache and any other unused FastEndpoints templates
- **FR-002**: System MUST remove dead code and commented-out logic from Java classes
- **FR-003**: System MUST create README.md in the generator project root
- **FR-004**: README MUST include Overview section explaining inheritance architecture
- **FR-005**: README MUST include Prerequisites section listing Java, Maven, devbox requirements
- **FR-006**: README MUST include Build Instructions with devbox run mvn clean package
- **FR-007**: README MUST include Usage section with complete generation command example
- **FR-008**: README MUST include Output Structure section describing generated project layout
- **FR-009**: README MUST include or reference CLI options documentation
- **FR-010**: System MUST document all CLI options added in processOpts()
- **FR-011**: Each CLI option MUST have description, type, default value, and example
- **FR-012**: Documentation MUST include example OpenAPI spec (or reference to petstore.oas)
- **FR-013**: README MUST include Contributing section explaining how to modify templates
- **FR-014**: All code comments MUST be reviewed for accuracy and clarity
- **FR-015**: Generator MUST still build and work correctly after cleanup

### Key Entities

- **Template File**: Mustache template in resources directory; Key attributes: file name, usage status (active/unused), deletion candidate flag
- **README Documentation**: Markdown file providing user guidance; Key attributes: sections (overview, build, usage, output), completeness, accuracy
- **CLI Option**: Generator configuration parameter; Key attributes: option name, type, default value, description, usage example
- **Code Comment**: Inline documentation in Java classes; Key attributes: location, accuracy, relevance to current code

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero unused template files remain in the resources directory
- **SC-002**: Zero dead code or commented-out logic blocks remain in Java classes
- **SC-003**: README documentation is complete with all required sections (minimum 6 sections)
- **SC-004**: A new user can follow README instructions and generate a working project in under 15 minutes
- **SC-005**: 100% of CLI options are documented with descriptions and examples
- **SC-006**: Generator builds and passes all Feature 003 tests after cleanup
- **SC-007**: Documentation reviewed and approved by at least one other developer (or passes readability checklist)

## Assumptions

- The generator codebase is stable after Feature 004 refactoring
- petstore.oas is an appropriate example for documentation
- Users have basic familiarity with OpenAPI Generator concepts
- devbox is the standard build environment for this project
- README.md is the primary documentation location (vs separate docs directory)
- CLI options finalized in Feature 004 do not require further changes

## Out of Scope

- Automated documentation generation from code
- Detailed template customization guide (basic guidance only)
- Publishing generator to Maven Central or npm
- Creating video tutorials or additional training materials
- Migrating documentation to a separate docs website
- Internationalization of documentation
- Creating detailed architecture diagrams (text descriptions sufficient)
