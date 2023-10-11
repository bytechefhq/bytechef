
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

package com.bytechef.helios.connection.web.rest;

import com.bytechef.helios.connection.web.rest.model.TagModel;
import com.bytechef.helios.connection.web.rest.model.UpdateTagsRequestModel;
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
    date = "2023-10-06T20:36:46.568258+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "connection-tag", description = "The Automation Connection Tag API")
public interface ConnectionTagApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /connections/tags : Get connection tags Get connection tags.
     *
     * @return A list of connection tags. (status code 200)
     */
    @Operation(
        operationId = "getConnectionTags",
        summary = "Get connection tags",
        description = "Get connection tags.",
        tags = {
            "connection-tag"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of connection tags.", content = {
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TagModel.class)))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connections/tags",
        produces = {
            "application/json"
        })
    default ResponseEntity<List<TagModel>> getConnectionTags(

    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "[ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 1 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * PUT /connections/{id}/tags : Updates tags of an existing connection Updates tags of an existing connection.
     *
     * @param id                     The id of the connection. (required)
     * @param updateTagsRequestModel (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "updateConnectionTags",
        summary = "Updates tags of an existing connection",
        description = "Updates tags of an existing connection.",
        tags = {
            "connection-tag"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        })
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/connections/{id}/tags",
        consumes = {
            "application/json"
        })
    default ResponseEntity<Void> updateConnectionTags(
        @Parameter(
            name = "id", description = "The id of the connection.", required = true,
            in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(
            name = "UpdateTagsRequestModel", description = "",
            required = true) @Valid @RequestBody UpdateTagsRequestModel updateTagsRequestModel) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
