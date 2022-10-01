/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.component.bash;

import com.bytechef.component.bash.constants.BashConstants;
import com.bytechef.hermes.component.definition.Action;
import com.bytechef.hermes.component.test.MockContext;
import com.bytechef.hermes.component.test.MockExecutionParameters;
import com.bytechef.hermes.test.definition.DefinitionAssert;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class BashComponentHandlerTest {

    private static final MockContext context = new MockContext();

    @Test
    public void testGetComponentDefinition() {
        DefinitionAssert.assertEquals("definition/bash_v1.json", new BashComponentHandler().getDefinition());
    }

    @Test
    public void testPerformExecute() throws IOException {
        BashComponentHandler bashComponentAccessor = new BashComponentHandler();
        ClassPathResource classPathResource = new ClassPathResource("dependencies/test.txt");

        String output = bashComponentAccessor.performExecute(
                context,
                new MockExecutionParameters(Map.of(
                        BashConstants.SCRIPT,
                        "ls -l " + classPathResource.getFile().getAbsolutePath(),
                        Action.ACTION,
                        BashConstants.EXECUTE)));

        Assertions.assertTrue(output.contains("build/resources/test/dependencies/test.txt"));
    }
}
