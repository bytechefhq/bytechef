/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.ai.copilot.CopilotGenerationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class WorkflowDescriptionCopilotGeneratorImpl implements WorkflowDescriptionCopilotGenerator {

    private final OverrideChatClientResolver overrideChatClientResolver;
    private final WorkflowService workflowService;
    private final WorkflowDescriptionPromptBuilder promptBuilder;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public WorkflowDescriptionCopilotGeneratorImpl(
        OverrideChatClientResolver overrideChatClientResolver, WorkflowService workflowService,
        WorkflowDescriptionPromptBuilder promptBuilder, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.overrideChatClientResolver = overrideChatClientResolver;
        this.workflowService = workflowService;
        this.promptBuilder = promptBuilder;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public WorkflowDescriptionCopilotResult generate(WorkflowDescriptionCopilotRequest request) {
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(request.environmentId());

        if (effectiveEnvironmentId != request.environmentId()) {
            request = request.withEnvironmentId(effectiveEnvironmentId);
        }

        ChatModel chatModel = resolveChatModel(request.environmentId());

        Workflow workflow = workflowService.getWorkflow(request.workflowId());

        String prompt = promptBuilder.build(workflow.getDefinition(), request.workflowNodeName());

        String value = clean(call(chatModel, prompt));

        String scope = request.workflowNodeName() == null ? "workflow" : "node";

        if (value.isEmpty()) {
            record(scope, "empty");

            throw new CopilotGenerationException("The AI model returned no description; please try again.");
        }

        record(scope, "success");

        return new WorkflowDescriptionCopilotResult(value);
    }

    private ChatModel resolveChatModel(long environmentId) {
        int environment = Math.toIntExact(environmentId);

        ChatModel chatModel = overrideChatClientResolver.resolveDefaultChatModel(environment);

        if (chatModel == null) {
            throw ChatModelUnavailableException.noProviderEnabled(environment);
        }

        return chatModel;
    }

    private static String call(ChatModel chatModel, String promptText) {
        ChatResponse chatResponse = chatModel.call(new Prompt(promptText));

        Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation is required");

        return generation.getOutput()
            .getText();
    }

    private static String clean(@Nullable String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("```[a-zA-Z]*", "")
            .strip();
    }

    private void record(String scope, String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_workflow_description_copilot_generate")
            .tag("scope", scope)
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
