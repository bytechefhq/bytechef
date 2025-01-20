/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.10.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.configuration.public_.web.rest;

import com.bytechef.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.configuration.public_.web.rest.model.IntegrationModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-20T07:11:57.734213+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
@Validated
@Tag(name = "integration", description = "The Embedded Integration Public API")
public interface IntegrationApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /integrations : Get active integrations
     * Get active integrations.
     *
     * @param externalUserId The external user id. (required)
     * @param xEnvironment The environment. (optional)
     * @return The list of active integrations. (status code 200)
     *         or Access token is missing or invalid (status code 401)
     */
    @Operation(
        operationId = "getIntegrations",
        summary = "Get active integrations",
        description = "Get active integrations.",
        tags = { "integration" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of active integrations.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = IntegrationModel.class)))
            }),
            @ApiResponse(responseCode = "401", description = "Access token is missing or invalid")
        },
        security = {
            @SecurityRequirement(name = "frontendBearerAuth")
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<IntegrationModel>> getIntegrations(
        @NotNull @Parameter(name = "externalUserId", description = "The external user id.", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "externalUserId", required = true) String externalUserId,
        @Parameter(name = "X-Environment", description = "The environment.", in = ParameterIn.HEADER) @RequestHeader(value = "X-Environment", required = false) EnvironmentModel xEnvironment
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"allowMultipleInstances\" : false, \"icon\" : \"icon\", \"integrationVersion\" : 6, \"description\" : \"description\", \"id\" : 0, \"componentName\" : \"componentName\", \"workflows\" : [ { \"description\" : \"description\", \"label\" : \"label\", \"workflowReferenceCode\" : \"workflowReferenceCode\" }, { \"description\" : \"description\", \"label\" : \"label\", \"workflowReferenceCode\" : \"workflowReferenceCode\" } ], \"title\" : \"title\" }, { \"allowMultipleInstances\" : false, \"icon\" : \"icon\", \"integrationVersion\" : 6, \"description\" : \"description\", \"id\" : 0, \"componentName\" : \"componentName\", \"workflows\" : [ { \"description\" : \"description\", \"label\" : \"label\", \"workflowReferenceCode\" : \"workflowReferenceCode\" }, { \"description\" : \"description\", \"label\" : \"label\", \"workflowReferenceCode\" : \"workflowReferenceCode\" } ], \"title\" : \"title\" } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
