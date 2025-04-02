/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionModel;
import com.bytechef.platform.configuration.web.rest.model.UnifiedApiCategoryModel;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-04-13T22:23:09.531481+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "component-definition", description = "The Platform Component Definition Internal API")
public interface ComponentDefinitionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /component-definitions/{componentName}/versions/{componentVersion} : Get a component definition
     * Get a component definition.
     *
     * @param componentName The name of a component to get. (required)
     * @param componentVersion The version of a component to get. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentDefinition",
        summary = "Get a component definition",
        description = "Get a component definition.",
        tags = { "component-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ComponentDefinitionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/versions/{componentVersion}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ComponentDefinitionModel> getComponentDefinition(
        @Parameter(name = "componentName", description = "The name of a component to get.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(name = "componentVersion", description = "The version of a component to get.", required = true, in = ParameterIn.PATH) @PathVariable("componentVersion") Integer componentVersion
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"clusterElement\" : true, \"clusterRoot\" : true, \"icon\" : \"icon\", \"description\" : \"description\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"title\" : \"title\", \"triggers\" : [ { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 6, \"title\" : \"title\", \"type\" : \"STATIC_WEBHOOK\" }, { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 6, \"title\" : \"title\", \"type\" : \"STATIC_WEBHOOK\" } ], \"version\" : 1, \"tags\" : [ \"tags\", \"tags\" ], \"actionClusterElementTypes\" : { \"key\" : [ \"actionClusterElementTypes\", \"actionClusterElementTypes\" ] }, \"clusterElementTypes\" : [ { \"multipleElements\" : true, \"name\" : \"name\", \"label\" : \"label\", \"required\" : true }, { \"multipleElements\" : true, \"name\" : \"name\", \"label\" : \"label\", \"required\" : true } ], \"connectionRequired\" : true, \"name\" : \"name\", \"connection\" : { \"componentTitle\" : \"componentTitle\", \"componentDescription\" : \"componentDescription\", \"componentName\" : \"componentName\", \"version\" : 0 }, \"actions\" : [ { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"title\" : \"title\" }, { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"title\" : \"title\" } ], \"unifiedApiCategory\" : \"ACCOUNTING\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /component-definitions/{componentName}/versions : Get all component definition versions of a component
     * Get all component definition versions of a component.
     *
     * @param componentName The name of a component. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentDefinitionVersions",
        summary = "Get all component definition versions of a component",
        description = "Get all component definition versions of a component.",
        tags = { "component-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ComponentDefinitionBasicModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/versions",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ComponentDefinitionBasicModel>> getComponentDefinitionVersions(
        @Parameter(name = "componentName", description = "The name of a component.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 0, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 1, \"triggersCount\" : 6 }, { \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 0, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 1, \"triggersCount\" : 6 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /component-definitions : Get all component definitions
     * Get all component definitions.
     *
     * @param modeType The application mode. (required)
     * @param actionDefinitions Use for filtering components which define action definitions. (optional)
     * @param connectionDefinitions Use for filtering components which define connection definitions. (optional)
     * @param triggerDefinitions Use for filtering components which define trigger definitions. (optional)
     * @param include The list of component names to include in the result. (optional)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentDefinitions",
        summary = "Get all component definitions",
        description = "Get all component definitions.",
        tags = { "component-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ComponentDefinitionBasicModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ComponentDefinitionBasicModel>> getComponentDefinitions(
        @NotNull @Parameter(name = "modeType", description = "The application mode.", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "modeType", required = true) Integer modeType,
        @Parameter(name = "actionDefinitions", description = "Use for filtering components which define action definitions.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "actionDefinitions", required = false) Boolean actionDefinitions,
        @Parameter(name = "connectionDefinitions", description = "Use for filtering components which define connection definitions.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "connectionDefinitions", required = false) Boolean connectionDefinitions,
        @Parameter(name = "triggerDefinitions", description = "Use for filtering components which define trigger definitions.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "triggerDefinitions", required = false) Boolean triggerDefinitions,
        @Parameter(name = "include", description = "The list of component names to include in the result.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "include", required = false) List<String> include
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 0, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 1, \"triggersCount\" : 6 }, { \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 0, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 1, \"triggersCount\" : 6 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /component-definitions/{componentName}/connection-versions/{connectionVersion} : Get a connection component definition
     * Get a connection component definition.
     *
     * @param componentName The name of a component to get. (required)
     * @param connectionVersion The version of a component connection to get. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getConnectionComponentDefinition",
        summary = "Get a connection component definition",
        description = "Get a connection component definition.",
        tags = { "component-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ComponentDefinitionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/connection-versions/{connectionVersion}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ComponentDefinitionModel> getConnectionComponentDefinition(
        @Parameter(name = "componentName", description = "The name of a component to get.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(name = "connectionVersion", description = "The version of a component connection to get.", required = true, in = ParameterIn.PATH) @PathVariable("connectionVersion") Integer connectionVersion
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"clusterElement\" : true, \"clusterRoot\" : true, \"icon\" : \"icon\", \"description\" : \"description\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"title\" : \"title\", \"triggers\" : [ { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 6, \"title\" : \"title\", \"type\" : \"STATIC_WEBHOOK\" }, { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 6, \"title\" : \"title\", \"type\" : \"STATIC_WEBHOOK\" } ], \"version\" : 1, \"tags\" : [ \"tags\", \"tags\" ], \"actionClusterElementTypes\" : { \"key\" : [ \"actionClusterElementTypes\", \"actionClusterElementTypes\" ] }, \"clusterElementTypes\" : [ { \"multipleElements\" : true, \"name\" : \"name\", \"label\" : \"label\", \"required\" : true }, { \"multipleElements\" : true, \"name\" : \"name\", \"label\" : \"label\", \"required\" : true } ], \"connectionRequired\" : true, \"name\" : \"name\", \"connection\" : { \"componentTitle\" : \"componentTitle\", \"componentDescription\" : \"componentDescription\", \"componentName\" : \"componentName\", \"version\" : 0 }, \"actions\" : [ { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"title\" : \"title\" }, { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"title\" : \"title\" } ], \"unifiedApiCategory\" : \"ACCOUNTING\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /unified-api/{category}/component-definitions : Get all compatible component definitions for a unified API category
     * Get all compatible component definitions for a unified API category.
     *
     * @param category The name of a unified API category. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getUnifiedApiComponentDefinitions",
        summary = "Get all compatible component definitions for a unified API category",
        description = "Get all compatible component definitions for a unified API category.",
        tags = { "component-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ComponentDefinitionBasicModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/unified-api/{category}/component-definitions",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ComponentDefinitionBasicModel>> getUnifiedApiComponentDefinitions(
        @Parameter(name = "category", description = "The name of a unified API category.", required = true, in = ParameterIn.PATH) @PathVariable("category") UnifiedApiCategoryModel category
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 0, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 1, \"triggersCount\" : 6 }, { \"componentCategories\" : [ { \"name\" : \"name\", \"label\" : \"label\" }, { \"name\" : \"name\", \"label\" : \"label\" } ], \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 0, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 1, \"triggersCount\" : 6 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
