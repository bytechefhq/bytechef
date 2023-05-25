
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

package com.bytechef.component.bash.action;

import com.bytechef.component.bash.BashComponentHandlerTest;
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.util.MapValueUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static com.bytechef.component.bash.constant.BashConstants.SCRIPT;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class BashExecuteActionTest {

    @Test
    public void testExecute() {
        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            String script = "ls -l " + BashComponentHandlerTest.class
                .getClassLoader()
                .getResource("dependencies/test.txt")
                .getFile();

            mockedStatic.when(() -> MapValueUtils.getRequiredString(Mockito.anyMap(), Mockito.eq(SCRIPT)))
                .thenReturn(script);

            String output = BashExecuteAction.perform(Map.of(), Mockito.mock(ActionContext.class));

            Assertions.assertTrue(output.contains("build/resources/test/dependencies/test.txt"));
        }
    }
}
