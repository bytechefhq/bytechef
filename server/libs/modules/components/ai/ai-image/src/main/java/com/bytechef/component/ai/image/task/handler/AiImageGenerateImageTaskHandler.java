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

package com.bytechef.component.ai.image.task.handler;

import static com.bytechef.component.ai.image.constant.AiImageConstants.GENERATE_IMAGE;
import static com.bytechef.platform.component.definition.AiUniversalComponentDefinition.AI_IMAGE;

import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(AI_IMAGE + "/v1/" + GENERATE_IMAGE)
public class AiImageGenerateImageTaskHandler extends AbstractTaskHandler {

    public AiImageGenerateImageTaskHandler(ActionDefinitionFacade actionDefinitionFacade) {
        super(AI_IMAGE, 1, GENERATE_IMAGE, actionDefinitionFacade);
    }
}
