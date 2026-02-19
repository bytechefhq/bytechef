/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ai.agent.tool;

import static com.bytechef.component.ai.agent.constant.AiAgentConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.ai.agent.action.AiAgentChatAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import java.util.List;

public class AiAgentChatTool {

    public final ClusterElementDefinition<MultipleConnectionsToolFunction> clusterElementDefinition;

    public AiAgentChatTool(AiAgentChatAction aiAgentChatAction) {
        MultipleConnectionsPerformFunction performFn =
            (MultipleConnectionsPerformFunction) aiAgentChatAction.actionDefinition
                .getPerform()
                .orElseThrow();

        this.clusterElementDefinition = ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("aiAgent")
            .title("AI Agent")
            .description("AI Agent tool")
            .properties(
                List.of(
                    string("description")
                        .label("Description")
                        .description("Description used when registering this AI Agent as a tool."),
                    FORMAT_PROPERTY,
                    PROMPT_PROPERTY,
                    SYSTEM_PROMPT_PROPERTY,
                    ATTACHMENTS_PROPERTY,
                    MESSAGES_PROPERTY,
                    RESPONSE_PROPERTY,
                    string(CONVERSATION_ID)
                        .description("The conversation id used in conjunction with memory.")))
            .type(TOOLS)
            .object(
                () -> (inputParameters, connectionParameters, extensions, componentConnections, context) -> performFn
                    .apply(inputParameters, componentConnections, extensions, new ActionContextAdapter(context)));
    }

    private static class ActionContextAdapter implements ActionContext {
        private final ClusterElementContext context;

        public ActionContextAdapter(ClusterElementContext context) {
            this.context = context;
        }

        @Override
        public Approval.Links approval(ContextFunction<Approval, Approval.Links> approvalFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R data(ContextFunction<Data, R> dataFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R convert(ContextFunction<Convert, R> convertFunction) {
            return context.convert(convertFunction);
        }

        @Override
        public void event(java.util.function.Consumer<Event> eventConsumer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R encoder(ContextFunction<Encoder, R> encoderFunction) {
            return context.encoder(encoderFunction);
        }

        @Override
        public <R> R file(ContextFunction<File, R> fileFunction) {
            return context.file(fileFunction);
        }

        @Override
        public <R> R http(ContextFunction<Http, R> httpFunction) {
            return context.http(httpFunction);
        }

        @Override
        public boolean isEditorEnvironment() {
            return context.isEditorEnvironment();
        }

        @Override
        public <R> R json(ContextFunction<Json, R> jsonFunction) {
            return context.json(jsonFunction);
        }

        @Override
        public void log(ContextConsumer<Log> logConsumer) {
            context.log(logConsumer);
        }

        @Override
        public <R> R mimeType(ContextFunction<MimeType, R> mimeTypeContextFunction) {
            return context.mimeType(mimeTypeContextFunction);
        }

        @Override
        public <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction) {
            return context.outputSchema(outputSchemaFunction);
        }

        @Override
        public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
            return context.xml(xmlFunction);
        }
    }
}
