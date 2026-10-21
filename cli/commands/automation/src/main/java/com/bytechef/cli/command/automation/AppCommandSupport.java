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
import com.bytechef.cli.client.automationapp.model.AppHostingTargetModel;
import com.bytechef.cli.client.automationapp.model.AppInstanceModel;
import com.bytechef.cli.client.automationapp.model.AppModel;
import com.bytechef.cli.client.automationapp.model.AppUserAuthModeModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.error.CliException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * What every app command shares: the profile resolution (DEVELOPMENT-only for commands that change an app), finding the
 * app instance through {@code .bytechef/app.json}, and locating the env file in the app directory.
 *
 * @author Ivica Cardic
 */
final class AppCommandSupport {

    private AppCommandSupport() {
    }

    static CliConfig resolve(
        Path configPath, Map<String, String> environmentVariables, String profile, String host, String token,
        String environment, Long workspaceId) {

        Environment environmentOverride = parseEnvironment(environment);
        ProfileResolver profileResolver = new ProfileResolver(configPath, environmentVariables);

        return profileResolver.resolve(new Overrides(host, token, environmentOverride, workspaceId, profile));
    }

    static CliConfig resolveDevelopment(
        Path configPath, Map<String, String> environmentVariables, String profile, String host, String token,
        String environment, Long workspaceId) {

        CliConfig config = resolve(configPath, environmentVariables, profile, host, token, environment, workspaceId);

        if (config.environment() != Environment.DEVELOPMENT) {
            throw new CliException(
                1, "This command changes an app and runs in DEVELOPMENT only, but the resolved environment is "
                    + config.environment()
                    + ". Use a DEVELOPMENT profile with an automation API key created in DEVELOPMENT (bytechef "
                    + "configure --profile dev --environment DEVELOPMENT --token <key> --host <url> --workspace-id "
                    + "<id>), then pass --profile dev.");
        }

        return config;
    }

    static long requireWorkspaceId(CliConfig config) {
        Long workspaceId = config.workspaceId();

        if (workspaceId == null) {
            throw new CliException(
                4, "No workspace configured. Pass --workspace-id or store one with 'bytechef configure "
                    + "--workspace-id <id>'.");
        }

        return workspaceId;
    }

    static void refuseExistingAppLink(Path workingDirectory) {
        Optional<Path> appDirectory = AppLinkFile.findAppDirectory(workingDirectory);

        if (appDirectory.isPresent()) {
            throw new CliException(
                1, AppLinkFile.pathIn(appDirectory.get()) + " already links " + appDirectory.get() + " to an app. "
                    + "One directory holds one app: run 'bytechef automation app status' to see it, or run this "
                    + "command in a directory outside it.");
        }
    }

    static void requireEnvFileForAnotherInstance(AppLocation appLocation, Long requestedInstanceId) {
        if (!appLocation.writesAKeyOtherThanTheLinkedOne(requestedInstanceId)) {
            return;
        }

        String target = requestedInstanceId == null
            ? "an instance chosen from the profile" : "instance " + requestedInstanceId;

        throw new CliException(
            1, "This directory is linked to app " + appLocation.linkedAppId() + " instance "
                + appLocation.linkedInstanceId() + ", whose key the default env file holds, but this command writes "
                + "the key of app " + appLocation.appId() + " " + target + ". Pass --env-file with the file that "
                + "should receive it, or run the command outside the linked directory.");
    }

    static AppLocation locateApp(Path workingDirectory, Long appId, CliConfig config) {
        Optional<Path> appDirectory = AppLinkFile.findAppDirectory(workingDirectory);

        if (appDirectory.isEmpty()) {
            if (appId == null) {
                throw new CliException(
                    1, "No .bytechef/app.json in " + workingDirectory + " or any parent directory. Pass --app-id, "
                        + "or run 'bytechef automation app link --app-id <id>' in the app's directory.");
            }

            return new AppLocation(appId, workingDirectory, null, null, null);
        }

        AppLink appLink = readAppLink(appDirectory.get());

        if (appId != null && appId.longValue() != appLink.id()) {
            return new AppLocation(appId, workingDirectory, appLink.id(), null, appLink.instanceId());
        }

        List<String> differences = profileDifferences(appLink, config);

        if (!differences.isEmpty()) {
            if (appId != null) {
                return new AppLocation(appId, workingDirectory, appLink.id(), null, appLink.instanceId());
            }

            throw new CliException(
                1, AppLinkFile.pathIn(appDirectory.get()) + " was written for another ByteChef target: "
                    + String.join(", ", differences) + ". Use the profile the app was created with, or pass "
                    + "--app-id.");
        }

        return new AppLocation(
            appLink.id(), appDirectory.get(), appLink.id(), appLink.instanceId(), appLink.instanceId());
    }

