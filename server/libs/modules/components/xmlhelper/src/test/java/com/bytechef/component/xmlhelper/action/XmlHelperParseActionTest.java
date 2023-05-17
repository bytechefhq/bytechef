
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
import com.bytechef.hermes.component.util.XmlMapper;
import com.bytechef.hermes.component.util.XmlUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
public class XmlHelperParseActionTest {

    @BeforeAll
    public static void beforeAll() {
        ReflectionTestUtils.setField(XmlUtils.class, "xmlMapper", new XmlMapper());
    }

    @Test
    public void testExecuteParse() {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequiredString(SOURCE))
            .thenReturn(
                """
                    <Flower id="45">
                        <name>Poppy</name>
                    </Flower>
                    """);

        assertThat((Map<String, ?>) XmlHelperParseAction.executeParse(Mockito.mock(Context.class), inputParameters))
            .isEqualTo(Map.of("id", "45", "name", "Poppy"));

        Mockito.when(inputParameters.getRequiredString(SOURCE))
            .thenReturn(
                """
                    <Flowers>
                        <Flower id="45">
                            <name>Poppy</name>
                        </Flower>
                        <Flower id="50">
                            <name>Rose</name>
                        </Flower>
                    </Flowers>
                    """);

        assertThat(XmlHelperParseAction.executeParse(Mockito.mock(Context.class), inputParameters))
            .isEqualTo(
                Map.of("Flower", List.of(Map.of("id", "45", "name", "Poppy"), Map.of("id", "50", "name", "Rose"))));
    }
}
