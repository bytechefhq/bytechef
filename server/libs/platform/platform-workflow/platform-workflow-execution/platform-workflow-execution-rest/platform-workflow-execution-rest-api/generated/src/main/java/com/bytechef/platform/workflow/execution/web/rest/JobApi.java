/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.8.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.platform.workflow.execution.web.rest;

import com.bytechef.platform.workflow.execution.web.rest.model.CreateJob200ResponseModel;
import com.bytechef.platform.workflow.execution.web.rest.model.JobModel;
import com.bytechef.platform.workflow.execution.web.rest.model.JobParametersModel;
import com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-09-18T05:52:29.956477+02:00[Europe/Zagreb]", comments = "Generator version: 7.8.0")
@Validated
@Tag(name = "job", description = "The Platform Workflow Job API")
public interface JobApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /jobs : Create a request for running a new job
     * Create a request for running a new job.
     *
     * @param jobParametersModel Parameters required to run a job, for example &#39;{\&quot;workflowId\&quot;:\&quot;samples/hello\&quot;,\&quot;inputs\&quot;:{\&quot;yourName\&quot;:\&quot;Joe Jones\&quot;}}&#39; (required)
     * @return The id of a created job. (status code 200)
     */
    @Operation(
        operationId = "createJob",
        summary = "Create a request for running a new job",
        description = "Create a request for running a new job.",
        tags = { "job" },
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
    
    default ResponseEntity<CreateJob200ResponseModel> createJob(
        @Parameter(name = "JobParametersModel", description = "Parameters required to run a job, for example '{\"workflowId\":\"samples/hello\",\"inputs\":{\"yourName\":\"Joe Jones\"}}'", required = true) @Valid @RequestBody JobParametersModel jobParametersModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"jobId\" : 0 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /jobs/{id} : Get a job by id
     * Get a job by id.
     *
     * @param id The id of a job to return. (required)
     * @return The job object. (status code 200)
     */
    @Operation(
        operationId = "getJob",
        summary = "Get a job by id",
        description = "Get a job by id.",
        tags = { "job" },
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
    
    default ResponseEntity<JobModel> getJob(
        @Parameter(name = "id", description = "The id of a job to return.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"outputs\" : { \"key\" : \"\" }, \"taskExecutions\" : [ { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 1, \"id\" : \"id\", \"retryAttempts\" : 4, \"retryDelayFactor\" : 7, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 3, \"parentId\" : \"parentId\", \"executionTime\" : 7, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 5, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 2, \"triggersCount\" : 5 }, \"maxRetries\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 2, \"taskNumber\" : 1, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 1, \"id\" : \"id\", \"retryAttempts\" : 4, \"retryDelayFactor\" : 7, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 3, \"parentId\" : \"parentId\", \"executionTime\" : 7, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 5, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 2, \"triggersCount\" : 5 }, \"maxRetries\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 2, \"taskNumber\" : 1, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" } ], \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"currentTask\" : 0, \"label\" : \"label\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"parentTaskExecutionId\" : 6, \"webhooks\" : [ { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 4, \"multiplier\" : 5, \"initialInterval\" : 7, \"maxInterval\" : 1 } }, { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 4, \"multiplier\" : 5, \"initialInterval\" : 7, \"maxInterval\" : 1 } } ], \"id\" : \"id\", \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"workflowId\" : \"workflowId\", \"status\" : \"CREATED\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /jobs : Get a page of jobs
     * Get a page of jobs.
     *
     * @param pageNumber The number of the page to return. (optional, default to 0)
     * @return The page of jobs. (status code 200)
     */
    @Operation(
        operationId = "getJobsPage",
        summary = "Get a page of jobs",
        description = "Get a page of jobs.",
        tags = { "job" },
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
    
    default ResponseEntity<org.springframework.data.domain.Page> getJobsPage(
        @Parameter(name = "pageNumber", description = "The number of the page to return.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "pageNumber", required = false, defaultValue = "0") Integer pageNumber
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"number\" : 0, \"size\" : 6, \"numberOfElements\" : 1, \"totalPages\" : 5, \"content\" : [ \"{}\", \"{}\" ], \"totalElements\" : 5 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /jobs/latest : Get the latest job
     * Get the latest job.
     *
     * @return The latest job. (status code 200)
     */
    @Operation(
        operationId = "getLatestJob",
        summary = "Get the latest job",
        description = "Get the latest job.",
        tags = { "job" },
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
    
    default ResponseEntity<JobModel> getLatestJob(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"outputs\" : { \"key\" : \"\" }, \"taskExecutions\" : [ { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 1, \"id\" : \"id\", \"retryAttempts\" : 4, \"retryDelayFactor\" : 7, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 3, \"parentId\" : \"parentId\", \"executionTime\" : 7, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 5, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 2, \"triggersCount\" : 5 }, \"maxRetries\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 2, \"taskNumber\" : 1, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 1, \"id\" : \"id\", \"retryAttempts\" : 4, \"retryDelayFactor\" : 7, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 6 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 3, \"parentId\" : \"parentId\", \"executionTime\" : 7, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 5, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 2, \"triggersCount\" : 5 }, \"maxRetries\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 2, \"taskNumber\" : 1, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" } ], \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"currentTask\" : 0, \"label\" : \"label\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"parentTaskExecutionId\" : 6, \"webhooks\" : [ { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 4, \"multiplier\" : 5, \"initialInterval\" : 7, \"maxInterval\" : 1 } }, { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 4, \"multiplier\" : 5, \"initialInterval\" : 7, \"maxInterval\" : 1 } } ], \"id\" : \"id\", \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"workflowId\" : \"workflowId\", \"status\" : \"CREATED\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /trigger-executions/latest : Get the latest trigger execution
     * Get the latest trigger execution.
     *
     * @return The latest trigger execution. (status code 200)
     */
    @Operation(
        operationId = "getLatestTriggerExecution",
        summary = "Get the latest trigger execution",
        description = "Get the latest trigger execution.",
        tags = { "job" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The latest trigger execution.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = TriggerExecutionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/trigger-executions/latest",
        produces = { "application/json" }
    )
    
    default ResponseEntity<TriggerExecutionModel> getLatestTriggerExecution(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"retryDelayFactor\" : 5, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"batch\" : true, \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 1, \"type\" : \"type\", \"executionTime\" : 0, \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"input\" : { \"key\" : \"\" }, \"retryDelayMillis\" : 2, \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 5, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 2, \"triggersCount\" : 5 }, \"maxRetries\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"workflowTrigger\" : { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, \"id\" : \"id\", \"retryAttempts\" : 5, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /jobs/{id}/restart : Restart a job
     * Restart a job.
     *
     * @param id The id of a job to restart. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "restartJob",
        summary = "Restart a job",
        description = "Restart a job.",
        tags = { "job" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/jobs/{id}/restart"
    )
    
    default ResponseEntity<Void> restartJob(
        @Parameter(name = "id", description = "The id of a job to restart.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /jobs/{id}/stop : Stop a job
     * Stop a job.
     *
     * @param id The id of a job to stop. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "stopJob",
        summary = "Stop a job",
        description = "Stop a job.",
        tags = { "job" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/jobs/{id}/stop"
    )
    
    default ResponseEntity<Void> stopJob(
        @Parameter(name = "id", description = "The id of a job to stop.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
