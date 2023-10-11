
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.helios.configuration.web.rest;

import com.bytechef.helios.configuration.web.rest.model.TagModel;
import com.bytechef.helios.configuration.web.rest.model.UpdateTagsRequestModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-09T13:39:54.113168+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "project-tag", description = "The Automation Project Tag API")
public interface ProjectTagApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /projects/tags : Get project tags. Get project tags.
     *
     * @return A list of project tags. (status code 200)
     */
    @Operation(
        operationId = "getProjectTags",
        summary = "Get project tags.",
        description = "Get project tags.",
        tags = {
            "project-tag"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of project tags.", content = {
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TagModel.class)))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/projects/tags",
        produces = {
            "application/json"
        })
    default ResponseEntity<List<TagModel>> getProjectTags(

    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "[ { \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 7 }, { \"__version\" : 9, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 7 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * PUT /projects/{id}/tags : Updates tags of an existing project. Updates tags of an existing project.
     *
     * @param id                     The id of a project. (required)
     * @param updateTagsRequestModel (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "updateProjectTags",
        summary = "Updates tags of an existing project.",
        description = "Updates tags of an existing project.",
        tags = {
            "project-tag"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        })
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/projects/{id}/tags",
        consumes = {
            "application/json"
        })
    default ResponseEntity<Void> updateProjectTags(
        @Parameter(
            name = "id", description = "The id of a project.", required = true,
            in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(
            name = "UpdateTagsRequestModel", description = "",
            required = true) @Valid @RequestBody UpdateTagsRequestModel updateTagsRequestModel) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
