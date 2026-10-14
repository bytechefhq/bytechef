/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.runner.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class DeadlineTest {

    @Test
    public void testAFreshDeadlineHasTimeLeft() {
        Deadline deadline = Deadline.after(Duration.ofSeconds(30));

        assertThat(deadline.isExpired()).isFalse();
        assertThat(deadline.remainingMillis()).isBetween(29_000L, 30_000L);
        assertThat(deadline.remaining()).isPositive();
    }

    @Test
    public void testAnElapsedDeadlineReportsZeroRemaining() {
        Deadline deadline = Deadline.after(Duration.ZERO);

        assertThat(deadline.isExpired()).isTrue();
        assertThat(deadline.remainingMillis()).isZero();
    }

    @Test
    public void testCheckNamesTheStageAndTheBudget() {
        Deadline deadline = Deadline.after(Duration.ZERO);

        assertThatThrownBy(() -> deadline.check("docker", "pulling the image"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("docker")
            .hasMessageContaining("pulling the image")
            .hasMessageContaining("PT0S");
    }

    @Test
    public void testTheEarlierOfTwoDeadlinesWins() {
        Deadline later = Deadline.after(Duration.ofMinutes(10));
        Deadline earlier = Deadline.after(Duration.ofSeconds(5));

        assertThat(later.earliest(earlier)).isSameAs(earlier);
        assertThat(earlier.earliest(later)).isSameAs(earlier);
    }
}
