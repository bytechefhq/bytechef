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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the {@code slide_builder} subagent to the parent ai_hub BUILD
 * agent.
 *
 * <p>
 * When the parent LLM invokes this tool it passes a JSON object with a {@code topic} field (and optional
 * {@code outlineOrSourceFileId}, {@code filename}, {@code slideCount} fields). The callback delegates to a
 * pre-configured {@link ChatClient} that carries slide-deck-construction tools ({@code createSlideDeck},
 * {@code createBinaryAssetFile}, {@code getAssetFileContent}, {@code openFileTab}) and the
 * {@code prompt_slide_builder.txt} system prompt. The isolated chat client context means the parent never sees the
 * construction transcript; it only receives the one-paragraph summary returned by {@code call()}.
 *
 * <p>
 * <b>Why hand-rolled instead of {@code ChatTool.builder()}?</b> The same reason as {@link ResearchToolCallback} — no
 * API exists to register a named subagent backed by an externally-constructed {@link ChatClient}. The hand-rolled
 * approach achieves the same architecture without incompatible tooling.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SlideBuilderToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(SlideBuilderToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate .pptx presentation creation to a specialised slide_builder subagent.
            The subagent drafts the outline, calls createSlideDeck to build the file, and
            saves it as a workspace asset.

            If outlineOrSourceFileId is provided, the subagent reads that workspace file
            for source material (headings and key points) to use as the backbone of the
            deck. Otherwise it drafts from the topic directly. Slide count defaults to
            5-12; maximum is 15.

            After it returns, the .pptx file has already been saved and opened; summarize
            the deck in one paragraph in chat.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "topic": {
                        "type": "string",
                        "description": "The subject or title of the presentation. Be specific enough for the subagent to draft a useful outline."
                    },
                    "outlineOrSourceFileId": {
                        "type": "string",
                        "description": "Optional workspace file id containing an outline or source material (text/markdown). The subagent will read and use it as the backbone of the deck."
                    },
                    "filename": {
                        "type": "string",
                        "description": "Optional preferred filename ending in .pptx (e.g. q3-roadmap.pptx). If omitted the subagent will choose a descriptive name."
                    },
                    "slideCount": {
                        "type": "integer",
                        "description": "Optional desired number of slides (5-15). Defaults to 5-12 if omitted."
                    }
                },
                "required": ["topic"]
            }""";

    private final ChatClient slideBuilderChatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SlideBuilderToolCallback(ChatClient slideBuilderChatClient) {
        this.slideBuilderChatClient = slideBuilderChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("slide_builder")
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
            SlideBuilderInput input = jsonMapper.readValue(toolInput, SlideBuilderInput.class);

            String topic = input.topic();

            if (topic == null || topic.isBlank()) {
                return toolError("topic is required and must not be blank");
            }

            StringBuilder promptBuilder = new StringBuilder(topic);

            String outlineOrSourceFileId = input.outlineOrSourceFileId();

            if (outlineOrSourceFileId != null && !outlineOrSourceFileId.isBlank()) {
                promptBuilder.append("\n\nSource file id for outline/material: ")
                    .append(outlineOrSourceFileId);
            }

            String filename = input.filename();

            if (filename != null && !filename.isBlank()) {
                promptBuilder.append("\nSave as filename: ")
                    .append(filename);
            }

            if (input.slideCount() != null) {
                promptBuilder.append("\nRequested slide count: ")
                    .append(input.slideCount());
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            String result = CurrentAgentContext.callWith(AiHubAgentType.SLIDE_BUILDER, parentAgent,
                () -> slideBuilderChatClient.prompt(promptBuilder.toString())
                    .call()
                    .content());

            if (result == null) {
                log.warn(
                    "slide_builder subagent returned null for topic='{}'",
                    input.topic());

                return ToolErrors.toolError(jsonMapper, "slide_builder subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "slide_builder rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, SlideBuilderToolCallback.class, "slide_builder", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record SlideBuilderInput(
        String topic, @Nullable String outlineOrSourceFileId, @Nullable String filename,
        @Nullable Integer slideCount) {
    }
}
