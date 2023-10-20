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

package com.bytechef.task.handler.xmlhelpers.v1_0;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.task.commons.xml.XMLHelper;
import com.bytechef.task.handler.xmlhelpers.XMLHelpersTaskConstants;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class XMLHelpersTaskHandler {

    @Component(XMLHelpersTaskConstants.XML_HELPERS
            + "/"
            + XMLHelpersTaskConstants.VERSION_1_0
            + "/"
            + XMLHelpersTaskConstants.XML_PARSE)
    public static class XMLHelpersParseTaskHandler implements TaskHandler<Map<String, ?>> {

        private final XMLHelper xmlHelper;

        public XMLHelpersParseTaskHandler(XMLHelper xmlHelper) {
            this.xmlHelper = xmlHelper;
        }

        @Override
        public Map<String, ?> handle(TaskExecution taskExecution) {
            String input = taskExecution.getRequiredString(XMLHelpersTaskConstants.SOURCE);

            return xmlHelper.read(input);
        }
    }

    @Component(XMLHelpersTaskConstants.XML_HELPERS
            + "/"
            + XMLHelpersTaskConstants.VERSION_1_0
            + "/"
            + XMLHelpersTaskConstants.XML_STRINGIFY)
    public static class XMLHelpersStringifyTaskHandler implements TaskHandler<String> {

        private final XMLHelper xmlHelper;

        public XMLHelpersStringifyTaskHandler(XMLHelper xmlHelper) {
            this.xmlHelper = xmlHelper;
        }

        @Override
        public String handle(TaskExecution taskExecution) {
            Object input = taskExecution.getRequired(XMLHelpersTaskConstants.SOURCE);

            return xmlHelper.write(input);
        }
    }
}
