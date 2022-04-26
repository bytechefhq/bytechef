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

package com.integri.atlas.task.handler.xml.converter;

import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.xml.helper.XMLHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("xmlConverter")
public class XMLConverterTaskHandler implements TaskHandler<Object> {

    private final JSONHelper jsonHelper;
    private final XMLHelper xmlHelper;

    public XMLConverterTaskHandler(JSONHelper jsonHelper, XMLHelper xmlHelper) {
        this.jsonHelper = jsonHelper;
        this.xmlHelper = xmlHelper;
    }

    private enum Operation {
        FROM_XML,
        TO_XML,
    }

    @Override
    public Object handle(TaskExecution taskExecution) {
        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));

        if (operation == Operation.FROM_XML) {
            String input = taskExecution.getRequiredString("input");

            return xmlHelper.deserialize(input);
        } else {
            Object input = jsonHelper.checkJSON(taskExecution.getRequired("input"));

            return xmlHelper.serialize(input);
        }
    }
}
