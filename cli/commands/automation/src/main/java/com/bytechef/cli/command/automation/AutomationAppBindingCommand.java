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

import com.bytechef.cli.client.automationapp.ApiException;
import com.bytechef.cli.client.automationapp.api.AppApi;
import com.bytechef.cli.client.automationapp.api.AppApiCollectionBindingApi;
import com.bytechef.cli.client.automationapp.api.AppWorkflowBindingApi;
import com.bytechef.cli.client.automationapp.model.AppApiCollectionBindingModel;
import com.bytechef.cli.client.automationapp.model.AppBindableApiCollectionModel;
import com.bytechef.cli.client.automationapp.model.AppBindableConnectionModel;
import com.bytechef.cli.client.automationapp.model.AppBindableWorkflowModel;
import com.bytechef.cli.client.automationapp.model.AppBindingInputModel;
import com.bytechef.cli.client.automationapp.model.AppBindingModel;
import com.bytechef.cli.client.automationapp.model.AppComponentOperationsModel;
import com.bytechef.cli.client.automationapp.model.AppWorkflowBindingInputModel;
import com.bytechef.cli.client.automationapp.model.AppWorkflowBindingModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.input.CliArgs;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Binds connections, deployed workflows and API collections to an app, and removes bindings, DEVELOPMENT only. A
 * connection binding is always a full replace: the binding becomes exactly what the options name.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationAppBindingCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private Path workingDirectory = Path.of("")
        .toAbsolutePath();
    private PrintStream out = System.out;

    @Command(
        name = "automation app bind connection",
        description = "Bind a connection to the app. The binding becomes exactly what the options name: an omitted "
            + "--actions, --tools or --proxy means none.")
    public void bindConnection(
        @Option(longName = "binding-key", required = true) String bindingKey,
        @Option(longName = "component-name") String componentName,
        @Option(longName = "connection-id") Long connectionId,
        @Option(longName = "actions") String actions,
        @Option(longName = "tools") String tools,
        @Option(longName = "proxy") String proxy,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        if (componentName == null && connectionId == null) {
            throw new CliException(1, "Pass --component-name (the CLI then picks the connection) or --connection-id.");
        }

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppApi appApi = AutomationClientFactory.appApi(config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, appApi);

        List<String> actionNames = AppCommandSupport.nullToEmpty(CliArgs.splitCsv(actions));
        List<String> toolNames = AppCommandSupport.nullToEmpty(CliArgs.splitCsv(tools));
        List<String> proxyMethods = AppCommandSupport.nullToEmpty(CliArgs.splitCsv(proxy))
            .stream()
            .map(proxyMethod -> proxyMethod.toUpperCase(Locale.ROOT))
            .toList();

        try {
            AppBindableConnectionModel connection = chooseConnection(
                appApi, appLocation.appId(), componentName, connectionId);

            checkOperationNames(appApi, connection, actionNames, toolNames);

            AppBindingInputModel appBindingInputModel = new AppBindingInputModel();

            appBindingInputModel.setConnectionId(
                AppCommandSupport.required(connection.getId(), "the connection id"));
            appBindingInputModel.setActionNames(actionNames);
            appBindingInputModel.setToolNames(toolNames);
            appBindingInputModel.setProxyEnabled(!proxyMethods.isEmpty());
            appBindingInputModel.setProxyAllowedMethods(proxyMethods);

            AppBindingModel appBindingModel = AppCommandSupport.required(
                appApi.bindAppConnection(appLocation.appId(), bindingKey, appBindingInputModel, null), "the binding");

            new OutputRenderer(out).render(appBindingModel, output);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.BIND_CONNECTION_FORBIDDEN_HINT);
        }
    }

    @Command(
        name = "automation app bind workflow",
        description = "Bind a deployed app-callable workflow to the app under a workflow key.")
    public void bindWorkflow(
        @Option(longName = "workflow-key", required = true) String workflowKey,
        @Option(longName = "deployment-id", required = true) Long deploymentId,
        @Option(longName = "workflow-uuid", required = true) String workflowUuid,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, AutomationClientFactory.appApi(config));

        AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(config);
        AppWorkflowBindingInputModel appWorkflowBindingInputModel = new AppWorkflowBindingInputModel();

        appWorkflowBindingInputModel.setProjectDeploymentId(deploymentId);
        appWorkflowBindingInputModel.setWorkflowUuid(workflowUuid);

        AppWorkflowBindingModel appWorkflowBindingModel;

        try {
            appWorkflowBindingModel = AppCommandSupport.required(
                appWorkflowBindingApi.bindAppWorkflow(appLocation.appId(), workflowKey, appWorkflowBindingInputModel,
                    null),
                "the workflow binding");
        } catch (ApiException exception) {
            CliException cliException = AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);

            if (exception.getCode() == 400) {
                throw new CliException(
                    cliException.exitCode(),
                    cliException.getMessage() + " "
                        + describeBindableWorkflows(appWorkflowBindingApi, appLocation.appId()));
            }

            throw cliException;
        }

        new OutputRenderer(out).render(appWorkflowBindingModel, output);
    }

    @Command(
        name = "automation app bind api-collection",
        description = "Bind an API collection of the app's workspace to the app. Without --api-collection-id the "
            + "bindable API collections are listed.")
    public void bindApiCollection(
        @Option(longName = "api-collection-id") Long apiCollectionId,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, AutomationClientFactory.appApi(config));

        AppApiCollectionBindingApi appApiCollectionBindingApi =
            AutomationClientFactory.appApiCollectionBindingApi(config);

        if (apiCollectionId == null) {
            throw new CliException(
                1, "Pass --api-collection-id. "
                    + describeBindableApiCollections(appApiCollectionBindingApi, appLocation.appId()));
        }

        AppApiCollectionBindingModel appApiCollectionBindingModel;

        try {
            appApiCollectionBindingModel = AppCommandSupport.required(
                appApiCollectionBindingApi.bindAppApiCollection(appLocation.appId(), apiCollectionId, null),
                "the API collection binding");
        } catch (ApiException exception) {
            CliException cliException = AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);

            if (exception.getCode() == 400) {
                throw new CliException(
                    cliException.exitCode(),
                    cliException.getMessage() + " "
                        + describeBindableApiCollections(appApiCollectionBindingApi, appLocation.appId()));
            }

            throw cliException;
        }

        new OutputRenderer(out).render(appApiCollectionBindingModel, output);
    }

    @Command(name = "automation app unbind connection", description = "Remove the app's connection binding.")
    public void unbindConnection(
        @Option(longName = "binding-key", required = true) String bindingKey,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppApi appApi = AutomationClientFactory.appApi(config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, appApi);

        try {
            appApi.unbindAppConnection(appLocation.appId(), bindingKey, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);
        }

        new OutputRenderer(out).render(new UnbindResult(appLocation.appId(), bindingKey, true), output);
    }

    @Command(name = "automation app unbind workflow", description = "Remove the app's workflow binding.")
    public void unbindWorkflow(
        @Option(longName = "workflow-key", required = true) String workflowKey,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, AutomationClientFactory.appApi(config));

        AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(config);

        try {
            appWorkflowBindingApi.unbindAppWorkflow(appLocation.appId(), workflowKey, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);
        }

        new OutputRenderer(out).render(new UnbindResult(appLocation.appId(), workflowKey, true), output);
    }

    @Command(name = "automation app unbind api-collection", description = "Remove the app's API collection binding.")
    public void unbindApiCollection(
        @Option(longName = "api-collection-id", required = true) Long apiCollectionId,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, AutomationClientFactory.appApi(config));

        AppApiCollectionBindingApi appApiCollectionBindingApi =
            AutomationClientFactory.appApiCollectionBindingApi(config);

        try {
            appApiCollectionBindingApi.unbindAppApiCollection(appLocation.appId(), apiCollectionId, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);
        }

        new OutputRenderer(out).render(
            new UnbindResult(appLocation.appId(), String.valueOf(apiCollectionId), true), output);
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    void setOutput(PrintStream out) {
        this.out = out;
    }

    private static AppBindableConnectionModel chooseConnection(
        AppApi appApi, long appId, String componentName, Long connectionId) throws ApiException {

        List<AppBindableConnectionModel> candidates = AppCommandSupport.required(
            appApi.getAppBindableConnections(appId, componentName, null), "the bindable connections");

        if (connectionId != null) {
            AppBindableConnectionModel connection = candidates.stream()
                .filter(candidate -> connectionId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new CliException(
                    1, "Connection " + connectionId + " cannot be bound to app " + appId + ". "
                        + describeCandidates(candidates)));

            if (!Boolean.TRUE.equals(connection.getUsable())) {
                throw new CliException(
                    1, "Connection " + connectionId + " (" + connection.getName() + ") is not usable: its credentials "
                        + "are not valid or you may not use it. " + describeCandidates(candidates));
            }

            return connection;
        }

        List<AppBindableConnectionModel> usableCandidates = candidates.stream()
            .filter(candidate -> Boolean.TRUE.equals(candidate.getUsable()))
            .toList();

        if (usableCandidates.size() == 1) {
            return usableCandidates.getFirst();
        }

        if (candidates.isEmpty()) {
            throw new CliException(
                1, "No " + componentName + " connection can be bound to app " + appId + ". Ask a person to create "
                    + "one in the ByteChef UI (DEVELOPMENT, the app's workspace), then run this command again.");
        }

        if (usableCandidates.isEmpty()) {
            throw new CliException(
                1, "No usable " + componentName + " connection for app " + appId + ": fix a connection's "
                    + "credentials in the ByteChef UI or ask for permission to use one. "
                    + describeCandidates(candidates));
        }

        throw new CliException(
            1, "Several usable " + componentName + " connections for app " + appId + "; pass --connection-id with "
                + "one of them. " + describeCandidates(candidates));
    }

    private static void checkOperationNames(
        AppApi appApi, AppBindableConnectionModel connection, List<String> actionNames, List<String> toolNames)
        throws ApiException {

        if (actionNames.isEmpty() && toolNames.isEmpty()) {
            return;
        }

        String componentName = AppCommandSupport.required(connection.getComponentName(), "the connection's component");
        Integer componentVersion = connection.getComponentVersion();

        if (componentVersion == null) {
            throw new CliException(
                1, "The component version of connection " + connection.getId() + " (" + connection.getName()
                    + ") is unknown, so the --actions and --tools names cannot be checked. Bind it without --actions "
                    + "and --tools, or ask a person to check the connection in the ByteChef UI.");
        }

        AppComponentOperationsModel appComponentOperationsModel = AppCommandSupport.required(
            appApi.getAppComponentOperations(componentName, componentVersion), "the component operations");
        List<String> allowedActionNames = AppCommandSupport.nullToEmpty(appComponentOperationsModel.getActionNames());
        List<String> allowedToolNames = AppCommandSupport.nullToEmpty(appComponentOperationsModel.getToolNames());
        List<String> unknownActionNames = actionNames.stream()
            .filter(actionName -> !allowedActionNames.contains(actionName))
            .toList();
        List<String> unknownToolNames = toolNames.stream()
            .filter(toolName -> !allowedToolNames.contains(toolName))
            .toList();

        if (unknownActionNames.isEmpty() && unknownToolNames.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();

        message.append("Unknown names for ")
            .append(componentName)
            .append(" version ")
            .append(componentVersion)
            .append(':');

        if (!unknownActionNames.isEmpty()) {
            message.append(" actions ")
                .append(String.join(", ", unknownActionNames))
                .append('.');
        }

        if (!unknownToolNames.isEmpty()) {
            message.append(" tools ")
                .append(String.join(", ", unknownToolNames))
                .append('.');
        }

        message.append(" Allowed actions: ")
            .append(joinOrNone(allowedActionNames))
            .append(". Allowed tools: ")
            .append(joinOrNone(allowedToolNames))
            .append('.');

        throw new CliException(1, message.toString());
    }

    private static String describeCandidates(List<AppBindableConnectionModel> candidates) {
        if (candidates.isEmpty()) {
            return "No connection can be bound to this app.";
        }

        return "Candidates: " + candidates.stream()
            .map(
                candidate -> candidate.getId() + " (" + candidate.getName() + ", " + candidate.getComponentName() + ", "
                    + (Boolean.TRUE.equals(candidate.getUsable()) ? "usable" : "not usable") + ")")
            .collect(Collectors.joining(", ")) + ".";
    }

    private static String describeBindableWorkflows(AppWorkflowBindingApi appWorkflowBindingApi, long appId) {
        List<AppBindableWorkflowModel> bindableWorkflows;

        try {
            bindableWorkflows = appWorkflowBindingApi.getAppBindableWorkflows(appId, null);
        } catch (ApiException exception) {
            return "The bindable workflows could not be listed (HTTP " + exception.getCode() + ").";
        }

        if (bindableWorkflows == null || bindableWorkflows.isEmpty()) {
            return "No workflow can be bound yet: build an app-callable workflow, publish it and deploy it with the "
                + "management MCP server's deployProject first.";
        }

        return "Bindable workflows: " + bindableWorkflows.stream()
            .map(bindableWorkflow -> "--deployment-id " + bindableWorkflow.getProjectDeploymentId()
                + " --workflow-uuid " + bindableWorkflow.getWorkflowUuid() + " (" + bindableWorkflow.getWorkflowName()
                + ", project " + bindableWorkflow.getProjectName() + ")")
            .collect(Collectors.joining("; ")) + ".";
    }

    private static String describeBindableApiCollections(
        AppApiCollectionBindingApi appApiCollectionBindingApi, long appId) {

        List<AppBindableApiCollectionModel> bindableApiCollections;

        try {
            bindableApiCollections = appApiCollectionBindingApi.getAppBindableApiCollections(appId, null);
        } catch (ApiException exception) {
            return "The bindable API collections could not be listed (HTTP " + exception.getCode() + ").";
        }

        if (bindableApiCollections == null || bindableApiCollections.isEmpty()) {
            return "No API collection can be bound yet: create an API collection in the app's workspace, publish it "
                + "and deploy it to the app's environment first.";
        }

        return "Bindable API collections: " + bindableApiCollections.stream()
            .map(bindableApiCollection -> "--api-collection-id " + bindableApiCollection.getApiCollectionId() + " ("
                + bindableApiCollection.getName() + ", v" + bindableApiCollection.getCollectionVersion() + "/"
                + bindableApiCollection.getContextPath() + ", project " + bindableApiCollection.getProjectName()
                + (Boolean.TRUE.equals(bindableApiCollection.getAvailable()) ? "" : ", not available") + ")")
            .collect(Collectors.joining("; ")) + ".";
    }

    private static String joinOrNone(List<String> names) {
        return names.isEmpty() ? "none" : String.join(", ", names);
    }

    public record UnbindResult(long appId, String key, boolean removed) {
    }
}
