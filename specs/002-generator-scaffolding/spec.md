# Feature Specification: Generator Scaffolding via Inheritance

**Feature Branch**: `002-generator-scaffolding`  
**Created**: 2025-11-10  
**Updated**: 2025-11-11  
**Status**: Draft  
**Input**: User description: "Create MinimalApiServerCodegen class extending AbstractCSharpCodegen with FastEndpoints structure (based on AspnetFastendpointsServerCodegen analysis)"

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

### User Story 1 - Initialize Generator Project Structure (Priority: P1)

As a generator developer, I need to create the Maven project structure for the new generator within the existing openapi-generator3 repository so that I have the proper package structure to add the MinimalApiServerCodegen class.

**Why this priority**: This is the foundational step. Without the project structure integrated into the openapi-generator repository, no other development can proceed.

**Independent Test**: Can be fully tested by creating the directory structure, verifying it matches the pattern of AspnetFastendpointsServerCodegen, and confirming the build system recognizes it.

**Acceptance Scenarios**:

1. **Given** the openapi-generator repository at `~/scratch/git/openapi-generator3`, **When** I create the package directory `modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/`, **Then** I can place the MinimalApiServerCodegen.java class there
2. **Given** the created class location, **When** I inspect the structure, **Then** it matches the same pattern as AspnetFastendpointsServerCodegen.java (same parent directory)
3. **Given** the integrated structure, **When** I build with `devbox run mvn clean package` from repository root, **Then** the build includes the new generator

---

### User Story 2 - Create Generator Class with Inheritance (Priority: P1)

As a generator developer, I need to create `MinimalApiServerCodegen.java` that extends `AbstractCSharpCodegen` (same as AspnetFastendpointsServerCodegen) and replicate the FastEndpoints structure so that the generator produces FastEndpoints-compatible output initially.

**Why this priority**: This is the core logic of the generator. Without the properly configured class, no code generation will work. We replicate FastEndpoints structure first to establish a working baseline before refactoring to Minimal API patterns in Feature 004.

**Independent Test**: Can be fully tested by compiling the class, verifying it extends AbstractCSharpCodegen, and confirming all required methods from the analysis (constructor, processOpts, addSupportingFiles, processOperation, and 11 setter methods) are implemented.

**Acceptance Scenarios**:

1. **Given** the method override map from Feature 001, **When** I create the class extending AbstractCSharpCodegen, **Then** it compiles without errors
2. **Given** the constructor, **When** I configure template paths and register templates, **Then** it sets `templateDir = "aspnet-minimalapi"` and registers model.mustache, endpoint.mustache, request.mustache
3. **Given** the overridden processOpts(), **When** I implement the method, **Then** it calls all 11 setter methods (setUseProblemDetails, setUseRecordForRequest, etc.) following the FastEndpoints pattern
4. **Given** the addSupportingFiles() method, **When** I implement it, **Then** it registers all supporting files (program.mustache, project.csproj.mustache, solution.mustache, etc.) with conditional authentication files
5. **Given** the implemented class, **When** I register it in META-INF/services/org.openapitools.codegen.CodegenConfig, **Then** the generator is discoverable with `-g aspnetcore-minimalapi`

---

### User Story 3 - Copy and Register Templates (Priority: P1)

As a generator developer, I need to copy all 17 FastEndpoints templates from `aspnet-fastendpoints/` to a new `aspnet-minimalapi/` resource directory and ensure they are properly registered so that the generator can produce FastEndpoints code initially.

**Why this priority**: Templates are the output mechanism. Without templates, the generator cannot produce any code. We copy FastEndpoints templates first to establish a working baseline before refactoring them to Minimal API patterns in Feature 004.

**Independent Test**: Can be fully tested by copying templates, building the generator, running against a test OpenAPI spec, and verifying FastEndpoints-compatible code is generated with all expected files.

**Acceptance Scenarios**:

1. **Given** the 17 templates from Feature 001 template catalog, **When** I copy them to `modules/openapi-generator/src/main/resources/aspnet-minimalapi/`, **Then** all templates are present: 9 operation templates (endpoint.mustache, request.mustache, requestClass.mustache, requestRecord.mustache, endpointType.mustache, endpointRequestType.mustache, endpointResponseType.mustache, loginRequest.mustache, userLoginEndpoint.mustache), 4 supporting templates (program.mustache, project.csproj.mustache, solution.mustache, readme.mustache), 4 model templates (model.mustache, modelClass.mustache, modelRecord.mustache, enumClass.mustache)
2. **Given** the constructor implementation, **When** I set `templateDir = "aspnet-minimalapi"`, **Then** the generator loads templates from the new resource directory
3. **Given** the complete setup, **When** I run `devbox run mvn clean package` from repository root, **Then** the build succeeds and includes the new generator with templates
4. **Given** the built generator, **When** I run it against a simple OpenAPI spec (e.g., petstore.yaml), **Then** it produces a complete project structure with Program.cs, .csproj, Models/, and endpoint files

---

### Edge Cases

