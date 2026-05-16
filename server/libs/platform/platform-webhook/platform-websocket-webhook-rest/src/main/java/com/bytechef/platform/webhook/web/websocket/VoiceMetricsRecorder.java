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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Voice metrics. Wired via {@link ObjectProvider} so lightweight app variants that don't pull in Micrometer's
 * actuator/auto-config (some EE microservices) start cleanly — when the registry isn't present every method becomes a
 * no-op.
 *
 * <p>
 * Counters and tag values are intentionally low-cardinality:
 * <ul>
 * <li>{@code bytechef_voice_session_total{outcome=opened|closed|error}} — global session outcomes (Path A workflow
 * webhook + Path B native AI Hub voice). Cardinality: 3 outcomes.</li>
 * <li>{@code bytechef_voice_session_path_total{outcome=opened|closed|error,path=A|B,provider=DEEPGRAM|...|unknown}} —
 * path-and-provider-aware breakdown. Cardinality bounded by (3 outcomes) × (2 paths) × (n providers + "unknown"). Use
 * this to compare Path A vs Path B reliability or to slice errors by provider.</li>
 * <li>{@code bytechef_voice_token_mint_total{result=issued|consumed|rejected}}</li>
 * <li>{@code bytechef_voice_session_duration_seconds} — Timer (histogram-enabled)</li>
 * </ul>
 *
 * Per-workspace / per-workflow tags are deferred to Tier 2 since they would create unbounded cardinality in
 * multi-tenant deployments.
 *
 * @author Ivica Cardic
 */
@Component
public class VoiceMetricsRecorder {

    /** Path A — workflow-webhook routed voice (browser → workflow → STT/TTS components). */
    public static final String PATH_WORKFLOW = "A";

    /** Path B — native AI Hub voice (browser → AI Hub server → provider WS). */
    public static final String PATH_NATIVE = "B";

    /** Fallback provider tag value for Path A (no single provider — the workflow chains components). */
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

    /**
     * Path-aware variant. Increments both the global counter (so existing dashboards keep working) and the path-tagged
     * counter (so we can compare Path A vs Path B reliability and slice by provider).
     */
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
