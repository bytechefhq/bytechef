/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.9.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.configuration.web.rest.model.DeleteWorkflowNodeParameter200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.DeleteWorkflowNodeParameterRequestModel;
import com.bytechef.platform.configuration.web.rest.model.GetWorkflowNodeParameterDisplayConditions200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateWorkflowNodeParameter200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateWorkflowNodeParameterRequestModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-10-31T18:29:59.023043+01:00[Europe/Zagreb]", comments = "Generator version: 7.9.0")
@Validated
@Tag(name = "workflow-node-parameter", description = "The Platform Workflow Node Parameter Internal API")
public interface WorkflowNodeParameterApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * DELETE /workflows/{id}/parameters : Deletes a workflow node parameter
     * Deletes a workflow node parameter.
     *
     * @param id The workflow id (required)
     * @param deleteWorkflowNodeParameterRequestModel  (optional)
     * @return The updated workflow node parameters. (status code 200)
     */
    @Operation(
        operationId = "deleteWorkflowNodeParameter",
        summary = "Deletes a workflow node parameter",
        description = "Deletes a workflow node parameter.",
        tags = { "workflow-node-parameter" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated workflow node parameters.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = DeleteWorkflowNodeParameter200ResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/workflows/{id}/parameters",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<DeleteWorkflowNodeParameter200ResponseModel> deleteWorkflowNodeParameter(
        @Parameter(name = "id", description = "The workflow id", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(name = "DeleteWorkflowNodeParameterRequestModel", description = "") @Valid @RequestBody(required = false) DeleteWorkflowNodeParameterRequestModel deleteWorkflowNodeParameterRequestModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"parameters\" : { \"key\" : \"\" } }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workflows/{id}/parameters/{workflowNodeName}/display-conditions : Get an action or trigger property options shown in the editor
     * Get an action or trigger property options shown in the editor.
     *
     * @param id The workflow id (required)
     * @param workflowNodeName The name of a workflow&#39;s action task or trigger (E.g. mailchimp_1) (required)
     * @return The workflow node parameter display conditions. (status code 200)
     */
    @Operation(
        operationId = "getWorkflowNodeParameterDisplayConditions",
        summary = "Get an action or trigger property options shown in the editor",
        description = "Get an action or trigger property options shown in the editor.",
        tags = { "workflow-node-parameter" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The workflow node parameter display conditions.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = GetWorkflowNodeParameterDisplayConditions200ResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workflows/{id}/parameters/{workflowNodeName}/display-conditions",
        produces = { "application/json" }
    )
    
    default ResponseEntity<GetWorkflowNodeParameterDisplayConditions200ResponseModel> getWorkflowNodeParameterDisplayConditions(
        @Parameter(name = "id", description = "The workflow id", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(name = "workflowNodeName", description = "The name of a workflow's action task or trigger (E.g. mailchimp_1)", required = true, in = ParameterIn.PATH) @PathVariable("workflowNodeName") String workflowNodeName
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"displayConditions\" : { \"key\" : true } }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /workflows/{id}/parameters : Updates a workflow node parameter
     * Updates a workflow node parameter.
     *
     * @param id The workflow id (required)
     * @param updateWorkflowNodeParameterRequestModel  (optional)
     * @return The updated workflow node parameters. (status code 200)
     */
    @Operation(
        operationId = "updateWorkflowNodeParameter",
        summary = "Updates a workflow node parameter",
        description = "Updates a workflow node parameter.",
        tags = { "workflow-node-parameter" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated workflow node parameters.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateWorkflowNodeParameter200ResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/workflows/{id}/parameters",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<UpdateWorkflowNodeParameter200ResponseModel> updateWorkflowNodeParameter(
        @Parameter(name = "id", description = "The workflow id", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(name = "UpdateWorkflowNodeParameterRequestModel", description = "") @Valid @RequestBody(required = false) UpdateWorkflowNodeParameterRequestModel updateWorkflowNodeParameterRequestModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"metadata\" : { \"key\" : \"\" }, \"displayConditions\" : { \"key\" : true }, \"parameters\" : { \"key\" : \"\" } }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
