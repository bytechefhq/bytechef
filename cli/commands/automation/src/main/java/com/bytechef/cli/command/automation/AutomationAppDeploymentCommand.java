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
import com.bytechef.cli.client.automationapp.api.AppDeploymentApi;
import com.bytechef.cli.client.automationapp.model.AppDeploymentModel;
import com.bytechef.cli.client.automationapp.model.AppDeploymentRecordInputModel;
import com.bytechef.cli.client.automationapp.model.AppDeploymentStatusModel;
import com.bytechef.cli.client.automationapp.model.AppDeploymentTriggerInputModel;
import com.bytechef.cli.client.automationapp.model.AppVersionInputModel;
import com.bytechef.cli.client.automationapp.model.AppVersionModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Deploys, rolls back, cancels and records the deployments of an app instance and creates the versions of an app, the
 * commands a CI job runs. They are the only app commands that change something in every environment, not only in
 * DEVELOPMENT: a version is a label for code ByteChef does not hold, a deployment ships that code through the
 * instance's hosting provider, and a recorded deployment is history. None of them writes a file.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationAppDeploymentCommand {

    static final String DEPLOY_PERMISSION = "APP_DEPLOY";
    static final String VIEW_PERMISSION = "APP_VIEW";

    private static final int DEFAULT_TIMEOUT_SECONDS = 600;
    private static final Set<AppDeploymentStatusModel> IN_PROGRESS_STATUSES = Set.of(
        AppDeploymentStatusModel.QUEUED, AppDeploymentStatusModel.BUILDING);
    private static final Set<AppDeploymentStatusModel> UNSUCCESSFUL_STATUSES = Set.of(
        AppDeploymentStatusModel.FAILED, AppDeploymentStatusModel.CANCELED);
    private static final String VERSION_NAME_ALREADY_EXISTS = "APP_VERSION_NAME_ALREADY_EXISTS";

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private Path workingDirectory = Path.of("")
        .toAbsolutePath();
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private LongSupplier nanoClock = System::nanoTime;
    private Duration pollInterval = Duration.ofSeconds(5);
    private Sleeper sleeper = Thread::sleep;

    @Command(
        name = "automation app deployment deploy",
        description = "Deploy a version of the app to an instance through its hosting provider, in any environment. "
            + "A version the app does not have is created only when --commit or --git-ref says which code it is.")
    public void deploymentDeploy(
        @Option(longName = "version", required = true) String version,
        @Option(
            longName = "commit",
            description = "The git commit of the version; creates the version when the app has none of that name.") String commit,
        @Option(
            longName = "git-ref",
            description = "The git branch or tag of the version; creates the version when the app has none of that "
                + "name.") String gitRef,
        @Option(
            longName = "wait",
            description = "Wait until the deployment is READY, FAILED or CANCELED.") boolean wait,
        @Option(
            longName = "timeout",
            description = "With --wait, the most seconds to wait. Defaults to 600.") Integer timeout,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        Duration waitTimeout = toWaitTimeout(timeout);
        CliConfig config = AppCommandSupport.resolve(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        long appInstanceId = AppCommandSupport.resolveInstanceId(
            appLocation, instanceId, config, AutomationClientFactory.appApi(config), VIEW_PERMISSION);
        AppDeploymentApi appDeploymentApi = AutomationClientFactory.appDeploymentApi(config);
        long appVersionId = resolveOrCreateVersionId(
            appDeploymentApi, appLocation.appId(), appInstanceId, version, commit, gitRef, config);
        AppDeploymentModel appDeploymentModel;

        try {
            appDeploymentModel = appDeploymentApi.deployAppInstance(
                appInstanceId, toTriggerInput(appVersionId), null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), DEPLOY_PERMISSION);
        }

        finishDeployment(
            appDeploymentApi, appInstanceId, AppCommandSupport.required(appDeploymentModel, "the deployment"), wait,
            waitTimeout, config, output);
    }

    @Command(
        name = "automation app deployment rollback",
        description = "Deploy an earlier version of the app to an instance again, in any environment. The version "
            + "must exist.")
    public void deploymentRollback(
        @Option(longName = "version", required = true) String version,
        @Option(
            longName = "wait",
            description = "Wait until the deployment is READY, FAILED or CANCELED.") boolean wait,
        @Option(
            longName = "timeout",
            description = "With --wait, the most seconds to wait. Defaults to 600.") Integer timeout,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        Duration waitTimeout = toWaitTimeout(timeout);
        CliConfig config = AppCommandSupport.resolve(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        long appInstanceId = AppCommandSupport.resolveInstanceId(
            appLocation, instanceId, config, AutomationClientFactory.appApi(config), VIEW_PERMISSION);
        AppDeploymentApi appDeploymentApi = AutomationClientFactory.appDeploymentApi(config);
        AppVersionModel appVersionModel = fetchVersion(appDeploymentApi, appInstanceId, version, config)
            .orElseThrow(() -> new CliException(
                3, "App " + appLocation.appId() + " has no version named '" + version.strip() + "'. Run 'bytechef "
                    + "automation app version list' to see the versions you can roll back to."));
        AppDeploymentModel appDeploymentModel;

        try {
            appDeploymentModel = appDeploymentApi.rollbackAppInstance(
                appInstanceId, toTriggerInput(AppCommandSupport.required(appVersionModel.getId(), "the version id")),
                null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), DEPLOY_PERMISSION);
        }

        finishDeployment(
            appDeploymentApi, appInstanceId, AppCommandSupport.required(appDeploymentModel, "the deployment"), wait,
            waitTimeout, config, output);
    }

    @Command(
        name = "automation app deployment cancel",
        description = "Cancel a deployment of an instance that is still in progress, in any environment. Without "
            + "--deployment-id it cancels the instance's one deployment in progress.")
    public void deploymentCancel(
        @Option(longName = "deployment-id") Long deploymentId,
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
            appLocation, instanceId, config, AutomationClientFactory.appApi(config), VIEW_PERMISSION);
        AppDeploymentApi appDeploymentApi = AutomationClientFactory.appDeploymentApi(config);
        long canceledDeploymentId = deploymentId == null
            ? findDeploymentInProgress(appDeploymentApi, appInstanceId, config) : deploymentId;
        AppDeploymentModel appDeploymentModel;

        try {
            appDeploymentModel = appDeploymentApi.cancelAppInstanceDeployment(
                appInstanceId, canceledDeploymentId, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), DEPLOY_PERMISSION);
        }

        new OutputRenderer(out).render(
            toDeploymentSummary(AppCommandSupport.required(appDeploymentModel, "the canceled deployment")), output);
    }

    @Command(
        name = "automation app deployment record",
        description = "Record that a version of the app was deployed to an instance, in any environment. The version "
            + "is created when the app has none of that name.")
    public void deploymentRecord(
        @Option(longName = "version", required = true) String version,
        @Option(
            longName = "url", description = "Where the deployed version is served, an http or https URL.") String url,
        @Option(longName = "commit", description = "The git commit of the deployed version.") String commit,
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
            appLocation, instanceId, config, AutomationClientFactory.appApi(config), VIEW_PERMISSION);
        AppDeploymentRecordInputModel appDeploymentRecordInputModel = new AppDeploymentRecordInputModel();

        appDeploymentRecordInputModel.setVersionName(version);
        appDeploymentRecordInputModel.setUrl(url);
        appDeploymentRecordInputModel.setGitCommitSha(commit);

        AppDeploymentApi appDeploymentApi = AutomationClientFactory.appDeploymentApi(config);
        AppDeploymentModel appDeploymentModel;

        try {
            appDeploymentModel = recordDeployment(appDeploymentApi, appInstanceId, appDeploymentRecordInputModel);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), DEPLOY_PERMISSION);
        }

        new OutputRenderer(out).render(
            toDeploymentSummary(AppCommandSupport.required(appDeploymentModel, "the recorded deployment")), output);
    }

    @Command(
        name = "automation app deployment list",
        description = "List the newest recorded deployments of an app instance, newest first.")
    public void deploymentList(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(
            longName = "limit",
            description = "The most to list, newest first, from 1 to 200. Defaults to 50.") Integer limit,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolve(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        long appInstanceId = AppCommandSupport.resolveInstanceId(
            appLocation, instanceId, config, AutomationClientFactory.appApi(config), VIEW_PERMISSION);
        List<AppDeploymentModel> appDeploymentModels;

        try {
            appDeploymentModels = AppCommandSupport.required(
                AutomationClientFactory.appDeploymentApi(config)
                    .getAppInstanceDeployments(appInstanceId, null, limit),
                "the deployments of app instance " + appInstanceId);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), VIEW_PERMISSION);
        }

        new OutputRenderer(out).render(
            appDeploymentModels.stream()
                .map(AutomationAppDeploymentCommand::toDeploymentSummary)
                .toList(),
            output);
    }

    @Command(
        name = "automation app version create",
        description = "Create a version of the app, in any environment: a name for code ByteChef does not hold.")
    public void versionCreate(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "commit", description = "The git commit of the version.") String commit,
        @Option(longName = "git-ref", description = "The git branch or tag of the version.") String gitRef,
        @Option(longName = "description") String description,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolve(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppVersionInputModel appVersionInputModel = new AppVersionInputModel();

        appVersionInputModel.setName(name);
        appVersionInputModel.setGitCommitSha(commit);
        appVersionInputModel.setGitRef(gitRef);
        appVersionInputModel.setDescription(description);

        AppVersionModel appVersionModel;

        try {
            appVersionModel = AutomationClientFactory.appDeploymentApi(config)
                .createAppVersion(appLocation.appId(), appVersionInputModel, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), DEPLOY_PERMISSION);
        }

        new OutputRenderer(out).render(
            toVersionSummary(AppCommandSupport.required(appVersionModel, "the created version")), output);
    }

    @Command(
        name = "automation app version list",
        description = "List the newest versions of the app, newest first. With --instance-id, or in a directory "
            + "linked to an instance, the versions are read through that instance, which also works for an instance "
            + "your workspace hosts of an app another workspace owns.")
    public void versionList(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "instance-id") Long instanceId,
        @Option(
            longName = "limit",
            description = "The most to list, newest first, from 1 to 200. Defaults to 50.") Integer limit,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolve(
            configPath, environmentVariables, profile, host, token, environment, null);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        Long appInstanceId = instanceId == null ? appLocation.instanceId() : instanceId;
        AppDeploymentApi appDeploymentApi = AutomationClientFactory.appDeploymentApi(config);
        List<AppVersionModel> appVersionModels;

        try {
            if (appInstanceId == null) {
                appVersionModels = AppCommandSupport.required(
                    appDeploymentApi.getAppVersions(appLocation.appId(), null, limit, null),
                    "the versions of app " + appLocation.appId());
            } else {
                appVersionModels = AppCommandSupport.required(
                    appDeploymentApi.getAppInstanceVersions(appInstanceId, null, limit, null),
                    "the versions of the app of instance " + appInstanceId);
            }
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), VIEW_PERMISSION);
        }

        new OutputRenderer(out).render(
            appVersionModels.stream()
                .map(AutomationAppDeploymentCommand::toVersionSummary)
                .toList(),
            output);
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

    void setErrorOutput(PrintStream err) {
        this.err = err;
    }

    void setNanoClock(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    private void finishDeployment(
        AppDeploymentApi appDeploymentApi, long appInstanceId, AppDeploymentModel appDeploymentModel, boolean wait,
        Duration waitTimeout, CliConfig config, String output) {

        AppDeploymentModel finishedDeploymentModel = wait
            ? waitForDeployment(appDeploymentApi, appInstanceId, appDeploymentModel, waitTimeout, config, output)
            : appDeploymentModel;

        new OutputRenderer(out).render(toDeploymentSummary(finishedDeploymentModel), output);

        AppDeploymentStatusModel status = finishedDeploymentModel.getStatus();

        if (UNSUCCESSFUL_STATUSES.contains(status)) {
            String errorMessage = finishedDeploymentModel.getErrorMessage();

            throw new CliException(
                1, "Deployment " + finishedDeploymentModel.getId() + " ended " + AppCommandSupport.valueOf(status)
                    + (errorMessage == null || errorMessage.isBlank() ? "" : ": " + errorMessage) + ".");
        }
    }

    private AppDeploymentModel waitForDeployment(
        AppDeploymentApi appDeploymentApi, long appInstanceId, AppDeploymentModel appDeploymentModel,
        Duration waitTimeout, CliConfig config, String output) {

        long deploymentId = AppCommandSupport.required(appDeploymentModel.getId(), "the deployment id");
        long deadline = nanoClock.getAsLong() + waitTimeout.toNanos();
        AppDeploymentModel currentDeploymentModel = appDeploymentModel;
        ApiException lastTransientException = null;

        err.println(
            "Deployment " + deploymentId + " is " + AppCommandSupport.valueOf(appDeploymentModel.getStatus())
                + "; waiting up to " + describeSeconds(waitTimeout) + " for it to finish.");

        while (IN_PROGRESS_STATUSES.contains(currentDeploymentModel.getStatus())) {
            try {
                currentDeploymentModel = AppCommandSupport.required(
                    appDeploymentApi.getAppInstanceDeployment(appInstanceId, deploymentId, null),
                    "deployment " + deploymentId);
                lastTransientException = null;
            } catch (ApiException exception) {
                if (!isTransient(exception)) {
                    CliException cliException = AutomationClientFactory.toCliException(
                        exception, config.environment(), VIEW_PERMISSION);

                    throw stillRunning(
                        cliException.exitCode(), currentDeploymentModel, output,
                        "; reading it failed: " + withoutTrailingPeriod(cliException.getMessage()));
                }

                lastTransientException = exception;
            }

            if (!IN_PROGRESS_STATUSES.contains(currentDeploymentModel.getStatus())) {
                break;
            }

            long remainingNanos = deadline - nanoClock.getAsLong();

            if (remainingNanos <= 0) {
                String lastFailure = lastTransientException == null
                    ? "" : " (the last poll failed with HTTP " + lastTransientException.getCode() + ")";

                throw stillRunning(
                    1, currentDeploymentModel, output, " after " + describeSeconds(waitTimeout) + lastFailure);
            }

            try {
                sleeper.sleep(pollInterval.compareTo(Duration.ofNanos(remainingNanos)) < 0
                    ? pollInterval : Duration.ofNanos(remainingNanos));
            } catch (InterruptedException exception) {
                Thread.currentThread()
                    .interrupt();

                throw stillRunning(1, currentDeploymentModel, output, "; the wait was interrupted");
            }
        }

        return currentDeploymentModel;
    }

    private CliException stillRunning(
        int exitCode, AppDeploymentModel lastKnownDeploymentModel, String output, String reason) {

        new OutputRenderer(out).render(toDeploymentSummary(lastKnownDeploymentModel), output);

        Long deploymentId = lastKnownDeploymentModel.getId();

        return new CliException(
            exitCode, "Deployment " + deploymentId + " was last seen "
                + AppCommandSupport.valueOf(lastKnownDeploymentModel.getStatus()) + reason + ". It keeps running on "
                + "the server: run 'bytechef automation app deployment list' to follow it or 'bytechef automation app "
                + "deployment cancel --deployment-id " + deploymentId + "' to stop it.");
    }

    private static String withoutTrailingPeriod(String message) {
        return message.endsWith(".") ? message.substring(0, message.length() - 1) : message;
    }

    private static boolean isTransient(ApiException exception) {
        int code = exception.getCode();

        return code == 0 || code == 408 || code == 429 || code >= 500;
    }

    private static String describeSeconds(Duration duration) {
        long seconds = duration.toSeconds();

        return seconds + (seconds == 1 ? " second" : " seconds");
    }

    private static Duration toWaitTimeout(Integer timeout) {
        if (timeout == null) {
            return Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);
        }

        if (timeout < 1) {
            throw new CliException(1, "--timeout must be at least 1 second, not " + timeout + ".");
        }

        return Duration.ofSeconds(timeout);
    }

    private static long resolveOrCreateVersionId(
        AppDeploymentApi appDeploymentApi, long appId, long appInstanceId, String version, String commit,
        String gitRef, CliConfig config) {

        Optional<AppVersionModel> existingVersion = fetchVersion(appDeploymentApi, appInstanceId, version, config);
        boolean hasCommit = commit != null && !commit.isBlank();

        if (existingVersion.isPresent()) {
            AppVersionModel appVersionModel = existingVersion.get();
            String existingCommit = appVersionModel.getGitCommitSha();

            if (hasCommit && existingCommit != null && !existingCommit.equalsIgnoreCase(commit.strip())) {
                throw new CliException(
                    1, "Version '" + appVersionModel.getName() + "' of app " + appId + " is commit " + existingCommit
                        + ", not " + commit.strip() + ". Deploy it without --commit or name a new version.");
            }

            return AppCommandSupport.required(appVersionModel.getId(), "the version id");
        }

        boolean hasGitRef = gitRef != null && !gitRef.isBlank();

        if (!hasCommit && !hasGitRef) {
            throw new CliException(
                3, "App " + appId + " has no version named '" + version.strip() + "'. Pass --commit or --git-ref to "
                    + "create it, or run 'bytechef automation app version create --name " + version.strip() + "'.");
        }

        AppVersionInputModel appVersionInputModel = new AppVersionInputModel();

        appVersionInputModel.setName(version);
        appVersionInputModel.setGitCommitSha(hasCommit ? commit.strip() : null);
        appVersionInputModel.setGitRef(hasGitRef ? gitRef.strip() : null);

        try {
            AppVersionModel createdVersionModel = appDeploymentApi.createAppVersion(appId, appVersionInputModel, null);

            return AppCommandSupport.required(
                AppCommandSupport.required(createdVersionModel, "the created version")
                    .getId(),
                "the created version id");
        } catch (ApiException exception) {
            if (!isVersionNameRace(exception)) {
                throw AutomationClientFactory.toCliException(exception, config.environment(), DEPLOY_PERMISSION);
            }
        }

        AppVersionModel racedVersionModel = fetchVersion(appDeploymentApi, appInstanceId, version, config)
            .orElseThrow(() -> new CliException(
                1, "Version '" + version.strip() + "' of app " + appId + " was created by someone else and could "
                    + "not be read back. Retry the deploy."));

        return AppCommandSupport.required(racedVersionModel.getId(), "the version id");
    }

    private static Optional<AppVersionModel> fetchVersion(
        AppDeploymentApi appDeploymentApi, long appInstanceId, String version, CliConfig config) {

        if (version == null || version.isBlank()) {
            throw new CliException(1, "--version must not be blank.");
        }

        List<AppVersionModel> appVersionModels;

        try {
            appVersionModels = appDeploymentApi.getAppInstanceVersions(appInstanceId, null, null, version.strip());
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), VIEW_PERMISSION);
        }

        if (appVersionModels == null) {
            return Optional.empty();
        }

        return appVersionModels.stream()
            .filter(appVersionModel -> version.strip()
                .equals(appVersionModel.getName()))
            .findFirst();
    }

    private static long findDeploymentInProgress(
        AppDeploymentApi appDeploymentApi, long appInstanceId, CliConfig config) {

        List<AppDeploymentModel> appDeploymentModels;

        try {
            appDeploymentModels = AppCommandSupport.required(
                appDeploymentApi.getAppInstanceDeployments(appInstanceId, null, null),
                "the deployments of app instance " + appInstanceId);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception, config.environment(), VIEW_PERMISSION);
        }

        List<Long> inProgressDeploymentIds = appDeploymentModels.stream()
            .filter(appDeploymentModel -> IN_PROGRESS_STATUSES.contains(appDeploymentModel.getStatus()))
            .map(AppDeploymentModel::getId)
            .toList();

        if (inProgressDeploymentIds.isEmpty()) {
            throw new CliException(3, "App instance " + appInstanceId + " has no deployment in progress to cancel.");
        }

        if (inProgressDeploymentIds.size() > 1) {
            throw new CliException(
                1, "App instance " + appInstanceId + " has several deployments in progress ("
                    + inProgressDeploymentIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "))
                    + "). Pass --deployment-id to say which one to cancel.");
        }

        return inProgressDeploymentIds.getFirst();
    }

    private static AppDeploymentTriggerInputModel toTriggerInput(long appVersionId) {
        AppDeploymentTriggerInputModel appDeploymentTriggerInputModel = new AppDeploymentTriggerInputModel();

        appDeploymentTriggerInputModel.setAppVersionId(appVersionId);

        return appDeploymentTriggerInputModel;
    }

    private static AppDeploymentModel recordDeployment(
        AppDeploymentApi appDeploymentApi, long appInstanceId,
        AppDeploymentRecordInputModel appDeploymentRecordInputModel) throws ApiException {

        try {
            return appDeploymentApi.recordAppInstanceDeployment(appInstanceId, appDeploymentRecordInputModel, null);
        } catch (ApiException exception) {
            if (!isVersionNameRace(exception)) {
                throw exception;
            }

            return appDeploymentApi.recordAppInstanceDeployment(appInstanceId, appDeploymentRecordInputModel, null);
        }
    }

    private static boolean isVersionNameRace(ApiException exception) {
        String responseBody = exception.getResponseBody();

        return exception.getCode() == 409 && responseBody != null && responseBody.contains(VERSION_NAME_ALREADY_EXISTS);
    }

    private static DeploymentSummary toDeploymentSummary(AppDeploymentModel appDeploymentModel) {
        AppVersionModel appVersionModel = appDeploymentModel.getAppVersion();

        return new DeploymentSummary(
            appDeploymentModel.getId(), appDeploymentModel.getAppInstanceId(),
            appVersionModel == null ? null : appVersionModel.getName(),
            appVersionModel == null ? null : appVersionModel.getGitCommitSha(),
            AppCommandSupport.valueOf(appDeploymentModel.getStatus()),
            AppCommandSupport.valueOf(appDeploymentModel.getProvider()), appDeploymentModel.getUrl(),
            appDeploymentModel.getErrorMessage(), appDeploymentModel.getCurrent(), appDeploymentModel.getCreatedBy(),
            AppCommandSupport.valueOf(appDeploymentModel.getCreatedDate()),
            AppCommandSupport.valueOf(appDeploymentModel.getFinishedDate()));
    }

    private static VersionSummary toVersionSummary(AppVersionModel appVersionModel) {
        return new VersionSummary(
            appVersionModel.getId(), appVersionModel.getAppId(), appVersionModel.getName(),
            appVersionModel.getGitRef(), appVersionModel.getGitCommitSha(), appVersionModel.getDescription(),
            appVersionModel.getCreatedBy(), AppCommandSupport.valueOf(appVersionModel.getCreatedDate()));
    }

    @FunctionalInterface
    interface Sleeper {

        void sleep(Duration duration) throws InterruptedException;
    }

    public record DeploymentSummary(
        Long id, Long instanceId, String version, String commit, String status, String provider, String url,
        String errorMessage, Boolean current, String createdBy, String createdDate, String finishedDate) {
    }

    public record VersionSummary(
        Long id, Long appId, String name, String gitRef, String commit, String description, String createdBy,
        String createdDate) {
    }
}
