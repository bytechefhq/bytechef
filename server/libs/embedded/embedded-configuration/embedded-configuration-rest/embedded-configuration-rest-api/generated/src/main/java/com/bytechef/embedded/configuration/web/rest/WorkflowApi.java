/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.configuration.web.rest;

import com.bytechef.embedded.configuration.web.rest.model.WorkflowModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-10T21:49:30.205405+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "workflow", description = "The Embedded Workflow Internal API")
public interface WorkflowApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * DELETE /workflows/{id} : Delete a workflow
     * Delete a workflow.
     *
     * @param id The id of the workflow to delete. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "deleteWorkflow",
        summary = "Delete a workflow",
        description = "Delete a workflow.",
        tags = { "workflow" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/workflows/{id}"
    )
    
    default ResponseEntity<Void> deleteWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to delete.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /integrations/{id}/versions/{integrationVersion}/workflows : Get workflows for particular integration version.
     * Get workflows for particular integration version.
     *
     * @param id The id of a integration. (required)
     * @param integrationVersion The version of a integration. (required)
     * @param includeAllFields Use for including all fields or just basic ones. (optional, default to true)
     * @return The array of integration workflows. (status code 200)
     */
    @Operation(
        operationId = "getIntegrationVersionWorkflows",
        summary = "Get workflows for particular integration version.",
        description = "Get workflows for particular integration version.",
        tags = { "workflow" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The array of integration workflows.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WorkflowModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations/{id}/versions/{integrationVersion}/workflows",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<WorkflowModel>> getIntegrationVersionWorkflows(
        @Parameter(name = "id", description = "The id of a integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "integrationVersion", description = "The version of a integration.", required = true, in = ParameterIn.PATH) @PathVariable("integrationVersion") Integer integrationVersion,
        @Parameter(name = "includeAllFields", description = "Use for including all fields or just basic ones.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "includeAllFields", required = false, defaultValue = "true") Boolean includeAllFields
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"format\" : \"JSON\", \"description\" : \"description\", \"workflowTaskComponentNames\" : [ \"workflowTaskComponentNames\", \"workflowTaskComponentNames\" ], \"label\" : \"label\", \"inputsCount\" : 6, \"triggers\" : [ { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ], \"connectionsCount\" : 0, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 5, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"workflowTriggerComponentNames\" : [ \"workflowTriggerComponentNames\", \"workflowTriggerComponentNames\" ], \"integrationWorkflowId\" : 2, \"definition\" : \"definition\", \"id\" : \"id\", \"tasks\" : [ { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ] }, { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"format\" : \"JSON\", \"description\" : \"description\", \"workflowTaskComponentNames\" : [ \"workflowTaskComponentNames\", \"workflowTaskComponentNames\" ], \"label\" : \"label\", \"inputsCount\" : 6, \"triggers\" : [ { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ], \"connectionsCount\" : 0, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 5, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"workflowTriggerComponentNames\" : [ \"workflowTriggerComponentNames\", \"workflowTriggerComponentNames\" ], \"integrationWorkflowId\" : 2, \"definition\" : \"definition\", \"id\" : \"id\", \"tasks\" : [ { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ] } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workflows/by-integration-workflow-id/{integrationWorkflowId} : Get workflow for particular integration.
     * Get workflow for particular integration.
     *
     * @param integrationWorkflowId The id of an integration workflow. (required)
     * @return The integration workflow object. (status code 200)
     */
    @Operation(
        operationId = "getIntegrationWorkflow",
        summary = "Get workflow for particular integration.",
        description = "Get workflow for particular integration.",
        tags = { "workflow" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The integration workflow object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workflows/by-integration-workflow-id/{integrationWorkflowId}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<WorkflowModel> getIntegrationWorkflow(
        @Parameter(name = "integrationWorkflowId", description = "The id of an integration workflow.", required = true, in = ParameterIn.PATH) @PathVariable("integrationWorkflowId") Long integrationWorkflowId
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"format\" : \"JSON\", \"description\" : \"description\", \"workflowTaskComponentNames\" : [ \"workflowTaskComponentNames\", \"workflowTaskComponentNames\" ], \"label\" : \"label\", \"inputsCount\" : 6, \"triggers\" : [ { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ], \"connectionsCount\" : 0, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 5, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"workflowTriggerComponentNames\" : [ \"workflowTriggerComponentNames\", \"workflowTriggerComponentNames\" ], \"integrationWorkflowId\" : 2, \"definition\" : \"definition\", \"id\" : \"id\", \"tasks\" : [ { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /integrations/{id}/workflows : Get integration workflows for particular integration
     * Get integration workflows for particular integration.
     *
     * @param id The id of an integration. (required)
     * @return The updated integration object. (status code 200)
     */
    @Operation(
        operationId = "getIntegrationWorkflows",
        summary = "Get integration workflows for particular integration",
        description = "Get integration workflows for particular integration.",
        tags = { "workflow" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated integration object.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WorkflowModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations/{id}/workflows",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<WorkflowModel>> getIntegrationWorkflows(
        @Parameter(name = "id", description = "The id of an integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"format\" : \"JSON\", \"description\" : \"description\", \"workflowTaskComponentNames\" : [ \"workflowTaskComponentNames\", \"workflowTaskComponentNames\" ], \"label\" : \"label\", \"inputsCount\" : 6, \"triggers\" : [ { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ], \"connectionsCount\" : 0, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 5, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"workflowTriggerComponentNames\" : [ \"workflowTriggerComponentNames\", \"workflowTriggerComponentNames\" ], \"integrationWorkflowId\" : 2, \"definition\" : \"definition\", \"id\" : \"id\", \"tasks\" : [ { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ] }, { \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"format\" : \"JSON\", \"description\" : \"description\", \"workflowTaskComponentNames\" : [ \"workflowTaskComponentNames\", \"workflowTaskComponentNames\" ], \"label\" : \"label\", \"inputsCount\" : 6, \"triggers\" : [ { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ], \"connectionsCount\" : 0, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 5, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"workflowTriggerComponentNames\" : [ \"workflowTriggerComponentNames\", \"workflowTriggerComponentNames\" ], \"integrationWorkflowId\" : 2, \"definition\" : \"definition\", \"id\" : \"id\", \"tasks\" : [ { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ] } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workflows/{id} : Get a workflow by id
     * Get a workflow by id.
     *
     * @param id The id of the workflow to get. (required)
     * @return The workflow object. (status code 200)
     */
    @Operation(
        operationId = "getWorkflow",
        summary = "Get a workflow by id",
        description = "Get a workflow by id.",
        tags = { "workflow" },
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
    
    default ResponseEntity<WorkflowModel> getWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to get.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"format\" : \"JSON\", \"description\" : \"description\", \"workflowTaskComponentNames\" : [ \"workflowTaskComponentNames\", \"workflowTaskComponentNames\" ], \"label\" : \"label\", \"inputsCount\" : 6, \"triggers\" : [ { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ], \"connectionsCount\" : 0, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 5, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"workflowTriggerComponentNames\" : [ \"workflowTriggerComponentNames\", \"workflowTriggerComponentNames\" ], \"integrationWorkflowId\" : 2, \"definition\" : \"definition\", \"id\" : \"id\", \"tasks\" : [ { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"node\" : \"node\", \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"post\" : [ null, null ], \"name\" : \"name\", \"description\" : \"description\", \"finalize\" : [ null, null ], \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /workflows/{id} : Update an existing workflow
     * Update an existing workflow.
     *
     * @param id The id of the workflow to update. (required)
     * @param workflowModel  (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "updateWorkflow",
        summary = "Update an existing workflow",
        description = "Update an existing workflow.",
        tags = { "workflow" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/workflows/{id}",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateWorkflow(
        @Parameter(name = "id", description = "The id of the workflow to update.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @Parameter(name = "WorkflowModel", description = "", required = true) @Valid @RequestBody WorkflowModel workflowModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
