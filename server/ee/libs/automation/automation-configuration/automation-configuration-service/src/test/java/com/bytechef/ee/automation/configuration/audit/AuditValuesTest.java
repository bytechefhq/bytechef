/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AuditValuesTest {

    @Test
    void testJoinBoundedReturnsEverythingWhenItFitsWithinTheLimit() {
        assertThat(AuditValues.joinBounded(List.of("1", "2", "3"))).isEqualTo("1,2,3");
    }

    @Test
    void testJoinBoundedStopsBeforeExceedingTheLimitWithoutCuttingAnElement() {
        List<String> ids = new ArrayList<>();

        for (int index = 0; index < 100; index++) {
            ids.add(String.format("ID%03d", index));
        }

        // Each id is 5 characters; joined with a 1-character delimiter, 42 elements reach 251 characters and a 43rd
        // would reach 257, over the 256-character limit. A naive "keep the last 256 characters" truncation would cut
        // the 43rd id in half instead of dropping it whole.
        String expectedJoined = String.join(",", ids.subList(0, 42));

        String joined = AuditValues.joinBounded(ids);

        assertThat(joined).isEqualTo(expectedJoined);
        assertThat(joined.length()).isLessThanOrEqualTo(256);
        assertThat(joined).doesNotContain(",ID042");
    }

    @Test
    void testJoinBoundedOnEmptyCollectionReturnsEmptyString() {
        assertThat(AuditValues.joinBounded(List.of())).isEmpty();
    }
}
