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

import java.util.HashMap;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class PolyglotSandboxTest {

    @Test
    public void testContextRestrictionsPinned() {
        Engine engine = Engine.newBuilder()
            .build();

        try (Context context = PolyglotSandbox.newContext(engine, "js")) {
            Value value = context.eval("js", "1 + 1");

            assertThat(value.asInt()).isEqualTo(2);

            assertThatThrownBy(() -> context.eval("js", "Java.type('java.lang.System')"))
                .isInstanceOf(PolyglotException.class);
        } finally {
            engine.close();
        }
    }

    @Test
    public void testRubyContextAllowsHostIterationOfGuestHashes() {
        Engine engine = Engine.newBuilder()
            .build();

        // TruffleRuby backs host-side hash iteration with a fiber-based Enumerator, so this pins the ruby-only
        // thread-creation carve-out; without it the copy below fails with "fibers not allowed with
        // allowCreateThread(false)". Host class lookup must stay denied regardless.
        try (Context context = PolyglotSandbox.newContext(engine, "ruby")) {
            Value value = context.eval("ruby", "{ \"x\" => 1 }");

            Map<?, ?> guestBackedMap = value.as(Map.class);

            Map<Object, Object> copiedMap = new HashMap<>(guestBackedMap);

            assertThat(copiedMap).containsKey("x");

            assertThatThrownBy(() -> context.eval("ruby", "Java.type('java.lang.System')"))
                .isInstanceOf(PolyglotException.class);
        } finally {
            engine.close();
        }
    }

}
