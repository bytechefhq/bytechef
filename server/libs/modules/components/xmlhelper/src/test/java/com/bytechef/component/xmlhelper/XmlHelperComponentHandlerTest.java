
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

package com.bytechef.component.xmlhelper;

import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.xmlhelper.action.XmlHelperParseAction;
import com.bytechef.component.xmlhelper.action.XmlHelperStringifyAction;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.util.XmlUtils;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class XmlHelperComponentHandlerTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/xmlhelper_v1.json", new XmlHelperComponentHandler().getDefinition());
    }

    @Test
    public void testPerformParse() {
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequiredString(SOURCE))
            .thenReturn(
                """
                    <Flower id="45">
                        <name>Poppy</name>
                    </Flower>
                    """);

        assertThat((Map<String, ?>) XmlHelperParseAction.performParse(context, parameters))
            .isEqualTo(Map.of("id", "45", "name", "Poppy"));

        Mockito.when(parameters.getRequiredString(SOURCE))
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

        assertThat(XmlHelperParseAction.performParse(context, parameters))
            .isEqualTo(Map.of(
                "Flower", List.of(Map.of("id", "45", "name", "Poppy"), Map.of("id", "50", "name", "Rose"))));
    }

    @Test
    public void testPerformStringify() {
        Parameters parameters = Mockito.mock(Parameters.class);

        Map<String, ?> source = Map.of("id", 45, "name", "Poppy");

        Mockito.when(parameters.getRequired(SOURCE))
            .thenReturn(source);

        assertThat(XmlHelperStringifyAction.performStringify(context, parameters))
            .isEqualTo(XmlUtils.write(source));
    }
}
