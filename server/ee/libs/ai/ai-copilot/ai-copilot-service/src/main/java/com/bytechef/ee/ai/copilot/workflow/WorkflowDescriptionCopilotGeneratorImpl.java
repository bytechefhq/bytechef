/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class WorkflowDescriptionCopilotGeneratorImpl implements WorkflowDescriptionCopilotGenerator {

    private final ChatModel chatModel;
    private final WorkflowService workflowService;
    private final WorkflowDescriptionPromptBuilder promptBuilder;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public WorkflowDescriptionCopilotGeneratorImpl(
        ChatModel chatModel, WorkflowService workflowService, WorkflowDescriptionPromptBuilder promptBuilder,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.chatModel = chatModel;
        this.workflowService = workflowService;
        this.promptBuilder = promptBuilder;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public WorkflowDescriptionCopilotResult generate(WorkflowDescriptionCopilotRequest request) {
        Workflow workflow = workflowService.getWorkflow(request.workflowId());

        String prompt = promptBuilder.build(workflow.getDefinition(), request.workflowNodeName());

        String value = clean(call(prompt));

        record(request.workflowNodeName() == null ? "workflow" : "node");

        return new WorkflowDescriptionCopilotResult(value);
    }

    private String call(String promptText) {
        return chatModel.call(new Prompt(promptText))
            .getResult()
            .getOutput()
            .getText();
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("```[a-zA-Z]*", "")
            .strip();
    }

    private void record(String scope) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_workflow_description_copilot_generate")
            .tag("scope", scope)
            .tag("outcome", "success")
            .register(meterRegistry)
            .increment();
    }
}
