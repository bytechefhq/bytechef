/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.12.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.bytechef.ee.automation.configuration.web.rest;

import com.bytechef.ee.automation.configuration.web.rest.model.ProjectGitConfigurationModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-14T15:53:15.074797+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
@Validated
@Tag(name = "project-git", description = "The Automation Project Git Internal API")
public interface ProjectGitApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /projects/{id}/project-git-configuration : Get git configuration of a project .
     * Get git configuration of a project.
     *
     * @param id The id of a project. (required)
     * @return The project git configuration object. (status code 200)
     */
    @Operation(
        operationId = "getProjectGitConfiguration",
        summary = "Get git configuration of a project .",
        description = "Get git configuration of a project.",
        tags = { "project-git" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The project git configuration object.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectGitConfigurationModel.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/projects/{id}/project-git-configuration",
        produces = { "application/json" }
    )
    
    default ResponseEntity<ProjectGitConfigurationModel> getProjectGitConfiguration(
        @Parameter(name = "id", description = "The id of a project.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "{ \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"projectId\" : 0, \"branch\" : \"branch\", \"enabled\" : true }";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /workspaces/{id}/project-git-configurations : Get project git configurations of a workspace.
     * Get project git configurations of a workspace.
     *
     * @param id The id of a workspace. (required)
     * @return The list of workspace project git configuration objects. (status code 200)
     */
    @Operation(
        operationId = "getWorkspaceProjectGitConfigurations",
        summary = "Get project git configurations of a workspace.",
        description = "Get project git configurations of a workspace.",
        tags = { "project-git" },
        responses = {
            @ApiResponse(responseCode = "200", description = "The list of workspace project git configuration objects.", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProjectGitConfigurationModel.class)))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/workspaces/{id}/project-git-configurations",
        produces = { "application/json" }
    )
    
    default ResponseEntity<List<ProjectGitConfigurationModel>> getWorkspaceProjectGitConfigurations(
        @Parameter(name = "id", description = "The id of a workspace.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    String exampleString = "[ { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"projectId\" : 0, \"branch\" : \"branch\", \"enabled\" : true }, { \"__version\" : 6, \"createdDate\" : \"2000-01-23T04:56:07.000+00:00\", \"createdBy\" : \"createdBy\", \"lastModifiedDate\" : \"2000-01-23T04:56:07.000+00:00\", \"lastModifiedBy\" : \"lastModifiedBy\", \"projectId\" : 0, \"branch\" : \"branch\", \"enabled\" : true } ]";
                    ApiUtil.setExampleResponse(request, "application/json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /projects/{id}/git/pull : Pulls project from git repository.
     * Pulls project from git repository.
     *
     * @param id The id of a project. (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "pullProjectFromGit",
        summary = "Pulls project from git repository.",
        description = "Pulls project from git repository.",
        tags = { "project-git" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/projects/{id}/git/pull"
    )
    
    default ResponseEntity<Void> pullProjectFromGit(
        @Parameter(name = "id", description = "The id of a project.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /projects/{id}/project-git-configuration : Update git configuration of an existing project.
     * Update git configuration of an existing project.
     *
     * @param id The id of a project. (required)
     * @param projectGitConfigurationModel  (required)
     * @return Successful operation. (status code 204)
     */
    @Operation(
        operationId = "updateProjectGitConfiguration",
        summary = "Update git configuration of an existing project.",
        description = "Update git configuration of an existing project.",
        tags = { "project-git" },
        responses = {
            @ApiResponse(responseCode = "204", description = "Successful operation.")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/projects/{id}/project-git-configuration",
        consumes = { "application/json" }
    )
    
    default ResponseEntity<Void> updateProjectGitConfiguration(
        @Parameter(name = "id", description = "The id of a project.", required = true, in = ParameterIn.PATH) @PathVariable("id") Long id,
        @Parameter(name = "ProjectGitConfigurationModel", description = "", required = true) @Valid @RequestBody ProjectGitConfigurationModel projectGitConfigurationModel
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
