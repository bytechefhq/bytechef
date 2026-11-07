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
public class ImageGeneratorToolCallback implements ToolCallback {

    static final String TOOL_NAME = "image_generator";

    private static final Logger log = LoggerFactory.getLogger(ImageGeneratorToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate image creation to a specialised image_generator subagent. The subagent
            generates the image via OpenAI's image API and saves it as a PNG workspace file.

            Use for banners, diagrams, illustrations, avatars, hero images, or any other
            visual asset the user requests. Pass the user's description verbatim as the
            'prompt' field. Optionally supply 'size' (1024x1024, 1024x1792, 1792x1024),
            'style' (e.g. photorealistic, illustration, minimal), or a preferred 'filename'.

            After it returns, the file has already been saved; summarize in one sentence
            in chat and name the file.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "prompt": {
                        "type": "string",
                        "description": "Text description of the image to generate. Be specific about subject, style, and context."
                    },
                    "size": {
                        "type": "string",
                        "description": "Image size. Defaults to 1024x1024. Allowed: 1024x1024, 1024x1792 (portrait), 1792x1024 (landscape banner)."
                    },
                    "style": {
                        "type": "string",
                        "description": "Optional style hint (e.g. photorealistic, illustration, minimal, flat, abstract)."
                    },
                    "filename": {
                        "type": "string",
                        "description": "Optional preferred filename with extension (e.g. hero-banner.png). If omitted the subagent will choose a descriptive name."
                    }
                },
                "required": ["prompt"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;
    private final List<ToolCallback> toolCallbacks;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ImageGeneratorToolCallback(
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
            ImageGeneratorInput input = JsonUtils.read(toolInput, ImageGeneratorInput.class);

            String prompt = input.prompt();

            if (prompt == null || prompt.isBlank()) {
                return ToolErrors.toolError("prompt is required and must not be blank");
            }

            StringBuilder promptBuilder = new StringBuilder(prompt);

            appendHint(promptBuilder, "\n\nRequested size: ", input.size());
            appendHint(promptBuilder, "\nRequested style: ", input.style());
            appendHint(promptBuilder, "\nSave as filename: ", input.filename());

            String summary = SubAgentCalls.call(
                chatClientFactory, chatModelResolver, AiHubAgentType.IMAGE_GENERATOR, promptBuilder.toString(),
                toolCallbacks, SubAgentCalls.parentContext(toolContext));

            if (summary == null) {
                log.warn("image_generator subagent returned null for prompt='{}'", prompt);

                return ToolErrors.toolError("image_generator subagent returned null");
            }

            return JsonUtils.write(Map.of("summary", summary));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (ChatModelUnavailableException exception) {
            log.warn("image_generator cannot run its sub-agent: {}", exception.getMessage());

            throw exception;
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(ImageGeneratorToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static void appendHint(StringBuilder promptBuilder, String label, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            promptBuilder.append(label)
                .append(value);
        }
    }

    public record ImageGeneratorInput(
        @Nullable String prompt, @Nullable String size, @Nullable String style, @Nullable String filename) {
    }
}
