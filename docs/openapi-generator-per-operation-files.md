# OpenAPI Generator: Per-Operation File Generation Pattern

**Source**: https://github.com/OpenAPITools/openapi-generator/blob/4a7e0c9bdcc252e13110d10e09c59ef079e92d02/docs/customization.md

## Problem
Need to generate one file per operation (e.g., `AddPetCommand.cs`, `GetPetByIdQuery.cs`) instead of one file per tag.

## Solution Pattern

### 1. Override `postProcessOperationsWithModels()`

```java
@Override
public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
    // Called after all operations are processed
    // This is where you manually write per-operation files
    return super.postProcessAllModels(objs);
}
```

### 2. Store Operation Data During Processing

```java
private List<CodegenOperation> allOperations = new ArrayList<>();

@Override
protected void processOperation(CodegenOperation operation) {
    super.processOperation(operation);
    
    // Add vendor extensions
    operation.vendorExtensions.put("commandClassName", getCommandClassName(operation.operationId));
    
    // Store for later file generation
    if (useMediatr) {
        allOperations.add(operation);
    }
}
```

### 3. Write Files in `postProcessAllModels()`

```java
@Override
public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
    Map<String, ModelsMap> result = super.postProcessAllModels(objs);
    
    if (!useMediatr || allOperations.isEmpty()) {
        return result;
    }
    
    for (CodegenOperation op : allOperations) {
        writeOperationFile(op);
    }
    
    return result;
}

private void writeOperationFile(CodegenOperation operation) {
    String templateName = "command.mustache";
    String outputFolder = "Commands";
    String filename = operation.vendorExtensions.get("commandClassName") + ".cs";
    
    // Prepare data for template
    Map<String, Object> data = new HashMap<>();
    data.put("operation", operation);
    data.putAll(additionalProperties);
    
    // Write file using template engine
    String templateContent = readTemplate(templateName);
    String generatedContent = processTemplate(data, templateContent);
    writeFile(outputFolder, filename, generatedContent);
}
```

## Key Methods to Use

- `readTemplate(String name)` - Read template file
- `processTemplate(Map<String, Object> data, String template)` - Process mustache template
- `writeToFile(String filename, String content)` - Write output file

## Template Access

Templates have access to:
- `{{packageName}}` - from additionalProperties
- `{{operation}}` - the CodegenOperation object
- `{{operation.operationId}}` - operation ID
- `{{operation.vendorExtensions.commandClassName}}` - custom vendor extensions

## Important Notes

1. **Timing**: File writing must happen in `postProcessAllModels()` or similar late-stage hook
2. **Data**: All vendor extensions added in `processOperation()` are available in templates
3. **File Exists Check**: Check if file exists before writing (for handler protection per R4)
4. **Template Engine**: Use framework's template processor, don't invoke manually

## For Feature 006 (MediatR)

Need to generate:
- One command file per POST/PUT/PATCH/DELETE operation
- One query file per GET operation  
- One handler file per operation (with exists check)

All vendor extensions are already added in `processOperation()`, so templates can use:
- `{{operation.vendorExtensions.commandClassName}}`
- `{{operation.vendorExtensions.queryClassName}}`
- `{{operation.vendorExtensions.handlerClassName}}`
- `{{operation.vendorExtensions.mediatrResponseType}}`
- `{{operation.vendorExtensions.isQuery}}`
- `{{operation.vendorExtensions.isCommand}}`
