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

package com.bytechef.platform.ai.sensitivedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Ivica Cardic
 */
class MatchDeadlineTest {

    private static final Pattern CATASTROPHIC = Pattern.compile("(x+x+)+y");

    private static final String CATASTROPHIC_INPUT = "x".repeat(1000);

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAMatchRunningPastTheDeadlineIsInterrupted() {
        MatchDeadline deadline = MatchDeadline.in(Duration.ofMillis(100));

        long start = System.nanoTime();

        assertThatThrownBy(
            () -> CATASTROPHIC.matcher(deadline.bound(CATASTROPHIC_INPUT))
                .find())
                    .isInstanceOf(DetectionTimeoutException.class)
                    .hasMessageContaining("deadline");

        assertThat(Duration.ofNanos(System.nanoTime() - start))
            .isLessThan(Duration.ofSeconds(1));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAnAlreadyExpiredDeadlineInterruptsImmediately() {
        MatchDeadline deadline = MatchDeadline.at(System.nanoTime() - 1);

        assertThatThrownBy(
            () -> CATASTROPHIC.matcher(deadline.bound(CATASTROPHIC_INPUT))
                .find())
                    .isInstanceOf(DetectionTimeoutException.class);
    }

    @Test
    void testAnOrdinaryMatchIsUnaffected() {
        Pattern email = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
        MatchDeadline deadline = MatchDeadline.in(Duration.ofSeconds(30));

        var matcher = email.matcher(deadline.bound("write to bob@acme.io today"));

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("bob@acme.io");
        assertThat(matcher.start()).isEqualTo(9);
        assertThat(matcher.end()).isEqualTo(20);
    }

    @Test
    void testAnUnboundedDeadlineHandsBackTheStringItself() {
        String text = "write to bob@acme.io today";

        assertThat(MatchDeadline.unbounded()
            .bound(text)).isSameAs(text);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAnUnboundedDeadlineDoesNotInterrupt() {
        assertThatCode(
            () -> CATASTROPHIC.matcher(
                MatchDeadline.unbounded()
                    .bound("x".repeat(50)))
                .find())
                    .doesNotThrowAnyException();
    }

    @Test
    void testTheBoundedViewReportsTheSameContentAsTheString() {
        String text = "write to bob@acme.io today";
        CharSequence bounded = MatchDeadline.in(Duration.ofSeconds(30))
            .bound(text);

        assertThat(bounded.length()).isEqualTo(text.length());
        assertThat(bounded.charAt(0)).isEqualTo('w');
        assertThat(bounded.subSequence(9, 20)).isEqualTo("bob@acme.io");
        assertThat(bounded).hasToString(text);
    }

    @Test
    void testExpiredReportsTheDeadlineForBetweenUnitChecks() {
        assertThat(MatchDeadline.at(System.nanoTime() - 1)
            .expired()).isTrue();
        assertThat(MatchDeadline.in(Duration.ofSeconds(30))
            .expired()).isFalse();
        assertThat(MatchDeadline.unbounded()
            .expired()).isFalse();
    }
}
