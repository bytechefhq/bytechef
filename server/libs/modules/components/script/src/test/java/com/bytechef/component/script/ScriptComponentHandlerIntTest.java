/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.sync.executor.WorkflowExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.test.task.handler.TestVarTaskHandler;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class ScriptComponentHandlerIntTest {

    private TestVarTaskHandler<Integer, Integer> testVarTaskHandler;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Disabled
    @Test
    public void testPerformJava() {
        // TODO
    }

    @Test
    public void testPerformJavaScript() {
        Job job = workflowExecutor.execute(
                "script_v1_javascript", Map.of("factor", 3), Map.of("var/v1/set", testVarTaskHandler));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @Test
    public void testPerformPython() {
        Job job = workflowExecutor.execute(
                "script_v1_python", Map.of("factor", 3), Map.of("var/v1/set", testVarTaskHandler));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @Test
    public void testPerformR() {
        Job job =
                workflowExecutor.execute("script_v1_r", Map.of("factor", 3), Map.of("var/v1/set", testVarTaskHandler));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        Assertions.assertEquals(6000, outputs.get("result"));
    }

    @Test
    public void testPerformRuby() {
        Job job = workflowExecutor.execute(
                "script_v1_ruby", Map.of("factor", 3), Map.of("var/v1/set", testVarTaskHandler));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        Assertions.assertEquals(6000, outputs.get("result"));
    }
}
