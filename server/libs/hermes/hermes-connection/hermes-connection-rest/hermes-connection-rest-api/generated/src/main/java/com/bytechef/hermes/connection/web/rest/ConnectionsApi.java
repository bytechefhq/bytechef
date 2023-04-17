/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.connection.web.rest;

import com.bytechef.hermes.connection.web.rest.model.ConnectionModel;
import com.bytechef.hermes.connection.web.rest.model.OAuth2AuthorizationParametersModel;
import com.bytechef.hermes.connection.web.rest.model.UpdateTagsRequestModel;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.codec.multipart.Part;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-17T21:20:25.683401+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "connections", description = "the connections API")
public interface ConnectionsApi {

    /**
     * POST /connections : Create a new connection.
     * Create a new connection.
     *
     * @param connectionModel  (required)
     * @return The connection object. (status code 200)
     */
    @Operation(
        operationId = "createConnection",
        summary = "Create a new connection.",
        description = "Create a new connection.",
        tags = { "connections" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The connection object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ConnectionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/connections",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<ConnectionModel>> createConnection(
        @Parameter(name = "ConnectionModel", description = "", required = true) @Valid @RequestBody Mono<ConnectionModel> connectionModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"{}\" }, \"connectionVersion\" : 0 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(connectionModel).then(Mono.empty());

    }


    /**
     * DELETE /connections/{id} : Delete a connection.
     * Delete a connection.
     *
     * @param id The id of a connection. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteConnection",
        summary = "Delete a connection.",
        description = "Delete a connection.",
        tags = { "connections" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/connections/{id}"
    )
    default Mono<ResponseEntity<Void>> deleteConnection(
        @Parameter(name = "id", description = "The id of a connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(Mono.empty());

    }


    /**
     * GET /connections/{id} : Get a connection by id.
     * Get a connection by id.
     *
     * @param id The id of a connection. (required)
     * @return The connection object. (status code 200)
     */
    @Operation(
        operationId = "getConnection",
        summary = "Get a connection by id.",
        description = "Get a connection by id.",
        tags = { "connections" },
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
    default Mono<ResponseEntity<ConnectionModel>> getConnection(
        @Parameter(name = "id", description = "The id of a connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"{}\" }, \"connectionVersion\" : 0 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * POST /connections/oauth2 : Populates a new connection with oauth parameters.
     * Populates a new connection with oauth parameters.
     *
     * @param connectionModel  (required)
     * @return The connection object with oauth parameters. (status code 200)
     */
    @Operation(
        operationId = "getConnectionOAuth2AuthorizationParameters",
        summary = "Populates a new connection with oauth parameters.",
        description = "Populates a new connection with oauth parameters.",
        tags = { "connections" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The connection object with oauth parameters.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = OAuth2AuthorizationParametersModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/connections/oauth2",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<OAuth2AuthorizationParametersModel>> getConnectionOAuth2AuthorizationParameters(
        @Parameter(name = "ConnectionModel", description = "", required = true) @Valid @RequestBody Mono<ConnectionModel> connectionModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"clientId\" : \"clientId\", \"authorizationUrl\" : \"authorizationUrl\", \"scopes\" : [ \"scopes\", \"scopes\" ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(connectionModel).then(Mono.empty());

    }


    /**
     * GET /connections : Get connections.
     * Get connections.
     *
     * @param componentNames The list of component names used for filtering connections. (optional)
     * @param tagIds The list of tag ids of used for filtering connections. (optional)
     * @return A list of connections. (status code 200)
     */
    @Operation(
        operationId = "getConnections",
        summary = "Get connections.",
        description = "Get connections.",
        tags = { "connections" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of connections.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ConnectionModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connections",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<ConnectionModel>>> getConnections(
        @Parameter(name = "componentNames", description = "The list of component names used for filtering connections.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "componentNames", required = false) List<String> componentNames,
        @Parameter(name = "tagIds", description = "The list of tag ids of used for filtering connections.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "[ { \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"{}\" }, \"connectionVersion\" : 0 }, { \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"{}\" }, \"connectionVersion\" : 0 } ]";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * PUT /connections/{id} : Update an existing connection.
     * Update an existing connection.
     *
     * @param id The id of a connection. (required)
     * @param connectionModel  (required)
     * @return The updated connection object. (status code 200)
     */
    @Operation(
        operationId = "updateConnection",
        summary = "Update an existing connection.",
        description = "Update an existing connection.",
        tags = { "connections" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated connection object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ConnectionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/connections/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<ConnectionModel>> updateConnection(
        @Parameter(name = "id", description = "The id of a connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "ConnectionModel", description = "", required = true) @Valid @RequestBody Mono<ConnectionModel> connectionModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"authorizationName\" : \"authorizationName\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"active\" : true, \"tags\" : [ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ], \"__version\" : 1, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"name\" : \"name\", \"componentName\" : \"componentName\", \"id\" : 6, \"parameters\" : { \"key\" : \"{}\" }, \"connectionVersion\" : 0 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(connectionModel).then(Mono.empty());

    }


    /**
     * PUT /connections/{id}/tags : Updates tags of an existing connection.
     * Updates tags of an existing connection.
     *
     * @param id The id of the connection. (required)
     * @param updateTagsRequestModel  (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "updateConnectionTags",
        summary = "Updates tags of an existing connection.",
        description = "Updates tags of an existing connection.",
        tags = { "connections" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/connections/{id}/tags",
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<Void>> updateConnectionTags(
        @Parameter(name = "id", description = "The id of the connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "UpdateTagsRequestModel", description = "", required = true) @Valid @RequestBody Mono<UpdateTagsRequestModel> updateTagsRequestModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(updateTagsRequestModel).then(Mono.empty());

    }

}
