/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MemoizationUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ArrayProperty;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.component.definition.PropertyGroup;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.PropertiesFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Component.Registry;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.exception.ComponentErrorType;
import com.bytechef.platform.component.handler.DynamicComponentHandlerRegistry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ProviderEntry;
import com.bytechef.platform.component.handler.loader.DefaultComponentHandlerLoader;
import com.bytechef.platform.component.index.ComponentIndex;
import com.bytechef.platform.component.jdbc.handler.loader.JdbcComponentHandlerLoader;
import com.bytechef.platform.component.oas.handler.loader.OpenApiComponentHandlerLoader;
import com.bytechef.platform.util.PropertyUtils;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of all component definitions, loaded <b>lazily</b>: constructing the registry is cheap (it only captures its
 * collaborators), and the expensive work — asking every {@code ComponentHandler} for its definition, validating every
 * action/trigger property tree, and indexing the definitions by name and version — is deferred to the first method call
 * that actually needs a definition. Application startup therefore never pays for building the component catalog; the
 * first caller (typically the first components-list or workflow request) pays it exactly once, and every call
 * afterwards hits the memoized index.
 *
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@SuppressFBWarnings({
    "CT", "EI"
})
public class ComponentDefinitionRegistry {

    private static final Logger log = LoggerFactory.getLogger(ComponentDefinitionRegistry.class);

    private static final ComponentDefinition MANUAL_COMPONENT_DEFINITION = component("manual")
        .title("Manual")
        .icon("path:assets/manual.svg")
        .triggers(trigger("manual").type(TriggerType.STATIC_WEBHOOK));

    private static final ComponentDefinition MISSING_COMPONENT_DEFINITION = component("missing")
        .title("Missing Component")
        .icon("path:assets/missing.svg")
        .version(1)
        .actions(ComponentDsl.action("missing")
            .title("Missing Action"));

    private static final List<ComponentHandlerLoader> COMPONENT_HANDLER_LOADERS = List.of(
        new DefaultComponentHandlerLoader(), new JdbcComponentHandlerLoader(), new OpenApiComponentHandlerLoader());

    private final ApplicationProperties applicationProperties;
    private final Supplier<List<ComponentDefinition>> allComponentDefinitionsSupplier;
    private final Supplier<Map<String, Map<Integer, ComponentDefinition>>> componentDefinitionsMapSupplier;
    private final List<ComponentHandler> componentHandlers;
    private final Supplier<Optional<ComponentIndex>> componentIndexSupplier;
    private final List<DynamicComponentHandlerRegistry> dynamicComponentHandlerRegistries;
    private final Map<String, List<ComponentDefinition>> loadedComponentDefinitionsByName = new ConcurrentHashMap<>();
    private final Supplier<List<ComponentDefinition>> stubComponentDefinitionsSupplier;

    public ComponentDefinitionRegistry(
        ApplicationProperties applicationProperties, List<ComponentHandler> componentHandlers,
        Supplier<List<ComponentHandlerEntry>> componentHandlerEntriesSupplier,
        List<DynamicComponentHandlerRegistry> dynamicComponentHandlerRegistries) {

        this(
            applicationProperties, componentHandlers, componentHandlerEntriesSupplier,
            dynamicComponentHandlerRegistries,
            MemoizationUtils.memoize(() -> ComponentIndex.load(ComponentDefinitionRegistry.class.getClassLoader())));
    }

