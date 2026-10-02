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

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.io.IOAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class PolyglotSandbox {

    private static final Logger log = LoggerFactory.getLogger(PolyglotSandbox.class);

    private static final Set<String> CONSTRAINED_LANGUAGE_IDS = Set.of("js", "python");

    private static final HostAccess CONSTRAINED_HOST_ACCESS = HostAccess.newBuilder(HostAccess.NONE)
        .allowMutableTargetMappings()
        .build();

    private static final Map<String, Engine> ENGINES = new ConcurrentHashMap<>();

    private static final Object GUEST_EXECUTOR_LOCK = new Object();

    private static final ScheduledExecutorService WATCHDOG_SCHEDULER = newWatchdogScheduler();

    private static final ExecutorService WATCHDOG_CLOSE_EXECUTOR = Executors.newCachedThreadPool(
        newDaemonThreadFactory("polyglot-sandbox-watchdog-close"));

    private static volatile PolyglotSandboxSettings settings = PolyglotSandboxSettings.defaults();

    @Nullable
    private static volatile PolyglotGuestExecutor guestExecutor;

    private PolyglotSandbox() {
    }

    public static void setSettings(PolyglotSandboxSettings polyglotSandboxSettings) {
        settings = polyglotSandboxSettings;

        ENGINES.clear();

        synchronized (GUEST_EXECUTOR_LOCK) {
            PolyglotGuestExecutor previousGuestExecutor = guestExecutor;

            guestExecutor = null;

            if (previousGuestExecutor != null) {
                previousGuestExecutor.shutdown();
            }
        }
    }

    public static <V> V call(String languageId, Function<Context, V> guestFunction) {
        return call(ScriptSandboxMode.STRICT, languageId, guestFunction);
    }

    public static <V> V call(ScriptSandboxMode mode, String languageId, Function<Context, V> guestFunction) {
        return call(mode, languageId, null, guestFunction);
    }

    public static <V> V call(
        ScriptSandboxMode mode, String languageId, @Nullable Duration timeout, Function<Context, V> guestFunction) {

        PolyglotSandboxSettings currentSettings = settings;

        if (requiresGuestThread(currentSettings, mode, languageId)) {
            PolyglotGuestExecutor polyglotGuestExecutor = getGuestExecutor(currentSettings);

            return polyglotGuestExecutor.call(
                () -> callInline(currentSettings, mode, timeout, guestFunction, languageId));
        }

        return callInline(currentSettings, mode, timeout, guestFunction, languageId);
    }

    private static <V> V callInline(
        PolyglotSandboxSettings currentSettings, ScriptSandboxMode mode, @Nullable Duration timeout,
        Function<Context, V> guestFunction, String... permittedLanguages) {

        try (Context context = newContext(currentSettings, mode, permittedLanguages)) {
            if (timeout == null) {
                return guestFunction.apply(context);
            }

            ScheduledFuture<?> watchdogFuture = WATCHDOG_SCHEDULER.schedule(
                () -> submitWatchdogClose(context), timeout.toMillis(), TimeUnit.MILLISECONDS);

            try {
                return guestFunction.apply(context);
            } finally {
                watchdogFuture.cancel(false);
            }
        }
    }

    private static void submitWatchdogClose(Context context) {
        try {
            WATCHDOG_CLOSE_EXECUTOR.execute(() -> context.close(true));
        } catch (RuntimeException | Error exception) {
            log.error(
                "Could not schedule the guest execution's timeout close; the execution is no longer bounded by its " +
                    "wall-clock timeout",
                exception);
        }
    }

    private static Context newContext(
        PolyglotSandboxSettings currentSettings, ScriptSandboxMode mode, String... permittedLanguages) {

        if (mode == ScriptSandboxMode.TRUSTED) {
            return newTrustedContext(permittedLanguages);
        }

        boolean constrained = isConstrained(currentSettings, mode, permittedLanguages);

        Context.Builder builder = Context.newBuilder(permittedLanguages)
            .engine(getEngine(constrained, mode, permittedLanguages))
            .allowHostAccess(constrained ? CONSTRAINED_HOST_ACCESS : HostAccess.NONE)
            .allowHostClassLoading(false)
            .allowHostClassLookup(className -> false)
            .allowNativeAccess(false)
            .allowCreateThread(false)
            .allowCreateProcess(false)
            .allowIO(IOAccess.NONE)
            .allowEnvironmentAccess(EnvironmentAccess.NONE)
            .allowPolyglotAccess(PolyglotAccess.NONE);

        if (constrained) {
            applySandboxPolicy(builder, currentSettings);
        }

        return builder.build();
    }

    private static Context newTrustedContext(String... permittedLanguages) {
        return Context.newBuilder(permittedLanguages)
            .engine(getEngine(false, ScriptSandboxMode.TRUSTED, permittedLanguages))
            .allowAllAccess(true)
            .in(new ByteArrayInputStream(new byte[0]))
            .out(newGuestOutputStream(false))
            .err(newGuestOutputStream(true))
            .build();
    }

    private static void applySandboxPolicy(Context.Builder builder, PolyglotSandboxSettings currentSettings) {
        builder.sandbox(SandboxPolicy.CONSTRAINED)
            .in(new ByteArrayInputStream(new byte[0]))
            .out(newGuestOutputStream(false))
            .err(newGuestOutputStream(true));

        Duration maxCpuTime = currentSettings.maxCpuTime();

        if (maxCpuTime != null) {
            builder.option("sandbox.MaxCPUTime", maxCpuTime.toMillis() + "ms");
        }

        Long maxHeapMemory = currentSettings.maxHeapMemory();

        if (maxHeapMemory != null) {
            builder.option("sandbox.MaxHeapMemory", maxHeapMemory + "B");
        }
    }

    private static boolean requiresGuestThread(
        PolyglotSandboxSettings currentSettings, ScriptSandboxMode mode, String... permittedLanguages) {

        Thread currentThread = Thread.currentThread();

        if (!currentThread.isVirtual()) {
            return false;
        }

        return isConstrained(currentSettings, mode, permittedLanguages) && isThreadMetered(currentSettings);
    }

    private static boolean isThreadMetered(PolyglotSandboxSettings currentSettings) {
        return currentSettings.maxCpuTime() != null || currentSettings.maxHeapMemory() != null;
    }

    private static PolyglotGuestExecutor getGuestExecutor(PolyglotSandboxSettings currentSettings) {
        PolyglotGuestExecutor currentGuestExecutor = guestExecutor;

        if (currentGuestExecutor != null) {
            return currentGuestExecutor;
        }

        synchronized (GUEST_EXECUTOR_LOCK) {
            currentGuestExecutor = guestExecutor;

            if (currentGuestExecutor == null) {
                currentGuestExecutor = new PolyglotGuestExecutor(currentSettings.maxConcurrentExecutions());

                guestExecutor = currentGuestExecutor;
            }

            return currentGuestExecutor;
        }
    }

    private static boolean isConstrained(
        PolyglotSandboxSettings currentSettings, ScriptSandboxMode mode, String... permittedLanguages) {

        if (mode == ScriptSandboxMode.TRUSTED) {
            return false;
        }

        if (!currentSettings.enabled() || permittedLanguages.length == 0) {
            return false;
        }

        for (String permittedLanguage : permittedLanguages) {
            if (!CONSTRAINED_LANGUAGE_IDS.contains(permittedLanguage)) {
                return false;
            }
        }

        return true;
    }

    private static Engine getEngine(boolean constrained, ScriptSandboxMode mode, String... permittedLanguages) {
        return ENGINES.computeIfAbsent(
            toEngineKey(constrained, mode, permittedLanguages), key -> newEngine(constrained, permittedLanguages));
    }

    private static String toEngineKey(boolean constrained, ScriptSandboxMode mode, String... permittedLanguages) {
        List<String> sortedLanguages = Arrays.stream(permittedLanguages)
            .sorted()
            .toList();

        return constrained + ":" + mode + ":" + String.join(",", sortedLanguages);
    }

    private static Engine newEngine(boolean constrained, String... permittedLanguages) {
        if (!constrained) {
            return Engine.create();
        }

        return Engine.newBuilder(permittedLanguages)
            .sandbox(SandboxPolicy.CONSTRAINED)
            .in(new ByteArrayInputStream(new byte[0]))
            .out(newGuestOutputStream(false))
            .err(newGuestOutputStream(true))
            .build();
    }

    private static OutputStream newGuestOutputStream(boolean error) {
        return new GuestLoggingOutputStream(line -> {
            if (error) {
                log.warn("[guest] {}", line);
            } else if (log.isInfoEnabled()) {
                log.info("[guest] {}", line);
            }
        });
    }

    private static ScheduledThreadPoolExecutor newWatchdogScheduler() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
            1, newDaemonThreadFactory("polyglot-sandbox-watchdog"));

        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);

        return scheduledThreadPoolExecutor;
    }

    private static ThreadFactory newDaemonThreadFactory(String threadName) {
        return runnable -> {
            Thread thread = new Thread(runnable, threadName);

            thread.setDaemon(true);

            return thread;
        };
    }

}
