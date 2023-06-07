/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.6.0).
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
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-06-07T12:23:17.227182+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "categories", description = "The Core Categories API")
public interface CategoriesApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /categories/{id} : Get a category by id
     * Get a category by id.
     *
     * @param id The id of a category. (required)
     * @return The category object. (status code 200)
     */
    @Operation(
        operationId = "getCategory",
        summary = "Get a category by id",
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
    default ResponseEntity<CategoryModel> getCategory(
        @Parameter(name = "id", description = "The id of a category.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /categories/{id} : Update an existing category
     * Update an existing category.
     *
     * @param id The id of a category. (required)
     * @param categoryModel  (required)
     * @return The updated category object. (status code 200)
     */
    @Operation(
        operationId = "updateCategory",
        summary = "Update an existing category",
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
    default ResponseEntity<CategoryModel> updateCategory(
        @Parameter(name = "id", description = "The id of a category.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "CategoryModel", description = "", required = true) @Valid @RequestBody CategoryModel categoryModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
