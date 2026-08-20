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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.io.IOAccess;

/**
 * Builds the strict-sandbox {@link Context} shared by every guest polyglot execution that runs user-supplied code (the
 * Script component, Script Tool and SkillsTool scripts, and code workflow tasks).
 *
 * @author Ivica Cardic
 */
public final class PolyglotSandbox {

    private PolyglotSandbox() {
    }

    /**
     * Builds a strict-sandbox {@link Context} for the given engine and permitted languages.
     *
     * @param engine             the polyglot engine to build the context for
     * @param permittedLanguages the language ids the context is permitted to evaluate
     * @return a new, strictly sandboxed {@link Context}
     */
    public static Context newContext(Engine engine, String... permittedLanguages) {
        // User-supplied scripts (Script component, Script Tool, and SkillsTool scripts) are evaluated here, so the
        // guest context is pinned to a no-host, no-IO sandbox. The script still interacts with the platform solely
        // through the ContextProxyObject/ComponentProxyObject guest proxies, which do not require host access. These
        // restrictions match GraalVM's secure defaults but are set explicitly so the sandbox cannot be silently
        // weakened by a future default change or accidental builder edit. The single carve-out is thread creation
        // for Ruby contexts: TruffleRuby backs core interop operations with fibers (host-side iteration of a guest
        // hash crosses an Enumerator, and Enumerator's generator is fiber-based), and fibers require thread
        // creation — without it any guest hash argument fails with "fibers not allowed with allowCreateThread(false)".
        return Context.newBuilder(permittedLanguages)
            .engine(engine)
            .allowHostAccess(HostAccess.NONE)
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
            .allowPolyglotAccess(PolyglotAccess.NONE)
            .build();
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
