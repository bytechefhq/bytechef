/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.IMAGE;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The AWS Batch runner's task properties, read and checked once.
 *
 * @author Ivica Cardic
 * @version ee
 */
record AwsBatchRunnerSettings(
    String image, String jobQueue, String bucket, Platform platform, @Nullable String executionRoleArn,
    @Nullable String taskRoleArn, BigDecimal vcpu, int memory, boolean assignPublicIp,
    Duration completionCheckInterval, String fileSidecarImage) {

    static final String ASSIGN_PUBLIC_IP = "assignPublicIp";
    static final String BUCKET = "bucket";
    static final String COMPLETION_CHECK_INTERVAL = "completionCheckInterval";
    static final String EXECUTION_ROLE_ARN = "executionRoleArn";
    static final String FILE_SIDECAR_IMAGE = "fileSidecarImage";
    static final String JOB_QUEUE = "jobQueue";
    static final String MEMORY = "memory";
    static final String PLATFORM = "platform";
    static final String TASK_ROLE_ARN = "taskRoleArn";
    static final String VCPU = "vcpu";

    static final BigDecimal SIDECAR_VCPU = new BigDecimal("0.25");
    static final int SIDECAR_MEMORY = 64;

    private static final Pattern BUCKET_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");
    private static final Duration DEFAULT_COMPLETION_CHECK_INTERVAL = Duration.ofSeconds(5);
    private static final String DEFAULT_FILE_SIDECAR_IMAGE = "curlimages/curl:8.11.1";
    private static final Map<String, String> DEFAULT_IMAGES = Map.of(
        "bash", "ubuntu:24.04", "javascript", "node:22-alpine", "js", "node:22-alpine", "python", "python:3.12-slim",
        "shell", "ubuntu:24.04");
    private static final int DEFAULT_MEMORY = 2048;
    private static final BigDecimal DEFAULT_VCPU = BigDecimal.ONE;
    private static final BigDecimal MIN_MAIN_VCPU = new BigDecimal("0.25");
    private static final int MIN_MAIN_MEMORY = 128;
    private static final NavigableMap<BigDecimal, List<Integer>> FARGATE_MEMORIES = fargateMemories();

    enum Platform {

        FARGATE, EC2
    }

    static AwsBatchRunnerSettings of(String languageId, Parameters parameters) {
        String image = text(parameters.getString(IMAGE));

        if (image == null) {
            image = DEFAULT_IMAGES.get(languageId);
        }

        if (image == null) {
            throw new IllegalArgumentException(
                "The aws-batch runner has no default image for language '%s'; name one on the task runner"
                    .formatted(languageId));
        }

        String jobQueue = jobQueue(parameters);

        if (jobQueue.isEmpty()) {
            throw new IllegalArgumentException("The aws-batch runner needs a job queue (its name or ARN)");
        }

        String bucket = bucket(parameters);

        if (!BUCKET_PATTERN.matcher(bucket)
            .matches()) {

            throw new IllegalArgumentException(
                "The aws-batch runner's staging bucket '%s' is not a valid S3 bucket name".formatted(bucket));
        }

        Platform platform = platform(parameters);

        String executionRoleArn = text(parameters.getString(EXECUTION_ROLE_ARN));

        if (platform == Platform.FARGATE && executionRoleArn == null) {
            throw new IllegalArgumentException(
                "The aws-batch runner needs an execution role on Fargate, so the task can pull images and write logs");
        }

        BigDecimal vcpu = vcpu(parameters);
        int memory = parameters.getInteger(MEMORY, DEFAULT_MEMORY);

        checkSize(platform, vcpu, memory);

        String fileSidecarImage = text(parameters.getString(FILE_SIDECAR_IMAGE));

        return new AwsBatchRunnerSettings(
            image, jobQueue, bucket, platform, executionRoleArn, text(parameters.getString(TASK_ROLE_ARN)), vcpu,
            memory, parameters.getBoolean(ASSIGN_PUBLIC_IP, false), completionCheckInterval(parameters),
            fileSidecarImage == null ? DEFAULT_FILE_SIDECAR_IMAGE : fileSidecarImage);
    }

    static String bucket(Parameters parameters) {
        String bucket = text(parameters.getString(BUCKET));

        return bucket == null ? "" : bucket;
    }

    static String jobQueue(Parameters parameters) {
        String jobQueue = text(parameters.getString(JOB_QUEUE));

        return jobQueue == null ? "" : jobQueue;
    }

    static List<ModifiableValueProperty<?, ?>> properties() {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(
            string(IMAGE)
                .label("Image")
                .description("The container image the task runs in. Defaults to the image for the action's language.")
                .required(false));
        properties.add(
            array(ENTRYPOINT)
                .label("Entrypoint")
                .description(
                    "Replaces the interpreter the source is handed to. Defaults to the interpreter for the action's " +
                        "language; the source file is appended either way. The image must provide /bin/sh.")
                .items(string())
                .required(false));
        properties.add(
            string(JOB_QUEUE)
                .label("Job Queue")
                .description("The name or ARN of an ECS-backed AWS Batch job queue.")
                .required(true));
        properties.add(
            string(BUCKET)
                .label("Staging Bucket")
                .description(
                    "The S3 bucket inputs and outputs are staged in, under bytechef/. The job reaches it only through "
                        +
                        "presigned URLs.")
                .required(true));
        properties.add(
            string(PLATFORM)
                .label("Platform")
                .description("The compute the job queue runs on.")
                .options(option("Fargate", Platform.FARGATE.name()), option("EC2", Platform.EC2.name()))
                .defaultValue(Platform.FARGATE.name())
                .required(false));
        properties.add(
            string(EXECUTION_ROLE_ARN)
                .label("Execution Role ARN")
                .description("The role ECS uses to pull the images and write the logs. Required on Fargate.")
                .required(false));
        properties.add(
            string(TASK_ROLE_ARN)
                .label("Task Role ARN")
                .description("The role the task's own code runs as, for its AWS calls.")
                .required(false));
        properties.add(
            string(VCPU)
                .label("vCPU")
                .description(
                    "The task's total vCPU, sidecars included (they take 0.5). On Fargate: 1, 2, 4, 8 or 16.")
                .defaultValue("1")
                .required(false));
        properties.add(
            integer(MEMORY)
                .label("Memory (MiB)")
                .description(
                    "The task's total memory in MiB, sidecars included (they take 128). On Fargate it must suit the " +
                        "vCPU.")
                .defaultValue(DEFAULT_MEMORY)
                .required(false));
        properties.add(
            bool(ASSIGN_PUBLIC_IP)
                .label("Assign Public IP")
                .description(
                    "Gives a Fargate task a public IP, which it needs to pull public images and reach S3 from a " +
                        "public subnet without a NAT gateway.")
                .defaultValue(false)
                .required(false));
        properties.add(
            string(COMPLETION_CHECK_INTERVAL)
                .label("Completion Check Interval")
                .description("How often the job's state and logs are polled, as an ISO-8601 duration.")
                .defaultValue("PT5S")
                .required(false));
        properties.add(
            string(FILE_SIDECAR_IMAGE)
                .label("File Sidecar Image")
                .description("The image that moves files in and out of the task; it needs sh, curl and tar.")
                .defaultValue(DEFAULT_FILE_SIDECAR_IMAGE)
                .required(false));

        return properties;
    }

    BigDecimal mainVcpu() {
        return vcpu.subtract(SIDECAR_VCPU.multiply(BigDecimal.valueOf(2)));
    }

    int mainMemory() {
        return memory - SIDECAR_MEMORY * 2;
    }

    private static void checkSize(Platform platform, BigDecimal vcpu, int memory) {
        BigDecimal mainVcpu = vcpu.subtract(SIDECAR_VCPU.multiply(BigDecimal.valueOf(2)));
        int mainMemory = memory - SIDECAR_MEMORY * 2;

        if (mainVcpu.compareTo(MIN_MAIN_VCPU) < 0 || mainMemory < MIN_MAIN_MEMORY) {
            throw new IllegalArgumentException(
                ("The aws-batch runner's task size %s vCPU / %d MiB leaves the task's own container less than " +
                    "0.25 vCPU / 128 MiB after the two file sidecars (0.25 vCPU / 64 MiB each)").formatted(
                        vcpu.toPlainString(), memory));
        }

        if (platform != Platform.FARGATE) {
            return;
        }

        List<Integer> memories = FARGATE_MEMORIES.get(vcpu);

        if (memories == null) {
            throw new IllegalArgumentException(
                "The aws-batch runner's vcpu %s is not a Fargate size; use 1, 2, 4, 8 or 16".formatted(
                    vcpu.toPlainString()));
        }

        if (!memories.contains(memory)) {
            throw new IllegalArgumentException(
                "The aws-batch runner's memory %d MiB does not suit %s vCPU on Fargate; use one of %s".formatted(
                    memory, vcpu.toPlainString(), memories));
        }
    }

    static Duration completionCheckInterval(Parameters parameters) {
        String value = text(parameters.getString(COMPLETION_CHECK_INTERVAL));

        if (value == null) {
            return DEFAULT_COMPLETION_CHECK_INTERVAL;
        }

        Duration duration;

        try {
            duration = Duration.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "The aws-batch runner cannot read '%s' as the ISO-8601 duration %s expects".formatted(
                    value, COMPLETION_CHECK_INTERVAL),
                exception);
        }

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                "The aws-batch runner's %s must be greater than zero".formatted(COMPLETION_CHECK_INTERVAL));
        }

        return duration;
    }

    private static NavigableMap<BigDecimal, List<Integer>> fargateMemories() {
        NavigableMap<BigDecimal, List<Integer>> fargateMemories = new TreeMap<>();

        fargateMemories.put(BigDecimal.ONE, range(2048, 8192, 1024));
        fargateMemories.put(BigDecimal.TWO, range(4096, 16384, 1024));
        fargateMemories.put(new BigDecimal("4"), range(8192, 30720, 1024));
        fargateMemories.put(new BigDecimal("8"), range(16384, 61440, 4096));
        fargateMemories.put(new BigDecimal("16"), range(32768, 122880, 8192));

        return Collections.unmodifiableNavigableMap(fargateMemories);
    }

    private static Platform platform(Parameters parameters) {
        String value = text(parameters.getString(PLATFORM));

        if (value == null) {
            return Platform.FARGATE;
        }

        try {
            return Platform.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "The aws-batch runner does not know the platform '%s'; use FARGATE or EC2".formatted(value),
                exception);
        }
    }

    private static List<Integer> range(int from, int to, int step) {
        List<Integer> values = new ArrayList<>();

        for (int value = from; value <= to; value += step) {
            values.add(value);
        }

        return List.copyOf(values);
    }

    @Nullable
    private static String text(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal vcpu(Parameters parameters) {
        String value = text(parameters.getString(VCPU));

        if (value == null) {
            return DEFAULT_VCPU;
        }

        try {
            return new BigDecimal(value).stripTrailingZeros();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "The aws-batch runner cannot read '%s' as a vcpu amount".formatted(value), exception);
        }
    }
}
