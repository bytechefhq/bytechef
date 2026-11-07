/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.Resource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubAgentContributingChatClient implements ChatClient {

    private final ChatClient delegate;
    private final List<SubAgentAdvisorContributor> contributors;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private SubAgentContributingChatClient(ChatClient delegate, List<SubAgentAdvisorContributor> contributors) {
        this.delegate = delegate;
        this.contributors = contributors;
    }

    public static ChatClient wrap(ChatClient chatClient, List<SubAgentAdvisorContributor> contributors) {
        if (contributors.isEmpty()) {
            return chatClient;
        }

        return new SubAgentContributingChatClient(chatClient, List.copyOf(contributors));
    }

    @Override
    public ChatClientRequestSpec prompt() {
        return new ContributingRequestSpec(delegate.prompt());
    }

    @Override
    public ChatClientRequestSpec prompt(String content) {
        return new ContributingRequestSpec(delegate.prompt(content));
    }

    @Override
    public ChatClientRequestSpec prompt(Prompt prompt) {
        return new ContributingRequestSpec(delegate.prompt(prompt));
    }

    @Override
    public Builder mutate() {
        return delegate.mutate();
    }

    private final class ContributingRequestSpec implements ChatClientRequestSpec {

        private ChatClientRequestSpec delegateSpec;
        private @Nullable Map<String, Object> capturedToolContext;

        private ContributingRequestSpec(ChatClientRequestSpec delegateSpec) {
            this.delegateSpec = delegateSpec;
        }

        @Override
        public Builder mutate() {
            return delegateSpec.mutate();
        }

        @Override
        public ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer) {
            delegateSpec = delegateSpec.advisors(consumer);

            return this;
        }

        @Override
        public ChatClientRequestSpec advisors(Advisor... advisors) {
            delegateSpec = delegateSpec.advisors(advisors);

            return this;
        }

        @Override
        public ChatClientRequestSpec advisors(List<Advisor> advisors) {
            delegateSpec = delegateSpec.advisors(advisors);

            return this;
        }

        @Override
        public ChatClientRequestSpec messages(Message... messages) {
            delegateSpec = delegateSpec.messages(messages);

            return this;
        }

        @Override
        public ChatClientRequestSpec messages(List<Message> messages) {
            delegateSpec = delegateSpec.messages(messages);

            return this;
        }

        @Override
        public <B extends ChatOptions.Builder<?>> ChatClientRequestSpec options(B optionsBuilder) {
            delegateSpec = delegateSpec.options(optionsBuilder);

            return this;
        }

        @Override
        public ChatClientRequestSpec tools(Object... toolObjects) {
            delegateSpec = delegateSpec.tools(toolObjects);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks) {
            delegateSpec = delegateSpec.toolCallbacks(toolCallbacks);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks) {
            delegateSpec = delegateSpec.toolCallbacks(toolCallbacks);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders) {
            delegateSpec = delegateSpec.toolCallbacks(toolCallbackProviders);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolContext(Map<String, Object> toolContext) {
            this.capturedToolContext = new HashMap<>(toolContext);

            delegateSpec = delegateSpec.toolContext(toolContext);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(String text) {
            delegateSpec = delegateSpec.system(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(Resource text, Charset charset) {
            delegateSpec = delegateSpec.system(text, charset);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(Resource text) {
            delegateSpec = delegateSpec.system(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer) {
            delegateSpec = delegateSpec.system(consumer);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(String text) {
            delegateSpec = delegateSpec.user(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(Resource text, Charset charset) {
            delegateSpec = delegateSpec.user(text, charset);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(Resource text) {
            delegateSpec = delegateSpec.user(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer) {
            delegateSpec = delegateSpec.user(consumer);

            return this;
        }

        @Override
        public ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer) {
            delegateSpec = delegateSpec.templateRenderer(templateRenderer);

            return this;
        }

        @Override
        public CallResponseSpec call() {
            attachContributedAdvisors();

            return delegateSpec.call();
        }

        @Override
        public StreamResponseSpec stream() {
            attachContributedAdvisors();

            return delegateSpec.stream();
        }

        private void attachContributedAdvisors() {
            for (SubAgentAdvisorContributor contributor : contributors) {
                delegateSpec = contributor.contribute(delegateSpec, capturedToolContext);
            }
        }
    }
}
