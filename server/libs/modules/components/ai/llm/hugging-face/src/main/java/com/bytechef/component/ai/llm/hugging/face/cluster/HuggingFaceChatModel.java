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

package com.bytechef.component.ai.llm.hugging.face.cluster;

import static com.bytechef.component.ai.llm.hugging.face.constant.HuggingFaceConstants.URL_PROPERTY;

import com.bytechef.component.ai.llm.hugging.face.action.HuggingFaceChatAction;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Monika Kušter
 */
public class HuggingFaceChatModel {

    public static final ClusterElementDefinition<ModelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ModelFunction>clusterElement("model")
            .title("Hugging Face Model")
            .description("Hugging Face model.")
            .type(ModelFunction.MODEL)
            .object(() -> HuggingFaceChatModel::apply)
            .properties(URL_PROPERTY);

    protected static ChatModel apply(Parameters inputParameters, Parameters connectionParameters) {
        return HuggingFaceChatAction.CHAT_MODEL.createChatModel(inputParameters, connectionParameters);
    }
}
