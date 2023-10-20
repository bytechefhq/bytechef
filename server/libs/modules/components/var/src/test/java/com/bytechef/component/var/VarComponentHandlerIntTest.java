
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

package com.bytechef.component.var;

import com.bytechef.atlas.domain.Job;
import com.bytechef.hermes.component.test.workflow.ComponentWorkflowTestSupport;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class VarComponentHandlerIntTest {

    @Autowired
    private ComponentWorkflowTestSupport componentWorkflowTestSupport;

    @Test
    public void testVar() {
        Job job = componentWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("var_v1".getBytes(StandardCharsets.UTF_8)),
            Map.of());

        Map<String, Object> outputs = job.getOutputs();

        Assertions.assertEquals("1234", outputs.get("stringNumber"));
        Assertions.assertEquals(1234, (Integer) outputs.get("intNumber"));
    }
}