    /**
     * The app instance a command addresses. {@code requestedInstanceId} is {@code --instance-id}. A directory linked to
     * the app answers with the instance in its {@code .bytechef/app.json} without a request, unless
     * {@code --instance-id} names another one. Otherwise the app named by {@code --app-id} is fetched first: the
     * requested instance must be one of its instances in the profile's environment, and without {@code --instance-id}
     * the app must have exactly one instance in the profile's environment and workspace. When the app may not be read
     * (403) and {@code --instance-id} is given, as for an instance the profile's workspace hosts of an app another
     * workspace owns, the instance itself is read instead and must belong to the app and the profile's environment.
     * Every command that takes an app calls this, the read-only {@code status} included, before any other request.
     */
    static long resolveInstanceId(
        AppLocation appLocation, Long requestedInstanceId, CliConfig config, AppApi appApi) {

        return resolveInstanceId(appLocation, requestedInstanceId, config, appApi, null);
    }

    /**
     * Like {@link #resolveInstanceId(AppLocation, Long, CliConfig, AppApi)}, for a command allowed in every
     * environment: a failure to read the app names the profile's environment and the permission reading it needs.
     */
    static long resolveInstanceId(
        AppLocation appLocation, Long requestedInstanceId, CliConfig config, AppApi appApi,
        String requiredPermission) {

        if (appLocation.isLinked() && !appLocation.overridesLinkedInstance(requestedInstanceId)) {
            return appLocation.instanceId();
        }

        long appId = appLocation.appId();
        AppModel appModel;

        try {
            appModel = required(appApi.getApp(appId, null), "app " + appId);
        } catch (ApiException exception) {
            if (requestedInstanceId != null && exception.getCode() == 403) {
                Optional<Long> hostedInstanceId = fetchHostedInstanceId(appId, requestedInstanceId, config);

                if (hostedInstanceId.isPresent()) {
                    return hostedInstanceId.get();
                }
            }

            if (requiredPermission == null) {
                throw AutomationClientFactory.toCliException(exception);
            }

            throw AutomationClientFactory.toCliException(exception, config.environment(), requiredPermission);
        }

        AppInstanceModel appInstanceModel = chooseInstance(appModel, appId, requestedInstanceId, config);

        return required(appInstanceModel.getId(), "the id of the instance of app " + appId);
    }

    private static Optional<Long> fetchHostedInstanceId(long appId, long requestedInstanceId, CliConfig config) {
        AppInstanceModel appInstanceModel;

        try {
            appInstanceModel = AutomationClientFactory.appInstanceApi(config)
                .getAppInstance(requestedInstanceId, null);
        } catch (ApiException exception) {
            return Optional.empty();
        }

        if (appInstanceModel == null) {
            return Optional.empty();
        }

        Environment environment = config.environment();

        if (!Long.valueOf(appId)
            .equals(appInstanceModel.getAppId()) ||
            !environment.name()
                .equals(valueOf(appInstanceModel.getEnvironment()))) {

            throw new CliException(
                1, "App " + appId + " has no instance " + requestedInstanceId + " in " + environment + ".");
        }

        return Optional.of(required(appInstanceModel.getId(), "the id of the instance of app " + appId));
    }

    static AppInstanceModel chooseInstance(
        AppModel appModel, long appId, Long requestedInstanceId, CliConfig config) {

        if (requestedInstanceId != null) {
            return requireInstance(appModel, appId, requestedInstanceId, config);
        }

        Environment environment = config.environment();
        Long configWorkspaceId = config.workspaceId();
        List<AppInstanceModel> candidates = new ArrayList<>();

        for (AppInstanceModel appInstanceModel : instancesIn(appModel, environment)) {
            if (configWorkspaceId != null && !configWorkspaceId.equals(appInstanceModel.getWorkspaceId())) {
                continue;
            }

            candidates.add(appInstanceModel);
        }

        String scope = environment + (configWorkspaceId == null ? "" : " in workspace " + configWorkspaceId);

        if (candidates.isEmpty()) {
            throw new CliException(1, "App " + appId + " has no instance in " + scope + ".");
        }

        if (candidates.size() > 1) {
            throw new CliException(
                1, "App " + appId + " has several instances in " + scope + " (" + describeInstances(candidates)
                    + "). " + severalInstancesRemedy(appId, environment));
        }

        return candidates.getFirst();
    }

