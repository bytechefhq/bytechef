/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.ee.platform.api_connector.configuration.web.rest;

import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.ApiConnectorModel;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.ImportOpenApiSpecificationRequestModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-10T21:49:32.797292+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "api-connector", description = "The Platform API Connector Internal API")
public interface ApiConnectorApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /api-connectors : Create a new API Connector
     * Create a new API Connector.
     *
     * @param apiConnectorModel  (required)
     * @return The API Connector object. (status code 200)
     */
    @Operation(
        operationId = "createApiConnector",
        summary = "Create a new API Connector",
        description = "Create a new API Connector.",
        tags = { "api-connector" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The API Connector object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiConnectorModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api-connectors",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ApiConnectorModel> createApiConnector(
        @Parameter(name = "ApiConnectorModel", description = "", required = true) @Valid @RequestBody ApiConnectorModel apiConnectorModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"endpoints\" : [ { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"specification\" : \"specification\", \"title\" : \"title\", \"enabled\" : true, \"tags\" : [ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 } ], \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"connectorVersion\" : \"connectorVersion\", \"name\" : \"name\", \"definition\" : \"definition\", \"id\" : 1 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * DELETE /api-connectors/{id} : Delete an API Connector
     * Delete an API Connector.
     *
     * @param id The id of the API Connector. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "deleteApiConnector",
        summary = "Delete an API Connector",
        description = "Delete an API Connector.",
        tags = { "api-connector" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api-connectors/{id}"
    )
    
    default ResponseEntity<Void> deleteApiConnector(
        @Parameter(name = "id", description = "The id of the API Connector.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PATCH /api-connectors/{id}/enable/{enable} : Enable/disable a API Connector.
     * Enable/disable a API Connector.
     *
     * @param id The id of the API Connector. (required)
     * @param enable Enable/disable the API Connector. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "enableApiConnector",
        summary = "Enable/disable a API Connector.",
        description = "Enable/disable a API Connector.",
        tags = { "api-connector" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api-connectors/{id}/enable/{enable}"
    )
    
    default ResponseEntity<Void> enableApiConnector(
        @Parameter(name = "id", description = "The id of the API Connector.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "enable", description = "Enable/disable the API Connector.", required = true, in = ParameterIn.PATH) @PathVariable("enable") Boolean enable
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /api-connectors/{id} : Get an API Connector by id
     * Get an API Connector by id.
     *
     * @param id The id of the API Connector. (required)
     * @return The API Connector object. (status code 200)
     */
    @Operation(
        operationId = "getApiConnector",
        summary = "Get an API Connector by id",
        description = "Get an API Connector by id.",
        tags = { "api-connector" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The API Connector object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiConnectorModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api-connectors/{id}",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ApiConnectorModel> getApiConnector(
        @Parameter(name = "id", description = "The id of the API Connector.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"endpoints\" : [ { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"specification\" : \"specification\", \"title\" : \"title\", \"enabled\" : true, \"tags\" : [ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 } ], \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"connectorVersion\" : \"connectorVersion\", \"name\" : \"name\", \"definition\" : \"definition\", \"id\" : 1 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /api-connectors : Get API Connectors
     * Get API Connectors.
     *
     * @return A list of API Connectors. (status code 200)
     */
    @Operation(
        operationId = "getApiConnectors",
        summary = "Get API Connectors",
        description = "Get API Connectors.",
        tags = { "api-connector" },
        responses = {
            @ApiResponse(responseCode = "200", description = "A list of API Connectors.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ApiConnectorModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api-connectors",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ApiConnectorModel>> getApiConnectors(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"endpoints\" : [ { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"specification\" : \"specification\", \"title\" : \"title\", \"enabled\" : true, \"tags\" : [ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 } ], \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"connectorVersion\" : \"connectorVersion\", \"name\" : \"name\", \"definition\" : \"definition\", \"id\" : 1 }, { \"endpoints\" : [ { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"specification\" : \"specification\", \"title\" : \"title\", \"enabled\" : true, \"tags\" : [ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 } ], \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"connectorVersion\" : \"connectorVersion\", \"name\" : \"name\", \"definition\" : \"definition\", \"id\" : 1 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /api-connectors/import : Create API Connector by importing OpenAPI specification
     * Create API Connector by importing OpenAPI specification.
     *
     * @param importOpenApiSpecificationRequestModel  (required)
     * @return The API Connector object. (status code 200)
     */
    @Operation(
        operationId = "importOpenApiSpecification",
        summary = "Create API Connector by importing OpenAPI specification",
        description = "Create API Connector by importing OpenAPI specification.",
        tags = { "api-connector" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The API Connector object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiConnectorModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api-connectors/import",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ApiConnectorModel> importOpenApiSpecification(
        @Parameter(name = "ImportOpenApiSpecificationRequestModel", description = "", required = true) @Valid @RequestBody ImportOpenApiSpecificationRequestModel importOpenApiSpecificationRequestModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"endpoints\" : [ { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"specification\" : \"specification\", \"title\" : \"title\", \"enabled\" : true, \"tags\" : [ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 } ], \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"connectorVersion\" : \"connectorVersion\", \"name\" : \"name\", \"definition\" : \"definition\", \"id\" : 1 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /api-connectors/{id} : Update an existing API Connector
     * Update an existing API Connector.
     *
     * @param id The id of the API Connector. (required)
     * @param apiConnectorModel  (required)
     * @return The updated API Connector object. (status code 200)
     */
    @Operation(
        operationId = "updateApiConnector",
        summary = "Update an existing API Connector",
        description = "Update an existing API Connector.",
        tags = { "api-connector" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The updated API Connector object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ApiConnectorModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api-connectors/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<ApiConnectorModel> updateApiConnector(
        @Parameter(name = "id", description = "The id of the API Connector.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "ApiConnectorModel", description = "", required = true) @Valid @RequestBody ApiConnectorModel apiConnectorModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"endpoints\" : [ { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" }, { \"path\" : \"path\", \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"description\" : \"description\", \"id\" : 0, \"httpMethod\" : \"DELETE\", \"lastExecutionDate\" : \"2000-01-23T04:56:07.000+00:00\" } ], \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"icon\" : \"icon\", \"description\" : \"description\", \"specification\" : \"specification\", \"title\" : \"title\", \"enabled\" : true, \"tags\" : [ { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 }, { \"__version\" : 5, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 5 } ], \"__version\" : 2, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"connectorVersion\" : \"connectorVersion\", \"name\" : \"name\", \"definition\" : \"definition\", \"id\" : 1 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
