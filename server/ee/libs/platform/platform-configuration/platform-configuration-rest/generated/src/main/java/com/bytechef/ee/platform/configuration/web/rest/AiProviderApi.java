/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.ee.platform.configuration.web.rest;

import com.bytechef.ee.platform.configuration.web.rest.model.AiProviderModel;
import com.bytechef.ee.platform.configuration.web.rest.model.UpdateAiProviderRequestModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-10T21:49:29.280770+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "ai-provider", description = "The Platform AI Provider Internal API")
public interface AiProviderApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * DELETE /ai-providers/{id} : Delete an AI provider
     * Delete an AI provider.
     *
     * @param id The id of an AI provider. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "deleteAiProvider",
        summary = "Delete an AI provider",
        description = "Delete an AI provider.",
        tags = { "ai-provider" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/ai-providers/{id}"
    )
    
    default ResponseEntity<Void> deleteAiProvider(
        @Parameter(name = "id", description = "The id of an AI provider.", required = true, in = ParameterIn.PATH) @PathVariable("id") Integer id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /ai-providers/{id}/enable/{enable} : e
     * Enable/disable an AI provider.
     *
     * @param id The id of an AI provider. (required)
     * @param enable The enable status of an AI provider. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "enableAiProvider",
        summary = "e",
        description = "Enable/disable an AI provider.",
        tags = { "ai-provider" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/ai-providers/{id}/enable/{enable}"
    )
    
    default ResponseEntity<Void> enableAiProvider(
        @Parameter(name = "id", description = "The id of an AI provider.", required = true, in = ParameterIn.PATH) @PathVariable("id") Integer id,
        @Parameter(name = "enable", description = "The enable status of an AI provider.", required = true, in = ParameterIn.PATH) @PathVariable("enable") Boolean enable
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /ai-providers : Get AI providers
     * Get AI providers.
     *
     * @return The list of AI providers. (status code 200)
     */
    @Operation(
        operationId = "getAiProviders",
        summary = "Get AI providers",
        description = "Get AI providers.",
        tags = { "ai-provider" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of AI providers.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AiProviderModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/ai-providers",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<AiProviderModel>> getAiProviders(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"apiKey\" : \"apiKey\", \"name\" : \"name\", \"icon\" : \"icon\", \"id\" : 0, \"enabled\" : true }, { \"apiKey\" : \"apiKey\", \"name\" : \"name\", \"icon\" : \"icon\", \"id\" : 0, \"enabled\" : true } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /ai-providers/{id} : Update an existing AI provider
     * Update an existing AI provider.
     *
     * @param id The id of an AI provider. (required)
     * @param updateAiProviderRequestModel  (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "updateAiProvider",
        summary = "Update an existing AI provider",
        description = "Update an existing AI provider.",
        tags = { "ai-provider" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/ai-providers/{id}",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateAiProvider(
        @Parameter(name = "id", description = "The id of an AI provider.", required = true, in = ParameterIn.PATH) @PathVariable("id") Integer id,
        @Parameter(name = "UpdateAiProviderRequestModel", description = "", required = true) @Valid @RequestBody UpdateAiProviderRequestModel updateAiProviderRequestModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
