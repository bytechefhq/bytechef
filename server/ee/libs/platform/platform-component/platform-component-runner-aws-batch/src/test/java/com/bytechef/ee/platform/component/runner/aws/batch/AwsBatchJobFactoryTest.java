/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.batch.model.AssignPublicIp;
import software.amazon.awssdk.services.batch.model.JobDefinitionType;
import software.amazon.awssdk.services.batch.model.KeyValuePair;
import software.amazon.awssdk.services.batch.model.PlatformCapability;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.ResourceRequirement;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.batch.model.TaskContainerProperties;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AwsBatchJobFactoryTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);

    @Test
    void testTheDefinitionIsAnEcsMultiContainerJobOnFargate() throws Exception {
        RegisterJobDefinitionRequest request = definition(Map.of());

        assertThat(request.jobDefinitionName()).isEqualTo("bytechef-public-100-7");
        assertThat(request.type()).isEqualTo(JobDefinitionType.CONTAINER);
        assertThat(request.platformCapabilities()).containsExactly(PlatformCapability.FARGATE);
        assertThat(request.ecsProperties()
            .taskProperties()
            .getFirst()
            .executionRoleArn()).isEqualTo("arn:aws:iam::123456789012:role/exec");
        assertThat(request.ecsProperties()
            .taskProperties()
            .getFirst()
            .networkConfiguration()
            .assignPublicIp()).isEqualTo(AssignPublicIp.DISABLED);
        assertThat(request.timeout()
            .attemptDurationSeconds()).isEqualTo(3600 + 60);
        assertThat(request.retryStrategy()
            .attempts()).isEqualTo(1);
        assertThat(request.propagateTags()).isTrue();
        assertThat(request.tags()).containsEntry("bytechef.io/key", "public-100-7");
    }

    @Test
    void testOnlyTheOutputContainerIsEssentialAndTheOrderIsEnforced() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(definition(Map.of()));

        assertThat(containers).containsOnlyKeys("bytechef-init", "main", "bytechef-output");
        assertThat(containers.get("bytechef-init")
            .essential()).isFalse();
        assertThat(containers.get("main")
            .essential()).isFalse();
        assertThat(containers.get("bytechef-output")
            .essential()).isTrue();
        assertThat(containers.get("main")
            .dependsOn()
            .getFirst()
            .containerName()).isEqualTo("bytechef-init");
        assertThat(containers.get("main")
            .dependsOn()
            .getFirst()
            .condition()).isEqualTo("SUCCESS");
        assertThat(containers.get("bytechef-output")
            .dependsOn()
            .getFirst()
            .containerName()).isEqualTo("main");
        assertThat(containers.get("bytechef-output")
            .dependsOn()
            .getFirst()
            .condition()).isEqualTo("COMPLETE");
    }

    @Test
    void testTheContainersMoveTheFilesThroughThePresignedUrls() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(definition(Map.of()));

        assertThat(String.join(" ", containers.get("bytechef-init")
            .command())).contains("curl -fsS \"$BYTECHEF_INPUT_URL\"")
                .contains("tar -xf - -C /bytechef/work");
        assertThat(environment(containers.get("bytechef-init"))).containsEntry(
            "BYTECHEF_INPUT_URL", "https://bucket.s3.amazonaws.com/in?sig=1");

        String outputCommand = String.join(" ", containers.get("bytechef-output")
            .command());

        assertThat(outputCommand).contains("tar -cf output.tar")
            .contains("curl -fsS -T output.tar \"$BYTECHEF_OUTPUT_URL\"");
        assertThat(environment(containers.get("bytechef-output"))).containsEntry(
            "BYTECHEF_OUTPUT_URL", "https://bucket.s3.amazonaws.com/out?sig=2");

        assertThat(environment(containers.get("main"))).doesNotContainKeys("BYTECHEF_INPUT_URL", "BYTECHEF_OUTPUT_URL")
            .containsEntry("BYTECHEF_INPUT_FILE", "/bytechef/work/input.json");
    }

    @Test
    void testConnectionVariablesReachOnlyTheMainContainer() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(
            definition(Map.of(), Map.of("BYTECHEF_CONNECTION_SLACK_TOKEN", "xoxb-123456")));

        assertThat(environment(containers.get("main"))).containsEntry("BYTECHEF_CONNECTION_SLACK_TOKEN", "xoxb-123456");
        assertThat(environment(containers.get("bytechef-init"))).doesNotContainKey("BYTECHEF_CONNECTION_SLACK_TOKEN");
        assertThat(environment(containers.get("bytechef-output")))
            .doesNotContainKey("BYTECHEF_CONNECTION_SLACK_TOKEN");
    }

    @Test
    void testMainRunsTheInterpreterWithStderrRedirectedInTheAuthorsImage() throws Exception {
        TaskContainerProperties main = containers(definition(Map.of())).get("main");

        assertThat(main.image()).isEqualTo("python:3.12-slim");
        assertThat(main.command()).containsExactly(
            "sh", "-c", "cd /bytechef/work && exec \"$@\" 2>/bytechef/work/.stderr", "sh", "python3",
            "/bytechef/work/script.py");
        assertThat(main.mountPoints()
            .getFirst()
            .containerPath()).isEqualTo("/bytechef/work");
        assertThat(main.user()).isNull();
    }

    @Test
    void testTheSidecarsRunAsRootAndOpenTheWorkDirectoryToAnyUser() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(definition(Map.of()));

        assertThat(containers.get("bytechef-init")
            .user()).isEqualTo("0");
        assertThat(containers.get("bytechef-output")
            .user()).isEqualTo("0");
        assertThat(String.join(" ", containers.get("bytechef-init")
            .command())).contains("mkdir -p /bytechef/work/output && chmod 0777 /bytechef/work /bytechef/work/output");
    }

    @Test
    void testResourcesSplitTheTotalBetweenMainAndTheSidecars() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(definition(Map.of("vcpu", "2", "memory", 4096)));

        assertThat(resources(containers.get("main"))).containsEntry("VCPU", "1.5")
            .containsEntry("MEMORY", "3968");
        assertThat(resources(containers.get("bytechef-init"))).containsEntry("VCPU", "0.25")
            .containsEntry("MEMORY", "64");
        assertThat(resources(containers.get("bytechef-output"))).containsEntry("VCPU", "0.25")
            .containsEntry("MEMORY", "64");
    }

    @Test
    void testEc2HasNoNetworkConfigurationOrExecutionRoleRequirement() throws Exception {
        RegisterJobDefinitionRequest request = definition(Map.of("platform", "EC2", "executionRoleArn", ""));

        assertThat(request.platformCapabilities()).containsExactly(PlatformCapability.EC2);
        assertThat(request.ecsProperties()
            .taskProperties()
            .getFirst()
            .networkConfiguration()).isNull();
    }

    @Test
    void testTheSubmitCarriesTheJobNameQueueDefinitionAndTags() {
        SubmitJobRequest request = AwsBatchJobFactory.submitJob(
            KEY, "arn:aws:batch:eu-west-1:1:job-definition/bytechef-public-100-7:1", "bytechef",
            Map.of("bytechef.io/key", "public-100-7"));

        assertThat(request.jobName()).isEqualTo("bytechef-public-100-7");
        assertThat(request.jobQueue()).isEqualTo("bytechef");
        assertThat(request.jobDefinition()).endsWith(":1");
        assertThat(request.tags()).containsEntry("bytechef.io/key", "public-100-7");
        assertThat(request.propagateTags()).isTrue();
    }

    private static RegisterJobDefinitionRequest definition(Map<String, ?> overrides) throws Exception {
        return definition(overrides, Map.of());
    }

    private static RegisterJobDefinitionRequest definition(
        Map<String, ?> overrides, Map<String, String> extraEnvironment) throws Exception {

        Map<String, Object> parameters = new HashMap<>(
            Map.of(
                "jobQueue", "bytechef", "bucket", "bytechef-staging", "executionRoleArn",
                "arn:aws:iam::123456789012:role/exec"));

        parameters.putAll(overrides);

        AwsBatchRunnerSettings settings = AwsBatchRunnerSettings.of("python", ParametersFactory.create(parameters));

        Map<String, String> environment = new HashMap<>(
            Map.of("HOME", "/bytechef/work", "BYTECHEF_INPUT_FILE", "/bytechef/work/input.json"));

        environment.putAll(extraEnvironment);

        RemoteJobSpec spec = new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"), environment, "/bytechef/work",
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(parameters));

        URL inputUrl = URI.create("https://bucket.s3.amazonaws.com/in?sig=1")
            .toURL();
        URL outputUrl = URI.create("https://bucket.s3.amazonaws.com/out?sig=2")
            .toURL();

        return AwsBatchJobFactory.jobDefinition(
            spec, settings, AwsBatchJobNames.tags(KEY, "bytechef/100/x/"), inputUrl, outputUrl, Duration.ofHours(1));
    }

    private static Map<String, TaskContainerProperties> containers(RegisterJobDefinitionRequest request) {
        return request.ecsProperties()
            .taskProperties()
            .getFirst()
            .containers()
            .stream()
            .collect(Collectors.toMap(TaskContainerProperties::name, container -> container));
    }

    private static Map<String, String> environment(TaskContainerProperties container) {
        return container.environment()
            .stream()
            .collect(Collectors.toMap(KeyValuePair::name, KeyValuePair::value));
    }

    private static Map<String, String> resources(TaskContainerProperties container) {
        return container.resourceRequirements()
            .stream()
            .collect(Collectors.toMap(ResourceRequirement::typeAsString, ResourceRequirement::value));
    }
}
