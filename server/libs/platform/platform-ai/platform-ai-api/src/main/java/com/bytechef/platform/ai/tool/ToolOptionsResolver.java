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

package com.bytechef.platform.ai.tool;

import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.OptionsDataSourceAware;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class ToolOptionsResolver {

    private static final Logger log = LoggerFactory.getLogger(ToolOptionsResolver.class);

    static final long CACHE_TTL_MILLIS = 60_000;
    static final int MAX_CACHE_ENTRIES = 1_000;

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final LongSupplier currentTimeMillisSupplier;

    @SuppressFBWarnings("EI")
    public ToolOptionsResolver(ClusterElementDefinitionFacade clusterElementDefinitionFacade) {
        this(clusterElementDefinitionFacade, System::currentTimeMillis);
    }

    @SuppressFBWarnings("EI")
    ToolOptionsResolver(
        ClusterElementDefinitionFacade clusterElementDefinitionFacade, LongSupplier currentTimeMillisSupplier) {

        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.currentTimeMillisSupplier = currentTimeMillisSupplier;
    }

    public static boolean hasRuntimeOptions(Property property) {
        if (property instanceof ArrayProperty || property instanceof ObjectProperty) {
            return false;
        }

        return property instanceof OptionsDataSourceAware optionsDataSourceAware &&
            optionsDataSourceAware.getOptionsDataSource() != null;
    }

    public @Nullable List<Object> resolveOptions(
        String componentName, int componentVersion, String clusterElementName, Property property,
        Map<String, ?> parameters, @Nullable Long connectionId) {

        if (!hasRuntimeOptions(property)) {
            return null;
        }

        OptionsDataSource optionsDataSource =
            Objects.requireNonNull(((OptionsDataSourceAware) property).getOptionsDataSource());

        List<String> lookupDependsOnPaths = optionsDataSource.getOptionsLookupDependsOn();

        Map<String, Object> inputParameters = getFixedParameters(parameters);

        List<Object> lookupDependsOnValues = new ArrayList<>();

        for (String lookupDependsOnPath : lookupDependsOnPaths) {
            Object dependencyValue = inputParameters.get(lookupDependsOnPath);

            if (dependencyValue == null || "".equals(dependencyValue)) {
                return null;
            }

            lookupDependsOnValues.add(dependencyValue);
        }

        CacheKey cacheKey = new CacheKey(
            TenantContext.getCurrentTenantId(), componentName, componentVersion, clusterElementName,
            property.getName(), connectionId, lookupDependsOnValues);

        long now = currentTimeMillisSupplier.getAsLong();

        CacheEntry cacheEntry = cache.get(cacheKey);

        if (cacheEntry != null && cacheEntry.expiresAtMillis() > now) {
            return cacheEntry.options();
        }

        List<Object> options = loadOptions(
            componentName, componentVersion, clusterElementName, property.getName(), inputParameters,
            lookupDependsOnPaths, connectionId);

        evictIfFull(now);

        cache.put(cacheKey, new CacheEntry(options, now + CACHE_TTL_MILLIS));

        return options;
    }

    private void evictIfFull(long now) {
        if (cache.size() < MAX_CACHE_ENTRIES) {
            return;
        }

        cache.values()
            .removeIf(cacheEntry -> cacheEntry.expiresAtMillis() <= now);

        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
    }

    private static Map<String, Object> getFixedParameters(Map<String, ?> parameters) {
        Map<String, Object> fixedParameters = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : parameters.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof FromAiResult ||
                (value instanceof String string && (string.startsWith("=") || string.contains("${")))) {

                continue;
            }

            fixedParameters.put(entry.getKey(), value);
        }

        return fixedParameters;
    }

    private @Nullable List<Object> loadOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, Object> inputParameters, List<String> lookupDependsOnPaths, @Nullable Long connectionId) {

        List<Option> options;

        try {
            options = clusterElementDefinitionFacade.executeOptions(
                componentName, componentVersion, clusterElementName, propertyName, inputParameters, Map.of(),
                lookupDependsOnPaths, null, connectionId, Map.of(), Map.of());
        } catch (RuntimeException exception) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Unable to load the options of {}.{}.{} for the tool input schema", componentName,
                    clusterElementName, propertyName, exception);
            }

            return null;
        }

        if (options == null || options.isEmpty()) {
            return null;
        }

        List<Object> optionValues = new ArrayList<>();

        for (Option option : options) {
            Object value = option.getValue();

            if (value != null) {
                optionValues.add(value);
            }
        }

        return optionValues.isEmpty() ? null : List.copyOf(optionValues);
    }

    private record CacheEntry(@Nullable List<Object> options, long expiresAtMillis) {
    }

    private record CacheKey(
        String tenantId, String componentName, int componentVersion, String clusterElementName, String propertyName,
        @Nullable Long connectionId, List<Object> lookupDependsOnValues) {
    }
}
