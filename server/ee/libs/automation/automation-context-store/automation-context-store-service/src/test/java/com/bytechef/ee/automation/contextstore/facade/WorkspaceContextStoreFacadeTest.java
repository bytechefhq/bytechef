/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceContextStoreFacadeTest {

    private static final long CONTEXT_STORE_ID = 7L;
    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ContextStoreService contextStoreService;

    @Mock
    private ContextStoreSourceService contextStoreSourceService;

    @Mock
    private TagService tagService;

    @Mock
    private WorkspaceContextStoreService workspaceContextStoreService;

    @Mock
    private WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade;

    private WorkspaceContextStoreFacadeImpl workspaceContextStoreFacade;

    @BeforeEach
    void beforeEach() {
        workspaceContextStoreFacade = new WorkspaceContextStoreFacadeImpl(
            contextStoreService, contextStoreSourceService, tagService, workspaceContextStoreService,
            workspaceContextStoreSourceFacade);
    }

    @Test
    void testDeleteWorkspaceContextStoreDeletesSourcesBeforeTheStore() {
        when(workspaceContextStoreService.isStoreInWorkspace(WORKSPACE_ID, CONTEXT_STORE_ID)).thenReturn(true);
        when(contextStoreSourceService.findAllByContextStoreId(CONTEXT_STORE_ID))
            .thenReturn(List.of(createContextStoreSource(11L), createContextStoreSource(12L)));

        workspaceContextStoreFacade.deleteWorkspaceContextStore(WORKSPACE_ID, CONTEXT_STORE_ID);

        InOrder inOrder = inOrder(workspaceContextStoreSourceFacade, contextStoreService);

        inOrder.verify(workspaceContextStoreSourceFacade)
            .delete(WORKSPACE_ID, 11L);
        inOrder.verify(workspaceContextStoreSourceFacade)
            .delete(WORKSPACE_ID, 12L);
        inOrder.verify(contextStoreService)
            .delete(CONTEXT_STORE_ID);
    }

    @Test
    void testDeleteWorkspaceContextStoreRejectsStoreOutsideWorkspace() {
        when(workspaceContextStoreService.isStoreInWorkspace(WORKSPACE_ID, CONTEXT_STORE_ID)).thenReturn(false);

        assertThatThrownBy(
            () -> workspaceContextStoreFacade.deleteWorkspaceContextStore(WORKSPACE_ID, CONTEXT_STORE_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(contextStoreService, never()).delete(CONTEXT_STORE_ID);
    }

    private static ContextStoreSource createContextStoreSource(long id) {
        ContextStoreSource contextStoreSource = new ContextStoreSource();

        contextStoreSource.setId(id);

        return contextStoreSource;
    }
}
