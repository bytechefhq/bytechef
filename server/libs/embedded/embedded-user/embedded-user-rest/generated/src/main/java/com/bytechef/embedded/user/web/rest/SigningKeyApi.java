/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.5.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.user.web.rest;

import com.bytechef.embedded.user.web.rest.model.CreateSigningKey200ResponseModel;
import com.bytechef.embedded.user.web.rest.model.SigningKeyModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-12T12:40:27.518394+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
@Validated
@Tag(name = "signing-key", description = "The Embedded Signing Key API")
public interface SigningKeyApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /signing-keys : Create a new Signing key
     * Create a new Signing key.
     *
     * @param signingKeyModel  (required)
     * @return The Signing key object. (status code 200)
     */
    @Operation(
        operationId = "createSigningKey",
        summary = "Create a new Signing key",
        description = "Create a new Signing key.",
        tags = { "signing-key" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The Signing key object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CreateSigningKey200ResponseModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/signing-keys",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<CreateSigningKey200ResponseModel> createSigningKey(
        @Parameter(name = "SigningKeyModel", description = "", required = true) @Valid @RequestBody SigningKeyModel signingKeyModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"privateKey\" : \"privateKey\" }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * DELETE /signing-keys/{id} : Delete an Signing key
     * Delete an Signing key.
     *
     * @param id The id of an Signing key. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteSigningKey",
        summary = "Delete an Signing key",
        description = "Delete an Signing key.",
        tags = { "signing-key" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/signing-keys/{id}"
    )
    
    default ResponseEntity<Void> deleteSigningKey(
        @Parameter(name = "id", description = "The id of an Signing key.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /signing-keys/{id} : Get an Signing key by id
     * Get an Signing key by id.
     *
     * @param id The id of an Signing key. (required)
     * @return The Signing key object. (status code 200)
     */
    @Operation(
        operationId = "getSigningKey",
        summary = "Get an Signing key by id",
        description = "Get an Signing key by id.",
        tags = { "signing-key" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The Signing key object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = SigningKeyModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/signing-keys/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<SigningKeyModel> getSigningKey(
        @Parameter(name = "id", description = "The id of an Signing key.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastUsedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /signing-keys : Get Signing keys
     * Get Signing keys.
     *
     * @return A list of Signing keys. (status code 200)
     */
    @Operation(
        operationId = "getSigningKeys",
        summary = "Get Signing keys",
        description = "Get Signing keys.",
        tags = { "signing-key" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of Signing keys.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SigningKeyModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/signing-keys",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<SigningKeyModel>> getSigningKeys(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastUsedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastUsedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /signing-keys/{id} : Update an existing Signing key
     * Update an existing Signing key.
     *
     * @param id The id of an Signing key. (required)
     * @param signingKeyModel  (required)
     * @return The updated Signing key object. (status code 200)
     */
    @Operation(
        operationId = "updateSigningKey",
        summary = "Update an existing Signing key",
        description = "Update an existing Signing key.",
        tags = { "signing-key" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated Signing key object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = SigningKeyModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/signing-keys/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<SigningKeyModel> updateSigningKey(
        @Parameter(name = "id", description = "The id of an Signing key.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "SigningKeyModel", description = "", required = true) @Valid @RequestBody SigningKeyModel signingKeyModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastUsedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
