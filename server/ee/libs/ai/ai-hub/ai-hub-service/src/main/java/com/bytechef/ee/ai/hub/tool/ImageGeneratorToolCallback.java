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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the {@code image_generator} subagent to the parent ai_hub
 * BUILD agent.
 *
 * <p>
 * When the parent LLM invokes this tool it passes a JSON object with a {@code prompt} field (and optional {@code size},
 * {@code style}, {@code filename} fields). The callback delegates to a pre-configured {@link ChatClient} that carries
 * image-generation tools ({@code generateImage}, {@code createBinaryAssetFile}, {@code openFileTab}) and the
 * {@code prompt_image_generator.txt} system prompt. The isolated chat client context means the parent never sees the
 * generation transcript; it only receives the one-sentence summary returned by {@code call()}.
 *
 * <p>
 * <b>Why hand-rolled instead of {@code TaskTool.builder()}?</b> The same reason as {@link ResearchToolCallback} — no
 * API exists to register a named subagent backed by an externally-constructed {@link ChatClient}. The hand-rolled
 * approach achieves the same architecture without incompatible tooling.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ImageGeneratorToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ImageGeneratorToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate image creation to a specialised image_generator subagent. The subagent
            generates the image via OpenAI's image API and saves it as a workspace file.

            Use for banners, diagrams, illustrations, avatars, hero images, or any other
            visual asset the user requests. Pass the user's description verbatim as the
            'prompt' field. Optionally supply 'size' (1024x1024, 1024x1792, 1792x1024),
            'style' (e.g. photorealistic, illustration, minimal), or a preferred 'filename'.

            After it returns, the file has already been saved and opened; summarize in
            one sentence in chat.""";

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

    private final ChatClient imageGeneratorChatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ImageGeneratorToolCallback(ChatClient imageGeneratorChatClient) {
        this.imageGeneratorChatClient = imageGeneratorChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("image_generator")
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
            ImageGeneratorInput input = jsonMapper.readValue(toolInput, ImageGeneratorInput.class);

            if (input.prompt() == null || input.prompt()
                .isBlank()) {
                return toolError("prompt is required and must not be blank");
            }

            StringBuilder promptBuilder = new StringBuilder(input.prompt());

            if (input.size() != null && !input.size()
                .isBlank()) {
                promptBuilder.append("\n\nRequested size: ")
                    .append(input.size());
            }

            if (input.style() != null && !input.style()
                .isBlank()) {
                promptBuilder.append("\nRequested style: ")
                    .append(input.style());
            }

            if (input.filename() != null && !input.filename()
                .isBlank()) {
                promptBuilder.append("\nSave as filename: ")
                    .append(input.filename());
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            String result = CurrentAgentContext.callWith(AiHubAgentType.IMAGE_GENERATOR, parentAgent,
                () -> imageGeneratorChatClient.prompt(promptBuilder.toString())
                    .call()
                    .content());

            if (result == null) {
                log.warn(
                    "image_generator subagent returned null for prompt='{}'",
                    input.prompt());

                return ToolErrors.toolError(jsonMapper, "image_generator subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "image_generator rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ImageGeneratorToolCallback.class, "image_generator", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record ImageGeneratorInput(
        String prompt, @Nullable String size, @Nullable String style, @Nullable String filename) {
    }
}
