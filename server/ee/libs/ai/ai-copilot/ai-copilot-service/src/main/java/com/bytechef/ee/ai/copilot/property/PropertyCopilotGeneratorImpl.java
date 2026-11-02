/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.property;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.EvaluatorFunctionDefinition;
import com.bytechef.evaluator.EvaluatorFunctionDefinitionFactory;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.expression.ExpressionException;
import org.springframework.stereotype.Service;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class PropertyCopilotGeneratorImpl implements PropertyCopilotGenerator {

    private static final Logger log = LoggerFactory.getLogger(PropertyCopilotGeneratorImpl.class);

    private static final Pattern DATA_PILL_PATTERN = Pattern.compile("\\$\\{[^}]+}");

    private static final String EMPTY_VALUE_MESSAGE = "The AI model returned no value; please try again.";

    private final Evaluator evaluator;
    private final PropertyCopilotPromptBuilder promptBuilder;
    private final List<EvaluatorFunctionDefinitionFactory> evaluatorFunctionDefinitionFactories;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final OverrideChatClientResolver overrideChatClientResolver;

    public PropertyCopilotGeneratorImpl(
        Evaluator evaluator, PropertyCopilotPromptBuilder promptBuilder,
        List<EvaluatorFunctionDefinitionFactory> evaluatorFunctionDefinitionFactories,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, ObjectProvider<MeterRegistry> meterRegistryProvider,
        OverrideChatClientResolver overrideChatClientResolver) {

        this.evaluator = evaluator;
        this.promptBuilder = promptBuilder;
        this.evaluatorFunctionDefinitionFactories = evaluatorFunctionDefinitionFactories;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.meterRegistryProvider = meterRegistryProvider;
        this.overrideChatClientResolver = overrideChatClientResolver;
    }

    @Override
    public PropertyCopilotResult generate(PropertyCopilotRequest request) {
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(request.environmentId());

        if (effectiveEnvironmentId != request.environmentId()) {
            request = request.withEnvironmentId(effectiveEnvironmentId);
        }

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        EnvironmentContext.set(Math.toIntExact(request.environmentId()));

        try {
            return doGenerate(request);
        } finally {
            if (previousEnvironment == null) {
                EnvironmentContext.clear();
            } else {
                EnvironmentContext.set(previousEnvironment);
            }
        }
    }

    private PropertyCopilotResult doGenerate(PropertyCopilotRequest request) {
        ChatModel chatModel = resolveChatModel(request.environmentId());

        String availableOutputs = buildAvailableOutputs(request);
        String functionCatalog =
            request.mode() == PropertyCopilotMode.FORMULA ? buildFunctionCatalog() : "";

        String prompt = promptBuilder.build(request, availableOutputs, functionCatalog);

        if (request.mode() != PropertyCopilotMode.FORMULA) {
            return generateText(chatModel, request, prompt);
        }

        String value = clean(call(chatModel, prompt));

        if (value.isEmpty()) {
            return emptyValue(request);
        }

        value = toFormula(value);

        Map<String, ?> context = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            request.workflowId(), request.workflowNodeName(), request.environmentId());

        if (isValidFormula(value, context)) {
            record(request, "success");

            return PropertyCopilotResult.valid(value);
        }

        String repaired = clean(call(chatModel, prompt +
            "\n\nThe previous attempt was not a valid expression. Return a corrected single '=' expression."));

        if (repaired.isEmpty()) {
            return emptyValue(request);
        }

        repaired = toFormula(repaired);

        if (isValidFormula(repaired, context)) {
            record(request, "success");

            return PropertyCopilotResult.valid(repaired);
        }

        record(request, "invalid_formula");

        return PropertyCopilotResult.invalid(
            repaired, "The generated formula could not be validated; please review it.");
    }

    private PropertyCopilotResult generateText(ChatModel chatModel, PropertyCopilotRequest request, String prompt) {
        String value = clean(call(chatModel, prompt));

        if (value.isEmpty()) {
            return emptyValue(request);
        }

        Map<String, ?> context = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            request.workflowId(), request.workflowNodeName(), request.environmentId());

        if (!hasUnresolvedPills(value, context)) {
            record(request, "success");

            return PropertyCopilotResult.valid(value);
        }

        String repaired = clean(call(chatModel, prompt +
            "\n\nThe previous attempt referenced outputs that do not exist. Use ONLY ${nodeName.path} " +
            "references that appear in the available previous step outputs; otherwise return a constant value."));

        if (repaired.isEmpty()) {
            return emptyValue(request);
        }

        if (!hasUnresolvedPills(repaired, context)) {
            record(request, "success");

            return PropertyCopilotResult.valid(repaired);
        }

        record(request, "unresolved_pills");

        return PropertyCopilotResult.invalid(
            repaired, "The generated value references outputs that could not be resolved; please review it.");
    }

    private PropertyCopilotResult emptyValue(PropertyCopilotRequest request) {
        record(request, "empty");

        return PropertyCopilotResult.invalid("", EMPTY_VALUE_MESSAGE);
    }

    private String buildAvailableOutputs(PropertyCopilotRequest request) {
        List<WorkflowNodeOutputDTO> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            request.workflowId(), request.workflowNodeName(), request.environmentId());

        StringBuilder builder = new StringBuilder("\n");

        for (WorkflowNodeOutputDTO output : outputs) {
            builder.append(output.workflowNodeName())
                .append(": ")
                .append(output.getSampleOutput())
                .append("\n");
        }

        return builder.toString();
    }

    private String buildFunctionCatalog() {
        StringBuilder builder = new StringBuilder();

        for (EvaluatorFunctionDefinitionFactory factory : evaluatorFunctionDefinitionFactories) {
            for (EvaluatorFunctionDefinition definition : factory.getDefinitions()) {
                builder.append("- ")
                    .append(definition.name())
                    .append(": ")
                    .append(definition.description())
                    .append("\n");
            }
        }

        return builder.toString();
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

    private boolean hasUnresolvedPills(String value, Map<String, ?> context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        Matcher matcher = DATA_PILL_PATTERN.matcher(value);

        while (matcher.find()) {
            String pill = matcher.group();

            Object evaluated = evaluator.evaluate(Map.of("value", pill), context, true)
                .get("value");

            if (String.valueOf(evaluated)
                .contains("${")) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidFormula(String value, Map<String, ?> context) {
        try {
            evaluator.evaluate(Map.of("value", value), context, false);

            return true;
        } catch (IllegalArgumentException | ExpressionException exception) {
            log.debug("Generated formula '{}' is invalid: {}", value, exception.getMessage());

            return false;
        }
    }

    private static String toFormula(String value) {
        return value.startsWith("=") ? value : "=" + value;
    }

    private static String clean(@Nullable String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("```[a-zA-Z]*", "")
            .strip();
    }

    private void record(PropertyCopilotRequest request, String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_property_copilot_generate")
            .tag("mode", request.mode()
                .name())
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
