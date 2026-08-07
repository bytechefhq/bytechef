/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.tool.ResearchToolCallback;
import com.bytechef.ee.ai.hub.usage.AiHubToolUsageContextResolver;
import com.bytechef.ee.platform.ai.tool.usage.MeteredToolCallback;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import com.bytechef.platform.ai.tool.BraveWebSearchTools;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the {@code researchChatClient} Spring bean used by the ai_hub agents.
 *
 * <p>
 * The research subagent is a dedicated {@link ChatClient} pre-loaded with whichever web search tools the deployment has
 * configured and the {@code prompt_research.txt} system prompt. Its isolated context means the parent ai_hub agent
 * never sees the browsing transcript — it only receives the final synthesised markdown report.
 *
 * <p>
 * The {@link ResearchToolCallback} is intentionally <em>not</em> a Spring bean. It is instantiated inline in the ai_hub
 * ASK and BUILD agent bean methods (via {@link #createResearchToolCallback}) so that it is registered only on those two
 * agents. Other agents consume {@code ObjectProvider<ToolCallback>.orderedStream()} and must not receive the research
 * tool.
 *
 * <p>
 * The configuration is guarded by {@link OnAnyWebSearchToolsCondition} so it registers when either
 * {@link FirecrawlTools} or {@link BraveWebSearchTools} is present, and deployments with neither continue to boot
 * normally without the research tool. A Brave-only deployment can search but not fetch page bodies, so its reports are
 * synthesised from search snippets.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Conditional(ResearchConfiguration.OnAnyWebSearchToolsCondition.class)
public class ResearchConfiguration {

    @Bean
    ChatClient researchChatClient(
        ChatModel chatModel,
        Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools,
        ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider,
        ObjectProvider<AiHubTaskService> taskServiceProvider,
        JsonMapper jsonMapper,
        @Value("classpath:prompt_research.txt") Resource promptResource) {

        String systemPrompt = readPrompt(promptResource);

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        firecrawlTools.ifPresent(tools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(tools))));
        braveWebSearchTools.ifPresent(tools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(tools))));

        ToolUsageRecorder usageRecorder = toolUsageRecorderProvider.getIfAvailable();
        AiHubTaskService taskService = taskServiceProvider.getIfAvailable();

        List<ToolCallback> wrapped = new ArrayList<>(toolCallbacks.size());

        for (ToolCallback callback : toolCallbacks) {
            String name = callback.getToolDefinition()
                .name();

            String meteredToolName = mapMeteredToolName(name);

            if (meteredToolName != null && usageRecorder != null && taskService != null) {
                wrapped.add(new MeteredToolCallback(
                    callback, meteredToolName, 1, usageRecorder,
                    AiHubToolUsageContextResolver.create(taskService, meteredToolName),
                    MeteredToolCallback.singleStringField("websiteMap".equals(name) ? "url"
                        : "webpageScrape".equals(name) ? "url" : "query"),
                    jsonMapper));
            } else {
                wrapped.add(callback);
            }
        }

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(wrapped.toArray(new ToolCallback[0]))
            .build();
    }

    /**
     * Maps a web search tool's {@code @Tool} method name to the canonical {@code tool_name} stored in
     * {@code ai_hub_tool_usage}. Returns {@code null} for cheap or non-instrumented tools.
     */
    static String mapMeteredToolName(String toolName) {
        return switch (toolName) {
            case "webSearch" -> "firecrawl_search";
            case "webpageScrape" -> "firecrawl_scrape";
            case "braveWebSearch" -> "brave_search";
            default -> null;
        };
    }

    public static ResearchToolCallback createResearchToolCallback(ChatClient researchChatClient) {
        return new ResearchToolCallback(researchChatClient);
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read research prompt resource: " + resource.getDescription(), exception);
        }
    }

    /**
     * Matches when at least one web search provider bean is present. {@code @ConditionalOnBean} ANDs its values, so
     * "either provider" needs a nested-condition composite.
     */
    static class OnAnyWebSearchToolsCondition extends AnyNestedCondition {

        OnAnyWebSearchToolsCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(FirecrawlTools.class)
        static class OnFirecrawlTools {

            private OnFirecrawlTools() {
            }
        }

        @ConditionalOnBean(BraveWebSearchTools.class)
        static class OnBraveWebSearchTools {

            private OnBraveWebSearchTools() {
            }
        }
    }
}
