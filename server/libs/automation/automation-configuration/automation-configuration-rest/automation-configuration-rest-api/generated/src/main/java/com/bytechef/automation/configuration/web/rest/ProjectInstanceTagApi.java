/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.10.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.automation.configuration.web.rest;

import com.bytechef.platform.tag.web.rest.model.TagModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-07T09:20:57.435090+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
@Validated
@Tag(name = "project-instance-tag", description = "The Automation Project Instance Tag Internal API")
public interface ProjectInstanceTagApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /project-instances/tags : Get project instance tags
     * Get project instance tags.
     *
     * @return The list of project instance tags. (status code 200)
     */
    @Operation(
        operationId = "getProjectInstanceTags",
        summary = "Get project instance tags",
        description = "Get project instance tags.",
        tags = { "project-instance-tag" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of project instance tags.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TagModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-instances/tags",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<TagModel>> getProjectInstanceTags(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /project-instances/{id}/tags : Updates tags of an existing project instance
     * Updates tags of an existing project instance.
     *
     * @param id The id of a project instance. (required)
     * @param comBytechefPlatformTagWebRestModelUpdateTagsRequestModel  (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "updateProjectInstanceTags",
        summary = "Updates tags of an existing project instance",
        description = "Updates tags of an existing project instance.",
        tags = { "project-instance-tag" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/project-instances/{id}/tags",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateProjectInstanceTags(
        @Parameter(name = "id", description = "The id of a project instance.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "com.bytechef.platform.tag.web.rest.model.UpdateTagsRequestModel", description = "", required = true) @Valid @RequestBody com.bytechef.platform.tag.web.rest.model.UpdateTagsRequestModel comBytechefPlatformTagWebRestModelUpdateTagsRequestModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
