/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.workflow.web.rest;

import java.util.Map;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowModel;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowResponseModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-10T20:32:26.667648+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "workflows", description = "The Core Workflows API")
public interface WorkflowsApi {

    /**
     * POST /workflows : Create a new workflow.
     * Create a new workflow.
     *
     * @param workflowModel The workflow object that needs to be created. (required)
     * @return The workflow object. (status code 200)
     */
    @Operation(
        operationId = "createWorkflow",
        summary = "Create a new workflow.",
        description = "Create a new workflow.",
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
    default Mono<ResponseEntity<WorkflowModel>> createWorkflow(
        @Parameter(name = "WorkflowModel", description = "The workflow object that needs to be created.", required = true) @Valid @RequestBody Mono<WorkflowModel> workflowModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"componentName\" : \"componentName\", \"componentVersion\" : 0 }, { \"componentName\" : \"componentName\", \"componentVersion\" : 0 } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(workflowModel).then(Mono.empty());

    }


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
    default Mono<ResponseEntity<Void>> deleteWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to delete.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
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
    default Mono<ResponseEntity<WorkflowModel>> getWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to get.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"componentName\" : \"componentName\", \"componentVersion\" : 0 }, { \"componentName\" : \"componentName\", \"componentVersion\" : 0 } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
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
     * @return A list of workflows. (status code 200)
     */
    @Operation(
        operationId = "getWorkflows",
        summary = "Get workflow definitions.",
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
    default Mono<ResponseEntity<Flux<WorkflowModel>>> getWorkflows(
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "[ { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"componentName\" : \"componentName\", \"componentVersion\" : 0 }, { \"componentName\" : \"componentName\", \"componentVersion\" : 0 } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }, { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"componentName\" : \"componentName\", \"componentVersion\" : 0 }, { \"componentName\" : \"componentName\", \"componentVersion\" : 0 } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] } ]";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * POST /workflows/{id}/tests : Execute a workflow synchronously for testing purpose.
     * Execute a workflow synchronously for testing purposes.
     *
     * @param id The id of the workflow to test. (required)
     * @param requestBody Parameters required to run a workflow, for example &#39;{\&quot;yourName\&quot;:\&quot;Joe Jones\&quot;}&#39; (required)
     * @return The output expected by the workflow. (status code 200)
     */
    @Operation(
        operationId = "testWorkflow",
        summary = "Execute a workflow synchronously for testing purpose.",
        description = "Execute a workflow synchronously for testing purposes.",
        tags = { "workflows" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The output expected by the workflow.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/workflows/{id}/tests",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<WorkflowResponseModel>> testWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to test.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(name = "request_body", description = "Parameters required to run a workflow, for example '{\"yourName\":\"Joe Jones\"}'", required = true) @Valid @RequestBody Mono<Map<String, Object>> requestBody,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"taskExecutions\" : [ { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 9, \"id\" : \"id\", \"retryAttempts\" : 5, \"retryDelayFactor\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, \"priority\" : 6, \"parentId\" : \"parentId\", \"executionTime\" : 0, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"title\" : \"title\" }, \"maxRetries\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 1, \"taskNumber\" : 7, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 9, \"id\" : \"id\", \"retryAttempts\" : 5, \"retryDelayFactor\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, \"priority\" : 6, \"parentId\" : \"parentId\", \"executionTime\" : 0, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"title\" : \"title\" }, \"maxRetries\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 1, \"taskNumber\" : 7, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" } ], \"job\" : { \"outputs\" : { \"key\" : \"{}\" }, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"currentTask\" : 0, \"label\" : \"label\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"parentTaskExecutionId\" : 6, \"webhooks\" : [ { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 2, \"multiplier\" : 7, \"initialInterval\" : 5, \"maxInterval\" : 5 } }, { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 2, \"multiplier\" : 7, \"initialInterval\" : 5, \"maxInterval\" : 5 } } ], \"id\" : \"id\", \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"workflowId\" : \"workflowId\", \"status\" : \"CREATED\" } }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(requestBody).then(Mono.empty());

    }


    /**
     * PUT /workflows/{id} : Update an existing workflow.
     * Update an existing workflow.
     *
     * @param id The id of the workflow to update. (required)
     * @param workflowModel The workflow object that needs to updated. (required)
     * @return The updated workflow object. (status code 200)
     */
    @Operation(
        operationId = "updateWorkflow",
        summary = "Update an existing workflow.",
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
    default Mono<ResponseEntity<WorkflowModel>> updateWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to update.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(name = "WorkflowModel", description = "The workflow object that needs to updated.", required = true) @Valid @RequestBody Mono<WorkflowModel> workflowModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"type\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"label\" : \"label\", \"__version\" : 1, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"definition\" : \"definition\", \"id\" : \"id\", \"connections\" : [ { \"componentName\" : \"componentName\", \"componentVersion\" : 0 }, { \"componentName\" : \"componentName\", \"componentVersion\" : 0 } ], \"tasks\" : [ { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" } ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(workflowModel).then(Mono.empty());

    }

}
