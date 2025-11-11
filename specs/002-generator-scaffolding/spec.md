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

As a generator developer, I need to use the OpenAPI Generator `meta` command to scaffold a standalone generator project, then copy the generated structure into this project (~/scratch/git/minimal-api-gen) so that I can develop, build, and test the generator independently from the main openapi-generator repository.

**Why this priority**: This is the foundational step. The meta command creates the proper Maven project structure with all necessary configuration files. Copying it to our project allows independent development without modifying the upstream openapi-generator repository.

**Independent Test**: Can be fully tested by running the meta command, verifying the generated structure, copying files to this project, and confirming the Maven build succeeds in the new location.

**Acceptance Scenarios**:

1. **Given** the openapi-generator-cli at `~/scratch/git/openapi-generator3`, **When** I run `java -jar modules/openapi-generator-cli/target/openapi-generator-cli.jar meta -n aspnetcore-minimalapi -p org.openapitools.codegen.minimalapi -o /tmp/minimalapi-generator`, **Then** a complete Maven project is generated with pom.xml, src/main/java/, src/main/resources/, and README.md
2. **Given** the generated project at `/tmp/minimalapi-generator`, **When** I copy the generated structure to `~/scratch/git/minimal-api-gen/generator/`, **Then** the project structure includes src/main/java/org/openapitools/codegen/minimalapi/ directory with AspnetcoreMinimalapiGenerator.java skeleton
3. **Given** the copied project structure, **When** I inspect the files, **Then** I find pom.xml with openapi-generator dependencies, META-INF/services registration file, and resource directories
4. **Given** the copied project, **When** I run `devbox run mvn clean package` from generator/ directory, **Then** the build completes successfully producing openapi-generator-minimalapi-1.0.0.jar

---

### User Story 2 - Create Generator Class with Inheritance (Priority: P1)

As a generator developer, I need to replace the meta-generated skeleton class with `MinimalApiServerCodegen.java` that extends `AbstractCSharpCodegen` (following AspnetFastendpointsServerCodegen pattern) and replicate the FastEndpoints structure so that the generator produces FastEndpoints-compatible output initially.

**Why this priority**: This is the core logic of the generator. The meta command creates a basic skeleton, but we need to replace it with the full implementation based on Feature 001 analysis. We replicate FastEndpoints structure first to establish a working baseline before refactoring to Minimal API patterns in Feature 004.

**Independent Test**: Can be fully tested by replacing the skeleton class, compiling the new implementation, verifying it extends AbstractCSharpCodegen, and confirming all required methods from the analysis (constructor, processOpts, addSupportingFiles, processOperation, and 11 setter methods) are implemented.

**Acceptance Scenarios**:

1. **Given** the meta-generated AspnetcoreMinimalapiGenerator.java skeleton at generator/src/main/java/org/openapitools/codegen/minimalapi/, **When** I replace it with MinimalApiServerCodegen.java extending AbstractCSharpCodegen, **Then** it compiles without errors
2. **Given** the constructor, **When** I configure template paths and register templates, **Then** it sets `outputFolder = "generated-code/aspnet-minimalapi"`, `templateDir = "aspnet-minimalapi"`, and registers model.mustache, endpoint.mustache, request.mustache in modelTemplateFiles and apiTemplateFiles
3. **Given** the overridden processOpts(), **When** I implement the method calling all 11 setter methods, **Then** it processes CLI options following the FastEndpoints pattern (setUseProblemDetails, setUseRecordForRequest, setUseAuthentication, etc.)
4. **Given** the addSupportingFiles() method, **When** I implement it, **Then** it registers all supporting files (program.mustache, project.csproj.mustache, solution.mustache, readme.mustache, gitignore, appsettings.json, etc.) with conditional authentication files (loginRequest.mustache, userLoginEndpoint.mustache when useAuthentication is true)
5. **Given** the implemented class, **When** the META-INF/services/org.openapitools.codegen.CodegenConfig file is updated with org.openapitools.codegen.minimalapi.MinimalApiServerCodegen, **Then** the generator is discoverable with `-g aspnetcore-minimalapi`
6. **Given** the complete implementation, **When** I run `devbox run mvn clean package` from generator/ directory, **Then** the build succeeds producing a JAR with the generator class

---

### User Story 3 - Copy and Register Templates (Priority: P1)

As a generator developer, I need to copy all 17 FastEndpoints templates from the upstream openapi-generator repository (`~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/`) to the project's resource directory (`generator/src/main/resources/aspnet-minimalapi/`) and ensure they are properly registered so that the generator can produce FastEndpoints code initially.

**Why this priority**: Templates are the output mechanism. Without templates, the generator cannot produce any code. We copy FastEndpoints templates first to establish a working baseline before refactoring them to Minimal API patterns in Feature 004.

