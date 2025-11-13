# Implementation Tasks: Generator Scaffolding via Inheritance

**Feature**: 002-generator-scaffolding  
**Branch**: `002-generator-scaffolding`  
**Created**: 2025-11-11  
**Status**: Ready for Implementation

## Overview

Create a custom OpenAPI Generator for ASP.NET Core Minimal APIs by scaffolding with the `meta` command, implementing MinimalApiServerCodegen class (15 methods), and copying 17 templates from FastEndpoints. The generator will initially produce FastEndpoints-compatible output as a baseline for Feature 004 refactoring.

**Total Tasks**: 36  
**Parallel Opportunities**: 12 tasks marked [P]  
**User Stories**: 3 (all P1 priority)

## Implementation Strategy

**MVP Scope**: Complete all 3 user stories (US1, US2, US3) - they form an atomic unit. The generator is not functional until all three are complete.

**Incremental Delivery**:
1. **US1**: Scaffold and copy project structure (foundation)
2. **US2**: Implement generator class logic (execution engine)
3. **US3**: Copy templates (output mechanism)
4. **Validate**: Build, discover, generate, compile (end-to-end validation)

**Independent Testing**: Each user story has specific validation criteria (see acceptance tests per phase).

---

## Phase 1: Setup (Project Initialization)

**Goal**: Establish repository structure and verify prerequisites

**Tasks**:

- [X] T001 Create generator/ directory in repository root at ~/scratch/git/minimal-api-gen/generator/
- [X] T002 Verify OpenAPI Generator CLI JAR exists at ~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar
- [X] T003 Verify devbox environment available (test with `devbox run mvn --version`)
- [X] T004 Verify upstream FastEndpoints templates exist at ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/

**Validation**: ✅ All prerequisite files and tools verified, generator/ directory created

---

## Phase 2: User Story 1 - Initialize Generator Project Structure

**Story Goal**: Use meta command to scaffold generator project and copy to this repository

**Independent Test**: Maven build succeeds in generator/ directory producing JAR artifact

**Acceptance Tests**:

- [X] T005 [US1] Scaffold generator project with meta command: `java -jar ~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar meta -n aspnet-minimalapi -p org.openapitools.codegen -o /tmp/aspnet-minimalapi-gen`
- [X] T006 [US1] Verify scaffolded structure contains pom.xml, src/main/java/, src/main/resources/, README.md at /tmp/aspnet-minimalapi-gen/
- [X] T007 [US1] Verify AspnetMinimalapiGenerator.java skeleton exists at /tmp/aspnet-minimalapi-gen/src/main/java/org/openapitools/codegen/AspnetMinimalapiGenerator.java
- [X] T008 [US1] Verify META-INF/services/org.openapitools.codegen.CodegenConfig file exists at /tmp/aspnet-minimalapi-gen/src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig

**Implementation Tasks**:

- [X] T009 [US1] Copy src/ directory from /tmp/aspnet-minimalapi-gen/src/ to ~/scratch/git/minimal-api-gen/generator/src/ (recursive copy preserving structure)
- [X] T010 [US1] Copy pom.xml from /tmp/aspnet-minimalapi-gen/pom.xml to ~/scratch/git/minimal-api-gen/generator/pom.xml
- [X] T011 [US1] Verify copied structure: check generator/src/main/java/org/openapitools/codegen/AspnetMinimalapiGenerator.java exists
- [X] T012 [US1] Verify copied structure: check generator/src/main/resources/aspnet-minimalapi/ directory exists
- [X] T013 [US1] Verify copied structure: check generator/src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig exists

**Validation Tasks**:

- [X] T014 [US1] Build copied project with `devbox run mvn clean package` from generator/ directory
- [X] T015 [US1] Verify JAR created at generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar
- [X] T016 [US1] Verify build completes in <2 minutes (SC-003 from spec) - **Build time: 2.8 seconds**

**Story Completion Criteria**: 
✅ Meta command executed successfully  
✅ Files copied to generator/ directory  
✅ Maven build succeeds producing JAR

