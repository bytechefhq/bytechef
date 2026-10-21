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
import com.bytechef.cli.client.automationapp.model.AppBindableConnectionModel;
import com.bytechef.cli.client.automationapp.model.AppBindingModel;
import com.bytechef.cli.client.automationapp.model.AppHostingTargetModel;
import com.bytechef.cli.client.automationapp.model.AppInputModel;
import com.bytechef.cli.client.automationapp.model.AppModel;
import com.bytechef.cli.client.automationapp.model.AppSecretKeyModel;
import com.bytechef.cli.client.automationapp.model.AppWorkflowBindingModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * App management for the coding-agent loop, DEVELOPMENT only: create an app and write its key to the env file without
 * printing it, link a directory to an existing app, list apps, and show an app's bindings.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationAppCommand {

    private static final String DEFAULT_ENV_FILE = ".env.local";

    private static final String EXISTING_LINK_HINT =
        "Run 'bytechef automation app status' to see the linked app, or delete the file to link another app.";

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private Path workingDirectory = Path.of("")
        .toAbsolutePath();
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    @Command(
        name = "automation app create",
        description = "Create an app in DEVELOPMENT and write its key to the env file; the key is never printed.")
    public void create(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "description") String description,
        @Option(longName = "hosting-target", defaultValue = "VERCEL") String hostingTarget,
        @Option(longName = "env-file", defaultValue = ".env.local") String envFile,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        long resolvedWorkspaceId = AppCommandSupport.requireWorkspaceId(config);

        AppCommandSupport.refuseExistingAppLink(workingDirectory);

        Path envFilePath = AppCommandSupport.resolveEnvFile(workingDirectory, envFile);
        AppInputModel appInputModel = new AppInputModel();

        appInputModel.setName(name);
        appInputModel.setDescription(description);
        appInputModel.setHostingTarget(parseHostingTarget(hostingTarget));

        AppApi appApi = AutomationClientFactory.appApi(config);
        AppSecretKeyModel appSecretKeyModel;

        try {
            appSecretKeyModel = AppCommandSupport.required(
                appApi.createApp(resolvedWorkspaceId, appInputModel, null), "the created app");
        } catch (ApiException exception) {
            if (exception.getCode() == 0) {
                throw new CliException(
                    1, "ByteChef gave no usable reply to the create request. The request may have reached ByteChef: "
                        + "run 'bytechef automation app list' before retrying; if the app exists, run 'bytechef "
                        + "automation app link --app-id <id>' then 'bytechef automation app key rotate'.");
            }

            throw AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);
        }

        AppModel appModel = AppCommandSupport.required(appSecretKeyModel.getApp(), "the created app");
        long createdAppId = AppCommandSupport.required(appModel.getId(), "the created app id");
        String bytechefUrl = AppCommandSupport.normalizeHost(config.host());
        List<String> failures = new ArrayList<>();

        boolean appLinkWritten = writeAppLink(
            new AppLink(
                createdAppId, appModel.getUuid(), resolvedWorkspaceId, Environment.DEVELOPMENT.name(),
                bytechefUrl),
            failures);

        boolean envFileWritten = writeEnvFile(envFilePath, bytechefUrl, appSecretKeyModel.getSecretKey(), failures);

        if (!failures.isEmpty()) {
            throw new CliException(
                1, postCreateFailureMessage(createdAppId, appLinkWritten, envFileWritten, failures));
        }

        AppCommandSupport.warnWhenNotGitIgnored(envFilePath, err);

        AppCreatedResult appCreatedResult = new AppCreatedResult(
            createdAppId, appModel.getUuid(), appModel.getName(), resolvedWorkspaceId,
            Environment.DEVELOPMENT.name(), String.valueOf(AppLinkFile.pathIn(workingDirectory)),
            envFilePath.toString());

        new OutputRenderer(out).render(appCreatedResult, output);
    }

    @Command(
        name = "automation app link",
        description = "Write .bytechef/app.json for an existing DEVELOPMENT app in the current directory; no key.")
    public void link(
        @Option(longName = "app-id", required = true) Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);

        AppCommandSupport.refuseExistingAppLink(workingDirectory);

        AppApi appApi = AutomationClientFactory.appApi(config);
        AppModel appModel;

        try {
            appModel = AppCommandSupport.required(appApi.getApp(appId, null), "app " + appId);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        long appWorkspaceId = AppCommandSupport.checkDevelopmentApp(appModel, appId, config);
        long linkedAppId = AppCommandSupport.required(appModel.getId(), "the id of app " + appId);

        AppLink appLink = new AppLink(
            linkedAppId, appModel.getUuid(), appWorkspaceId, Environment.DEVELOPMENT.name(),
            AppCommandSupport.normalizeHost(config.host()));

        try {
            AppLinkFile.write(workingDirectory, appLink);
        } catch (FileAlreadyExistsException exception) {
            throw new CliException(1, AppLinkFile.pathIn(workingDirectory) + " already exists. " + EXISTING_LINK_HINT);
        } catch (IOException exception) {
            throw new CliException(
                1, "Could not write " + AppLinkFile.pathIn(workingDirectory) + ": " + exception.getMessage());
        }

        new OutputRenderer(out).render(appLink, output);
    }

    @Command(name = "automation app list", description = "List the workspace's apps in DEVELOPMENT.")
    public void appList(
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        long resolvedWorkspaceId = AppCommandSupport.requireWorkspaceId(config);
        AppApi appApi = AutomationClientFactory.appApi(config);

        try {
            new OutputRenderer(out).render(appApi.getApps(resolvedWorkspaceId, null), output);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }
    }

    @Command(
        name = "automation app status",
        description = "Show the app, its connection, workflow and API collection bindings with their availability.")
    public void status(
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
        long resolvedAppId = appLocation.appId();
        AppApi appApi = AutomationClientFactory.appApi(config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, appApi);

        AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(config);
        AppApiCollectionBindingApi appApiCollectionBindingApi =
            AutomationClientFactory.appApiCollectionBindingApi(config);
        AppStatusResult appStatusResult;

        try {
            AppModel appModel = AppCommandSupport.required(appApi.getApp(resolvedAppId, null), "app " + resolvedAppId);
            Map<Long, Integer> componentVersions = componentVersions(appApi, resolvedAppId);

            List<ConnectionBindingStatus> connections =
                AppCommandSupport.required(appApi.getAppBindings(resolvedAppId, null), "the connection bindings")
                    .stream()
                    .map(appBindingModel -> toConnectionBindingStatus(appBindingModel, componentVersions))
                    .toList();
            List<WorkflowBindingStatus> workflows = AppCommandSupport.required(
                appWorkflowBindingApi.getAppWorkflowBindings(resolvedAppId, null), "the workflow bindings")
                .stream()
                .map(AutomationAppCommand::toWorkflowBindingStatus)
                .toList();

            List<ApiCollectionBindingStatus> apiCollections = AppCommandSupport.required(
                appApiCollectionBindingApi.getAppApiCollectionBindings(resolvedAppId, null),
                "the API collection bindings")
                .stream()
                .map(AutomationAppCommand::toApiCollectionBindingStatus)
                .toList();

            appStatusResult = new AppStatusResult(toAppSummary(appModel), connections, workflows, apiCollections);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        if ("table".equalsIgnoreCase(output)) {
            renderStatusTable(appStatusResult);
        } else {
            new OutputRenderer(out).renderJson(appStatusResult);
        }
    }

    @Command(
        name = "automation app key rotate",
        description = "Rotate the app key and write the new one to the env file; the key is never printed.")
    public void keyRotate(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "env-file") String envFile,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);

        if (envFile == null && appLocation.overridesLinkedApp()) {
            throw new CliException(
                1, "App " + appLocation.appId() + " is not the app this directory is linked to (app "
                    + appLocation.linkedAppId() + "), whose key the default env file holds. Pass --env-file with the "
                    + "file that should receive the key of app " + appLocation.appId() + ", or run the command "
                    + "outside the linked directory.");
        }

        Path envFilePath = AppCommandSupport.resolveEnvFile(
            appLocation.appDirectory(), envFile == null ? DEFAULT_ENV_FILE : envFile);
        AppApi appApi = AutomationClientFactory.appApi(config);

        AppCommandSupport.requireDevelopmentApp(appLocation, config, appApi);

        String rotatedKey;

        try {
            AppSecretKeyModel appSecretKeyModel = appApi.rotateAppSecretKey(appLocation.appId(), null);

            rotatedKey = appSecretKeyModel == null ? null : appSecretKeyModel.getSecretKey();
        } catch (ApiException exception) {
            if (exception.getCode() == 0) {
                throw new CliException(
                    1, keyRotateFailureMessage(appLocation.appId(), "ByteChef gave no usable reply"));
            }

            throw AutomationClientFactory.toCliException(
                exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);
        }

        if (rotatedKey == null) {
            throw new CliException(
                1, keyRotateFailureMessage(appLocation.appId(), "ByteChef returned no key"));
        }

        try {
            EnvFile.writeBytechefValues(envFilePath, AppCommandSupport.normalizeHost(config.host()), rotatedKey);
        } catch (IOException | IllegalArgumentException exception) {
            throw new CliException(
                1, keyRotateFailureMessage(
                    appLocation.appId(), "could not write " + envFilePath + " (" + exception.getMessage() + ")"));
        }

        AppCommandSupport.warnWhenNotGitIgnored(envFilePath, err);

        new OutputRenderer(out).render(new AppKeyRotatedResult(appLocation.appId(), envFilePath.toString()), output);
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

    void setOutput(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    private static Map<Long, Integer> componentVersions(AppApi appApi, long appId) throws ApiException {
        Map<Long, Integer> componentVersions = new HashMap<>();

        try {
            for (AppBindableConnectionModel connection : AppCommandSupport
                .required(appApi.getAppBindableConnections(appId, null, null), "the bindable connections")) {
                componentVersions.put(connection.getId(), connection.getComponentVersion());
            }
        } catch (ApiException exception) {
            if (exception.getCode() != 403) {
                throw exception;
            }
        }

        return componentVersions;
    }

    private static AppHostingTargetModel parseHostingTarget(String hostingTarget) {
        try {
            return AppHostingTargetModel.fromValue(hostingTarget);
        } catch (IllegalArgumentException exception) {
            throw new CliException(1, "Unknown --hosting-target '" + hostingTarget + "'. Use VERCEL or SELF_HOSTED.");
        }
    }

    private static AppSummary toAppSummary(AppModel appModel) {
        return new AppSummary(
            appModel.getId(), appModel.getUuid(), appModel.getName(), AppCommandSupport.valueOf(appModel.getStatus()),
            AppCommandSupport.valueOf(appModel.getEnvironment()), appModel.getWorkspaceId(), appModel.getUrl());
    }

    private static ConnectionBindingStatus toConnectionBindingStatus(
        AppBindingModel appBindingModel, Map<Long, Integer> componentVersions) {

        return new ConnectionBindingStatus(
            appBindingModel.getBindingKey(), appBindingModel.getConnectionId(), appBindingModel.getConnectionName(),
            appBindingModel.getComponentName(), componentVersions.get(appBindingModel.getConnectionId()),
            AppCommandSupport.nullToEmpty(appBindingModel.getActionNames()),
            AppCommandSupport.nullToEmpty(appBindingModel.getToolNames()),
            Boolean.TRUE.equals(appBindingModel.getProxyEnabled()),
            AppCommandSupport.nullToEmpty(appBindingModel.getProxyAllowedMethods()));
    }

    private static WorkflowBindingStatus toWorkflowBindingStatus(AppWorkflowBindingModel appWorkflowBindingModel) {
        return new WorkflowBindingStatus(
            appWorkflowBindingModel.getWorkflowKey(), appWorkflowBindingModel.getProjectDeploymentId(),
            appWorkflowBindingModel.getProjectDeploymentName(), appWorkflowBindingModel.getWorkflowUuid(),
            appWorkflowBindingModel.getWorkflowName(), Boolean.TRUE.equals(appWorkflowBindingModel.getAvailable()));
    }

    private static ApiCollectionBindingStatus toApiCollectionBindingStatus(
        AppApiCollectionBindingModel appApiCollectionBindingModel) {

        return new ApiCollectionBindingStatus(
            appApiCollectionBindingModel.getApiCollectionId(), appApiCollectionBindingModel.getName(),
            "v" + appApiCollectionBindingModel.getCollectionVersion() + "/"
                + appApiCollectionBindingModel.getContextPath(),
            appApiCollectionBindingModel.getProjectName(),
            Boolean.TRUE.equals(appApiCollectionBindingModel.getAvailable()));
    }

    private static String cell(Object value) {
        return value == null ? "" : value.toString();
    }

    private void renderStatusTable(AppStatusResult appStatusResult) {
        OutputRenderer outputRenderer = new OutputRenderer(out);
        AppSummary appSummary = appStatusResult.app();

        outputRenderer.message(
            "App " + appSummary.id() + " " + appSummary.name() + " (" + appSummary.status() + ", "
                + appSummary.environment() + ")");
        outputRenderer.message("");
        outputRenderer.message("Connections");

        if (appStatusResult.connections()
            .isEmpty()) {

            outputRenderer.message("(none)");
        } else {
            List<List<String>> rows = new ArrayList<>();

            for (ConnectionBindingStatus connection : appStatusResult.connections()) {
                rows.add(
                    List.of(
                        cell(connection.bindingKey()),
                        cell(connection.connectionId()) + " " + cell(connection.connectionName()),
                        cell(connection.componentName()),
                        connection.componentVersion() == null ? "none" : cell(connection.componentVersion()),
                        String.join(",", connection.actions()), String.join(",", connection.tools()),
                        connection.proxyEnabled() ? String.join(",", connection.proxyMethods()) : "off"));
            }

            outputRenderer.renderTable(
                List.of("BINDING KEY", "CONNECTION", "COMPONENT", "VERSION", "ACTIONS", "TOOLS", "PROXY"), rows);
        }

        outputRenderer.message("");
        outputRenderer.message("Workflows");

        if (appStatusResult.workflows()
            .isEmpty()) {

            outputRenderer.message("(none)");
        } else {
            List<List<String>> rows = new ArrayList<>();

            for (WorkflowBindingStatus workflow : appStatusResult.workflows()) {
                rows.add(
                    List.of(
                        cell(workflow.workflowKey()), cell(workflow.projectDeploymentId()),
                        cell(workflow.workflowName()), cell(workflow.workflowUuid()),
                        workflow.available() ? "yes" : "no"));
            }

            outputRenderer.renderTable(
                List.of("WORKFLOW KEY", "DEPLOYMENT", "WORKFLOW", "WORKFLOW UUID", "AVAILABLE"), rows);
        }

        outputRenderer.message("");
        outputRenderer.message("API collections");

        if (appStatusResult.apiCollections()
            .isEmpty()) {

            outputRenderer.message("(none)");

            return;
        }

        List<List<String>> apiCollectionRows = new ArrayList<>();

        for (ApiCollectionBindingStatus apiCollection : appStatusResult.apiCollections()) {
            apiCollectionRows.add(
                List.of(
                    cell(apiCollection.apiCollectionId()), cell(apiCollection.name()), cell(apiCollection.path()),
                    cell(apiCollection.projectName()), apiCollection.available() ? "yes" : "no"));
        }

        outputRenderer.renderTable(List.of("ID", "API COLLECTION", "PATH", "PROJECT", "AVAILABLE"), apiCollectionRows);
    }

    private boolean writeAppLink(AppLink appLink, List<String> failures) {
        try {
            AppLinkFile.write(workingDirectory, appLink);

            return true;
        } catch (FileAlreadyExistsException exception) {
            throw new CliException(1, linkedMeanwhileMessage(appLink.id()));
        } catch (IOException exception) {
            failures.add(
                "could not write " + AppLinkFile.pathIn(workingDirectory) + " (" + exception.getMessage() + ")");

            return false;
        }
    }

    private static boolean writeEnvFile(Path envFile, String bytechefUrl, String secretKey, List<String> failures) {
        try {
            EnvFile.writeBytechefValues(envFile, bytechefUrl, secretKey);

            return true;
        } catch (IOException | IllegalArgumentException exception) {
            failures.add("could not write " + envFile + " (" + exception.getMessage() + ")");

            return false;
        }
    }

    private String linkedMeanwhileMessage(long appId) {
        return "Created app " + appId + ", but another run linked " + workingDirectory + " to another app in the "
            + "meantime (" + AppLinkFile.pathIn(workingDirectory) + "), so this directory now belongs to that app and "
            + "the key of app " + appId + " was not saved; it is never shown. Delete app " + appId + " in the "
            + "ByteChef UI, or run 'bytechef automation app link --app-id " + appId + "' in a directory of its own "
            + "and then 'bytechef automation app key rotate' there.";
    }

    private static String keyRotateFailureMessage(long appId, String problem) {
        return "Rotating the key of app " + appId + " failed after the request was sent: " + problem
            + ". The app may already be rotated and the previous key may no longer work; the new key was not saved. "
            + "Run 'bytechef automation app key rotate' again.";
    }

    private String postCreateFailureMessage(
        long appId, boolean appLinkWritten, boolean envFileWritten, List<String> failures) {

        StringBuilder message = new StringBuilder();

        message.append("Created app ")
            .append(appId)
            .append(", but ")
            .append(String.join(" and ", failures))
            .append('.');

        if (!appLinkWritten) {
            message.append(" Run 'bytechef automation app link --app-id ")
                .append(appId)
                .append("' in ")
                .append(workingDirectory)
                .append(" to restore .bytechef/app.json.");
        }

        if (!envFileWritten) {
            message.append(appLinkWritten ? " Run" : " Then run")
                .append(" 'bytechef automation app key rotate' there to put a new key in the env file; the key from ")
                .append("this run was not saved and is never shown.");
        }

        return message.toString();
    }

    public record AppCreatedResult(
        long id, String uuid, String name, long workspaceId, String environment, String appFile, String envFile) {
    }

    public record AppKeyRotatedResult(long id, String envFile) {
    }

    public record AppStatusResult(
        AppSummary app, List<ConnectionBindingStatus> connections, List<WorkflowBindingStatus> workflows,
        List<ApiCollectionBindingStatus> apiCollections) {

        public AppStatusResult {
            connections = List.copyOf(connections);
            workflows = List.copyOf(workflows);
            apiCollections = List.copyOf(apiCollections);
        }
    }

    public record ApiCollectionBindingStatus(
        Long apiCollectionId, String name, String path, String projectName, boolean available) {
    }

    public record AppSummary(
        Long id, String uuid, String name, String status, String environment, Long workspaceId, String url) {
    }

    public record ConnectionBindingStatus(
        String bindingKey, Long connectionId, String connectionName, String componentName, Integer componentVersion,
        List<String> actions, List<String> tools, boolean proxyEnabled, List<String> proxyMethods) {

        public ConnectionBindingStatus {
            actions = List.copyOf(actions);
            tools = List.copyOf(tools);
            proxyMethods = List.copyOf(proxyMethods);
        }
    }

    public record WorkflowBindingStatus(
        String workflowKey, Long projectDeploymentId, String projectDeploymentName, String workflowUuid,
        String workflowName, boolean available) {
    }
}
