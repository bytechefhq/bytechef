/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.11.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.automation.connection.web.rest;

import com.bytechef.automation.connection.web.rest.model.ConnectionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-02-06T15:10:20.853909+01:00[Europe/Zagreb]", comments = "Generator version: 7.11.0")
@Validated
@Tag(name = "connection", description = "The Automation Connection Internal API")
public interface ConnectionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /connections : Create a new connection
     * Create a new connection.
     *
     * @param connectionModel  (required)
     * @return The connection id. (status code 200)
     */
    @Operation(
        operationId = "createConnection",
        summary = "Create a new connection",
        description = "Create a new connection.",
        tags = { "connection" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The connection id.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/connections",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Long> createConnection(
        @Parameter(name = "ConnectionModel", description = "", required = true) @Valid @RequestBody ConnectionModel connectionModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * DELETE /connections/{id} : Delete a connection
     * Delete a connection.
     *
     * @param id The id of a connection. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "deleteConnection",
        summary = "Delete a connection",
        description = "Delete a connection.",
        tags = { "connection" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/connections/{id}"
    )
    
    default ResponseEntity<Void> deleteConnection(
        @Parameter(name = "id", description = "The id of a connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /connections/{id} : Get a connection by id
     * Get a connection by id.
     *
     * @param id The id of a connection. (required)
     * @return The connection object. (status code 200)
     */
    @Operation(
        operationId = "getConnection",
        summary = "Get a connection by id",
        description = "Get a connection by id.",
        tags = { "connection" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The connection object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ConnectionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connections/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ConnectionModel> getConnection(
        @Parameter(name = "id", description = "The id of a connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"credentialStatus\" : \"VALID\", \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"authorizationParameters\" : { \"key\" : \"\" }, \"__version\" : 1, \"environment\" : \"DEVELOPMENT\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"\" }, \"connectionVersion\" : 0, \"workspaceId\" : 5 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workspaces/{id}/connections : Get all workspace connections
     * Get all workspace connections.
     *
     * @param id The id of a workspace. (required)
     * @param componentName The component name used for filtering connections. (optional)
     * @param connectionVersion The connection version. (optional)
     * @param environment The environment. (optional)
     * @param tagId The tag id of used for filtering connections. (optional)
     * @return The list of connections. (status code 200)
     */
    @Operation(
        operationId = "getWorkspaceConnections",
        summary = "Get all workspace connections",
        description = "Get all workspace connections.",
        tags = { "connection" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of connections.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ConnectionModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workspaces/{id}/connections",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ConnectionModel>> getWorkspaceConnections(
        @Parameter(name = "id", description = "The id of a workspace.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "componentName", description = "The component name used for filtering connections.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "componentName", required = false) String componentName,
        @Parameter(name = "connectionVersion", description = "The connection version.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "connectionVersion", required = false) Integer connectionVersion,
        @Parameter(name = "environment", description = "The environment.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "environment", required = false) com.bytechef.platform.connection.web.rest.model.ConnectionEnvironmentModel environment,
        @Parameter(name = "tagId", description = "The tag id of used for filtering connections.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "tagId", required = false) Long tagId
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"credentialStatus\" : \"VALID\", \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"authorizationParameters\" : { \"key\" : \"\" }, \"__version\" : 1, \"environment\" : \"DEVELOPMENT\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"\" }, \"connectionVersion\" : 0, \"workspaceId\" : 5 }, { \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"credentialStatus\" : \"VALID\", \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"authorizationParameters\" : { \"key\" : \"\" }, \"__version\" : 1, \"environment\" : \"DEVELOPMENT\", \"connectionParameters\" : { \"key\" : \"\" }, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"\" }, \"connectionVersion\" : 0, \"workspaceId\" : 5 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /connections/{id} : Update an existing connection
     * Update an existing connection.
     *
     * @param id The id of a connection. (required)
     * @param comBytechefPlatformConnectionWebRestModelUpdateConnectionRequestModel  (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "updateConnection",
        summary = "Update an existing connection",
        description = "Update an existing connection.",
        tags = { "connection" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/connections/{id}",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateConnection(
        @Parameter(name = "id", description = "The id of a connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "com.bytechef.platform.connection.web.rest.model.UpdateConnectionRequestModel", description = "", required = true) @Valid @RequestBody com.bytechef.platform.connection.web.rest.model.UpdateConnectionRequestModel comBytechefPlatformConnectionWebRestModelUpdateConnectionRequestModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
