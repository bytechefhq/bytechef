/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.11.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.workflow.execution.web.rest;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import com.bytechef.embedded.workflow.execution.web.rest.model.WorkflowExecutionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-02-23T10:28:19.430643+01:00[Europe/Zagreb]", comments = "Generator version: 7.11.0")
@Validated
@Tag(name = "workflow-execution", description = "The Embedded Workflow Execution Internal API")
public interface WorkflowExecutionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /workflow-executions/{id} : Get workflow executions by id
     * Get workflow execution by id.
     *
     * @param id The id of an execution. (required)
     * @return The execution object. (status code 200)
     */
    @Operation(
        operationId = "getWorkflowExecution",
        summary = "Get workflow executions by id",
        description = "Get workflow execution by id.",
        tags = { "workflow-execution" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The execution object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowExecutionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workflow-executions/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(
        @Parameter(name = "id", description = "The id of an execution.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"triggerExecution\" : { \"retryDelayFactor\" : 7, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"batch\" : true, \"icon\" : \"icon\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 7, \"title\" : \"title\", \"type\" : \"type\", \"executionTime\" : 3, \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"input\" : { \"key\" : \"\" }, \"retryDelayMillis\" : 6, \"maxRetries\" : 3, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"workflowTrigger\" : { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, \"id\" : \"id\", \"retryAttempts\" : 0, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, \"workflow\" : { \"__version\" : 0, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"integrationWorkflowId\" : 4, \"description\" : \"description\", \"id\" : \"id\", \"label\" : \"label\", \"workflowReferenceCode\" : \"workflowReferenceCode\" }, \"integration\" : { \"lastVersion\" : 1, \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"allowMultipleInstances\" : false, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"lastStatus\" : \"DRAFT\" }, \"id\" : 0, \"integrationInstanceConfiguration\" : { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"integrationId\" : 5, \"integrationInstanceConfigurationWorkflows\" : [ { \"integrationInstanceConfigurationId\" : 3, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"id\" : 9, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 7, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 7, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"integrationInstanceConfigurationId\" : 3, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"id\" : 9, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 7, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 7, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"connectionAuthorizationParameters\" : { \"key\" : \"\" }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 4 }, { \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 4 } ], \"__version\" : 1, \"environment\" : \"TEST\", \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdBy\" : \"createdBy\", \"integrationVersion\" : 2, \"name\" : \"name\", \"integration\" : { \"lastVersion\" : 1, \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"allowMultipleInstances\" : false, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"lastStatus\" : \"DRAFT\" }, \"connectionConnectionParameters\" : { \"key\" : \"\" }, \"id\" : 5 }, \"job\" : { \"outputs\" : { \"key\" : \"\" }, \"taskExecutions\" : [ { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"icon\" : \"icon\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"title\" : \"title\", \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 6, \"id\" : \"id\", \"retryAttempts\" : 9, \"retryDelayFactor\" : 6, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 2 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 2 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 6, \"parentId\" : \"parentId\", \"executionTime\" : 9, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"maxRetries\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 8, \"taskNumber\" : 3, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"icon\" : \"icon\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"title\" : \"title\", \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 6, \"id\" : \"id\", \"retryAttempts\" : 9, \"retryDelayFactor\" : 6, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 2 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 2 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 1, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 6, \"parentId\" : \"parentId\", \"executionTime\" : 9, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"maxRetries\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 8, \"taskNumber\" : 3, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" } ], \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"currentTask\" : 1, \"label\" : \"label\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"parentTaskExecutionId\" : 4, \"webhooks\" : [ { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 5, \"multiplier\" : 6, \"initialInterval\" : 6, \"maxInterval\" : 6 } }, { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 5, \"multiplier\" : 6, \"initialInterval\" : 6, \"maxInterval\" : 6 } } ], \"id\" : \"id\", \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"workflowId\" : \"workflowId\", \"status\" : \"CREATED\" }, \"integrationInstance\" : { \"connectedUserId\" : 1, \"integrationInstanceConfigurationId\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"connectionId\" : 1, \"id\" : 6, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"enabled\" : true } }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workflow-executions : Get Integration workflow executions
     * Get Integration workflow executions.
     *
     * @param environment The environment. (optional)
     * @param jobStatus The status of an executed job (optional)
     * @param jobStartDate The start date of a job. (optional)
     * @param jobEndDate The end date of a job. (optional)
     * @param integrationId The id of a Integration. (optional)
     * @param integrationInstanceConfigurationId The id of a Integration instance configuration. (optional)
     * @param workflowId The id of a workflow. (optional)
     * @param pageNumber The number of the page to return. (optional, default to 0)
     * @return The page of workflow executions. (status code 200)
     */
    @Operation(
        operationId = "getWorkflowExecutionsPage",
        summary = "Get Integration workflow executions",
        description = "Get Integration workflow executions.",
        tags = { "workflow-execution" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The page of workflow executions.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workflow-executions",
        produces = { "application/json" }
    )
    
    default ResponseEntity<org.springframework.data.domain.Page> getWorkflowExecutionsPage(
        @Parameter(name = "environment", description = "The environment.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "environment", required = false) com.bytechef.embedded.configuration.web.rest.model.EnvironmentModel environment,
        @Parameter(name = "jobStatus", description = "The status of an executed job", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobStatus", required = false) String jobStatus,
        @Parameter(name = "jobStartDate", description = "The start date of a job.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobStartDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime jobStartDate,
        @Parameter(name = "jobEndDate", description = "The end date of a job.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobEndDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime jobEndDate,
        @Parameter(name = "integrationId", description = "The id of a Integration.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "integrationId", required = false) Long integrationId,
        @Parameter(name = "integrationInstanceConfigurationId", description = "The id of a Integration instance configuration.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "integrationInstanceConfigurationId", required = false) Long integrationInstanceConfigurationId,
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
