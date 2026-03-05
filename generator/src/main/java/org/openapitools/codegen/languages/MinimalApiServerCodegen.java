package org.openapitools.codegen.languages;

import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.samskivert.mustache.Mustache;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

public class MinimalApiServerCodegen extends AbstractCSharpCodegen implements CodegenConfig {

    public static final String PROJECT_NAME = "projectName";
    public static final String USE_PROBLEM_DETAILS = "useProblemDetails";
    public static final String USE_RECORDS = "useRecords";
    public static final String USE_VALIDATORS = "useValidators";
    public static final String USE_RESPONSE_CACHING = "useResponseCaching";
    public static final String USE_API_VERSIONING = "useApiVersioning";
    public static final String USE_GLOBAL_EXCEPTION_HANDLER = "useGlobalExceptionHandler";
    public static final String USE_MEDIATR = "useMediatr";
    public static final String USE_NUGET_PACKAGING = "useNugetPackaging";
    public static final String PACKAGE_DESCRIPTION = "packageDescription";
    public static final String PACKAGE_LICENSE_EXPRESSION = "packageLicenseExpression";
    public static final String PACKAGE_REPOSITORY_URL = "packageRepositoryUrl";
    public static final String PACKAGE_PROJECT_URL = "packageProjectUrl";
    public static final String PACKAGE_TAGS = "packageTags";
    public static final String ROUTE_PREFIX = "routePrefix";
    public static final String VERSIONING_PREFIX = "versioningPrefix";
    public static final String API_VERSION = "apiVersion";
    public static final String SOLUTION_GUID = "solutionGuid";
    public static final String PROJECT_CONFIGURATION_GUID = "projectConfigurationGuid";
    public static final String CONTRACTS_PROJECT_GUID = "contractsProjectGuid";

    private final Logger LOGGER = LoggerFactory.getLogger(MinimalApiServerCodegen.class);

    private boolean useProblemDetails = false;
    private boolean useRecords = false;
    private boolean useValidators = false;
    private boolean useResponseCaching = false;
    private boolean useApiVersioning = false;
    private boolean useGlobalExceptionHandler = true;
    private boolean useMediatr = false;
    private boolean useNugetPackaging = false;
    private String routePrefix = "api";
    private String versioningPrefix = "v";
    private String apiVersion = "1";
    private String solutionGuid = null;
    private String projectConfigurationGuid = null;
    private String contractsProjectGuid = null;
    private String generatedFolder = null; // Path for generated code (Commands, Queries, DTOs, etc.)


    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    public String getName() {
        return "aspnetcore-minimalapi";
    }

    public String getHelp() {
        return "Generates an ASP.NET Core Minimal API server.";
    }

    public MinimalApiServerCodegen() {
        super();

        outputFolder = "generated-code" + File.separator + "aspnet-minimalapi";
        embeddedTemplateDir = templateDir = "aspnet-minimalapi";

        modelTemplateFiles.put("model.mustache", ".cs");
        // Minimal API generates one endpoint file per tag using api.mustache
        apiTemplateFiles.put("api.mustache", "Endpoints.cs");
        
        // NOTE: Per-operation MediatR files (command, query, handler) are added dynamically
        // in processOperation() based on useMediatr flag - see generateMediatrFilesForOperation()
        // NOTE: Validator template files are added in processOpts() after flags are parsed

        addSwitch(USE_PROBLEM_DETAILS, "Enable RFC 7807 compatible error responses.", useProblemDetails);
        addSwitch(USE_RECORDS, "Use record instead of class for the requests and response.", useRecords);
        addSwitch(USE_VALIDATORS, "Enable FluentValidation request validators.", useValidators);
        addSwitch(USE_RESPONSE_CACHING, "Enable response caching.", useResponseCaching);
        addSwitch(USE_API_VERSIONING, "Enable API versioning.", useApiVersioning);
        addSwitch(USE_GLOBAL_EXCEPTION_HANDLER, "Enable global exception handling middleware.", useGlobalExceptionHandler);
        addSwitch(USE_MEDIATR, "Enable MediatR CQRS pattern with commands, queries, and handlers.", useMediatr);
        addSwitch(USE_NUGET_PACKAGING, "Generate separate NuGet package project for API contracts.", useNugetPackaging);
        addOption(PACKAGE_DESCRIPTION, "Package description for NuGet feed", null);
        addOption(PACKAGE_LICENSE_EXPRESSION, "SPDX license expression (e.g., Apache-2.0, MIT)", "Apache-2.0");
        addOption(PACKAGE_REPOSITORY_URL, "Git repository URL", null);
        addOption(PACKAGE_PROJECT_URL, "Project homepage URL", null);
        addOption(PACKAGE_TAGS, "Semicolon-separated NuGet tags", "openapi;minimal-api;contracts");
        addOption(ROUTE_PREFIX, "The route prefix for the API. Used only if useApiVersioning is true", routePrefix);
        addOption(VERSIONING_PREFIX, "The versioning prefix for the API. Used only if useApiVersioning is true", versioningPrefix);
        addOption(API_VERSION, "The version of the API. Used only if useApiVersioning is true", apiVersion);
        addOption(SOLUTION_GUID, "The solution GUID to be used in the solution file (auto generated if not provided)", solutionGuid);
        addOption(PROJECT_CONFIGURATION_GUID, "The project configuration GUID to be used in the solution file (auto generated if not provided)", projectConfigurationGuid);
    }

    @Override
    public void processOpts() {

        setPackageDescription(openAPI.getInfo().getDescription());
        setPackageVersion();
        setPackageMetadata();

        setUseProblemDetails();
        setUseRecordForRequest();
        setUseValidators();
        setUseResponseCaching();
        setUseApiVersioning();
        setUseGlobalExceptionHandler();
        setUseMediatr();
        setUseNugetPackaging();
        setRoutePrefix();
        setVersioningPrefix();
        setApiVersion();
        setSolutionGuid();
        setProjectConfigurationGuid();
        setContractsProjectGuid();
        
        // Extract basePath from server URL for endpoint routing
        setBasePath();
        
        // FluentValidation generates one validator file per tag (added after flags are parsed)
        if (useValidators && useMediatr) {
            apiTemplateFiles.put("validator.mustache", "Validators.cs");
        }

        super.processOpts();

        addSupportingFiles();
    }

