/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.4.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-05T16:27:34.189599+01:00[Europe/Zagreb]")
@Validated
@Tag(name = "connection-definitions", description = "the connection-definitions API")
public interface ConnectionDefinitionsApi {

    /**
     * GET /connection-definitions/{componentName} : Get all connection definitions for a component.
     * Get all connection definitions for a component.
     *
     * @param componentName The name of the component. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentConnectionDefinitions",
        summary = "Get all connection definitions for a component.",
        description = "Get all connection definitions for a component.",
        tags = { "connection-definitions" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ConnectionDefinitionBasicModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connection-definitions/{componentName}",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<ConnectionDefinitionBasicModel>>> getComponentConnectionDefinitions(
        @Parameter(name = "componentName", description = "The name of the component.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "[ { \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"componentName\" : \"componentName\", \"version\" : 0 }, { \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"componentName\" : \"componentName\", \"version\" : 0 } ]";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /connection-definitions/{componentName}/{componentVersion} : Get a connection definition of a component definition.
     * Get a connection definition of a component definition.
     *
     * @param componentName The name of the component. (required)
     * @param componentVersion The version of the component to get. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getConnectionDefinition",
        summary = "Get a connection definition of a component definition.",
        description = "Get a connection definition of a component definition.",
        tags = { "connection-definitions" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ConnectionDefinitionModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connection-definitions/{componentName}/{componentVersion}",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<ConnectionDefinitionModel>> getConnectionDefinition(
        @Parameter(name = "componentName", description = "The name of the component.", required = true, in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(name = "componentVersion", description = "The version of the component to get.", required = true, in = ParameterIn.PATH) @PathVariable("componentVersion") Integer componentVersion,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"authorizationRequired\" : true, \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"authorizations\" : [ { \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"name\" : \"name\", \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] }, { \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"name\" : \"name\", \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] } ], \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"componentName\" : \"componentName\", \"version\" : 0, \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /connection-definitions : Get all connection definitions.
     * Get all connection definitions.
     *
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getConnectionDefinitions",
        summary = "Get all connection definitions.",
        description = "Get all connection definitions.",
        tags = { "connection-definitions" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ConnectionDefinitionBasicModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connection-definitions",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<ConnectionDefinitionBasicModel>>> getConnectionDefinitions(
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "[ { \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"componentName\" : \"componentName\", \"version\" : 0 }, { \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"componentName\" : \"componentName\", \"version\" : 0 } ]";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }

}
