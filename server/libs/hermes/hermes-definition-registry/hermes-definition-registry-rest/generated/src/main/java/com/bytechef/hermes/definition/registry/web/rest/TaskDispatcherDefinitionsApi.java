/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.4.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.hermes.definition.registry.web.rest.model.TaskDispatcherDefinitionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-01T08:54:46.758794+01:00[Europe/Zagreb]")
@Validated
@Tag(name = "task-dispatcher-definitions", description = "the task-dispatcher-definitions API")
public interface TaskDispatcherDefinitionsApi {

    /**
     * GET /task-dispatcher-definitions : Get all task dispatcher definitions.
     * Get all task dispatcher definitions.
     *
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getTaskDispatcherDefinitions",
        summary = "Get all task dispatcher definitions.",
        description = "Get all task dispatcher definitions.",
        tags = { "task-dispatcher-definitions" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TaskDispatcherDefinitionModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-dispatcher-definitions",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<TaskDispatcherDefinitionModel>>> getTaskDispatcherDefinitions(
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "[ { \"taskProperties\" : [ { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true }, { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true } ], \"outputSchema\" : [ { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true }, { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true } ], \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"name\" : \"name\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"version\" : 0, \"properties\" : [ { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true }, { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true } ] }, { \"taskProperties\" : [ { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true }, { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true } ], \"outputSchema\" : [ { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true }, { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true } ], \"display\" : { \"subtitle\" : \"subtitle\", \"icon\" : \"icon\", \"description\" : \"description\", \"label\" : \"label\", \"category\" : \"category\", \"tags\" : [ \"tags\", \"tags\" ] }, \"name\" : \"name\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"version\" : 0, \"properties\" : [ { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true }, { \"metadata\" : { \"key\" : \"{}\" }, \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"displayOption\" : { \"hide\" : { \"key\" : [ \"{}\", \"{}\" ] }, \"show\" : { \"key\" : [ \"{}\", \"{}\" ] } }, \"required\" : true } ] } ]";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }

}
