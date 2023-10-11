
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

package com.bytechef.helios.execution.web.rest;

import com.bytechef.helios.execution.web.rest.model.JobModel;
import com.bytechef.helios.execution.web.rest.model.TestParametersModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-06T20:36:47.577089+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "workflow-test", description = "The Automation Workflow Test API")
public interface WorkflowTestApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /tests/workflows : Execute a workflow synchronously for testing purpose Execute a workflow synchronously for
     * testing purposes.
     *
     * @param testParametersModel Parameters required to run a test job, for example
     *                            &#39;{\&quot;workflowId\&quot;:\&quot;samples/hello\&quot;,\&quot;inputs\&quot;:{\&quot;yourName\&quot;:\&quot;Joe
     *                            Jones\&quot;}}&#39; (required)
     * @return The output expected by the workflow. (status code 200)
     */
    @Operation(
        operationId = "testWorkflow",
        summary = "Execute a workflow synchronously for testing purpose",
        description = "Execute a workflow synchronously for testing purposes.",
        tags = {
            "workflow-test"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "The output expected by the workflow.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = JobModel.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/tests/workflows",
        produces = {
            "application/json"
        },
        consumes = {
            "application/json"
        })
    default ResponseEntity<JobModel> testWorkflow(
        @Parameter(
            name = "TestParametersModel",
            description = "Parameters required to run a test job, for example '{\"workflowId\":\"samples/hello\",\"inputs\":{\"yourName\":\"Joe Jones\"}}'",
            required = true) @Valid @RequestBody TestParametersModel testParametersModel) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
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
