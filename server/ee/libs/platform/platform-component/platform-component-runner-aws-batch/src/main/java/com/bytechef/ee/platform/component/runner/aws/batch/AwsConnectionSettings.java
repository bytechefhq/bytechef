/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import com.bytechef.platform.component.ComponentConnection;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

/**
 * The AWS account an AWS Batch task runner works in, read from the task's {@code aws} connection.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AwsConnectionSettings(
    String accessKeyId, String secretAccessKey, String region, @Nullable String roleArn,
    @Nullable String externalId) {

    static final String ACCESS_KEY_ID = "accessKeyId";
    static final String EXTERNAL_ID = "externalId";
    static final String REGION = "region";
    static final String ROLE_ARN = "roleArn";
    static final String SECRET_ACCESS_KEY = "secretAccessKey";

    private static final Duration MAX_SESSION = Duration.ofHours(12);
    private static final Duration MIN_SESSION = Duration.ofMinutes(15);
    private static final int VISIBLE_ACCESS_KEY_ID_CHARACTERS = 4;
    private static final Pattern ROLE_ARN_PATTERN = Pattern.compile("^arn:aws[a-z-]*:iam::\\d{12}:role/.+$");
    private static final String ROLE_SESSION_NAME = "bytechef-task-runner";
    private static final Duration URL_MARGIN = Duration.ofMinutes(10);

    public static AwsConnectionSettings of(ComponentConnection connection) {
        Map<String, ?> parameters = connection.getParameters();

        return new AwsConnectionSettings(
            textOrEmpty(parameters, ACCESS_KEY_ID), textOrEmpty(parameters, SECRET_ACCESS_KEY),
            textOrEmpty(parameters, REGION), text(parameters, ROLE_ARN), text(parameters, EXTERNAL_ID));
    }

    static Duration urlValidity(Duration completionBudget) {
        return completionBudget.plus(URL_MARGIN);
    }

    public Region awsRegion() {
        return Region.of(region);
    }

    public AwsCredentialsProvider credentialsProvider(Duration sessionDuration) {
        StaticCredentialsProvider staticCredentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey));

        if (roleArn == null) {
            return staticCredentialsProvider;
        }

        Duration clamped = sessionDuration.compareTo(MIN_SESSION) < 0 ? MIN_SESSION : sessionDuration;

        if (clamped.compareTo(MAX_SESSION) > 0) {
            clamped = MAX_SESSION;
        }

        StsClient stsClient = StsClient.builder()
            .httpClientBuilder(ApacheHttpClient.builder())
            .region(awsRegion())
            .credentialsProvider(staticCredentialsProvider)
            .build();

        AssumeRoleRequest.Builder assumeRoleRequestBuilder = AssumeRoleRequest.builder()
            .roleArn(roleArn)
            .roleSessionName(ROLE_SESSION_NAME)
            .durationSeconds((int) clamped.toSeconds());

        if (externalId != null) {
            assumeRoleRequestBuilder.externalId(externalId);
        }

        StsAssumeRoleCredentialsProvider assumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
            .stsClient(stsClient)
            .refreshRequest(assumeRoleRequestBuilder.build())
            .build();

        return new AssumedRoleCredentialsProvider(stsClient, assumeRoleCredentialsProvider);
    }

    public void validate() {
        if (accessKeyId.isEmpty()) {
            throw new IllegalArgumentException("The aws connection needs an access key ID");
        }

        if (secretAccessKey.isEmpty()) {
            throw new IllegalArgumentException("The aws connection needs a secret access key");
        }

        if (region.isEmpty()) {
            throw new IllegalArgumentException("The aws connection needs a region");
        }

        if (roleArn != null && !ROLE_ARN_PATTERN.matcher(roleArn)
            .matches()) {

            throw new IllegalArgumentException(
                "The aws connection's role ARN must look like arn:aws:iam::<account>:role/<name>");
        }
    }

    public void validateBudget(Duration completionBudget) {
        if (roleArn != null && urlValidity(completionBudget).compareTo(MAX_SESSION) > 0) {
            throw new IllegalArgumentException(
                ("The aws-batch runner cannot run a job for %s with an assumed role: its presigned URLs must outlive " +
                    "the job by 10 minutes, and an assumed-role session lasts at most 12 hours").formatted(
                        completionBudget));
        }
    }

    @Override
    public String toString() {
        return "AwsConnectionSettings[accessKeyId=%s, secretAccessKey=***, region=%s, roleArn=%s]".formatted(
            maskedAccessKeyId(), region, roleArn);
    }

    private String maskedAccessKeyId() {
        return accessKeyId.length() > VISIBLE_ACCESS_KEY_ID_CHARACTERS * 2
            ? "****" + accessKeyId.substring(accessKeyId.length() - VISIBLE_ACCESS_KEY_ID_CHARACTERS) : "****";
    }

    @Nullable
    private static String text(Map<String, ?> parameters, String name) {
        Object value = parameters.get(name);

        if (value == null) {
            return null;
        }

        String trimmed = value.toString()
            .trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String textOrEmpty(Map<String, ?> parameters, String name) {
        String value = text(parameters, name);

        return value == null ? "" : value;
    }
}
