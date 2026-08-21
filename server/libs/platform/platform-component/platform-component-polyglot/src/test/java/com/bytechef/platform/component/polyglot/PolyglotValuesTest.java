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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class PolyglotValuesTest {

    private static final String LANGUAGE_ID = "js";

    @Test
    public void testCopyRoundTripMap() {
        Map<String, Object> input = Map.of("name", "ByteChef", "count", 3);

        Object result = roundTrip(input);

        assertThat(result).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;

        assertThat(resultMap.get("name")).isEqualTo("ByteChef");
        assertThat(((Number) resultMap.get("count")).intValue()).isEqualTo(3);
    }

    @Test
    public void testCopyRoundTripList() {
        List<Object> input = List.of("a", "b", "c");

        Object result = roundTrip(input);

        assertThat(result).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) result;

        assertThat(resultList).containsExactly("a", "b", "c");
    }

    @Test
    public void testCopyRoundTripInstant() {
        // copyToJavaValue checks value.isDate() before value.isInstant(), and a ProxyInstant-backed value reports
        // both as true, so the date branch wins and the round trip yields the date portion as a LocalDate. This
        // mirrors the extracted PolyglotEngine behavior exactly (not something introduced by the extraction).
        Instant input = Instant.parse("2024-01-01T00:00:00Z");

        Object result = roundTrip(input);

        assertThat(result).isInstanceOf(LocalDate.class);
        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    public void testCopyRoundTripByteArray() {
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);

        Object result = roundTrip(input);

        assertThat(result).isEqualTo(Base64.getEncoder()
            .encodeToString(input));
    }

    @Test
    public void testGuestMapSupportsPythonDictSubscript() {
        // GraalPy exposes a foreign object's members as attributes only, so a map handed to a python guest has
        // to carry hash entries for the idiomatic input['key'] to resolve.
        assertThat(evalWithGuestMap("python", "def f(value):\n    return value['name']")).isEqualTo("ByteChef");
    }

    @Test
    public void testGuestMapSupportsPythonAttributeAccess() {
        // The python script templates and the shipped workflow fixtures read input.name, so member access has to
        // keep working alongside the dict subscript added above.
        assertThat(evalWithGuestMap("python", "def f(value):\n    return value.name")).isEqualTo("ByteChef");
        assertThat(evalWithGuestMap("python", "def f(value):\n    return value.nested.inner")).isEqualTo("deep");
    }

    @Test
    public void testGuestMapSupportsPythonDictProtocol() {
        assertThat(evalWithGuestMap("python", "def f(value):\n    return dict(value)['name']"))
            .isEqualTo("ByteChef");
        assertThat(evalWithGuestMap("python", "def f(value):\n    return 'name' in value")).isEqualTo(true);
        assertThat(evalWithGuestMap("python", "def f(value):\n    return value.get('name')")).isEqualTo("ByteChef");
        assertThat(evalWithGuestMap("python", "def f(value):\n    return list(value.keys())[0]")).isEqualTo("name");
    }

    @Test
    public void testGuestMapSupportsJavaScriptMemberAndIndexAccess() {
        assertThat(evalWithGuestMap("js", "(function (value) { return value.name; })")).isEqualTo("ByteChef");
        assertThat(evalWithGuestMap("js", "(function (value) { return value['name']; })")).isEqualTo("ByteChef");
    }

    @Test
    public void testGuestMapIsNestedPerLanguage() {
        assertThat(evalWithGuestMap("python", "def f(value):\n    return value['nested']['inner']"))
            .isEqualTo("deep");
        assertThat(evalWithGuestMap("js", "(function (value) { return value.nested.inner; })")).isEqualTo("deep");
    }

    /**
     * Hands {@code {"name": "ByteChef", "nested": {"inner": "deep"}}} to a guest function written in the given language
     * and returns what that function read back out, as a host value.
     */
    private static Object evalWithGuestMap(String languageId, String functionSource) {
        Map<String, Object> hostMap = Map.of("name", "ByteChef", "nested", Map.of("inner", "deep"));

        return PolyglotSandbox.call(languageId, context -> {
            Value function;

            if ("python".equals(languageId)) {
                context.eval(languageId, functionSource);

                function = context.getBindings(languageId)
                    .getMember("f");
            } else {
                function = context.eval(languageId, functionSource);
            }

            Value value = function.execute(PolyglotValues.copyToGuestValue(hostMap, languageId));

            return PolyglotValues.copyFromPolyglotContext(PolyglotValues.copyToJavaValue(value));
        });
    }

    private static Object roundTrip(Object hostValue) {
        return PolyglotSandbox.call(LANGUAGE_ID, context -> {
            context.getBindings(LANGUAGE_ID)
                .putMember("value", PolyglotValues.copyToGuestValue(hostValue, LANGUAGE_ID));

            Value value = context.eval(LANGUAGE_ID, "value");

            return PolyglotValues.copyFromPolyglotContext(PolyglotValues.copyToJavaValue(value));
        });
    }

}