    ComponentDefinitionRegistry(
        ApplicationProperties applicationProperties, List<ComponentHandler> componentHandlers,
        Supplier<List<ComponentHandlerEntry>> componentHandlerEntriesSupplier,
        List<DynamicComponentHandlerRegistry> dynamicComponentHandlerRegistries,
        Supplier<Optional<ComponentIndex>> componentIndexSupplier) {

        this.applicationProperties = applicationProperties;
        this.componentHandlers = componentHandlers;
        this.allComponentDefinitionsSupplier = MemoizationUtils.memoize(this::loadAllComponentDefinitions);
        this.componentDefinitionsMapSupplier = MemoizationUtils.memoize(
            () -> loadComponentDefinitionsMap(
                applicationProperties, componentHandlers, componentHandlerEntriesSupplier));
        this.componentIndexSupplier = componentIndexSupplier;
        this.dynamicComponentHandlerRegistries = dynamicComponentHandlerRegistries;
        this.stubComponentDefinitionsSupplier = MemoizationUtils.memoize(this::loadStubComponentDefinitions);
    }

    /**
     * Builds the name/version index of all component definitions. Invoked at most once, on the first registry access
     * (guarded by the memoizing supplier), NOT at construction/startup time.
     */
    private static Map<String, Map<Integer, ComponentDefinition>> loadComponentDefinitionsMap(
        ApplicationProperties applicationProperties, List<ComponentHandler> componentHandlers,
        Supplier<List<ComponentHandlerEntry>> componentHandlerEntriesSupplier) {

        long startTime = System.currentTimeMillis();

        List<ComponentHandler> mergedComponentHandlers = CollectionUtils.concat(
            componentHandlers,
            CollectionUtils.map(componentHandlerEntriesSupplier.get(), ComponentHandlerEntry::componentHandler));

        List<ComponentDefinition> componentDefinitions = CollectionUtils.concat(
            CollectionUtils.map(mergedComponentHandlers, ComponentHandler::getDefinition),
            MANUAL_COMPONENT_DEFINITION, MISSING_COMPONENT_DEFINITION);

        ApplicationProperties.Component component = applicationProperties.getComponent();

        Registry registry = component.getRegistry();

        List<String> exclude = registry.getExclude();

        if (!CollectionUtils.isEmpty(exclude)) {
            componentDefinitions = componentDefinitions.stream()
                .filter(componentDefinition -> !CollectionUtils.contains(exclude, componentDefinition.getName()))
                .toList();
        }

        // Validate

        validate(componentDefinitions);

        Map<String, Map<Integer, ComponentDefinition>> componentDefinitionsMap = new HashMap<>();

        for (ComponentDefinition componentDefinition : componentDefinitions) {
            componentDefinitionsMap
                .computeIfAbsent(StringUtils.upperCase(componentDefinition.getName()), key -> new HashMap<>())
                .put(componentDefinition.getVersion(), componentDefinition);
        }

        if (log.isInfoEnabled()) {
            log.info(
                "Loaded, validated and indexed {} component definitions in {} ms", componentDefinitions.size(),
                System.currentTimeMillis() - startTime);
        }

        return componentDefinitionsMap;
    }

    private List<String> getExcludedComponentNames() {
        ApplicationProperties.Component component = applicationProperties.getComponent();

        Registry registry = component.getRegistry();

        List<String> exclude = registry.getExclude();

        return exclude == null ? List.of() : exclude;
    }

    private boolean isExcluded(String componentName) {
        return CollectionUtils.contains(getExcludedComponentNames(), componentName);
    }

    /**
     * Resolves all definitions (all versions) of a single component by name, loading ONLY that component: constants
     * (manual/missing), Spring-declared handler beans, and — via the build-time index — just the matching
     * {@code ServiceLoader} providers, leaving every other component untouched. Loaded definitions are validated once
     * and cached. Callers must only use this when the component index is present.
     */
    private List<ComponentDefinition> getComponentDefinitionsFromIndex(String name, ComponentIndex componentIndex) {
        return getComponentDefinitionsFromIndex(
            name, componentIndex,
            entry -> getComponentHandlerLoader(entry.loaderKind()).loadComponentHandler(entry.providerClassName()));
    }

