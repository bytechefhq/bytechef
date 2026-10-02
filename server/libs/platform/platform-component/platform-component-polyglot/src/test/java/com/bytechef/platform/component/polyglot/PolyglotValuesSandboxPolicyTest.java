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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class PolyglotValuesSandboxPolicyTest {

    private static final HostAccess HOST_ACCESS = HostAccess.newBuilder(HostAccess.NONE)
        .allowMutableTargetMappings()
        .build();

    @Test
    public void testGuestMapConvertsUnderConstrainedPolicy() {
        assertThat(evalAndConvert("js", "({ name: 'ByteChef', count: 3 })"))
            .isEqualTo(Map.of("name", "ByteChef", "count", 3));
    }

    @Test
    public void testGuestListConvertsUnderConstrainedPolicy() {
        assertThat(evalAndConvert("js", "([1, 2, 3])")).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void testNestedGuestValueConvertsUnderConstrainedPolicy() {
        assertThat(evalAndConvert("js", "({ list: [1, 2], nested: { inner: 'deep' } })"))
            .isEqualTo(Map.of("list", List.of(1, 2), "nested", Map.of("inner", "deep")));
    }

    @Test
    public void testGuestDictConvertsUnderConstrainedPolicy() {
        assertThat(evalAndConvert("python", "{'name': 'ByteChef', 'nested': {'inner': 'deep'}}"))
            .isEqualTo(Map.of("name", "ByteChef", "nested", Map.of("inner", "deep")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGuestFunctionStaysExecutableUnderConstrainedPolicy() {
        withConverted("js", "({ name: 'my-task', perform: function (context) { return context; } })", converted -> {
            assertThat(converted).isInstanceOf(Map.class);

            Map<String, Object> map = (Map<String, Object>) converted;

            assertThat(map).containsEntry("name", "my-task");

            Function<Object[], Object> perform = (Function<Object[], Object>) map.get("perform");

            assertThat(perform.apply(new Object[] {
                "echoed"
            })).isEqualTo("echoed");
        });
    }

    private static Object evalAndConvert(String languageId, String source) {
        Engine engine = Engine.newBuilder(languageId)
            .sandbox(SandboxPolicy.CONSTRAINED)
            .in(new ByteArrayInputStream(new byte[0]))
            .out(new ByteArrayOutputStream())
            .err(new ByteArrayOutputStream())
            .build();

        try (engine; Context context = Context.newBuilder(languageId)
            .engine(engine)
            .sandbox(SandboxPolicy.CONSTRAINED)
            .in(new ByteArrayInputStream(new byte[0]))
            .out(new ByteArrayOutputStream())
            .err(new ByteArrayOutputStream())
            .allowHostAccess(HOST_ACCESS)
            .build()) {

            Value value = context.eval(languageId, source);

            return PolyglotValues.copyFromPolyglotContext(PolyglotValues.copyToJavaValue(value));
        }
    }

    private static void withConverted(String languageId, String source, Consumer<Object> assertions) {
        Engine engine = Engine.newBuilder(languageId)
            .sandbox(SandboxPolicy.CONSTRAINED)
            .in(new ByteArrayInputStream(new byte[0]))
            .out(new ByteArrayOutputStream())
            .err(new ByteArrayOutputStream())
            .build();

        try (engine; Context context = Context.newBuilder(languageId)
            .engine(engine)
            .sandbox(SandboxPolicy.CONSTRAINED)
            .in(new ByteArrayInputStream(new byte[0]))
            .out(new ByteArrayOutputStream())
            .err(new ByteArrayOutputStream())
            .allowHostAccess(HOST_ACCESS)
            .build()) {

            assertions.accept(PolyglotValues.copyToJavaValue(context.eval(languageId, source)));
        }
    }
}
