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
 * Builds the strict-sandbox {@link Context} shared by every guest polyglot execution that runs user-supplied code (the
 * Script component, Script Tool and SkillsTool scripts, and code workflow tasks).
 *
 * @author Ivica Cardic
 */
public final class PolyglotSandbox {

    private static final Logger log = LoggerFactory.getLogger(PolyglotSandbox.class);

    /**
     * Languages built under {@link SandboxPolicy#CONSTRAINED}.
     *
     * <p>
     * Only the two Truffle languages whose sandboxing is exercised and supported. {@code java} (Espresso) and {@code R}
     * deliberately stay on {@code TRUSTED} - they keep every restriction below, they simply do not get the policy's
     * resource ceilings. A context permitting any language outside this set is built TRUSTED as a whole, because
     * GraalVM applies one policy per context.
     */
    private static final Set<String> CONSTRAINED_LANGUAGE_IDS = Set.of("js", "python");

    /**
     * {@link HostAccess#NONE} with mutable target mappings stripped.
     *
     * <p>
     * Every policy above {@code TRUSTED} rejects a host access that permits host object mappings of mutable target
     * types - which {@code HostAccess.NONE} does, despite its name, because the mappings are a separate axis from
     * member access. This grants no host access whatsoever; it only removes an axis the policy forbids.
     */
    private static final HostAccess CONSTRAINED_HOST_ACCESS = HostAccess.newBuilder(HostAccess.NONE)
        .allowMutableTargetMappings()
        .build();

    /**
     * Engines keyed by policy and permitted languages. GraalVM requires a context and its engine to carry the SAME
     * {@link SandboxPolicy}, so engines cannot be shared across the TRUSTED/CONSTRAINED split.
     */
    private static final Map<String, Engine> ENGINES = new ConcurrentHashMap<>();

    private static final Object GUEST_EXECUTOR_LOCK = new Object();

    private static volatile PolyglotSandboxSettings settings = PolyglotSandboxSettings.defaults();

    @Nullable
    private static volatile PolyglotGuestExecutor guestExecutor;

    private PolyglotSandbox() {
    }

    /**
     * Replaces the settings every subsequently built context uses. Called once at startup; contexts already built keep
     * the settings they were built with.
     *
     * @param polyglotSandboxSettings the settings to apply
     */
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

    /**
     * Runs the given function against a strictly sandboxed {@link Context}, on a thread the sandbox's resource ceilings
     * can be measured on, and closes the context before returning.
     *
     * <p>
     * This is the only way to reach a guest context: the ceilings GraalVM applies are metered per thread, so a context
     * must be built and evaluated on the same platform thread. Handing a {@link Context} back to the caller would let
     * guest code run on a thread the ceilings do not hold for. See {@link PolyglotGuestExecutor}.
     *
     * <p>
     * The value the function returns must not be backed by the guest context - convert guest values to host values (see
     * {@link PolyglotValues}) before returning them, or they are read after their context is closed.
     *
     * @param languageId    the language id the context is permitted to evaluate
     * @param guestFunction the guest execution
     * @return whatever the function returned
     */
    public static <V> V call(String languageId, Function<Context, V> guestFunction) {
        PolyglotSandboxSettings currentSettings = settings;

        if (requiresGuestThread(currentSettings, languageId)) {
            PolyglotGuestExecutor polyglotGuestExecutor = getGuestExecutor(currentSettings);

            return polyglotGuestExecutor.call(() -> callInline(currentSettings, guestFunction, languageId));
        }

        return callInline(currentSettings, guestFunction, languageId);
    }

    private static <V> V callInline(
        PolyglotSandboxSettings currentSettings, Function<Context, V> guestFunction, String... permittedLanguages) {

        try (Context context = newContext(currentSettings, permittedLanguages)) {
            return guestFunction.apply(context);
        }
    }

    private static Context newContext(PolyglotSandboxSettings currentSettings, String... permittedLanguages) {
        boolean constrained = isConstrained(currentSettings, permittedLanguages);

        // User-supplied scripts (Script component, Script Tool, and SkillsTool scripts) are evaluated here, so the
        // guest context is pinned to a no-host, no-IO sandbox. The script still interacts with the platform solely
        // through the ContextProxyObject/ComponentProxyObject guest proxies, which do not require host access. These
        // restrictions match GraalVM's secure defaults but are set explicitly so the sandbox cannot be silently
        // weakened by a future default change or accidental builder edit. The single carve-out is thread creation
        // for Ruby contexts: TruffleRuby backs core interop operations with fibers (host-side iteration of a guest
        // hash crosses an Enumerator, and Enumerator's generator is fiber-based), and fibers require thread
        // creation - without it any guest hash argument fails with "fibers not allowed with allowCreateThread(false)".
        Context.Builder builder = Context.newBuilder(permittedLanguages)
            .engine(getEngine(constrained, permittedLanguages))
            .allowHostAccess(constrained ? CONSTRAINED_HOST_ACCESS : HostAccess.NONE)
            .allowHostClassLoading(false)
            .allowHostClassLookup(className -> false)
            .allowNativeAccess(false)
            // RUBY-DISABLED: while Ruby is disabled no permitted language needs the fiber carve-out, so
            // thread creation stays off unconditionally. Restore
            // .allowCreateThread(isRubyPermitted(permittedLanguages)) together with the commented-out
            // helper below once a polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is
            // downgraded). Grep RUBY-DISABLED.
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

    /**
     * Whether the execution has to be moved onto a platform thread.
     *
     * <p>
     * Only when the context would carry a thread-metered ceiling and the caller is a virtual thread - the combination
     * GraalVM refuses to build. A caller already on a platform thread stays there, which also keeps a guest script that
     * invokes a polyglot custom component from queueing behind itself on a saturated pool.
     */
    private static boolean requiresGuestThread(
        PolyglotSandboxSettings currentSettings, String... permittedLanguages) {

        Thread currentThread = Thread.currentThread();

        if (!currentThread.isVirtual()) {
            return false;
        }

        return isConstrained(currentSettings, permittedLanguages) && isThreadMetered(currentSettings);
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

    private static boolean isConstrained(PolyglotSandboxSettings currentSettings, String... permittedLanguages) {
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

    private static Engine getEngine(boolean constrained, String... permittedLanguages) {
        return ENGINES.computeIfAbsent(
            toEngineKey(constrained, permittedLanguages), key -> newEngine(constrained, permittedLanguages));
    }

    private static String toEngineKey(boolean constrained, String... permittedLanguages) {
        List<String> sortedLanguages = Arrays.stream(permittedLanguages)
            .sorted()
            .toList();

        return constrained + ":" + String.join(",", sortedLanguages);
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

    // RUBY-DISABLED: unused while Ruby is disabled; see the allowCreateThread call above.
//    private static boolean isRubyPermitted(String... permittedLanguages) {
//        for (String permittedLanguage : permittedLanguages) {
//            if ("ruby".equals(permittedLanguage)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
}
