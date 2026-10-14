/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.batch.model.EcsPropertiesDetail;
import software.amazon.awssdk.services.batch.model.EcsTaskDetails;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobStatus;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.batch.model.TaskContainerDetails;

/**
 * @author Ivica Cardic
 * @version ee
 */
final class FakeAwsBatchOperations implements AwsBatchOperations {

    static final String JOB_DEFINITION_ARN = "arn:aws:batch:eu-west-1:123456789012:job-definition/bytechef-x:1";
    static final String JOB_ID = "job-1";

    final Map<String, byte[]> objects = new LinkedHashMap<>();
    final Deque<JobDetail> jobSnapshots = new ArrayDeque<>();
    final Deque<LogPage> logPages = new ArrayDeque<>();
    final List<RegisterJobDefinitionRequest> registered = new ArrayList<>();
    final List<SubmitJobRequest> submitted = new ArrayList<>();
    final List<String> deregistered = new ArrayList<>();
    final List<String> deletedPrefixes = new ArrayList<>();
    final List<String> terminated = new ArrayList<>();
    final List<Duration> presignValidities = new ArrayList<>();
    final List<Duration> sessionDurations = new ArrayList<>();
    final Map<String, JobDetail> describedById = new LinkedHashMap<>();

    List<JobSummary> listedJobs = List.of();
    @Nullable
    RuntimeException failPutObject;
    @Nullable
    RuntimeException failRegister;
    @Nullable
    RuntimeException failSubmit;
    @Nullable
    RuntimeException failDescribe;
    @Nullable
    RuntimeException failCleanupCalls;
    boolean failWhileInterrupted;
    int closeCount;

    AwsBatchOperationsFactory factory() {
        return (connectionSettings, sessionDuration) -> {
            sessionDurations.add(sessionDuration);

            return this;
        };
    }

    @Override
    public void putObject(String bucket, String key, Path file) {
        if (failPutObject != null) {
            throw failPutObject;
        }

        try {
            objects.put(bucket + "/" + key, Files.readAllBytes(file));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public URL presignGet(String bucket, String key, Duration validity) {
        presignValidities.add(validity);

        return url("https://" + bucket + ".s3.amazonaws.com/" + key + "?X-Amz-Signature=get");
    }

    @Override
    public URL presignPut(String bucket, String key, Duration validity) {
        presignValidities.add(validity);

        return url("https://" + bucket + ".s3.amazonaws.com/" + key + "?X-Amz-Signature=put");
    }

    @Override
    public @Nullable InputStream getObject(String bucket, String key) {
        byte[] bytes = objects.get(bucket + "/" + key);

        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }

    @Override
    public void deletePrefix(String bucket, String prefix) {
        failIfInterruptedOrBroken();

        deletedPrefixes.add(bucket + "/" + prefix);
    }

    @Override
    public String registerJobDefinition(RegisterJobDefinitionRequest request) {
        if (failRegister != null) {
            throw failRegister;
        }

        registered.add(request);

        return JOB_DEFINITION_ARN;
    }

    @Override
    public void deregisterJobDefinition(String jobDefinitionArn) {
        failIfInterruptedOrBroken();

        deregistered.add(jobDefinitionArn);
    }

    @Override
    public String submitJob(SubmitJobRequest request) {
        if (failSubmit != null) {
            throw failSubmit;
        }

        submitted.add(request);

        return JOB_ID;
    }

    @Override
    public @Nullable JobDetail describeJob(String jobId) {
        if (failDescribe != null) {
            throw failDescribe;
        }

        JobDetail described = describedById.get(jobId);

        if (described != null) {
            return described;
        }

        return jobSnapshots.size() > 1 ? jobSnapshots.poll() : jobSnapshots.peek();
    }

    @Override
    public List<JobSummary> listJobs(String jobQueue, String jobName) {
        return listedJobs;
    }

    @Override
    public void terminateJob(String jobId, String reason) {
        failIfInterruptedOrBroken();

        terminated.add(jobId);
    }

    @Override
    public LogPage getLogEvents(String logGroupName, String logStreamName, @Nullable String nextToken) {
        LogPage logPage = logPages.poll();

        return logPage == null ? new LogPage(List.of(), nextToken) : logPage;
    }

    @Override
    public void close() {
        closeCount++;
    }

    static JobDetail job(
        JobStatus status, @Nullable Integer mainExitCode, @Nullable String reason, long createdAt,
        Map<String, String> tags) {

        TaskContainerDetails.Builder mainBuilder = TaskContainerDetails.builder()
            .name("main")
            .logStreamName("bytechef/main/abc");

        if (mainExitCode != null) {
            mainBuilder.exitCode(mainExitCode);
        }

        if (reason != null) {
            mainBuilder.reason(reason);
        }

        return JobDetail.builder()
            .jobId(JOB_ID)
            .jobName("bytechef-public-100-7")
            .status(status)
            .statusReason(reason)
            .createdAt(createdAt)
            .jobDefinition(JOB_DEFINITION_ARN)
            .tags(tags)
            .ecsProperties(EcsPropertiesDetail.builder()
                .taskProperties(EcsTaskDetails.builder()
                    .containers(mainBuilder.build())
                    .build())
                .build())
            .build();
    }

    static JobDetail withOutputContainer(JobDetail job, @Nullable Integer exitCode, @Nullable String reason) {
        TaskContainerDetails.Builder outputBuilder = TaskContainerDetails.builder()
            .name("bytechef-output");

        if (exitCode != null) {
            outputBuilder.exitCode(exitCode);
        }

        if (reason != null) {
            outputBuilder.reason(reason);
        }

        List<TaskContainerDetails> containers = new ArrayList<>(job.ecsProperties()
            .taskProperties()
            .getFirst()
            .containers());

        containers.add(outputBuilder.build());

        return job.toBuilder()
            .ecsProperties(EcsPropertiesDetail.builder()
                .taskProperties(EcsTaskDetails.builder()
                    .containers(containers)
                    .build())
                .build())
            .build();
    }

    private void failIfInterruptedOrBroken() {
        if (failWhileInterrupted && Thread.currentThread()
            .isInterrupted()) {

            throw new IllegalStateException("interrupted");
        }

        if (failCleanupCalls != null) {
            throw failCleanupCalls;
        }
    }

    private static URL url(String value) {
        try {
            return URI.create(value)
                .toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
