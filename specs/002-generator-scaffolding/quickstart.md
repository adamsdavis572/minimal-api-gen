# Quickstart: Generator Scaffolding via Inheritance

**Date**: 2025-11-11  
**Feature**: 002-generator-scaffolding  
**Phase**: 1 (Design & Contracts)

## Prerequisites

- ✅ Feature 001 complete (analysis artifacts available)
- ✅ OpenAPI Generator repository at `~/scratch/git/openapi-generator3`
- ✅ OpenAPI Generator CLI JAR built at `~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar`
- ✅ devbox environment available (for `devbox run mvn` and `devbox run dotnet`)
- ✅ Git branch `002-generator-scaffolding` checked out

## Step 1: Scaffold Generator Project

Run the OpenAPI Generator `meta` command to create a generator project scaffold:

```bash
cd ~/scratch/git/minimal-api-gen

# Execute meta command to scaffold generator
java -jar ~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar \
  meta \
  -n aspnet-minimalapi \
  -p org.openapitools.codegen \
  -o /tmp/aspnet-minimalapi-gen

# Verify scaffold created
ls -la /tmp/aspnet-minimalapi-gen
# Expected: src/, pom.xml, README.md, LICENSE
```

**Expected Output**: Complete Maven project structure in `/tmp/aspnet-minimalapi-gen/`

**Validation**: 
- [ ] `/tmp/aspnet-minimalapi-gen/src/main/java/org/openapitools/codegen/AspnetMinimalapiGenerator.java` exists
- [ ] `/tmp/aspnet-minimalapi-gen/src/main/resources/aspnet-minimalapi/` directory exists
- [ ] `/tmp/aspnet-minimalapi-gen/pom.xml` exists

---

## Step 2: Copy Scaffold to This Project

Copy the scaffolded structure to this project's `generator/` directory:

```bash
cd ~/scratch/git/minimal-api-gen

# Create generator directory
mkdir -p generator

# Copy source directory
cp -r /tmp/aspnet-minimalapi-gen/src generator/

# Copy pom.xml
cp /tmp/aspnet-minimalapi-gen/pom.xml generator/

# Verify copy succeeded
ls -la generator/
# Expected: src/, pom.xml

# Verify Java package structure
ls -la generator/src/main/java/org/openapitools/codegen/
# Expected: AspnetMinimalapiGenerator.java
```

**Validation**:
- [ ] `generator/src/` directory exists
- [ ] `generator/pom.xml` exists
- [ ] `generator/src/main/java/org/openapitools/codegen/AspnetMinimalapiGenerator.java` exists
- [ ] `generator/src/main/resources/aspnet-minimalapi/` directory exists
- [ ] `generator/src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig` exists

---

## Step 3: Rename and Move Generator Class

Rename the skeleton class to match OpenAPI Generator conventions:

```bash
cd ~/scratch/git/minimal-api-gen/generator

# Create languages package directory
mkdir -p src/main/java/org/openapitools/codegen/languages

# Move and rename generator class
mv src/main/java/org/openapitools/codegen/AspnetMinimalapiGenerator.java \
   src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java

# Verify move succeeded
ls -la src/main/java/org/openapitools/codegen/languages/
# Expected: MinimalApiServerCodegen.java
```

**Validation**:
- [ ] `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java` exists
- [ ] Old location (`AspnetMinimalapiGenerator.java`) no longer exists

---

## Step 4: Implement Generator Class

Replace the meta-generated skeleton with the full implementation based on Feature 001 analysis.

### 4a. Update Class Declaration

Edit `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`:

```java
package org.openapitools.codegen.languages;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractCSharpCodegen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;

public class MinimalApiServerCodegen extends AbstractCSharpCodegen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinimalApiServerCodegen.class);
    
    // CLI option fields
    private String useProblemDetails = "true";
    private String useRecords = "false";
    private String useAuthentication = "false";
    // ... (11 total fields from Feature 001 method-override-map.md)
    
    // Constructor implementation follows...
}
```

