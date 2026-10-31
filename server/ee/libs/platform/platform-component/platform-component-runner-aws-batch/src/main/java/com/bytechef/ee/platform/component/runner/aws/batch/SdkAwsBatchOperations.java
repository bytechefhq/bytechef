/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.batch.BatchClient;
import software.amazon.awssdk.services.batch.BatchClientBuilder;
import software.amazon.awssdk.services.batch.model.DescribeJobsResponse;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.KeyValuesPair;
import software.amazon.awssdk.services.batch.model.ListJobsRequest;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * {@link AwsBatchOperations} over the AWS SDK v2.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class SdkAwsBatchOperations implements AwsBatchOperations {

    private static final int DELETE_BATCH_SIZE = 1000;

    private final BatchClient batchClient;
    private final AwsCredentialsProvider credentialsProvider;
    private final SdkHttpClient httpClient;
    private final CloudWatchLogsClient logsClient;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    SdkAwsBatchOperations(AwsConnectionSettings connectionSettings, Duration sessionDuration) {
        this(connectionSettings, sessionDuration, null);
    }

    SdkAwsBatchOperations(
        AwsConnectionSettings connectionSettings, Duration sessionDuration, @Nullable URI endpointOverride) {

        Region region = connectionSettings.awsRegion();

        this.credentialsProvider = connectionSettings.credentialsProvider(sessionDuration);
        this.httpClient = ApacheHttpClient.builder()
            .build();

        BatchClientBuilder batchClientBuilder = BatchClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient);
        CloudWatchLogsClientBuilder logsClientBuilder = CloudWatchLogsClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient);
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient);
        S3Presigner.Builder s3PresignerBuilder = S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentialsProvider);

        if (endpointOverride != null) {
            batchClientBuilder.endpointOverride(endpointOverride);
            logsClientBuilder.endpointOverride(endpointOverride);
            s3ClientBuilder.endpointOverride(endpointOverride)
                .forcePathStyle(true);
            s3PresignerBuilder.endpointOverride(endpointOverride)
                .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }

        this.batchClient = batchClientBuilder.build();
        this.logsClient = logsClientBuilder.build();
        this.s3Client = s3ClientBuilder.build();
        this.s3Presigner = s3PresignerBuilder.build();
    }

    @Override
    public void putObject(String bucket, String key, Path file) {
        s3Client.putObject(builder -> builder.bucket(bucket)
            .key(key), RequestBody.fromFile(file));
    }

    @Override
    public URL presignGet(String bucket, String key, Duration validity) {
        return s3Presigner.presignGetObject(builder -> builder.signatureDuration(validity)
            .getObjectRequest(request -> request.bucket(bucket)
                .key(key)))
            .url();
    }

    @Override
    public URL presignPut(String bucket, String key, Duration validity) {
        return s3Presigner.presignPutObject(builder -> builder.signatureDuration(validity)
            .putObjectRequest(request -> request.bucket(bucket)
                .key(key)))
            .url();
    }

    @Override
    public @Nullable InputStream getObject(String bucket, String key) {
        try {
            return s3Client.getObject(builder -> builder.bucket(bucket)
                .key(key));
        } catch (NoSuchKeyException exception) {
            return null;
        }
    }

    @Override
    public void deletePrefix(String bucket, String prefix) {
        List<ObjectIdentifier> objectIdentifiers = new ArrayList<>();

        for (S3Object s3Object : s3Client.listObjectsV2Paginator(builder -> builder.bucket(bucket)
            .prefix(prefix))
            .contents()) {

            objectIdentifiers.add(ObjectIdentifier.builder()
                .key(s3Object.key())
                .build());

            if (objectIdentifiers.size() == DELETE_BATCH_SIZE) {
                deleteObjects(bucket, objectIdentifiers);

                objectIdentifiers.clear();
            }
        }

        if (!objectIdentifiers.isEmpty()) {
            deleteObjects(bucket, objectIdentifiers);
        }
    }

    @Override
    public String registerJobDefinition(RegisterJobDefinitionRequest request) {
        return batchClient.registerJobDefinition(request)
            .jobDefinitionArn();
    }

    @Override
    public void deregisterJobDefinition(String jobDefinitionArn) {
        batchClient.deregisterJobDefinition(builder -> builder.jobDefinition(jobDefinitionArn));
    }

    @Override
    public String submitJob(SubmitJobRequest request) {
        return batchClient.submitJob(request)
            .jobId();
    }

    @Override
    public @Nullable JobDetail describeJob(String jobId) {
        DescribeJobsResponse describeJobsResponse = batchClient.describeJobs(builder -> builder.jobs(jobId));

        List<JobDetail> jobs = describeJobsResponse.jobs();

        return jobs.isEmpty() ? null : jobs.getFirst();
    }

    @Override
    public List<JobSummary> listJobs(String jobQueue, String jobName) {
        ListJobsRequest listJobsRequest = ListJobsRequest.builder()
            .jobQueue(jobQueue)
            .filters(KeyValuesPair.builder()
                .name("JOB_NAME")
                .values(jobName)
                .build())
            .build();

        List<JobSummary> jobSummaries = new ArrayList<>();

        batchClient.listJobsPaginator(listJobsRequest)
            .jobSummaryList()
            .forEach(jobSummaries::add);

        return jobSummaries;
    }

    @Override
    public void terminateJob(String jobId, String reason) {
        batchClient.terminateJob(builder -> builder.jobId(jobId)
            .reason(reason));
    }

    @Override
    public LogPage getLogEvents(String logGroupName, String logStreamName, @Nullable String nextToken) {
        try {
            GetLogEventsResponse getLogEventsResponse = logsClient.getLogEvents(builder -> builder
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .nextToken(nextToken)
                .startFromHead(true));

            return new LogPage(
                getLogEventsResponse.events()
                    .stream()
                    .map(OutputLogEvent::message)
                    .toList(),
                getLogEventsResponse.nextForwardToken());
        } catch (ResourceNotFoundException exception) {
            return new LogPage(List.of(), nextToken);
        }
    }

    @Override
    public void close() {
        s3Presigner.close();
        s3Client.close();
        logsClient.close();
        batchClient.close();
        httpClient.close();

        if (credentialsProvider instanceof SdkAutoCloseable sdkAutoCloseable) {
            sdkAutoCloseable.close();
        }
    }

    private void deleteObjects(String bucket, List<ObjectIdentifier> objectIdentifiers) {
        s3Client.deleteObjects(builder -> builder.bucket(bucket)
            .delete(delete -> delete.objects(objectIdentifiers)));
    }
}
