/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.2.1).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.integration.web.rest;

import com.bytechef.hermes.integration.web.rest.model.IntegrationModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-12-02T12:00:57.220855+01:00[Europe/Zagreb]")
@Validated
@Tag(name = "integrations", description = "the integrations API")
public interface IntegrationsApi {

    /**
     * DELETE /integrations/{id} : Delete an integration.
     * Delete an integration.
     *
     * @param id The id of the integration. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteIntegration",
        summary = "Delete an integration.",
        tags = { "integrations" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/integrations/{id}"
    )
    default Mono<ResponseEntity<Void>> deleteIntegration(
        @Parameter(name = "id", description = "The id of the integration.", required = true) @PathVariable("id") String id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(Mono.empty());

    }


    /**
     * GET /integrations/{id} : Get an integration by id.
     * Get an integration by id.
     *
     * @param id The id of the integration. (required)
     * @return The integration object. (status code 200)
     */
    @Operation(
        operationId = "getIntegration",
        summary = "Get an integration by id.",
        tags = { "integrations" },
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
    default Mono<ResponseEntity<IntegrationModel>> getIntegration(
        @Parameter(name = "id", description = "The id of the integration.", required = true) @PathVariable("id") String id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"workflowIds\" : [ \"workflowIds\", \"workflowIds\" ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : \"id\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /integrations : Get integrations.
     * Get integrations.
     *
     * @return The list of integrations. (status code 200)
     */
    @Operation(
        operationId = "getIntegrations",
        summary = "Get integrations.",
        tags = { "integrations" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of integrations.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = IntegrationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<IntegrationModel>>> getIntegrations(
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"workflowIds\" : [ \"workflowIds\", \"workflowIds\" ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : \"id\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * POST /integrations : Create a new integration.
     * Create a new integration.
     *
     * @param integrationModel  (required)
     * @return The integration object. (status code 200)
     */
    @Operation(
        operationId = "postIntegration",
        summary = "Create a new integration.",
        tags = { "integrations" },
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
    default Mono<ResponseEntity<IntegrationModel>> postIntegration(
        @Parameter(name = "IntegrationModel", description = "", required = true) @Valid @RequestBody Mono<IntegrationModel> integrationModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"workflowIds\" : [ \"workflowIds\", \"workflowIds\" ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : \"id\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(integrationModel).then(Mono.empty());

    }


    /**
     * PUT /integrations/{id} : Update an existing integration.
     * Update an existing integration.
     *
     * @param id The id of the integration. (required)
     * @param integrationModel  (required)
     * @return The updated integration object. (status code 200)
     */
    @Operation(
        operationId = "putIntegration",
        summary = "Update an existing integration.",
        tags = { "integrations" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated integration object.", content = {
                @Content(mediaType = "*/*", schema = @Schema(implementation = IntegrationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/integrations/{id}",
        produces = { "*/*" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<IntegrationModel>> putIntegration(
        @Parameter(name = "id", description = "The id of the integration.", required = true) @PathVariable("id") String id,
        @Parameter(name = "IntegrationModel", description = "", required = true) @Valid @RequestBody Mono<IntegrationModel> integrationModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("*/*"))) {
                String exampleString = "{ \"workflowIds\" : [ \"workflowIds\", \"workflowIds\" ], \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : \"id\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(integrationModel).then(Mono.empty());

    }

}
