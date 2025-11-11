# Research: Generator Scaffolding via Inheritance

**Date**: 2025-11-11  
**Feature**: 002-generator-scaffolding  
**Phase**: 0 (Outline & Research)

## Research Questions

### RQ-001: How does OpenAPI Generator's `meta` command work?

**Decision**: Use `java -jar openapi-generator-cli.jar meta -n <name> -p <package> -o <output>` to scaffold a generator project

**Rationale**: 
- The `meta` command is OpenAPI Generator's official scaffolding tool for custom generators
- Generates a complete Maven project with proper dependencies, directory structure, and ServiceLoader registration
- Creates skeleton generator class extending DefaultCodegen with all necessary boilerplate
- Produces README with usage instructions and examples

**Alternatives Considered**:
- Manual project creation: Rejected (high risk of missing configuration, ServiceLoader registration errors)
- Copying existing generator: Rejected (includes unwanted code, harder to understand structure)

**Evidence**: OpenAPI Generator documentation recommends meta command for custom generators; FastEndpoints generator likely started from meta scaffold

**Implementation Notes**:
- Command: `java -jar ~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar meta -n aspnet-minimalapi -p org.openapitools.codegen -o /tmp/aspnet-minimalapi-gen`
- Generates `AspnetMinimalapiGenerator.java` (name derived from `-n` parameter)
- Creates `src/main/resources/aspnet-minimalapi/` directory for templates
- Produces `pom.xml` with openapi-generator dependencies
- Creates `src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig` for ServiceLoader

---

### RQ-002: Why extend AbstractCSharpCodegen instead of DefaultCodegen?

**Decision**: Generator MUST extend AbstractCSharpCodegen (not the meta-generated DefaultCodegen scaffold)

**Rationale**:
- AbstractCSharpCodegen provides C#-specific type mappings (string, int, DateTime, List<T>, etc.)
- Handles C# naming conventions (PascalCase for classes/properties, camelCase for parameters)
- Includes C# reserved words protection (e.g., `class` → `@class`)
- Provides C# file extension handling (.cs files)
- Offers C# namespace management and using statements
- AspnetFastendpointsServerCodegen extends AbstractCSharpCodegen (proven pattern from Feature 001 analysis)

