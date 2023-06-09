
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

package com.bytechef.component.logger.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.util.MapValueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.bytechef.component.logger.constant.LoggerConstants.INFO;
import static com.bytechef.component.logger.constant.LoggerConstants.TEXT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class LoggerInfoAction {

    private static final Logger logger = LoggerFactory.getLogger(LoggerInfoAction.class);

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(INFO)
        .title("Info")
        .properties(string(TEXT))
        .perform(LoggerInfoAction::perform);

    protected static Object perform(Map<String, ?> inputParameters, Context context) {
        logger.info(MapValueUtils.getString(inputParameters, TEXT));

        return null;
    }
}
