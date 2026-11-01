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

package com.bytechef.platform.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class FailureLogRateLimiterTest {

    private static final Duration INTERVAL = Duration.ofMinutes(1);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
    private final FailureLogRateLimiter failureLogRateLimiter = new FailureLogRateLimiter(clock, INTERVAL);

    @Test
    void testFirstFailureIsLogged() {
        assertThat(failureLogRateLimiter.tryAcquire()).isEqualTo(OptionalLong.of(0));
    }

    @Test
    void testFailuresWithinTheIntervalAreSuppressedAndCountedInTheNextReport() {
        failureLogRateLimiter.tryAcquire();

        clock.advance(Duration.ofSeconds(30));

        assertThat(failureLogRateLimiter.tryAcquire()).isEmpty();
        assertThat(failureLogRateLimiter.tryAcquire()).isEmpty();

        clock.advance(Duration.ofSeconds(30));

        assertThat(failureLogRateLimiter.tryAcquire()).isEqualTo(OptionalLong.of(2));
        assertThat(failureLogRateLimiter.tryAcquire()).isEmpty();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