**Alternatives Considered**:
- Extend DefaultCodegen (meta scaffold default): Rejected (requires reimplementing all C# language support)
- Extend AspNetCoreServerCodegen: Rejected (Feature 001 analysis showed FastEndpoints uses AbstractCSharpCodegen, not AspNetCoreServerCodegen)

**Evidence**: 
- Feature 001 method-override-map.md line 57: `public class AspnetFastendpointsServerCodegen extends AbstractCSharpCodegen`
- OpenAPI Generator source shows AbstractCSharpCodegen handles C# type system comprehensively

**Implementation Notes**:
- Modify meta-generated class: change `extends DefaultCodegen` to `extends AbstractCSharpCodegen`
- Update imports: `import org.openapitools.codegen.languages.AbstractCSharpCodegen;`
- May need to adjust constructor super() call to match AbstractCSharpCodegen's signature

---

### RQ-003: What are the critical files/directories to copy from meta output?

**Decision**: Copy `src/` directory and `pom.xml` from meta output to `~/scratch/git/minimal-api-gen/generator/`

**Rationale**:
- `src/main/java/`: Contains generator class skeleton and package structure
- `src/main/resources/`: Contains template directory and META-INF ServiceLoader registration
- `pom.xml`: Defines Maven project, openapi-generator dependencies, build plugins
- These three components are sufficient for a working generator project

**Alternatives Considered**:
- Copy entire meta output including README, .gitignore: Rejected (this project has its own README and .gitignore at root)
- Copy only Java source: Rejected (missing Maven build configuration and resource directories)

**Evidence**: Feature 001 analysis identified 17 templates in `src/main/resources/aspnet-fastendpoints/` and Java class in `src/main/java/`; pom.xml required for Maven build

**Implementation Notes**:
- Source: `/tmp/aspnet-minimalapi-gen/src/` and `/tmp/aspnet-minimalapi-gen/pom.xml`
- Destination: `~/scratch/git/minimal-api-gen/generator/src/` and `~/scratch/git/minimal-api-gen/generator/pom.xml`
- Use `cp -r` to preserve directory structure
- Verify META-INF/services file is present after copy

---

### RQ-004: How should the generator class be named and where should it live?

**Decision**: Rename `AspnetMinimalapiGenerator.java` to `MinimalApiServerCodegen.java` in `generator/src/main/java/org/openapitools/codegen/languages/`

**Rationale**:
- Naming convention: All OpenAPI Generator language generators use `*Codegen` suffix (e.g., AbstractCSharpCodegen, JavaClientCodegen)
- Server generators use `*ServerCodegen` pattern (e.g., AspnetFastendpointsServerCodegen from Feature 001)
- Package `org.openapitools.codegen.languages` is the standard location for language-specific generators
- Name must match the class registered in META-INF/services for ServiceLoader discovery

**Alternatives Considered**:
- Keep `AspnetMinimalapiGenerator`: Rejected (doesn't follow OpenAPI Generator naming conventions)
- Use `MinimalApiCodegen`: Rejected (missing "Server" designation for server-side generators)

**Evidence**: Feature 001 analysis shows AspnetFastendpointsServerCodegen in package `org.openapitools.codegen.languages`

**Implementation Notes**:
- Meta generates: `src/main/java/org/openapitools/codegen/AspnetMinimalapiGenerator.java`
- Move to: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
- Update class name in Java file: `public class MinimalApiServerCodegen extends AbstractCSharpCodegen`
- Update META-INF/services: `org.openapitools.codegen.languages.MinimalApiServerCodegen`

---

### RQ-005: What are the 15 methods to implement from Feature 001 analysis?

**Decision**: Implement constructor, processOpts(), addSupportingFiles(), processOperation(), and 11 setter methods identified in method-override-map.md

**Rationale**: Feature 001 analysis documented the complete API surface of AspnetFastendpointsServerCodegen (222 lines, 15 methods)

**Method Inventory**:
1. **Constructor** (lines 57-81): Sets outputFolder, templateDir, registers templates, adds CLI options
2. **processOpts()** (lines 82-102): Calls all setters, adds supporting files, logs warnings
3. **addSupportingFiles()** (lines 103-125): Registers 10 supporting files (program.mustache, csproj, sln, etc.), conditionally adds auth files
4. **processOperation()** (lines 126-132): Converts HTTP methods to PascalCase (get→Get, post→Post)
5. **setUseProblemDetails()** (lines 133-137): Reads useProblemDetails option
6. **setUseRecordForRequest()** (lines 138-142): Reads useRecords option
7. **setUseAuthentication()** (lines 143-147): Reads useAuthentication option
8. **setUseValidators()** (lines 148-152): Reads useValidators option
9. **setUseResponseCaching()** (lines 153-157): Reads useResponseCaching option
10. **setUseApiVersioning()** (lines 158-162): Reads useApiVersioning option
11. **setRoutePrefix()** (lines 163-167): Reads routePrefix string option
12. **setVersioningPrefix()** (lines 168-172): Reads versioningPrefix string option
13. **setApiVersion()** (lines 173-177): Reads apiVersion string option
14. **setSolutionGuid()** (lines 178-182): Reads solutionGuid string option
15. **setProjectConfigurationGuid()** (lines 183-187): Reads projectConfigurationGuid string option

**Implementation Notes**:
- Copy method signatures exactly from method-override-map.md
- Constructor must call `super()` for AbstractCSharpCodegen initialization
- All methods must store values in `additionalProperties` map for template access
- Reference Feature 001 method-override-map.md for exact implementation logic

---

### RQ-006: Which templates need to be copied and where do they come from?

**Decision**: Copy all 17 templates + 4 static files from `~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/` to `generator/src/main/resources/aspnet-minimalapi/`

**Rationale**: Feature 001 template-catalog.md documented complete inventory; all templates required for functional baseline

**Template Inventory** (from Feature 001 template-catalog.md):

**Operation Templates** (9 files):
1. endpoint.mustache - Main endpoint class template
2. request.mustache - Polymorphic request (class or record)
3. requestClass.mustache - Class-based request DTO
4. requestRecord.mustache - Record-based request DTO
5. endpointType.mustache - Generic endpoint without request
6. endpointRequestType.mustache - Generic endpoint with request
7. endpointResponseType.mustache - Generic endpoint with response
8. loginRequest.mustache - Authentication login request
9. userLoginEndpoint.mustache - Authentication login endpoint

**Supporting Templates** (4 files):
1. program.mustache - Program.cs entry point
2. project.csproj.mustache - Project file with NuGet packages
3. solution.mustache - .sln solution file
4. readme.mustache - README.md documentation

**Model Templates** (4 files):
1. model.mustache - Polymorphic model (class or record)
2. modelClass.mustache - Class-based model DTO
3. modelRecord.mustache - Record-based model DTO
4. enumClass.mustache - Enum type

**Static Files** (4 files):
1. gitignore - Git ignore patterns
2. appsettings.json - Application configuration
3. appsettings.Development.json - Development configuration
4. Properties/launchSettings.json - Debug launch profiles

**Implementation Notes**:
- Use `cp` command to preserve file structure
- Maintain Properties/ subdirectory for launchSettings.json
- Verify all 21 files present after copy (17 .mustache + 4 static)
- Constructor must set `templateDir = "aspnet-minimalapi"` to load these templates

---

### RQ-007: How do we validate the generator works?

**Decision**: Multi-stage validation: (1) Maven build, (2) JAR execution, (3) Code generation, (4) dotnet build

**Rationale**: Progressive validation ensures each layer works before testing next layer

**Validation Stages**:

**Stage 1: Maven Build**
- Command: `devbox run mvn clean package` from `generator/` directory
- Success: `target/openapi-generator-minimalapi-1.0.0.jar` created
- Validates: Java compilation, template bundling, ServiceLoader registration

**Stage 2: JAR Execution**
- Command: `java -jar generator/target/openapi-generator-minimalapi-1.0.0.jar list`
- Success: Output includes "aspnetcore-minimalapi" in available generators
- Validates: ServiceLoader discovery, getName() method returns correct value

**Stage 3: Code Generation**
- Command: `java -jar generator/target/openapi-generator-minimalapi-1.0.0.jar generate -g aspnetcore-minimalapi -i petstore.yaml -o /tmp/test-output`
- Success: Complete project structure created (Program.cs, .csproj, .sln, Models/, Features/)
- Validates: Template rendering, file generation, OpenAPI spec processing

**Stage 4: Generated Code Compilation**
- Command: `devbox run dotnet build` from `/tmp/test-output/`
- Success: Zero compilation errors
- Validates: Templates produce syntactically correct C# code, NuGet packages resolve, FastEndpoints compatibility

**Implementation Notes**:
- Use petstore.yaml as standard test spec (commonly available in OpenAPI Generator examples)
- Inspect /tmp/test-output/ for expected file structure (Models/, Features/ directories)
- Check .csproj for FastEndpoints NuGet packages
- Compilation failure indicates template syntax errors or missing using statements

---

### RQ-008: What build tool configuration is needed?

**Decision**: Use pom.xml from meta command with minimal modifications (update artifact name, version)

**Rationale**: Meta-generated pom.xml includes all necessary dependencies and plugins for OpenAPI Generator custom generators

**Key Dependencies** (from meta-generated pom.xml):
- `openapi-generator` (parent POM - provides dependency management)
- `openapi-generator-core` (code generation framework)
- `mustache-java` (template engine, transitive dependency)
- Maven plugins: `maven-compiler-plugin`, `maven-jar-plugin`, `maven-assembly-plugin`

**Required Modifications**:
- `<artifactId>`: Change from default to `openapi-generator-minimalapi`
- `<version>`: Set to `1.0.0` (initial release)
- `<name>`: Update to "OpenAPI Generator Minimal API"
- No changes to dependencies (all correct from meta scaffold)

**Implementation Notes**:
- Verify Java version compatibility (source/target 1.8 minimum)
- maven-assembly-plugin creates uber-JAR with dependencies
- maven-jar-plugin configures Main-Class for `java -jar` execution

---

## Summary of Decisions

All research questions resolved with actionable decisions:

1. **Scaffolding Method**: Use OpenAPI Generator `meta` command to generate Maven project structure
2. **Base Class**: Extend `AbstractCSharpCodegen` (not DefaultCodegen from meta scaffold)
3. **File Copying**: Copy `src/` and `pom.xml` from meta output to this project's `generator/` directory
4. **Class Naming**: Rename to `MinimalApiServerCodegen` in `org.openapitools.codegen.languages` package
5. **Method Implementation**: Implement 15 methods from Feature 001 method-override-map.md
6. **Template Copying**: Copy 17 templates + 4 static files from upstream aspnet-fastendpoints/ directory
7. **Validation Strategy**: 4-stage validation (Maven build → JAR execution → code generation → dotnet build)
8. **Build Configuration**: Use meta-generated pom.xml with minimal modifications

**Blockers**: None identified. All dependencies available (openapi-generator3 repository, devbox environment, Feature 001 analysis artifacts).

**Ready for Phase 1**: Design & Contracts (data-model.md, contracts/, quickstart.md).
