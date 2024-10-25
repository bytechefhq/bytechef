/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.9.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.embedded.execution.public_.web.rest;

import com.bytechef.embedded.execution.public_.web.rest.model.EnvironmentModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-10-24T21:51:08.847975+02:00[Europe/Zagreb]", comments = "Generator version: 7.9.0")
@Validated
@Tag(name = "request-trigger", description = "The Embedded Request Trigger Public API")
public interface RequestTriggerApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /workflows/{id} : Execute workflows
     * Execute workflows.
     *
     * @param id The workflow execution id. (required)
     * @param externalUserId The external id of a connected user. (required)
     * @param xEnvironment The environment. (optional)
     * @return The list of active integrations. (status code 200)
     */
    @Operation(
        operationId = "executeWorkflows",
        summary = "Execute workflows",
        description = "Execute workflows.",
        tags = { "request-trigger" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of active integrations.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/workflows/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<Object> executeWorkflows(
        @Parameter(name = "id", description = "The workflow execution id.", required = true, in = ParameterIn.PATH) @PathVariable("id") String id,
        @NotNull @Parameter(name = "externalUserId", description = "The external id of a connected user.", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "externalUserId", required = true) String externalUserId,
        @Parameter(name = "x-environment", description = "The environment.", in = ParameterIn.HEADER) @RequestHeader(value = "x-environment", required = false) EnvironmentModel xEnvironment
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
