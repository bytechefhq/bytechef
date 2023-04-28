
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

package com.bytechef.component.xmlhelper.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.util.XmlUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
public class XmlHelperStringifyActionTest {

    @Test
    public void testExecuteStringify() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Map<String, ?> source = Map.of("id", 45, "name", "Poppy");

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(source);

        assertThat(XmlHelperStringifyAction.executeStringify(Mockito.mock(Context.class), inputParameters))
            .isEqualTo(XmlUtils.write(source));
    }
}