**Independent Test**: Can be fully tested by copying templates, building the generator in this project, running against a test OpenAPI spec, and verifying FastEndpoints-compatible code is generated with all expected files.

**Acceptance Scenarios**:

1. **Given** the 17 templates from Feature 001 template catalog at `~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/`, **When** I copy them to `~/scratch/git/minimal-api-gen/generator/src/main/resources/aspnet-minimalapi/`, **Then** all templates are present: 9 operation templates (endpoint.mustache, request.mustache, requestClass.mustache, requestRecord.mustache, endpointType.mustache, endpointRequestType.mustache, endpointResponseType.mustache, loginRequest.mustache, userLoginEndpoint.mustache), 4 supporting templates (program.mustache, project.csproj.mustache, solution.mustache, readme.mustache), 4 model templates (model.mustache, modelClass.mustache, modelRecord.mustache, enumClass.mustache)
2. **Given** the 4 static files from Feature 001 analysis, **When** I copy them to `generator/src/main/resources/aspnet-minimalapi/`, **Then** I have gitignore, appsettings.json, appsettings.Development.json, and Properties/launchSettings.json
3. **Given** the MinimalApiServerCodegen constructor, **When** it sets `templateDir = "aspnet-minimalapi"`, **Then** the generator loads templates from this project's resource directory
4. **Given** the complete setup, **When** I run `devbox run mvn clean package` from `~/scratch/git/minimal-api-gen/generator/`, **Then** the build succeeds producing openapi-generator-minimalapi-1.0.0.jar with all templates bundled
5. **Given** the built generator JAR, **When** I run `java -jar target/openapi-generator-minimalapi-1.0.0.jar generate -g aspnetcore-minimalapi -i petstore.yaml -o /tmp/test-output`, **Then** it produces a complete project structure with Program.cs, .csproj, .sln, Models/ directory, Features/ directory, and endpoint files

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

- **FR-001**: System MUST execute OpenAPI Generator meta command to scaffold generator project: `java -jar ~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar meta -n aspnet-minimalapi -p org.openapitools.codegen -o /tmp/aspnet-minimalapi-gen`
- **FR-002**: System MUST copy scaffolded files to this project: copy `src/` directory and `pom.xml` from `/tmp/aspnet-minimalapi-gen/` to `~/scratch/git/minimal-api-gen/generator/`
- **FR-003**: System MUST rename AspnetMinimalapiGenerator.java to MinimalApiServerCodegen.java in generator/src/main/java/org/openapitools/codegen/languages/
- **FR-004**: System MUST change base class from DefaultCodegen to AbstractCSharpCodegen
- **FR-005**: System MUST implement constructor that configures outputFolder, embeddedTemplateDir, templateDir, modelTemplateFiles, and apiTemplateFiles
- **FR-006**: System MUST register 11 CLI options in constructor (useProblemDetails, useRecords, useAuthentication, useValidators, useResponseCaching, useApiVersioning, routePrefix, versioningPrefix, apiVersion, solutionGuid, projectConfigurationGuid)
- **FR-007**: System MUST override processOpts() to call all 11 setter methods and addSupportingFiles()
- **FR-008**: System MUST implement addSupportingFiles() to register all supporting templates (conditionally including authentication files)
- **FR-009**: System MUST override processOperation() to convert HTTP methods to PascalCase
- **FR-010**: System MUST implement all 11 setter methods to read CLI options and store in additionalProperties
- **FR-011**: System MUST copy all 17 templates from ~/scratch/git/openapi-generator3/.../aspnet-fastendpoints/ to generator/src/main/resources/aspnet-minimalapi/
- **FR-012**: System MUST include 4 static files (gitignore, appsettings.json, appsettings.Development.json, Properties/launchSettings.json) in generator/src/main/resources/aspnet-minimalapi/
- **FR-013**: System MUST override getName() to return "aspnetcore-minimalapi"
- **FR-014**: System MUST override getHelp() to return descriptive help text
- **FR-015**: System MUST override getTag() to return CodegenType.SERVER
- **FR-016**: System MUST update META-INF/services/org.openapitools.codegen.CodegenConfig with fully qualified class name org.openapitools.codegen.languages.MinimalApiServerCodegen
- **FR-017**: System MUST build successfully using devbox run mvn clean package from generator/ directory
- **FR-018**: Generated JAR MUST be executable: java -jar generator/target/openapi-generator-minimalapi-1.0.0.jar generate -g aspnetcore-minimalapi -i petstore.yaml -o /tmp/test-output
- **FR-019**: Generated code MUST produce a complete FastEndpoints-compatible project when run against any valid OpenAPI spec

### Key Entities

