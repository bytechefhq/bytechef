/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.4.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.automation.configuration.web.rest;

import com.bytechef.automation.configuration.web.rest.model.CreateProjectInstanceWorkflowJob200ResponseModel;
import com.bytechef.automation.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceWorkflowModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-03-29T14:33:24.331772+01:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
@Validated
@Tag(name = "project-instance", description = "The Automation Project Instance API")
public interface ProjectInstanceApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /project-instances : Create a new project instance
     * Create a new project instance.
     *
     * @param projectInstanceModel  (required)
     * @return The project instance object. (status code 200)
     */
    @Operation(
        operationId = "createProjectInstance",
        summary = "Create a new project instance",
        description = "Create a new project instance.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The project instance object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectInstanceModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/project-instances",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ProjectInstanceModel> createProjectInstance(
        @Parameter(name = "ProjectInstanceModel", description = "", required = true) @Valid @RequestBody ProjectInstanceModel projectInstanceModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"project\" : { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 6, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectVersion\" : 1 }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 }, { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 } ], \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectInstanceWorkflows\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 5, \"projectVersion\" : 3 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /project-instances/{id}/workflows/{workflowId}/jobs : Create a request for running a new job
     * Create a request for running a new job.
     *
     * @param id The id of a project instance. (required)
     * @param workflowId The id of the workflow to execute. (required)
     * @return The id of a created job. (status code 200)
     */
    @Operation(
        operationId = "createProjectInstanceWorkflowJob",
        summary = "Create a request for running a new job",
        description = "Create a request for running a new job.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The id of a created job.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CreateProjectInstanceWorkflowJob200ResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/project-instances/{id}/workflows/{workflowId}/jobs",
        produces = { "application/json" }
    )
    
    default ResponseEntity<CreateProjectInstanceWorkflowJob200ResponseModel> createProjectInstanceWorkflowJob(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "workflowId", description = "The id of the workflow to execute.", required = true, in = ParameterIn.PATH) @PathVariable("workflowId") String workflowId
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
     * DELETE /project-instances/{id} : Delete a project instance
     * Delete a project instance.
     *
     * @param id The id of a project instance. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteProjectInstance",
        summary = "Delete a project instance",
        description = "Delete a project instance.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/project-instances/{id}"
    )
    
    default ResponseEntity<Void> deleteProjectInstance(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /project-instances/{id}/enable/{enable} : Enable/disable a project instance
     * Enable/disable a project instance.
     *
     * @param id The id of a project instance. (required)
     * @param enable Enable/disable the project instance. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "enableProjectInstance",
        summary = "Enable/disable a project instance",
        description = "Enable/disable a project instance.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/project-instances/{id}/enable/{enable}"
    )
    
    default ResponseEntity<Void> enableProjectInstance(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "enable", description = "Enable/disable the project instance.", required = true, in = ParameterIn.PATH) @PathVariable("enable") Boolean enable
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /project-instances/{id}/workflows/{workflowId}/enable/{enable} : Enable/disable a workflow of a project instance
     * Enable/disable a workflow of a project instance.
     *
     * @param id The id of a project instance. (required)
     * @param workflowId The id of a project workflow. (required)
     * @param enable Enable/disable the workflow of a project instance. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "enableProjectInstanceWorkflow",
        summary = "Enable/disable a workflow of a project instance",
        description = "Enable/disable a workflow of a project instance.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/project-instances/{id}/workflows/{workflowId}/enable/{enable}"
    )
    
    default ResponseEntity<Void> enableProjectInstanceWorkflow(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "workflowId", description = "The id of a project workflow.", required = true, in = ParameterIn.PATH) @PathVariable("workflowId") String workflowId,
        @Parameter(name = "enable", description = "Enable/disable the workflow of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("enable") Boolean enable
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /project-instances/{id} : Get a project instance by id
     * Get a project instance by id.
     *
     * @param id The id of a project instance. (required)
     * @return The project instance object. (status code 200)
     */
    @Operation(
        operationId = "getProjectInstance",
        summary = "Get a project instance by id",
        description = "Get a project instance by id.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The project instance object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectInstanceModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-instances/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ProjectInstanceModel> getProjectInstance(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"project\" : { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 6, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectVersion\" : 1 }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 }, { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 } ], \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectInstanceWorkflows\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 5, \"projectVersion\" : 3 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /project-instances : Get project instances
     * Get project instances.
     *
     * @param environment The project ids used for filtering project instances. (optional)
     * @param projectId The project ids used for filtering project instances. (optional)
     * @param tagId The tag id of used for filtering project instances. (optional)
     * @return The list of project instances. (status code 200)
     */
    @Operation(
        operationId = "getProjectInstances",
        summary = "Get project instances",
        description = "Get project instances.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of project instances.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProjectInstanceModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-instances",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ProjectInstanceModel>> getProjectInstances(
        @Parameter(name = "environment", description = "The project ids used for filtering project instances.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "environment", required = false) EnvironmentModel environment,
        @Parameter(name = "projectId", description = "The project ids used for filtering project instances.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "projectId", required = false) Long projectId,
        @Parameter(name = "tagId", description = "The tag id of used for filtering project instances.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "tagId", required = false) Long tagId
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"project\" : { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 6, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectVersion\" : 1 }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 }, { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 } ], \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectInstanceWorkflows\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 5, \"projectVersion\" : 3 }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"project\" : { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 6, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectVersion\" : 1 }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 }, { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 } ], \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectInstanceWorkflows\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 5, \"projectVersion\" : 3 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /project-instances/{id} : Update an existing project instance
     * Update an existing project instance.
     *
     * @param id The id of a project instance. (required)
     * @param projectInstanceModel  (required)
     * @return The updated project instance object. (status code 200)
     */
    @Operation(
        operationId = "updateProjectInstance",
        summary = "Update an existing project instance",
        description = "Update an existing project instance.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated project instance object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectInstanceModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/project-instances/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ProjectInstanceModel> updateProjectInstance(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "ProjectInstanceModel", description = "", required = true) @Valid @RequestBody ProjectInstanceModel projectInstanceModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"project\" : { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 6, \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectVersion\" : 1 }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 }, { \"__version\" : 4, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 2 } ], \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectInstanceWorkflows\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 5, \"projectVersion\" : 3 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /project-instances/{id}/project-instance-workflows/{projectInstanceWorkflowId} : Update an existing project instance workflow
     * Update an existing project instance workflow.
     *
     * @param id The id of a project instance. (required)
     * @param projectInstanceWorkflowId The id of a project instance workflow. (required)
     * @param projectInstanceWorkflowModel  (required)
     * @return The updated project instance workflow object. (status code 200)
     */
    @Operation(
        operationId = "updateProjectInstanceWorkflow",
        summary = "Update an existing project instance workflow",
        description = "Update an existing project instance workflow.",
        tags = { "project-instance" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated project instance workflow object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectInstanceWorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/project-instances/{id}/project-instance-workflows/{projectInstanceWorkflowId}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ProjectInstanceWorkflowModel> updateProjectInstanceWorkflow(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "projectInstanceWorkflowId", description = "The id of a project instance workflow.", required = true, in = ParameterIn.PATH) @PathVariable("projectInstanceWorkflowId") Long projectInstanceWorkflowId,
        @Parameter(name = "ProjectInstanceWorkflowModel", description = "", required = true) @Valid @RequestBody ProjectInstanceWorkflowModel projectInstanceWorkflowModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"{}\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"projectInstanceId\" : 7, \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 2, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
