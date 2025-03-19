/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.configuration.web.rest.model.ClusterElementDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ClusterElementDefinitionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-20T07:39:40.498527+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "cluster-element-definition", description = "The Platform Cluster Element Definition Internal API")
public interface ClusterElementDefinitionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /component-definitions/{componentName}/versions/{componentVersion}/cluster-element-definition/{clusterElementName} : Get a cluster element definition of a component
     * Get a cluster element definition of a component.
     *
     * @param componentName The name of a component. (required)
     * @param componentVersion The version of a component. (required)
     * @param clusterElementName The name of a cluster element to get. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentClusterElementDefinition",
        summary = "Get a cluster element definition of a component",
        description = "Get a cluster element definition of a component.",
        tags = { "cluster-element-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ClusterElementDefinitionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/versions/{componentVersion}/cluster-element-definition/{clusterElementName}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ClusterElementDefinitionModel> getComponentClusterElementDefinition(
        @Parameter(name = "componentName", description = "The name of a component.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(name = "componentVersion", description = "The version of a component.", required = true, in = ParameterIn.PATH) @PathVariable("componentVersion") Integer componentVersion,
        @Parameter(name = "clusterElementName", description = "The name of a cluster element to get.", required = true, in = ParameterIn.PATH) @PathVariable("clusterElementName") String clusterElementName
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"name\" : \"name\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"type\" : \"type\", \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : false, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : false, \"type\" : \"ARRAY\", \"required\" : false, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : false, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : false, \"type\" : \"ARRAY\", \"required\" : false, \"expressionEnabled\" : true } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /component-definitions/{rootComponentName}/versions/{rootComponentVersion}/cluster-element-definitions/{clusterElementType} : Get a cluster element definitions of a root component.
     * Get a cluster element definitions of a root component.
     *
     * @param rootComponentName The name of a root component. (required)
     * @param rootComponentVersion The version of a root component. (required)
     * @param clusterElementType The name of a cluster elements to get. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getRootComponentClusterElementDefinitions",
        summary = "Get a cluster element definitions of a root component.",
        description = "Get a cluster element definitions of a root component.",
        tags = { "cluster-element-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ClusterElementDefinitionBasicModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{rootComponentName}/versions/{rootComponentVersion}/cluster-element-definitions/{clusterElementType}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ClusterElementDefinitionBasicModel>> getRootComponentClusterElementDefinitions(
        @Parameter(name = "rootComponentName", description = "The name of a root component.", required = true, in = ParameterIn.PATH) @PathVariable("rootComponentName") String rootComponentName,
        @Parameter(name = "rootComponentVersion", description = "The version of a root component.", required = true, in = ParameterIn.PATH) @PathVariable("rootComponentVersion") Integer rootComponentVersion,
        @Parameter(name = "clusterElementType", description = "The name of a cluster elements to get.", required = true, in = ParameterIn.PATH) @PathVariable("clusterElementType") String clusterElementType
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"name\" : \"name\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"type\" : \"type\" }, { \"name\" : \"name\", \"outputDefined\" : true, \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"type\" : \"type\" } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
