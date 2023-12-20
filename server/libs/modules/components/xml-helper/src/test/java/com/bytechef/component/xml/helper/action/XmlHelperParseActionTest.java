/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.xml.helper.action;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.xml.helper.constant.XmlHelperConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class XmlHelperParseActionTest {

    @Test
    public void testPerformParse() {
        ActionContext context = Mockito.mock(ActionContext.class);
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequiredString(
            Mockito.eq(XmlHelperConstants.SOURCE)))
            .thenReturn("""
                <Flower id="45">
                    <name>Poppy</name>
                </Flower>
                """);
        Mockito.when(context.xml(Mockito.any()))
            .thenReturn(Map.of("id", "45", "name", "Poppy"));

        assertThat((Map<String, ?>) XmlHelperParseAction.perform(parameters, parameters, context))
            .isEqualTo(Map.of("id", "45", "name", "Poppy"));

        Mockito.when(parameters.getRequiredString(
            Mockito.eq(XmlHelperConstants.SOURCE)))
            .thenReturn("""
                <Flowers>
                    <Flower id="45">
                        <name>Poppy</name>
                    </Flower>
                    <Flower id="50">
                        <name>Rose</name>
                    </Flower>
                </Flowers>
                """);
        Mockito.when(context.xml(Mockito.any()))
            .thenReturn(
                Map.of("Flower", List.of(Map.of("id", "45", "name", "Poppy"), Map.of("id", "50", "name", "Rose"))));

        assertThat(XmlHelperParseAction.perform(parameters, parameters, context))
            .isEqualTo(
                Map.of("Flower", List.of(Map.of("id", "45", "name", "Poppy"), Map.of("id", "50", "name", "Rose"))));
    }
}
