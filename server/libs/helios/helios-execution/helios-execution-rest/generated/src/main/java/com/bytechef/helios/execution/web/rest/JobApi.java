
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

import com.bytechef.helios.execution.web.rest.model.CreateJob200ResponseModel;
import com.bytechef.helios.execution.web.rest.model.JobModel;
import com.bytechef.helios.execution.web.rest.model.JobParametersModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-06T20:36:47.577089+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "job", description = "The Automation Job API")
public interface JobApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /jobs : Create a request for running a new job Create a request for running a new job.
     *
     * @param jobParametersModel Parameters required to run a job, for example
     *                           &#39;{\&quot;workflowId\&quot;:\&quot;samples/hello\&quot;,\&quot;inputs\&quot;:{\&quot;yourName\&quot;:\&quot;Joe
     *                           Jones\&quot;}}&#39; (required)
     * @return The id of a created job. (status code 200)
     */
    @Operation(
        operationId = "createJob",
        summary = "Create a request for running a new job",
        description = "Create a request for running a new job.",
        tags = {
            "job"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "The id of a created job.", content = {
                @Content(
                    mediaType = "application/json", schema = @Schema(implementation = CreateJob200ResponseModel.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/jobs",
        produces = {
            "application/json"
        },
        consumes = {
            "application/json"
        })
    default ResponseEntity<CreateJob200ResponseModel> createJob(
        @Parameter(
            name = "JobParametersModel",
            description = "Parameters required to run a job, for example '{\"workflowId\":\"samples/hello\",\"inputs\":{\"yourName\":\"Joe Jones\"}}'",
            required = true) @Valid @RequestBody JobParametersModel jobParametersModel) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"jobId\" : 0 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * GET /jobs/{id} : Get a job by id Get a job by id.
     *
     * @param id The id of a job to return. (required)
     * @return The job object. (status code 200)
     */
    @Operation(
        operationId = "getJob",
        summary = "Get a job by id",
        description = "Get a job by id.",
        tags = {
            "job"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "The job object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = JobModel.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/jobs/{id}",
        produces = {
            "application/json"
        })
    default ResponseEntity<JobModel> getJob(
        @Parameter(
            name = "id", description = "The id of a job to return.", required = true,
            in = ParameterIn.PATH) @PathVariable("id") Long id) {
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

    /**
     * GET /jobs : Get a page of jobs Get a page of jobs.
     *
     * @param pageNumber The number of the page to return. (optional, default to 0)
     * @return The page of jobs. (status code 200)
     */
    @Operation(
        operationId = "getJobs",
        summary = "Get a page of jobs",
        description = "Get a page of jobs.",
        tags = {
            "job"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "The page of jobs.", content = {
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = org.springframework.data.domain.Page.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/jobs",
        produces = {
            "application/json"
        })
    default ResponseEntity<org.springframework.data.domain.Page> getJobs(
        @Parameter(
            name = "pageNumber", description = "The number of the page to return.",
            in = ParameterIn.QUERY) @Valid @RequestParam(
                value = "pageNumber", required = false, defaultValue = "0") Integer pageNumber) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "{ \"number\" : 0, \"size\" : 6, \"numberOfElements\" : 1, \"totalPages\" : 5, \"content\" : [ \"{}\", \"{}\" ], \"totalElements\" : 5 }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * GET /jobs/latest : Get the latest job Get the latest job.
     *
     * @return The latest job. (status code 200)
     */
    @Operation(
        operationId = "getLatestJob",
        summary = "Get the latest job",
        description = "Get the latest job.",
        tags = {
            "job"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "The latest job.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = JobModel.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/jobs/latest",
        produces = {
            "application/json"
        })
    default ResponseEntity<JobModel> getLatestJob(

    ) {
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

    /**
     * PUT /jobs/{id}/restart : Restart a job Restart a job.
     *
     * @param id The id of a job to restart. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "restartJob",
        summary = "Restart a job",
        description = "Restart a job.",
        tags = {
            "job"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        })
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/jobs/{id}/restart")
    default ResponseEntity<Void> restartJob(
        @Parameter(
            name = "id", description = "The id of a job to restart.", required = true,
            in = ParameterIn.PATH) @PathVariable("id") Long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * PUT /jobs/{id}/stop : Stop a job Stop a job.
     *
     * @param id The id of a job to stop. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "stopJob",
        summary = "Stop a job",
        description = "Stop a job.",
        tags = {
            "job"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.")
        })
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/jobs/{id}/stop")
    default ResponseEntity<Void> stopJob(
        @Parameter(
            name = "id", description = "The id of a job to stop.", required = true,
            in = ParameterIn.PATH) @PathVariable("id") Long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