**Reference**: Feature 001 `specs/001-fastendpoints-analysis/method-override-map.md` lines 57-81

### 4b. Implement Constructor

Add constructor that configures paths, registers templates, and adds CLI options:

```java
public MinimalApiServerCodegen() {
    super();
    
    outputFolder = "generated-code/aspnet-minimalapi";
    embeddedTemplateDir = templateDir = "aspnet-minimalapi";
    
    // Register templates
    modelTemplateFiles.put("model.mustache", ".cs");
    apiTemplateFiles.put("endpoint.mustache", ".cs");
    
    // Add CLI options (11 options from Feature 001)
    addOption("useProblemDetails", "Use RFC 7807 ProblemDetails for errors", useProblemDetails);
    addOption("useRecords", "Use C# records for request DTOs", useRecords);
    // ... (11 total options)
}
```

**Reference**: Feature 001 `specs/001-fastendpoints-analysis/method-override-map.md` lines 57-81

### 4c. Implement Processing Methods

Add the 3 core processing methods:

```java
@Override
public void processOpts() {
    super.processOpts();
    
    // Call all 11 setters
    setUseProblemDetails(useProblemDetails);
    setUseRecordForRequest(useRecords);
    // ... (11 total setter calls)
    
    addSupportingFiles();
}

@Override
public void addSupportingFiles() {
    supportingFiles.add(new SupportingFile("program.mustache", "", "Program.cs"));
    supportingFiles.add(new SupportingFile("project.csproj.mustache", "", packageName + ".csproj"));
    // ... (10 total supporting files, conditional auth files)
}

@Override
public void processOperation(CodegenOperation operation) {
    super.processOperation(operation);
    
    if (operation.httpMethod != null) {
        operation.httpMethod = operation.httpMethod.substring(0, 1).toUpperCase(Locale.ROOT) 
            + operation.httpMethod.substring(1).toLowerCase(Locale.ROOT);
    }
}
```

**Reference**: Feature 001 `specs/001-fastendpoints-analysis/method-override-map.md` lines 82-132

### 4d. Implement Setter Methods

Add all 11 setter methods:

```java
public void setUseProblemDetails(String useProblemDetails) {
    this.useProblemDetails = useProblemDetails;
    additionalProperties.put("useProblemDetails", Boolean.parseBoolean(useProblemDetails));
}

public void setUseRecordForRequest(String useRecords) {
    this.useRecords = useRecords;
    additionalProperties.put("useRecords", Boolean.parseBoolean(useRecords));
}

// ... (11 total setter methods)
```

**Reference**: Feature 001 `specs/001-fastendpoints-analysis/method-override-map.md` lines 133-187

### 4e. Implement Metadata Methods

Add getName(), getHelp(), getTag() overrides:

```java
@Override
public String getName() {
    return "aspnetcore-minimalapi";
}

@Override
public String getHelp() {
    return "Generates an ASP.NET Core Minimal API server.";
}

@Override
public CodegenType getTag() {
    return CodegenType.SERVER;
}
```

**Validation**:
- [ ] Class extends `AbstractCSharpCodegen` (not `DefaultCodegen`)
- [ ] All 15 methods implemented
- [ ] All 11 CLI options registered in constructor
- [ ] templateDir set to "aspnet-minimalapi"

---

## Step 5: Update Service Registration

Edit `generator/src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig`:

```text
org.openapitools.codegen.languages.MinimalApiServerCodegen
```

**Validation**:
- [ ] File contains fully qualified class name
- [ ] Class name matches renamed Java file

---

## Step 6: Copy Templates from Upstream

Copy all 17 templates and 4 static files from the upstream FastEndpoints generator:

