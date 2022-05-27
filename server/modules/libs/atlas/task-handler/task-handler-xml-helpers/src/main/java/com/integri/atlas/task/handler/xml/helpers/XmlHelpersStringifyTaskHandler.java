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

import static com.integri.atlas.task.handler.xml.helpers.XmlHelpersTaskConstants.PROPERTY_SOURCE;
import static com.integri.atlas.task.handler.xml.helpers.XmlHelpersTaskConstants.TASK_XML_HELPERS;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.xml.XmlHelper;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_XML_HELPERS + "/stringify")
public class XmlHelpersStringifyTaskHandler implements TaskHandler<String> {

    private final XmlHelper xmlHelper;

    public XmlHelpersStringifyTaskHandler(XmlHelper xmlHelper) {
        this.xmlHelper = xmlHelper;
    }

    @Override
    public String handle(TaskExecution taskExecution) {
        Object input = taskExecution.getRequired(PROPERTY_SOURCE);

        return xmlHelper.write(input);
    }
}