    private List<ComponentDefinition> getComponentDefinitionsFromIndex(
        String name, ComponentIndex componentIndex,
        Function<ComponentIndex.Entry, Optional<ComponentHandlerEntry>> componentHandlerEntryResolver) {

        if (isExcluded(name)) {
            return List.of();
        }

        if (name.equalsIgnoreCase(MANUAL_COMPONENT_DEFINITION.getName())) {
            return List.of(MANUAL_COMPONENT_DEFINITION);
        }

        if (name.equalsIgnoreCase(MISSING_COMPONENT_DEFINITION.getName())) {
            return List.of(MISSING_COMPONENT_DEFINITION);
        }

        return loadedComponentDefinitionsByName.computeIfAbsent(
            StringUtils.upperCase(name),
            key -> loadComponentDefinitionsByName(name, componentIndex, componentHandlerEntryResolver));
    }

    private List<ComponentDefinition> loadComponentDefinitionsByName(
        String name, ComponentIndex componentIndex,
        Function<ComponentIndex.Entry, Optional<ComponentHandlerEntry>> componentHandlerEntryResolver) {

        List<ComponentDefinition> componentDefinitions = new ArrayList<>();

        for (ComponentHandler componentHandler : componentHandlers) {
            ComponentDefinition componentDefinition = componentHandler.getDefinition();

            if (name.equalsIgnoreCase(componentDefinition.getName())) {
                validate(List.of(componentDefinition));

                componentDefinitions.add(componentDefinition);
            }
        }

        for (ComponentIndex.Entry entry : componentIndex.entries()) {
            if (!name.equalsIgnoreCase(entry.name())) {
                continue;
            }

            ComponentDefinition componentDefinition = componentHandlerEntryResolver.apply(entry)
                .map(componentHandlerEntry -> componentHandlerEntry.componentHandler()
                    .getDefinition())
                .orElse(null);

            if (componentDefinition == null) {
                log.warn(
                    "Component index entry '{}' v{} points to provider '{}' which is not present on the classpath",
                    entry.name(), entry.version(), entry.providerClassName());

                continue;
            }

            validate(List.of(componentDefinition));

            componentDefinitions.add(componentDefinition);
        }

        componentDefinitions.sort(Comparator.comparingInt(ComponentDefinition::getVersion));

        if (log.isDebugEnabled() && !componentDefinitions.isEmpty()) {
            log.debug("Loaded component '{}' on demand ({} version(s))", name, componentDefinitions.size());
        }

        return componentDefinitions;
    }

    private static ComponentHandlerLoader getComponentHandlerLoader(String loaderKind) {
        return COMPONENT_HANDLER_LOADERS.stream()
            .filter(componentHandlerLoader -> Objects.equals(componentHandlerLoader.getKind(), loaderKind))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown component loader kind: " + loaderKind));
    }

    /**
     * Full definitions of every component, resolved through the index with per-component caching. Only used when the
     * index is present; consumers that deep-read definitions (connections list, cluster elements, unified API) get
     * complete, executable definitions — never stubs.
     */
    private List<ComponentDefinition> loadAllComponentDefinitions() {
        ComponentIndex componentIndex = getRequiredComponentIndex();

        // Resolving the full catalog through per-component loadComponentHandler calls would re-scan every
        // ServiceLoader provider-config file once per component (~O(N^2) scans), so prefetch all handlers with a
        // single sweep per loader kind and resolve index entries from that map instead.
        Set<String> loaderKinds = new LinkedHashSet<>();

        for (ComponentIndex.Entry entry : componentIndex.entries()) {
            loaderKinds.add(entry.loaderKind());
        }

        Map<String, ComponentHandlerEntry> componentHandlerEntriesByProviderClassName = new HashMap<>();

        for (ComponentHandlerLoader componentHandlerLoader : COMPONENT_HANDLER_LOADERS) {
            if (!loaderKinds.contains(componentHandlerLoader.getKind())) {
                continue;
            }

            for (ProviderEntry providerEntry : componentHandlerLoader.loadProviderEntries()) {
                componentHandlerEntriesByProviderClassName.put(
                    providerEntry.providerClassName(), providerEntry.componentHandlerEntry());
            }
        }

        Function<ComponentIndex.Entry, Optional<ComponentHandlerEntry>> componentHandlerEntryResolver =
            entry -> Optional.ofNullable(componentHandlerEntriesByProviderClassName.get(entry.providerClassName()));

        List<ComponentDefinition> componentDefinitions = new ArrayList<>();

        Set<String> names = new LinkedHashSet<>();

        for (ComponentHandler componentHandler : componentHandlers) {
            ComponentDefinition componentDefinition = componentHandler.getDefinition();

            names.add(componentDefinition.getName());
        }

        for (ComponentIndex.Entry entry : componentIndex.entries()) {
            names.add(entry.name());
        }

        for (String name : names) {
            componentDefinitions.addAll(
                getComponentDefinitionsFromIndex(name, componentIndex, componentHandlerEntryResolver));
        }

        if (!isExcluded(MANUAL_COMPONENT_DEFINITION.getName())) {
            componentDefinitions.add(MANUAL_COMPONENT_DEFINITION);
        }

        if (!isExcluded(MISSING_COMPONENT_DEFINITION.getName())) {
            componentDefinitions.add(MISSING_COMPONENT_DEFINITION);
        }

        return componentDefinitions;
    }

