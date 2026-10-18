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

package com.bytechef.security.web.authentication;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caps failed TOTP code attempts per login. A six-digit code with a three-step validity window is guessable at scale,
 * and a fresh password login would otherwise reset any per-session count, so failures are counted per login and the
 * login is locked out of code verification for {@link #LOCKOUT} after {@link #MAX_FAILURES} failures.
 *
 * @author Ivica Cardic
 */
public class TotpAttemptLimiter {

    static final Duration LOCKOUT = Duration.ofMinutes(15);
    static final int MAX_FAILURES = 5;

    private final Clock clock;
    private final Map<String, Attempts> attemptsByLogin = new ConcurrentHashMap<>();

    public TotpAttemptLimiter() {
        this(Clock.systemUTC());
    }

    TotpAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isLocked(String login) {
        Attempts attempts = attemptsByLogin.get(login);

        if (attempts == null) {
            return false;
        }

        if (attempts.isExpired(clock.instant())) {
            attemptsByLogin.remove(login, attempts);

            return false;
        }

        return attempts.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String login) {
        Instant now = clock.instant();

        attemptsByLogin.compute(
            login, (key, attempts) -> attempts == null || attempts.isExpired(now)
                ? new Attempts(1, now) : new Attempts(attempts.failures() + 1, attempts.firstFailure()));
    }

    public void recordSuccess(String login) {
        attemptsByLogin.remove(login);
    }

    private record Attempts(int failures, Instant firstFailure) {

        boolean isExpired(Instant now) {
            return firstFailure.plus(LOCKOUT)
                .isBefore(now);
        }
    }
}