```bash
cd ~/scratch/git/minimal-api-gen

# Copy all mustache templates
cp ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/*.mustache \
   generator/src/main/resources/aspnet-minimalapi/

# Copy static files
cp ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/gitignore \
   generator/src/main/resources/aspnet-minimalapi/

cp ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/appsettings.json \
   generator/src/main/resources/aspnet-minimalapi/

cp ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/appsettings.Development.json \
   generator/src/main/resources/aspnet-minimalapi/

# Copy Properties directory
mkdir -p generator/src/main/resources/aspnet-minimalapi/Properties
cp ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/Properties/launchSettings.json \
   generator/src/main/resources/aspnet-minimalapi/Properties/

# Verify template count
ls generator/src/main/resources/aspnet-minimalapi/*.mustache | wc -l
# Expected: 17

# Verify all files present
ls -la generator/src/main/resources/aspnet-minimalapi/
```

**Expected Templates** (17 .mustache files from Feature 001 template-catalog.md):
- endpoint.mustache, request.mustache, requestClass.mustache, requestRecord.mustache
- endpointType.mustache, endpointRequestType.mustache, endpointResponseType.mustache
- loginRequest.mustache, userLoginEndpoint.mustache
- program.mustache, project.csproj.mustache, solution.mustache, readme.mustache
- model.mustache, modelClass.mustache, modelRecord.mustache, enumClass.mustache

**Expected Static Files** (4 files):
- gitignore, appsettings.json, appsettings.Development.json, Properties/launchSettings.json

**Validation**:
- [ ] 17 .mustache files in generator/src/main/resources/aspnet-minimalapi/
- [ ] 4 static files present
- [ ] Properties/ subdirectory exists with launchSettings.json

---

## Step 7: Build Generator

Build the generator JAR using Maven via devbox:

```bash
cd ~/scratch/git/minimal-api-gen/generator

# Clean and build (MUST use devbox wrapper)
devbox run mvn clean package

# Verify JAR created
ls -la target/
# Expected: aspnet-minimalapi-openapi-generator-1.0.0.jar
```

**Expected Output**: BUILD SUCCESS in <2 minutes

**⚠️ Important**: Always use `devbox run mvn` instead of direct `mvn` commands

**Validation**:
- [ ] Build completes without errors
- [ ] `target/aspnet-minimalapi-openapi-generator-1.0.0.jar` exists
- [ ] JAR size ~20KB (custom generator artifact)

---

## Step 8: Verify Generator Discovery

Test that the generator is discoverable by using classpath with OpenAPI Generator CLI:

```bash
cd ~/scratch/git/minimal-api-gen/generator

# List available generators using classpath
java -cp "target/aspnet-minimalapi-openapi-generator-1.0.0.jar:~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar" \
  org.openapitools.codegen.OpenAPIGenerator list

# Expected output should include:
# CLIENT generators:
# ...
# SERVER generators:
# ...
#   - aspnetcore-minimalapi
# ...
```

**Note**: Custom generators require classpath inclusion with OpenAPI Generator CLI JAR

**Validation**:
- [ ] Command executes without errors
- [ ] Output includes "aspnetcore-minimalapi" in SERVER generators section

---

## Step 9: Generate Test Project

Generate a complete ASP.NET Core project from a sample OpenAPI spec:

```bash
cd ~/scratch/git/minimal-api-gen/generator

# Generate from petstore.yaml using classpath
java -cp "target/aspnet-minimalapi-openapi-generator-1.0.0.jar:~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar" \
  org.openapitools.codegen.OpenAPIGenerator generate \
  -g aspnetcore-minimalapi \
  -i ~/scratch/git/openapi-generator3/modules/openapi-generator/src/test/resources/3_0/petstore.yaml \
  -o /tmp/test-minimalapi \
  --additional-properties=packageName=PetstoreApi

# Verify generated structure
ls -la /tmp/test-minimalapi/
# Expected: Program.cs, PetstoreApi.csproj, PetstoreApi.sln, Models/, Features/
```

