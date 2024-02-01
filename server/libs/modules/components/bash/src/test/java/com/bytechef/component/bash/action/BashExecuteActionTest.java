/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.component.bash.action;

import static com.bytechef.component.bash.constant.BashConstants.SCRIPT;

import com.bytechef.component.bash.BashComponentHandlerTest;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class BashExecuteActionTest {

    @Test
    public void testPerform() throws IOException, InterruptedException, TimeoutException {
        String script = "ls -l " + BashComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/bash/test.txt")
            .getFile();

        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequiredString(Mockito.eq(SCRIPT)))
            .thenReturn(script);

        Map<String, String> performMap = BashExecuteAction.perform(
            parameters, parameters, Mockito.mock(ActionContext.class));

        String result = performMap.get("result");

        Assertions.assertTrue(result.contains("build/resources/test/dependencies/bash/test.txt"));
    }
}
