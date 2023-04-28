
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

package com.bytechef.component.objecthelper.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static com.bytechef.component.objecthelper.constant.ObjectHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperParseActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    public void testExecuteParse() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn("""
                {
                    "key": 3
                }
                """);

        assertThat(ObjectHelperParseAction.executeParse(context, inputParameters))
            .isEqualTo(Map.of("key", 3));

        inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(
                """
                    [
                        {
                            "key": 3
                        }
                    ]
                    """);

        assertThat(ObjectHelperParseAction.executeParse(context, inputParameters))
            .isEqualTo(List.of(Map.of("key", 3)));
    }
}
