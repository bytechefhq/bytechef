/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AwsBatchRunnerSettingsTest {

    private static final String QUEUE = "arn:aws:batch:eu-west-1:123456789012:job-queue/bytechef";
    private static final String EXECUTION_ROLE = "arn:aws:iam::123456789012:role/ecsTaskExecutionRole";

    @Test
    void testDefaultsFollowTheLanguageAndFargate() {
        AwsBatchRunnerSettings settings = settings("python", Map.of());

        assertThat(settings.image()).isEqualTo("python:3.12-slim");
        assertThat(settings.platform()).isEqualTo(AwsBatchRunnerSettings.Platform.FARGATE);
        assertThat(settings.vcpu()).isEqualByComparingTo("1");
        assertThat(settings.memory()).isEqualTo(2048);
        assertThat(settings.assignPublicIp()).isFalse();
        assertThat(settings.completionCheckInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(settings.fileSidecarImage()).isEqualTo("curlimages/curl:8.11.1");
        assertThat(settings("shell", Map.of()).image()).isEqualTo("ubuntu:24.04");
        assertThat(settings("js", Map.of()).image()).isEqualTo("node:22-alpine");
    }

    @Test
    void testTheSidecarsAreSubtractedFromMain() {
        AwsBatchRunnerSettings settings = settings("python", Map.of("vcpu", "2", "memory", 4096));

        assertThat(settings.mainVcpu()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(settings.mainMemory()).isEqualTo(4096 - 128);
    }

    @Test
    void testAnInvalidFargateCombinationIsRejectedWithTheAllowedMemories() {
        assertThatThrownBy(() -> settings("python", Map.of("vcpu", "1", "memory", 1024)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2048")
            .hasMessageContaining("8192");

        assertThatThrownBy(() -> settings("python", Map.of("vcpu", "3", "memory", 8192)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vcpu");
    }

    @Test
    void testATotalThatLeavesMainTooLittleIsRejected() {
        assertThatThrownBy(() -> settings("python", Map.of("vcpu", "0.5", "memory", 1024)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0.25");

        assertThatThrownBy(
            () -> settings("python", Map.of("platform", "EC2", "vcpu", "1", "memory", 200)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128");
    }

    @Test
    void testEveryFargateSizeIsFoundWhateverItsScale() {
        assertThat(settings("python", Map.of("vcpu", "16", "memory", 32768)).mainVcpu()).isEqualByComparingTo("15.5");
        assertThat(settings("python", Map.of("vcpu", "2.0", "memory", 4096)).mainVcpu()).isEqualByComparingTo("1.5");
    }

    @Test
    void testEc2AcceptsAnySizeThatLeavesMainEnough() {
        AwsBatchRunnerSettings settings =
            settings("python", Map.of("platform", "EC2", "executionRoleArn", "", "vcpu", "3", "memory", 3000));

        assertThat(settings.mainVcpu()).isEqualByComparingTo("2.5");
        assertThat(settings.executionRoleArn()).isNull();
    }

    @Test
    void testFargateNeedsAnExecutionRole() {
        Map<String, Object> parameters = new HashMap<>(base());

        parameters.remove("executionRoleArn");

        assertThatThrownBy(() -> AwsBatchRunnerSettings.of("python", ParametersFactory.create(parameters)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("execution role");
    }

    @Test
    void testTheQueueAndTheBucketAreRequiredAndChecked() {
        Map<String, Object> noQueue = new HashMap<>(base());

        noQueue.remove("jobQueue");

        assertThatThrownBy(() -> AwsBatchRunnerSettings.of("python", ParametersFactory.create(noQueue)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("job queue");

        Map<String, Object> badBucket = new HashMap<>(base());

        badBucket.put("bucket", "Not_A_Bucket");

        assertThatThrownBy(() -> AwsBatchRunnerSettings.of("python", ParametersFactory.create(badBucket)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Not_A_Bucket");
    }

    @Test
    void testAQueueNameOrAnArnIsAccepted() {
        assertThat(settings("python", Map.of("jobQueue", "bytechef")).jobQueue()).isEqualTo("bytechef");
        assertThat(settings("python", Map.of()).jobQueue()).isEqualTo(QUEUE);
    }

    @Test
    void testAMalformedIntervalOrPlatformIsRejected() {
        assertThatThrownBy(() -> settings("python", Map.of("completionCheckInterval", "often")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("completionCheckInterval");

        assertThatThrownBy(() -> settings("python", Map.of("platform", "LAMBDA")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LAMBDA");
    }

    @Test
    void testPropertiesAreFreshAndCoverEverySetting() {
        assertThat(AwsBatchRunnerSettings.properties()
            .getFirst()).isNotSameAs(AwsBatchRunnerSettings.properties()
                .getFirst());
        assertThat(AwsBatchRunnerSettings.properties()).extracting(ValueProperty::getName)
            .containsExactly(
                "image", "entrypoint", "jobQueue", "bucket", "platform", "executionRoleArn", "taskRoleArn", "vcpu",
                "memory", "assignPublicIp", "completionCheckInterval", "fileSidecarImage");
    }

    private static Map<String, Object> base() {
        return Map.of("jobQueue", QUEUE, "bucket", "bytechef-staging", "executionRoleArn", EXECUTION_ROLE);
    }

    private static AwsBatchRunnerSettings settings(String languageId, Map<String, ?> overrides) {
        Map<String, Object> parameters = new HashMap<>(base());

        parameters.putAll(overrides);

        return AwsBatchRunnerSettings.of(languageId, ParametersFactory.create(parameters));
    }
}
