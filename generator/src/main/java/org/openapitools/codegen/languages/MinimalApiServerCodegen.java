package org.openapitools.codegen.languages;

import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    public static final String USE_ROUTE_GROUPS = "useRouteGroups";
    public static final String USE_GLOBAL_EXCEPTION_HANDLER = "useGlobalExceptionHandler";
    public static final String USE_MEDIATR = "useMediatr";
    public static final String ROUTE_PREFIX = "routePrefix";
    public static final String VERSIONING_PREFIX = "versioningPrefix";
    public static final String API_VERSION = "apiVersion";
    public static final String SOLUTION_GUID = "solutionGuid";
    public static final String PROJECT_CONFIGURATION_GUID = "projectConfigurationGuid";

    private final Logger LOGGER = LoggerFactory.getLogger(MinimalApiServerCodegen.class);

    private boolean useProblemDetails = false;
    private boolean useRecords = false;
    private boolean useAuthentication = false;
    private boolean useValidators = false;
    private boolean useResponseCaching = false;
    private boolean useApiVersioning = false;
    private boolean useRouteGroups = true;
    private boolean useGlobalExceptionHandler = true;
    private boolean useMediatr = false;
    private String routePrefix = "api";
    private String versioningPrefix = "v";
    private String apiVersion = "1";
    private String solutionGuid = null;
    private String projectConfigurationGuid = null;


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

        addSwitch(USE_PROBLEM_DETAILS, "Enable RFC 7807 compatible error responses.", useProblemDetails);
        addSwitch(USE_RECORDS, "Use record instead of class for the requests and response.", useRecords);
        addSwitch(USE_AUTHENTICATION, "Enable JWT authentication.", useAuthentication);
        addSwitch(USE_VALIDATORS, "Enable FluentValidation request validators.", useValidators);
        addSwitch(USE_RESPONSE_CACHING, "Enable response caching.", useResponseCaching);
        addSwitch(USE_API_VERSIONING, "Enable API versioning.", useApiVersioning);
        addSwitch(USE_ROUTE_GROUPS, "Use MapGroup for organizing endpoints by tag.", useRouteGroups);
        addSwitch(USE_GLOBAL_EXCEPTION_HANDLER, "Enable global exception handling middleware.", useGlobalExceptionHandler);
        addSwitch(USE_MEDIATR, "Enable MediatR CQRS pattern with commands, queries, and handlers.", useMediatr);
        addOption(ROUTE_PREFIX, "The route prefix for the API. Used only if useApiVersioning is true", routePrefix);
        addOption(VERSIONING_PREFIX, "The versioning prefix for the API. Used only if useApiVersioning is true", versioningPrefix);
        addOption(API_VERSION, "The version of the API. Used only if useApiVersioning is true", apiVersion);
        addOption(SOLUTION_GUID, "The solution GUID to be used in the solution file (auto generated if not provided)", solutionGuid);
        addOption(PROJECT_CONFIGURATION_GUID, "The project configuration GUID to be used in the solution file (auto generated if not provided)", projectConfigurationGuid);
    }

    @Override
    public void processOpts() {

        setPackageDescription(openAPI.getInfo().getDescription());

        setUseProblemDetails();
        setUseRecordForRequest();
        setUseAuthentication();
        setUseValidators();
        setUseResponseCaching();
        setUseApiVersioning();
        setUseRouteGroups();
        setUseGlobalExceptionHandler();
        setUseMediatr();
        setRoutePrefix();
        setVersioningPrefix();
        setApiVersion();
        setSolutionGuid();
        setProjectConfigurationGuid();
        
        // Extract basePath from server URL for endpoint routing
        setBasePath();

        super.processOpts();

        addSupportingFiles();
    }

    private void addSupportingFiles() {
        apiPackage = "Features";
        modelPackage = "Models";
        String packageFolder = sourceFolder + File.separator + packageName;

        if (useAuthentication) {
            supportingFiles.add(new SupportingFile("loginRequest.mustache", packageFolder + File.separator + apiPackage, "LoginRequest.cs"));
            supportingFiles.add(new SupportingFile("userLoginEndpoint.mustache", packageFolder + File.separator + apiPackage, "UserLoginEndpoint.cs"));
        }

        supportingFiles.add(new SupportingFile("readme.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("solution.mustache", "", packageName + ".sln"));
        supportingFiles.add(new SupportingFile("project.csproj.mustache", packageFolder, packageName + ".csproj"));
        supportingFiles.add(new SupportingFile("Properties" + File.separator + "launchSettings.json", packageFolder + File.separator + "Properties", "launchSettings.json"));

        supportingFiles.add(new SupportingFile("appsettings.json", packageFolder, "appsettings.json"));
        supportingFiles.add(new SupportingFile("appsettings.Development.json", packageFolder, "appsettings.Development.json"));

        supportingFiles.add(new SupportingFile("program.mustache", packageFolder, "Program.cs"));
        
        // Minimal API: EndpointMapper extension for MapAllEndpoints()
        supportingFiles.add(new SupportingFile("endpointMapper.mustache", 
            packageFolder + File.separator + "Extensions", "EndpointMapper.cs"));
    }

    @Override
    protected void processOperation(CodegenOperation operation) {
        super.processOperation(operation);

        // Converts, for example, PUT to Put for endpoint configuration
        operation.httpMethod = operation.httpMethod.charAt(0) + operation.httpMethod.substring(1).toLowerCase(Locale.ROOT);
        
        // Convert List<T> to T[] for query array parameters
        // Minimal APIs support string[] natively but not List<string>
        if (operation.allParams != null) {
            for (CodegenParameter param : operation.allParams) {
                if (param.isQueryParam && param.isContainer && param.isArray) {
                    // Change List<string> to string[], List<int> to int[], etc.
                    String innerType = param.items != null ? param.items.dataType : "string";
                    param.dataType = innerType + "[]";
                    LOGGER.info("Converted query array parameter '{}' from List<{}> to {}[] for Minimal API compatibility", 
                        param.paramName, innerType, innerType);
                }
                // Log model-type query parameters (will use custom JSON deserialization)
                if (param.isQueryParam && param.isModel) {
                    LOGGER.info("Operation '{}' has model-type query parameter '{}' - will use JSON deserialization from query string", 
                        operation.operationId, param.paramName);
                }
            }
        }

        // Add MediatR-specific vendor extensions for template generation
        if (useMediatr) {
            String mediatrResponseType = getMediatrResponseType(operation);
            operation.vendorExtensions.put("mediatrResponseType", mediatrResponseType);
            operation.vendorExtensions.put("isUnit", "Unit".equals(mediatrResponseType));
            
            // Determine if operation is a command (mutation) or query (read)
            boolean isQuery = "GET".equalsIgnoreCase(operation.httpMethod) || "Get".equals(operation.httpMethod);
            operation.vendorExtensions.put("isQuery", isQuery);
            operation.vendorExtensions.put("isCommand", !isQuery);
            
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
        co.vendorExtensions.put("x-isPetApi", "Pet".equalsIgnoreCase(groupKey));
        
        // Add flags for specific CRUD operations to enable logic implementation
        String opId = co.operationId != null ? co.operationId.toLowerCase() : "";
        co.vendorExtensions.put("x-isAddPet", opId.equals("addpet"));
        co.vendorExtensions.put("x-isGetPetById", opId.equals("getpetbyid"));
        co.vendorExtensions.put("x-isUpdatePet", opId.equals("updatepet"));
        co.vendorExtensions.put("x-isDeletePet", opId.equals("deletepet"));
        
        co.baseName = groupKey;
        
        LOGGER.info("Added operation '{}' to tag group '{}'", co.operationId, groupKey);
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

    private void setUseRouteGroups() {
        if (additionalProperties.containsKey(USE_ROUTE_GROUPS)) {
            useRouteGroups = convertPropertyToBooleanAndWriteBack(USE_ROUTE_GROUPS);
        } else {
            additionalProperties.put(USE_ROUTE_GROUPS, useRouteGroups);
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

    /**
     * Get the MediatR response type for IRequest<TResponse> based on the operation's return type.
     * @param operation The CodegenOperation to analyze
     * @return The response type string for IRequest<T>
     */
    private String getMediatrResponseType(CodegenOperation operation) {
        if (operation.returnType == null || operation.returnType.equals("void")) {
            return "Unit"; // MediatR.Unit for void operations (DELETE, etc.)
        }
        
        if (operation.returnContainer != null && operation.returnContainer.equals("array")) {
            // Array responses use IEnumerable<T>
            return "IEnumerable<" + operation.returnBaseType + ">";
        }
        
        // Direct model type or primitive
        return operation.returnType;
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
