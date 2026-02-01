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

package com.bytechef.platform.component.context;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ClusterElementContextImpl extends ContextImpl implements ClusterElementContextAware {

    private final CacheManager cacheManager;
    private final @Nullable ClusterElementResolverFunction clusterElementResolver;
    private final DataStorage dataStorage;
    private final boolean editorEnvironment;
    private final @Nullable Long environmentId;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpClientExecutor httpClientExecutor;
    private final @Nullable Long jobId;
    private final @Nullable Long jobPrincipalId;
    private final @Nullable Long jobPrincipalWorkflowId;
    private final Nested nested;
    private final @Nullable String publicUrl;
    private final TempFileStorage tempFileStorage;
    private final @Nullable PlatformType type;
    private final @Nullable String workflowId;

    @SuppressFBWarnings("EI")
    private ClusterElementContextImpl(Builder builder) {
        super(
            builder.componentName, builder.componentVersion, builder.clusterElementName, builder.componentConnection,
            builder.editorEnvironment, builder.httpClientExecutor, builder.tempFileStorage);

        this.cacheManager = builder.cacheManager;
        this.clusterElementResolver = builder.clusterElementResolver;
        this.dataStorage = builder.dataStorage;
        this.editorEnvironment = builder.editorEnvironment;
        this.environmentId = builder.environmentId;
        this.eventPublisher = builder.eventPublisher;
        this.httpClientExecutor = builder.httpClientExecutor;
        this.jobId = builder.jobId;
        this.jobPrincipalId = builder.jobPrincipalId;
        this.jobPrincipalWorkflowId = builder.jobPrincipalWorkflowId;
        this.nested = new NestedImpl();
        this.publicUrl = builder.publicUrl;
        this.tempFileStorage = builder.tempFileStorage;
        this.type = builder.type;
        this.workflowId = builder.workflowId;
    }

    static Builder builder(
        String componentName, int componentVersion, String clusterElementName, boolean editorEnvironment,
        CacheManager cacheManager, DataStorage dataStorage, ApplicationEventPublisher eventPublisher,
        HttpClientExecutor httpClientExecutor, TempFileStorage tempFileStorage) {

        return new Builder(
            componentName, componentVersion, clusterElementName, editorEnvironment, cacheManager, dataStorage,
            eventPublisher, httpClientExecutor, tempFileStorage);
    }

    @Override
    public <T> T resolveClusterElement(
        ClusterElementType clusterElementType, ClusterElementFunction<T> clusterElementFunction) {

        return Objects.requireNonNull(clusterElementResolver)
            .resolve(clusterElementType, clusterElementFunction);
    }

    @Override
    public ActionContext toActionContext(
        String componentName, int componentVersion, String actionName,
        @Nullable ComponentConnection componentConnection) {

        return ActionContextImpl
            .builder(
                componentName, componentVersion, actionName, editorEnvironment, cacheManager, dataStorage,
                eventPublisher, httpClientExecutor, tempFileStorage)
            .componentConnection(componentConnection)
            .environmentId(environmentId)
            .jobId(jobId)
            .jobPrincipalId(jobPrincipalId)
            .jobPrincipalWorkflowId(jobPrincipalWorkflowId)
            .publicUrl(publicUrl)
            .type(type)
            .workflowId(workflowId)
            .build();
    }

    @Override
    public @Nullable Long getEnvironmentId() {
        return environmentId;
    }

    @Override
    public <R> R nested(ContextFunction<Nested, R> nestedFunction) {
        try {
            return nestedFunction.apply(nested);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    private record NestedImpl() implements Nested {

        @Override
        public boolean containsPath(Map<String, Object> map, String path) {
            if (path.isEmpty()) {
                return false;
            }

            String[] parts = path.split("\\.");

            Object current = map;

            for (int i = 0; i < parts.length; i++) {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMap = (Map<String, Object>) current;

                    if (i == parts.length - 1) {
                        return currentMap.containsKey(parts[i]);
                    }

                    current = currentMap.get(parts[i]);

                    if (current == null) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            return false;
        }

        @Override
        public Map<String, Object> flatten(Map<String, Object> map) {
            Map<String, Object> result = new HashMap<>();

            flattenRecursive(map, "", result);

            return result;
        }

        @Override
        public Object getValue(Map<String, Object> map, String path) {
            String[] parts = path.split("\\.");

            Object current = map;

            for (String part : parts) {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMap = (Map<String, Object>) current;

                    current = currentMap.get(part);

                    if (current == null) {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            return current;
        }

        @Override
        public Map<String, Object> setValue(Map<String, Object> map, String path, @Nullable Object value) {
            if (path.isEmpty()) {
                return map;
            }

            String[] parts = path.split("\\.");

            Map<String, Object> current = map;

            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);

                if (next instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nextMap = (Map<String, Object>) next;

                    current = nextMap;
                } else {
                    Map<String, Object> newMap = new HashMap<>();

                    current.put(parts[i], newMap);

                    current = newMap;
                }
            }

            if (value == null) {
                current.remove(parts[parts.length - 1]);
            } else {
                current.put(parts[parts.length - 1], value);
            }

            return map;
        }

        @Override
        public Map<String, Object> unflatten(Map<String, Object> map) {
            Map<String, Object> result = new HashMap<>();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                setValue(result, entry.getKey(), entry.getValue());
            }

            return result;
        }

        private void flattenRecursive(Map<String, Object> map, String prefix, Map<String, Object> result) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> valueMap = (Map<String, Object>) value;

                    flattenRecursive(valueMap, key, result);
                } else {
                    result.put(key, value);
                }
            }
        }
    }

    static final class Builder {

        private final CacheManager cacheManager;
        private final String clusterElementName;
        private @Nullable ClusterElementResolverFunction clusterElementResolver;
        private @Nullable ComponentConnection componentConnection;
        private final String componentName;
        private final int componentVersion;
        private final DataStorage dataStorage;
        private final boolean editorEnvironment;
        private @Nullable Long environmentId;
        private final ApplicationEventPublisher eventPublisher;
        private final HttpClientExecutor httpClientExecutor;
        private @Nullable Long jobId;
        private @Nullable Long jobPrincipalId;
        private @Nullable Long jobPrincipalWorkflowId;
        private @Nullable String publicUrl;
        private final TempFileStorage tempFileStorage;
        private @Nullable PlatformType type;
        private @Nullable String workflowId;

        private Builder(
            String componentName, int componentVersion, String clusterElementName, boolean editorEnvironment,
            CacheManager cacheManager, DataStorage dataStorage, ApplicationEventPublisher eventPublisher,
            HttpClientExecutor httpClientExecutor, TempFileStorage tempFileStorage) {

            this.componentName = componentName;
            this.componentVersion = componentVersion;
            this.clusterElementName = clusterElementName;
            this.editorEnvironment = editorEnvironment;
            this.cacheManager = cacheManager;
            this.dataStorage = dataStorage;
            this.eventPublisher = eventPublisher;
            this.httpClientExecutor = httpClientExecutor;
            this.tempFileStorage = tempFileStorage;
        }

        Builder clusterElementResolver(@Nullable ClusterElementResolverFunction clusterElementResolver) {
            this.clusterElementResolver = clusterElementResolver;

            return this;
        }

        Builder componentConnection(@Nullable ComponentConnection componentConnection) {
            this.componentConnection = componentConnection;

            return this;
        }

        Builder environmentId(@Nullable Long environmentId) {
            this.environmentId = environmentId;

            return this;
        }

        Builder jobId(@Nullable Long jobId) {
            this.jobId = jobId;

            return this;
        }

        Builder jobPrincipalId(@Nullable Long jobPrincipalId) {
            this.jobPrincipalId = jobPrincipalId;

            return this;
        }

        Builder jobPrincipalWorkflowId(@Nullable Long jobPrincipalWorkflowId) {
            this.jobPrincipalWorkflowId = jobPrincipalWorkflowId;

            return this;
        }

        Builder publicUrl(@Nullable String publicUrl) {
            this.publicUrl = publicUrl;

            return this;
        }

        Builder type(@Nullable PlatformType type) {
            this.type = type;

            return this;
        }

        Builder workflowId(@Nullable String workflowId) {
            this.workflowId = workflowId;

            return this;
        }

        ClusterElementContextImpl build() {
            return new ClusterElementContextImpl(this);
        }
    }
}
