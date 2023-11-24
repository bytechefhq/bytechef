/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.hermes.component.test.ComponentJobTestExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class ScriptComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private TaskHandler<Object> taskHandler = taskExecution -> {
        Map<String, ?> parameters = taskExecution.getParameters();

        return parameters.get("value");
    };

    @Autowired
    private ComponentJobTestExecutor componentJobTestExecutor;

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
            Map.of("factor", 3), Map.of("var/v1/set", taskHandler));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @Test
    public void testPerformPython() {
        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("script_v1_python".getBytes(StandardCharsets.UTF_8)),
            Map.of("factor", 3), Map.of("var/v1/set", taskHandler));

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
            Map.of("factor", 3), Map.of("var/v1/set", taskHandler));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals(6000, outputs.get("result"));
    }
}