    static AppHostingTargetModel parseHostingTarget(String hostingTarget) {
        try {
            return AppHostingTargetModel.fromValue(hostingTarget);
        } catch (IllegalArgumentException exception) {
            throw new CliException(1, "Unknown --hosting-target '" + hostingTarget + "'. Use VERCEL or SELF_HOSTED.");
        }
    }

    static AppUserAuthModeModel parseUserAuthMode(String userAuthMode) {
        if (userAuthMode == null) {
            return null;
        }

        try {
            return AppUserAuthModeModel.fromValue(
                userAuthMode.strip()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CliException(
                1, "Unknown --user-auth-mode '" + userAuthMode + "'. Use NONE, OPTIONAL or REQUIRED.");
        }
    }

    static CliException toCreateException(ApiException exception) {
        CliException cliException = AutomationClientFactory.toCliException(
            exception, AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);
        String responseBody = exception.getResponseBody();

        if (exception.getCode() == 409 && responseBody != null && responseBody.contains("APP_USER_AUTH_UNAVAILABLE")) {
            return new CliException(
                cliException.exitCode(),
                cliException.getMessage() + " To create it without sign-in, pass --user-auth-mode NONE.");
        }

        return cliException;
    }

    static AutomationAppInstanceCommand.AppInstanceSummary toInstanceSummary(AppInstanceModel appInstanceModel) {
        return new AutomationAppInstanceCommand.AppInstanceSummary(
            appInstanceModel.getId(), appInstanceModel.getName(), valueOf(appInstanceModel.getEnvironment()),
            appInstanceModel.getWorkspaceId(), valueOf(appInstanceModel.getStatus()),
            valueOf(appInstanceModel.getUserAuthMode()), appInstanceModel.getUrl());
    }

    static List<AutomationAppInstanceCommand.AppInstanceSummary> toInstanceSummaries(
        List<AppInstanceModel> appInstanceModels) {

        return nullToEmptyInstances(appInstanceModels).stream()
            .map(AppCommandSupport::toInstanceSummary)
            .toList();
    }

    @SuppressFBWarnings(
        value = {
            "PATH_TRAVERSAL_IN", "PATH_TRAVERSAL_OUT"
        }, justification = "The env file path is chosen by the CLI user on purpose.")
    static Path resolveEnvFile(Path appDirectory, String envFile) {
        Path resolvedPath = appDirectory.resolve(envFile);

        Path absolutePath = resolvedPath.toAbsolutePath();

        Path envFilePath = absolutePath.normalize();
        Path directory = envFilePath.getParent();

        if (Files.isSymbolicLink(envFilePath)) {
            throw new CliException(
                1, "The env file " + envFilePath + " is a symbolic link, which the CLI will not write through. Point "
                    + "--env-file at the real file or replace the link with a regular file.");
        }

        if (directory == null || !Files.isDirectory(directory)) {
            throw new CliException(
                1, "The directory of the env file " + envFilePath + " does not exist. Create it, or pass --env-file "
                    + "with a path in an existing directory.");
        }

        if (Files.isDirectory(envFilePath)) {
            throw new CliException(
                1, "The env file " + envFilePath + " is a directory. Pass --env-file with the path of a file.");
        }

        if (!Files.isWritable(directory) || Files.exists(envFilePath) && !Files.isWritable(envFilePath)) {
            throw new CliException(
                1, "The env file " + envFilePath + " cannot be written: check the permissions of the file and of its "
                    + "directory, or pass --env-file with a path you can write.");
        }

        return envFilePath;
    }

    static void warnWhenCreatedWithoutSignIn(
        String requestedUserAuthMode, AppUserAuthModeModel createdUserAuthMode, PrintStream err) {

        if (requestedUserAuthMode == null && createdUserAuthMode == AppUserAuthModeModel.NONE) {
            err.println(
                "Warning: The instance has no end-user sign-in because this ByteChef server has no authorization "
                    + "server enabled; anyone with the app's URL can open it.");
        }
    }

    static void warnWhenNotGitIgnored(Path envFile, PrintStream err) {
        if (EnvFile.gitIgnoreStatus(envFile) == EnvFile.GitIgnoreStatus.NOT_IGNORED) {
            err.println(
                "Warning: " + envFile + " is not git-ignored. Add it to .gitignore so the app key is never "
                    + "committed.");
        }
    }

    static String normalizeHost(String host) {
        String normalizedHost = host == null ? "" : host.strip();

        while (normalizedHost.endsWith("/")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }

        return normalizedHost;
    }

    static <T> T required(T value, String description) {
        if (value == null) {
            throw new CliException(1, "ByteChef returned an empty response for " + description + ".");
        }

        return value;
    }

    static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }

