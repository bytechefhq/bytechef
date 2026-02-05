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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ClusterElementContextImplTest {

    private CacheManager cacheManager;
    private DataStorage dataStorage;
    private ApplicationEventPublisher eventPublisher;
    private HttpClientExecutor httpClientExecutor;
    private TempFileStorage tempFileStorage;

    @BeforeEach
    void setUp() {
        cacheManager = mock(CacheManager.class);
        dataStorage = mock(DataStorage.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        httpClientExecutor = mock(HttpClientExecutor.class);
        tempFileStorage = mock(TempFileStorage.class);
    }

    @Test
    void testBuilderWithAllFields() {
        LogFileStorage logFileStorage = mock(LogFileStorage.class);
        ComponentConnection componentConnection = mock(ComponentConnection.class);
        ClusterElementResolverFunction clusterElementResolver = mock(ClusterElementResolverFunction.class);

        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .clusterElementResolver(clusterElementResolver)
            .componentConnection(componentConnection)
            .environmentId(100L)
            .jobId(200L)
            .jobPrincipalId(300L)
            .jobPrincipalWorkflowId(400L)
            .logFileStorageWriter(logFileStorage)
            .publicUrl("https://example.com")
            .type(PlatformType.AUTOMATION)
            .workflowId("workflow-123")
            .build();

        assertNotNull(context);
        assertEquals(100L, context.getEnvironmentId());
    }

    @Test
    void testBuilderWithMinimalFields() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", true,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        assertNotNull(context);
        assertNull(context.getEnvironmentId());
    }

    @Test
    void testResolveClusterElement() {
        ClusterElementResolverFunction clusterElementResolver = mock(ClusterElementResolverFunction.class);
        ClusterElementType clusterElementType = mock(ClusterElementType.class);

        when(clusterElementResolver.resolve(
            org.mockito.ArgumentMatchers.eq(clusterElementType), org.mockito.ArgumentMatchers.any()))
                .thenReturn("result");

        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .clusterElementResolver(clusterElementResolver)
            .build();

        String result = context.resolveClusterElement(
            clusterElementType,
            (element, inputParams, connectionParams, ctx) -> "result");

        assertEquals("result", result);
    }

    @Test
    void testResolveClusterElementWithNullResolver() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        ClusterElementType clusterElementType = mock(ClusterElementType.class);

        assertThrows(NullPointerException.class, () -> context.resolveClusterElement(
            clusterElementType,
            (element, inputParams, connectionParams, ctx) -> "result"));
    }

    @Test
    void testToActionContext() {
        ComponentConnection componentConnection = mock(ComponentConnection.class);

        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .environmentId(100L)
            .jobId(200L)
            .type(PlatformType.AUTOMATION)
            .workflowId("workflow-123")
            .build();

        ActionContext actionContext = context.toActionContext(
            "newComponent", 2, "newAction", componentConnection);

        assertNotNull(actionContext);
    }

    @Test
    void testNestedContainsPath() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        Map<String, Object> map = new HashMap<>();

        map.put("level1", Map.of("level2", Map.of("level3", "value")));
        map.put("simple", "simpleValue");

        boolean result = context.nested(nested -> {
            assertTrue(nested.containsPath(map, "simple"));
            assertTrue(nested.containsPath(map, "level1"));
            assertTrue(nested.containsPath(map, "level1.level2"));
            assertTrue(nested.containsPath(map, "level1.level2.level3"));
            assertFalse(nested.containsPath(map, "nonexistent"));
            assertFalse(nested.containsPath(map, "level1.nonexistent"));
            assertFalse(nested.containsPath(map, ""));

            return true;
        });

        assertTrue(result);
    }

    @Test
    void testNestedGetValue() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        Map<String, Object> map = new HashMap<>();

        map.put("level1", Map.of("level2", Map.of("level3", "deepValue")));
        map.put("simple", "simpleValue");
        map.put("number", 42);

        context.nested(nested -> {
            assertEquals("simpleValue", nested.getValue(map, "simple"));
            assertEquals(42, nested.getValue(map, "number"));
            assertEquals("deepValue", nested.getValue(map, "level1.level2.level3"));
            assertNull(nested.getValue(map, "nonexistent"));
            assertNull(nested.getValue(map, "level1.nonexistent"));

            return null;
        });
    }

    @Test
    void testNestedSetValue() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        Map<String, Object> map = new HashMap<>();

        context.nested(nested -> {
            nested.setValue(map, "simple", "value1");

            assertEquals("value1", map.get("simple"));

            nested.setValue(map, "level1.level2.level3", "deepValue");

            @SuppressWarnings("unchecked")
            Map<String, Object> level1 = (Map<String, Object>) map.get("level1");

            assertNotNull(level1);

            @SuppressWarnings("unchecked")
            Map<String, Object> level2 = (Map<String, Object>) level1.get("level2");

            assertNotNull(level2);
            assertEquals("deepValue", level2.get("level3"));

            nested.setValue(map, "simple", null);

            assertFalse(map.containsKey("simple"));

            nested.setValue(map, "", "ignored");

            assertFalse(map.containsKey(""));

            return null;
        });
    }

    @Test
    void testNestedFlatten() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        Map<String, Object> nestedMap = new HashMap<>();

        nestedMap.put("level1", Map.of(
            "level2a", "value2a",
            "level2b", Map.of("level3", "value3")));
        nestedMap.put("simple", "simpleValue");

        Map<String, Object> flattened = context.nested(nested -> nested.flatten(nestedMap));

        assertEquals(3, flattened.size());
        assertEquals("simpleValue", flattened.get("simple"));
        assertEquals("value2a", flattened.get("level1.level2a"));
        assertEquals("value3", flattened.get("level1.level2b.level3"));
    }

    @Test
    void testNestedUnflatten() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        Map<String, Object> flatMap = new HashMap<>();

        flatMap.put("simple", "simpleValue");
        flatMap.put("level1.level2a", "value2a");
        flatMap.put("level1.level2b.level3", "value3");

        Map<String, Object> unflattened = context.nested(nested -> nested.unflatten(flatMap));

        assertEquals("simpleValue", unflattened.get("simple"));

        @SuppressWarnings("unchecked")
        Map<String, Object> level1 = (Map<String, Object>) unflattened.get("level1");

        assertNotNull(level1);
        assertEquals("value2a", level1.get("level2a"));

        @SuppressWarnings("unchecked")
        Map<String, Object> level2b = (Map<String, Object>) level1.get("level2b");

        assertNotNull(level2b);
        assertEquals("value3", level2b.get("level3"));
    }

    @Test
    void testNestedFlattenAndUnflattenRoundTrip() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        Map<String, Object> original = new HashMap<>();

        original.put("a", Map.of(
            "b", Map.of("c", "abc"),
            "d", "ad"));
        original.put("e", "e");

        context.nested(nested -> {
            Map<String, Object> flattened = nested.flatten(original);
            Map<String, Object> restored = nested.unflatten(flattened);

            assertEquals("abc", nested.getValue(restored, "a.b.c"));
            assertEquals("ad", nested.getValue(restored, "a.d"));
            assertEquals("e", nested.getValue(restored, "e"));

            return null;
        });
    }

    @Test
    void testNestedWithEmptyMap() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        Map<String, Object> emptyMap = new HashMap<>();

        context.nested(nested -> {
            assertFalse(nested.containsPath(emptyMap, "any"));
            assertNull(nested.getValue(emptyMap, "any"));

            Map<String, Object> flattened = nested.flatten(emptyMap);

            assertTrue(flattened.isEmpty());

            Map<String, Object> unflattened = nested.unflatten(emptyMap);

            assertTrue(unflattened.isEmpty());

            return null;
        });
    }

    @Test
    void testNestedExceptionHandling() {
        ClusterElementContextImpl context = ClusterElementContextImpl.builder(
            "testComponent", 1, "testElement", false,
            cacheManager, dataStorage, eventPublisher, httpClientExecutor, tempFileStorage)
            .build();

        assertThrows(RuntimeException.class, () -> context.nested(nested -> {
            throw new Exception("Test exception");
        }));
    }
}
