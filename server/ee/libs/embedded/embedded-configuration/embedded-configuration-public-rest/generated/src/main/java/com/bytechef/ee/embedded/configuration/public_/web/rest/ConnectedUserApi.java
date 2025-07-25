/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.13.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import java.util.Map;
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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T11:11:22.962422+02:00[Europe/Zagreb]", comments = "Generator version: 7.13.0")
@Validated
@Tag(name = "connected-user", description = "the connected-user API")
public interface ConnectedUserApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * PATCH /{externalUserId} : Update data of an external user
     * Update data of an external user.
     *
     * @param externalUserId The external user id. (required)
     * @param xEnvironment The environment. (optional)
     * @param requestBody  (optional)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "updateConnectedUser",
        summary = "Update data of an external user",
        description = "Update data of an external user.",
        tags = { "connected-user" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        },
        security = {
            @SecurityRequirement(name = "bearerAuth")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/{externalUserId}",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateConnectedUser(
        @Parameter(name = "externalUserId", description = "The external user id.", required = true, in = ParameterIn.PATH) @PathVariable("externalUserId") String externalUserId,
        @Parameter(name = "X-Environment", description = "The environment.", in = ParameterIn.HEADER) @RequestHeader(value = "X-Environment", required = false) EnvironmentModel xEnvironment,
        @Parameter(name = "request_body", description = "") @Valid @RequestBody(required = false) Map<String, Object> requestBody
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /me : Update data of an external user
     * Update data of an external user.
     *
     * @param xEnvironment The environment. (optional)
     * @param requestBody  (optional)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "updateFrontendConnectedUser",
        summary = "Update data of an external user",
        description = "Update data of an external user.",
        tags = { "connected-user" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        },
        security = {
            @SecurityRequirement(name = "jwtBearerAuth")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/me",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateFrontendConnectedUser(
        @Parameter(name = "X-Environment", description = "The environment.", in = ParameterIn.HEADER) @RequestHeader(value = "X-Environment", required = false) EnvironmentModel xEnvironment,
        @Parameter(name = "request_body", description = "") @Valid @RequestBody(required = false) Map<String, Object> requestBody
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
