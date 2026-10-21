/*
 * Copyright 2025 ByteChef
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

package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automationapiplatform.ApiException;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionEndpointEnabledInputModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionEndpointInputModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionEndpointModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionEndpointUpdateModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionInputModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionProjectVersionInputModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionUpdateModel;
import com.bytechef.cli.client.automationapiplatform.model.ApiCollectionVersionUpdateModel;
import com.bytechef.cli.client.automationapiplatform.model.EndpointConnectionModel;
import com.bytechef.cli.client.automationapiplatform.model.HttpMethodModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.output.OutputRenderer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Commands for managing API collections and their endpoints via the public API.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationApiCollectionCommand {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "automation api-collection list", description = "List the API collections of a workspace.")
    public void apiCollectionList(
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "project-id") Long projectId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, workspaceId);

        List<ApiCollectionModel> apiCollections;

        try {
            apiCollections = AutomationClientFactory.apiCollectionApi(config)
                .getApiCollections(config.workspaceId(), projectId, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(apiCollections, output);
    }

    @Command(name = "automation api-collection get", description = "Get an API collection.")
    public void apiCollectionGet(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        ApiCollectionModel apiCollection;

        try {
            apiCollection = AutomationClientFactory.apiCollectionApi(config)
                .getApiCollection(id, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(apiCollection, output);
    }

    @Command(
        name = "automation api-collection create",
        description = "Create an API collection from a published project version.")
    public void apiCollectionCreate(
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "project-id", required = true) Long projectId,
        @Option(longName = "project-version", required = true) Integer projectVersion,
        @Option(longName = "name", required = true) String name,
        @Option(longName = "context-path", required = true) String contextPath,
        @Option(longName = "collection-version") Integer collectionVersion,
        @Option(longName = "description") String description,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, workspaceId);

        ApiCollectionInputModel request = new ApiCollectionInputModel()
            .projectId(projectId)
            .projectVersion(projectVersion)
            .name(name)
            .contextPath(contextPath)
            .collectionVersion(collectionVersion)
            .description(description);

        ApiCollectionModel apiCollection;

        try {
            apiCollection = AutomationClientFactory.apiCollectionApi(config)
                .createApiCollection(config.workspaceId(), request, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(apiCollection, output);
    }

    @Command(name = "automation api-collection update", description = "Update an API collection's name or description.")
    public void apiCollectionUpdate(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "name") String name,
        @Option(longName = "description") String description,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        ApiCollectionUpdateModel request = new ApiCollectionUpdateModel()
            .name(name)
            .description(description);

        ApiCollectionModel apiCollection;

        try {
            apiCollection = AutomationClientFactory.apiCollectionApi(config)
                .updateApiCollection(id, request, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(apiCollection, output);
    }

    @Command(name = "automation api-collection delete", description = "Delete an API collection and its endpoints.")
    public void apiCollectionDelete(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment, null);

        try {
            AutomationClientFactory.apiCollectionApi(config)
                .deleteApiCollection(id, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        System.out.println("API collection deleted.");
    }

    @Command(
        name = "automation api-collection update-version",
        description = "Move an API collection to a newer published project version; prints the removed endpoints and"
            + " the connections and inputs the new version still needs.")
    public void apiCollectionUpdateVersion(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "project-version", required = true) Integer projectVersion,
        @Option(longName = "remove-missing-endpoints") Boolean removeMissingEndpoints,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        ApiCollectionProjectVersionInputModel request = new ApiCollectionProjectVersionInputModel()
            .projectVersion(projectVersion)
            .removeMissingEndpoints(Boolean.TRUE.equals(removeMissingEndpoints));

        ApiCollectionVersionUpdateModel versionUpdate;

        try {
            versionUpdate = AutomationClientFactory.apiCollectionApi(config)
                .updateApiCollectionProjectVersion(id, request, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(versionUpdate, output);
    }

    @Command(name = "automation api-collection endpoint list", description = "List the endpoints of an API collection.")
    public void apiCollectionEndpointList(
        @Option(longName = "collection-id", required = true) Long collectionId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        List<ApiCollectionEndpointModel> endpoints;

        try {
            endpoints = AutomationClientFactory.apiCollectionEndpointApi(config)
                .getApiCollectionEndpoints(collectionId, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(endpoints, output);
    }

    @Command(
        name = "automation api-collection endpoint create",
        description = "Create an endpoint in an API collection; repeat --connection node:key=connectionId to bind"
            + " connections and pass --inputs-json for the workflow inputs.")
    public void apiCollectionEndpointCreate(
        @Option(longName = "collection-id", required = true) Long collectionId,
        @Option(longName = "name", required = true) String name,
        @Option(longName = "method", required = true) String method,
        @Option(longName = "path", required = true) String path,
        @Option(longName = "workflow-uuid", required = true) String workflowUuid,
        @Option(longName = "inputs-json") String inputsJson,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output,
        CommandContext commandContext) {

        ApiCollectionEndpointInputModel request = new ApiCollectionEndpointInputModel()
            .name(name)
            .httpMethod(parseMethod(method))
            .path(path)
            .workflowUuid(workflowUuid);

        List<EndpointConnectionModel> connections = parseConnections(optionValues(commandContext, "connection"));

        if (!connections.isEmpty()) {
            request.setConnections(connections);
        }

        if (inputsJson != null) {
            request.setInputs(parseInputs(inputsJson));
        }

        CliConfig config = resolve(profile, host, token, environment, null);

        ApiCollectionEndpointModel endpoint;

        try {
            endpoint = AutomationClientFactory.apiCollectionEndpointApi(config)
                .createApiCollectionEndpoint(collectionId, request, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(endpoint, output);
    }

    @Command(
        name = "automation api-collection endpoint update",
        description = "Update an API collection endpoint; omitted fields keep their value, and --connection or"
            + " --inputs-json replace the stored connections or inputs.")
    public void apiCollectionEndpointUpdate(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "name") String name,
        @Option(longName = "method") String method,
        @Option(longName = "path") String path,
        @Option(longName = "workflow-uuid") String workflowUuid,
        @Option(longName = "inputs-json") String inputsJson,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output,
        CommandContext commandContext) {

        ApiCollectionEndpointUpdateModel request = new ApiCollectionEndpointUpdateModel()
            .name(name)
            .path(path)
            .workflowUuid(workflowUuid)
            .connections(null)
            .inputs(null);

        if (method != null) {
            request.setHttpMethod(parseMethod(method));
        }

        List<EndpointConnectionModel> connections = parseConnections(optionValues(commandContext, "connection"));

        if (!connections.isEmpty()) {
            request.setConnections(connections);
        }

        if (inputsJson != null) {
            request.setInputs(parseInputs(inputsJson));
        }

        CliConfig config = resolve(profile, host, token, environment, null);

        ApiCollectionEndpointModel endpoint;

        try {
            endpoint = AutomationClientFactory.apiCollectionEndpointApi(config)
                .updateApiCollectionEndpoint(id, request, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(endpoint, output);
    }

    @Command(name = "automation api-collection endpoint delete", description = "Delete an API collection endpoint.")
    public void apiCollectionEndpointDelete(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment, null);

        try {
            AutomationClientFactory.apiCollectionEndpointApi(config)
                .deleteApiCollectionEndpoint(id, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        System.out.println("API collection endpoint deleted.");
    }

    @Command(name = "automation api-collection endpoint enable", description = "Enable an API collection endpoint.")
    public void apiCollectionEndpointEnable(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        setEnabled(id, true, profile, host, token, environment, output);
    }

    @Command(name = "automation api-collection endpoint disable", description = "Disable an API collection endpoint.")
    public void apiCollectionEndpointDisable(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        setEnabled(id, false, profile, host, token, environment, output);
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private void setEnabled(
        Long id, boolean enabled, String profile, String host, String token, String environment, String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        ApiCollectionEndpointEnabledInputModel request = new ApiCollectionEndpointEnabledInputModel().enabled(enabled);

        ApiCollectionEndpointModel endpoint;

        try {
            endpoint = AutomationClientFactory.apiCollectionEndpointApi(config)
                .enableApiCollectionEndpoint(id, request, null);
        } catch (ApiException apiException) {
            throw AutomationClientFactory.toCliException(apiException);
        }

        new OutputRenderer(System.out).render(endpoint, output);
    }

    private CliConfig resolve(String profile, String host, String token, String environment, Long workspaceId) {
        return new ProfileResolver(configPath, environmentVariables).resolve(
            new Overrides(
                host, token, environment == null ? null : Environment.valueOf(environment), workspaceId, profile));
    }

    /**
     * Spring Shell's annotation-based command options bind a single value per option name; a repeated flag such as
     * {@code --connection a:b=1 --connection c:d=2} needs to be read off the raw parsed input instead.
     */
    private static List<String> optionValues(CommandContext commandContext, String longName) {
        return commandContext.parsedInput()
            .options()
            .stream()
            .filter(option -> longName.equals(option.longName()))
            .map(CommandOption::value)
            .toList();
    }

    private static HttpMethodModel parseMethod(String method) {
        try {
            return HttpMethodModel.fromValue(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new CliException(1, "Unknown HTTP method '" + method + "'; use DELETE, GET, PATCH, POST or PUT.");
        }
    }

    static List<EndpointConnectionModel> parseConnections(List<String> specs) {
        return specs.stream()
            .map(AutomationApiCollectionCommand::parseConnection)
            .toList();
    }

    static EndpointConnectionModel parseConnection(String spec) {
        int equalsIndex = spec.lastIndexOf('=');
        int colonIndex = spec.indexOf(':');

        if (colonIndex <= 0 || equalsIndex <= colonIndex + 1 || equalsIndex == spec.length() - 1) {
            throw new CliException(1, "A connection is node:key=connectionId, got '" + spec + "'");
        }

        long connectionId;

        try {
            connectionId = Long.parseLong(spec.substring(equalsIndex + 1));
        } catch (NumberFormatException numberFormatException) {
            throw new CliException(1, "A connection is node:key=connectionId, got '" + spec + "'");
        }

        return new EndpointConnectionModel()
            .workflowNodeName(spec.substring(0, colonIndex))
            .key(spec.substring(colonIndex + 1, equalsIndex))
            .connectionId(connectionId);
    }

    static Map<String, Object> parseInputs(String inputsJson) {
        try {
            return OBJECT_MAPPER.readValue(inputsJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException jsonProcessingException) {
            throw new CliException(1, "--inputs-json must be a JSON object, got '" + inputsJson + "'");
        }
    }
}
