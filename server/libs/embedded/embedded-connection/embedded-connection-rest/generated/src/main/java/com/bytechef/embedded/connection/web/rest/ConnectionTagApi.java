/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.connection.web.rest;

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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-26T11:09:38.305724+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
@Validated
@Tag(name = "connection-tag", description = "The Embedded Connection Tag API")
public interface ConnectionTagApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /connections/tags : Get connection tags
     * Get connection tags.
     *
     * @return A list of connection tags. (status code 200)
     */
    @Operation(
        operationId = "getConnectionTags",
        summary = "Get connection tags",
        description = "Get connection tags.",
        tags = { "connection-tag" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of connection tags.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.bytechef.platform.connection.web.rest.model.TagModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connections/tags",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<com.bytechef.platform.connection.web.rest.model.TagModel>> getConnectionTags(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /connections/{id}/tags : Updates tags of an existing connection
     * Updates tags of an existing connection.
     *
     * @param id The id of the connection. (required)
     * @param comBytechefPlatformConnectionWebRestModelUpdateTagsRequestModel  (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "updateConnectionTags",
        summary = "Updates tags of an existing connection",
        description = "Updates tags of an existing connection.",
        tags = { "connection-tag" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/connections/{id}/tags",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateConnectionTags(
        @Parameter(name = "id", description = "The id of the connection.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "com.bytechef.platform.connection.web.rest.model.UpdateTagsRequestModel", description = "", required = true) @Valid @RequestBody com.bytechef.platform.connection.web.rest.model.UpdateTagsRequestModel comBytechefPlatformConnectionWebRestModelUpdateTagsRequestModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