    /**
     * List-view definitions: lightweight stubs built from the build-time index (plus the real definitions of
     * Spring-declared handlers, which exist as beans anyway, and the synthetic manual/missing components). Served ONLY
     * by {@link #getStaticComponentDefinitions()} for the components-list view; no properties, no executable functions.
     */
    private List<ComponentDefinition> loadStubComponentDefinitions() {
        ComponentIndex componentIndex = getRequiredComponentIndex();

        List<ComponentDefinition> componentDefinitions = new ArrayList<>();

        for (ComponentHandler componentHandler : componentHandlers) {
            componentDefinitions.add(componentHandler.getDefinition());
        }

        for (ComponentIndex.Entry entry : componentIndex.entries()) {
            try {
                componentDefinitions.add(ComponentIndex.toStubComponentDefinition(entry));
            } catch (RuntimeException exception) {
                // A stale or hand-edited index (e.g. an enum name that no longer exists) must not break the
                // components list — degrade to serving full definitions, the same shape the no-index fallback uses.
                log.warn(
                    "Failed to build the list-view stub for component '{}' v{} from the index; falling back to full "
                        + "component loading for the components list",
                    entry.name(), entry.version(), exception);

                return CollectionUtils.sort(allComponentDefinitionsSupplier.get(), this::compare);
            }
        }

        componentDefinitions.add(MANUAL_COMPONENT_DEFINITION);
        componentDefinitions.add(MISSING_COMPONENT_DEFINITION);

        List<String> excludedComponentNames = getExcludedComponentNames();

        if (!excludedComponentNames.isEmpty()) {
            componentDefinitions = componentDefinitions.stream()
                .filter(componentDefinition -> !CollectionUtils.contains(
                    excludedComponentNames, componentDefinition.getName()))
                .toList();
        }

        return CollectionUtils.sort(componentDefinitions, this::compare);
    }

    private ComponentIndex getRequiredComponentIndex() {
        return componentIndexSupplier.get()
            .orElseThrow(() -> new IllegalStateException("Component index is not present"));
    }

    private Optional<ComponentIndex> fetchComponentIndex() {
        return componentIndexSupplier.get();
    }

    public Optional<Authorization> fetchAuthorization(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        return fetchConnectionDefinition(componentName, connectionVersion)
            .map(ConnectionDefinition::getAuthorizations)
            .orElse(List.of())
            .stream()
            .filter(authorization -> authorization.getType() == authorizationType)
            .map(authorization -> (Authorization) authorization)
            .findFirst();
    }

