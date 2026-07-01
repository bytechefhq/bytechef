/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Shared helper for the property-options lookup tool callbacks (action and trigger variants). Centralises three pieces
 * of logic that would otherwise be duplicated between the two callbacks:
 *
 * <ul>
 * <li>SecurityContext rehydration on Reactor scheduler threads, mirroring the pattern used by
 * {@link ListConnectionsForComponentToolCallback} so the wrapped facade call sees the user's login + authorities
 * instead of throwing {@code IllegalStateException} for a missing current user.</li>
 * <li>Success envelope assembly with insertion-ordered keys so the LLM consistently sees
 * {@code componentName, actionName|triggerName, propertyName, options, truncated}.</li>
 * <li>Structured error envelopes for the {@code connection_required}, {@code dependency_missing}, and
 * {@code no_options_for_property} cases. The generic {@code lookup_failed} envelope stays with
 * {@code ToolErrors.runtimeFailure(...)} and is intentionally not handled here.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class PropertyOptionsResolver {

    private final SecurityContextRehydrator securityContextRehydrator;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PropertyOptionsResolver(SecurityContextRehydrator securityContextRehydrator) {
        this.securityContextRehydrator = securityContextRehydrator;
    }

    /**
     * Delegates security context rehydration to {@link SecurityContextRehydrator}. See that class for the full
     * behaviour contract.
     */
    public <T> T withUserSecurityContext(@Nullable Long userId, Supplier<T> action) {
        return securityContextRehydrator.withUserSecurityContext(userId, action);
    }

    /**
     * Builds the success envelope returned to the LLM, including a {@code truncated} flag when the option list was
     * capped. {@code entityKey} is {@code "actionName"} or {@code "triggerName"} depending on the caller. Each
     * {@link Option} is rendered as a {@code {label, value}} map, preserving the runtime type of {@code value} so
     * numeric / boolean / string options survive JSON serialization without coercion.
     */
    public Map<String, Object> buildSuccessEnvelope(
        String componentName, String entityKey, String entityName, String propertyName, List<Option> options,
        boolean truncated) {

        List<Map<String, Object>> optionRows = new ArrayList<>(options.size());

        for (Option option : options) {
            Map<String, Object> optionRow = new LinkedHashMap<>();

            optionRow.put("label", option.getLabel());
            optionRow.put("value", option.getValue());

            optionRows.add(optionRow);
        }

        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("componentName", componentName);
        envelope.put(entityKey, entityName);
        envelope.put("propertyName", propertyName);
        envelope.put("options", optionRows);
        envelope.put("truncated", truncated);

        return envelope;
    }

    /**
     * Builds the {@code connection_required} error envelope returned when the LLM invoked the lookup without a
     * {@code connectionId} for a property whose options provider needs an active connection.
     */
    public Map<String, Object> connectionRequiredEnvelope(String componentName) {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", "connection_required");
        envelope.put("componentName", componentName);
        envelope.put(
            "hint",
            "No connectionId supplied. Call listConnectionsForComponent for '" + componentName +
                "' to pick an existing one, or createConnection to make a new one, then retry.");

        return envelope;
    }

    /**
     * Builds the {@code dependency_missing} error envelope returned when the options provider requires sibling input
     * properties that the LLM did not include in {@code inputParameters}.
     */
    public Map<String, Object> dependencyMissingEnvelope(List<String> missing) {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", "dependency_missing");
        envelope.put("missing", missing);
        envelope.put("hint", "Place values for these siblings first and include them in inputParameters, then retry.");

        return envelope;
    }

    /**
     * Builds the {@code no_options_for_property} error envelope returned when the resolved property has no dynamic
     * options provider — typically a free-form string / number property whose value the LLM should set directly.
     */
    public Map<String, Object> noOptionsForPropertyEnvelope() {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", "no_options_for_property");
        envelope.put(
            "hint",
            "This property does not have dynamic options. Set the value directly per the property's description.");

        return envelope;
    }

    /**
     * Builds an {@code action_not_found} / {@code trigger_not_found} / {@code property_not_found} error envelope. The
     * {@code valid} list lets the LLM self-correct by retrying with a real name instead of treating a wrong-name guess
     * as "this entity has no options". {@code entityKey} is {@code "actionName"}, {@code "triggerName"}, or
     * {@code "propertyName"}.
     */
    public Map<String, Object> entityNotFoundEnvelope(
        String errorCode, String entityKey, String requested, List<String> valid) {

        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("error", errorCode);
        envelope.put(entityKey, requested);
        envelope.put("valid", valid);
        envelope.put(
            "hint",
            "No " + entityKey + " '" + requested + "' exists on this component. Retry with one of the names listed in" +
                " 'valid'.");

        return envelope;
    }

    /**
     * Outcome of a property-options resolution: either a structured {@link Failure} envelope (with the metric tag the
     * caller should record) or a {@link Success} carrying the capped options. Lets the lookup callbacks (which format
     * success as an LLM envelope) and the select callbacks (which format it as a client render marker) share the entire
     * gating + fetch sequence.
     */
    public sealed interface OptionsLookupResult {

        @SuppressFBWarnings({
            "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
        })
        record Failure(Map<String, Object> envelope, String metricTag) implements OptionsLookupResult {
        }

        @SuppressFBWarnings({
            "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
        })
        record Success(List<Option> options, boolean truncated) implements OptionsLookupResult {
        }
    }

    /**
     * Runs the full action-property options gating + fetch sequence. Returns a {@link OptionsLookupResult.Failure} with
     * the matching error envelope + metric tag at the first failing gate, or {@link OptionsLookupResult.Success} with
     * the options capped at {@code maxOptions}.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OptionsLookupResult resolveActionPropertyOptions(
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        @Nullable AgentToolInvocationContext invocationContext, String componentName, int componentVersion,
        String actionName, String propertyName, @Nullable Map<String, Object> inputParameters,
        @Nullable Long connectionId, @Nullable String searchText, int maxOptions) {

        List<String> validActionNames = actionDefinitionService.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(ActionDefinition::getName)
            .toList();

        if (!validActionNames.contains(actionName)) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("action_not_found", "actionName", actionName, validActionNames),
                "action_not_found");
        }

        ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
            componentName, componentVersion, actionName);

        List<String> validPropertyNames = actionDefinition.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        if (!validPropertyNames.contains(topPropertySegment(propertyName))) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("property_not_found", "propertyName", propertyName, validPropertyNames),
                "property_not_found");
        }

        if (!actionDefinitionService.propertyHasOptionsDataSource(
            componentName, componentVersion, actionName, propertyName)) {

            return new OptionsLookupResult.Failure(noOptionsForPropertyEnvelope(), "no_options");
        }

        List<String> lookupDependsOnPaths = actionDefinitionService.getPropertyLookupDependsOn(
            componentName, componentVersion, actionName, propertyName);

        Map<String, Object> parameters = inputParameters == null ? Map.of() : inputParameters;

        List<String> missing = lookupDependsOnPaths.stream()
            .filter(path -> !parameters.containsKey(path) &&
                !parameters.containsKey(path.substring(path.lastIndexOf('.') + 1)))
            .toList();

        if (!missing.isEmpty()) {
            return new OptionsLookupResult.Failure(dependencyMissingEnvelope(missing), "dependency_missing");
        }

        if (connectionId == null
            && actionDefinitionService.actionDefinesConnection(componentName, componentVersion, actionName)) {

            return new OptionsLookupResult.Failure(connectionRequiredEnvelope(componentName), "connection_required");
        }

        List<Option> options = withUserSecurityContext(
            invocationContext == null ? null : invocationContext.userId(),
            () -> actionDefinitionFacade.executeOptions(
                componentName, componentVersion, actionName, propertyName, parameters, lookupDependsOnPaths,
                searchText == null ? "" : searchText, connectionId));

        boolean truncated = options.size() > maxOptions;

        List<Option> capped = truncated ? List.copyOf(options.subList(0, maxOptions)) : options;

        return new OptionsLookupResult.Success(capped, truncated);
    }

    /**
     * Trigger twin of {@link #resolveActionPropertyOptions}; uses {@code trigger_not_found} and
     * {@code triggerDefinesConnection}.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OptionsLookupResult resolveTriggerPropertyOptions(
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        @Nullable AgentToolInvocationContext invocationContext, String componentName, int componentVersion,
        String triggerName, String propertyName, @Nullable Map<String, Object> inputParameters,
        @Nullable Long connectionId, @Nullable String searchText, int maxOptions) {

        List<String> validTriggerNames = triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion)
            .stream()
            .map(TriggerDefinition::getName)
            .toList();

        if (!validTriggerNames.contains(triggerName)) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("trigger_not_found", "triggerName", triggerName, validTriggerNames),
                "trigger_not_found");
        }

        TriggerDefinition triggerDefinition =
            triggerDefinitionService.getTriggerDefinition(componentName, componentVersion, triggerName);

        List<String> validPropertyNames = triggerDefinition.getProperties()
            .stream()
            .map(Property::getName)
            .toList();

        if (!validPropertyNames.contains(topPropertySegment(propertyName))) {
            return new OptionsLookupResult.Failure(
                entityNotFoundEnvelope("property_not_found", "propertyName", propertyName, validPropertyNames),
                "property_not_found");
        }

        if (!triggerDefinitionService.propertyHasOptionsDataSource(
            componentName, componentVersion, triggerName, propertyName)) {

            return new OptionsLookupResult.Failure(noOptionsForPropertyEnvelope(), "no_options");
        }

        List<String> lookupDependsOnPaths = triggerDefinitionService.getPropertyLookupDependsOn(
            componentName, componentVersion, triggerName, propertyName);

        Map<String, Object> parameters = inputParameters == null ? Map.of() : inputParameters;

        List<String> missing = lookupDependsOnPaths.stream()
            .filter(path -> !parameters.containsKey(path) &&
                !parameters.containsKey(path.substring(path.lastIndexOf('.') + 1)))
            .toList();

        if (!missing.isEmpty()) {
            return new OptionsLookupResult.Failure(dependencyMissingEnvelope(missing), "dependency_missing");
        }

        if (connectionId == null &&
            triggerDefinitionService.triggerDefinesConnection(componentName, componentVersion, triggerName)) {

            return new OptionsLookupResult.Failure(connectionRequiredEnvelope(componentName), "connection_required");
        }

        List<Option> options = withUserSecurityContext(
            invocationContext == null ? null : invocationContext.userId(),
            () -> triggerDefinitionFacade.executeOptions(
                componentName, componentVersion, triggerName, propertyName, parameters, lookupDependsOnPaths,
                searchText == null ? "" : searchText, connectionId));

        boolean truncated = options.size() > maxOptions;

        List<Option> capped = truncated ? List.copyOf(options.subList(0, maxOptions)) : options;

        return new OptionsLookupResult.Success(capped, truncated);
    }

    /**
     * Top-level container segment of a (possibly dotted / array) property path: {@code parent.child} → {@code parent},
     * {@code items[].id} → {@code items}, {@code channel} → {@code channel}. Existence is checked only at the top
     * level.
     */
    public static String topPropertySegment(String propertyName) {
        int dot = propertyName.indexOf('.');
        int bracket = propertyName.indexOf('[');

        int cut;

        if (dot >= 0 && bracket >= 0) {
            cut = Math.min(dot, bracket);
        } else if (dot >= 0) {
            cut = dot;
        } else {
            cut = bracket;
        }

        return cut >= 0 ? propertyName.substring(0, cut) : propertyName;
    }
}
