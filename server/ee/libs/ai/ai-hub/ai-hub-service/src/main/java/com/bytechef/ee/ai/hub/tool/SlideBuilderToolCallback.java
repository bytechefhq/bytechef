/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SlideBuilderToolCallback implements ToolCallback {

    static final String TOOL_NAME = "slide_builder";

    private static final Logger log = LoggerFactory.getLogger(SlideBuilderToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate .pptx presentation creation to a specialised slide_builder subagent.
            The subagent drafts the outline, calls createSlideDeck to build the file, and
            saves it as a workspace file.

            If outlineOrSourceFileId is provided, the subagent reads that workspace file
            for source material (headings and key points) to use as the backbone of the
            deck. Otherwise it drafts from the topic directly. Slide count defaults to
            5-12; maximum is 15.

            After it returns, the .pptx file has already been saved; summarize the deck in
            one paragraph in chat and name the file.""";

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

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;
    private final List<ToolCallback> toolCallbacks;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SlideBuilderToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        List<ToolCallback> toolCallbacks) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
        this.toolCallbacks = List.copyOf(toolCallbacks);
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
            SlideBuilderInput input = JsonUtils.read(toolInput, SlideBuilderInput.class);

            String topic = input.topic();

            if (topic == null || topic.isBlank()) {
                return ToolErrors.toolError("topic is required and must not be blank");
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

            Integer slideCount = input.slideCount();

            if (slideCount != null) {
                promptBuilder.append("\nRequested slide count: ")
                    .append(slideCount);
            }

            String summary = SubAgentCalls.call(
                chatClientFactory, chatModelResolver, AiHubAgentType.SLIDE_BUILDER, promptBuilder.toString(),
                toolCallbacks, SubAgentCalls.parentContext(toolContext));

            if (summary == null) {
                log.warn("slide_builder subagent returned null for topic='{}'", topic);

                return ToolErrors.toolError("slide_builder subagent returned null");
            }

            return JsonUtils.write(Map.of("summary", summary));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (ChatModelUnavailableException exception) {
            log.warn("slide_builder cannot run its sub-agent: {}", exception.getMessage());

            throw exception;
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(SlideBuilderToolCallback.class, TOOL_NAME, exception);
        }
    }

    public record SlideBuilderInput(
        @Nullable String topic, @Nullable String outlineOrSourceFileId, @Nullable String filename,
        @Nullable Integer slideCount) {
    }
}