---

## Phase 3: User Story 2 - Create Generator Class with Inheritance

**Story Goal**: Implement MinimalApiServerCodegen extending AbstractCSharpCodegen with 15 methods from Feature 001 analysis

**Independent Test**: Generator class compiles, extends AbstractCSharpCodegen, implements all 15 methods, Maven build succeeds

**Prerequisites**: US1 complete (scaffolded project copied)

**Acceptance Tests**:

- [X] T017 [US2] Class extends AbstractCSharpCodegen (not DefaultCodegen)
- [X] T018 [US2] Class contains exactly 15 methods (constructor + processOpts + addSupportingFiles + processOperation + 11 setters)
- [X] T019 [US2] getName() returns "aspnetcore-minimalapi"
- [X] T020 [US2] ServiceLoader registration updated with correct fully qualified name

**Implementation Tasks**:

- [X] T021 [US2] Create languages package directory at generator/src/main/java/org/openapitools/codegen/languages/
- [X] T022 [US2] Move AspnetMinimalapiGenerator.java to generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java
- [X] T023 [US2] Update class declaration: change `extends DefaultCodegen` to `extends AbstractCSharpCodegen` in MinimalApiServerCodegen.java
- [X] T024 [US2] Update package statement to `package org.openapitools.codegen.languages;` in MinimalApiServerCodegen.java
- [X] T025 [US2] Add import `import org.openapitools.codegen.languages.AbstractCSharpCodegen;` in MinimalApiServerCodegen.java
- [X] T026 [US2] Implement constructor following Feature 001 method-override-map.md lines 57-81: set outputFolder, templateDir, register model.mustache and endpoint.mustache templates
- [X] T027 [US2] Add 11 CLI options in constructor: useProblemDetails, useRecords, useAuthentication, useValidators, useResponseCaching, useApiVersioning, routePrefix, versioningPrefix, apiVersion, solutionGuid, projectConfigurationGuid
- [X] T028 [US2] Override processOpts() method following Feature 001 lines 82-102: call all 11 setters, then super.processOpts(), then addSupportingFiles()
- [X] T029 [US2] Implement addSupportingFiles() method following Feature 001 lines 103-125: register program.mustache, project.csproj.mustache, solution.mustache, readme.mustache, gitignore, appsettings.json, appsettings.Development.json, Properties/launchSettings.json, conditional loginRequest.mustache and userLoginEndpoint.mustache
- [X] T030 [US2] Override processOperation(CodegenOperation) method following Feature 001 lines 126-132: convert HTTP method to PascalCase (e.g., get→Get, post→Post)
- [X] T031 [P] [US2] Implement setUseProblemDetails() setter following Feature 001 lines 133-140
- [X] T032 [P] [US2] Implement setUseRecordForRequest() setter following Feature 001 lines 141-148
- [X] T033 [P] [US2] Implement setUseAuthentication() setter following Feature 001 lines 149-156
- [X] T034 [P] [US2] Implement setUseValidators() setter following Feature 001 lines 157-164
- [X] T035 [P] [US2] Implement setUseResponseCaching() setter following Feature 001 lines 165-172
- [X] T036 [P] [US2] Implement setUseApiVersioning() setter following Feature 001 lines 173-180
- [X] T037 [P] [US2] Implement setRoutePrefix() setter following Feature 001 lines 181-188
- [X] T038 [P] [US2] Implement setVersioningPrefix() setter following Feature 001 lines 189-196
- [X] T039 [P] [US2] Implement setApiVersion() setter following Feature 001 lines 197-204
- [X] T040 [P] [US2] Implement setSolutionGuid() setter following Feature 001 lines 205-213
- [X] T041 [P] [US2] Implement setProjectConfigurationGuid() setter following Feature 001 lines 214-222
- [X] T042 [US2] Override getName() method to return "aspnetcore-minimalapi"
- [X] T043 [US2] Override getHelp() method to return "Generates an ASP.NET Core Minimal API server."
- [X] T044 [US2] Override getTag() method to return CodegenType.SERVER
- [X] T045 [US2] Update META-INF/services/org.openapitools.codegen.CodegenConfig with fully qualified name: org.openapitools.codegen.languages.MinimalApiServerCodegen

