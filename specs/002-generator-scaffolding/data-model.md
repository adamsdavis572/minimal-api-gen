# Data Model: Generator Scaffolding via Inheritance

**Date**: 2025-11-11  
**Feature**: 002-generator-scaffolding  
**Phase**: 1 (Design & Contracts)

## Core Entities

### Entity 1: Generator Project

**Purpose**: Maven-based OpenAPI Generator custom generator project structure

**Fields**:
- `rootPath`: string (absolute path to generator/ directory)
- `pomXml`: PomConfiguration (Maven project configuration)
- `sourceDirectory`: string (src/main/java/ path)
- `resourceDirectory`: string (src/main/resources/ path)
- `targetDirectory`: string (target/ path for build outputs)

**Relationships**:
- Contains exactly 1 GeneratorClass
- Contains exactly 1 TemplateSet
- Contains exactly 1 ServiceRegistration
- Produces 1 GeneratorJAR when built

**State Transitions**:
```
[Scaffolded] --copy--> [Copied] --build--> [Compiled] --package--> [Built]
```

**Validation Rules**:
- `rootPath` must exist and contain pom.xml
- `sourceDirectory` must contain valid Java package structure
- `resourceDirectory` must contain META-INF/services/ directory
- Build must complete in <2 minutes (SC-003)

---

### Entity 2: Generator Class

**Purpose**: Java class implementing custom code generation logic

**Fields**:
- `className`: string = "MinimalApiServerCodegen"
- `packageName`: string = "org.openapitools.codegen.languages"
- `baseClass`: string = "AbstractCSharpCodegen"
- `methods`: List<MethodSignature> (15 methods from Feature 001)
- `cliOptions`: List<CLIOption> (11 configurable options)
- `templateDir`: string = "aspnet-minimalapi"
- `generatorName`: string = "aspnetcore-minimalapi"

**Relationships**:
- Extends AbstractCSharpCodegen (inheritance)
- Registers TemplateSet via templateDir
- Exposes CLIOptions for user configuration
- Registered in ServiceRegistration

**State Transitions**:
```
[Skeleton] --rename--> [Renamed] --extend--> [Inherited] --implement--> [Complete]
```

**Validation Rules**:
- Must extend AbstractCSharpCodegen (not DefaultCodegen)
- Must implement all 15 methods from Feature 001 analysis
- getName() must return "aspnetcore-minimalapi"
- Must compile without errors (SC-004)
- Must contain exactly 15 methods (SC-005)

---

### Entity 3: MethodSignature

**Purpose**: Java method override from AspnetFastendpointsServerCodegen

**Fields**:
- `name`: string (method name)
- `returnType`: string (Java return type)
- `parameters`: List<Parameter> (method parameters)
- `visibility`: string (public/protected/private)
- `implementation`: string (method body code)
- `sourceLineRange`: tuple<int, int> (line numbers from Feature 001 analysis)

**Method Inventory** (from Feature 001 method-override-map.md):

**Constructor** (lines 57-81):
```java
public MinimalApiServerCodegen() {
  super();
  outputFolder = "generated-code/aspnet-minimalapi";
  embeddedTemplateDir = templateDir = "aspnet-minimalapi";
  // Register templates and CLI options
}
```

**Processing Methods**:
- `processOpts()`: void (lines 82-102) - Orchestrates option processing
- `addSupportingFiles()`: void (lines 103-125) - Registers supporting templates
- `processOperation(CodegenOperation)`: void (lines 126-132) - Formats HTTP methods

**Setter Methods** (11 methods):
- `setUseProblemDetails(String)`: void (lines 133-137)
- `setUseRecordForRequest(String)`: void (lines 138-142)
- `setUseAuthentication(String)`: void (lines 143-147)
- `setUseValidators(String)`: void (lines 148-152)
- `setUseResponseCaching(String)`: void (lines 153-157)
- `setUseApiVersioning(String)`: void (lines 158-162)
- `setRoutePrefix(String)`: void (lines 163-167)
- `setVersioningPrefix(String)`: void (lines 168-172)
- `setApiVersion(String)`: void (lines 173-177)
- `setSolutionGuid(String)`: void (lines 178-182)
- `setProjectConfigurationGuid(String)`: void (lines 183-187)

**Relationships**:
- Belongs to GeneratorClass
- Reads from CLIOptions
- Registers templates from TemplateSet

**Validation Rules**:
- Signature must match AbstractCSharpCodegen inheritance requirements
- Implementation must store values in additionalProperties map
- Constructor must call super() first
- All setters must be called from processOpts()

