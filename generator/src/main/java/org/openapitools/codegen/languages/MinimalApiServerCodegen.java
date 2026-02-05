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
    public static final String USE_AUTHENTICATION = "useAuthentication";
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
    private boolean useAuthentication = false;
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
        addSwitch(USE_AUTHENTICATION, "Enable JWT authentication.", useAuthentication);
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
        setUseAuthentication();
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
            // Also generate validators for each model with required properties
            modelTemplateFiles.put("modelValidator.mustache", "Validator.cs");
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

        if (useAuthentication) {
            supportingFiles.add(new SupportingFile("loginRequest.mustache", packageFolder + File.separator + apiPackage, "LoginRequest.cs"));
            supportingFiles.add(new SupportingFile("userLoginEndpoint.mustache", packageFolder + File.separator + apiPackage, "UserLoginEndpoint.cs"));
        }

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

    private void setUseAuthentication() {
        if (additionalProperties.containsKey(USE_AUTHENTICATION)) {
            useAuthentication = convertPropertyToBooleanAndWriteBack(USE_AUTHENTICATION);
        } else {
            additionalProperties.put(USE_AUTHENTICATION, useAuthentication);
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
                    param.dataType = innerType + "[]";
                    LOGGER.info("Converted query array parameter '{}' from List<{}> to {}[] for Minimal API compatibility", 
                        param.paramName, innerType, innerType);
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
                    param.dataType = innerType + "[]";
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
                
                generateMediatrFilesForOperation(compiler, op, results);
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
                                                   OperationsMap objs) throws Exception {
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