    public Optional<ComponentDefinition> fetchComponentDefinition(String name, @Nullable Integer version) {
        ComponentDefinition componentDefinition = null;

        if (version == null || version == -1) {
            List<ComponentDefinition> filteredComponentDefinitions = getComponentDefinitions(name);

            if (!filteredComponentDefinitions.isEmpty()) {
                componentDefinition = filteredComponentDefinitions.getLast();
            }
        } else {
            Optional<ComponentIndex> componentIndexOptional = fetchComponentIndex();

            if (componentIndexOptional.isPresent()) {
                componentDefinition = getComponentDefinitionsFromIndex(name, componentIndexOptional.get())
                    .stream()
                    .filter(curComponentDefinition -> version.equals(curComponentDefinition.getVersion()))
                    .findFirst()
                    .orElse(null);
            } else {
                Map<Integer, ComponentDefinition> componentDefinitionMap = componentDefinitionsMapSupplier.get()
                    .get(StringUtils.upperCase(name));

                if (componentDefinitionMap != null) {
                    componentDefinition = componentDefinitionMap.get(version);
                }
            }

            if (componentDefinition == null) {
                componentDefinition = dynamicComponentHandlerRegistries.stream()
                    .map(dynamicComponentHandlerRegistry -> dynamicComponentHandlerRegistry.fetchComponentHandler(
                        name, version))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .map(ComponentHandler::getDefinition)
                    .orElse(null);
            }
        }

        return Optional.ofNullable(componentDefinition);
    }

    public Optional<ConnectionDefinition> fetchConnectionDefinition(String componentName, int connectionVersion) {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions(componentName);

        return componentDefinitions.stream()
            .filter(componentDefinition -> componentDefinition.getConnection()
                .map(connectionDefinition -> connectionDefinition.getVersion() == connectionVersion)
                .orElse(false))
            .findFirst()
            .flatMap(ComponentDefinition::getConnection);
    }

    public List<ComponentDefinition> getComponentDefinitions() {
        List<ComponentDefinition> staticComponentDefinitions;

        if (fetchComponentIndex().isPresent()) {
            staticComponentDefinitions = allComponentDefinitionsSupplier.get();
        } else {
            staticComponentDefinitions = componentDefinitionsMapSupplier.get()
                .values()
                .stream()
                .flatMap(map -> CollectionUtils.stream(map.values()))
                .toList();
        }

        return CollectionUtils.sort(
            CollectionUtils.concat(
                staticComponentDefinitions,
                dynamicComponentHandlerRegistries.stream()
                    .flatMap(dynamicComponentHandlerRegistry -> CollectionUtils.stream(
                        dynamicComponentHandlerRegistry.getComponentHandlers()))
                    .map(ComponentHandler::getDefinition)
                    .toList()),
            this::compare);
    }

