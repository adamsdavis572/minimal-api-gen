# Quick Start: FastEndpoints Analysis

**Feature**: 001-fastendpoints-analysis  
**Target Audience**: Developer performing the analysis  
**Time Required**: ~30-60 minutes

## Prerequisites

- [x] macOS development environment
- [x] VS Code with Java extensions installed
- [x] Access to `~/scratch/git/openapi-generator3` repository
- [x] Basic familiarity with Java syntax and Mustache templates
- [x] `grep`, `find`, and standard Unix tools available

## Step 1: Verify Repository Access

```bash
# Navigate to openapi-generator repository
cd ~/scratch/git/openapi-generator3

# Verify AspNetCoreServerCodegen.java exists
find . -name "AspNetCoreServerCodegen.java"
# Expected output: ./modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AspNetCoreServerCodegen.java

# Verify templates directory exists
ls -la modules/openapi-generator/src/main/resources/aspnetcore/
# Expected: List of .mustache files
```

**If repository not found**: Analysis cannot proceed. Update spec assumptions or locate correct repository path.

## Step 2: Locate and Open Base Class

```bash
# Open the base class in VS Code
code modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AspNetCoreServerCodegen.java
```

**What to look for**:
- Package declaration: `org.openapitools.codegen.languages`
- Class declaration: `public class AspNetCoreServerCodegen extends AbstractCSharpCodegen`
- Method declarations containing `if ("fastendpoints".equals(library))`

## Step 3: Find All FastEndpoints Conditional Blocks

```bash
# Navigate to the base class directory
cd modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/

# Search for FastEndpoints conditionals
grep -n 'if.*"fastendpoints".*equals' AspNetCoreServerCodegen.java

# Alternative: case-insensitive search
grep -in 'fastendpoints' AspNetCoreServerCodegen.java
```

**Expected results**: Multiple matches showing line numbers where FastEndpoints logic appears.

**Record**:
- Line number of each match
- Method name containing the match (scroll up to find method signature)

## Step 4: Document Method Override Map

For each method containing FastEndpoints logic:

1. **Find method signature**: Scroll to method start, note return type and parameters
2. **Identify line range**: Note method start and end lines
3. **Extract logic summary**: Read the FastEndpoints conditional block, summarize what it does
4. **Copy code excerpt**: Copy the relevant if-block for documentation

**Create file**: `specs/001-fastendpoints-analysis/method-override-map.md`

**Template**:
```markdown
# Method Override Map

| Method Name | Line Range | FastEndpoints Logic Summary | CLI Options / Templates | Code Excerpt |
|-------------|------------|----------------------------|------------------------|--------------|
| processOpts | 150-200 | Registers FastEndpoints CLI options | useMediatR, useAuthorizationHandler | `if ("fastendpoints".equals(library)) { cliOptions.add(...) }` |
| ... | ... | ... | ... | ... |
```

**Target**: At least 4 methods documented (processOpts, apiTemplateFiles, postProcessOperationsWithModels, supportingFiles).

## Step 5: Locate All Mustache Templates

```bash
# Navigate back to repo root
cd ~/scratch/git/openapi-generator3

# Find all mustache templates in aspnetcore resources
find modules/openapi-generator/src/main/resources/aspnetcore -name "*.mustache" -type f

# Count templates
find modules/openapi-generator/src/main/resources/aspnetcore -name "*.mustache" -type f | wc -l
```

**Expected**: 10-15 template files.

**For each template**:
1. Open in VS Code
2. Note file name and path
3. Scan for FastEndpoints-specific code (e.g., `Endpoint<TRequest, TResponse>`)
4. Identify Mustache variables used (e.g., `{{vars}}`, `{{operations}}`)

## Step 6: Categorize Templates

**Create file**: `specs/001-fastendpoints-analysis/template-catalog.md`

**For each template**, determine:
- **Type**: Operation (endpoint.mustache), Supporting (program.cs.mustache, csproj.mustache), or Model (model.mustache)
- **Registered In**: Which Java method adds this template (check method override map)
- **Variables**: List Mustache variables found (e.g., `{{classname}}`, `{{vars}}`)
- **Framework Dependencies**: Any FastEndpoints-specific code patterns

**Template**:
```markdown
# Template Catalog

## Operation Templates
| Template File | Registered In | Consumed Variables | Framework Dependencies |
|---------------|---------------|-------------------|------------------------|
| endpoint.mustache | apiTemplateFiles | {{operationId}}, {{httpMethod}} | Endpoint<TRequest, TResponse> |

## Supporting Templates
| Template File | Registered In | Consumed Variables | Framework Dependencies |
|---------------|---------------|-------------------|------------------------|
| program.cs.mustache | supportingFiles | {{package}} | UseFastEndpoints() |

## Model Templates
| Template File | Registered In | Consumed Variables | Framework Dependencies |
|---------------|---------------|-------------------|------------------------|
| model.mustache | modelTemplateFiles | {{classname}}, {{vars}} | None (POCO) |
```

