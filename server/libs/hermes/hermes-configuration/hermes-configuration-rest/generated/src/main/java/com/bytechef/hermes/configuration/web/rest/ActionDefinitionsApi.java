/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.0.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.hermes.configuration.web.rest;

import com.bytechef.hermes.configuration.web.rest.model.ActionDefinitionModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-09-15T08:31:26.598462+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "action-definition", description = "The Core Action Definition API")
public interface ActionDefinitionsApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /action-definitions : Get all action definitions
     * Get all action definitions.
     *
     * @param taskTypes The list of task types defined in workflows.(E.g. mailchimp/v1/addMemberToList) (optional)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getActionDefinitions",
        summary = "Get all action definitions",
        description = "Get all action definitions.",
        tags = { "action-definition" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ActionDefinitionModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/action-definitions",
        produces = { "application/json" }
    )
    default ResponseEntity<List<ActionDefinitionModel>> getActionDefinitions(
        @Parameter(name = "taskTypes", description = "The list of task types defined in workflows.(E.g. mailchimp/v1/addMemberToList)", in = ParameterIn.QUERY) @Valid @RequestParam(value = "taskTypes", required = false) List<String> taskTypes
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputSchema\" : { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, \"name\" : \"name\", \"description\" : \"description\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"title\" : \"title\", \"sampleOutput\" : \"{}\", \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] }, { \"help\" : { \"body\" : \"body\", \"learnMoreUrl\" : \"learnMoreUrl\" }, \"outputSchema\" : { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, \"name\" : \"name\", \"description\" : \"description\", \"componentName\" : \"componentName\", \"componentVersion\" : 0, \"title\" : \"title\", \"sampleOutput\" : \"{}\", \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
