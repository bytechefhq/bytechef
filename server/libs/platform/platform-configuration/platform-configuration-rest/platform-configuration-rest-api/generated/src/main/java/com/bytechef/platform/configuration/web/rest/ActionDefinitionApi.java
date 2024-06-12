/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.configuration.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ActionDefinitionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-12T12:42:24.418512+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
@Validated
@Tag(name = "action-definition", description = "The Platform Action Definition API")
public interface ActionDefinitionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /component-definitions/{componentName}/versions/{componentVersion}/action-definitions/{actionName} : Get an action definition of a component
     * Get an action definition of a component.
     *
     * @param componentName The name of a component. (required)
     * @param componentVersion The version of a component. (required)
     * @param actionName The name of the action to get. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentActionDefinition",
        summary = "Get an action definition of a component",
        description = "Get an action definition of a component.",
        tags = { "action-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ActionDefinitionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/versions/{componentVersion}/action-definitions/{actionName}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ActionDefinitionModel> getComponentActionDefinition(
        @Parameter(name = "componentName", description = "The name of a component.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(name = "componentVersion", description = "The version of a component.", required = true, in = ParameterIn.PATH) @PathVariable("componentVersion") Integer componentVersion,
        @Parameter(name = "actionName", description = "The name of the action to get.", required = true, in = ParameterIn.PATH) @PathVariable("actionName") String actionName
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputFunctionDefined\" : true, \"name\" : \"name\", \"description\" : \"description\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"title\" : \"title\", \"workflowNodeDescriptionDefined\" : true, \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : false, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : false, \"required\" : false, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : false, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : false, \"required\" : false, \"expressionEnabled\" : true } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /component-definitions/{componentName}/versions/{componentVersion}/action-definitions : Get a list of action definitions for a component
     * Get a list of action definitions for a component.
     *
     * @param componentName The name of a component. (required)
     * @param componentVersion The version of a component. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentActionDefinitions",
        summary = "Get a list of action definitions for a component",
        description = "Get a list of action definitions for a component.",
        tags = { "action-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ActionDefinitionBasicModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/versions/{componentVersion}/action-definitions",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ActionDefinitionBasicModel>> getComponentActionDefinitions(
        @Parameter(name = "componentName", description = "The name of a component.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(name = "componentVersion", description = "The version of a component.", required = true, in = ParameterIn.PATH) @PathVariable("componentVersion") Integer componentVersion
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"name\" : \"name\", \"description\" : \"description\", \"title\" : \"title\" }, { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"name\" : \"name\", \"description\" : \"description\", \"title\" : \"title\" } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
