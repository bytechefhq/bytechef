/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.4.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.workflow.web.rest;

import com.bytechef.hermes.workflow.web.rest.model.TaskExecutionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-01T14:11:10.397866+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "task-executions", description = "the task-executions API")
public interface TaskExecutionsApi {

    /**
     * GET /task-executions/{id} : Get a task execution by id.
     * Get a task execution by id.
     *
     * @param id The id of the task execution to get. (required)
     * @return The task execution object. (status code 200)
     */
    @Operation(
        operationId = "getTaskExecution",
        summary = "Get a task execution by id.",
        description = "Get a task execution by id.",
        tags = { "task-executions" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The task execution object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = TaskExecutionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-executions/{id}",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<TaskExecutionModel>> getTaskExecution(
        @Parameter(name = "id", description = "The id of the task execution to get.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"retryDelayFactor\" : 2, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 6, \"type\" : \"type\", \"parentId\" : \"parentId\", \"executionTime\" : 0, \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"retryDelayMillis\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 1, \"id\" : \"id\", \"taskNumber\" : 7, \"retry\" : 5, \"retryAttempts\" : 5, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }

}
