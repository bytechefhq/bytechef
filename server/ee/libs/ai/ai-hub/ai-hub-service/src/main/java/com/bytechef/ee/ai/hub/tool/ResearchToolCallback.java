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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the research subagent to the parent ai_hub agent.
 *
 * <p>
 * When the parent LLM invokes this tool it passes a JSON object with a {@code topic} field. The callback delegates to a
 * pre-configured {@link ChatClient} that carries Firecrawl search/scrape tools and the {@code prompt_research.txt}
 * system prompt. The isolated chat client context means the parent never sees the browsing transcript; it only receives
 * the synthesised markdown report returned by {@code call()}.
 *
 * <p>
 * <b>Why hand-rolled instead of {@code ChatTool.builder()}?</b> The
 * {@code org.springaicommunity.agent.tools.chat.ChatTool} library's {@code ClaudeSubagentExecutor} injects a shared set
 * of Claude Code SDK tools (Grep, Glob, Shell, FileSystem, SmartWebFetch) that are incompatible with ByteChef's
 * Spring-managed {@link com.bytechef.platform.ai.tool.FirecrawlTools} bean. There is no API to register a named
 * subagent backed by an externally-constructed {@link ChatClient}; the builder only accepts {@link ChatClient.Builder}
 * instances from which it constructs its own client. The hand-rolled approach achieves the same architecture — parent
 * delegates via tool call, isolated chat client synthesises, result is returned — without the incompatible tooling.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ResearchToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ResearchToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a web research request to a specialised research subagent.
            The subagent searches the public web using Firecrawl, fetches and reads relevant pages,
            synthesises the findings, and returns a self-contained markdown report with citations.
            Use this tool when the user asks for up-to-date information, comparisons, market research,
            documentation lookups, or any topic that benefits from browsing the live web.
            The report includes a summary, key findings, a detailed narrative, a sources list, and open
            questions.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "topic": {
                        "type": "string",
                        "description": "The research topic or question. Be specific — the subagent uses this as its primary prompt."
                    }
                },
                "required": ["topic"]
            }""";

    private final ChatClient researchChatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ResearchToolCallback(ChatClient researchChatClient) {
        this.researchChatClient = researchChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("research")
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
            ResearchInput input = jsonMapper.readValue(toolInput, ResearchInput.class);

            if (input.topic() == null || input.topic()
                .isBlank()) {
                return toolError("topic is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            // Forward parent AiHubToolInvocationContext into the subagent so any workspace-scoped tool
            // callbacks the subagent invokes can resolve workspaceId/userId/environmentId. Mirrors the fix
            // mirrored in the Copilot sub-agent ToolCallbacks; research subagent registers no
            // workspace-scoped tools today, but the forwarding is correct ahead of any future addition and
            // costs nothing at runtime.
            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String report = CurrentAgentContext.callWith(AiHubAgentType.RESEARCH, parentAgent,
                () -> researchChatClient.prompt(input.topic())
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (report == null) {
                log.warn(
                    "research subagent returned null for topic='{}'",
                    input.topic());

                return ToolErrors.toolError(jsonMapper, "research subagent returned null");
            }

            return jsonMapper.writeValueAsString(Map.of("report", report));
        } catch (JacksonException exception) {
            log.warn(
                "research rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ResearchToolCallback.class, "research", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record ResearchInput(String topic) {
    }
}
