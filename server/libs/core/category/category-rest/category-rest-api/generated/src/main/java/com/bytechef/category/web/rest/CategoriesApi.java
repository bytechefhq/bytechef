/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.4.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.category.web.rest;

import com.bytechef.category.web.rest.model.CategoryModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-22T18:48:19.848032+01:00[Europe/Zagreb]")
@Validated
@Tag(name = "categories", description = "the categories API")
public interface CategoriesApi {

    /**
     * POST /categories : Create a new category.
     * Create a new category.
     *
     * @param categoryModel  (required)
     * @return The category object. (status code 200)
     */
    @Operation(
        operationId = "createCategory",
        summary = "Create a new category.",
        description = "Create a new category.",
        tags = { "categories" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The category object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/categories",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<CategoryModel>> createCategory(
        @Parameter(name = "CategoryModel", description = "", required = true) @Valid @RequestBody Mono<CategoryModel> categoryModel,
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
        return result.then(categoryModel).then(Mono.empty());

    }


    /**
     * DELETE /categories/{id} : Delete a category.
     * Delete a category.
     *
     * @param id The id of a category. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "deleteCategory",
        summary = "Delete a category.",
        description = "Delete a category.",
        tags = { "categories" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/categories/{id}"
    )
    default Mono<ResponseEntity<Void>> deleteCategory(
        @Parameter(name = "id", description = "The id of a category.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return result.then(Mono.empty());

    }


    /**
     * GET /categories : Get all categories.
     * Get all categories.
     *
     * @return A list of categories. (status code 200)
     */
    @Operation(
        operationId = "getCategories",
        summary = "Get all categories.",
        description = "Get all categories.",
        tags = { "categories" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of categories.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CategoryModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/categories",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<Flux<CategoryModel>>> getCategories(
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
     * GET /categories/{id} : Get a category by id.
     * Get a category by id.
     *
     * @param id The id of a category. (required)
     * @return The category object. (status code 200)
     */
    @Operation(
        operationId = "getCategory",
        summary = "Get a category by id.",
        description = "Get a category by id.",
        tags = { "categories" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The category object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/categories/{id}",
        produces = { "application/json" }
    )
    default Mono<ResponseEntity<CategoryModel>> getCategory(
        @Parameter(name = "id", description = "The id of a category.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
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
     * PUT /categories/{id} : Update an existing category.
     * Update an existing category.
     *
     * @param id The id of a category. (required)
     * @param categoryModel  (required)
     * @return The updated category object. (status code 200)
     */
    @Operation(
        operationId = "updateCategory",
        summary = "Update an existing category.",
        description = "Update an existing category.",
        tags = { "categories" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated category object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/categories/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default Mono<ResponseEntity<CategoryModel>> updateCategory(
        @Parameter(name = "id", description = "The id of a category.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "CategoryModel", description = "", required = true) @Valid @RequestBody Mono<CategoryModel> categoryModel,
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
        return result.then(categoryModel).then(Mono.empty());

    }

}
