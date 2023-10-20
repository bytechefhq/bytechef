
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.athena.configuration.web.rest;

import com.bytechef.athena.configuration.web.rest.model.TagModel;
import com.bytechef.athena.configuration.web.rest.model.UpdateTagsRequestModel;
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

import java.util.List;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-09T13:39:53.714562+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "integration-tag", description = "The Embedded Tag API")
public interface IntegrationTagApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /integrations/tags : Get integration tags Get integration tags.
     *
     * @return A list of integration tags. (status code 200)
     */
    @Operation(
        operationId = "getIntegrationTags",
        summary = "Get integration tags",
        description = "Get integration tags.",
        tags = {
            "integration-tag"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of integration tags.", content = {
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TagModel.class)))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/integrations/tags",
        produces = {
            "application/json"
        })
    default ResponseEntity<List<TagModel>> getIntegrationTags(

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
     * PUT /integrations/{id}/tags : Updates tags of an existing integration Updates tags of an existing integration.
     *
     * @param id                     The id of an integration. (required)
     * @param updateTagsRequestModel (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "updateIntegrationTags",
        summary = "Updates tags of an existing integration",
        description = "Updates tags of an existing integration.",
        tags = {
            "integration-tag"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        })
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/integrations/{id}/tags",
        consumes = {
            "application/json"
        })
    default ResponseEntity<Void> updateIntegrationTags(
        @Parameter(
            name = "id", description = "The id of an integration.", required = true,
            in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(
            name = "UpdateTagsRequestModel", description = "",
            required = true) @Valid @RequestBody UpdateTagsRequestModel updateTagsRequestModel) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