---

### Entity 4: CLI Option

**Purpose**: User-configurable command-line option for generator behavior

**Fields**:
- `name`: string (option name, e.g., "useProblemDetails")
- `type`: string (boolean|string)
- `defaultValue`: string (default value if not provided)
- `description`: string (help text)
- `setterMethod`: string (corresponding setter method name)

**Option Inventory** (from Feature 001 method-override-map.md):

**Boolean Options**:
1. `useProblemDetails`: "Use RFC 7807 ProblemDetails for errors" (default: true)
2. `useRecords`: "Use C# records for request DTOs" (default: false)
3. `useAuthentication`: "Generate authentication endpoints" (default: false)
4. `useValidators`: "Generate FluentValidation validators" (default: true)
5. `useResponseCaching`: "Enable response caching middleware" (default: false)
6. `useApiVersioning`: "Enable API versioning" (default: false)

**String Options**:
7. `routePrefix`: "API route prefix" (default: "api")
8. `versioningPrefix`: "API versioning prefix" (default: "v")
9. `apiVersion`: "API version number" (default: "1")
10. `solutionGuid`: "Solution GUID" (default: auto-generated)
11. `projectConfigurationGuid`: "Project configuration GUID" (default: auto-generated)

**Relationships**:
- Registered in GeneratorClass constructor
- Read by corresponding setter method in MethodSignature
- Values passed to TemplateSet via additionalProperties

**Validation Rules**:
- Boolean options must accept "true"|"false" strings
- String options must not be null
- All options must be registered in constructor (FR-006)
- Must have corresponding setter method

---

### Entity 5: Template Set

**Purpose**: Collection of Mustache templates for code generation

**Fields**:
- `templateDirectory`: string = "aspnet-minimalapi"
- `operationTemplates`: List<Template> (9 templates)
- `supportingTemplates`: List<Template> (4 templates)
- `modelTemplates`: List<Template> (4 templates)
- `staticFiles`: List<StaticFile> (4 files)

**Template Inventory** (from Feature 001 template-catalog.md):

**Operation Templates** (generate endpoint code):
1. `endpoint.mustache` - Main endpoint class
2. `request.mustache` - Polymorphic request DTO
3. `requestClass.mustache` - Class-based request
4. `requestRecord.mustache` - Record-based request
5. `endpointType.mustache` - Generic endpoint (no request)
6. `endpointRequestType.mustache` - Generic endpoint (with request)
7. `endpointResponseType.mustache` - Generic endpoint (with response)
8. `loginRequest.mustache` - Login request DTO
9. `userLoginEndpoint.mustache` - Login endpoint

**Supporting Templates** (generate project infrastructure):
1. `program.mustache` → Program.cs
2. `project.csproj.mustache` → [ProjectName].csproj
3. `solution.mustache` → [SolutionName].sln
4. `readme.mustache` → README.md

**Model Templates** (generate data models):
1. `model.mustache` - Polymorphic model
2. `modelClass.mustache` - Class-based model
3. `modelRecord.mustache` - Record-based model
4. `enumClass.mustache` - Enum type

**Static Files** (copied without template processing):
1. `.gitignore` - Git ignore patterns
2. `appsettings.json` - App configuration
3. `appsettings.Development.json` - Dev configuration
4. `Properties/launchSettings.json` - Launch profiles

**Relationships**:
- Bundled in GeneratorProject resources
- Loaded by GeneratorClass via templateDir
- Rendered using values from CLIOptions
- Sourced from upstream aspnet-fastendpoints/ directory

**Validation Rules**:
- All 21 files must be present (17 templates + 4 static)
- Must be in generator/src/main/resources/aspnet-minimalapi/
- Must bundle in JAR during Maven build (SC-006)
- Templates must be syntactically valid Mustache

---

### Entity 6: Service Registration

**Purpose**: ServiceLoader registration for generator discovery

**Fields**:
- `serviceFile`: string = "META-INF/services/org.openapitools.codegen.CodegenConfig"
- `implementation`: string = "org.openapitools.codegen.languages.MinimalApiServerCodegen"

**Relationships**:
- References GeneratorClass fully qualified name
- Required by OpenAPI Generator framework for discovery
- Bundled in GeneratorProject resources

**Validation Rules**:
- File must exist at src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig
- Must contain fully qualified class name
- Class name must match GeneratorClass
- Must be included in JAR (validated by `java -jar ... list` command)