    private void addSupportingFiles() {
        apiPackage = "Features";
        modelPackage = "Models";
        String packageFolder = sourceFolder + File.separator + packageName;
        // For NuGet packaging: API contract goes to Contract/, templates go to Implementation
        this.generatedFolder = useNugetPackaging ? "Contract" : packageFolder;

        supportingFiles.add(new SupportingFile("readme.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("solution.mustache", "", packageName + ".sln"));
        
        // Conditional project structure: dual-project (NuGet) vs single-project (default)
        if (useNugetPackaging) {
            generateNugetPackageProject();
            generateImplementationProject();
        } else {
            supportingFiles.add(new SupportingFile("project.csproj.mustache", packageFolder, packageName + ".csproj"));
        }
        
        supportingFiles.add(new SupportingFile("Properties" + File.separator + "launchSettings.json", packageFolder + File.separator + "Properties", "launchSettings.json"));

        supportingFiles.add(new SupportingFile("appsettings.json", packageFolder, "appsettings.json"));
        supportingFiles.add(new SupportingFile("appsettings.Development.json", packageFolder, "appsettings.Development.json"));

        supportingFiles.add(new SupportingFile("program.mustache", packageFolder, "Program.cs"));
        
        // Enum converter that supports JsonPropertyName attributes
        // For NuGet packaging: Converters go to Contract/ (they serialize DTOs)
        String convertersFolder = useNugetPackaging ? 
            "Contract" + File.separator + "Converters" :
            packageFolder + File.separator + "Converters";
        supportingFiles.add(new SupportingFile("EnumMemberJsonConverter.mustache",
            convertersFolder, "EnumMemberJsonConverter.cs"));
        
        // Factory for creating enum converters globally
        supportingFiles.add(new SupportingFile("EnumMemberJsonConverterFactory.mustache",
            convertersFolder, "EnumMemberJsonConverterFactory.cs"));
        
        // Minimal API: EndpointMapper extension for MapAllEndpoints()
        supportingFiles.add(new SupportingFile("endpointMapper.mustache", 
            packageFolder + File.separator + "Extensions", "EndpointMapper.cs"));
        
        // Minimal API: ServiceCollectionExtensions for AddApplicationServices()
        supportingFiles.add(new SupportingFile("serviceCollectionExtensions.mustache", 
            packageFolder + File.separator + "Extensions", "ServiceCollectionExtensions.cs")
            .doNotOverwrite());
        
        // FluentValidation: ValidationBehavior for MediatR pipeline
        if (useValidators && useMediatr) {
            supportingFiles.add(new SupportingFile("ValidationBehavior.mustache",
                packageFolder + File.separator + "Behaviors", "ValidationBehavior.cs"));
        }

        // Configurator interfaces for assembly-scan DI pattern
        String configuratorsFolder = packageFolder + File.separator + "Configurators";
        supportingFiles.add(new SupportingFile("IServiceConfigurator.mustache",
            configuratorsFolder, "IServiceConfigurator.cs"));
        supportingFiles.add(new SupportingFile("IApplicationConfigurator.mustache",
            configuratorsFolder, "IApplicationConfigurator.cs"));

        // Global exception handler extension (extracted from inline program.mustache block)
        if (useGlobalExceptionHandler) {
            supportingFiles.add(new SupportingFile("exceptionHandlingExtensions.mustache",
                packageFolder + File.separator + "Extensions", "ExceptionHandlingExtensions.cs"));
        }
    }

    private void generateNugetPackageProject() {
        String contractsFolder = sourceFolder + File.separator + packageName + ".Contracts";
        
        // Contracts .csproj file
        supportingFiles.add(new SupportingFile("nuget-project.csproj.mustache", 
            contractsFolder, packageName + ".Contracts.csproj"));
        
        // Extension methods for Contracts project
        supportingFiles.add(new SupportingFile("endpointExtensions.mustache", 
            contractsFolder + File.separator + "Extensions", "EndpointExtensions.cs"));
        
        if (useValidators) {
            supportingFiles.add(new SupportingFile("validatorExtensions.mustache", 
                contractsFolder + File.separator + "Extensions", "ValidatorExtensions.cs"));
        }
    }

    private void generateImplementationProject() {
        String packageFolder = sourceFolder + File.separator + packageName;
        
        // Implementation .csproj file (references Contracts project)
        supportingFiles.add(new SupportingFile("implementation-project.csproj.mustache", 
            packageFolder, packageName + ".csproj"));
        
        // Extension method for handler registration
        supportingFiles.add(new SupportingFile("handlerExtensions.mustache", 
            packageFolder + File.separator + "Extensions", "HandlerExtensions.cs"));
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, io.swagger.v3.oas.models.Operation operation,
                                     CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
        // Add computed fields for templates
        if (co.operationId != null) {
            co.vendorExtensions.put("operationIdPascalCase", toModelName(co.operationId));
        }
        
        // Success response details
        if (co.responses != null && !co.responses.isEmpty()) {
            co.responses.stream()
                .filter(r -> r.is2xx)
                .findFirst()
                .ifPresent(successResponse -> {
                    co.vendorExtensions.put("successCode", successResponse.code);
                    co.vendorExtensions.put("resultMethod", 
                        successResponse.dataType != null ? "Results.Ok" : "Results.Ok");
                });
        }
        
        // Group by tag for Minimal API structure
        String groupKey = tag != null && !tag.isEmpty() ? tag : "Default";
        operations.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(co);
        
        // Store tag for template use
        co.vendorExtensions.put("x-tag-name", groupKey);
        co.vendorExtensions.put("tagPascalCase", toModelName(groupKey));
        
        co.baseName = groupKey;
        
        LOGGER.info("Added operation '{}' to tag group '{}'", co.operationId, groupKey);
    }

    private void setPackageVersion() {
        String version = "1.0.0"; // Default version
        
        // Priority 1: Check if CLI option is provided
        if (additionalProperties.containsKey("packageVersion")) {
            version = (String) additionalProperties.get("packageVersion");
        }
        // Priority 2: Read from OpenAPI spec
        else if (openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null && !openAPI.getInfo().getVersion().isEmpty()) {
            version = openAPI.getInfo().getVersion();
        }
        // Priority 3: Use default "1.0.0" (already set)
        
        additionalProperties.put("packageVersion", version);
    }

    private void setPackageMetadata() {
        // Package description - default to OpenAPI spec description
        String packageDescription = (String) additionalProperties.get(PACKAGE_DESCRIPTION);
        if (packageDescription == null || packageDescription.isEmpty()) {
            if (openAPI.getInfo() != null && openAPI.getInfo().getDescription() != null && !openAPI.getInfo().getDescription().isEmpty()) {
                packageDescription = openAPI.getInfo().getDescription();
            } else if (openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null) {
                packageDescription = "API contracts for " + openAPI.getInfo().getTitle();
            } else {
                packageDescription = "API contracts generated from OpenAPI specification";
            }
        }
        additionalProperties.put("packageDescription", packageDescription);

        // License expression - default to Apache-2.0
        String packageLicenseExpression = (String) additionalProperties.getOrDefault(PACKAGE_LICENSE_EXPRESSION, "Apache-2.0");
        additionalProperties.put("packageLicenseExpression", packageLicenseExpression);

        // Repository URL - optional, only add if provided
        if (additionalProperties.containsKey(PACKAGE_REPOSITORY_URL)) {
            String packageRepositoryUrl = (String) additionalProperties.get(PACKAGE_REPOSITORY_URL);
            if (packageRepositoryUrl != null && !packageRepositoryUrl.isEmpty()) {
                additionalProperties.put("packageRepositoryUrl", packageRepositoryUrl);
            }
        }

        // Project URL - optional, only add if provided
        if (additionalProperties.containsKey(PACKAGE_PROJECT_URL)) {
            String packageProjectUrl = (String) additionalProperties.get(PACKAGE_PROJECT_URL);
            if (packageProjectUrl != null && !packageProjectUrl.isEmpty()) {
                additionalProperties.put("packageProjectUrl", packageProjectUrl);
            }
        }

        // Tags - default to openapi;minimal-api;contracts
        String packageTags = (String) additionalProperties.getOrDefault(PACKAGE_TAGS, "openapi;minimal-api;contracts");
        additionalProperties.put("packageTags", packageTags);
    }

    private void setUseProblemDetails() {
        if (additionalProperties.containsKey(USE_PROBLEM_DETAILS)) {
            useProblemDetails = convertPropertyToBooleanAndWriteBack(USE_PROBLEM_DETAILS);
        } else {
            additionalProperties.put(USE_PROBLEM_DETAILS, useProblemDetails);
        }
    }

    private void setUseRecordForRequest() {
        if (additionalProperties.containsKey(USE_RECORDS)) {
            useRecords = convertPropertyToBooleanAndWriteBack(USE_RECORDS);
        } else {
            additionalProperties.put(USE_RECORDS, useRecords);
        }
    }

    private void setUseValidators() {
        if (additionalProperties.containsKey(USE_VALIDATORS)) {
            useValidators = convertPropertyToBooleanAndWriteBack(USE_VALIDATORS);
        } else {
            additionalProperties.put(USE_VALIDATORS, useValidators);
        }
    }

    private void setUseResponseCaching() {
        if (additionalProperties.containsKey(USE_RESPONSE_CACHING)) {
            useResponseCaching = convertPropertyToBooleanAndWriteBack(USE_RESPONSE_CACHING);
        } else {
            additionalProperties.put(USE_RESPONSE_CACHING, useResponseCaching);
        }
    }

    private void setUseApiVersioning() {
        if (additionalProperties.containsKey(USE_API_VERSIONING)) {
            useApiVersioning = convertPropertyToBooleanAndWriteBack(USE_API_VERSIONING);
        } else {
            additionalProperties.put(USE_API_VERSIONING, useApiVersioning);
        }
    }


    private void setUseGlobalExceptionHandler() {
        if (additionalProperties.containsKey(USE_GLOBAL_EXCEPTION_HANDLER)) {
            useGlobalExceptionHandler = convertPropertyToBooleanAndWriteBack(USE_GLOBAL_EXCEPTION_HANDLER);
        } else {
            additionalProperties.put(USE_GLOBAL_EXCEPTION_HANDLER, useGlobalExceptionHandler);
        }
    }

    private void setUseMediatr() {
        if (additionalProperties.containsKey(USE_MEDIATR)) {
            useMediatr = convertPropertyToBooleanAndWriteBack(USE_MEDIATR);
        } else {
            additionalProperties.put(USE_MEDIATR, useMediatr);
        }
    }

    private void setUseNugetPackaging() {
        if (additionalProperties.containsKey(USE_NUGET_PACKAGING)) {
            useNugetPackaging = convertPropertyToBooleanAndWriteBack(USE_NUGET_PACKAGING);
        } else {
            additionalProperties.put(USE_NUGET_PACKAGING, useNugetPackaging);
        }
    }

    private void setRoutePrefix() {
        if (additionalProperties.containsKey(ROUTE_PREFIX)) {
            routePrefix = (String) additionalProperties.get(ROUTE_PREFIX);
        } else {
            additionalProperties.put(ROUTE_PREFIX, routePrefix);
        }
    }

    private void setVersioningPrefix() {
        if (additionalProperties.containsKey(VERSIONING_PREFIX)) {
            versioningPrefix = (String) additionalProperties.get(VERSIONING_PREFIX);
        } else {
            additionalProperties.put(VERSIONING_PREFIX, versioningPrefix);
        }
    }

    private void setApiVersion() {
        if (additionalProperties.containsKey(API_VERSION)) {
            apiVersion = (String) additionalProperties.get(API_VERSION);
        } else {
            additionalProperties.put(API_VERSION, apiVersion);
        }
    }

    private void setSolutionGuid() {
        if (additionalProperties.containsKey(SOLUTION_GUID)) {
            solutionGuid = (String) additionalProperties.get(SOLUTION_GUID);
        } else {
            solutionGuid = "{" + randomUUID().toString().toUpperCase(Locale.ROOT) + "}";
            additionalProperties.put(SOLUTION_GUID, solutionGuid);
        }
    }

    private void setProjectConfigurationGuid() {
        if (additionalProperties.containsKey(PROJECT_CONFIGURATION_GUID)) {
            projectConfigurationGuid = (String) additionalProperties.get(PROJECT_CONFIGURATION_GUID);
        } else {
            projectConfigurationGuid = "{" + randomUUID().toString().toUpperCase(Locale.ROOT) + "}";
            additionalProperties.put(PROJECT_CONFIGURATION_GUID, projectConfigurationGuid);
        }
    }

    private void setContractsProjectGuid() {
        if (additionalProperties.containsKey(CONTRACTS_PROJECT_GUID)) {
            contractsProjectGuid = (String) additionalProperties.get(CONTRACTS_PROJECT_GUID);
        } else {
            contractsProjectGuid = "{" + randomUUID().toString().toUpperCase(Locale.ROOT) + "}";
            additionalProperties.put(CONTRACTS_PROJECT_GUID, contractsProjectGuid);
        }
    }
    
    private void setBasePath() {
        // Extract basePath from the first server URL if available
        String basePath = "";
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            String serverUrl = openAPI.getServers().get(0).getUrl();
            try {
                // Handle both absolute URLs and relative paths
                if (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) {
                    // Extract path from absolute URL (e.g., "http://petstore.swagger.io/v2" -> "/v2")
                    java.net.URI uri = new java.net.URI(serverUrl);
                    basePath = uri.getPath();
                } else if (serverUrl.startsWith("/")) {
                    // Already a path (e.g., "/v2")
                    basePath = serverUrl;
                } else {
                    // Relative path without leading slash
                    basePath = "/" + serverUrl;
                }
                
                if (basePath == null || basePath.isEmpty() || basePath.equals("/")) {
                    basePath = "";
                }
                LOGGER.info("Extracted basePath '{}' from server URL '{}'", basePath, serverUrl);
            } catch (java.net.URISyntaxException e) {
                LOGGER.warn("Could not parse server URL '{}': {}", serverUrl, e.getMessage());
                // Fallback: try to extract path manually
                if (serverUrl.contains("://")) {
                    int pathStart = serverUrl.indexOf('/', serverUrl.indexOf("://") + 3);
                    if (pathStart > 0) {
                        basePath = serverUrl.substring(pathStart);
                    }
                }
            }
        }
        additionalProperties.put("serverBasePath", basePath);
    }

    @Override
    public String toApiFilename(String name) {
        // Custom routing for MediatR files based on template type
        // This is called during file generation with the tag or operation name
        return super.toApiFilename(name);
    }

    @Override
    public String modelFileFolder() {
        if (useNugetPackaging) {
            // Models are templates for Implementation project, not in Contract/
            return outputFolder + File.separator + sourceFolder + File.separator + packageName + File.separator + "Models";
        }
        return super.modelFileFolder();
    }

    @Override
    public String apiFileFolder() {
        if (useNugetPackaging) {
            return outputFolder + File.separator + "Contract" + File.separator + "Endpoints";
        }
        return super.apiFileFolder();
    }
    
    @Override
    protected void processOperation(CodegenOperation operation) {
        super.processOperation(operation);

        // Converts, for example, PUT to Put for endpoint configuration
        operation.httpMethod = operation.httpMethod.charAt(0) + operation.httpMethod.substring(1).toLowerCase(Locale.ROOT);
        
        // Convert List<T> to T[] for query array parameters
        // Minimal APIs support string[] natively but not List<string>
        // Apply to allParams AND queryParams (they are separate lists, not references)
        if (operation.allParams != null) {
            for (CodegenParameter param : operation.allParams) {
                if (param.isQueryParam && param.isContainer && param.isArray) {
                    // Change List<string> to string[], List<int> to int[], etc.
                    String innerType = param.items != null ? param.items.dataType : "string";
                    // FR-027: Array $ref enum/model params must use Dto type so Contracts layer never references Models.
                    // Use complexType != null as the discriminator: set only for $ref types (named schema enum/model),
                    // NOT for inline enums (e.g. string items with enum: [...]) or plain primitives.
                    boolean innerIsRefType = param.items != null && param.items.complexType != null;
                    String resolvedInnerType = innerIsRefType ? innerType + "Dto" : innerType;
                    param.dataType = resolvedInnerType + "[]";
                    LOGGER.info("Converted query array parameter '{}' from List<{}> to {}[] for Minimal API compatibility",
                        param.paramName, innerType, resolvedInnerType);
                }
                // Mark model-type query parameters for special handling (complex JSON deserialization)
                if (param.isQueryParam && param.isModel) {
                    param.vendorExtensions.put("x-is-complex-query-param", true);
                    LOGGER.info("Operation '{}' has model-type query parameter '{}' - will use JSON deserialization from query string", 
                        operation.operationId, param.paramName);
                }
                // FR-027: Convert Model types to DTO types for query/path parameters (Contract-First CQRS)
                // Don't convert array/collection types (they contain primitive or already-converted types)
                // NOTE: Enums MUST be converted to DTO types because they're in the Contract package
                if ((param.isQueryParam || param.isPathParam) && (param.isModel || param.isEnum) && !param.isContainer) {
                    String originalType = param.dataType;
                    param.dataType = originalType + "Dto";
                    LOGGER.info("Converted parameter '{}' type from {} to {} for Contract-First CQRS", 
                        param.paramName, originalType, param.dataType);
                }
            }
        }
        
        // Also apply conversion to queryParams list for MediatR Query class generation
        if (operation.queryParams != null) {
            for (CodegenParameter param : operation.queryParams) {
                if (param.isContainer && param.isArray) {
                    String innerType = param.items != null ? param.items.dataType : "string";
                    // FR-027: Array $ref enum/model params must use Dto type so Contracts layer never references Models.
                    // Use complexType != null as the discriminator: set only for $ref types (named schema enum/model),
                    // NOT for inline enums (e.g. string items with enum: [...]) or plain primitives.
                    boolean innerIsRefType = param.items != null && param.items.complexType != null;
                    String resolvedInnerType = innerIsRefType ? innerType + "Dto" : innerType;
                    param.dataType = resolvedInnerType + "[]";
                }
                // FR-027: Convert Model types to DTO types for query parameters (Contract-First CQRS)
                // Don't convert array/collection types (they contain primitive or already-converted types)
                // NOTE: Enums MUST be converted to DTO types because they're in the Contract package
                if ((param.isModel || param.isEnum) && !param.isContainer) {
                    String originalType = param.dataType;
                    param.dataType = originalType + "Dto";
                    LOGGER.info("Converted query parameter '{}' type from {} to {} for Contract-First CQRS", 
                        param.paramName, originalType, param.dataType);
                }
            }
        }

        // Add MediatR-specific vendor extensions for template generation
        if (useMediatr) {
            String mediatrResponseType = getMediatrResponseType(operation);
            operation.vendorExtensions.put("mediatrResponseType", mediatrResponseType);
            operation.vendorExtensions.put("isUnit", "Unit".equals(mediatrResponseType));
            
            // Add DTO response type for Contract-First CQRS (FR-027)
            String dtoResponseType = getResponseDtoType(operation);
            operation.vendorExtensions.put("dtoResponseType", dtoResponseType);
            
            // For DELETE operations, set returnType to bool so template conditions work
            if ("DELETE".equalsIgnoreCase(operation.httpMethod) && "bool".equals(mediatrResponseType)) {
                operation.returnType = "bool";
            }
            
            // Determine if operation is a command (mutation) or query (read)
            boolean isQuery = "GET".equalsIgnoreCase(operation.httpMethod) || "Get".equals(operation.httpMethod);
            operation.vendorExtensions.put("isQuery", isQuery);
            operation.vendorExtensions.put("isCommand", !isQuery);
            
            // Mark POST operations for Created (201) response
            boolean isPost = "POST".equalsIgnoreCase(operation.httpMethod);
            operation.vendorExtensions.put("x-is-post-operation", isPost);
            
            // Mark DELETE operations that return bool (success/failure indicator)
            boolean isDeleteWithBool = "DELETE".equalsIgnoreCase(operation.httpMethod) && 
                                       "bool".equals(mediatrResponseType);
            operation.vendorExtensions.put("x-is-delete-with-bool", isDeleteWithBool);
            
            // Check if operation has complex query parameters (needs HttpContext)
            boolean hasComplexQueryParam = operation.allParams != null && operation.allParams.stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.vendorExtensions.get("x-is-complex-query-param")));
            operation.vendorExtensions.put("hasComplexQueryParam", hasComplexQueryParam);
            
            if (isQuery) {
                String queryClassName = getQueryClassName(operation.operationId);
                operation.vendorExtensions.put("queryClassName", queryClassName);
                operation.vendorExtensions.put("requestClassName", queryClassName);
                operation.vendorExtensions.put("handlerClassName", getHandlerClassName(queryClassName));
            } else {
                String commandClassName = getCommandClassName(operation.operationId);
                operation.vendorExtensions.put("commandClassName", commandClassName);
                operation.vendorExtensions.put("requestClassName", commandClassName);
                operation.vendorExtensions.put("handlerClassName", getHandlerClassName(commandClassName));
            }
            
            LOGGER.info("Added MediatR vendor extensions for operation '{}': type={}, response={}", 
                operation.operationId, isQuery ? "Query" : "Command", mediatrResponseType);
        }

