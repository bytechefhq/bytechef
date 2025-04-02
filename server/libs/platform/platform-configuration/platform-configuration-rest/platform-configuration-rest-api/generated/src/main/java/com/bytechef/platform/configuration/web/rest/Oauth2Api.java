/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.configuration.web.rest.model.GetOAuth2AuthorizationParametersRequestModel;
import com.bytechef.platform.configuration.web.rest.model.OAuth2AuthorizationParametersModel;
import com.bytechef.platform.configuration.web.rest.model.OAuth2PropertiesModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-04-13T22:23:09.531481+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "oauth2", description = "the oauth2 API")
public interface Oauth2Api {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /oauth2/authorization-parameters : Retrieves oauth2 authorization parameters
     * Retrieves oauth2 authorization parameters.
     *
     * @param getOAuth2AuthorizationParametersRequestModel  (required)
     * @return The object with oauth2 authorization parameters. (status code 200)
     */
    @Operation(
        operationId = "getOAuth2AuthorizationParameters",
        summary = "Retrieves oauth2 authorization parameters",
        description = "Retrieves oauth2 authorization parameters.",
        tags = { "oauth2" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The object with oauth2 authorization parameters.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = OAuth2AuthorizationParametersModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/oauth2/authorization-parameters",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<OAuth2AuthorizationParametersModel> getOAuth2AuthorizationParameters(
        @Parameter(name = "GetOAuth2AuthorizationParametersRequestModel", description = "", required = true) @Valid @RequestBody GetOAuth2AuthorizationParametersRequestModel getOAuth2AuthorizationParametersRequestModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"clientId\" : \"clientId\", \"authorizationUrl\" : \"authorizationUrl\", \"extraQueryParameters\" : { \"key\" : \"extraQueryParameters\" }, \"scopes\" : [ \"scopes\", \"scopes\" ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /oauth2/properties : Get OAuth2 properties
     * Get OAuth2 properties.
     *
     * @return The OAuth2Properties object. (status code 200)
     */
    @Operation(
        operationId = "getOAuth2Properties",
        summary = "Get OAuth2 properties",
        description = "Get OAuth2 properties.",
        tags = { "oauth2" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The OAuth2Properties object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = OAuth2PropertiesModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/oauth2/properties",
        produces = { "application/json" }
    )
    
    default ResponseEntity<OAuth2PropertiesModel> getOAuth2Properties(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"redirectUri\" : \"redirectUri\", \"predefinedApps\" : [ \"predefinedApps\", \"predefinedApps\" ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
