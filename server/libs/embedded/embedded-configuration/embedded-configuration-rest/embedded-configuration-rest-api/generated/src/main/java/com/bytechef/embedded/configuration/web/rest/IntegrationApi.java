/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.configuration.web.rest;

import com.bytechef.embedded.configuration.web.rest.model.IntegrationModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationStatusModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationVersionModel;
import com.bytechef.embedded.configuration.web.rest.model.PublishIntegrationRequestModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-08-10T18:39:37.461007+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
@Validated
@Tag(name = "integration", description = "The Embedded Integration Internal API")
public interface IntegrationApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /integrations : Create a new integration
     * Create a new integration.
     *
     * @param integrationModel  (required)
     * @return The integration object. (status code 200)
     */
    @Operation(
        operationId = "createIntegration",
        summary = "Create a new integration",
        description = "Create a new integration.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The integration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/integrations",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<IntegrationModel> createIntegration(
        @Parameter(name = "IntegrationModel", description = "", required = true) @Valid @RequestBody IntegrationModel integrationModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"integrationWorkflowIds\" : [ \"integrationWorkflowIds\", \"integrationWorkflowIds\" ], \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"componentVersion\" : 0, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"allowMultipleInstances\" : false, \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"componentName\" : \"componentName\", \"id\" : 6, \"category\" : { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, \"lastIntegrationVersion\" : 1 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /integrations/{id}/workflows : Create new workflow and adds it to an existing integration
     * Create new workflow and adds it to an existing integration.
     *
     * @param id The id of an integration. (required)
     * @param workflowModel  (required)
     * @return The updated integration object. (status code 200)
     */
    @Operation(
        operationId = "createIntegrationWorkflow",
        summary = "Create new workflow and adds it to an existing integration",
        description = "Create new workflow and adds it to an existing integration.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated integration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/integrations/{id}/workflows",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<WorkflowModel> createIntegrationWorkflow(
        @Parameter(name = "id", description = "The id of an integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "WorkflowModel", description = "", required = true) @Valid @RequestBody WorkflowModel workflowModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"outputs\" : [ { \"name\" : \"name\", \"value\" : \"{}\" }, { \"name\" : \"name\", \"value\" : \"{}\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"inputs\" : [ { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false }, { \"name\" : \"name\", \"label\" : \"label\", \"type\" : \"string\", \"required\" : false } ], \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"workflowTaskComponentNames\" : [ \"workflowTaskComponentNames\", \"workflowTaskComponentNames\" ], \"label\" : \"label\", \"inputsCount\" : 6, \"triggers\" : [ { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" }, { \"metadata\" : { \"key\" : \"\" }, \"name\" : \"name\", \"description\" : \"description\", \"label\" : \"label\", \"type\" : \"type\", \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ], \"timeout\" : \"timeout\" } ], \"connectionsCount\" : 0, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"__version\" : 2, \"maxRetries\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"sourceType\" : \"CLASSPATH\", \"workflowTriggerComponentNames\" : [ \"workflowTriggerComponentNames\", \"workflowTriggerComponentNames\" ], \"integrationWorkflowId\" : 7.061401241503109, \"definition\" : \"definition\", \"id\" : \"id\", \"tasks\" : [ { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 5 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 5 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ] }, { \"metadata\" : { \"key\" : \"\" }, \"pre\" : [ null, null ], \"destination\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 5 }, \"description\" : \"description\", \"label\" : \"label\", \"source\" : { \"componentName\" : \"componentName\", \"componentVersion\" : 5 }, \"type\" : \"type\", \"timeout\" : \"timeout\", \"node\" : \"node\", \"post\" : [ null, null ], \"name\" : \"name\", \"finalize\" : [ null, null ], \"parameters\" : { \"key\" : \"\" }, \"connections\" : [ { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true }, { \"workflowNodeName\" : \"workflowNodeName\", \"componentName\" : \"componentName\", \"componentVersion\" : 5, \"key\" : \"key\", \"required\" : true } ] } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * DELETE /integrations/{id} : Delete an integration
     * Delete an integration.
     *
     * @param id The id of an integration. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteIntegration",
        summary = "Delete an integration",
        description = "Delete an integration.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/integrations/{id}"
    )
    
    default ResponseEntity<Void> deleteIntegration(
        @Parameter(name = "id", description = "The id of an integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /integrations/{id} : Get an integration by id
     * Get an integration by id.
     *
     * @param id The id of an integration. (required)
     * @return The integration object. (status code 200)
     */
    @Operation(
        operationId = "getIntegration",
        summary = "Get an integration by id",
        description = "Get an integration by id.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The integration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<IntegrationModel> getIntegration(
        @Parameter(name = "id", description = "The id of an integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"integrationWorkflowIds\" : [ \"integrationWorkflowIds\", \"integrationWorkflowIds\" ], \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"componentVersion\" : 0, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"allowMultipleInstances\" : false, \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"componentName\" : \"componentName\", \"id\" : 6, \"category\" : { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, \"lastIntegrationVersion\" : 1 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /integrations/{id}/versions : Get a integration versions.
     * Get a integration versions.
     *
     * @param id The id of an integration. (required)
     * @return The list of integration version objects. (status code 200)
     */
    @Operation(
        operationId = "getIntegrationVersions",
        summary = "Get a integration versions.",
        description = "Get a integration versions.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of integration version objects.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = IntegrationVersionModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations/{id}/versions",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<IntegrationVersionModel>> getIntegrationVersions(
        @Parameter(name = "id", description = "The id of an integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"description\" : \"description\", \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"version\" : 0 }, { \"description\" : \"description\", \"publishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"version\" : 0 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /integrations : Get integrations
     * Get integrations.
     *
     * @param categoryId The category id used for filtering integrations. (optional)
     * @param integrationInstanceConfigurations Use for filtering integrations for which integration instance configurations exist. (optional)
     * @param tagId The tag id of used for filtering integrations. (optional)
     * @param status Use for filtering integrations by status. (optional)
     * @return The list of integrations. (status code 200)
     */
    @Operation(
        operationId = "getIntegrations",
        summary = "Get integrations",
        description = "Get integrations.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of integrations.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = IntegrationModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<IntegrationModel>> getIntegrations(
        @Parameter(name = "categoryId", description = "The category id used for filtering integrations.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "categoryId", required = false) Long categoryId,
        @Parameter(name = "integrationInstanceConfigurations", description = "Use for filtering integrations for which integration instance configurations exist.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "integrationInstanceConfigurations", required = false) Boolean integrationInstanceConfigurations,
        @Parameter(name = "tagId", description = "The tag id of used for filtering integrations.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "tagId", required = false) Long tagId,
        @Parameter(name = "status", description = "Use for filtering integrations by status.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "status", required = false) IntegrationStatusModel status
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"integrationWorkflowIds\" : [ \"integrationWorkflowIds\", \"integrationWorkflowIds\" ], \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"componentVersion\" : 0, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"allowMultipleInstances\" : false, \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"componentName\" : \"componentName\", \"id\" : 6, \"category\" : { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, \"lastIntegrationVersion\" : 1 }, { \"integrationWorkflowIds\" : [ \"integrationWorkflowIds\", \"integrationWorkflowIds\" ], \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"componentVersion\" : 0, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"allowMultipleInstances\" : false, \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"componentName\" : \"componentName\", \"id\" : 6, \"category\" : { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, \"lastIntegrationVersion\" : 1 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /integrations/{id}/publish : Publishes existing integration.
     * Publishes existing integration.
     *
     * @param id The id of a integration. (required)
     * @param publishIntegrationRequestModel  (optional)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "publishIntegration",
        summary = "Publishes existing integration.",
        description = "Publishes existing integration.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/integrations/{id}/publish",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> publishIntegration(
        @Parameter(name = "id", description = "The id of a integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "PublishIntegrationRequestModel", description = "") @Valid @RequestBody(required = false) PublishIntegrationRequestModel publishIntegrationRequestModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /integrations/{id} : Update an existing integration
     * Update an existing integration.
     *
     * @param id The id of an integration. (required)
     * @param integrationModel  (required)
     * @return The updated integration object. (status code 200)
     */
    @Operation(
        operationId = "updateIntegration",
        summary = "Update an existing integration",
        description = "Update an existing integration.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated integration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/integrations/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<IntegrationModel> updateIntegration(
        @Parameter(name = "id", description = "The id of an integration.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "IntegrationModel", description = "", required = true) @Valid @RequestBody IntegrationModel integrationModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"integrationWorkflowIds\" : [ \"integrationWorkflowIds\", \"integrationWorkflowIds\" ], \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"description\" : \"description\", \"componentVersion\" : 0, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"allowMultipleInstances\" : false, \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"componentName\" : \"componentName\", \"id\" : 6, \"category\" : { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, \"lastIntegrationVersion\" : 1 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
