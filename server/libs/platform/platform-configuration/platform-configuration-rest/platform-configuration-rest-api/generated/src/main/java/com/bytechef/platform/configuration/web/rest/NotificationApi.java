/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.configuration.web.rest.model.NotificationModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-04-13T22:23:09.531481+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "notification", description = "The Platform Notification Internal API")
public interface NotificationApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /notifications : Get a list of notifications
     * Get a list of notifications
     *
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getNotifications",
        summary = "Get a list of notifications",
        description = "Get a list of notifications",
        tags = { "notification" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = NotificationModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/notifications",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<NotificationModel>> getNotifications(
        
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"settings\" : { \"key\" : \"settings\" }, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0, \"notificationType\" : \"EMAIL\", \"events\" : [ { \"id\" : 6, \"type\" : \"JOB_CANCELLED\" }, { \"id\" : 6, \"type\" : \"JOB_CANCELLED\" } ] }, { \"settings\" : { \"key\" : \"settings\" }, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0, \"notificationType\" : \"EMAIL\", \"events\" : [ { \"id\" : 6, \"type\" : \"JOB_CANCELLED\" }, { \"id\" : 6, \"type\" : \"JOB_CANCELLED\" } ] } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /notifications : Create a notification entry
     * Create a notification entry
     *
     * @param notificationModel  (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "postNotification",
        summary = "Create a notification entry",
        description = "Create a notification entry",
        tags = { "notification" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/notifications",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    
    default ResponseEntity<NotificationModel> postNotification(
        @Parameter(name = "NotificationModel", description = "", required = true) @Valid @RequestBody NotificationModel notificationModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"settings\" : { \"key\" : \"settings\" }, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"name\" : \"name\", \"id\" : 0, \"notificationType\" : \"EMAIL\", \"events\" : [ { \"id\" : 6, \"type\" : \"JOB_CANCELLED\" }, { \"id\" : 6, \"type\" : \"JOB_CANCELLED\" } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
