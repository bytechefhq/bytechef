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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.hermes.component.utils.XmlUtils;
import com.bytechef.test.jsonasssert.AssertUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class XmlHelperComponentHandlerTest {

    private static final MockContext context = new MockContext();
    private static final XmlHelperComponentHandler xmlHelperComponentHandler = new XmlHelperComponentHandler();

    @Test
    public void testGetComponentDefinition() throws IOException {
        AssertUtils.assertEquals("definition/xmlhelper_v1.json", new XmlHelperComponentHandler().getDefinition());
    }

    @Test
    public void testPerformParse() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        String source =
                """
            <Flower id="45">
                <name>Poppy</name>
            </Flower>
            """;

        parameters.set("source", source);

        assertThat((Map<String, ?>) xmlHelperComponentHandler.performParse(context, parameters))
                .isEqualTo(Map.of("id", "45", "name", "Poppy"));

        source =
                """
            <Flowers>
                <Flower id="45">
                    <name>Poppy</name>
                </Flower>
                <Flower id="50">
                    <name>Rose</name>
                </Flower>
            </Flowers>
            """;

        parameters.set("source", source);

        assertThat(xmlHelperComponentHandler.performParse(context, parameters))
                .isEqualTo(Map.of(
                        "Flower", List.of(Map.of("id", "45", "name", "Poppy"), Map.of("id", "50", "name", "Rose"))));
    }

    @Test
    public void testPerformStringify() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        Map<String, ?> source = Map.of("id", 45, "name", "Poppy");

        parameters.set("source", source);

        assertThat(xmlHelperComponentHandler.performStringify(context, parameters))
                .isEqualTo(XmlUtils.write(source));
    }
}