- **Generator Project (generator/)**: The custom OpenAPI Generator project in this repository; Key attributes: Maven-based structure copied from meta command output, contains src/ directory and pom.xml, isolated from upstream openapi-generator3 repository
- **Generator Class (MinimalApiServerCodegen)**: The main code generation class; Key attributes: extends AbstractCSharpCodegen, 222 lines (similar to AspnetFastendpointsServerCodegen), 15 methods (constructor, processOpts, addSupportingFiles, processOperation, 11 setters), template directory path "aspnet-minimalapi", located at generator/src/main/java/org/openapitools/codegen/languages/
- **Template Set**: Collection of 17 Mustache templates + 4 static files; Key attributes: 9 operation templates, 4 supporting templates, 4 model templates, resource directory location at generator/src/main/resources/aspnet-minimalapi/, copied from upstream aspnet-fastendpoints/ templates
- **CLI Options**: 11 configurable boolean/string options; Key attributes: useProblemDetails, useRecords, useAuthentication, useValidators, useResponseCaching, useApiVersioning, routePrefix, versioningPrefix, apiVersion, solutionGuid, projectConfigurationGuid
- **Service Registration**: META-INF configuration for generator discovery; Key attributes: fully qualified class name org.openapitools.codegen.languages.MinimalApiServerCodegen, service provider interface org.openapitools.codegen.CodegenConfig, file location at generator/src/main/resources/META-INF/services/
- **Generator JAR**: Built artifact for executing the generator; Key attributes: filename openapi-generator-minimalapi-1.0.0.jar, location generator/target/, executable via java -jar with generate subcommand

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Meta command executes successfully creating scaffolded project in /tmp/aspnet-minimalapi-gen/ with src/ directory and pom.xml
- **SC-002**: Scaffolded files successfully copied to ~/scratch/git/minimal-api-gen/generator/ with complete directory structure
- **SC-003**: Generator project builds successfully in under 2 minutes using devbox run mvn clean package from generator/ directory
- **SC-004**: MinimalApiServerCodegen class compiles with zero errors and extends AbstractCSharpCodegen
- **SC-005**: Class contains exactly 15 methods matching AspnetFastendpointsServerCodegen structure (constructor, processOpts, addSupportingFiles, processOperation, 11 setter methods)
- **SC-006**: All 17 templates copied to generator/src/main/resources/aspnet-minimalapi/ with 100% file coverage
- **SC-007**: Generator JAR is executable: java -jar generator/target/openapi-generator-minimalapi-1.0.0.jar list shows aspnetcore-minimalapi
- **SC-008**: Running generator against petstore.yaml produces complete FastEndpoints-compatible project in /tmp/test-output
- **SC-009**: Generated project structure includes expected directories and files (Program.cs, .csproj, .sln, Models/, Features/)
- **SC-010**: Generated .csproj file contains FastEndpoints NuGet package references (FastEndpoints, FastEndpoints.Swagger, conditionally FastEndpoints.Security)
- **SC-011**: Generated C# code compiles successfully with devbox run dotnet build from /tmp/test-output (proves templates are syntactically correct)

## Assumptions

- OpenAPI Generator repository is available at ~/scratch/git/openapi-generator3 with full source code and aspnet-fastendpoints templates
- OpenAPI Generator CLI JAR is built and available at ~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar
- Java (matching OpenAPI Generator requirements) and Maven are available through devbox
- The meta command generates a valid Maven project structure suitable for custom generator development
- The base class AbstractCSharpCodegen API is stable across OpenAPI Generator versions
- All 17 FastEndpoints templates from Feature 001 analysis are syntactically correct and complete
- AspnetFastendpointsServerCodegen serves as the reference implementation (222 lines, proven working pattern)
- Copying templates from aspnet-fastendpoints/ to aspnet-minimalapi/ requires no modifications for initial scaffolding
- Generated code will initially be FastEndpoints-compatible (Minimal API refactoring deferred to Feature 004)
- This project (~/scratch/git/minimal-api-gen/) will be the development and testing environment for the custom generator

## Out of Scope

- Modifying templates to Minimal API patterns (deferred to Feature 004 - TDD Refactoring)
- Writing test suites or "Golden Standard" validation (deferred to Feature 003 - Baseline Test Suite)
- Refactoring Java class logic beyond exact FastEndpoints replication
- Changing CLI option names or behavior from FastEndpoints pattern
- Performance optimization or code generation speed improvements
- Comprehensive documentation beyond inline code comments (deferred to Feature 005 - Finalization & Documentation)
- Integration with upstream OpenAPI Generator CI/CD pipeline or release process
- Publishing generator to Maven Central or other artifact repositories
- Modifying the upstream openapi-generator3 repository (all development happens in this project)
- Supporting OpenAPI 2.0 (Swagger) specs (focus on OpenAPI 3.x only)
- Creating wrapper scripts or tooling around the generator JAR (use java -jar directly)