    static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static List<AppInstanceModel> nullToEmptyInstances(List<AppInstanceModel> appInstanceModels) {
        return appInstanceModels == null ? List.of() : appInstanceModels;
    }

    private static String describeInstances(List<AppInstanceModel> candidates) {
        List<String> descriptions = new ArrayList<>();

        for (AppInstanceModel candidate : candidates) {
            descriptions.add("instance " + candidate.getId() + " " + candidate.getName());
        }

        return String.join(", ", descriptions);
    }

    private static Environment parseEnvironment(String environment) {
        if (environment == null) {
            return null;
        }

        try {
            return Environment.valueOf(environment);
        } catch (IllegalArgumentException exception) {
            throw new CliException(
                1, "Unknown environment '" + environment + "'. Use DEVELOPMENT, STAGING or PRODUCTION.");
        }
    }

    private static AppLink readAppLink(Path appDirectory) {
        try {
            return AppLinkFile.read(appDirectory);
        } catch (IOException exception) {
            throw new CliException(1, exception.getMessage());
        }
    }

    private static List<String> profileDifferences(AppLink appLink, CliConfig config) {
        List<String> differences = new ArrayList<>();

        if (!normalizeHost(appLink.host()).equals(normalizeHost(config.host()))) {
            differences.add("host " + appLink.host() + " (profile: " + config.host() + ")");
        }

        Environment environment = config.environment();

        if (!environment.name()
            .equals(appLink.environment())) {

            differences.add("environment " + appLink.environment() + " (profile: " + environment + ")");
        }

        Long workspaceId = config.workspaceId();

        if (workspaceId != null && workspaceId.longValue() != appLink.workspaceId()) {
            differences.add("workspace " + appLink.workspaceId() + " (profile: " + workspaceId + ")");
        }

        return differences;
    }

    private static List<AppInstanceModel> instancesIn(AppModel appModel, Environment environment) {
        List<AppInstanceModel> instances = new ArrayList<>();

        for (AppInstanceModel appInstanceModel : nullToEmptyInstances(appModel.getInstances())) {
            if (environment.name()
                .equals(valueOf(appInstanceModel.getEnvironment()))) {

                instances.add(appInstanceModel);
            }
        }

        return instances;
    }

    private static AppInstanceModel requireInstance(
        AppModel appModel, long appId, long requestedInstanceId, CliConfig config) {

        Environment environment = config.environment();
        List<AppInstanceModel> instances = instancesIn(appModel, environment);

        for (AppInstanceModel appInstanceModel : instances) {
            if (Long.valueOf(requestedInstanceId)
                .equals(appInstanceModel.getId())) {

                return appInstanceModel;
            }
        }

        String known = instances.isEmpty() ? "" : " Its instances in " + environment + ": "
            + describeInstances(instances) + ".";

        throw new CliException(
            1, "App " + appId + " has no instance " + requestedInstanceId + " in " + environment + "." + known);
    }

    private static String severalInstancesRemedy(long appId, Environment environment) {
        if (environment == Environment.DEVELOPMENT) {
            return "Pass --instance-id with one of them, or link this directory to one with 'bytechef automation app "
                + "link --app-id " + appId + " --instance-id <id>'.";
        }

        return "Pass --instance-id with one of them; linking a directory is DEVELOPMENT only.";
    }

    /**
     * Where the app was found. {@code linkedAppId} is the id in the {@code .bytechef/app.json} above the working
     * directory, or null when there is none; it differs from {@code appId} when {@code --app-id} names another app.
     * {@code instanceId} is the instance of that file, set only when the file's app is the one addressed.
     */
    record AppLocation(long appId, Path appDirectory, Long linkedAppId, Long instanceId, Long linkedInstanceId) {

        boolean isLinked() {
            return linkedAppId != null && linkedAppId.longValue() == appId && instanceId != null;
        }

        boolean overridesLinkedApp() {
            return linkedAppId != null && linkedAppId.longValue() != appId;
        }

        boolean writesAKeyOtherThanTheLinkedOne(Long requestedInstanceId) {
            return linkedAppId != null && (!isLinked() || overridesLinkedInstance(requestedInstanceId));
        }

        boolean overridesLinkedInstance(Long requestedInstanceId) {
            return isLinked() && requestedInstanceId != null && !requestedInstanceId.equals(instanceId);
        }
    }
}
