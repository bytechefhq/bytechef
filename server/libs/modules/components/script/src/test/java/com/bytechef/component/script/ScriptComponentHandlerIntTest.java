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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.component.script.task.handler.ScriptJavaScriptTaskHandler;
import com.bytechef.component.script.task.handler.ScriptPythonTaskHandler;
import com.bytechef.component.script.task.handler.ScriptRubyTaskHandler;
import com.bytechef.platform.component.test.ComponentJobTestExecutor;
import com.bytechef.platform.component.test.config.ComponentTestIntConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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
        ScriptComponentHandlerIntTest.ScriptComponentHandlerIntTestConfiguration.class
    },
    properties = "bytechef.workflow.repository.classpath.enabled=true")
public class ScriptComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private final TaskHandler<Object> taskHandler = taskExecution -> {
        Map<String, ?> parameters = taskExecution.getParameters();

        return parameters.get("value");
    };

    @Autowired
    private ComponentJobTestExecutor componentJobTestExecutor;

    @Autowired
    private ScriptPythonTaskHandler scriptPythonTaskHandler;

    @Autowired
    private ScriptRubyTaskHandler scriptRubyTaskHandler;

    @Autowired
    private ScriptJavaScriptTaskHandler scriptJavaScriptTaskHandler;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Disabled
    @Test
    public void testPerformJava() {
        // TODO
    }

    @Test
    public void testPerformJavaScript() {
        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("script_v1_javascript".getBytes(StandardCharsets.UTF_8)),
            Map.of("factor", 3),
            Map.of("var/v1/set", taskHandler, "script/v1/javascript", scriptJavaScriptTaskHandler));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @Test
    public void testPerformPython() {
        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("script_v1_python".getBytes(StandardCharsets.UTF_8)),
            Map.of("factor", 3),
            Map.of("var/v1/set", taskHandler, "python/v1/javascript", scriptPythonTaskHandler));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @Disabled
    @Test
    public void testPerformR() {
        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("script_v1_r".getBytes(StandardCharsets.UTF_8)),
            Map.of("factor", 3), Map.of("var/v1/set", taskHandler));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @Test
    public void testPerformRuby() {
        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("script_v1_ruby".getBytes(StandardCharsets.UTF_8)),
            Map.of("factor", 3),
            Map.of("var/v1/set", taskHandler, "ruby/v1/javascript", scriptRubyTaskHandler));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @ComponentScan(basePackages = "com.bytechef.component.script")
    @TestConfiguration
    public static class ScriptComponentHandlerIntTestConfiguration {
    }
}
