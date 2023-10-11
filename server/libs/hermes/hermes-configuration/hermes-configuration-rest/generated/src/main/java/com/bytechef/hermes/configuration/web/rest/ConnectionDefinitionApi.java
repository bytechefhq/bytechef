
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

import com.bytechef.hermes.configuration.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.ConnectionDefinitionModel;
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
@Tag(name = "connection-definition", description = "The Core Connection Definition API")
public interface ConnectionDefinitionApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /component-definitions/{componentName}/{componentVersion}/connection-definition : Get connection definition
     * for a component Get connection definition for a component.
     *
     * @param componentName    The name of a component. (required)
     * @param componentVersion The version of a component. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentConnectionDefinition",
        summary = "Get connection definition for a component",
        description = "Get connection definition for a component.",
        tags = {
            "connection-definition"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ConnectionDefinitionModel.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/{componentVersion}/connection-definition",
        produces = {
            "application/json"
        })
    default ResponseEntity<ConnectionDefinitionModel> getComponentConnectionDefinition(
        @Parameter(
            name = "componentName", description = "The name of a component.", required = true,
            in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(
            name = "componentVersion", description = "The version of a component.", required = true,
            in = ParameterIn.PATH) @PathVariable("componentVersion") Integer componentVersion) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "{ \"authorizationRequired\" : true, \"componentTitle\" : \"componentTitle\", \"baseUri\" : \"baseUri\", \"authorizations\" : [ { \"name\" : \"name\", \"description\" : \"description\", \"title\" : \"title\", \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] }, { \"name\" : \"name\", \"description\" : \"description\", \"title\" : \"title\", \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] } ], \"componentDescription\" : \"componentDescription\", \"componentName\" : \"componentName\", \"version\" : 0, \"properties\" : [ { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true }, { \"displayCondition\" : \"displayCondition\", \"hidden\" : true, \"name\" : \"name\", \"description\" : \"description\", \"advancedOption\" : true, \"label\" : \"label\", \"placeholder\" : \"placeholder\", \"required\" : true, \"expressionEnabled\" : true } ] }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    /**
     * GET /component-definitions/{componentName}/{componentVersion}/connection-definitions : Get all compatible
     * connection definitions for a component Get all compatible connection definitions for a component.
     *
     * @param componentName    The name of a component. (required)
     * @param componentVersion The version of a component. (required)
     * @return Successful operation. (status code 200)
     */
    @Operation(
        operationId = "getComponentConnectionDefinitions",
        summary = "Get all compatible connection definitions for a component",
        description = "Get all compatible connection definitions for a component.",
        tags = {
            "connection-definition"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = {
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ConnectionDefinitionBasicModel.class)))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/{componentVersion}/connection-definitions",
        produces = {
            "application/json"
        })
    default ResponseEntity<List<ConnectionDefinitionBasicModel>> getComponentConnectionDefinitions(
        @Parameter(
            name = "componentName", description = "The name of a component.", required = true,
            in = ParameterIn.PATH) @PathVariable("componentName") String componentName,
        @Parameter(
            name = "componentVersion", description = "The version of a component.", required = true,
            in = ParameterIn.PATH) @PathVariable("componentVersion") Integer componentVersion) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString =
                        "[ { \"componentTitle\" : \"componentTitle\", \"componentDescription\" : \"componentDescription\", \"componentName\" : \"componentName\", \"version\" : 0 }, { \"componentTitle\" : \"componentTitle\", \"componentDescription\" : \"componentDescription\", \"componentName\" : \"componentName\", \"version\" : 0 } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