**Expected Files**:
- Program.cs (entry point)
- PetstoreApi.csproj (project file with FastEndpoints NuGet packages)
- PetstoreApi.sln (solution file)
- Models/ directory (Pet.cs, Category.cs, Tag.cs, etc.)
- Features/ directory (PetEndpoints.cs, etc.)
- appsettings.json, appsettings.Development.json
- .gitignore
- README.md

**Validation**:
- [ ] All expected files generated
- [ ] .csproj contains FastEndpoints NuGet packages
- [ ] Models/ contains C# DTO classes
- [ ] Features/ contains endpoint classes

---

## Step 10: Validate Generated Code

Compile the generated project to ensure templates produce valid C#:

```bash
# Build generated project (MUST use devbox wrapper)
cd /tmp/test-minimalapi && devbox run dotnet build

# Expected output: Build succeeded. 0 Warning(s), 0 Error(s)
```

**⚠️ Important**: Always use `devbox run dotnet` instead of direct `dotnet` commands

**Validation**:
- [ ] Build completes without errors
- [ ] bin/ directory created at /tmp/test-minimalapi/src/PetstoreApi/bin/
- [ ] No compiler warnings or errors

---

## Success Criteria Checklist

Phase completion validated when all criteria met:

- [ ] **SC-001**: Meta command executed successfully
- [ ] **SC-002**: Files copied to generator/ directory
- [ ] **SC-003**: Maven build completes in <2 minutes
- [ ] **SC-004**: MinimalApiServerCodegen compiles, extends AbstractCSharpCodegen
- [ ] **SC-005**: Class contains exactly 15 methods
- [ ] **SC-006**: All 17 templates + 4 static files copied
- [ ] **SC-007**: `java -jar ... list` shows aspnetcore-minimalapi
- [ ] **SC-008**: Generator produces complete project from petstore.yaml
- [ ] **SC-009**: Generated project has Program.cs, .csproj, .sln, Models/, Features/
- [ ] **SC-010**: .csproj contains FastEndpoints NuGet packages
- [ ] **SC-011**: Generated code compiles with dotnet build

---

## Troubleshooting

### Build Fails: "Cannot find symbol AbstractCSharpCodegen"

**Cause**: Import statement incorrect or base class not in classpath

**Fix**: Verify import in MinimalApiServerCodegen.java:
```java
import org.openapitools.codegen.languages.AbstractCSharpCodegen;
```

Check pom.xml includes openapi-generator-core dependency.

---

### Generator Not Listed

**Cause**: ServiceLoader registration incorrect or class name mismatch

**Fix**: Verify META-INF/services/org.openapitools.codegen.CodegenConfig contains:
```
org.openapitools.codegen.languages.MinimalApiServerCodegen
```

Ensure class name matches exactly (case-sensitive).

---

### Code Generation Fails: "Template not found"

**Cause**: Templates not bundled in JAR or templateDir incorrect

**Fix**: Verify templates in `generator/src/main/resources/aspnet-minimalapi/`

Rebuild: `devbox run mvn clean package`

Check JAR contents: `jar tf target/openapi-generator-minimalapi-1.0.0.jar | grep mustache`

---

### Generated Code Doesn't Compile

**Cause**: Template syntax errors or missing using statements

**Fix**: Check generated .cs files for syntax errors

Compare with FastEndpoints generator output

Verify templates copied correctly from upstream

Review Feature 001 template-catalog.md for template structure

---

## Next Steps

After completing this quickstart:

1. **Commit work**: Stage generator/ directory and commit to 002-generator-scaffolding branch
2. **Feature 003**: Create baseline test suite validating FastEndpoints output
3. **Feature 004**: Refactor templates from FastEndpoints to Minimal API patterns
4. **Feature 005**: Finalize documentation and polish

**Branch Management**:
```bash
git add generator/
git add specs/002-generator-scaffolding/
git commit -m "feat(002): complete generator scaffolding with FastEndpoints baseline"
git push origin 002-generator-scaffolding
```
