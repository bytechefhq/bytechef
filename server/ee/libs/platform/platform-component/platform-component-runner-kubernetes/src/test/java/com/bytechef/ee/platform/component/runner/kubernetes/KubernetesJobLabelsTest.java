/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class KubernetesJobLabelsTest {

    @Test
    void testAKeyThatIsAValidLabelValueIsUsedAsIs() {
        assertThat(KubernetesJobLabels.labelValue("public-100-7")).isEqualTo("public-100-7");
    }

    @Test
    void testATooLongKeyIsHashed() {
        String key = "t".repeat(60) + "-100-7";

        String labelValue = KubernetesJobLabels.labelValue(key);

        assertThat(labelValue).matches("k-[0-9a-f]{16}");
        assertThat(KubernetesJobLabels.labelValue(key)).isEqualTo(labelValue);
        assertThat(KubernetesJobLabels.labelValue(key + "x")).isNotEqualTo(labelValue);
    }

    @Test
    void testAKeyWithCharactersALabelCannotHoldIsHashed() {
        assertThat(KubernetesJobLabels.labelValue("tenant/a-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(KubernetesJobLabels.labelValue("_tenant-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(KubernetesJobLabels.labelValue("tenant-1-7.")).matches("k-[0-9a-f]{16}");
    }

    @Test
    void testLabelsCarryTheManagerTheKeyAndTheJob() {
        assertThat(KubernetesJobLabels.labels(new RemoteJobKey("public-100-7", 100L)))
            .containsEntry("bytechef.io/managed-by", "bytechef")
            .containsEntry("bytechef.io/task-execution-id", "public-100-7")
            .containsEntry("bytechef.io/job-id", "100");

        assertThat(KubernetesJobLabels.labels(new RemoteJobKey("random", null)))
            .doesNotContainKey("bytechef.io/job-id");
    }

    @Test
    void testTheSelectorIsTheManagerAndTheKey() {
        assertThat(KubernetesJobLabels.selector(new RemoteJobKey("public-100-7", 100L)))
            .containsOnlyKeys("bytechef.io/managed-by", "bytechef.io/task-execution-id");
    }
}
