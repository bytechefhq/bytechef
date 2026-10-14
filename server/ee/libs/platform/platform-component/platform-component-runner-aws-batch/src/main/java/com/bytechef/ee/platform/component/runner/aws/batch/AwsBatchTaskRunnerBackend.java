/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static com.bytechef.ee.platform.component.runner.aws.batch.AwsBatchJobFactory.MAIN_CONTAINER;
import static com.bytechef.ee.platform.component.runner.aws.batch.AwsBatchJobFactory.OUTPUT_CONTAINER;
import static com.bytechef.ee.platform.component.runner.aws.batch.AwsBatchJobFactory.WORK_DIRECTORY;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobExit;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemoteLogSink;
import com.bytechef.platform.component.runner.remote.RemoteTaskRunnerBackend;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobStatus;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.TaskContainerDetails;

/**
 * Runs a task as an AWS Batch job in the account the task's {@code aws} connection names.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class AwsBatchTaskRunnerBackend implements RemoteTaskRunnerBackend {

    static final String BUCKET_ATTRIBUTE = "bucket";
    static final String JOB_DEFINITION_ATTRIBUTE = "jobDefinitionArn";
    static final String LOG_GROUP = "/aws/batch/job";
    static final String PREFIX_ATTRIBUTE = "prefix";
    static final String TYPE = "aws-batch";

    private static final Logger log = LoggerFactory.getLogger(AwsBatchTaskRunnerBackend.class);

    private static final Duration CLIENT_GRACE = Duration.ofMinutes(10);
    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_LOG_PAGES_PER_POLL = 10;
    private static final int MAX_LOG_PAGES_TERMINAL = 1_000;
    private static final Set<JobStatus> NOT_STARTED = Set.of(
        JobStatus.SUBMITTED, JobStatus.PENDING, JobStatus.RUNNABLE, JobStatus.STARTING);
    private static final Set<JobStatus> TERMINAL = Set.of(JobStatus.SUCCEEDED, JobStatus.FAILED);

    private final AwsBatchOperationsFactory operationsFactory;
    private final Duration minimumPollInterval;

    @Autowired
    public AwsBatchTaskRunnerBackend() {
        this(SdkAwsBatchOperations::new, Duration.ofSeconds(1));
    }

    AwsBatchTaskRunnerBackend(AwsBatchOperationsFactory operationsFactory, Duration minimumPollInterval) {
        this.operationsFactory = operationsFactory;
        this.minimumPollInterval = minimumPollInterval;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTitle() {
        return "AWS Batch";
    }

    @Override
    public String getRemoteWorkingDirectory() {
        return WORK_DIRECTORY;
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties) {
        return AwsBatchRunnerSettings.properties();
    }

    @Override
    public Optional<TaskRunnerConnection> getConnection() {
        return Optional.of(new TaskRunnerConnection("aws", 1));
    }

    @Override
    public void validate(TaskRunnerRequest request, RemoteJobContext context) {
        AwsConnectionSettings connectionSettings = connectionSettings(context);

        connectionSettings.validate();
        connectionSettings.validateBudget(context.completionBudget());

        AwsBatchRunnerSettings.of(request.languageId(), request.runnerParameters());
    }

    @Override
    public Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context) {
        Parameters runnerParameters = context.runnerParameters();

        String bucket = AwsBatchRunnerSettings.bucket(runnerParameters);

        try (AwsBatchOperations operations = operationsFactory.create(
            connectionSettings(context), AwsConnectionSettings.urlValidity(context.completionBudget()))) {

            List<JobSummary> candidates = operations
                .listJobs(AwsBatchRunnerSettings.jobQueue(runnerParameters), AwsBatchJobNames.jobName(key))
                .stream()
                .filter(summary -> !TERMINAL.contains(summary.status()))
                .sorted(Comparator.comparing(JobSummary::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .reversed())
                .toList();

            for (JobSummary candidate : candidates) {
                JobDetail jobDetail = operations.describeJob(candidate.jobId());

                if (jobDetail == null || TERMINAL.contains(jobDetail.status())) {
                    continue;
                }

                Map<String, String> tags = jobDetail.tags();

                if (Objects.equals(tags.get(AwsBatchJobNames.TAG_KEY), key.value())) {
                    return Optional.of(
                        handle(jobDetail.jobId(), bucket, tags.get(AwsBatchJobNames.TAG_PREFIX),
                            jobDetail.jobDefinition()));
                }
            }

            return Optional.empty();
        } catch (SdkException exception) {
            if (isInterrupt(exception)) {
                Thread.currentThread()
                    .interrupt();

                throw new IllegalStateException(
                    "Interrupted while the aws-batch runner was looking for a job to resume", exception);
            }

            throw clientFailure("looking for a job to resume", exception);
        }
    }

    @Override
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public RemoteJobHandle submit(
        RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
        throws IOException, InterruptedException {

        AwsConnectionSettings connectionSettings = connectionSettings(context);
        AwsBatchRunnerSettings runnerSettings = AwsBatchRunnerSettings.of(spec.languageId(), spec.runnerParameters());

        Duration urlValidity = AwsConnectionSettings.urlValidity(context.completionBudget());

        RemoteJobKey key = spec.key();

        String prefix = AwsBatchJobNames.newPrefix(key);
        String bucket = runnerSettings.bucket();

        Map<String, String> tags = AwsBatchJobNames.tags(key, prefix);

        Path archivePath = Files.createTempFile("bytechef-aws-batch-input-" + UUID.randomUUID() + "-", ".tar");

        try (AwsBatchOperations operations = operationsFactory.create(connectionSettings, urlValidity)) {
            Files.copy(inputArchive, archivePath, StandardCopyOption.REPLACE_EXISTING);

            boolean staged = false;
            String jobDefinitionArn = null;

            try {
                operations.putObject(bucket, prefix + AwsBatchJobNames.INPUT_OBJECT, archivePath);

                staged = true;

                URL inputUrl = operations.presignGet(bucket, prefix + AwsBatchJobNames.INPUT_OBJECT, urlValidity);
                URL outputUrl = operations.presignPut(bucket, prefix + AwsBatchJobNames.OUTPUT_OBJECT, urlValidity);

                jobDefinitionArn = operations.registerJobDefinition(
                    AwsBatchJobFactory.jobDefinition(
                        spec, runnerSettings, tags, inputUrl, outputUrl, context.completionBudget()));

                String jobId = operations.submitJob(
                    AwsBatchJobFactory.submitJob(key, jobDefinitionArn, runnerSettings.jobQueue(), tags));

                return handle(jobId, bucket, prefix, jobDefinitionArn);
            } catch (SdkException exception) {
                discard(operations, jobDefinitionArn, bucket, staged ? prefix : null);

                throw failure("submitting the job", exception);
            } catch (RuntimeException exception) {
                discard(operations, jobDefinitionArn, bucket, staged ? prefix : null);

                throw exception;
            }
        } finally {
            Files.deleteIfExists(archivePath);
        }
    }

    @Override
    public RemoteJobExit await(
        RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
        RemoteJobContext context) throws InterruptedException {

        Duration pollInterval = pollInterval(context.runnerParameters());

        try (AwsBatchOperations operations = operationsFactory.create(
            connectionSettings(context), completionDeadline.remaining()
                .plus(CLIENT_GRACE))) {

            String logStreamName = null;
            String nextToken = null;

            while (true) {
                JobDetail jobDetail = operations.describeJob(handle.id());

                if (jobDetail == null) {
                    throw new IllegalStateException("The aws-batch job %s disappeared".formatted(handle.id()));
                }

                TaskContainerDetails main = mainContainer(jobDetail);

                if (logStreamName == null && main != null && main.logStreamName() != null) {
                    logStreamName = main.logStreamName();
                }

                JobStatus status = jobDetail.status();

                boolean terminal = TERMINAL.contains(status);

                if (logStreamName != null) {
                    nextToken = drainLog(operations, logStreamName, nextToken, logSink, terminal);
                }

                if (terminal) {
                    return exit(jobDetail, main);
                }

                if (NOT_STARTED.contains(status) && runningDeadline.isExpired()) {
                    throw new IllegalStateException(
                        "The aws-batch job did not start within %s; the job queue may have no capacity: %s"
                            .formatted(runningDeadline.getBudget(), jobDetail.statusReason()));
                }

                if (completionDeadline.isExpired()) {
                    return RemoteJobExit.timedOut();
                }

                Thread.sleep(pollInterval.toMillis());
            }
        } catch (SdkException exception) {
            throw failure("waiting for the job", exception);
        }
    }

    @Override
    public @Nullable InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
        throws InterruptedException {

        AwsBatchOperations operations = operationsFactory.create(
            connectionSettings(context), completionDeadline.remaining()
                .plus(CLIENT_GRACE));

        try {
            InputStream inputStream = operations.getObject(
                attribute(handle, BUCKET_ATTRIBUTE),
                attribute(handle, PREFIX_ATTRIBUTE) + AwsBatchJobNames.OUTPUT_OBJECT);

            if (inputStream == null) {
                operations.close();

                return null;
            }

            return new FilterInputStream(inputStream) {

                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        operations.close();
                    }
                }
            };
        } catch (SdkException exception) {
            operations.close();

            throw failure("collecting the outputs", exception);
        } catch (RuntimeException exception) {
            operations.close();

            throw exception;
        }
    }

    @Override
    public void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context) {
        if (cleanupMode == CleanupMode.DETACH) {
            return;
        }

        boolean interrupted = Thread.interrupted();

        try (AwsBatchOperations operations = operationsFactory.create(connectionSettings(context), CLIENT_GRACE)) {
            if (cleanupMode == CleanupMode.KILL_AND_DELETE) {
                quietly("terminate the job", () -> operations.terminateJob(handle.id(), "Stopped by ByteChef"));
            }

            discard(
                operations, handle.attributes()
                    .get(JOB_DEFINITION_ATTRIBUTE),
                handle.attributes()
                    .get(BUCKET_ATTRIBUTE),
                handle.attributes()
                    .get(PREFIX_ATTRIBUTE));
        } catch (RuntimeException exception) {
            log.warn("Could not clean up the aws-batch job {}", handle.id(), exception);
        } finally {
            if (interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
        }
    }

    private Duration pollInterval(Parameters runnerParameters) {
        Duration interval = AwsBatchRunnerSettings.completionCheckInterval(runnerParameters);

        return interval.compareTo(minimumPollInterval) < 0 ? minimumPollInterval : interval;
    }

    private static String attribute(RemoteJobHandle handle, String name) {
        String value = handle.attributes()
            .get(name);

        if (value == null) {
            throw new IllegalStateException("The aws-batch job handle %s has no %s".formatted(handle.id(), name));
        }

        return value;
    }

    private static IllegalStateException clientFailure(String phase, SdkException exception) {
        if (exception instanceof AwsServiceException awsServiceException &&
            awsServiceException.awsErrorDetails() != null) {

            return new IllegalStateException(
                "The aws-batch runner failed while %s: %s (HTTP %d)".formatted(
                    phase, awsServiceException.awsErrorDetails()
                        .errorCode(),
                    awsServiceException.statusCode()),
                exception);
        }

        return new IllegalStateException(
            "The aws-batch runner failed while %s: no response from AWS".formatted(phase), exception);
    }

    private static AwsConnectionSettings connectionSettings(RemoteJobContext context) {
        ComponentConnection connection = context.connection();

        if (connection == null) {
            throw new IllegalArgumentException(
                "The AWS Batch runner needs a connection; select one in the task's connections");
        }

        return AwsConnectionSettings.of(connection);
    }

    private static void discard(
        AwsBatchOperations operations, @Nullable String jobDefinitionArn, @Nullable String bucket,
        @Nullable String prefix) {

        boolean interrupted = Thread.interrupted();

        try {
            if (jobDefinitionArn != null) {
                quietly("deregister the job definition", () -> operations.deregisterJobDefinition(jobDefinitionArn));
            }

            if (bucket != null && prefix != null) {
                quietly("delete the staging prefix", () -> operations.deletePrefix(bucket, prefix));
            }
        } finally {
            if (interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
        }
    }

    @Nullable
    private static String drainLog(
        AwsBatchOperations operations, String logStreamName, @Nullable String nextToken, RemoteLogSink logSink,
        boolean terminal) {

        String token = nextToken;
        int maxPages = terminal ? MAX_LOG_PAGES_TERMINAL : MAX_LOG_PAGES_PER_POLL;

        for (int page = 0; page < maxPages; page++) {
            AwsBatchOperations.LogPage logPage = operations.getLogEvents(LOG_GROUP, logStreamName, token);

            List<String> messages = logPage.messages();

            for (String message : messages) {
                byte[] bytes = (message + "\n").getBytes(StandardCharsets.UTF_8);

                logSink.appendStdout(bytes, bytes.length);
            }

            String next = logPage.nextForwardToken();

            if (next == null || Objects.equals(next, token) || (!terminal && messages.isEmpty())) {
                return next == null ? token : next;
            }

            token = next;
        }

        return token;
    }

    private static RemoteJobExit exit(JobDetail jobDetail, @Nullable TaskContainerDetails main) {
        if (main != null && main.exitCode() != null) {
            int mainExitCode = main.exitCode();

            if (mainExitCode == 0 && jobDetail.status() == JobStatus.FAILED) {
                TaskContainerDetails output = container(jobDetail, OUTPUT_CONTAINER);

                Integer outputExitCode = output == null ? null : output.exitCode();

                if (outputExitCode == null || outputExitCode != 0) {
                    String outputReason = output == null ? null : output.reason();

                    return new RemoteJobExit(
                        true, null,
                        "the output upload failed: " + (outputReason == null || outputReason.isBlank()
                            ? statusReason(jobDetail) : outputReason));
                }
            }

            String reason = main.reason();

            return new RemoteJobExit(true, mainExitCode, reason == null || reason.isBlank() ? null : reason);
        }

        return new RemoteJobExit(true, null, statusReason(jobDetail));
    }

    private static IllegalStateException failure(String phase, SdkException exception)
        throws InterruptedException {

        if (isInterrupt(exception)) {
            Thread.interrupted();

            InterruptedException interruptedException = new InterruptedException(
                "Interrupted while the aws-batch runner was " + phase);

            interruptedException.initCause(exception);

            throw interruptedException;
        }

        return clientFailure(phase, exception);
    }

    private static RemoteJobHandle handle(
        String jobId, String bucket, @Nullable String prefix, @Nullable String jobDefinitionArn) {

        Map<String, String> attributes = new LinkedHashMap<>();

        attributes.put(BUCKET_ATTRIBUTE, bucket);

        if (prefix != null) {
            attributes.put(PREFIX_ATTRIBUTE, prefix);
        }

        if (jobDefinitionArn != null) {
            attributes.put(JOB_DEFINITION_ATTRIBUTE, jobDefinitionArn);
        }

        return new RemoteJobHandle(jobId, attributes);
    }

    private static boolean isInterrupt(Throwable exception) {
        if (Thread.currentThread()
            .isInterrupted()) {

            return true;
        }

        Throwable cause = exception;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof InterruptedException || cause instanceof AbortedException ||
                (cause instanceof InterruptedIOException && !(cause instanceof SocketTimeoutException))) {

                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    @Nullable
    private static TaskContainerDetails mainContainer(JobDetail jobDetail) {
        return container(jobDetail, MAIN_CONTAINER);
    }

    @Nullable
    private static TaskContainerDetails container(JobDetail jobDetail, String containerName) {
        if (jobDetail.ecsProperties() == null) {
            return null;
        }

        return jobDetail.ecsProperties()
            .taskProperties()
            .stream()
            .flatMap(taskDetails -> taskDetails.containers()
                .stream())
            .filter(container -> containerName.equals(container.name()))
            .findFirst()
            .orElse(null);
    }

    private static String statusReason(JobDetail jobDetail) {
        String statusReason = jobDetail.statusReason();

        return statusReason == null ? jobDetail.statusAsString() : statusReason;
    }

    private static void quietly(String action, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException exception) {
            log.warn("The aws-batch runner could not {}", action, exception);
        }
    }
}