- What happens when the base class AbstractCSharpCodegen changes its method signatures in a future OpenAPI Generator version?
- How does the generator handle missing templates or template syntax errors during code generation?
- What happens if a required CLI option is not provided when running the generator?
- How does the generator behave when the OpenAPI spec contains unsupported features or malformed data?
- What happens if the template directory path is incorrect or templates are not bundled in the JAR?
- How does the generator handle conflicts when output files already exist in the target directory?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create MinimalApiServerCodegen.java in modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/
- **FR-002**: System MUST extend AbstractCSharpCodegen (not AspNetCoreServerCodegen)
- **FR-003**: System MUST implement constructor that configures outputFolder, embeddedTemplateDir, templateDir, modelTemplateFiles, and apiTemplateFiles
- **FR-004**: System MUST register 11 CLI options in constructor (useProblemDetails, useRecords, useAuthentication, useValidators, useResponseCaching, useApiVersioning, routePrefix, versioningPrefix, apiVersion, solutionGuid, projectConfigurationGuid)
- **FR-005**: System MUST override processOpts() to call all 11 setter methods and addSupportingFiles()
- **FR-006**: System MUST implement addSupportingFiles() to register all supporting templates (conditionally including authentication files)
- **FR-007**: System MUST override processOperation() to convert HTTP methods to PascalCase
- **FR-008**: System MUST implement all 11 setter methods to read CLI options and store in additionalProperties
- **FR-009**: System MUST copy all 17 templates from aspnet-fastendpoints/ to aspnet-minimalapi/ resource directory
- **FR-010**: System MUST include 4 static files (gitignore, appsettings.json, appsettings.Development.json, Properties/launchSettings.json)
- **FR-011**: System MUST override getName() to return "aspnetcore-minimalapi"
- **FR-012**: System MUST override getHelp() to return descriptive help text
- **FR-013**: System MUST override getTag() to return CodegenType.SERVER
- **FR-014**: System MUST register generator in META-INF/services/org.openapitools.codegen.CodegenConfig with fully qualified class name
- **FR-015**: System MUST build successfully using devbox run mvn clean package from repository root
- **FR-016**: Generated code MUST produce a complete FastEndpoints-compatible project when run against any valid OpenAPI spec

### Key Entities

- **Generator Class (MinimalApiServerCodegen)**: The main code generation class; Key attributes: extends AbstractCSharpCodegen, 222 lines (similar to AspnetFastendpointsServerCodegen), 15 methods (constructor, processOpts, addSupportingFiles, processOperation, 11 setters), template directory path "aspnet-minimalapi"
- **Template Set**: Collection of 17 Mustache templates + 4 static files; Key attributes: 9 operation templates, 4 supporting templates, 4 model templates, resource directory location at modules/openapi-generator/src/main/resources/aspnet-minimalapi/
- **CLI Options**: 11 configurable boolean/string options; Key attributes: useProblemDetails, useRecords, useAuthentication, useValidators, useResponseCaching, useApiVersioning, routePrefix, versioningPrefix, apiVersion, solutionGuid, projectConfigurationGuid
- **Service Registration**: META-INF configuration for generator discovery; Key attributes: fully qualified class name org.openapitools.codegen.languages.MinimalApiServerCodegen, service provider interface org.openapitools.codegen.CodegenConfig

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Generator project builds successfully in under 2 minutes using devbox run mvn clean package from repository root
- **SC-002**: MinimalApiServerCodegen class compiles with zero errors and extends AbstractCSharpCodegen
- **SC-003**: Class contains exactly 15 methods matching AspnetFastendpointsServerCodegen structure (constructor, processOpts, addSupportingFiles, processOperation, 11 setter methods)
- **SC-004**: All 17 templates copied to aspnet-minimalapi/ resource directory with 100% file coverage
- **SC-005**: Generator discoverable via `-g aspnetcore-minimalapi` flag when running openapi-generator-cli
- **SC-006**: Running generator against any valid OpenAPI 3.x spec produces complete FastEndpoints-compatible project
- **SC-007**: Generated project structure includes expected directories and files (Program.cs, .csproj, .sln, Models/, Features/)
- **SC-008**: Generated .csproj file contains FastEndpoints NuGet package references (FastEndpoints, FastEndpoints.Swagger, conditionally FastEndpoints.Security)
- **SC-009**: Generated C# code compiles successfully with dotnet build (proves templates are syntactically correct)

## Assumptions

- OpenAPI Generator repository is available at ~/scratch/git/openapi-generator3 with full source code
- Java (matching repository requirements) and Maven are available through devbox
- The base class AbstractCSharpCodegen API is stable across OpenAPI Generator versions
- All 17 FastEndpoints templates from Feature 001 analysis are syntactically correct and complete
- The existing openapi-generator build system supports adding new generators without modifying core build configuration
- AspnetFastendpointsServerCodegen serves as the reference implementation (222 lines, proven working pattern)
- Generated code will initially be FastEndpoints-compatible (Minimal API refactoring deferred to Feature 004)

## Out of Scope

- Modifying templates to Minimal API patterns (deferred to Feature 004 - TDD Refactoring)
- Writing test suites or "Golden Standard" validation (deferred to Feature 003 - Baseline Test Suite)
- Refactoring Java class logic beyond exact FastEndpoints replication
- Changing CLI option names or behavior from FastEndpoints pattern
- Performance optimization or code generation speed improvements
- Comprehensive documentation beyond inline code comments (deferred to Feature 005 - Finalization & Documentation)
- Integration with OpenAPI Generator CI/CD pipeline or release process
- Supporting OpenAPI 2.0 (Swagger) specs (focus on OpenAPI 3.x only)
