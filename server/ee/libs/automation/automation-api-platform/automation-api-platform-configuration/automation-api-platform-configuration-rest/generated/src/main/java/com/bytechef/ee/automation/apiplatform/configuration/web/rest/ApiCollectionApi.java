/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.11.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-02-23T10:28:21.445606+01:00[Europe/Zagreb]", comments = "Generator version: 7.11.0")
@Validated
@Tag(name = "api-collection", description = "The Automation API Platform Collection Internal API")
public interface ApiCollectionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /api-collections : Create a new API collection
     * Create a new API collection.
     *
     * @param apiCollectionModel  (required)
     * @return The API collection object. (status code 200)
     */
    @Operation(
        operationId = "createApiCollection",
        summary = "Create a new API collection",
        description = "Create a new API collection.",
        tags = { "api-collection" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The API collection object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiCollectionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api-collections",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ApiCollectionModel> createApiCollection(
        @Parameter(name = "ApiCollectionModel", description = "", required = true) @Valid @RequestBody ApiCollectionModel apiCollectionModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"endpoints\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"contextPath\" : \"contextPath\", \"description\" : \"description\", \"project\" : { \"lastVersion\" : 3, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 9, \"lastStatus\" : \"DRAFT\" }, \"collectionVersion\" : 0, \"enabled\" : false, \"projectDeployment\" : { \"environment\" : \"TEST\", \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 4, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 7, \"enabled\" : true, \"projectVersion\" : 1 }, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 2, \"projectDeploymentId\" : 2, \"projectId\" : 7, \"projectVersion\" : 1, \"workspaceId\" : 7 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * DELETE /api-collections/{id} : Delete an API collection
     * Delete an API collection.
     *
     * @param id The id of an API collection. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "deleteApiCollection",
        summary = "Delete an API collection",
        description = "Delete an API collection.",
        tags = { "api-collection" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api-collections/{id}"
    )
    
    default ResponseEntity<Void> deleteApiCollection(
        @Parameter(name = "id", description = "The id of an API collection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /api-collections/{id} : Get an API collection by id
     * Get an API collection by id.
     *
     * @param id The id of an API collection. (required)
     * @return The API collection object. (status code 200)
     */
    @Operation(
        operationId = "getApiCollection",
        summary = "Get an API collection by id",
        description = "Get an API collection by id.",
        tags = { "api-collection" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The API collection object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiCollectionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api-collections/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ApiCollectionModel> getApiCollection(
        @Parameter(name = "id", description = "The id of an API collection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"endpoints\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"contextPath\" : \"contextPath\", \"description\" : \"description\", \"project\" : { \"lastVersion\" : 3, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 9, \"lastStatus\" : \"DRAFT\" }, \"collectionVersion\" : 0, \"enabled\" : false, \"projectDeployment\" : { \"environment\" : \"TEST\", \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 4, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 7, \"enabled\" : true, \"projectVersion\" : 1 }, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 2, \"projectDeploymentId\" : 2, \"projectId\" : 7, \"projectVersion\" : 1, \"workspaceId\" : 7 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workspaces/{id}/api-collections : Get all workspace API collections
     * Get all workspace API collections.
     *
     * @param id The id of a workspace. (required)
     * @param environment The environment. (optional)
     * @param projectId The project ids used for filtering project deployments. (optional)
     * @param tagId The tag id of used for filtering project deployments. (optional)
     * @return A list of API collections. (status code 200)
     */
    @Operation(
        operationId = "getWorkspaceApiCollections",
        summary = "Get all workspace API collections",
        description = "Get all workspace API collections.",
        tags = { "api-collection" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of API collections.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ApiCollectionModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workspaces/{id}/api-collections",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ApiCollectionModel>> getWorkspaceApiCollections(
        @Parameter(name = "id", description = "The id of a workspace.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "environment", description = "The environment.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "environment", required = false) com.bytechef.automation.configuration.web.rest.model.EnvironmentModel environment,
        @Parameter(name = "projectId", description = "The project ids used for filtering project deployments.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "projectId", required = false) Long projectId,
        @Parameter(name = "tagId", description = "The tag id of used for filtering project deployments.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "tagId", required = false) Long tagId
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"endpoints\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"contextPath\" : \"contextPath\", \"description\" : \"description\", \"project\" : { \"lastVersion\" : 3, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 9, \"lastStatus\" : \"DRAFT\" }, \"collectionVersion\" : 0, \"enabled\" : false, \"projectDeployment\" : { \"environment\" : \"TEST\", \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 4, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 7, \"enabled\" : true, \"projectVersion\" : 1 }, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 2, \"projectDeploymentId\" : 2, \"projectId\" : 7, \"projectVersion\" : 1, \"workspaceId\" : 7 }, { \"endpoints\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"contextPath\" : \"contextPath\", \"description\" : \"description\", \"project\" : { \"lastVersion\" : 3, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 9, \"lastStatus\" : \"DRAFT\" }, \"collectionVersion\" : 0, \"enabled\" : false, \"projectDeployment\" : { \"environment\" : \"TEST\", \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 4, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 7, \"enabled\" : true, \"projectVersion\" : 1 }, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 2, \"projectDeploymentId\" : 2, \"projectId\" : 7, \"projectVersion\" : 1, \"workspaceId\" : 7 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /api-collections/{id} : Update an existing API collection
     * Update an existing API collection.
     *
     * @param id The id of an API collection. (required)
     * @param apiCollectionModel  (required)
     * @return The updated API collection object. (status code 200)
     */
    @Operation(
        operationId = "updateApiCollection",
        summary = "Update an existing API collection",
        description = "Update an existing API collection.",
        tags = { "api-collection" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated API collection object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiCollectionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api-collections/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ApiCollectionModel> updateApiCollection(
        @Parameter(name = "id", description = "The id of an API collection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "ApiCollectionModel", description = "", required = true) @Valid @RequestBody ApiCollectionModel apiCollectionModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"endpoints\" : [ { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"apiCollectionId\" : 6, \"httpMethod\" : \"DELETE\", \"enabled\" : false, \"workflowReferenceCode\" : \"workflowReferenceCode\", \"path\" : \"path\", \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"projectDeploymentWorkflowId\" : 5, \"id\" : 1, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"contextPath\" : \"contextPath\", \"description\" : \"description\", \"project\" : { \"lastVersion\" : 3, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastPublishedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 9, \"lastStatus\" : \"DRAFT\" }, \"collectionVersion\" : 0, \"enabled\" : false, \"projectDeployment\" : { \"environment\" : \"TEST\", \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 4, \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\", \"projectId\" : 7, \"enabled\" : true, \"projectVersion\" : 1 }, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"id\" : 2, \"projectDeploymentId\" : 2, \"projectId\" : 7, \"projectVersion\" : 1, \"workspaceId\" : 7 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
