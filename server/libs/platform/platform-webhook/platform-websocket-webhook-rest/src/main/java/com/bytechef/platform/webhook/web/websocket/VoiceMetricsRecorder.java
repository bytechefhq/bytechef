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

package com.bytechef.platform.webhook.web.websocket;

import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnWebhook
public class VoiceMetricsRecorder {

    public static final String PATH_WORKFLOW = "A";

    public static final String PATH_NATIVE = "B";

    public static final String PROVIDER_UNKNOWN = "unknown";

    private static final String SESSION_COUNTER = "bytechef_voice_session_total";
    private static final String SESSION_PATH_COUNTER = "bytechef_voice_session_path_total";
    private static final String TOKEN_COUNTER = "bytechef_voice_token_mint_total";
    private static final String DURATION_TIMER = "bytechef_voice_session_duration_seconds";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final ConcurrentMap<String, Counter> sessionCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> sessionPathCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenCounters = new ConcurrentHashMap<>();
    private volatile Timer durationTimer;

    public VoiceMetricsRecorder(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    public void recordSessionOpened() {
        sessionCounter("opened").ifPresent(Counter::increment);
    }

    public void recordSessionOpened(String path, String provider) {
        recordSessionOpened();

        sessionPathCounter("opened", path, provider).ifPresent(Counter::increment);
    }

    public void recordSessionClosed(Duration duration) {
        sessionCounter("closed").ifPresent(Counter::increment);

        Timer timer = durationTimer();

        if (timer != null) {
            timer.record(duration);
        }
    }

    public void recordSessionClosed(Duration duration, String path, String provider) {
        recordSessionClosed(duration);

        sessionPathCounter("closed", path, provider).ifPresent(Counter::increment);
    }

    public void recordSessionError() {
        sessionCounter("error").ifPresent(Counter::increment);
    }

    public void recordSessionError(String path, String provider) {
        recordSessionError();

        sessionPathCounter("error", path, provider).ifPresent(Counter::increment);
    }

    public void recordTokenIssued() {
        tokenCounter("issued").ifPresent(Counter::increment);
    }

    public void recordTokenConsumed() {
        tokenCounter("consumed").ifPresent(Counter::increment);
    }

    public void recordTokenRejected() {
        tokenCounter("rejected").ifPresent(Counter::increment);
    }

    private java.util.Optional<Counter> sessionCounter(String outcome) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();

        if (registry == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(sessionCounters.computeIfAbsent(outcome,
            key -> Counter.builder(SESSION_COUNTER)
                .tag("outcome", key)
                .register(registry)));
    }

    private java.util.Optional<Counter> sessionPathCounter(String outcome, String path, String provider) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();

        if (registry == null) {
            return java.util.Optional.empty();
        }

        String normalisedPath = path == null ? PATH_WORKFLOW : path;
        String normalisedProvider = provider == null ? PROVIDER_UNKNOWN : provider;
        String key = outcome + "|" + normalisedPath + "|" + normalisedProvider;

        return java.util.Optional.of(sessionPathCounters.computeIfAbsent(key,
            ignored -> Counter.builder(SESSION_PATH_COUNTER)
                .tag("outcome", outcome)
                .tag("path", normalisedPath)
                .tag("provider", normalisedProvider)
                .register(registry)));
    }

    private java.util.Optional<Counter> tokenCounter(String result) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();

        if (registry == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(tokenCounters.computeIfAbsent(result,
            key -> Counter.builder(TOKEN_COUNTER)
                .tag("result", key)
                .register(registry)));
    }

    private Timer durationTimer() {
        Timer timer = durationTimer;

        if (timer != null) {
            return timer;
        }

        MeterRegistry registry = meterRegistryProvider.getIfAvailable();

        if (registry == null) {
            return null;
        }

        timer = Timer.builder(DURATION_TIMER)
            .publishPercentileHistogram()
            .register(registry);

        durationTimer = timer;

        return timer;
    }
}
