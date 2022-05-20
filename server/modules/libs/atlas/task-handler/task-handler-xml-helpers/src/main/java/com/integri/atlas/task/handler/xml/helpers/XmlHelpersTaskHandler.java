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

import static com.integri.atlas.task.handler.xml.helpers.XmlHelpersTaskConstants.*;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.json.helper.JsonHelper;
import com.integri.atlas.task.handler.xml.helper.XmlHelper;
import com.integri.atlas.task.handler.xml.helpers.XmlHelpersTaskConstants.Operation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_XML_HELPERS)
public class XmlHelpersTaskHandler implements TaskHandler<Object> {

    private final JsonHelper jsonHelper;
    private final XmlHelper xmlHelper;

    public XmlHelpersTaskHandler(JsonHelper jsonHelper, XmlHelper xmlHelper) {
        this.jsonHelper = jsonHelper;
        this.xmlHelper = xmlHelper;
    }

    @Override
    public Object handle(TaskExecution taskExecution) {
        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired(PROPERTY_OPERATION)));

        if (operation == Operation.XML_TO_JSON) {
            String input = taskExecution.getRequiredString(PROPERTY_SOURCE);

            return xmlHelper.read(input);
        } else {
            Object input = jsonHelper.check(taskExecution.getRequired(PROPERTY_SOURCE));

            return xmlHelper.write(input);
        }
    }
}
