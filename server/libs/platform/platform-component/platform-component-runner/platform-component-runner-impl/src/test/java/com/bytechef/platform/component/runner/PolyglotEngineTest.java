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

package com.bytechef.platform.component.runner;

import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.polyglot.ScriptSandboxMode;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Timeout(120)
class PolyglotEngineTest {

    private static final String INPUT = "input";

    private static final Map<String, ComponentConnection> COMPONENT_CONNECTIONS = Map.of(
        "my-connection",
        new ComponentConnection("httpClient", 1, 1L, Map.of("token", "secret"), AuthorizationType.BEARER_TOKEN));

    private final JobContextAware jobContextAware = mock(JobContextAware.class);
    private final PolyglotEngine polyglotEngine = new PolyglotEngine(mock(ApplicationContext.class));

    @Test
    void testExecuteJavaScriptWithInputAndContext() {
        Object result = execute("js", "function perform(input, context) {\n\treturn input.name;\n}");

        assertThat(result).isEqualTo("ByteChef");
    }

    @Test
    void testExecuteJavaScriptWithInputConnectionAndContext() {
        Object result = execute(
            "js", "function perform(input, connection, context) {\n\treturn connection[\"my-connection\"].token;\n}");

        assertThat(result).isEqualTo("secret");
    }

    @Test
    void testExecutePythonWithInputAndContext() {
        Object result = execute("python", "def perform(input, context):\n\treturn input.name");

        assertThat(result).isEqualTo("ByteChef");
    }

    @Test
    void testExecutePythonWithInputConnectionAndContext() {
        Object result = execute(
            "python", "def perform(input, connection, context):\n\treturn connection[\"my-connection\"][\"token\"]");

        assertThat(result).isEqualTo("secret");
    }

    @Test
    void testExecuteWithoutComponentConnections() {
        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                INPUT, Map.of(),
                SCRIPT, "function perform(input, connection, context) {\n\treturn Object.keys(connection).length;\n}"));

        Object result =
            polyglotEngine.execute(ScriptSandboxMode.STRICT, null, "js", inputParameters, null, jobContextAware);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void testExecutePythonReadsInputKeyNamedLikeDictMethodBySubscript() {
        Object result = execute(
            "python", "def perform(input, context):\n\treturn input[\"items\"]", Map.of("items", List.of(1, 2)));

        assertThat(result).isEqualTo(List.of(1, 2));
    }

    @Test
    void testExecutePythonWithInputContextAndOptionalParameter() {
        Object result = execute(
            "python",
            "def perform(input, context, debug=False):\n\treturn 'context' if hasattr(context, 'component') " +
                "else 'connection'");

        assertThat(result).isEqualTo("context");
    }

    @Test
    void testExecuteJavaScriptPassesContextAsThirdArgument() {
        Object result = execute(
            "js", "function perform(input, connection, context) {\n\treturn typeof context.component;\n}");

        assertThat(result).isEqualTo("object");
    }

    @Test
    void testExecuteWithoutExposedConnectionParameters() {
        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                INPUT, Map.of(),
                SCRIPT, "function perform(input, connection, context) {\n\treturn Object.keys(connection).length;\n}"));

        Object result = polyglotEngine.execute(
            ScriptSandboxMode.STRICT, null, "js", inputParameters, COMPONENT_CONNECTIONS, false, jobContextAware);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void testExecuteWithoutPerformFunction() {
        Parameters inputParameters = ParametersFactory.create(Map.of(INPUT, Map.of(), SCRIPT, "var perform = {};"));

        assertThatThrownBy(
            () -> polyglotEngine.execute(
                ScriptSandboxMode.STRICT, null, "js", inputParameters, COMPONENT_CONNECTIONS, jobContextAware))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("perform");
    }

    private Object execute(String languageId, String script) {
        return execute(languageId, script, Map.of("name", "ByteChef"));
    }

    private Object execute(String languageId, String script, Map<String, Object> input) {
        Parameters inputParameters = ParametersFactory.create(Map.of(INPUT, input, SCRIPT, script));

        return polyglotEngine.execute(
            ScriptSandboxMode.STRICT, null, languageId, inputParameters, COMPONENT_CONNECTIONS, jobContextAware);
    }
}
