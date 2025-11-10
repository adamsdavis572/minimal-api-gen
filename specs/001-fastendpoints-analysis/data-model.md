# Data Model: FastEndpoints Analysis Entities

**Feature**: 001-fastendpoints-analysis  
**Created**: 2025-11-10

## Overview

This document defines the entities used during analysis of AspNetCoreServerCodegen. These are documentation artifacts, not code entities.

## Core Entities

### Base Class Analysis

**Entity**: `AspNetCoreServerCodegen`

**Description**: The OpenAPI Generator class that contains FastEndpoints implementation logic to be analyzed.

**Attributes**:
- `filePath`: string - Absolute path to AspNetCoreServerCodegen.java
- `packageName`: string - Java package (org.openapitools.codegen.languages)
- `lineCount`: number - Total lines in file
- `fastendpointsBlockCount`: number - Number of conditional blocks found

**Relationships**:
- Contains multiple `OverrideMethod` instances
- References multiple `MustacheTemplate` files

---

### Override Method

**Entity**: `OverrideMethod`

**Description**: A Java method in AspNetCoreServerCodegen containing FastEndpoints-specific logic.

**Attributes**:
- `methodName`: string - Name of the method (e.g., "processOpts")
- `lineRange`: object - { start: number, end: number }
- `signature`: string - Full method signature with return type and parameters
- `fastendpointsBlockCount`: number - Number of FastEndpoints conditionals in method
- `logicSummary`: string - Description of what FastEndpoints logic does
- `codeExcerpt`: string - Relevant code showing FastEndpoints conditional

**Relationships**:
- Belongs to `AspNetCoreServerCodegen` class
- Contains one or more `ConditionalLogicBlock` instances

**Example**:
```json
{
  "methodName": "processOpts",
  "lineRange": { "start": 150, "end": 200 },
  "signature": "public void processOpts()",
  "fastendpointsBlockCount": 1,
  "logicSummary": "Registers FastEndpoints CLI options like useMediatR",
  "codeExcerpt": "if (\"fastendpoints\".equals(library)) { ... }"
}
```

---

### Conditional Logic Block

**Entity**: `ConditionalLogicBlock`

**Description**: A section of Java code within an `if ("fastendpoints".equals(library))` statement.

**Attributes**:
- `containingMethod`: string - Method name where this block appears
- `lineRange`: object - { start: number, end: number }
- `condition`: string - The conditional expression (usually 'if ("fastendpoints".equals(library))')
- `logicDescription`: string - What this specific block does
- `cliOptions`: array[string] - If in processOpts, list of CLI options registered
- `templateFiles`: array[string] - If in apiTemplateFiles/supportingFiles, list of templates registered

**Relationships**:
- Belongs to an `OverrideMethod`
- May register `MustacheTemplate` files

---

### Mustache Template

**Entity**: `MustacheTemplate`

**Description**: A template file that generates code output for FastEndpoints.

**Attributes**:
- `fileName`: string - Template file name (e.g., "endpoint.mustache")
- `filePath`: string - Absolute path in openapi-generator repo
- `templateType`: enum - "operation" | "supporting" | "model"
- `registeredIn`: string - Java method that registers this template (e.g., "apiTemplateFiles")
- `outputExtension`: string - File extension for generated output (e.g., ".cs")
- `frameworkDependencies`: array[string] - Framework-specific code found (e.g., ["Endpoint<TRequest, TResponse>"])
- `consumedVariables`: array[object] - Mustache variables used [{ name: string, description: string }]
- `reusabilityClassification`: enum - "Reuse Unchanged" | "Modify for Minimal API" | "Replace Completely"
- `reusabilityRationale`: string - Why this classification was chosen

**Relationships**:
- Referenced by `ConditionalLogicBlock` (if registered in method)
- May reference other templates (e.g., endpoint.mustache references model.mustache)

**Example**:
```json
{
  "fileName": "model.mustache",
  "filePath": "~/scratch/git/openapi-generator3/.../aspnetcore/model.mustache",
  "templateType": "model",
  "registeredIn": "modelTemplateFiles (inherited)",
  "outputExtension": ".cs",
  "frameworkDependencies": [],
  "consumedVariables": [
    { "name": "{{classname}}", "description": "C# class name for model" },
    { "name": "{{vars}}", "description": "Array of properties" }
  ],
  "reusabilityClassification": "Reuse Unchanged",
  "reusabilityRationale": "Generates framework-agnostic C# POCO classes"
}
```

---

### Template Variable

**Entity**: `TemplateVariable`

**Description**: A data model property consumed by Mustache templates.

**Attributes**:
- `variableName`: string - Mustache variable name (e.g., "{{vars}}")
- `dataType`: string - Expected type (e.g., "array", "string", "object")
- `sourceInDataModel`: string - Where this comes from in OpenAPI Generator's data model
- `consumingTemplates`: array[string] - List of template files using this variable
- `description`: string - What this variable represents

**Relationships**:
- Consumed by one or more `MustacheTemplate` instances

---

## Analysis Outputs (Documentation Artifacts)

### Method Override Map

**Structure**: Markdown table

**Columns**:
- Method Name
- Line Range
- FastEndpoints Logic Summary
- CLI Options / Templates Registered
- Code Excerpt

**Purpose**: Quick reference for Phase 2 developers to know what methods to override

---

### Template Catalog

**Structure**: Markdown table

**Columns**:
- Template File
- Type (Operation/Supporting/Model)
- Registered In (Java Method)
- Consumed Variables
- Framework Dependencies

**Purpose**: Complete inventory of all templates to be copied in Phase 2

---

### Reusability Matrix

**Structure**: Markdown table with three sections

**Sections**:
1. **Reuse Unchanged**: Templates that are framework-agnostic
2. **Modify for Minimal API**: Templates needing dependency/pattern changes
3. **Replace Completely**: Templates tightly coupled to FastEndpoints

**Columns**:
- Template File
- Current Framework Dependencies
- Rationale for Classification
- Phase 4 Action Required

**Purpose**: Guide Phase 4 refactoring decisions

---

## Validation Rules

### Method Override Map Completeness
- MUST include at least 4 methods: processOpts, apiTemplateFiles, postProcessOperationsWithModels, supportingFiles
- Each method MUST have line range and logic summary
- Each FastEndpoints conditional block MUST be documented

### Template Catalog Completeness
- ALL `.mustache` files in `resources/aspnetcore` MUST be listed
- Each template MUST be categorized by type
- Each template MUST have consumed variables documented

### Reusability Matrix Accuracy
- Model templates MUST be classified as "Reuse Unchanged" per Constitution Principle III
- Classification rationale MUST reference specific code evidence (e.g., "contains Endpoint<T, R> base class")
- At least 3 templates expected in each category

## State Transitions

1. **Initial**: Repository located, analysis starting
2. **Base Class Located**: AspNetCoreServerCodegen.java found and readable
3. **Conditionals Identified**: All FastEndpoints blocks found via grep
4. **Methods Mapped**: All override methods documented with logic summaries
5. **Templates Located**: All .mustache files found in resources directory
6. **Templates Categorized**: Each template assigned type (operation/supporting/model)
7. **Reusability Assessed**: Each template classified for Phase 4 refactoring
8. **Complete**: All documentation artifacts validated and stored in specs directory

## Dependencies

- **Input**: AspNetCoreServerCodegen.java source file from openapi-generator repository
- **Input**: Mustache templates from resources/aspnetcore directory
- **Output**: method-override-map.md (Phase 1)
- **Output**: template-catalog.md (Phase 1)
- **Output**: reusability-matrix.md (Phase 1)
