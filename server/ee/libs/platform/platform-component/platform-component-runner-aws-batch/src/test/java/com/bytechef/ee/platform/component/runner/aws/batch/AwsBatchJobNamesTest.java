/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AwsBatchJobNamesTest {

    @Test
    void testAKeyThatFitsAJobNameIsUsedAsIs() {
        assertThat(AwsBatchJobNames.nameValue("public-100-7")).isEqualTo("public-100-7");
        assertThat(AwsBatchJobNames.jobName(new RemoteJobKey("public-100-7", 100L))).isEqualTo("bytechef-public-100-7");
    }

    @Test
    void testAKeyThatDoesNotFitIsHashed() {
        assertThat(AwsBatchJobNames.nameValue("t".repeat(120))).matches("k-[0-9a-f]{16}");
        assertThat(AwsBatchJobNames.nameValue("tenant.a-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(AwsBatchJobNames.nameValue("-tenant-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(AwsBatchJobNames.nameValue("tenant.a-1-7")).isEqualTo(AwsBatchJobNames.nameValue("tenant.a-1-7"));
    }

    @Test
    void testEachRunGetsItsOwnPrefixUnderTheJob() {
        RemoteJobKey key = new RemoteJobKey("public-100-7", 100L);

        String first = AwsBatchJobNames.newPrefix(key);
        String second = AwsBatchJobNames.newPrefix(key);

        assertThat(first).matches("bytechef/100/public-100-7-[0-9a-f]{8}/");
        assertThat(first).isNotEqualTo(second);
        assertThat(AwsBatchJobNames.newPrefix(new RemoteJobKey("random", null))).startsWith("bytechef/none/random-");
    }

    @Test
    void testTagsCarryTheManagerTheFullKeyTheJobAndThePrefix() {
        assertThat(AwsBatchJobNames.tags(new RemoteJobKey("tenant.a-1-7", 1L), "bytechef/1/x/"))
            .containsEntry("bytechef.io/managed-by", "bytechef")
            .containsEntry("bytechef.io/key", "tenant.a-1-7")
            .containsEntry("bytechef.io/job-id", "1")
            .containsEntry("bytechef.io/prefix", "bytechef/1/x/");

        assertThat(AwsBatchJobNames.tags(new RemoteJobKey("random", null), "p/"))
            .doesNotContainKey("bytechef.io/job-id");
    }
}
