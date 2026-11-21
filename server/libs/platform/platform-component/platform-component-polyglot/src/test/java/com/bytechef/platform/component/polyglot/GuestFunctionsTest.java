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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class GuestFunctionsTest {

    @Test
    void testJavaScriptRequiredParameterCount() {
        assertEquals(3, getRequiredParameterCount("js", "(function perform(input, connection, context) {})"));
    }

    @Test
    void testPythonRequiredParameterCountSkipsDefaults() {
        assertEquals(2,
            getRequiredParameterCount("python", "def perform(input, context, extra=None):\n    pass\nperform"));
    }

    private static int getRequiredParameterCount(String languageId, String script) {
        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value function = polyglotContext.eval(languageId, script);

            return GuestFunctions.getRequiredParameterCount(polyglotContext, languageId, function, 2);
        });
    }
}
