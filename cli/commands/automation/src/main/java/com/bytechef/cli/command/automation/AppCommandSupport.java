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
import com.bytechef.cli.client.automationapp.model.AppModel;
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
import java.util.Map;
import java.util.Optional;

/**
 * What every app command shares: the DEVELOPMENT-only profile resolution, finding the app through
 * {@code .bytechef/app.json}, and locating the env file in the app directory.
 *
 * @author Ivica Cardic
 */
final class AppCommandSupport {

    private AppCommandSupport() {
    }

    static CliConfig resolveDevelopment(
        Path configPath, Map<String, String> environmentVariables, String profile, String host, String token,
        String environment, Long workspaceId) {

        Environment environmentOverride = parseEnvironment(environment);
        ProfileResolver profileResolver = new ProfileResolver(configPath, environmentVariables);

        CliConfig config = profileResolver.resolve(
            new Overrides(host, token, environmentOverride, workspaceId, profile));

        if (config.environment() != Environment.DEVELOPMENT) {
            throw new CliException(
                1, "These commands run in DEVELOPMENT only, but the resolved environment is " + config.environment()
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

    static AppLocation locateApp(Path workingDirectory, Long appId, CliConfig config) {
        Optional<Path> appDirectory = AppLinkFile.findAppDirectory(workingDirectory);

        if (appDirectory.isEmpty()) {
            if (appId == null) {
                throw new CliException(
                    1, "No .bytechef/app.json in " + workingDirectory + " or any parent directory. Pass --app-id, "
                        + "or run 'bytechef automation app link --app-id <id>' in the app's directory.");
            }

            return new AppLocation(appId, workingDirectory, null);
        }

        AppLink appLink = readAppLink(appDirectory.get());

        if (appId != null && appId.longValue() != appLink.id()) {
            return new AppLocation(appId, workingDirectory, appLink.id());
        }

        checkMatchesProfile(appLink, appDirectory.get(), config);

        return new AppLocation(appLink.id(), appDirectory.get(), appLink.id());
    }

    /**
     * An app named by {@code --app-id} rather than by a matching {@code .bytechef/app.json} is fetched first and
     * refused unless it is a DEVELOPMENT app of the profile's workspace. Every command that takes an app calls this,
     * the read-only {@code status} included, before any other request. The server's by-id routes already answer
     * {@code APP_NOT_FOUND} for an app outside the request's environment; the workspace is checked only here.
     */
    static void requireDevelopmentApp(AppLocation appLocation, CliConfig config, AppApi appApi) {
        if (appLocation.isLinked()) {
            return;
        }

        long appId = appLocation.appId();
        AppModel appModel;

        try {
            appModel = required(appApi.getApp(appId, null), "app " + appId);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        checkDevelopmentApp(appModel, appId, config);
    }

    static long checkDevelopmentApp(AppModel appModel, long appId, CliConfig config) {
        String appEnvironment = valueOf(appModel.getEnvironment());

        if (!Environment.DEVELOPMENT.name()
            .equals(appEnvironment)) {

            throw new CliException(
                1, "App " + appId + " is in " + appEnvironment + "; these commands manage DEVELOPMENT apps only.");
        }

        long appWorkspaceId = required(appModel.getWorkspaceId(), "the workspace of app " + appId);
        Long configWorkspaceId = config.workspaceId();

        if (configWorkspaceId != null && configWorkspaceId.longValue() != appWorkspaceId) {
            throw new CliException(
                1, "App " + appId + " belongs to workspace " + appWorkspaceId + ", not to the profile's "
                    + "workspace " + configWorkspaceId + ".");
        }

        return appWorkspaceId;
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

    private static Environment parseEnvironment(String environment) {
        if (environment == null) {
            return null;
        }

        try {
            return Environment.valueOf(environment);
        } catch (IllegalArgumentException exception) {
            throw new CliException(
                1, "Unknown environment '" + environment + "'. These commands accept DEVELOPMENT only.");
        }
    }

    private static AppLink readAppLink(Path appDirectory) {
        try {
            return AppLinkFile.read(appDirectory);
        } catch (IOException exception) {
            throw new CliException(1, exception.getMessage());
        }
    }

    private static void checkMatchesProfile(AppLink appLink, Path appDirectory, CliConfig config) {
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

        if (!differences.isEmpty()) {
            throw new CliException(
                1, AppLinkFile.pathIn(appDirectory) + " was written for another ByteChef target: "
                    + String.join(", ", differences) + ". Use the profile the app was created with, or pass "
                    + "--app-id.");
        }
    }

    /**
     * Where the app was found. {@code linkedAppId} is the id in the {@code .bytechef/app.json} above the working
     * directory, or null when there is none; it differs from {@code appId} when {@code --app-id} names another app.
     */
    record AppLocation(long appId, Path appDirectory, Long linkedAppId) {

        boolean isLinked() {
            return linkedAppId != null && linkedAppId.longValue() == appId;
        }

        boolean overridesLinkedApp() {
            return linkedAppId != null && linkedAppId.longValue() != appId;
        }
    }
}