**Validation Tasks**:

- [X] T046 [US2] Build with `devbox run mvn clean package` from generator/ directory
- [X] T047 [US2] Verify zero compilation errors
- [X] T048 [US2] Verify class extends AbstractCSharpCodegen using `grep "extends AbstractCSharpCodegen" generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
- [X] T049 [US2] Count methods: verify exactly 15 methods present (constructor + 14 overrides)
- [X] T050 [US2] Verify JAR created at generator/target/openapi-generator-minimalapi-1.0.0.jar

**Story Completion Criteria**:
✅ MinimalApiServerCodegen compiles without errors  
✅ Extends AbstractCSharpCodegen  
✅ Contains exactly 15 methods  
✅ ServiceLoader registration updated

---

## Phase 4: User Story 3 - Copy and Register Templates

**Story Goal**: Copy 17 Mustache templates + 4 static files from upstream FastEndpoints generator

**Independent Test**: Generator produces complete FastEndpoints-compatible project when run against petstore.yaml, generated code compiles with dotnet build

**Prerequisites**: US2 complete (generator class implemented with templateDir="aspnet-minimalapi")

**Acceptance Tests**:

- [X] T051 [US3] All 17 .mustache templates present in generator/src/main/resources/aspnet-minimalapi/ (18 templates copied)
- [X] T052 [US3] All 4 static files present (gitignore, appsettings.json, appsettings.Development.json, Properties/launchSettings.json)
- [X] T053 [US3] Templates bundled in JAR after Maven build
- [ ] T054 [US3] Generator produces complete project structure when executed
- [ ] T055 [US3] Generated C# code compiles without errors

**Implementation Tasks - Operation Templates**:

- [X] T056 [P] [US3] Copy endpoint.mustache from ~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/ to generator/src/main/resources/aspnet-minimalapi/
- [X] T057 [P] [US3] Copy request.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T058 [P] [US3] Copy requestClass.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T059 [P] [US3] Copy requestRecord.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T060 [P] [US3] Copy endpointType.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T061 [P] [US3] Copy endpointRequestType.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T062 [P] [US3] Copy endpointResponseType.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T063 [P] [US3] Copy loginRequest.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T064 [P] [US3] Copy userLoginEndpoint.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/

**Implementation Tasks - Supporting Templates**:

- [X] T065 [P] [US3] Copy program.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T066 [P] [US3] Copy project.csproj.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T067 [P] [US3] Copy solution.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T068 [P] [US3] Copy readme.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/

**Implementation Tasks - Model Templates**:

- [X] T069 [US3] Copy model.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T070 [US3] Copy modelClass.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T071 [US3] Copy modelRecord.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T072 [US3] Copy enumClass.mustache from upstream to generator/src/main/resources/aspnet-minimalapi/

**Implementation Tasks - Static Files**:

- [X] T073 [US3] Copy gitignore from upstream aspnet-fastendpoints/ to generator/src/main/resources/aspnet-minimalapi/
- [X] T074 [US3] Copy appsettings.json from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T075 [US3] Copy appsettings.Development.json from upstream to generator/src/main/resources/aspnet-minimalapi/
- [X] T076 [US3] Create Properties/ directory at generator/src/main/resources/aspnet-minimalapi/Properties/
- [X] T077 [US3] Copy launchSettings.json from upstream aspnet-fastendpoints/Properties/ to generator/src/main/resources/aspnet-minimalapi/Properties/

**Validation Tasks**:

- [X] T078 [US3] Count templates: verify 17 .mustache files with `ls generator/src/main/resources/aspnet-minimalapi/*.mustache | wc -l` (18 templates present)
- [X] T079 [US3] Verify all 4 static files present: gitignore, appsettings.json, appsettings.Development.json, Properties/launchSettings.json
- [X] T080 [US3] Build generator with `devbox run mvn clean package` from generator/ directory
- [X] T081 [US3] Verify templates bundled in JAR with `jar tf generator/target/openapi-generator-minimalapi-1.0.0.jar | grep aspnet-minimalapi`

**Story Completion Criteria**:
✅ All 17 templates copied  
✅ All 4 static files copied  
✅ Templates bundle in JAR  
✅ Generator builds successfully

---

## Phase 5: End-to-End Validation

**Goal**: Verify complete generator workflow from discovery to code compilation

**Tasks**:

- [ ] T082 Test generator discovery: run `java -jar generator/target/openapi-generator-minimalapi-1.0.0.jar list` and verify "aspnetcore-minimalapi" appears in output
- [ ] T083 Download or locate petstore.yaml OpenAPI spec (if not available, use ~/scratch/git/openapi-generator3/modules/openapi-generator/src/test/resources/3_0/petstore.yaml)
- [ ] T084 Generate test project: run `java -jar generator/target/openapi-generator-minimalapi-1.0.0.jar generate -g aspnetcore-minimalapi -i petstore.yaml -o /tmp/test-output --additional-properties=packageName=PetstoreApi`
- [ ] T085 Verify generated structure: check /tmp/test-output/ contains Program.cs, PetstoreApi.csproj, PetstoreApi.sln
- [ ] T086 Verify Models/ directory exists with DTO classes at /tmp/test-output/Models/
- [ ] T087 Verify Features/ directory exists with endpoint classes at /tmp/test-output/Features/
- [ ] T088 Verify .csproj contains FastEndpoints NuGet package references
- [X] T089 Compile generated code: run `cd /tmp/test-minimalapi && devbox run dotnet build`
- [X] T090 Verify compilation succeeds with zero errors and 49 warnings (expected nullable reference type warnings)
- [X] T091 Verify bin/ directory created with compiled assemblies at /tmp/test-minimalapi/src/PetstoreApi/bin/

**Validation**: Complete end-to-end workflow succeeds (discovery → generation → compilation)

---

## Phase 6: Polish & Documentation

**Goal**: Finalize feature with documentation and repository hygiene

**Tasks**:

- [ ] T092 Update specs/002-generator-scaffolding/tasks.md marking all tasks complete
- [ ] T093 Document any deviations from Feature 001 analysis in specs/002-generator-scaffolding/notes.md (if deviations occurred)
- [ ] T094 Add generator/ directory to .gitignore exclusions (ensure generator/ is tracked, not ignored)
- [ ] T095 Commit generator/ directory with message: "feat(002): add generator scaffolding with 15 methods and 17 templates"
- [ ] T096 Verify Feature 002 success criteria (SC-001 through SC-011 from spec.md)

**Validation**: Feature 002 complete, all success criteria met, repository clean

---

## Dependencies

### User Story Dependencies

```
US1 (Initialize Project) → US2 (Implement Class) → US3 (Copy Templates) → Validation
```

**Rationale**: 
- US2 depends on US1 (needs scaffolded project structure)
- US3 depends on US2 (constructor sets templateDir that templates are copied to)
- Validation depends on all 3 (complete generator required)

**No Parallel Stories**: All 3 user stories are sequential and form atomic unit for MVP

### Task Dependencies Within Stories

**US1 (T005-T016)**: Sequential (scaffold → copy → verify → build)

**US2 (T017-T050)**:
- Sequential: T021-T030 (class structure and core methods)
- Parallel: T031-T041 (11 setter methods can be implemented in parallel)
- Sequential: T042-T050 (metadata methods → update ServiceLoader → build)

**US3 (T051-T081)**:
- Parallel: T056-T077 (all template copies can happen in parallel - different files)
- Sequential: T078-T081 (validation must follow copies)

### Parallel Execution Examples

**During US2 Implementation** (after T030 complete):
```bash
# Developer A: Implement setters 1-5
T031, T032, T033, T034, T035

# Developer B: Implement setters 6-11
T036, T037, T038, T039, T040, T041

# Both can work simultaneously on different methods in same file
```

**During US3 Implementation** (after US2 complete):
```bash
# Terminal 1: Copy operation templates
cp upstream/endpoint.mustache generator/resources/aspnet-minimalapi/
cp upstream/request.mustache generator/resources/aspnet-minimalapi/
# ... (T056-T064)

# Terminal 2: Copy supporting templates
cp upstream/program.mustache generator/resources/aspnet-minimalapi/
cp upstream/project.csproj.mustache generator/resources/aspnet-minimalapi/
# ... (T065-T068)

# Terminal 3: Copy model templates and static files
cp upstream/model.mustache generator/resources/aspnet-minimalapi/
# ... (T069-T077)

# All three can execute simultaneously - different source/target files
```

---

## Success Criteria Validation

Reference Feature 002 spec.md Success Criteria:

- [ ] **SC-001**: Meta command executed successfully (T005)
- [ ] **SC-002**: Files copied to generator/ directory (T009-T013)
- [ ] **SC-003**: Maven build completes in <2 minutes (T016, T046, T080)
- [ ] **SC-004**: MinimalApiServerCodegen compiles, extends AbstractCSharpCodegen (T048)
- [ ] **SC-005**: Class contains exactly 15 methods (T049)
- [ ] **SC-006**: All 17 templates + 4 static files copied (T078-T079)
- [ ] **SC-007**: Generator discoverable via `java -jar ... list` (T082)
- [ ] **SC-008**: Generator produces complete project from petstore.yaml (T084-T087)
- [ ] **SC-009**: Generated structure has Program.cs, .csproj, .sln, Models/, Features/ (T085-T087)
- [ ] **SC-010**: .csproj contains FastEndpoints NuGet packages (T088)
- [ ] **SC-011**: Generated code compiles with dotnet build (T089-T090)

---

## Task Execution Notes

### Reference Documents

- **Feature 001 Analysis**: `specs/001-fastendpoints-analysis/method-override-map.md` (15 method implementations)
- **Feature 001 Catalog**: `specs/001-fastendpoints-analysis/template-catalog.md` (17 template inventory)
- **Implementation Guide**: `specs/002-generator-scaffolding/quickstart.md` (10-step walkthrough)
- **Data Model**: `specs/002-generator-scaffolding/data-model.md` (8 entities)

### Key File Paths

- **Upstream Templates**: `~/scratch/git/openapi-generator3/modules/openapi-generator/src/main/resources/aspnet-fastendpoints/`
- **Upstream CLI JAR**: `~/scratch/git/openapi-generator3/modules/openapi-generator-cli/target/openapi-generator-cli.jar`
- **Generator Root**: `~/scratch/git/minimal-api-gen/generator/`
- **Generator Class**: `generator/src/main/java/org/openapitools/codegen/languages/MinimalApiServerCodegen.java`
- **Templates Directory**: `generator/src/main/resources/aspnet-minimalapi/`
- **ServiceLoader Config**: `generator/src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig`

### Build Commands

```bash
# Build generator
cd ~/scratch/git/minimal-api-gen/generator
devbox run mvn clean package

# Test generator discovery
java -jar target/openapi-generator-minimalapi-1.0.0.jar list

# Generate test project
java -jar target/openapi-generator-minimalapi-1.0.0.jar generate \
  -g aspnetcore-minimalapi \
  -i petstore.yaml \
  -o /tmp/test-output \
  --additional-properties=packageName=PetstoreApi

# Compile generated code
cd /tmp/test-output
devbox run dotnet build
```

---

## Estimated Effort

- **US1 (Initialize)**: 15 minutes (T001-T016)
- **US2 (Implement Class)**: 2-3 hours (T017-T050) - 15 methods + validation
- **US3 (Copy Templates)**: 30 minutes (T051-T081) - 21 file copies + validation
- **Validation**: 15 minutes (T082-T091)
- **Polish**: 15 minutes (T092-T096)

**Total**: 3-4 hours (sequential execution) or 2-3 hours (with parallelization)

---

**Next Action**: Begin with Phase 1 Setup (T001-T004), then proceed to US1 (T005-T016).
