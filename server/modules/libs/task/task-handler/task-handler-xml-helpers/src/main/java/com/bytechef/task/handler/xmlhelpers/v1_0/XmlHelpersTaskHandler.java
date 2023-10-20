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
import com.bytechef.task.commons.xml.XmlHelper;
import com.bytechef.task.handler.xmlhelpers.XmlHelpersTaskConstants;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class XmlHelpersTaskHandler {

    @Component(XmlHelpersTaskConstants.XML_HELPERS
            + "/"
            + XmlHelpersTaskConstants.VERSION_1_0
            + "/"
            + XmlHelpersTaskConstants.XML_PARSE)
    public static class XmlHelpersParseTaskHandler implements TaskHandler<Map<String, ?>> {

        private final XmlHelper xmlHelper;

        public XmlHelpersParseTaskHandler(XmlHelper xmlHelper) {
            this.xmlHelper = xmlHelper;
        }

        @Override
        public Map<String, ?> handle(TaskExecution taskExecution) {
            String input = taskExecution.getRequiredString(XmlHelpersTaskConstants.SOURCE);

            return xmlHelper.read(input);
        }
    }

    @Component(XmlHelpersTaskConstants.XML_HELPERS
            + "/"
            + XmlHelpersTaskConstants.VERSION_1_0
            + "/"
            + XmlHelpersTaskConstants.XML_STRINGIFY)
    public static class XmlHelpersStringifyTaskHandler implements TaskHandler<String> {

        private final XmlHelper xmlHelper;

        public XmlHelpersStringifyTaskHandler(XmlHelper xmlHelper) {
            this.xmlHelper = xmlHelper;
        }

        @Override
        public String handle(TaskExecution taskExecution) {
            Object input = taskExecution.getRequired(XmlHelpersTaskConstants.SOURCE);

            return xmlHelper.write(input);
        }
    }
}
