/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.batch.model.AssignPublicIp;
import software.amazon.awssdk.services.batch.model.EcsProperties;
import software.amazon.awssdk.services.batch.model.EcsTaskProperties;
import software.amazon.awssdk.services.batch.model.JobDefinitionType;
import software.amazon.awssdk.services.batch.model.JobTimeout;
import software.amazon.awssdk.services.batch.model.KeyValuePair;
import software.amazon.awssdk.services.batch.model.MountPoint;
import software.amazon.awssdk.services.batch.model.NetworkConfiguration;
import software.amazon.awssdk.services.batch.model.PlatformCapability;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.ResourceRequirement;
import software.amazon.awssdk.services.batch.model.ResourceType;
import software.amazon.awssdk.services.batch.model.RetryStrategy;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.batch.model.TaskContainerDependency;
import software.amazon.awssdk.services.batch.model.TaskContainerProperties;
import software.amazon.awssdk.services.batch.model.Volume;

/**
 * Builds the job definition an AWS Batch run registers: a file sidecar that downloads the inputs, the author's image as
 * {@code main}, and an essential file sidecar that uploads the outputs, so the job ends only after the upload.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class AwsBatchJobFactory {

    static final String INIT_CONTAINER = "bytechef-init";
    static final String INPUT_URL_VARIABLE = "BYTECHEF_INPUT_URL";
    static final String MAIN_CONTAINER = "main";
    static final String OUTPUT_CONTAINER = "bytechef-output";
    static final String OUTPUT_URL_VARIABLE = "BYTECHEF_OUTPUT_URL";
    static final String WORK_DIRECTORY = "/bytechef/work";

    private static final String INIT_SCRIPT =
        "curl -fsS \"$" + INPUT_URL_VARIABLE + "\" | tar -xf - -C " + WORK_DIRECTORY + " && mkdir -p " +
            WORK_DIRECTORY + "/output && chmod 0777 " + WORK_DIRECTORY + " " + WORK_DIRECTORY + "/output";
    private static final String OUTPUT_SCRIPT =
        "cd " + WORK_DIRECTORY + " && if [ -f .stderr ]; then tar -cf output.tar output .stderr; " +
            "else tar -cf output.tar output; fi && curl -fsS -T output.tar \"$" + OUTPUT_URL_VARIABLE + "\"";
    private static final String ROOT_USER = "0";
    private static final int TIMEOUT_MARGIN_SECONDS = 60;
    private static final String VOLUME_NAME = "work";

    private AwsBatchJobFactory() {
    }

    static RegisterJobDefinitionRequest jobDefinition(
        RemoteJobSpec spec, AwsBatchRunnerSettings settings, Map<String, String> tags, URL inputUrl, URL outputUrl,
        Duration completionBudget) {

        List<String> mainCommand = new ArrayList<>(
            List.of(
                "sh", "-c", "cd " + spec.remoteWorkingDirectory() + " && exec \"$@\" 2>" + spec.remoteStderrPath(),
                "sh"));

        mainCommand.addAll(spec.interpreter());
        mainCommand.add(spec.remoteSourcePath());

        TaskContainerProperties init = TaskContainerProperties.builder()
            .name(INIT_CONTAINER)
            .image(settings.fileSidecarImage())
            .user(ROOT_USER)
            .essential(false)
            .command("sh", "-c", INIT_SCRIPT)
            .environment(KeyValuePair.builder()
                .name(INPUT_URL_VARIABLE)
                .value(inputUrl.toString())
                .build())
            .mountPoints(workMount())
            .resourceRequirements(resources(AwsBatchRunnerSettings.SIDECAR_VCPU, AwsBatchRunnerSettings.SIDECAR_MEMORY))
            .build();

        TaskContainerProperties main = TaskContainerProperties.builder()
            .name(MAIN_CONTAINER)
            .image(settings.image())
            .essential(false)
            .command(mainCommand)
            .environment(
                spec.environment()
                    .entrySet()
                    .stream()
                    .map(entry -> KeyValuePair.builder()
                        .name(entry.getKey())
                        .value(entry.getValue())
                        .build())
                    .toList())
            .dependsOn(TaskContainerDependency.builder()
                .containerName(INIT_CONTAINER)
                .condition("SUCCESS")
                .build())
            .mountPoints(workMount())
            .resourceRequirements(resources(settings.mainVcpu(), settings.mainMemory()))
            .build();

        TaskContainerProperties output = TaskContainerProperties.builder()
            .name(OUTPUT_CONTAINER)
            .image(settings.fileSidecarImage())
            .user(ROOT_USER)
            .essential(true)
            .command("sh", "-c", OUTPUT_SCRIPT)
            .environment(KeyValuePair.builder()
                .name(OUTPUT_URL_VARIABLE)
                .value(outputUrl.toString())
                .build())
            .dependsOn(TaskContainerDependency.builder()
                .containerName(MAIN_CONTAINER)
                .condition("COMPLETE")
                .build())
            .mountPoints(workMount())
            .resourceRequirements(resources(AwsBatchRunnerSettings.SIDECAR_VCPU, AwsBatchRunnerSettings.SIDECAR_MEMORY))
            .build();

        EcsTaskProperties.Builder taskPropertiesBuilder = EcsTaskProperties.builder()
            .containers(init, main, output)
            .volumes(Volume.builder()
                .name(VOLUME_NAME)
                .build())
            .executionRoleArn(settings.executionRoleArn())
            .taskRoleArn(settings.taskRoleArn());

        boolean fargate = settings.platform() == AwsBatchRunnerSettings.Platform.FARGATE;

        if (fargate) {
            taskPropertiesBuilder.networkConfiguration(NetworkConfiguration.builder()
                .assignPublicIp(settings.assignPublicIp() ? AssignPublicIp.ENABLED : AssignPublicIp.DISABLED)
                .build());
        }

        return RegisterJobDefinitionRequest.builder()
            .jobDefinitionName(AwsBatchJobNames.jobName(spec.key()))
            .type(JobDefinitionType.CONTAINER)
            .platformCapabilities(fargate ? PlatformCapability.FARGATE : PlatformCapability.EC2)
            .ecsProperties(EcsProperties.builder()
                .taskProperties(taskPropertiesBuilder.build())
                .build())
            .timeout(JobTimeout.builder()
                .attemptDurationSeconds((int) completionBudget.toSeconds() + TIMEOUT_MARGIN_SECONDS)
                .build())
            .retryStrategy(RetryStrategy.builder()
                .attempts(1)
                .build())
            .propagateTags(true)
            .tags(tags)
            .build();
    }

    static SubmitJobRequest submitJob(
        RemoteJobKey key, String jobDefinitionArn, String jobQueue, Map<String, String> tags) {

        return SubmitJobRequest.builder()
            .jobName(AwsBatchJobNames.jobName(key))
            .jobDefinition(jobDefinitionArn)
            .jobQueue(jobQueue)
            .propagateTags(true)
            .tags(tags)
            .build();
    }

    private static List<ResourceRequirement> resources(BigDecimal vcpu, int memory) {
        return List.of(
            ResourceRequirement.builder()
                .type(ResourceType.VCPU)
                .value(vcpu.stripTrailingZeros()
                    .toPlainString())
                .build(),
            ResourceRequirement.builder()
                .type(ResourceType.MEMORY)
                .value(String.valueOf(memory))
                .build());
    }

    private static MountPoint workMount() {
        return MountPoint.builder()
            .sourceVolume(VOLUME_NAME)
            .containerPath(WORK_DIRECTORY)
            .build();
    }
}
