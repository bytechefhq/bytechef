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

package com.integri.atlas.task.handler.xml.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.task.handler.json.helper.JsonHelper;
import com.integri.atlas.task.handler.xml.helper.XmlHelper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class XmlHelpersTaskHandlerTest {

    private static final JsonHelper jsonHelper = new JsonHelper(new ObjectMapper());
    private static final XmlHelper xmlHelper = new XmlHelper();
    private static final XmlHelpersTaskHandler xmlHelpersTaskHandler = new XmlHelpersTaskHandler(xmlHelper);

    @Test
    public void testJSONToObject() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        String source =
            """
            <Flower id="45">
                <name>Poppy</name>
            </Flower>
            """;

        taskExecution.put("source", source);
        taskExecution.put("operation", "XML_TO_JSON");

        assertThat(xmlHelpersTaskHandler.handle(taskExecution)).isEqualTo(Map.of("id", "45", "name", "Poppy"));

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

        taskExecution.put("source", source);
        taskExecution.put("operation", "XML_TO_JSON");

        assertThat(xmlHelpersTaskHandler.handle(taskExecution))
            .isEqualTo(
                Map.of("Flower", List.of(Map.of("id", "45", "name", "Poppy"), Map.of("id", "50", "name", "Rose")))
            );
    }

    @Test
    public void testObjectToJSON() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        Map<String, ?> source = Map.of("id", 45, "name", "Poppy");

        taskExecution.put("source", source);
        taskExecution.put("operation", "JSON_TO_XML");

        assertThat(xmlHelpersTaskHandler.handle(taskExecution)).isEqualTo(xmlHelper.write(source));
    }
}