---

### Entity 7: Generator JAR

**Purpose**: Executable JAR artifact containing compiled generator

**Fields**:
- `fileName`: string = "openapi-generator-minimalapi-1.0.0.jar"
- `location`: string = "generator/target/"
- `mainClass`: string = "org.openapitools.codegen.OpenAPIGenerator"
- `bundledTemplates`: TemplateSet
- `bundledClasses`: List<CompiledClass>

**Relationships**:
- Produced by GeneratorProject build
- Contains GeneratorClass (compiled)
- Contains TemplateSet (bundled)
- Contains ServiceRegistration (bundled)
- Executable via `java -jar` command

**State Transitions**:
```
[Building] --compile--> [Compiled] --package--> [Packaged] --verify--> [Validated]
```

**Validation Rules**:
- Must be executable: `java -jar generator/target/openapi-generator-minimalapi-1.0.0.jar list` succeeds (SC-007)
- Must show "aspnetcore-minimalapi" in list output
- Must accept `generate` subcommand with `-g aspnetcore-minimalapi` flag
- Must produce complete project when run against petstore.yaml (SC-008)

---

### Entity 8: Generated Project

**Purpose**: Output of generator execution (ASP.NET Core project)

**Fields**:
- `rootPath`: string (output directory path)
- `programFile`: string (Program.cs entry point)
- `projectFile`: string (.csproj project file)
- `solutionFile`: string (.sln solution file)
- `modelsDirectory`: string (Models/ directory)
- `featuresDirectory`: string (Features/ directory)

**Relationships**:
- Produced by GeneratorJAR execution
- Generated from TemplateSet
- Configured by CLIOptions
- Based on input OpenAPI specification

**Validation Rules**:
- Must contain Program.cs (SC-009)
- Must contain .csproj with FastEndpoints NuGet packages (SC-010)
- Must contain .sln solution file
- Must contain Models/ directory with DTO classes
- Must contain Features/ directory with endpoint classes
- Must compile with dotnet build (SC-011)

---

## Entity Relationships

```
GeneratorProject
├── contains: GeneratorClass
│   ├── implements: MethodSignature (15 methods)
│   ├── exposes: CLIOption (11 options)
│   └── loads: TemplateSet (via templateDir)
├── contains: TemplateSet
│   ├── operationTemplates (9 files)
│   ├── supportingTemplates (4 files)
│   ├── modelTemplates (4 files)
│   └── staticFiles (4 files)
├── contains: ServiceRegistration
│   └── references: GeneratorClass (fully qualified name)
└── produces: GeneratorJAR (when built)
    └── generates: GeneratedProject (when executed)
        ├── uses: TemplateSet (for rendering)
        └── configured by: CLIOption (from command line)
```

## Data Flow

1. **Scaffolding Flow**:
   ```
   meta command → [GeneratorProject scaffold] → copy to this repo → [GeneratorProject in generator/]
   ```

2. **Build Flow**:
   ```
   GeneratorProject → Maven compile → GeneratorClass bytecode → Maven package → GeneratorJAR
   ```

3. **Execution Flow**:
   ```
   OpenAPI Spec + CLIOptions → GeneratorJAR → GeneratorClass.processOpts() → TemplateSet rendering → GeneratedProject
   ```

4. **Validation Flow**:
   ```
   GeneratorJAR → execute list command → verify aspnetcore-minimalapi present
   GeneratorJAR → execute generate command → GeneratedProject → dotnet build → verify compilation
   ```

## Invariants

1. **Structure Invariant**: GeneratorProject MUST have src/, resources/, pom.xml
2. **Method Invariant**: GeneratorClass MUST have exactly 15 methods (no more, no less)
3. **Template Invariant**: TemplateSet MUST have exactly 21 files (17 templates + 4 static)
4. **Registration Invariant**: ServiceRegistration MUST reference GeneratorClass by fully qualified name
5. **Build Invariant**: Maven build MUST complete in <2 minutes
6. **Generation Invariant**: GeneratedProject MUST compile with dotnet build (proves template validity)

## Implementation Notes

- All paths use Unix-style separators (/) for cross-platform compatibility
- GUIDs for solution/project are typically auto-generated UUIDs
- additionalProperties map is the bridge between Java (GeneratorClass) and Mustache (TemplateSet)
- Constructor order: super() → set fields → register options → register templates
- processOpts() order: call setters → validate → add supporting files
- Template variables come from OpenAPI spec (operations, models) + additionalProperties (CLI options)
