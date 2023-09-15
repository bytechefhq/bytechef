/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.0.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.helios.execution.web.rest;

import com.bytechef.helios.execution.web.rest.model.JobModel;
import com.bytechef.helios.execution.web.rest.model.TestParametersModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-09-15T07:35:18.469889+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "automation-test", description = "The Automation Test API")
public interface WorkflowTestsApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /workflow-tests : Execute a workflow synchronously for testing purpose
     * Execute a workflow synchronously for testing purposes.
     *
     * @param testParametersModel Parameters required to run a test job, for example &#39;{\&quot;workflowId\&quot;:\&quot;samples/hello\&quot;,\&quot;inputs\&quot;:{\&quot;yourName\&quot;:\&quot;Joe Jones\&quot;}}&#39; (required)
     * @return The output expected by the workflow. (status code 200)
     */
    @Operation(
        operationId = "testWorkflow",
        summary = "Execute a workflow synchronously for testing purpose",
        description = "Execute a workflow synchronously for testing purposes.",
        tags = { "automation-test" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The output expected by the workflow.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = JobModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/workflow-tests",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    default ResponseEntity<JobModel> testWorkflow(
        @Parameter(name = "TestParametersModel", description = "Parameters required to run a test job, for example '{\"workflowId\":\"samples/hello\",\"inputs\":{\"yourName\":\"Joe Jones\"}}'", required = true) @Valid @RequestBody TestParametersModel testParametersModel
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "null";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
