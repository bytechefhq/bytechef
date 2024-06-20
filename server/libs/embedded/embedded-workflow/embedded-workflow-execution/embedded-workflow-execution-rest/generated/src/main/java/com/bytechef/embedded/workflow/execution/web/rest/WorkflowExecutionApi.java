/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.workflow.execution.web.rest;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-19T22:37:09.039219+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
@Validated
@Tag(name = "workflow-execution", description = "The Embedded Workflow Execution API")
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
                    String exampleString = "{ \"triggerExecution\" : { \"retryDelayFactor\" : 3, \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"batch\" : true, \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 7, \"type\" : \"type\", \"executionTime\" : 4, \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"input\" : { \"key\" : \"\" }, \"retryDelayMillis\" : 7, \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 9, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 3, \"triggersCount\" : 6 }, \"maxRetries\" : 8, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"workflowTrigger\" : { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 3, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 3, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, \"id\" : \"id\", \"retryAttempts\" : 3, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, \"workflow\" : { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"integrationWorkflowId\" : 3.2588565619047607, \"description\" : \"description\", \"id\" : \"id\", \"label\" : \"label\", \"workflowReferenceCode\" : \"workflowReferenceCode\" }, \"integration\" : { \"allowMultipleInstances\" : false, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"integrationVersion\" : 5, \"description\" : \"description\", \"componentName\" : \"componentName\", \"componentVersion\" : 6, \"id\" : 1, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\" }, \"id\" : 0, \"integrationInstanceConfiguration\" : { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"integrationId\" : 3, \"integrationInstanceConfigurationWorkflows\" : [ { \"integrationInstanceConfigurationId\" : 7, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 4, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 2, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 2, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"integrationInstanceConfigurationId\" : 7, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 4, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 2, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 2, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"enabled\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ], \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"integration\" : { \"allowMultipleInstances\" : false, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"integrationVersion\" : 9, \"description\" : \"description\", \"componentName\" : \"componentName\", \"componentVersion\" : 2, \"id\" : 7, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\" }, \"integrationVersion\" : 1, \"name\" : \"name\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, \"job\" : { \"outputs\" : { \"key\" : \"\" }, \"taskExecutions\" : [ { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 3, \"id\" : \"id\", \"retryAttempts\" : 6, \"retryDelayFactor\" : 5, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 7 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 7 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 3, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 3, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 2, \"parentId\" : \"parentId\", \"executionTime\" : 6, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 9, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 3, \"triggersCount\" : 6 }, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 6, \"taskNumber\" : 6, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" }, { \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"type\" : \"type\", \"output\" : \"{}\", \"retryDelay\" : \"retryDelay\", \"retryDelayMillis\" : 3, \"id\" : \"id\", \"retryAttempts\" : 6, \"retryDelayFactor\" : 5, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"workflowTask\" : { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 7 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 7 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 3, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 3, \"key\" : \"key\", \"required\" : true } ] }, \"priority\" : 2, \"parentId\" : \"parentId\", \"executionTime\" : 6, \"input\" : { \"key\" : \"\" }, \"jobId\" : \"jobId\", \"component\" : { \"icon\" : \"icon\", \"name\" : \"name\", \"actionsCount\" : 9, \"description\" : \"description\", \"title\" : \"title\", \"version\" : 3, \"triggersCount\" : 6 }, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"progress\" : 6, \"taskNumber\" : 6, \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"status\" : \"CREATED\" } ], \"endDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"currentTask\" : 9, \"label\" : \"label\", \"error\" : { \"stackTrace\" : [ \"stackTrace\", \"stackTrace\" ], \"message\" : \"message\" }, \"priority\" : 8, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"parentTaskExecutionId\" : 6, \"webhooks\" : [ { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 6, \"multiplier\" : 0, \"initialInterval\" : 0, \"maxInterval\" : 7 } }, { \"type\" : \"type\", \"url\" : \"url\", \"retry\" : { \"maxAttempts\" : 6, \"multiplier\" : 0, \"initialInterval\" : 0, \"maxInterval\" : 7 } } ], \"id\" : \"id\", \"startDate\" : \"2000-01-23T04:56:07.000+00:00\", \"workflowId\" : \"workflowId\", \"status\" : \"CREATED\" }, \"integrationInstance\" : { \"connectedUserId\" : 4, \"integrationInstanceConfigurationId\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"connectionId\" : 1, \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"enabled\" : true } }";
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
        @Parameter(name = "jobStartDate", description = "The start date of a job.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobStartDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime jobStartDate,
        @Parameter(name = "jobEndDate", description = "The end date of a job.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "jobEndDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime jobEndDate,
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
