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

import static com.integri.atlas.task.handler.xml.helpers.XmlHelpersTaskHandler.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.task.commons.xml.XmlHelper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class XmlHelpersTaskHandlerTest {

    private static final XmlHelper xmlHelper = new XmlHelper();
    private static final XmlHelpersParseTaskHandler xmlHelpersParseTaskHandler = new XmlHelpersParseTaskHandler(
        xmlHelper
    );
    private static final XmlHelpersStringifyTaskHandler xmlHelpersStringifyTaskHandler = new XmlHelpersStringifyTaskHandler(
        xmlHelper
    );

    @Test
    public void testParse() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        String source =
            """
            <Flower id="45">
                <name>Poppy</name>
            </Flower>
            """;

        taskExecution.put("source", source);
        taskExecution.put("operation", "XML_TO_JSON");

        assertThat(xmlHelpersParseTaskHandler.handle(taskExecution)).isEqualTo(Map.of("id", "45", "name", "Poppy"));

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

        assertThat(xmlHelpersParseTaskHandler.handle(taskExecution))
            .isEqualTo(
                Map.of("Flower", List.of(Map.of("id", "45", "name", "Poppy"), Map.of("id", "50", "name", "Rose")))
            );
    }

    @Test
    public void testStringify() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        Map<String, ?> source = Map.of("id", 45, "name", "Poppy");

        taskExecution.put("source", source);
        taskExecution.put("operation", "JSON_TO_XML");

        assertThat(xmlHelpersStringifyTaskHandler.handle(taskExecution)).isEqualTo(xmlHelper.write(source));
    }
}
