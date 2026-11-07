/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolution;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ResearchToolCallback implements ToolCallback {

    static final String TOOL_NAME = "research";

    private static final Logger log = LoggerFactory.getLogger(ResearchToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a web research request to a specialised research subagent.
            The subagent searches the public web with the web search tools enabled for this environment,
            reads the most relevant pages, synthesises the findings, and returns a self-contained markdown
            report with citations. Use this tool when the user asks for up-to-date information, comparisons,
            market research, documentation lookups, or any topic that needs several web sources read and
            compared. The report includes a summary, key findings, a detailed narrative, a sources list, and
            open questions.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "topic": {
                        "type": "string",
                        "description": "The research topic or question. Be specific: the subagent uses this as its primary prompt."
                    }
                },
                "required": ["topic"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;
    private final List<ToolCallback> webSearchToolCallbacks;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ResearchToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        List<ToolCallback> webSearchToolCallbacks) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
        this.webSearchToolCallbacks = List.copyOf(webSearchToolCallbacks);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            ResearchInput input = JsonUtils.read(toolInput, ResearchInput.class);

            String topic = input.topic();

            if (topic == null || topic.isBlank()) {
                return ToolErrors.toolError("topic is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            ChatClient researchChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            String report = CurrentAgentContext.callWith(
                AiHubAgentType.RESEARCH, parentAgent,
                () -> researchChatClient.prompt(topic)
                    .tools(webSearchToolCallbacks)
                    .toolContext(parentContext)
                    .call()
                    .content());

            if (report == null) {
                log.warn("research subagent returned null for topic='{}'", topic);

                return ToolErrors.toolError("research subagent returned null");
            }

            return JsonUtils.write(Map.of("report", report));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (ChatModelUnavailableException exception) {
            log.warn("research cannot run its sub-agent: {}", exception.getMessage());

            throw exception;
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(ResearchToolCallback.class, TOOL_NAME, exception);
        }
    }

    public record ResearchInput(@Nullable String topic) {
    }
}
