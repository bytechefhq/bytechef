/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.configuration.web.rest;

import com.bytechef.embedded.configuration.web.rest.model.CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel;
import com.bytechef.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-08-10T18:39:37.461007+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
@Validated
@Tag(name = "integration-instance-configuration", description = "the integration-instance-configuration API")
public interface IntegrationInstanceConfigurationApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /integration-instance-configurations : Create a new integration instance configuration
     * Create a new integration instance configuration.
     *
     * @param integrationInstanceConfigurationModel  (required)
     * @return The integration instance configuration object. (status code 200)
     */
    @Operation(
        operationId = "createIntegrationInstanceConfiguration",
        summary = "Create a new integration instance configuration",
        description = "Create a new integration instance configuration.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The integration instance configuration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationInstanceConfigurationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/integration-instance-configurations",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<IntegrationInstanceConfigurationModel> createIntegrationInstanceConfiguration(
        @Parameter(name = "IntegrationInstanceConfigurationModel", description = "", required = true) @Valid @RequestBody IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"integrationId\" : 6, \"integrationInstanceConfigurationWorkflows\" : [ { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"connectionAuthorizationParameters\" : { \"key\" : \"\" }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdBy\" : \"createdBy\", \"integrationVersion\" : 1, \"name\" : \"name\", \"connectionConnectionParameters\" : { \"key\" : \"\" }, \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /integration-instance-configurations/{id}/workflows/{workflowId}/jobs : Create a request for running a new job
     * Create a request for running a new job.
     *
     * @param id The id of an integration instance configuration. (required)
     * @param workflowId The id of the workflow to execute. (required)
     * @return The id of a created job. (status code 200)
     */
    @Operation(
        operationId = "createIntegrationInstanceConfigurationWorkflowJob",
        summary = "Create a request for running a new job",
        description = "Create a request for running a new job.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The id of a created job.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/integration-instance-configurations/{id}/workflows/{workflowId}/jobs",
        produces = { "application/json" }
    )
    
    default ResponseEntity<CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel> createIntegrationInstanceConfigurationWorkflowJob(
        @Parameter(name = "id", description = "The id of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
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
     * DELETE /integration-instance-configurations/{id} : Delete an integration instance configuration
     * Delete an integration instance configuration.
     *
     * @param id The id of an integration instance configuration. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteIntegrationInstanceConfiguration",
        summary = "Delete an integration instance configuration",
        description = "Delete an integration instance configuration.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/integration-instance-configurations/{id}"
    )
    
    default ResponseEntity<Void> deleteIntegrationInstanceConfiguration(
        @Parameter(name = "id", description = "The id of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /integration-instance-configurations/{id}/enable/{enable} : Enable/disable an integration instance configuration
     * Enable/disable an integration instance configuration.
     *
     * @param id The id of an integration instance configuration. (required)
     * @param enable Enable/disable the integration instance configuration. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "enableIntegrationInstanceConfiguration",
        summary = "Enable/disable an integration instance configuration",
        description = "Enable/disable an integration instance configuration.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/integration-instance-configurations/{id}/enable/{enable}"
    )
    
    default ResponseEntity<Void> enableIntegrationInstanceConfiguration(
        @Parameter(name = "id", description = "The id of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "enable", description = "Enable/disable the integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("enable") Boolean enable
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /integration-instance-configurations/{id}/workflows/{workflowId}/enable/{enable} : Enable/disable a workflow of an integration instance configuration
     * Enable/disable a workflow of an integration instance configuration.
     *
     * @param id The id of an integration instance configuration. (required)
     * @param workflowId The id of an integration workflow. (required)
     * @param enable Enable/disable the workflow of an integration instance configuration. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "enableIntegrationInstanceConfigurationWorkflow",
        summary = "Enable/disable a workflow of an integration instance configuration",
        description = "Enable/disable a workflow of an integration instance configuration.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/integration-instance-configurations/{id}/workflows/{workflowId}/enable/{enable}"
    )
    
    default ResponseEntity<Void> enableIntegrationInstanceConfigurationWorkflow(
        @Parameter(name = "id", description = "The id of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "workflowId", description = "The id of an integration workflow.", required = true, in = ParameterIn.PATH) @PathVariable("workflowId") String workflowId,
        @Parameter(name = "enable", description = "Enable/disable the workflow of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("enable") Boolean enable
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /integration-instance-configurations/{id} : Get an integration instance configuration by id
     * Get an integration instance configuration by id.
     *
     * @param id The id of an integration instance configuration. (required)
     * @return The integration instance configuration object. (status code 200)
     */
    @Operation(
        operationId = "getIntegrationInstanceConfiguration",
        summary = "Get an integration instance configuration by id",
        description = "Get an integration instance configuration by id.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The integration instance configuration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationInstanceConfigurationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integration-instance-configurations/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<IntegrationInstanceConfigurationModel> getIntegrationInstanceConfiguration(
        @Parameter(name = "id", description = "The id of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"integrationId\" : 6, \"integrationInstanceConfigurationWorkflows\" : [ { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"connectionAuthorizationParameters\" : { \"key\" : \"\" }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdBy\" : \"createdBy\", \"integrationVersion\" : 1, \"name\" : \"name\", \"connectionConnectionParameters\" : { \"key\" : \"\" }, \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /integration-instance-configurations : Get integration instance configurations
     * Get integration instance configurations.
     *
     * @param environment The environment. (optional)
     * @param integrationId The integration id used for filtering integration instance configurations. (optional)
     * @param tagId The tag id of used for filtering integration instance configurations. (optional)
     * @return The list of integration instance configurations. (status code 200)
     */
    @Operation(
        operationId = "getIntegrationInstanceConfigurations",
        summary = "Get integration instance configurations",
        description = "Get integration instance configurations.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of integration instance configurations.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = IntegrationInstanceConfigurationModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integration-instance-configurations",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<IntegrationInstanceConfigurationModel>> getIntegrationInstanceConfigurations(
        @Parameter(name = "environment", description = "The environment.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "environment", required = false) EnvironmentModel environment,
        @Parameter(name = "integrationId", description = "The integration id used for filtering integration instance configurations.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "integrationId", required = false) Long integrationId,
        @Parameter(name = "tagId", description = "The tag id of used for filtering integration instance configurations.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "tagId", required = false) Long tagId
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"integrationId\" : 6, \"integrationInstanceConfigurationWorkflows\" : [ { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"connectionAuthorizationParameters\" : { \"key\" : \"\" }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdBy\" : \"createdBy\", \"integrationVersion\" : 1, \"name\" : \"name\", \"connectionConnectionParameters\" : { \"key\" : \"\" }, \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"integrationId\" : 6, \"integrationInstanceConfigurationWorkflows\" : [ { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"connectionAuthorizationParameters\" : { \"key\" : \"\" }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdBy\" : \"createdBy\", \"integrationVersion\" : 1, \"name\" : \"name\", \"connectionConnectionParameters\" : { \"key\" : \"\" }, \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /integration-instance-configurations/{id} : Update an existing integration instance configuration
     * Update an existing integration instance configuration.
     *
     * @param id The id of an integration instance configuration. (required)
     * @param integrationInstanceConfigurationModel  (required)
     * @return The updated integration instance configuration object. (status code 200)
     */
    @Operation(
        operationId = "updateIntegrationInstanceConfiguration",
        summary = "Update an existing integration instance configuration",
        description = "Update an existing integration instance configuration.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated integration instance configuration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationInstanceConfigurationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/integration-instance-configurations/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<IntegrationInstanceConfigurationModel> updateIntegrationInstanceConfiguration(
        @Parameter(name = "id", description = "The id of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "IntegrationInstanceConfigurationModel", description = "", required = true) @Valid @RequestBody IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"integrationId\" : 6, \"integrationInstanceConfigurationWorkflows\" : [ { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }, { \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" } ], \"connectionAuthorizationParameters\" : { \"key\" : \"\" }, \"enabled\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdBy\" : \"createdBy\", \"integrationVersion\" : 1, \"name\" : \"name\", \"connectionConnectionParameters\" : { \"key\" : \"\" }, \"id\" : 0, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /integration-instance-configurations/{id}/workflows/{workflowId} : Update an existing integration instance configuration workflow
     * Update an existing integration instance configuration workflow.
     *
     * @param id The id of an integration instance configuration. (required)
     * @param workflowId The id of an integration instance configuration workflow. (required)
     * @param integrationInstanceConfigurationWorkflowModel  (required)
     * @return The updated integration instance configuration workflow object. (status code 200)
     */
    @Operation(
        operationId = "updateIntegrationInstanceConfigurationWorkflow",
        summary = "Update an existing integration instance configuration workflow",
        description = "Update an existing integration instance configuration workflow.",
        tags = { "integration-instance-configuration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated integration instance configuration workflow object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationInstanceConfigurationWorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/integration-instance-configurations/{id}/workflows/{workflowId}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<IntegrationInstanceConfigurationWorkflowModel> updateIntegrationInstanceConfigurationWorkflow(
        @Parameter(name = "id", description = "The id of an integration instance configuration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "workflowId", description = "The id of an integration instance configuration workflow.", required = true, in = ParameterIn.PATH) @PathVariable("workflowId") Long workflowId,
        @Parameter(name = "IntegrationInstanceConfigurationWorkflowModel", description = "", required = true) @Valid @RequestBody IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflowModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"integrationInstanceConfigurationId\" : 2, \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : { \"key\" : \"\" }, \"lastModifiedBy\" : \"lastModifiedBy\", \"enabled\" : true, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 7, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"staticWebhookUrl\" : \"staticWebhookUrl\", \"id\" : 5, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" }, { \"workflowNodeName\" : \"workflowNodeName\", \"connectionId\" : 5, \"key\" : \"key\" } ], \"workflowId\" : \"workflowId\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
