/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;

/**
 * @author Ivica Cardic
 * @version ee
 */
interface AwsBatchOperations extends AutoCloseable {

    void putObject(String bucket, String key, Path file);

    URL presignGet(String bucket, String key, Duration validity);

    URL presignPut(String bucket, String key, Duration validity);

    @Nullable
    InputStream getObject(String bucket, String key);

    boolean objectExists(String bucket, String key);

    void deletePrefix(String bucket, String prefix);

    String registerJobDefinition(RegisterJobDefinitionRequest request);

    void deregisterJobDefinition(String jobDefinitionArn);

    String submitJob(SubmitJobRequest request);

    @Nullable
    JobDetail describeJob(String jobId);

    List<JobSummary> listJobs(String jobQueue, String jobName);

    void terminateJob(String jobId, String reason);

    LogPage getLogEvents(String logGroupName, String logStreamName, @Nullable String nextToken);

    @Override
    void close();

    record LogPage(List<String> messages, @Nullable String nextForwardToken) {

        public LogPage {
            messages = List.copyOf(messages);
        }
    }
}
