/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.workflow.web.rest;

import com.bytechef.hermes.workflow.web.rest.model.CreateJob200ResponseModel;
import com.bytechef.hermes.workflow.web.rest.model.JobModel;
import com.bytechef.hermes.workflow.web.rest.model.JobParametersModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-18T15:32:47.160250+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "jobs", description = "the jobs API")
public interface JobsApi {

    /**
     * POST /jobs : Create a request for running a new job.
     * Create a request for running a new job.
     *
     * @param jobParametersModel Parameters required to run a job, for example &#39;{\&quot;workflowId\&quot;:\&quot;samples/hello\&quot;,\&quot;inputs\&quot;:{\&quot;yourName\&quot;:\&quot;Joe Jones\&quot;}}&#39; (required)
     * @return The id of a created job. (status code 200)
     */
    @Operation(
        operationId = "createJob",
        summary = "Create a request for running a new job.",
        description = "Create a request for running a new job.",
        tags = { "jobs" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The id of a created job.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CreateJob200ResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/jobs",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<CreateJob200ResponseModel>> createJob(
        @Parameter(name = "JobParametersModel", description = "Parameters required to run a job, for example '{\"workflowId\":\"samples/hello\",\"inputs\":{\"yourName\":\"Joe Jones\"}}'", required = true) @Valid @RequestBody Mono<JobParametersModel> jobParametersModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"jobId\" : 0 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(jobParametersModel).then(Mono.empty());

    }


    /**
     * GET /jobs/{id} : Get a job by id.
     * Get a job by id.
     *
     * @param id The id of a job to return. (required)
     * @return The job object. (status code 200)
     */
    @Operation(
        operationId = "getJob",
        summary = "Get a job by id.",
        description = "Get a job by id.",
        tags = { "jobs" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The job object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = JobModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/jobs/{id}",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<JobModel>> getJob(
        @Parameter(name = "id", description = "The id of a job to return.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : { \"key\" : \"{}\" }, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"currentTask\" : 0, \"label\" : \"label\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"parentTaskExecutionId\" : 6, \"webhooks\" : [ { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 2, \"multiplier\" : 7, \"initialInterval\" : 5, \"maxInterval\" : 5 } }, { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 2, \"multiplier\" : 7, \"initialInterval\" : 5, \"maxInterval\" : 5 } } ], \"id\" : \"id\", \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"workflowId\" : \"workflowId\", \"status\" : \"CREATED\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /jobs/{id}/task-executions : Get task executions of a job.
     * Get task executions of a job.
     *
     * @param id The id of a job to return task executions for. (required)
     * @return A list of task executions. (status code 200)
     */
    @Operation(
        operationId = "getJobTaskExecutions",
        summary = "Get task executions of a job.",
        description = "Get task executions of a job.",
        tags = { "jobs" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of task executions.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TaskExecutionModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/jobs/{id}/task-executions",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<TaskExecutionModel>>> getJobTaskExecutions(
        @Parameter(name = "id", description = "The id of a job to return task executions for.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "[ { \"retryDelayFactor\" : 2, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 6, \"type\" : \"type\", \"parentId\" : \"parentId\", \"executionTime\" : 0, \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"retryDelayMillis\" : 9, \"maxRetries\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 1, \"id\" : \"id\", \"taskNumber\" : 7, \"retryAttempts\" : 5, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, { \"retryDelayFactor\" : 2, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"node\" : \"node\", \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"{}\" }, \"timeout\" : \"timeout\" }, \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 6, \"type\" : \"type\", \"parentId\" : \"parentId\", \"executionTime\" : 0, \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"retryDelayMillis\" : 9, \"maxRetries\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 1, \"id\" : \"id\", \"taskNumber\" : 7, \"retryAttempts\" : 5, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" } ]";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /jobs : Get a page of jobs.
     * Get a page of jobs.
     *
     * @param pageNumber The number of the page to return. (optional, default to 0)
     * @return The page of jobs. (status code 200)
     */
    @Operation(
        operationId = "getJobs",
        summary = "Get a page of jobs.",
        description = "Get a page of jobs.",
        tags = { "jobs" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The page of jobs.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/jobs",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<org.springframework.data.domain.Page>> getJobs(
        @Parameter(name = "pageNumber", description = "The number of the page to return.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "pageNumber", required = false, defaultValue = "0") Integer pageNumber,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"number\" : 0, \"size\" : 6, \"numberOfElements\" : 1, \"totalPages\" : 5, \"content\" : [ \"{}\", \"{}\" ], \"totalElements\" : 5 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /jobs/latest : Get the latest job.
     * Get the latest job.
     *
     * @return The latest job. (status code 200)
     */
    @Operation(
        operationId = "getLatestJob",
        summary = "Get the latest job.",
        description = "Get the latest job.",
        tags = { "jobs" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The latest job.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = JobModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/jobs/latest",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<JobModel>> getLatestJob(
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"outputs\" : { \"key\" : \"{}\" }, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"currentTask\" : 0, \"label\" : \"label\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"parentTaskExecutionId\" : 6, \"webhooks\" : [ { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 2, \"multiplier\" : 7, \"initialInterval\" : 5, \"maxInterval\" : 5 } }, { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 2, \"multiplier\" : 7, \"initialInterval\" : 5, \"maxInterval\" : 5 } } ], \"id\" : \"id\", \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"workflowId\" : \"workflowId\", \"status\" : \"CREATED\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * PUT /jobs/{id}/restart : Restart a job.
     * Restart a job.
     *
     * @param id The id of a job to restart. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "restartJob",
        summary = "Restart a job.",
        description = "Restart a job.",
        tags = { "jobs" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/jobs/{id}/restart"
    )
    default Mono<ResponseEntity<Void>> restartJob(
        @Parameter(name = "id", description = "The id of a job to restart.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(Mono.empty());

    }


    /**
     * PUT /jobs/{id}/stop : Stop a job.
     * Stop a job.
     *
     * @param id The id of a job to stop. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "stopJob",
        summary = "Stop a job.",
        description = "Stop a job.",
        tags = { "jobs" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/jobs/{id}/stop"
    )
    default Mono<ResponseEntity<Void>> stopJob(
        @Parameter(name = "id", description = "The id of a job to stop.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(Mono.empty());

    }

}
