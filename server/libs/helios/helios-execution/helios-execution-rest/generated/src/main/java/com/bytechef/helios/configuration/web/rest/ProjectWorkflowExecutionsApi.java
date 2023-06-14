/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.6.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.helios.configuration.web.rest;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.bytechef.helios.configuration.web.rest.model.ProjectWorkflowExecutionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-06-14T09:22:28.470279+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "project-workflow-executions", description = "The Automation Project Workflow Executions API")
public interface ProjectWorkflowExecutionsApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /project-workflow-executions/{id} : Get workflow executions by id
     * Get workflow execution by id.
     *
     * @param id The id of a workflow execution. (required)
     * @return The of workflow execution object. (status code 200)
     */
    @Operation(
        operationId = "getProjectWorkflowExecution",
        summary = "Get workflow executions by id",
        description = "Get workflow execution by id.",
        tags = { "project-workflow-executions" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The of workflow execution object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectWorkflowExecutionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-workflow-executions/{id}",
        produces = { "application/json" }
    )
    default ResponseEntity<ProjectWorkflowExecutionModel> getProjectWorkflowExecution(
        @Parameter(name = "id", description = "The id of a workflow execution.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"instance\" : { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"tags\" : [ { \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 3 }, { \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 3 } ], \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectInstanceWorkflows\" : [ { \"projectInstanceId\" : 2, \"__version\" : 9, \"inputs\" : { \"key\" : \"{}\" }, \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"connectionId\" : 5, \"taskName\" : \"taskName\", \"key\" : \"key\" }, { \"connectionId\" : 5, \"taskName\" : \"taskName\", \"key\" : \"key\" } ], \"enabled\" : true, \"workflowId\" : 7 }, { \"projectInstanceId\" : 2, \"__version\" : 9, \"inputs\" : { \"key\" : \"{}\" }, \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"connectionId\" : 5, \"taskName\" : \"taskName\", \"key\" : \"key\" }, { \"connectionId\" : 5, \"taskName\" : \"taskName\", \"key\" : \"key\" } ], \"enabled\" : true, \"workflowId\" : 7 } ], \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 6, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 1, \"status\" : \"DISABLED\" }, \"workflow\" : { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"id\" : \"id\", \"label\" : \"label\" }, \"project\" : { \"workflowIds\" : [ \"workflowIds\", \"workflowIds\" ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"tags\" : [ { \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 3 }, { \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 3 } ], \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 1, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"category\" : { \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 7 }, \"projectVersion\" : 1, \"status\" : \"PUBLISHED\" }, \"id\" : 0 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /project-workflow-executions : Get project workflow executions
     * Get project workflow executions.
     *
     * @param jobStatus The status of an executed job (optional)
     * @param jobStartDate The start date of a job. (optional)
     * @param jobEndDate The end date of a job. (optional)
     * @param projectId The id of a project. (optional)
     * @param projectInstanceId The id of a project instance. (optional)
     * @param workflowId The id of a workflow. (optional)
     * @param pageNumber The number of the page to return. (optional, default to 0)
     * @return The page of workflow executions. (status code 200)
     */
    @Operation(
        operationId = "searchProjectWorkflowExecutions",
        summary = "Get project workflow executions",
        description = "Get project workflow executions.",
        tags = { "project-workflow-executions" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The page of workflow executions.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-workflow-executions",
        produces = { "application/json" }
    )
    default ResponseEntity<org.springframework.data.domain.Page> searchProjectWorkflowExecutions(
        @Parameter(name = "jobStatus", description = "The status of an executed job", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobStatus", required = false) String jobStatus,
        @Parameter(name = "jobStartDate", description = "The start date of a job.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobStartDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime jobStartDate,
        @Parameter(name = "jobEndDate", description = "The end date of a job.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobEndDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime jobEndDate,
        @Parameter(name = "projectId", description = "The id of a project.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "projectId", required = false) Long projectId,
        @Parameter(name = "projectInstanceId", description = "The id of a project instance.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "projectInstanceId", required = false) Long projectInstanceId,
        @Parameter(name = "workflowId", description = "The id of a workflow.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "workflowId", required = false) String workflowId,
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

}
