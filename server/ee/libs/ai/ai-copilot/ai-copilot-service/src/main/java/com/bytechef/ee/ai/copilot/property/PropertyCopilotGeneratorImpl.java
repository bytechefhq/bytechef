/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.property;

import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
public class PropertyCopilotGeneratorImpl implements PropertyCopilotGenerator {

    private static final Pattern DATA_PILL_PATTERN = Pattern.compile("\\$\\{[^}]+}");

    private final ChatModel chatModel;
    private final Evaluator evaluator;
    private final PropertyCopilotPromptBuilder promptBuilder;
    private final List<EvaluatorFunctionDefinitionFactory> evaluatorFunctionDefinitionFactories;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final String defaultProvider;
    private final ObjectProvider<CatalogChatClientResolver> catalogChatClientResolverProvider;

    public PropertyCopilotGeneratorImpl(
        ChatModel chatModel, Evaluator evaluator, PropertyCopilotPromptBuilder promptBuilder,
        List<EvaluatorFunctionDefinitionFactory> evaluatorFunctionDefinitionFactories,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, ObjectProvider<MeterRegistry> meterRegistryProvider,
        @Value("${bytechef.ai.copilot.provider:}") String defaultProvider,
        ObjectProvider<CatalogChatClientResolver> catalogChatClientResolverProvider) {

        this.chatModel = chatModel;
        this.evaluator = evaluator;
        this.promptBuilder = promptBuilder;
        this.evaluatorFunctionDefinitionFactories = evaluatorFunctionDefinitionFactories;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.meterRegistryProvider = meterRegistryProvider;
        this.defaultProvider = defaultProvider;
        this.catalogChatClientResolverProvider = catalogChatClientResolverProvider;
    }

    @Override
    public PropertyCopilotResult generate(PropertyCopilotRequest request) {
        // PropertyCopilotFacadeImpl's guard is hasWorkspaceScopeForProject(projectId, 'WORKFLOW_VIEW') --
        // environment-agnostic (the two-argument overload, not the one that takes an Environment) -- so the
        // client-supplied request.environmentId() is never checked. Resolved here, on the request thread, and
        // substituted into the request used for the rest of this call (EnvironmentContext, the @Cacheable
        // getPreviousWorkflowNode{Outputs,SampleOutputs} reads below) so a confined principal cannot read another
        // environment's previous-step outputs by naming it. See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(request.environmentId());

        if (effectiveEnvironmentId != request.environmentId()) {
            request = new PropertyCopilotRequest(
                request.prompt(), request.mode(), request.workflowId(), request.workflowNodeName(),
                request.propertyPath(), request.propertyType(), request.dynamic(), effectiveEnvironmentId);
        }

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        EnvironmentContext.set((int) request.environmentId());

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
        String availableOutputs = buildAvailableOutputs(request);
        String functionCatalog =
            request.mode() == PropertyCopilotMode.FORMULA ? buildFunctionCatalog() : "";

        String prompt = promptBuilder.build(request, availableOutputs, functionCatalog);

        if (request.mode() != PropertyCopilotMode.FORMULA) {
            return generateText(request, prompt);
        }

        String value = clean(call(prompt));

        if (!value.startsWith("=")) {
            value = "=" + value;
        }

        Map<String, ?> context = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            request.workflowId(), request.workflowNodeName(), request.environmentId());

        if (isValidFormula(value, context)) {
            record(request, "success");

            return new PropertyCopilotResult(value, true, null);
        }

        String repaired = clean(call(prompt +
            "\n\nThe previous attempt was not a valid expression. Return a corrected single '=' expression."));

        if (!repaired.startsWith("=")) {
            repaired = "=" + repaired;
        }

        if (isValidFormula(repaired, context)) {
            record(request, "success");

            return new PropertyCopilotResult(repaired, true, null);
        }

        record(request, "invalid_formula");

        return new PropertyCopilotResult(
            repaired, false, "The generated formula could not be validated; please review it.");
    }

    private PropertyCopilotResult generateText(PropertyCopilotRequest request, String prompt) {
        String value = clean(call(prompt));

        Map<String, ?> context = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            request.workflowId(), request.workflowNodeName(), request.environmentId());

        if (!hasUnresolvedPills(value, context)) {
            record(request, "success");

            return new PropertyCopilotResult(value, true, null);
        }

        String repaired = clean(call(prompt +
            "\n\nThe previous attempt referenced outputs that do not exist. Use ONLY ${nodeName.path} " +
            "references that appear in the available previous step outputs; otherwise return a constant value."));

        if (!hasUnresolvedPills(repaired, context)) {
            record(request, "success");

            return new PropertyCopilotResult(repaired, true, null);
        }

        record(request, "unresolved_pills");

        return new PropertyCopilotResult(
            repaired, false, "The generated value references outputs that could not be resolved; please review it.");
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

    private String call(String promptText) {
        ChatClient chatClient = resolveCatalogChatClient();

        if (chatClient != null) {
            return chatClient.prompt(promptText)
                .call()
                .content();
        }

        ChatResponse chatResponse = chatModel.call(new Prompt(promptText));

        Generation generation = Objects.requireNonNull(chatResponse.getResult(), "generation is required");

        return generation.getOutput()
            .getText();
    }

    private ChatClient resolveCatalogChatClient() {
        CatalogChatClientResolver catalogChatClientResolver = catalogChatClientResolverProvider.getIfAvailable();

        if (catalogChatClientResolver == null) {
            return null;
        }

        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        if (environment == null) {
            return null;
        }

        try {
            if (defaultProvider != null && !defaultProvider.isBlank()) {
                ChatClient preferred = catalogChatClientResolver.resolvePreferred(
                    defaultProvider, environment.ordinal());

                if (preferred != null) {
                    return preferred;
                }
            }

            return catalogChatClientResolver.resolveDefault(environment.ordinal());
        } catch (RuntimeException exception) {
            return null;
        }
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

            // An unresolvable reference is returned unchanged by the evaluator, so the ${...} token survives.
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
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String clean(String text) {
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
