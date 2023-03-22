/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.4.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.tag.web.rest;

import com.bytechef.tag.web.rest.model.TagModel;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.codec.multipart.Part;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-22T18:48:20.007025+01:00[Europe/Zagreb]")
@Validated
@Tag(name = "tags", description = "the tags API")
public interface TagsApi {

    /**
     * POST /tags : Create a new tag.
     * Create a new tag.
     *
     * @param tagModel  (required)
     * @return The tag object. (status code 200)
     */
    @Operation(
        operationId = "createTag",
        summary = "Create a new tag.",
        description = "Create a new tag.",
        tags = { "tags" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The tag object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = TagModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/tags",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<TagModel>> createTag(
        @Parameter(name = "TagModel", description = "", required = true) @Valid @RequestBody Mono<TagModel> tagModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(tagModel).then(Mono.empty());

    }


    /**
     * DELETE /tags/{id} : Delete a tag.
     * Delete a tag.
     *
     * @param id The id of a tag. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteTag",
        summary = "Delete a tag.",
        description = "Delete a tag.",
        tags = { "tags" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/tags/{id}"
    )
    default Mono<ResponseEntity<Void>> deleteTag(
        @Parameter(name = "id", description = "The id of a tag.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(Mono.empty());

    }


    /**
     * GET /tags/{id} : Get a tag by id.
     * Get a tag by id.
     *
     * @param id The id of a tag. (required)
     * @return The tag object. (status code 200)
     */
    @Operation(
        operationId = "getTag",
        summary = "Get a tag by id.",
        description = "Get a tag by id.",
        tags = { "tags" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The tag object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = TagModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/tags/{id}",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<TagModel>> getTag(
        @Parameter(name = "id", description = "The id of a tag.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * GET /tags : Get all tags.
     * Get all tags.
     *
     * @return A list of tags. (status code 200)
     */
    @Operation(
        operationId = "getTags",
        summary = "Get all tags.",
        description = "Get all tags.",
        tags = { "tags" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of tags.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TagModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/tags",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<TagModel>>> getTags(
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "[ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 } ]";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());

    }


    /**
     * PUT /tags/{id} : Update an existing tag.
     * Update an existing tag.
     *
     * @param id The id of a tag. (required)
     * @param tagModel  (required)
     * @return The updated tag object. (status code 200)
     */
    @Operation(
        operationId = "updateTag",
        summary = "Update an existing tag.",
        description = "Update an existing tag.",
        tags = { "tags" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated tag object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = TagModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/tags/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<TagModel>> updateTag(
        @Parameter(name = "id", description = "The id of a tag.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "TagModel", description = "", required = true) @Valid @RequestBody Mono<TagModel> tagModel,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(tagModel).then(Mono.empty());

    }

}