        // BUG-001: Build a clean path for C# string interpolations.
        // OpenAPI path params may use kebab-case (e.g. {object-id}) which is invalid
        // inside a C# $"..." interpolation hole.  Replace each {baseName} placeholder
        // with the sanitised C# camelCase paramName (e.g. {objectId}).
        String cleanPath = operation.path;
        if (operation.pathParams != null) {
            for (CodegenParameter param : operation.pathParams) {
                if (!param.baseName.equals(param.paramName)) {
                    cleanPath = cleanPath.replace("{" + param.baseName + "}", "{" + param.paramName + "}");
                }
            }
        }
        operation.vendorExtensions.put("cleanPath", cleanPath);
    }
    
    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        // Let base class do its standard work (generating the grouped API files)
        OperationsMap results = super.postProcessOperationsWithModels(objs, allModels);
        
        // Only generate MediatR files if feature is enabled
        if (!useMediatr) {
            return results;
        }
        
        // Get the operations for this tag group
        OperationMap operations = results.getOperations();
        List<CodegenOperation> opList = operations.getOperation();
        
        if (opList == null || opList.isEmpty()) {
            return results;
        }
        
        LOGGER.info("Generating MediatR files for {} operations (T009)", opList.size());
        
        // Setup Mustache compiler once
        Mustache.Compiler compiler = Mustache.compiler().defaultValue("");
        
        // Track unique DTOs to generate (keyed by schema name)
        Map<String, Map<String, Object>> dtosToGenerate = new HashMap<>();
        
        // Generate files for each operation
        for (CodegenOperation op : opList) {
            try {
                // Process DTOs for this operation (T019-T021)
                if (op.bodyParam != null) {
                    String dtoName = getDtoNameFromOperation(op);
                    op.vendorExtensions.put("dtoClassName", dtoName);
                    
                    // Mark body parameter as DTO (both in bodyParam and allParams)
                    op.bodyParam.vendorExtensions.put("isDtoParam", true);
                    op.bodyParam.vendorExtensions.put("dtoType", dtoName);
                    
                    // Enable validation for body parameters when validators are enabled
                    if (useValidators) {
                        op.bodyParam.hasValidation = true;
                    }
                    
                    // Also mark in allParams list (bodyParam and allParams are separate)
                    if (op.allParams != null) {
                        for (CodegenParameter param : op.allParams) {
                            if (param.isBodyParam) {
                                param.vendorExtensions.put("isDtoParam", true);
                                param.vendorExtensions.put("dtoType", dtoName);
                                
                                // Enable validation for body parameters when validators are enabled
                                if (useValidators) {
                                    param.hasValidation = true;
                                }
                            }
                        }
                    }
                    
                    // Collect DTO for generation (deduplicate by name)
                    if (!dtosToGenerate.containsKey(dtoName)) {
                        Map<String, Object> dtoData = prepareDtoData(op, dtoName, allModels);
                        dtosToGenerate.put(dtoName, dtoData);
                        
                        // Also collect nested DTOs (e.g., CategoryDto, TagDto)
                        collectNestedDtos(dtoData, allModels, dtosToGenerate);
                    }
                }
                
                generateMediatrFilesForOperation(compiler, op, results, allModels);
            } catch (Exception e) {
                LOGGER.error("Failed to generate MediatR files for operation '{}': {}", 
                    op.operationId, e.getMessage(), e);
            }
        }
        
        // Generate DTO files
        for (Map<String, Object> dtoData : dtosToGenerate.values()) {
            try {
                writeDtoFile(compiler, dtoData);
            } catch (Exception e) {
                LOGGER.error("Failed to generate DTO file for '{}': {}", 
                    dtoData.get("classname"), e.getMessage(), e);
            }
        }
        
        // Generate Validator files if useValidators is enabled (T032-T033)
        if (useValidators) {
            for (Map<String, Object> dtoData : dtosToGenerate.values()) {
                try {
                    writeValidatorFile(compiler, dtoData);
                } catch (Exception e) {
                    LOGGER.error("Failed to generate Validator file for '{}': {}", 
                        dtoData.get("classname"), e.getMessage(), e);
                }
            }
        }
        
        // Generate response DTOs from all models (FR-027: Commands/Queries return DTO types)
        generateResponseDtos(compiler, allModels);
        
        return results;
    }
    
    /**
     * Generate DTO class name from operation (e.g., "AddPetDto" from addPet operation).
     */
    private String getDtoNameFromOperation(CodegenOperation operation) {
        String operationName = toModelName(operation.operationId);
        return operationName + "Dto";
    }
    
    /**
     * Prepare template data for DTO generation from operation's requestBody schema.
     * Converts Model property types to DTO types for complete decoupling.
     */
    private Map<String, Object> prepareDtoData(CodegenOperation operation, String dtoName, List<ModelMap> allModels) {
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("classname", dtoName);
        data.put("operationId", operation.operationId);
        data.put("description", operation.summary);
        
        // Get properties from the body parameter's model
        if (operation.bodyParam != null) {
            // Use bodyParam's data type to look up model properties
            String bodyType = operation.bodyParam.dataType;
            
            // Find the model in allModels
            for (ModelMap modelMap : allModels) {
                if (modelMap.getModel() != null && 
                    modelMap.getModel().getClassname().equals(bodyType)) {
                    // Clone and convert model properties for DTO (convert complex types to DTO types)
                    List<CodegenProperty> dtoVars = new ArrayList<>();
                    for (CodegenProperty prop : modelMap.getModel().getVars()) {
                        CodegenProperty dtoProp = prop.clone();
                        
                        // Strip regex delimiters from pattern (e.g., "/^pattern$/" -> "^pattern$")
                        if (dtoProp.pattern != null && dtoProp.pattern.startsWith("/") && dtoProp.pattern.endsWith("/")) {
                            dtoProp.pattern = dtoProp.pattern.substring(1, dtoProp.pattern.length() - 1);
                        }
                        
                        // Convert complex types to DTO equivalents
                        if (prop.complexType != null && !prop.isContainer) {
                            // Single complex type: Category -> CategoryDto
                            dtoProp.dataType = prop.complexType + "Dto";
                            dtoProp.datatypeWithEnum = prop.complexType + "Dto";
                        } else if (prop.isContainer && prop.complexType != null) {
                            // Collection of complex types: List<Tag> -> List<TagDto>
                            dtoProp.dataType = dtoProp.dataType.replace(prop.complexType, prop.complexType + "Dto");
                            dtoProp.datatypeWithEnum = dtoProp.datatypeWithEnum.replace(prop.complexType, prop.complexType + "Dto");
                        }
                        dtoVars.add(dtoProp);
                    }
                    data.put("vars", dtoVars);
                    data.put("hasVars", !dtoVars.isEmpty());
                    break;
                }
            }
        }
        
        return data;
    }
    
    /**
     * Recursively collect nested DTOs that need to be generated (e.g., CategoryDto, TagDto).
     * This ensures complete decoupling - DTOs never reference Model types.
     */
    private void collectNestedDtos(Map<String, Object> parentDtoData, List<ModelMap> allModels, 
                                   Map<String, Map<String, Object>> dtosToGenerate) {
        @SuppressWarnings("unchecked")
        List<CodegenProperty> vars = (List<CodegenProperty>) parentDtoData.get("vars");
        if (vars == null) return;
        
        for (CodegenProperty prop : vars) {
            String nestedModelName = null;
            
            // Check if this property references a complex type
            if (prop.complexType != null && !prop.isContainer) {
                // Single complex type: CategoryDto needs Category model
                nestedModelName = prop.complexType;
            } else if (prop.isContainer && prop.complexType != null) {
                // Collection of complex types: List<TagDto> needs Tag model
                nestedModelName = prop.complexType;
            }
            
            // Generate nested DTO if not already tracked
            if (nestedModelName != null) {
                String nestedDtoName = nestedModelName + "Dto";
                if (!dtosToGenerate.containsKey(nestedDtoName)) {
                    // Find the nested model and generate its DTO
                    for (ModelMap modelMap : allModels) {
                        if (modelMap.getModel() != null && 
                            modelMap.getModel().getClassname().equals(nestedModelName)) {
                            // Create DTO data for nested model
                            Map<String, Object> nestedDtoData = new HashMap<>();
                            nestedDtoData.put("packageName", packageName);
                            nestedDtoData.put("classname", nestedDtoName);
                            nestedDtoData.put("description", modelMap.getModel().getDescription());
                            
                            // Convert nested model properties to DTO types
                            List<CodegenProperty> nestedDtoVars = new ArrayList<>();
                            for (CodegenProperty nestedProp : modelMap.getModel().getVars()) {
                                CodegenProperty nestedDtoProp = nestedProp.clone();
                                
                                // Strip regex delimiters from pattern (e.g., "/^pattern$/" -> "^pattern$")
                                if (nestedDtoProp.pattern != null && nestedDtoProp.pattern.startsWith("/") && nestedDtoProp.pattern.endsWith("/")) {
                                    nestedDtoProp.pattern = nestedDtoProp.pattern.substring(1, nestedDtoProp.pattern.length() - 1);
                                }
                                
                                // Recursively convert complex types
                                if (nestedProp.complexType != null && !nestedProp.isContainer) {
                                    nestedDtoProp.dataType = nestedProp.complexType + "Dto";
                                    nestedDtoProp.datatypeWithEnum = nestedProp.complexType + "Dto";
                                } else if (nestedProp.isContainer && nestedProp.complexType != null) {
                                    nestedDtoProp.dataType = nestedDtoProp.dataType.replace(nestedProp.complexType, nestedProp.complexType + "Dto");
                                    nestedDtoProp.datatypeWithEnum = nestedDtoProp.datatypeWithEnum.replace(nestedProp.complexType, nestedProp.complexType + "Dto");
                                }
                                nestedDtoVars.add(nestedDtoProp);
                            }
                            nestedDtoData.put("vars", nestedDtoVars);
                            nestedDtoData.put("hasVars", !nestedDtoVars.isEmpty());
                            
                            dtosToGenerate.put(nestedDtoName, nestedDtoData);
                            
                            // Recursively process nested DTOs (e.g., if Category has a nested object)
                            collectNestedDtos(nestedDtoData, allModels, dtosToGenerate);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Write DTO file to disk using dto.mustache template.
     */
    private void writeDtoFile(Mustache.Compiler compiler, Map<String, Object> dtoData) throws Exception {
        String dtoName = (String) dtoData.get("classname");
        writeMediatrFile(compiler, "dto.mustache", dtoData, "DTOs", dtoName + ".cs");
        LOGGER.info("Generated DTO file: DTOs/{}.cs", dtoName);
    }
    
    /**
     * Write Validator file to disk using dtoValidator.mustache template.
     * Generates FluentValidation validators for DTOs with comprehensive constraint support.
     * (T032-T033)
     */
    private void writeValidatorFile(Mustache.Compiler compiler, Map<String, Object> dtoData) throws Exception {
        String dtoName = (String) dtoData.get("classname");
        String validatorName = dtoName + "Validator";
        writeMediatrFile(compiler, "dtoValidator.mustache", dtoData, "Validators", validatorName + ".cs");
        LOGGER.info("Generated Validator file: Validators/{}.cs", validatorName);
    }
    
    /**
     * Generate response DTOs from all models in the OpenAPI specification.
     * This ensures every Model has a corresponding DTO for use in Command/Query response types.
     * FR-027: Commands/Queries must return DTO types (PetDto, OrderDto, UserDto) not Model types.
     * 
     * @param compiler The Mustache compiler instance
     * @param allModels List of all models from the OpenAPI spec
     */
    private void generateResponseDtos(Mustache.Compiler compiler, List<ModelMap> allModels) {
        if (allModels == null || allModels.isEmpty()) {
            return;
        }
        
        LOGGER.info("Generating response DTOs for {} models", allModels.size());
        
        for (ModelMap modelMap : allModels) {
            if (modelMap.getModel() == null) {
                continue;
            }
            
            CodegenModel model = modelMap.getModel();
            String modelName = model.getClassname();
            String dtoName = modelName + "Dto";
            
            try {
                // Prepare DTO data from model
                Map<String, Object> dtoData = new HashMap<>();
                dtoData.put("packageName", packageName);
                dtoData.put("classname", dtoName);
                dtoData.put("description", model.getDescription());
                dtoData.put("isEnum", model.isEnum);
                dtoData.put("allowableValues", model.allowableValues);
                dtoData.put("isString", model.isString);
                
                // Clone and convert model properties for DTO
                List<CodegenProperty> dtoVars = new ArrayList<>();
                for (CodegenProperty prop : model.getVars()) {
                    CodegenProperty dtoProp = prop.clone();
                    
                    // Strip regex delimiters from pattern
                    if (dtoProp.pattern != null && dtoProp.pattern.startsWith("/") && dtoProp.pattern.endsWith("/")) {
                        dtoProp.pattern = dtoProp.pattern.substring(1, dtoProp.pattern.length() - 1);
                    }
                    
                    // Convert complex types to DTO equivalents
                    if (prop.complexType != null && !prop.isContainer) {
                        // Single complex type: Category -> CategoryDto
                        dtoProp.dataType = prop.complexType + "Dto";
                        dtoProp.datatypeWithEnum = prop.complexType + "Dto";
                    } else if (prop.isContainer && prop.complexType != null) {
                        // Collection of complex types: List<Tag> -> List<TagDto>
                        dtoProp.dataType = dtoProp.dataType.replace(prop.complexType, prop.complexType + "Dto");
                        dtoProp.datatypeWithEnum = dtoProp.datatypeWithEnum.replace(prop.complexType, prop.complexType + "Dto");
                    }
                    dtoVars.add(dtoProp);
                }
                dtoData.put("vars", dtoVars);
                dtoData.put("hasVars", !dtoVars.isEmpty());
                
                // Generate DTO file
                writeDtoFile(compiler, dtoData);
                LOGGER.info("Generated response DTO: {}.cs", dtoName);
                
            } catch (Exception e) {
                LOGGER.error("Failed to generate response DTO for model '{}': {}", modelName, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Generate command/query and handler files for a single operation using Mustache template engine.
     * This is the proper T009 implementation - files get full operation data context.
     * 
     * @param compiler The Mustache compiler instance
     * @param operation The operation to generate files for
     * @param objs The operations map containing shared context (packageName, imports, etc.)
     */
    private void generateMediatrFilesForOperation(Mustache.Compiler compiler, CodegenOperation operation,
                                                   OperationsMap objs, List<ModelMap> allModels) throws Exception {
        Boolean isQuery = (Boolean) operation.vendorExtensions.get("isQuery");
        String requestClassName = (String) operation.vendorExtensions.get("requestClassName");
        String handlerClassName = (String) operation.vendorExtensions.get("handlerClassName");
        
        if (requestClassName == null) {
            LOGGER.warn("Skipping MediatR file generation for operation '{}' - missing class names", operation.operationId);
            return;
        }
        
        // Prepare template data - merge operation data with shared context
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("operation", operation);
        data.put("operationId", operation.operationId);
        data.put("commandClassName", operation.vendorExtensions.get("commandClassName"));
        data.put("queryClassName", operation.vendorExtensions.get("queryClassName"));
        data.put("requestClassName", requestClassName);
        data.put("handlerClassName", handlerClassName);
        data.put("mediatrResponseType", operation.vendorExtensions.get("mediatrResponseType"));
        data.put("dtoResponseType", operation.vendorExtensions.get("dtoResponseType"));
        data.put("returnType", operation.returnType);
        data.put("returnBaseType", operation.returnBaseType);
        data.put("isUnit", operation.vendorExtensions.get("isUnit"));
        data.put("allParams", operation.allParams);
        data.put("queryParams", operation.queryParams);
        data.put("pathParams", operation.pathParams);
        data.put("headerParams", operation.headerParams);
        data.put("bodyParam", operation.bodyParam);
        data.put("hasBodyParam", operation.getHasBodyParam());
        data.put("description", operation.summary);
        // Add handler implementation data (property-by-property mapping code, enum switch methods)
        Map<String, Object> handlerData = prepareHandlerData(operation, allModels);
        data.putAll(handlerData);
        
        // Determine template and folder based on operation type
        String requestTemplate = (isQuery != null && isQuery) ? "query.mustache" : "command.mustache";
        String requestFolder = (isQuery != null && isQuery) ? "Queries" : "Commands";
        
        // Generate command/query file
        String requestFile = requestClassName + ".cs";
        writeMediatrFile(compiler, requestTemplate, data, requestFolder, requestFile);
        LOGGER.info("Generated {} file: {}/{}", isQuery ? "Query" : "Command", requestFolder, requestFile);
        
        // Generate handler file (with existence check per R4)
        String handlerFolder = "Handlers";
        String handlerFile = handlerClassName + ".cs";
        String handlerPath = outputFolder + File.separator + generatedFolder + 
            File.separator + handlerFolder + File.separator + handlerFile;
        
        File handlerFileObj = new File(handlerPath);
        if (!handlerFileObj.exists()) {
            writeMediatrFile(compiler, "handler.mustache", data, handlerFolder, handlerFile);
            LOGGER.info("Generated handler file: {}/{}", handlerFolder, handlerFile);
        } else {
            LOGGER.info("Skipping handler '{}' - already exists", handlerFile);
        }
    }
    
    // =========================================================================
    // Handler implementation data helpers (property mapping, enum switch code)
    // =========================================================================

    /** Simple holder for one enum mapping method's parameters */
    private static class EnumMappingInfo {
        final String methodName;
        final String sourceType;
        final boolean isSourceNullable;
        final String targetType;
        final String defaultValue;
        final List<String> enumNames;

        EnumMappingInfo(String methodName, String sourceType, boolean isSourceNullable,
                        String targetType, String defaultValue, List<String> enumNames) {
            this.methodName = methodName;
            this.sourceType = sourceType;
            this.isSourceNullable = isSourceNullable;
            this.targetType = targetType;
            this.defaultValue = defaultValue;
            this.enumNames = enumNames;
        }
    }

    /**
     * Prepare handler implementation data for the handler.mustache template.
     * Builds DTO↔Model property mapping code and enum switch helper methods as
     * pre-rendered strings so Mustache just outputs them verbatim via {{{...}}}.
     */
    private Map<String, Object> prepareHandlerData(CodegenOperation operation, List<ModelMap> allModels) {
        Map<String, Object> data = new HashMap<>();

        Boolean isUnit = Boolean.TRUE.equals(operation.vendorExtensions.get("isUnit"));
        String dtoResponseType = (String) operation.vendorExtensions.get("dtoResponseType");
        boolean isBool = "bool".equals(dtoResponseType);
        boolean isCollection = dtoResponseType != null && dtoResponseType.startsWith("IEnumerable<");
        boolean isDeleteWithBool = Boolean.TRUE.equals(operation.vendorExtensions.get("x-is-delete-with-bool"));

        data.put("isDeleteWithBool", isDeleteWithBool ? Boolean.TRUE : null);

        // Accumulate enum mapping methods (deduplicated by key)
        Map<String, EnumMappingInfo> dtoToModelEnumMappings = new LinkedHashMap<>();
        Map<String, EnumMappingInfo> modelToDtoEnumMappings = new LinkedHashMap<>();

        // --- Body param: DTO → Model mapping ---
        if (operation.bodyParam != null && !isUnit) {
            String dtoClassName = (String) operation.bodyParam.vendorExtensions.get("dtoType");
            String modelClassName = operation.bodyParam.dataType;
            String paramName = operation.bodyParam.paramName;
            CodegenModel bodyModel = findModelByName(modelClassName, allModels);
            if (bodyModel != null && dtoClassName != null) {
                String body = buildDtoToModelBody(bodyModel, dtoClassName, modelClassName, allModels, dtoToModelEnumMappings);
                data.put("dtoToModelBody", body);
                data.put("bodyModelClassName", modelClassName);
                data.put("bodyDtoClassName", dtoClassName);
                data.put("bodyParamName", paramName);
                data.put("hasDtoToModelMapping", Boolean.TRUE);
            }
        }

        // --- Response model: Model → DTO mapping ---
        if (!isUnit && !isBool && operation.returnBaseType != null && !isPrimitiveType(operation.returnBaseType)) {
            String responseModelName = operation.returnBaseType;
            String responseDtoName = responseModelName + "Dto";
            CodegenModel responseModel = findModelByName(responseModelName, allModels);
            if (responseModel != null) {
                String body = buildModelToDtoBody(responseModel, responseModelName, responseDtoName, allModels, modelToDtoEnumMappings);
                data.put("modelToDtoBody", body);
                data.put("responseModelClassName", responseModelName);
                data.put("responseDtoClassName", responseDtoName);
                data.put("isCollection", isCollection ? Boolean.TRUE : null);
                data.put("hasModelToDtoMapping", Boolean.TRUE);
            }
        }

        // --- Build enum mapping methods ---
        StringBuilder enumMethods = new StringBuilder();
        for (EnumMappingInfo m : dtoToModelEnumMappings.values()) {
            enumMethods.append("\n").append(buildEnumSwitchMethod(m));
        }
        for (EnumMappingInfo m : modelToDtoEnumMappings.values()) {
            enumMethods.append("\n").append(buildEnumSwitchMethod(m));
        }

        // --- Array enum query params: Dto[] → Model[] conversion helpers ---
        // Handlers own the DTO-to-Model mapping; these helpers keep the Contracts layer
        // (Queries/Commands) free of any Models references (FR-027).
        Set<String> arrayEnumHelpersSeen = new HashSet<>();
        if (operation.queryParams != null) {
            for (CodegenParameter param : operation.queryParams) {
                if (param.isContainer && param.isArray && param.items != null
                        && param.items.complexType != null) {
                    String innerType = param.items.dataType;
                    if (arrayEnumHelpersSeen.add(innerType)) {
                        String capitalizedInner = Character.toUpperCase(innerType.charAt(0)) + innerType.substring(1);
                        String methodName = "Map" + capitalizedInner + "DtoArrayToModel";
                        enumMethods.append("\n    private static ").append(innerType).append("[]? ")
                            .append(methodName).append("(").append(innerType).append("Dto[]? dtos)")
                            .append("\n        => dtos?.Select(d => (").append(innerType).append(")(int)d).ToArray();\n");
                    }
                }
            }
        }

        if (enumMethods.length() > 0) {
            data.put("enumMappingMethods", enumMethods.toString());
            data.put("hasEnumMappingMethods", Boolean.TRUE);
        }

        return data;
    }

    /** Find a CodegenModel by its class name in the allModels list. */
    private CodegenModel findModelByName(String className, List<ModelMap> allModels) {
        if (className == null || allModels == null) return null;
        for (ModelMap mm : allModels) {
            if (mm.getModel() != null && className.equals(mm.getModel().getClassname())) {
                return mm.getModel();
            }
        }
        return null;
    }

    /**
     * Build the body of the static MapDtoToDomain(dtoClassName dto) method.
     * Returns new ModelType { Prop1 = dto.Prop1, ... };
     */
    private String buildDtoToModelBody(CodegenModel model, String dtoType, String modelType,
                                       List<ModelMap> allModels,
                                       Map<String, EnumMappingInfo> enumMappings) {
        StringBuilder sb = new StringBuilder();
        sb.append("        return new ").append(modelType).append("\n        {\n");
        for (CodegenProperty prop : model.getVars()) {
            sb.append("            ").append(prop.name).append(" = ");
            if (prop.isEnum && !prop.isContainer && prop.complexType != null) {
                // $ref enum — cast directly, no switch method needed
                sb.append("(").append(prop.complexType).append(")(").append("int)dto.").append(prop.name);
            } else if (prop.isEnum && !prop.isContainer && prop.complexType == null) {
                List<String> names = getEnumNames(prop);
                String defaultVal = names.isEmpty() ? "default" : modelType + "." + prop.datatypeWithEnum + "." + names.get(0);
                String methodName = "Map" + prop.name + "DtoToModel";
                enumMappings.computeIfAbsent(prop.name + "_dtoToModel", k -> new EnumMappingInfo(
                    methodName, dtoType + "." + prop.datatypeWithEnum, true,
                    modelType + "." + prop.datatypeWithEnum, defaultVal, names));
                if (prop.required) {
                    // BUG-006: Non-nullable enum — call the mapper directly (C# allows T -> T? coercion)
                    sb.append(methodName).append("(dto.").append(prop.name).append(")");
                } else {
                    sb.append("dto.").append(prop.name).append(".HasValue ? ")
                      .append(methodName).append("(dto.").append(prop.name).append(".Value) : ").append(defaultVal);
                }
            } else if (prop.complexType != null && !prop.isContainer) {
                sb.append(buildNestedObjectDtoToModel(prop, allModels));
            } else if (prop.isContainer && prop.complexType != null) {
                sb.append(buildCollectionDtoToModel(prop, allModels));
            } else if (isNullableValueInModel(prop)) {
                sb.append("dto.").append(prop.name).append(" ?? ").append(getZeroValue(prop));
            } else {
                sb.append("dto.").append(prop.name);
            }
            sb.append(",\n");
        }
        sb.append("        };");
        return sb.toString();
    }

    /**
     * Build the body of the static MapDomainToDto(modelClassName model) method.
     * Returns new DtoType { Prop1 = model.Prop1, ... };
     */
    private String buildModelToDtoBody(CodegenModel model, String modelType, String dtoType,
                                       List<ModelMap> allModels,
                                       Map<String, EnumMappingInfo> enumMappings) {
        StringBuilder sb = new StringBuilder();
        sb.append("        return new ").append(dtoType).append("\n        {\n");
        for (CodegenProperty prop : model.getVars()) {
            sb.append("            ").append(prop.name).append(" = ");
            if (prop.isEnum && !prop.isContainer && prop.complexType != null) {
                // $ref enum — cast directly, no switch method needed
                sb.append("(").append(prop.complexType).append("Dto)(").append("int)model.").append(prop.name);
            } else if (prop.isEnum && !prop.isContainer && prop.complexType == null) {
                List<String> names = getEnumNames(prop);
                String defaultVal = names.isEmpty() ? "default" : dtoType + "." + prop.datatypeWithEnum + "." + names.get(0);
                String methodName = "Map" + prop.name + "ModelToDto";
                enumMappings.computeIfAbsent(prop.name + "_modelToDto", k -> new EnumMappingInfo(
                    methodName, modelType + "." + prop.datatypeWithEnum, false,
                    dtoType + "." + prop.datatypeWithEnum, defaultVal, names));
                sb.append(methodName).append("(model.").append(prop.name).append(")");
            } else if (prop.complexType != null && !prop.isContainer) {
                sb.append(buildNestedObjectModelToDto(prop, allModels));
            } else if (prop.isContainer && prop.complexType != null) {
                sb.append(buildCollectionModelToDto(prop, allModels));
            } else {
                sb.append("model.").append(prop.name);
            }
            sb.append(",\n");
        }
        sb.append("        };");
        return sb.toString();
    }

    /** DTO→Model for a single nested object property: dto.Prop != null ? new ModelType { ... } : null */
    private String buildNestedObjectDtoToModel(CodegenProperty prop, List<ModelMap> allModels) {
        CodegenModel nested = findModelByName(prop.complexType, allModels);
        StringBuilder sb = new StringBuilder();
        sb.append("dto.").append(prop.name).append(" != null ? new ").append(prop.complexType).append(" { ");
        if (nested != null) {
            List<CodegenProperty> vars = nested.getVars();
            for (int i = 0; i < vars.size(); i++) {
                CodegenProperty p = vars.get(i);
                sb.append(p.name).append(" = ");
                if (p.isContainer && p.complexType != null) {
                    // nested list inside a nested object — emit null stub
                    sb.append("null");
                } else {
                    sb.append("dto.").append(prop.name).append(".").append(p.name);
                    if (isNullableValueInModel(p)) sb.append(" ?? ").append(getZeroValue(p));
                }
                if (i < vars.size() - 1) sb.append(", ");
            }
        }
        sb.append(" } : null");
        return sb.toString();
    }

    /** DTO→Model for a collection property: dto.Prop?.Select(x => new ModelType { ... }).ToList() */
    private String buildCollectionDtoToModel(CodegenProperty prop, List<ModelMap> allModels) {
        CodegenModel nested = findModelByName(prop.complexType, allModels);
        String iterVar = prop.complexType.substring(0, 1).toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("dto.").append(prop.name).append("?.Select(").append(iterVar).append(" => new ").append(prop.complexType).append(" { ");
        if (nested != null) {
            List<CodegenProperty> vars = nested.getVars();
            for (int i = 0; i < vars.size(); i++) {
                CodegenProperty p = vars.get(i);
                sb.append(p.name).append(" = ");
                if (p.isEnum && !p.isContainer) {
                    // BUG-005: qualify inline enum with enclosing model type
                    String modelEnumType = p.complexType != null ? p.complexType : prop.complexType + "." + p.datatypeWithEnum;
                    sb.append("(").append(modelEnumType).append(")(").append("int)").append(iterVar).append(".").append(p.name);
                } else if (p.complexType != null && !p.isContainer) {
                    String nestedModelType2 = p.complexType;
                    CodegenModel deepNested = findModelByName(p.complexType, allModels);
                    sb.append(iterVar).append(".").append(p.name).append(" != null ? new ").append(nestedModelType2).append(" { ");
                    if (deepNested != null) {
                        List<CodegenProperty> deepVars = deepNested.getVars();
                        for (int j = 0; j < deepVars.size(); j++) {
                            CodegenProperty dp = deepVars.get(j);
                            sb.append(dp.name).append(" = ");
                            if (dp.isContainer && dp.complexType != null) {
                                sb.append("null"); // null stub for deep nested list
                            } else {
                                sb.append(iterVar).append(".").append(p.name).append(".").append(dp.name);
                                if (isNullableValueInModel(dp)) sb.append(" ?? ").append(getZeroValue(dp));
                            }
                            if (j < deepVars.size() - 1) sb.append(", ");
                        }
                    }
                    sb.append(" } : new ").append(nestedModelType2).append("()");
                } else if (p.isContainer) {
                    // BUG-004/007: nested collection — emit null stub; Impl uses MapRecursive
                    sb.append("null");
                } else {
                    sb.append(iterVar).append(".").append(p.name);
                    if (isNullableValueInModel(p)) sb.append(" ?? ").append(getZeroValue(p));
                }
                if (i < vars.size() - 1) sb.append(", ");
            }
        }
        sb.append(" }).ToList()");
        return sb.toString();
    }

    /** Model→DTO for a single nested object property: model.Prop != null ? new DtoType { ... } : null */
    private String buildNestedObjectModelToDto(CodegenProperty prop, List<ModelMap> allModels) {
        CodegenModel nested = findModelByName(prop.complexType, allModels);
        String nestedDtoType = prop.complexType + "Dto";
        StringBuilder sb = new StringBuilder();
        sb.append("model.").append(prop.name).append(" != null ? new ").append(nestedDtoType).append(" { ");
        if (nested != null) {
            List<CodegenProperty> vars = nested.getVars();
            for (int i = 0; i < vars.size(); i++) {
                CodegenProperty p = vars.get(i);
                sb.append(p.name).append(" = ");
                if (p.isContainer && p.complexType != null) {
                    // nested list inside a nested object — emit null stub
                    sb.append("null");
                } else {
                    sb.append("model.").append(prop.name).append(".").append(p.name);
                }
                if (i < vars.size() - 1) sb.append(", ");
            }
        }
        sb.append(" } : null");
        return sb.toString();
    }

    /** Model→DTO for a collection property: model.Prop?.Select(x => new DtoType { ... }).ToList() */
    private String buildCollectionModelToDto(CodegenProperty prop, List<ModelMap> allModels) {
        CodegenModel nested = findModelByName(prop.complexType, allModels);
        String nestedDtoType = prop.complexType + "Dto";
        String iterVar = prop.complexType.substring(0, 1).toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("model.").append(prop.name).append("?.Select(").append(iterVar).append(" => new ").append(nestedDtoType).append(" { ");
        if (nested != null) {
            List<CodegenProperty> vars = nested.getVars();
            for (int i = 0; i < vars.size(); i++) {
                CodegenProperty p = vars.get(i);
                sb.append(p.name).append(" = ");
                if (p.isEnum && !p.isContainer) {
                    // BUG-005: qualify inline enum with enclosing DTO type
                    String dtoEnumType = p.complexType != null ? p.complexType + "Dto" : nestedDtoType + "." + p.datatypeWithEnum;
                    sb.append("(").append(dtoEnumType).append(")(").append("int)").append(iterVar).append(".").append(p.name);
                } else if (p.complexType != null && !p.isContainer) {
                    String nestedDtoType2 = p.complexType + "Dto";
                    CodegenModel deepNested = findModelByName(p.complexType, allModels);
                    sb.append(iterVar).append(".").append(p.name).append(" != null ? new ").append(nestedDtoType2).append(" { ");
                    if (deepNested != null) {
                        List<CodegenProperty> deepVars = deepNested.getVars();
                        for (int j = 0; j < deepVars.size(); j++) {
                            CodegenProperty dp = deepVars.get(j);
                            sb.append(dp.name).append(" = ");
                            if (dp.isContainer && dp.complexType != null) {
                                sb.append("null"); // null stub for deep nested list
                            } else {
                                sb.append(iterVar).append(".").append(p.name).append(".").append(dp.name);
                            }
                            if (j < deepVars.size() - 1) sb.append(", ");
                        }
                    }
                    sb.append(" } : new ").append(nestedDtoType2).append("()");
                } else if (p.isContainer) {
                    // BUG-004/007: nested collection — emit null stub; Impl uses MapRecursive
                    sb.append("null");
                } else {
                    sb.append(iterVar).append(".").append(p.name);
                }
                if (i < vars.size() - 1) sb.append(", ");
            }
        }
        sb.append(" }).ToList()");
        return sb.toString();
    }

    /** Build a static enum switch expression method as a C# code string. */
    private String buildEnumSwitchMethod(EnumMappingInfo m) {
        StringBuilder sb = new StringBuilder();
        String paramType = m.isSourceNullable ? m.sourceType + "?" : m.sourceType;
        sb.append("    private static ").append(m.targetType).append(" ").append(m.methodName)
          .append("(").append(paramType).append(" source)\n    {\n");
        if (m.isSourceNullable) {
            sb.append("        if (!source.HasValue) return ").append(m.defaultValue).append(";\n");
            sb.append("        return source.Value switch\n        {\n");
        } else {
            sb.append("        return source switch\n        {\n");
        }
        for (String name : m.enumNames) {
            sb.append("            ").append(m.sourceType).append(".").append(name)
              .append(" => ").append(m.targetType).append(".").append(name).append(",\n");
        }
        sb.append("            _ => ").append(m.defaultValue).append(",\n");
        sb.append("        };\n    }");
        return sb.toString();
    }

    /**
     * Extract the list of enum value names from a CodegenProperty's allowableValues.
     * Returns e.g. ["AvailableEnum", "PendingEnum", "SoldEnum"].
     */
    @SuppressWarnings("unchecked")
    private List<String> getEnumNames(CodegenProperty prop) {
        List<String> names = new ArrayList<>();
        if (prop.allowableValues == null) return names;
        Object enumVarsObj = prop.allowableValues.get("enumVars");
        if (!(enumVarsObj instanceof List)) return names;
        for (Map<?, ?> enumVar : (List<Map<?, ?>>) enumVarsObj) {
            Object name = enumVar.get("name");
            if (name != null) names.add(name.toString());
        }
        return names;
    }

    /**
     * True if the model property should be handled with null-coalescing when mapped from a nullable DTO field.
     * Applies to non-required value types (struct types that cannot be null in the model).
     */
    private boolean isNullableValueInModel(CodegenProperty prop) {
        if (prop.required || prop.dataType == null) return false;
        String t = prop.dataType.replace("?", "");
        return t.equals("long") || t.equals("int") || t.equals("double")
            || t.equals("float") || t.equals("decimal") || t.equals("bool")
            || t.equals("DateTime") || t.equals("DateTimeOffset")
            || t.equals("Guid") || t.equals("TimeSpan");
    }

    /** Return the appropriate zero/default literal for a value-type property. */
    private String getZeroValue(CodegenProperty prop) {
        if (prop.dataType == null) return "default";
        String t = prop.dataType.replace("?", "");
        if (t.equals("bool")) return "false";
        if (t.equals("DateTime") || t.equals("DateTimeOffset")
                || t.equals("Guid") || t.equals("TimeSpan")) return "default";
        return "0";
    }

    /**
     * Load a template, render it with data, and write to disk.
     * 
     * @param compiler The Mustache compiler instance
     * @param templateName Template file name (e.g., "command.mustache")
     * @param data Data context for template rendering
     * @param folder Relative folder path (e.g., "Commands", "Queries", "Handlers")
     * @param filename Output filename (e.g., "AddPetCommand.cs")
     */
    private void writeMediatrFile(Mustache.Compiler compiler, String templateName, 
                                   Map<String, Object> data, String folder, String filename) throws Exception {
        // Load template from resources
        String templatePath = templateDir + File.separator + templateName;
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(templatePath);
        
        if (stream == null) {
            throw new IllegalStateException("Template not found: " + templatePath);
        }
        
        // Compile and render template
        String content = compiler.compile(new InputStreamReader(stream, StandardCharsets.UTF_8))
                                 .execute(data);
        
        // Construct output path
        // For NuGet packaging: Handlers go to Implementation (templates), everything else to Contract (package)
        String relativePath;
        if (useNugetPackaging && "Handlers".equals(folder)) {
            relativePath = sourceFolder + File.separator + packageName + File.separator + folder + File.separator + filename;
        } else {
            relativePath = generatedFolder + File.separator + folder + File.separator + filename;
        }
        File outputFile = new File(outputFolder, relativePath);
        
        // Ensure directory exists
        outputFile.getParentFile().mkdirs();
        
        // Write file
        Files.write(outputFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the MediatR response type for IRequest<TResponse> based on the operation's return type.
     * @param operation The CodegenOperation to analyze
     * @return The response type string for IRequest<T>
     */
    private String getMediatrResponseType(CodegenOperation operation) {
        if (operation.returnType == null || operation.returnType.equals("void")) {
            // DELETE operations should return bool (success/notfound)
            if (operation.httpMethod != null && operation.httpMethod.equalsIgnoreCase("DELETE")) {
                return "bool";
            }
            return "Unit"; // MediatR.Unit for other void operations
        }
        
        if (operation.returnContainer != null && operation.returnContainer.equals("array")) {
            // Array responses use IEnumerable<T>
            return "IEnumerable<" + operation.returnBaseType + ">";
        }

        // BUG-002: binary/stream responses map to FileDto
        if ("System.IO.Stream".equals(operation.returnType) ||
                (operation.returnType != null && operation.returnType.startsWith("System.IO."))) {
            return "FileDto";
        }

        // Direct model type or primitive
        return operation.returnType;
    }

    /**
     * Get the DTO response type for Contract-First CQRS architecture.
     * Commands/Queries MUST return DTO types (not Model/Domain types) to ensure
     * Contract package has zero dependencies on Implementation project.
     * 
     * FR-027: Commands and Queries return IRequest<TDto>, never IRequest<TModel>
     * 
     * @param operation The CodegenOperation to analyze
     * @return The DTO response type string for IRequest<TDto>
     */
    private String getResponseDtoType(CodegenOperation operation) {
        if (operation.returnType == null || operation.returnType.equals("void")) {
            // DELETE operations should return bool (success/notfound)
            if (operation.httpMethod != null && operation.httpMethod.equalsIgnoreCase("DELETE")) {
                return "bool";
            }
            return "Unit"; // MediatR.Unit for other void operations
        }
        
        if (operation.returnContainer != null && operation.returnContainer.equals("array")) {
            // Array responses use IEnumerable<TDto>
            // Convention: Model "Pet" -> DTO "PetDto"
            String dtoType = operation.returnBaseType;
            if (!dtoType.endsWith("Dto") && !isPrimitiveType(dtoType)) {
                dtoType = dtoType + "Dto";
            }
            return "IEnumerable<" + dtoType + ">";
        }
        
        if (operation.returnContainer != null && operation.returnContainer.equals("map")) {
            // Dictionary responses are already correct - Dictionary<string, int>
            return operation.returnType;
        }
        
        // Check if returnType is a generic type (contains < and >)
        if (operation.returnType.contains("<") && operation.returnType.contains(">")) {
            // Generic type like List<Pet> or Dictionary<string, int>
            // For List/IEnumerable of models, convert to IEnumerable<ModelDto>
            if (operation.returnType.startsWith("List<") || operation.returnType.startsWith("IEnumerable<")) {
                String innerType = extractGenericType(operation.returnType);
                if (!innerType.endsWith("Dto") && !isPrimitiveType(innerType)) {
                    innerType = innerType + "Dto";
                }
                return "IEnumerable<" + innerType + ">";
            }
            // For Dictionary and other generic types, return as-is
            return operation.returnType;
        }

        // Single object response - convert Model to DTO
        String dtoType = operation.returnType;

        // BUG-002: binary/stream responses (format: binary, type: string) are resolved
        // by the OpenAPI generator as System.IO.Stream, which must map to FileDto.
        if ("System.IO.Stream".equals(dtoType) || dtoType.startsWith("System.IO.")) {
            return "FileDto";
        }

        // Primitive types (int, string, bool, etc.) don't need Dto suffix
        if (isPrimitiveType(dtoType)) {
            return dtoType;
        }
        
        // Model types: Pet -> PetDto, Order -> OrderDto
        if (!dtoType.endsWith("Dto")) {
            dtoType = dtoType + "Dto";
        }
        
        return dtoType;
    }

    /**
     * Extract the inner type from a generic type declaration.
     * Example: "List<Pet>" -> "Pet", "Dictionary<string, int>" -> "string, int"
     */
    private String extractGenericType(String genericType) {
        int startIndex = genericType.indexOf('<');
        int endIndex = genericType.lastIndexOf('>');
        if (startIndex >= 0 && endIndex > startIndex) {
            return genericType.substring(startIndex + 1, endIndex);
        }
        return genericType;
    }

    /**
     * Check if a type is a primitive C# type that doesn't need DTO conversion.
     * @param typeName The type name to check
     * @return true if primitive, false otherwise
     */
    private boolean isPrimitiveType(String typeName) {
        if (typeName == null) {
            return false;
        }
        
        // Remove nullable marker for checking
        String baseType = typeName.replace("?", "");
        
        return baseType.equals("int") ||
               baseType.equals("long") ||
               baseType.equals("float") ||
               baseType.equals("double") ||
               baseType.equals("decimal") ||
               baseType.equals("bool") ||
               baseType.equals("string") ||
               baseType.equals("String") ||   // Java-style uppercase from OpenAPI generator internals
               baseType.equals("Integer") ||
               baseType.equals("Long") ||
               baseType.equals("Float") ||
               baseType.equals("Double") ||
               baseType.equals("Boolean") ||
               baseType.equals("byte") ||
               baseType.equals("short") ||
               baseType.equals("char") ||
               baseType.equals("DateTime") ||
               baseType.equals("DateTimeOffset") ||
               baseType.equals("Guid") ||
               baseType.equals("TimeSpan") ||
               baseType.equals("Unit");
    }

    /**
     * Generate command class name from operation ID (POST/PUT/PATCH/DELETE operations).
     * @param operationId The OpenAPI operation ID
     * @return Command class name (e.g., "AddPetCommand")
     */
    private String getCommandClassName(String operationId) {
        // Convert operationId to PascalCase and append "Command"
        // e.g., "addPet" -> "AddPetCommand", "updatePet" -> "UpdatePetCommand"
        return toModelName(operationId) + "Command";
    }

    /**
     * Generate query class name from operation ID (GET operations).
     * @param operationId The OpenAPI operation ID
     * @return Query class name (e.g., "GetPetByIdQuery")
     */
    private String getQueryClassName(String operationId) {
        // Convert operationId to PascalCase and append "Query"
        // e.g., "getPetById" -> "GetPetByIdQuery", "findPetsByStatus" -> "FindPetsByStatusQuery"
        return toModelName(operationId) + "Query";
    }

    /**
     * Generate handler class name from request class name.
     * @param requestClassName The command or query class name
     * @return Handler class name (e.g., "AddPetCommandHandler")
     */
    private String getHandlerClassName(String requestClassName) {
        // Append "Handler" to the request class name
        // e.g., "AddPetCommand" -> "AddPetCommandHandler"
        return requestClassName + "Handler";
    }
}