    public ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getActions()
            .stream()
            .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' action.".formatted(componentName, actionName)));
    }

    public List<? extends ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getActions();
    }

    public Property getActionProperty(
        String componentName, int componentVersion, String actionName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) throws Exception {

        ActionDefinition actionDefinition = getActionDefinition(componentName, componentVersion, actionName);

        List<? extends Property> properties = actionDefinition.getProperties();

        return getProperty(
            propertyName, properties, inputParameters, connectionParameters, lookupDependsOnPaths, context);
    }

    public Authorization getAuthorization(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        ConnectionDefinition connectionDefinition = getConnectionDefinition(componentName, connectionVersion);

        return connectionDefinition.getAuthorizations()
            .stream()
            .filter(authorization -> {
                AuthorizationType curAuthorizationType = authorization.getType();

                return curAuthorizationType == authorizationType;
            })
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }

    public ClusterElementDefinition<?> getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getClusterElements()
            .stream()
            .filter(clusterElementDefinition -> clusterElementName.equalsIgnoreCase(clusterElementDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' cluster element.".formatted(
                        componentName, clusterElementName)));
    }

    public ClusterElementDefinition<?> getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName,
        ClusterElementDefinition.ClusterElementType clusterElementType) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getClusterElements()
            .stream()
            .filter(clusterElementDefinition -> clusterElementName.equalsIgnoreCase(clusterElementDefinition.getName()))
            .filter(clusterElementDefinition -> clusterElementType.equals(clusterElementDefinition.getType()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' cluster element with type '%s'.".formatted(
                        componentName, clusterElementName, clusterElementType.name())));
    }

    public Property getClusterElementProperty(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ClusterElementContext context) throws Exception {

        ClusterElementDefinition<?> clusterElementDefinition = getClusterElementDefinition(
            componentName, componentVersion, clusterElementName);

        List<? extends Property> properties = clusterElementDefinition.getProperties();

        return getProperty(
            propertyName, properties, inputParameters, connectionParameters, lookupDependsOnPaths, context);
    }

    public ComponentDefinition getComponentDefinition(String name, @Nullable Integer version) {
        return fetchComponentDefinition(name, version)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Component definition with name '%s' and version '%s' not found", name, version)));
    }

    public List<ComponentDefinition> getComponentDefinitions(String name) {
        List<ComponentDefinition> filteredComponentDefinitions = List.of();

        Optional<ComponentIndex> componentIndexOptional = fetchComponentIndex();

        if (componentIndexOptional.isPresent()) {
            filteredComponentDefinitions = getComponentDefinitionsFromIndex(name, componentIndexOptional.get());
        } else {
            Map<Integer, ComponentDefinition> integerComponentDefinitionMap = componentDefinitionsMapSupplier.get()
                .get(StringUtils.upperCase(name));

            if (integerComponentDefinitionMap != null) {
                filteredComponentDefinitions = integerComponentDefinitionMap.values()
                    .stream()
                    .toList();
            }
        }

        if (filteredComponentDefinitions.isEmpty()) {
            filteredComponentDefinitions = dynamicComponentHandlerRegistries.stream()
                .flatMap(dynamicComponentHandlerRegistry -> CollectionUtils.stream(
                    dynamicComponentHandlerRegistry.getComponentHandlers()))
                .map(ComponentHandler::getDefinition)
                .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
                .toList();
        }

        return filteredComponentDefinitions;
    }

    public Property getComponentInputProperty(
        String componentName, int componentVersion, String groupName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        Context context) throws Exception {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        List<? extends PropertyGroup> inputs = componentDefinition.getInputs();

        PropertyGroup propertyGroup = CollectionUtils.getFirst(
            inputs, group -> Objects.equals(group.getName(), groupName),
            "Input group '%s' not found in component '%s'".formatted(groupName, componentName));

        return getProperty(
            propertyName, propertyGroup.getProperties(), inputParameters, connectionParameters, lookupDependsOnPaths,
            context);
    }

    public ConnectionDefinition getConnectionDefinition(String componentName, int connectionVersion) {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions(componentName);

        return CollectionUtils.getFirstFilter(
            componentDefinitions,
            componentDefinition -> componentDefinition.getConnection()
                .map(connectionDefinition -> connectionDefinition.getVersion() == connectionVersion)
                .orElse(false),
            componentDefinition -> componentDefinition.getConnection()
                .orElseThrow());
    }

    public List<ComponentDefinition> getDynamicComponentDefinitions() {
        return dynamicComponentHandlerRegistries.stream()
            .flatMap(dynamicComponentHandlerRegistry -> CollectionUtils.stream(
                dynamicComponentHandlerRegistry.getComponentHandlers()))
            .map(ComponentHandler::getDefinition)
            .toList();
    }

    /**
     * Definitions backing the components-list view. When the build-time component index is present, this returns
     * lightweight stubs carrying only the list-view metadata (identity, texts, icon, categories, counts) — no property
     * trees and no executable functions — without loading a single component handler. Detail and execution paths never
     * see these stubs: they resolve full definitions per component on demand.
     */
    public List<ComponentDefinition> getStaticComponentDefinitions() {
        if (fetchComponentIndex().isPresent()) {
            return stubComponentDefinitionsSupplier.get();
        }

        return CollectionUtils.sort(
            componentDefinitionsMapSupplier.get()
                .values()
                .stream()
                .flatMap(map -> CollectionUtils.stream(map.values()))
                .toList(),
            this::compare);
    }

    public TriggerDefinition getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getTriggers()
            .stream()
            .filter(triggerDefinition -> triggerName.equalsIgnoreCase(triggerDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' trigger.".formatted(
                        componentName, triggerName)));
    }

    public List<? extends TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getTriggers();
    }

    public Property getTriggerProperty(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        TriggerContext context) throws Exception {

        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, triggerName);

        List<? extends Property> properties = triggerDefinition.getProperties();

        return getProperty(
            propertyName, properties, inputParameters, connectionParameters, lookupDependsOnPaths, context);
    }

    public boolean hasComponentDefinition(String name, @Nullable Integer version) {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions(name);

        return componentDefinitions.stream()
            .anyMatch(componentDefinition -> version == null || version == componentDefinition.getVersion());
    }

    private int compare(ComponentDefinition o1, ComponentDefinition o2) {
        String o1Name = o1.getName();

        return o1Name.compareTo(o2.getName());
    }

    private static Property getProperty(
        String propertyName, List<? extends Property> properties, Parameters inputParameters,
        Parameters connectionParameters, Map<String, String> lookupDependsOnPaths, Context context) throws Exception {

        List<String> subProperties = Arrays.asList(propertyName.split("\\."));

        if (subProperties.size() == 1) {
            if (propertyName.endsWith("]")) {
                ArrayProperty arrayProperty = (ArrayProperty) CollectionUtils.getFirst(
                    properties,
                    curProperty -> Objects.equals(
                        curProperty.getName(), propertyName.substring(0, propertyName.length() - 3)));

                List<? extends Property> items = arrayProperty.getItems();

                return items.getFirst();
            } else {
                return CollectionUtils.getFirst(
                    properties, property -> Objects.equals(propertyName, property.getName()),
                    "Property '%s' not found in component properties".formatted(propertyName));
            }
        } else {
            String firstSubPropertyName = getFirstSubPropertyName(subProperties);

            Property firstProperty = CollectionUtils.getFirst(
                properties, property -> Objects.equals(property.getName(), firstSubPropertyName));

            if (firstProperty instanceof Property.DynamicPropertiesProperty dynamicPropertiesProperty) {
                PropertiesDataSource<?> dynamicPropertiesDataSource = dynamicPropertiesProperty
                    .getDynamicPropertiesDataSource();

                PropertiesDataSource.BasePropertiesFunction propertiesFunction = dynamicPropertiesDataSource
                    .getProperties();

                List<? extends Property.ValueProperty<?>> dynamicPropertyProperties;

                if (propertiesFunction instanceof ActionDefinition.PropertiesFunction actionPropertiesFunction) {
                    dynamicPropertyProperties = actionPropertiesFunction.apply(
                        inputParameters, connectionParameters, lookupDependsOnPaths, (ActionContext) context);
                } else if (propertiesFunction instanceof ClusterElementDefinition.PropertiesFunction clusterElementPropertiesFunction) {

                    dynamicPropertyProperties = clusterElementPropertiesFunction.apply(
                        inputParameters, connectionParameters, lookupDependsOnPaths,
                        (ClusterElementContext) context);
                } else {
                    dynamicPropertyProperties = ((PropertiesFunction) propertiesFunction).apply(
                        inputParameters, connectionParameters, lookupDependsOnPaths, (TriggerContext) context);
                }

                return getProperty(
                    String.join(".", subProperties.subList(1, subProperties.size())),
                    dynamicPropertyProperties, inputParameters, connectionParameters, lookupDependsOnPaths, context);
            } else if (firstProperty instanceof ArrayProperty arrayProperty) {
                List<? extends Property.ValueProperty<?>> items = arrayProperty.getItems();

                if (items.getFirst() instanceof ObjectProperty objectProperty) {
                    items = objectProperty.getProperties();
                }

                return getProperty(
                    String.join(".", subProperties.subList(1, subProperties.size())), items, inputParameters,
                    connectionParameters, lookupDependsOnPaths, context);
            } else {
                ObjectProperty objectProperty = (ObjectProperty) CollectionUtils.getFirst(
                    properties, property -> Objects.equals(property.getName(), subProperties.getFirst()));

                for (int i = 1; i < subProperties.size() - 1; i++) {
                    String subProperty = subProperties.get(i);

                    if (subProperty.endsWith("]")) {
                        List<? extends Property.ValueProperty<?>> objectPropertyProperties = objectProperty
                            .getProperties();

                        ArrayProperty arrayProperty = (ArrayProperty) CollectionUtils.getFirst(
                            objectPropertyProperties,
                            curProperty -> Objects.equals(
                                curProperty.getName(), subProperty.substring(0, subProperty.length() - 3)));

                        List<? extends Property> items = arrayProperty.getItems();

                        objectProperty = (ObjectProperty) items.getFirst();
                    } else {
                        List<? extends Property.ValueProperty<?>> objectPropertyProperties = objectProperty
                            .getProperties();

                        objectProperty = (ObjectProperty) CollectionUtils.getFirst(
                            objectPropertyProperties,
                            curProperty -> Objects.equals(curProperty.getName(), subProperty));
                    }
                }

                List<? extends Property.ValueProperty<?>> objectPropertyProperties = objectProperty.getProperties();

                return CollectionUtils.getFirst(
                    objectPropertyProperties,
                    curProperty -> Objects.equals(curProperty.getName(), subProperties.getLast()));
            }
        }
    }

    private static String getFirstSubPropertyName(List<String> subProperties) {
        String firstSubPropertyName = subProperties.getFirst();

        if (firstSubPropertyName.endsWith("]")) {
            firstSubPropertyName = firstSubPropertyName.substring(0, firstSubPropertyName.indexOf("["));
        }
        return firstSubPropertyName;
    }

    private static void validate(List<ComponentDefinition> componentDefinitions) {
        for (ComponentDefinition componentDefinition : componentDefinitions) {
            List<? extends ActionDefinition> actionDefinitions = componentDefinition.getActions();

            for (ActionDefinition actionDefinition : actionDefinitions) {
                if (log.isTraceEnabled()) {
                    log.trace("Validating %s.%s".formatted(componentDefinition.getName(), actionDefinition.getName()));
                }

                List<? extends Property> properties = actionDefinition.getProperties();

                PropertyUtils.checkInputProperties(properties);
                PropertyUtils.checkOutputProperty(
                    actionDefinition.getOutputDefinition()
                        .map(OutputDefinition::getOutputSchema)
                        .orElse(null));
            }

            List<? extends TriggerDefinition> triggerDefinitions = componentDefinition.getTriggers();

            for (TriggerDefinition triggerDefinition : triggerDefinitions) {
                if (log.isTraceEnabled()) {
                    log.trace("Validating %s.%s".formatted(componentDefinition.getName(), triggerDefinition.getName()));
                }

                List<? extends Property> properties = triggerDefinition.getProperties();

                PropertyUtils.checkInputProperties(properties);
                PropertyUtils.checkOutputProperty(
                    triggerDefinition.getOutputDefinition()
                        .map(OutputDefinition::getOutputSchema)
                        .orElse(null));

                if (triggerDefinition.getType() == null) {
                    throw new ConfigurationException(
                        "Trigger type for trigger=%s is not defined".formatted(triggerDefinition.getName()),
                        ComponentErrorType.UNKNOWN_TRIGGER_TYPE);
                }
            }
        }
    }
}
