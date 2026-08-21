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

import io.micrometer.context.ContextRegistry;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

        // Asserted around the whole call, the way every caller writes it: closing a cancelled context throws too,
        // and what must reach the caller is the limit that was hit, not the close failure.
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

    /**
     * The resource ceilings are metered per thread through {@code ThreadMXBean}, which does not account for virtual
     * threads - it answers {@code -1} for both CPU time and allocated bytes, and GraalVM reads that as "the host VM
     * cannot do this" and refuses to build the context at all. Every request thread in this application is virtual, so
     * without the platform-thread hop no sandboxed script runs anywhere but in a test.
     */
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

    /**
     * The guest reaches back into the platform through {@code ContextProxyObject}, and the component actions it invokes
     * are tenant-scoped and authorized off thread-bound state. The hop carries that state across, or every script that
     * calls a component fails on a thread that belongs to no tenant.
     */
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
        // The mutable target mappings CONSTRAINED forbids are what Value.as(Map.class) needs; their absence is
        // the observable difference between a sandboxed context and a trusted one, and the reason
        // PolyglotValues walks guest aggregates by hand.
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

            // The kill switch drops the sandbox policy, never the hardening below it.
            assertThatThrownBy(() -> context.eval("js", "Java.type('java.lang.System')"))
                .isInstanceOf(PolyglotException.class);

            return null;
        });
    }

    // RUBY-DISABLED: org.graalvm.polyglot:ruby is published only up to 25.0.0 and crashes on the pinned
    // Truffle 25.2.4; the ruby dependency is commented out so the language is not even installed. Remove
    // this @Disabled once a polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is
    // downgraded). Grep RUBY-DISABLED.
    @Disabled("RUBY-DISABLED")
    @Test
    public void testRubyContextAllowsHostIterationOfGuestHashes() {
        // TruffleRuby backs host-side hash iteration with a fiber-based Enumerator, so this pins the ruby-only
        // thread-creation carve-out; without it the copy below fails with "fibers not allowed with
        // allowCreateThread(false)". Host class lookup must stay denied regardless.
        PolyglotSandbox.call("ruby", context -> {
            Value value = context.eval("ruby", "{ \"x\" => 1 }");

            Map<?, ?> guestBackedMap = value.as(Map.class);

            Map<Object, Object> copiedMap = new HashMap<>(guestBackedMap);

            assertThat(copiedMap).containsKey("x");

            assertThatThrownBy(() -> context.eval("ruby", "Java.type('java.lang.System')"))
                .isInstanceOf(PolyglotException.class);

            return null;
        });
    }

    /**
     * Runs the supplier on a virtual thread and replays its outcome on the calling thread, so the assertions above read
     * the same either way.
     */
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
