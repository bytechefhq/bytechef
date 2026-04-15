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

package com.bytechef.component.ai.agenticai.tool;

import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_DESCRIPTION;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class AgenticAiTool {

    public static ClusterElementDefinition<MultipleConnectionsToolFunction> of(ActionDefinition actionDefinition) {
        MultipleConnectionsPerformFunction performFn = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("agenticAi")
            .title("Agentic AI")
            .description("Agentic AI tool")
            .properties(
                List.of(
                    string(TOOL_NAME)
                        .label("Name")
                        .description("The tool name exposed to the AI model.")
                        .expressionEnabled(false)
                        .required(true),
                    string(TOOL_DESCRIPTION)
                        .label("Description")
                        .description("The tool description exposed to the AI model.")
                        .controlType(TEXT_AREA)
                        .expressionEnabled(false)
                        .required(true),
                    string(GOAL_DESCRIPTION)
                        .label("Goal Description")
                        .description("Describe the goal the agentic AI should achieve using the configured tools.")
                        .controlType(TEXT_AREA)
                        .required(true),
                    SYSTEM_PROMPT_PROPERTY,
                    RESPONSE_PROPERTY))
            .type(TOOLS)
            .object(
                () -> (inputParameters, connectionParameters, extensions, componentConnections, context) -> performFn
                    .apply(inputParameters, componentConnections, extensions, new ActionContextAdapter(context)));
    }

    private AgenticAiTool() {
    }

    private static class ActionContextAdapter implements ActionContext {
        private final ClusterElementContext context;

        public ActionContextAdapter(ClusterElementContext context) {
            this.context = context;
        }

        @Override
        public Approval.Links approval(ContextFunction<Approval, Approval.Links> approvalFunction) {
            throw new UnsupportedOperationException(
                "Approval is not supported inside an Agentic AI tool cluster element; call it from an action instead.");
        }

        @Override
        public <R> R data(ContextFunction<Data, R> dataFunction) {
            throw new UnsupportedOperationException(
                "Data operations are not supported inside an Agentic AI tool cluster element; " +
                    "call them from an action instead.");
        }

        @Override
        public <R> R convert(ContextFunction<Convert, R> convertFunction) {
            return context.convert(convertFunction);
        }

        @Override
        public void event(java.util.function.Consumer<Event> eventConsumer) {
            throw new UnsupportedOperationException(
                "Events are not supported inside an Agentic AI tool cluster element; " +
                    "emit them from an action instead.");
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
        public void suspend(Suspend suspend) {
            throw new UnsupportedOperationException(
                "Suspend is not supported inside an Agentic AI tool cluster element; " +
                    "use it from an action instead.");
        }

        @Override
        public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
            return context.xml(xmlFunction);
        }
    }
}
