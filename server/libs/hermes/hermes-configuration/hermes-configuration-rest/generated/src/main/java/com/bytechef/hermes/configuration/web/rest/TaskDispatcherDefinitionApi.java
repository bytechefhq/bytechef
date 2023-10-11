
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

package com.bytechef.hermes.configuration.web.rest;

import com.bytechef.hermes.configuration.web.rest.model.TaskDispatcherDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.TaskDispatcherDefinitionModel;
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

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.Generated;

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-06T20:36:49.034607+02:00[Europe/Zagreb]")
@Validated
@Tag(name = "task-dispatcher-definition", description = "The Core Task Dispatcher Definition API")
public interface TaskDispatcherDefinitionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /task-dispatcher-definitions/{taskDispatcherName}/{taskDispatcherVersion} : Get a task dispatcher definition
     * Get a task dispatcher definition.
     *
     * @param taskDispatcherName    The name of a task dispatcher to get. (required)
     * @param taskDispatcherVersion The version of a task dispatcher to get. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getTaskDispatcherDefinition",
        summary = "Get a task dispatcher definition",
        description = "Get a task dispatcher definition.",
        tags = {
            "task-dispatcher-definition"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TaskDispatcherDefinitionModel.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-dispatcher-definitions/{taskDispatcherName}/{taskDispatcherVersion}",
        produces = {
            "application/json"
        })
    default ResponseEntity<TaskDispatcherDefinitionModel> getTaskDispatcherDefinition(
        @Parameter(
            name = "taskDispatcherName", description = "The name of a task dispatcher to get.", required = true,
            in = ParameterIn.PATH) @PathVariable("taskDispatcherName") String taskDispatcherName,
        @Parameter(
            name = "taskDispatcherVersion", description = "The version of a task dispatcher to get.", required = true,
            in = ParameterIn.PATH) @PathVariable("taskDispatcherVersion") Integer taskDispatcherVersion) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "{ \"taskProperties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ], \"outputSchema\" : { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, \"icon\" : \"icon\", \"name\" : \"name\", \"description\" : \"description\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"title\" : \"title\", \"version\" : 0, \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * GET /task-dispatcher-definitions/{taskDispatcherName}/versions : Get all task dispatcher definition versions of a
     * task dispatcher Get all task dispatcher definition versions of a task dispatcher.
     *
     * @param taskDispatcherName The name of a task dispatcher. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getTaskDispatcherDefinitionVersions",
        summary = "Get all task dispatcher definition versions of a task dispatcher",
        description = "Get all task dispatcher definition versions of a task dispatcher.",
        tags = {
            "task-dispatcher-definition"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TaskDispatcherDefinitionBasicModel.class)))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-dispatcher-definitions/{taskDispatcherName}/versions",
        produces = {
            "application/json"
        })
    default ResponseEntity<List<TaskDispatcherDefinitionBasicModel>> getTaskDispatcherDefinitionVersions(
        @Parameter(
            name = "taskDispatcherName", description = "The name of a task dispatcher.", required = true,
            in = ParameterIn.PATH) @PathVariable("taskDispatcherName") String taskDispatcherName) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "[ { \"icon\" : \"icon\", \"name\" : \"name\", \"description\" : \"description\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"title\" : \"title\" }, { \"icon\" : \"icon\", \"name\" : \"name\", \"description\" : \"description\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"title\" : \"title\" } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * GET /task-dispatcher-definitions : Get all task dispatcher definitions Get all task dispatcher definitions.
     *
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getTaskDispatcherDefinitions",
        summary = "Get all task dispatcher definitions",
        description = "Get all task dispatcher definitions.",
        tags = {
            "task-dispatcher-definition"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TaskDispatcherDefinitionModel.class)))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/task-dispatcher-definitions",
        produces = {
            "application/json"
        })
    default ResponseEntity<List<TaskDispatcherDefinitionModel>> getTaskDispatcherDefinitions(

    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "[ { \"taskProperties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ], \"outputSchema\" : { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, \"icon\" : \"icon\", \"name\" : \"name\", \"description\" : \"description\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"title\" : \"title\", \"version\" : 0, \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] }, { \"taskProperties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ], \"outputSchema\" : { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, \"icon\" : \"icon\", \"name\" : \"name\", \"description\" : \"description\", \"resources\" : { \"documentationUrl\" : \"documentationUrl\" }, \"title\" : \"title\", \"version\" : 0, \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
