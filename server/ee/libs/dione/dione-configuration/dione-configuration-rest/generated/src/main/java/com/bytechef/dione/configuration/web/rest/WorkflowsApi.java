/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.6.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.dione.configuration.web.rest;

import com.bytechef.dione.configuration.web.rest.model.WorkflowModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-07-06T09:23:30.497811+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "workflows", description = "The Embedded Workflows API")
public interface WorkflowsApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * DELETE /workflows/{id} : Delete a workflow
     * Delete a workflow.
     *
     * @param id The id of the workflow to delete. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteWorkflow",
        summary = "Delete a workflow",
        description = "Delete a workflow.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/workflows/{id}"
    )
    default ResponseEntity<Void> deleteWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to delete.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workflows/{id} : Get a workflow by id
     * Get a workflow by id.
     *
     * @param id The id of the workflow to get. (required)
     * @return The workflow object. (status code 200)
     */
    @Operation(
        operationId = "getWorkflow",
        summary = "Get a workflow by id",
        description = "Get a workflow by id.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The workflow object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workflows/{id}",
        produces = { "application/json" }
    )
    default ResponseEntity<WorkflowModel> getWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to get.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" }, { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workflows : Get workflow definitions
     * Get workflow definitions.
     *
     * @return A list of workflows. (status code 200)
     */
    @Operation(
        operationId = "getWorkflows",
        summary = "Get workflow definitions",
        description = "Get workflow definitions.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of workflows.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WorkflowModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workflows",
        produces = { "application/json" }
    )
    default ResponseEntity<List<WorkflowModel>> getWorkflows(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" }, { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }, { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" }, { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /workflows/{id} : Update an existing workflow
     * Update an existing workflow.
     *
     * @param id The id of the workflow to update. (required)
     * @param workflowModel The workflow object that needs to updated. (required)
     * @return The updated workflow object. (status code 200)
     */
    @Operation(
        operationId = "updateWorkflow",
        summary = "Update an existing workflow",
        description = "Update an existing workflow.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated workflow object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/workflows/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default ResponseEntity<WorkflowModel> updateWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to update.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(name = "WorkflowModel", description = "The workflow object that needs to updated.", required = true) @Valid @RequestBody WorkflowModel workflowModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" }, { \"operationName\" : \"operationName\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"key\" : \"key\" } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
