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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.component.definition.Context.Log;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.polyglot.ScriptSandboxMode;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(120)
class PolyglotEngineTest {

    private static final String INPUT = "input";

    private static final Map<String, ComponentConnection> COMPONENT_CONNECTIONS = Map.of(
        "my-connection",
        new ComponentConnection("httpClient", 1, 1L, Map.of("token", "secret"), AuthorizationType.BEARER_TOKEN));

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final JobContextAware jobContextAware = mock(JobContextAware.class);
    private final PolyglotEngine polyglotEngine = new PolyglotEngine(applicationContext);

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
    void testExecuteJavaScriptWithoutComponentConnections() {
        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                INPUT, Map.of(),
                SCRIPT, "function perform(input, connection, context) {\n\treturn Object.keys(connection).length;\n}"));

        Object result =
            polyglotEngine.execute(ScriptSandboxMode.STRICT, null, "js", inputParameters, null, jobContextAware);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void testExecuteJavaScriptPassesContextAsThirdArgument() {
        Object result = execute(
            "js", "function perform(input, connection, context) {\n\treturn typeof context.component;\n}");

        assertThat(result).isEqualTo("object");
    }

    @Test
    void testExecuteJavaScriptWithoutExposedConnectionParameters() {
        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                INPUT, Map.of(),
                SCRIPT, "function perform(input, connection, context) {\n\treturn Object.keys(connection).length;\n}"));

        Object result = polyglotEngine.execute(
            ScriptSandboxMode.STRICT, null, "js", inputParameters, COMPONENT_CONNECTIONS, false, jobContextAware);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void testExecuteJavaScriptWithoutPerformFunction() {
        Parameters inputParameters = ParametersFactory.create(Map.of(INPUT, Map.of(), SCRIPT, "var perform = {};"));

        assertThatThrownBy(
            () -> polyglotEngine.execute(
                ScriptSandboxMode.STRICT, null, "js", inputParameters, COMPONENT_CONNECTIONS, jobContextAware))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("perform");
    }

    @Order(Integer.MAX_VALUE)
    @Test
    void testExecuteJavaScriptNamedConnectionWithoutComponentConnections() {
        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                INPUT, Map.of(),
                SCRIPT,
                "function perform(input, context) {\n\treturn context.component.httpClient.get({}, 'missing');\n}"));
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(applicationContext.getBean(ComponentDefinitionService.class)).thenReturn(componentDefinitionService);
        when(componentDefinitionService.getComponentDefinition("httpClient", null)).thenReturn(componentDefinition);
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getName()).thenReturn("get");
        when(componentDefinition.getActions()).thenReturn(List.of(actionDefinition));

        assertThatThrownBy(
            () -> polyglotEngine.execute(ScriptSandboxMode.STRICT, null, "js", inputParameters, null, jobContextAware))
                .hasMessageContaining("Connection with name missing does not exist");
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
    void testExecutePythonWithInputContextAndOptionalParameter() {
        Object result = execute(
            "python",
            "def perform(input, context, debug=False):\n\treturn 'context' if debug is False else 'connection'");

        assertThat(result).isEqualTo("context");
    }

    @Test
    void testExecuteJavaScriptLogsAtLevel() {
        Log log = mock(Log.class);
        JobContextAware actionContext = mockActionContext(log);

        Object result = execute(
            "js", "function perform(input, context) {\n\tcontext.log('info', 'my-task ran');\n\treturn 1;\n}",
            actionContext);

        assertThat(result).isEqualTo(1);

        verify(log).info("my-task ran");
    }

    @Test
    void testExecutePythonLogsAtCaseInsensitiveLevel() {
        Log log = mock(Log.class);
        JobContextAware actionContext = mockActionContext(log);

        execute("python", "def perform(input, context):\n\tcontext.log('WARN', 'careful')\n\treturn 1", actionContext);

        verify(log).warn("careful");
    }

    @Test
    void testExecuteJavaScriptLogsAtEveryLevel() {
        Log log = mock(Log.class);
        JobContextAware actionContext = mockActionContext(log);

        execute(
            "js",
            "function perform(input, context) {\n\t['trace', 'Debug', 'info', 'warn', 'ERROR'].forEach(" +
                "level => context.log(level, level));\n\treturn 1;\n}",
            actionContext);

        verify(log).trace("trace");
        verify(log).debug("Debug");
        verify(log).info("info");
        verify(log).warn("warn");
        verify(log).error("ERROR");
    }

    @Test
    void testExecuteJavaScriptLogWithUnknownLevel() {
        Log log = mock(Log.class);
        JobContextAware actionContext = mockActionContext(log);

        assertThatThrownBy(
            () -> execute(
                "js", "function perform(input, context) {\n\tcontext.log('verbose', 'message');\n}", actionContext))
                    .hasMessageContaining("Unknown log level 'verbose'")
                    .hasMessageContaining("trace, debug, info, warn, error");

        verify(log, never()).info(any(String.class));
    }

    @Test
    void testExecuteJavaScriptLogWithoutLogSink() {
        Object result = execute(
            "js", "function perform(input, context) {\n\tcontext.log('info', 'message');\n\treturn 1;\n}");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void testExecuteJavaScriptContextMemberKeysContainLog() {
        Object result = execute("js", "function perform(input, context) {\n\treturn Object.keys(context);\n}");

        assertThat(result).isEqualTo(List.of("component", "log"));
    }

    @Test
    void testExecutePythonContextHasLog() {
        Object result = execute(
            "python", "def perform(input, context):\n\treturn 'log' if hasattr(context, 'log') else 'none'");

        assertThat(result).isEqualTo("log");
    }

    @SuppressWarnings("unchecked")
    private static JobContextAware mockActionContext(Log log) {
        ActionContext actionContext = mock(
            ActionContext.class, withSettings().extraInterfaces(JobContextAware.class));

        doAnswer(invocation -> {
            ContextConsumer<Log> logConsumer = invocation.getArgument(0);

            logConsumer.accept(log);

            return null;
        }).when(actionContext)
            .log(any(ContextConsumer.class));

        return (JobContextAware) actionContext;
    }

    private Object execute(String languageId, String script, JobContextAware contextAware) {
        Parameters inputParameters = ParametersFactory.create(Map.of(INPUT, Map.of(), SCRIPT, script));

        return polyglotEngine.execute(
            ScriptSandboxMode.STRICT, null, languageId, inputParameters, COMPONENT_CONNECTIONS, contextAware);
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
