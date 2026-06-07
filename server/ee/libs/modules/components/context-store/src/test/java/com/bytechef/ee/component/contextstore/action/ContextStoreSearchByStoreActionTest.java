/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.contextstore.action;

import static com.bytechef.ee.component.contextstore.action.ContextStoreSearchByStoreAction.CONTEXT_STORE_ID;
import static com.bytechef.ee.component.contextstore.action.ContextStoreSearchByStoreAction.CONTEXT_STORE_NAME;
import static com.bytechef.ee.component.contextstore.action.ContextStoreSearchByStoreAction.ENTITY;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.service.ContextStoreNameLookupService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Unit tests for the env-portable name-based resolution path on {@link ContextStoreSearchByStoreAction}. Focused on
 * {@code resolveContextStoreId}: ensures id-takes-precedence, name+workspace+env round-trip, and every error branch
 * raises with a clear actionable message.
 *
 * <p>
 * The fan-out search loop is exercised separately by integration coverage; this test mocks the source service to return
 * an empty list so each test isolates the resolution logic.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ContextStoreSearchByStoreActionTest {

    private static final long WORKSPACE_ID = 7L;
    private static final long ENVIRONMENT_ID = 1L;
    private static final long RESOLVED_STORE_ID = 42L;

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ContextStoreNameLookupService> lookupServiceProvider = mock(ObjectProvider.class);
    private final ContextStoreNameLookupService lookupService = mock(ContextStoreNameLookupService.class);
    private final ContextStoreQueryService queryService = mock(ContextStoreQueryService.class);
    private final ContextStoreSourceService sourceService = mock(ContextStoreSourceService.class);

    private ContextStoreSearchByStoreAction action;

    @BeforeEach
    void setUp() {
        when(lookupServiceProvider.getIfAvailable()).thenReturn(lookupService);
        when(sourceService.findAllByContextStoreId(anyLong())).thenReturn(List.of());

        action = newAction();
    }

    @Test
    void testExplicitContextStoreIdSkipsNameLookup() {
        Parameters inputs = mockInputs(123L, null, "contacts");
        ActionContextAware ctx = mockAware(WORKSPACE_ID, ENVIRONMENT_ID);

        action.perform(inputs, mock(Parameters.class), ctx);

        verify(sourceService).findAllByContextStoreId(123L);
        verify(lookupService, never()).findIdByName(anyLong(), any(), anyLong());
    }

    @Test
    void testNameResolvesViaWorkspaceAndEnvironmentFromContext() {
        when(lookupService.findIdByName(eq(WORKSPACE_ID), eq("ProductCatalog"), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.of(RESOLVED_STORE_ID));

        Parameters inputs = mockInputs(null, "ProductCatalog", "products");
        ActionContextAware ctx = mockAware(WORKSPACE_ID, ENVIRONMENT_ID);

        action.perform(inputs, mock(Parameters.class), ctx);

        verify(sourceService).findAllByContextStoreId(RESOLVED_STORE_ID);
    }

    @Test
    void testNeitherIdNorNameThrowsClearError() {
        Parameters inputs = mockInputs(null, null, "products");
        ActionContextAware ctx = mockAware(WORKSPACE_ID, ENVIRONMENT_ID);

        assertThatThrownBy(() -> action.perform(inputs, mock(Parameters.class), ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(CONTEXT_STORE_ID)
            .hasMessageContaining(CONTEXT_STORE_NAME);
    }

    @Test
    void testNameWithoutWorkspaceInMetadataThrowsActionableError() {
        // No __jobParameters in metadata — simulates an ad-hoc REST invocation or editor-environment run where the
        // trigger-time contributor never fired.
        Parameters inputs = mockInputs(null, "ProductCatalog", "products");
        ActionContextAware ctx = mock(ActionContextAware.class);

        when(ctx.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        when(ctx.getJobMetadata()).thenReturn(Map.of());

        assertThatThrownBy(() -> action.perform(inputs, mock(Parameters.class), ctx))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ProductCatalog")
            .hasMessageContaining(CONTEXT_STORE_ID);
    }

    @Test
    void testNameNotFoundInEnvThrowsClearError() {
        when(lookupService.findIdByName(anyLong(), any(), anyLong())).thenReturn(Optional.empty());

        Parameters inputs = mockInputs(null, "ProductCatalog", "products");
        ActionContextAware ctx = mockAware(WORKSPACE_ID, ENVIRONMENT_ID);

        assertThatThrownBy(() -> action.perform(inputs, mock(Parameters.class), ctx))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ProductCatalog")
            .hasMessageContaining(String.valueOf(WORKSPACE_ID))
            .hasMessageContaining(String.valueOf(ENVIRONMENT_ID));
    }

    @Test
    void testNamePathDegradesGracefullyWhenLookupServiceBeanAbsent() {
        when(lookupServiceProvider.getIfAvailable()).thenReturn(null);

        Parameters inputs = mockInputs(null, "ProductCatalog", "products");
        ActionContextAware ctx = mockAware(WORKSPACE_ID, ENVIRONMENT_ID);

        assertThatThrownBy(() -> action.perform(inputs, mock(Parameters.class), ctx))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("automation-context-store");
    }

    private ContextStoreSearchByStoreAction newAction() {
        // Package-private ctor: test sits in the action's package so it can build the instance directly. The public
        // entry-point of(...) returns a ModifiableActionDefinition, which is wrong for exercising perform() in
        // isolation.
        return new ContextStoreSearchByStoreAction(lookupServiceProvider, queryService, sourceService);
    }

    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    private static Parameters mockInputs(Long id, String name, String entity) {
        Parameters parameters = mock(Parameters.class);

        when(parameters.getLong(CONTEXT_STORE_ID)).thenReturn(id);
        when(parameters.getString(CONTEXT_STORE_NAME)).thenReturn(name);
        when(parameters.getRequiredString(ENTITY)).thenReturn(entity);
        when(parameters.getInteger(eq("limit"), eq(50))).thenReturn(50);
        when(parameters.getBoolean(eq("includeDeleted"), eq(false))).thenReturn(false);
        // Disambiguate the overloaded getList: the action uses the TypeReference variant. Raw cast avoids the
        // ambiguity-detection warning Mockito flags between the Class-based and TypeReference-based overloads.
        when(parameters.getList(eq("filters"), any(TypeReference.class), eq((List) List.of())))
            .thenReturn(List.of());
        when(parameters.getList(eq("sort"), any(TypeReference.class), eq((List) List.of()))).thenReturn(List.of());

        return parameters;
    }

    private static ActionContextAware mockAware(long workspaceId, long environmentId) {
        ActionContextAware ctx = mock(ActionContextAware.class);

        when(ctx.getEnvironmentId()).thenReturn(environmentId);
        when(ctx.getJobMetadata()).thenReturn(Map.of(
            "__jobParameters", Map.of("contextStore.workspaceId", workspaceId)));

        return ctx;
    }

    @SuppressWarnings("unused")
    private static ContextStoreSearchResult emptyResult() {
        return new ContextStoreSearchResult(List.of(), null);
    }
}