## Step 7: Build Reusability Matrix

**Create file**: `specs/001-fastendpoints-analysis/reusability-matrix.md`

**For each template**, classify:
- **Reuse Unchanged**: No FastEndpoints dependencies (typically model templates)
- **Modify for Minimal API**: Has FastEndpoints dependencies but structure reusable (program.cs, csproj)
- **Replace Completely**: Tightly coupled to FastEndpoints patterns (endpoint.mustache)

**Template**:
```markdown
# Reusability Matrix

## Reuse Unchanged (Framework-Agnostic)
| Template | Current Dependencies | Rationale | Phase 4 Action |
|----------|---------------------|-----------|----------------|
| model.mustache | None | Generates C# POCO classes | Copy as-is |
| modelEnum.mustache | None | Generates C# enums | Copy as-is |

## Modify for Minimal API
| Template | Current Dependencies | Rationale | Phase 4 Action |
|----------|---------------------|-----------|----------------|
| program.cs.mustache | UseFastEndpoints() | Startup logic structure reusable | Replace FastEndpoints calls with Minimal API patterns |
| csproj.mustache | FastEndpoints NuGet | Project file structure reusable | Change package references |

## Replace Completely
| Template | Current Dependencies | Rationale | Phase 4 Action |
|----------|---------------------|-----------|----------------|
| endpoint.mustache | Endpoint<TRequest, TResponse> base class | Entire class structure FastEndpoints-specific | Create new TagEndpoints.cs.mustache for Minimal API |
```

## Step 8: Validate Completeness

**Checklist**:
- [ ] method-override-map.md created with at least 4 methods
- [ ] template-catalog.md created with all .mustache files categorized
- [ ] reusability-matrix.md created with all templates classified
- [ ] All FastEndpoints conditional blocks from Step 3 accounted for in method map
- [ ] Model templates confirmed framework-agnostic (aligns with Constitution Principle III)

**Validation commands**:
```bash
# Check files created
ls specs/001-fastendpoints-analysis/*.md

# Count methods in override map
grep -c "^|" specs/001-fastendpoints-analysis/method-override-map.md

# Count templates in catalog
grep -c "\.mustache" specs/001-fastendpoints-analysis/template-catalog.md
```

## Step 9: Re-check Constitution Gates

Return to `plan.md` and update Constitution Check:

- **Phase-Gated Progression**: Confirm Phase 1 deliverables complete
- **Template Reusability**: Verify reusability matrix shows model templates are framework-agnostic
- **Documentation**: Ensure all artifacts are markdown files in version control

**Update plan.md** with post-Phase 1 status.

## Step 10: Commit Analysis Artifacts

```bash
# Navigate to minimal-api-gen repository
cd /Users/adam/scratch/git/minimal-api-gen

# Ensure on correct branch
git branch --show-current
# Expected: 001-fastendpoints-analysis

# Add analysis artifacts
git add specs/001-fastendpoints-analysis/*.md

# Commit with descriptive message
git commit -m "feat(001): complete FastEndpoints analysis with method map, template catalog, and reusability matrix"
```

## Troubleshooting

**Problem**: Repository not found at `~/scratch/git/openapi-generator3`

**Solution**: Verify path or update spec assumptions. Check if repository is at different location.

---

**Problem**: `grep` finds no FastEndpoints conditionals

**Solution**: Verify correct file (AspNetCoreServerCodegen.java not AspNetServerCodegen.java). Try case-insensitive search: `grep -i fastendpoints`.

---

**Problem**: Templates directory empty or not found

**Solution**: Verify path to resources: `modules/openapi-generator/src/main/resources/aspnetcore/`. Check repository version.

---

**Problem**: Unclear which method registers a template

**Solution**: Search for template filename in Java source: `grep "endpoint.mustache" AspNetCoreServerCodegen.java`. Look for `put()` or `add()` calls.

## Next Steps

After completing this analysis:

1. **Feature 002** can begin: Use method override map to create MinimalApiServerCodegen class
2. **Phase 2 Scaffolding**: Copy templates from catalog into new generator project
3. **Template Reusability**: Phase 4 will use reusability matrix to guide refactoring

## Success Indicators

- ✅ All 4 key methods identified and documented
- ✅ Complete template inventory with categorization
- ✅ Model templates confirmed as framework-agnostic
- ✅ Analysis completed within 30-60 minutes
- ✅ Documentation artifacts committed to version control
- ✅ Phase 2 can proceed without referring back to base class source
