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

package com.bytechef.component.script;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.component.script.task.handler.ScriptJavaScriptTaskHandler;
import com.bytechef.platform.component.test.ComponentJobTestExecutor;
import com.bytechef.platform.component.test.config.ComponentTestIntConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ComponentTestIntConfiguration.class,
        ScriptComponentHandlerRunnerDisabledIntTest.ScriptComponentHandlerRunnerDisabledIntTestConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.classpath.enabled=true",
        "bytechef.script.runners.graalvm.enabled=false"
    })
public class ScriptComponentHandlerRunnerDisabledIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private final TaskHandler<Object> taskHandler = taskExecution -> {
        Map<String, ?> parameters = taskExecution.getParameters();

        return parameters.get("value");
    };

    @Autowired
    private ComponentJobTestExecutor componentJobTestExecutor;

    @Autowired
    private ScriptJavaScriptTaskHandler scriptJavaScriptTaskHandler;

    @Test
    @Timeout(120)
    public void testPerformJavaScriptFailsWhenTheGraalVmRunnerIsDisabled() {
        assertThatThrownBy(
            () -> componentJobTestExecutor.execute(
                ENCODER.encodeToString("script_v1_javascript".getBytes(StandardCharsets.UTF_8)),
                Map.of("factor", 3),
                Map.of("var/v1/set", taskHandler, "script/v1/javascript", scriptJavaScriptTaskHandler)))
                    .hasMessageContaining("bytechef.script.runners.graalvm.enabled=true");
    }

    @ComponentScan(basePackages = "com.bytechef.component.script")
    @TestConfiguration
    public static class ScriptComponentHandlerRunnerDisabledIntTestConfiguration {
    }
}
