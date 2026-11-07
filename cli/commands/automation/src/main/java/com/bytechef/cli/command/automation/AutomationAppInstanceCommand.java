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
import com.bytechef.cli.client.automationapp.api.AppInstanceApi;
import com.bytechef.cli.client.automationapp.model.AppInstanceDeploymentSettingsUpdateModel;
import com.bytechef.cli.client.automationapp.model.AppInstanceInputModel;
import com.bytechef.cli.client.automationapp.model.AppInstanceModel;
import com.bytechef.cli.client.automationapp.model.AppInstanceSecretKeyModel;
import com.bytechef.cli.client.automationapp.model.AppInstanceUpdateModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Creates, lists and updates the instances of an app and manages how they are deployed. An instance is one deployment
 * target of the app in one environment, with its own key, URL, bindings, sign-in mode and deployment settings.
 * {@code create}, {@code update} and changing the deployment settings are DEVELOPMENT only, and {@code create} never
 * touches {@code .bytechef/app.json}; {@code list} and reading the deployment settings work in every environment.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationAppInstanceCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private Path workingDirectory = Path.of("")
        .toAbsolutePath();
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    @Command(
        name = "automation app instance create",
        description = "Create another instance of an app in DEVELOPMENT and write its key to --env-file; the key is "
            + "never printed. Without --user-auth-mode the server picks the sign-in mode and the output names it.")
    public void instanceCreate(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "name", required = true) String name,
        @Option(longName = "hosting-target", defaultValue = "VERCEL") String hostingTarget,
        @Option(
            longName = "user-auth-mode",
            description = "NONE, OPTIONAL or REQUIRED. Without it the instance requires sign-in when the server "
                + "can provide it and has none otherwise.") String userAuthMode,
        @Option(longName = "env-file", required = true) String envFile,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(
            longName = "workspace-id",
            description = "The workspace of the new instance; defaults to the profile's workspace.") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, null);
        long instanceWorkspaceId = workspaceId == null ? AppCommandSupport.requireWorkspaceId(config) : workspaceId;
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppInstanceInputModel appInstanceInputModel = new AppInstanceInputModel();

        appInstanceInputModel.setWorkspaceId(instanceWorkspaceId);
        appInstanceInputModel.setName(name);
        appInstanceInputModel.setHostingTarget(AppCommandSupport.parseHostingTarget(hostingTarget));
        appInstanceInputModel.setUserAuthMode(AppCommandSupport.parseUserAuthMode(userAuthMode));

        Path envFilePath = AppCommandSupport.resolveEnvFile(appLocation.appDirectory(), envFile);
        AppInstanceSecretKeyModel appInstanceSecretKeyModel;

        try {
            appInstanceSecretKeyModel = AppCommandSupport.required(
                AutomationClientFactory.appInstanceApi(config)
                    .createAppInstance(appLocation.appId(), appInstanceInputModel, null),
                "the created app instance");
        } catch (ApiException exception) {
            if (exception.getCode() == 0) {
                throw new CliException(
                    1, "ByteChef gave no usable reply to the create request. The request may have reached ByteChef: "
                        + "run 'bytechef automation app instance list --app-id " + appLocation.appId()
                        + "' before retrying; if the instance exists, run 'bytechef automation app key rotate "
                        + "--app-id " + appLocation.appId() + " --instance-id <id> --env-file " + envFile + "'.");
            }

            throw AppCommandSupport.toCreateException(exception);
        }

        AppInstanceModel appInstanceModel = AppCommandSupport.required(
            appInstanceSecretKeyModel.getAppInstance(), "the created app instance");
        long createdInstanceId = AppCommandSupport.required(appInstanceModel.getId(), "the created instance id");
        String bytechefUrl = AppCommandSupport.normalizeHost(config.host());

        try {
            EnvFile.writeBytechefValues(envFilePath, bytechefUrl, appInstanceSecretKeyModel.getSecretKey());
        } catch (IOException | IllegalArgumentException exception) {
            throw new CliException(
                1, "Created instance " + createdInstanceId + " of app " + appLocation.appId() + ", but could not "
                    + "write " + envFilePath + " (" + exception.getMessage() + "). Run 'bytechef automation app key "
                    + "rotate --app-id " + appLocation.appId() + " --instance-id " + createdInstanceId
                    + " --env-file " + envFile + "' to put a new key in the file; the key from this run was not "
                    + "saved and is never shown.");
        }

        AppCommandSupport.warnWhenNotGitIgnored(envFilePath, err);
        AppCommandSupport.warnWhenCreatedWithoutSignIn(userAuthMode, appInstanceModel.getUserAuthMode(), err);

        new OutputRenderer(out).render(
            new AppInstanceCreatedResult(
                appLocation.appId(), createdInstanceId, appInstanceModel.getUuid(), appInstanceModel.getName(),
                instanceWorkspaceId, Environment.DEVELOPMENT.name(),
                AppCommandSupport.valueOf(appInstanceModel.getUserAuthMode()), envFilePath.toString()),
            output);
    }

    @Command(
        name = "automation app instance list",
        description = "List the app's instances in the environment of the profile with their sign-in mode.")
    public void instanceList(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolve(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppInstanceApi appInstanceApi = AutomationClientFactory.appInstanceApi(config);
        List<AppInstanceSummary> appInstanceSummaries;

        try {
            appInstanceSummaries = AppCommandSupport.toInstanceSummaries(
                AppCommandSupport.required(
                    appInstanceApi.getAppInstances(appLocation.appId(), null),
                    "the instances of app " + appLocation.appId()));
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(
                exception, config.environment(), AutomationAppDeploymentCommand.VIEW_PERMISSION);
        }

        OutputRenderer outputRenderer = new OutputRenderer(out);

        if ("table".equalsIgnoreCase(output)) {
            if (appInstanceSummaries.isEmpty()) {
                outputRenderer.message("(no results)");
            } else {
                renderInstancesTable(outputRenderer, appInstanceSummaries);
            }
        } else {
            outputRenderer.renderJson(appInstanceSummaries);
        }
    }

    @Command(
        name = "automation app instance update",
        description = "Set the URL or the name of an app instance, in DEVELOPMENT. The URL is where the app runs, for "
            + "example http://localhost:3000; sign-in registers its callback from it. Options left out keep their "
            + "stored value.")
    public void instanceUpdate(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(longName = "url", description = "Where the app runs, for example http://localhost:3000.") String url,
        @Option(longName = "name") String name,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, null);

        if (url == null && name == null) {
            throw new CliException(1, "Give --url, --name or both.");
        }

        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        long appInstanceId = AppCommandSupport.resolveInstanceId(
            appLocation, instanceId, config, AutomationClientFactory.appApi(config));
        AppInstanceUpdateModel appInstanceUpdateModel = new AppInstanceUpdateModel();

        appInstanceUpdateModel.setName(name);
        appInstanceUpdateModel.setUrl(url);

        AppInstanceModel appInstanceModel;

        try {
            appInstanceModel = AppCommandSupport.required(
                AutomationClientFactory.appInstanceApi(config)
                    .updateAppInstance(appInstanceId, appInstanceUpdateModel, null),
                "the updated app instance");
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        new OutputRenderer(out).render(AppCommandSupport.toInstanceSummary(appInstanceModel), output);
    }

    @Command(
        name = "automation app instance deployment-settings set",
        description = "Set the connection and the provider settings the instance is deployed with, in DEVELOPMENT. "
            + "The settings replace the stored ones; repeat --setting for each.")
    public void deploymentSettingsSet(
        CommandContext commandContext,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(
            longName = "connection-id", required = true,
            description = "The connection of the hosting provider to deploy with.") Long connectionId,
        @Option(
            longName = "setting",
            description = "A provider setting as key=value, for example teamId=team_1. Repeat it for each setting.") String setting,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        List<String> settings = commandContext.parsedInput()
            .options()
            .stream()
            .filter(commandOption -> "setting".equals(commandOption.longName()))
            .map(CommandOption::value)
            .filter(Objects::nonNull)
            .toList();

        deploymentSettingsSet(
            settings, appId, instanceId, connectionId, profile, host, token, environment, output);
    }

    @Command(
        name = "automation app instance deployment-settings clear",
        description = "Remove the connection and the provider settings the instance is deployed with, in "
            + "DEVELOPMENT. The instance cannot be deployed until they are set again.")
    public void deploymentSettingsClear(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppInstanceDeploymentSettingsUpdateModel appInstanceDeploymentSettingsUpdateModel =
            new AppInstanceDeploymentSettingsUpdateModel();

        appInstanceDeploymentSettingsUpdateModel.setSettings(new LinkedHashMap<>());

        updateDeploymentSettings(config, appId, instanceId, appInstanceDeploymentSettingsUpdateModel, output);
    }

    @Command(
        name = "automation app instance deployment-settings get",
        description = "Show the connection and the provider settings the instance is deployed with, in any "
            + "environment.")
    public void deploymentSettingsGet(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolve(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        long appInstanceId = AppCommandSupport.resolveInstanceId(
            appLocation, instanceId, config, AutomationClientFactory.appApi(config),
            AutomationAppDeploymentCommand.VIEW_PERMISSION);
        AppInstanceModel appInstanceModel;

        try {
            appInstanceModel = AppCommandSupport.required(
                AutomationClientFactory.appInstanceApi(config)
                    .getAppInstance(appInstanceId, null),
                "app instance " + appInstanceId);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(
                exception, config.environment(), AutomationAppDeploymentCommand.VIEW_PERMISSION);
        }

        new OutputRenderer(out).render(toDeploymentSettingsResult(appInstanceModel), output);
    }

    void deploymentSettingsSet(
        List<String> settings, Long appId, Long instanceId, Long connectionId, String profile, String host,
        String token, String environment, String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, null);

        if (connectionId == null) {
            throw new CliException(1, "--connection-id is required.");
        }

        AppInstanceDeploymentSettingsUpdateModel appInstanceDeploymentSettingsUpdateModel =
            new AppInstanceDeploymentSettingsUpdateModel();

        appInstanceDeploymentSettingsUpdateModel.setConnectionId(connectionId);
        appInstanceDeploymentSettingsUpdateModel.setSettings(parseSettings(settings));

        updateDeploymentSettings(config, appId, instanceId, appInstanceDeploymentSettingsUpdateModel, output);
    }

    static void renderInstancesTable(OutputRenderer outputRenderer, List<AppInstanceSummary> appInstanceSummaries) {
        List<List<String>> rows = new ArrayList<>();

        for (AppInstanceSummary appInstanceSummary : appInstanceSummaries) {
            rows.add(
                List.of(
                    cell(appInstanceSummary.id()), cell(appInstanceSummary.name()),
                    cell(appInstanceSummary.environment()), cell(appInstanceSummary.workspaceId()),
                    cell(appInstanceSummary.status()), cell(appInstanceSummary.userAuthMode()),
                    cell(appInstanceSummary.url())));
        }

        outputRenderer.renderTable(
            List.of("ID", "INSTANCE", "ENVIRONMENT", "WORKSPACE", "STATUS", "SIGN-IN", "URL"), rows);
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

    private void updateDeploymentSettings(
        CliConfig config, Long appId, Long instanceId,
        AppInstanceDeploymentSettingsUpdateModel appInstanceDeploymentSettingsUpdateModel, String output) {

        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        long appInstanceId = AppCommandSupport.resolveInstanceId(
            appLocation, instanceId, config, AutomationClientFactory.appApi(config));
        AppInstanceModel appInstanceModel;

        try {
            appInstanceModel = AppCommandSupport.required(
                AutomationClientFactory.appInstanceApi(config)
                    .updateAppInstanceDeploymentSettings(appInstanceId, appInstanceDeploymentSettingsUpdateModel, null),
                "the updated app instance");
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        new OutputRenderer(out).render(toDeploymentSettingsResult(appInstanceModel), output);
    }

    private static Map<String, String> parseSettings(List<String> settings) {
        Map<String, String> parsedSettings = new LinkedHashMap<>();

        for (String setting : settings) {
            int separatorIndex = setting.indexOf('=');
            String key = separatorIndex < 0 ? "" : setting.substring(0, separatorIndex)
                .strip();

            if (key.isEmpty()) {
                throw new CliException(
                    1, "A --setting argument is not key=value with a non-blank key, for example --setting "
                        + "teamId=team_1. Its value is not shown.");
            }

            if (parsedSettings.containsKey(key)) {
                throw new CliException(1, "--setting gives '" + key + "' more than once; give each key once.");
            }

            parsedSettings.put(key, setting.substring(separatorIndex + 1));
        }

        return parsedSettings;
    }

    private static DeploymentSettingsResult toDeploymentSettingsResult(AppInstanceModel appInstanceModel) {
        Map<String, Object> deploymentSettings = appInstanceModel.getDeploymentSettings();

        return new DeploymentSettingsResult(
            appInstanceModel.getId(), AppCommandSupport.valueOf(appInstanceModel.getHostingTarget()),
            appInstanceModel.getDeploymentConnectionId(),
            deploymentSettings == null ? Map.of() : deploymentSettings);
    }

    private static String cell(Object value) {
        return value == null ? "" : value.toString();
    }

    public record AppInstanceCreatedResult(
        long id, long instanceId, String uuid, String name, long workspaceId, String environment,
        String userAuthMode, String envFile) {
    }

    public record DeploymentSettingsResult(
        Long instanceId, String hostingTarget, Long connectionId, Map<String, Object> settings) {

        public DeploymentSettingsResult {
            settings = Collections.unmodifiableMap(new LinkedHashMap<>(settings));
        }
    }

    public record AppInstanceSummary(
        Long id, String name, String environment, Long workspaceId, String status, String userAuthMode, String url) {
    }
}
