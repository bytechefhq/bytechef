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

package com.bytechef.platform.component.polyglot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.context.ContextRegistry;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class PolyglotSandboxTest {

    @AfterEach
    public void afterEach() {
        PolyglotSandbox.setSettings(PolyglotSandboxSettings.defaults());
    }

    @Test
    public void testContextRestrictionsPinned() {
        PolyglotSandbox.call("js", context -> {
            Value value = context.eval("js", "1 + 1");

            assertThat(value.asInt()).isEqualTo(2);

            assertThatThrownBy(() -> context.eval("js", "Java.type('java.lang.System')"))
                .isInstanceOf(PolyglotException.class);

            return null;
        });
    }

    @Test
    public void testConstrainedContextEnforcesCpuLimit() {
        PolyglotSandbox.setSettings(
            new PolyglotSandboxSettings(
                true, Duration.ofSeconds(1), PolyglotSandboxSettings.DEFAULT_MAX_HEAP_MEMORY,
                PolyglotSandboxSettings.DEFAULT_MAX_CONCURRENT_EXECUTIONS));

        assertThatThrownBy(() -> PolyglotSandbox.call("js", context -> context.eval("js", "while (true) {}")))
            .isInstanceOf(PolyglotException.class)
            .hasMessageContaining("CPU time limit");
    }

    @Test
    public void testConstrainedContextEnforcesHeapLimit() {
        PolyglotSandbox.setSettings(
            new PolyglotSandboxSettings(
                true, PolyglotSandboxSettings.DEFAULT_MAX_CPU_TIME, 64L * 1024 * 1024,
                PolyglotSandboxSettings.DEFAULT_MAX_CONCURRENT_EXECUTIONS));

        assertThatThrownBy(
            () -> PolyglotSandbox.call(
                "js",
                context -> context.eval("js", "const a = []; while (true) { a.push(new Array(100000).fill(1)); }")))
                    .isInstanceOf(PolyglotException.class)
                    .hasMessageContaining("heap memory limit");
    }

    @Test
    public void testConstrainedContextBuildsWhenCalledFromVirtualThread() {
        int value = onVirtualThread(() -> PolyglotSandbox.call("js", context -> context.eval("js", "1 + 1")
            .asInt()));

        assertThat(value).isEqualTo(2);
    }

    @Test
    public void testGuestRunsOnPlatformThreadWhenCalledFromVirtualThread() {
        Boolean guestThreadIsVirtual = onVirtualThread(() -> PolyglotSandbox.call("js", context -> {
            Thread currentThread = Thread.currentThread();

            return currentThread.isVirtual();
        }));

        assertThat(guestThreadIsVirtual).isFalse();
    }

    @Test
    public void testCpuLimitStillEnforcedWhenCalledFromVirtualThread() {
        PolyglotSandbox.setSettings(
            new PolyglotSandboxSettings(
                true, Duration.ofSeconds(1), PolyglotSandboxSettings.DEFAULT_MAX_HEAP_MEMORY,
                PolyglotSandboxSettings.DEFAULT_MAX_CONCURRENT_EXECUTIONS));

        assertThatThrownBy(
            () -> onVirtualThread(() -> PolyglotSandbox.call("js", context -> context.eval("js", "while (true) {}"))))
                .isInstanceOf(PolyglotException.class)
                .hasMessageContaining("CPU time limit");
    }

    @Test
    public void testThreadLocalStateCrossesTheHop() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();

        ContextRegistry contextRegistry = ContextRegistry.getInstance();

        contextRegistry.registerThreadLocalAccessor("test.polyglot", threadLocal);

        try {
            String observed = onVirtualThread(() -> {
                threadLocal.set("tenant-42");

                return PolyglotSandbox.call("js", context -> threadLocal.get());
            });

            assertThat(observed).isEqualTo("tenant-42");
        } finally {
            contextRegistry.removeThreadLocalAccessor("test.polyglot");
        }
    }

    @Test
    public void testSandboxedContextRejectsMutableTargetMappings() {
        PolyglotSandbox.call("js", context -> {
            Value value = context.eval("js", "({ a: 1 })");

            assertThatThrownBy(() -> value.as(Map.class)).isInstanceOf(ClassCastException.class);

            return null;
        });
    }

    @Test
    public void testDisabledSandboxFallsBackToTrustedContext() {
        PolyglotSandbox.setSettings(PolyglotSandboxSettings.disabled());

        PolyglotSandbox.call("js", context -> {
            Value value = context.eval("js", "({ a: 1 })");

            assertThat(value.as(Map.class)).containsEntry("a", 1);

            assertThatThrownBy(() -> context.eval("js", "Java.type('java.lang.System')"))
                .isInstanceOf(PolyglotException.class);

            return null;
        });
    }

    @Test
    public void testStrictModeDeniesHostClassLookup() {
        assertThatThrownBy(
            () -> PolyglotSandbox.call(
                ScriptSandboxMode.STRICT, "js", context -> context.eval("js", "Java.type('java.lang.System')")))
                    .isInstanceOf(PolyglotException.class);
    }

    @Test
    public void testTrustedModeAllowsHostClassLookup() {
        Value value = PolyglotSandbox.call(
            ScriptSandboxMode.TRUSTED, "js", null,
            context -> context.eval("js", "Java.type('java.lang.System').getProperty('java.version')"));

        assertThat(value.asString()).isNotBlank();
    }

    @Test
    public void testLegacyCallDefaultsToStrictMode() {
        assertThatThrownBy(
            () -> PolyglotSandbox.call("js", context -> context.eval("js", "Java.type('java.lang.System')")))
                .isInstanceOf(PolyglotException.class);
    }

    @Test
    public void testTrustedModeTimeoutCancelsRunawayScript() {
        assertThatThrownBy(
            () -> PolyglotSandbox.call(
                ScriptSandboxMode.TRUSTED, "js", Duration.ofSeconds(2),
                context -> context.eval("js", "while (true) {}")))
                    .isInstanceOf(PolyglotException.class)
                    .matches(throwable -> ((PolyglotException) throwable).isCancelled(), "isCancelled");
    }

    @Test
    public void testTrustedModeRoutesGuestOutputThroughTheGuestLogger() {
        ch.qos.logback.classic.Logger polyglotSandboxLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PolyglotSandbox.class);

        Level previousLevel = polyglotSandboxLogger.getLevel();
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

        logAppender.start();
        polyglotSandboxLogger.addAppender(logAppender);
        polyglotSandboxLogger.setLevel(Level.INFO);

        try {
            PolyglotSandbox.call(
                ScriptSandboxMode.TRUSTED, "js", null,
                context -> context.eval("js", "print('trusted-guest-line')"));

            assertThat(getFormattedMessages(logAppender)).contains("[guest] trusted-guest-line");
        } finally {
            polyglotSandboxLogger.setLevel(previousLevel);
            polyglotSandboxLogger.detachAppender(logAppender);
        }
    }

    @Test
    public void testContextStreamsOverrideTheEngineWithoutMutatingIt() {
        ByteArrayOutputStream engineOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream contextOutputStream = new ByteArrayOutputStream();

        try (Engine engine = Engine.newBuilder("js")
            .out(engineOutputStream)
            .build()) {

            try (Context overridingContext = Context.newBuilder("js")
                .engine(engine)
                .out(contextOutputStream)
                .build()) {

                overridingContext.eval("js", "print('overridden-line')");
            }

            try (Context inheritingContext = Context.newBuilder("js")
                .engine(engine)
                .build()) {

                inheritingContext.eval("js", "print('inherited-line')");
            }
        }

        assertThat(contextOutputStream.toString(StandardCharsets.UTF_8))
            .contains("overridden-line")
            .doesNotContain("inherited-line");
        assertThat(engineOutputStream.toString(StandardCharsets.UTF_8))
            .contains("inherited-line")
            .doesNotContain("overridden-line");
    }

    @Test
    public void testTrustedAndStrictContextsDoNotShareAnEngine() {
        PolyglotSandbox.setSettings(PolyglotSandboxSettings.disabled());

        Value trustedValue = PolyglotSandbox.call(
            ScriptSandboxMode.TRUSTED, "js", null, context -> context.eval("js", "1 + 1"));

        assertThat(trustedValue.asInt()).isEqualTo(2);

        Value strictValue =
            PolyglotSandbox.call(ScriptSandboxMode.STRICT, "js", context -> context.eval("js", "2 + 2"));

        assertThat(strictValue.asInt()).isEqualTo(4);
    }

    @Test
    public void testTrustedModeIgnoresResourceCeilings() {
        PolyglotSandbox.setSettings(
            new PolyglotSandboxSettings(
                true, Duration.ofSeconds(1), PolyglotSandboxSettings.DEFAULT_MAX_HEAP_MEMORY,
                PolyglotSandboxSettings.DEFAULT_MAX_CONCURRENT_EXECUTIONS));

        String loopScript = "let total = 0; for (let i = 0; i < 20000000; i++) { total += i; } total";

        assertThatThrownBy(
            () -> PolyglotSandbox.call(ScriptSandboxMode.STRICT, "js", context -> context.eval("js", loopScript)))
                .isInstanceOf(PolyglotException.class)
                .hasMessageContaining("CPU time limit");

        Value value = PolyglotSandbox.call(
            ScriptSandboxMode.TRUSTED, "js", null, context -> context.eval("js", loopScript));

        assertThat(value.asDouble()).isEqualTo(199999990000000.0);
    }

    private static List<String> getFormattedMessages(ListAppender<ILoggingEvent> logAppender) {
        return logAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    private static <V> V onVirtualThread(Supplier<V> supplier) {
        AtomicReference<V> resultReference = new AtomicReference<>();
        AtomicReference<Throwable> throwableReference = new AtomicReference<>();

        Thread thread = Thread.ofVirtual()
            .unstarted(() -> {
                try {
                    resultReference.set(supplier.get());
                } catch (Throwable throwable) {
                    throwableReference.set(throwable);
                }
            });

        thread.start();

        try {
            thread.join();
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();

            throw new IllegalStateException(interruptedException);
        }

        Throwable throwable = throwableReference.get();

        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        if (throwable != null) {
            throw new IllegalStateException(throwable);
        }

        return resultReference.get();
    }
}
