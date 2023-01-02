/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.2.1).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.atlas.web.rest;

import com.bytechef.atlas.web.rest.model.PostWorkflowRequestModel;
import com.bytechef.atlas.web.rest.model.PutWorkflowRequestModel;
import com.bytechef.atlas.web.rest.model.WorkflowModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.codec.multipart.Part;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-12-26T09:25:21.049913+01:00[Europe/Zagreb]")
@Validated
@Tag(name = "workflows", description = "the workflows API")
public interface WorkflowsApi {

    /**
     * DELETE /workflows/{id} : Delete a workflow.
     * Delete a workflow.
     *
     * @param id The id of the workflow to delete. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteWorkflow",
        summary = "Delete a workflow.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/workflows/{id}"
    )
    default Mono<ResponseEntity<Void>> deleteWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to delete.", required = true) @PathVariable("id") String id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(Mono.empty());

    }


    /**
     * GET /workflows/{id} : Get a workflow by id.
     * Get a workflow by id.
     *
     * @param id The id of the workflow to get. (required)
     * @return The workflow object. (status code 200)
     */
    @Operation(
        operationId = "getWorkflow",
        summary = "Get a workflow by id.",
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
    default Mono<ResponseEntity<WorkflowModel>> getWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to get.", required = true) @PathVariable("id") String id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"sourceType\" : \"CLASSPATH\", \"inputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"id\" : \"id\", \"label\" : \"label\", \"retry\" : 0, \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /workflows : Get workflow definitions.
     * Get workflow definitions.
     *
     * @return The list of workflows. (status code 200)
     */
    @Operation(
        operationId = "getWorkflows",
        summary = "Get workflow definitions.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of workflows.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workflows",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<WorkflowModel>>> getWorkflows(
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"sourceType\" : \"CLASSPATH\", \"inputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"id\" : \"id\", \"label\" : \"label\", \"retry\" : 0, \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * POST /workflows : Create a new workflow.
     * Create a new workflow.
     *
     * @param postWorkflowRequestModel The workflow object that needs to be created. (required)
     * @return The workflow object. (status code 200)
     */
    @Operation(
        operationId = "postWorkflow",
        summary = "Create a new workflow.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The workflow object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/workflows",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<WorkflowModel>> postWorkflow(
        @Parameter(name = "PostWorkflowRequestModel", description = "The workflow object that needs to be created.", required = true) @Valid @RequestBody Mono<PostWorkflowRequestModel> postWorkflowRequestModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"sourceType\" : \"CLASSPATH\", \"inputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"id\" : \"id\", \"label\" : \"label\", \"retry\" : 0, \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(postWorkflowRequestModel).then(Mono.empty());

    }


    /**
     * PUT /workflows/{id} : Update an existing workflow.
     * Update an existing workflow.
     *
     * @param id The id of the workflow to update. (required)
     * @param putWorkflowRequestModel The workflow object that needs to updated. (required)
     * @return The updated workflow object. (status code 200)
     */
    @Operation(
        operationId = "putWorkflow",
        summary = "Update an existing workflow.",
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
    default Mono<ResponseEntity<WorkflowModel>> putWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to update.", required = true) @PathVariable("id") String id,
        @Parameter(name = "PutWorkflowRequestModel", description = "The workflow object that needs to updated.", required = true) @Valid @RequestBody Mono<PutWorkflowRequestModel> putWorkflowRequestModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"sourceType\" : \"CLASSPATH\", \"inputs\" : [ { \"key\" : \"{}\" }, { \"key\" : \"{}\" } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"id\" : \"id\", \"label\" : \"label\", \"retry\" : 0, \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(putWorkflowRequestModel).then(Mono.empty());

    }

}
