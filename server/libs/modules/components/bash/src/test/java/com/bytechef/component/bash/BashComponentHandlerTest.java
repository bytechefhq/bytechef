
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

import com.bytechef.component.bash.action.BashExecuteAction;
import com.bytechef.component.bash.constant.BashConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class BashComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/bash_v1.json", new BashComponentHandler().getDefinition());
    }

    @Test
    public void testExecute() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequiredString(BashConstants.SCRIPT))
            .thenReturn("ls -l "
                + BashComponentHandlerTest.class
                    .getClassLoader()
                    .getResource("dependencies/test.txt")
                    .getFile());

        String output = BashExecuteAction.execute(Mockito.mock(Context.class), inputParameters);

        Assertions.assertTrue(output.contains("build/resources/test/dependencies/test.txt"));
    }
}
