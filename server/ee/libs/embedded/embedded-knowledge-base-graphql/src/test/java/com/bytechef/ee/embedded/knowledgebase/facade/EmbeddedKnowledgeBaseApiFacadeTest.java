/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.knowledgebase.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.owner.Owner;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class EmbeddedKnowledgeBaseApiFacadeTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private EmbeddedKnowledgeBaseApiFacadeImpl embeddedKnowledgeBaseApiFacade;

    /**
     * Not a tautology. The failure this catches is a later refactor moving the gate onto the controller, where it
     * compiles, reads as protected, and silently stops applying.
     */
    @Test
    void testEveryFacadeMethodIsGatedOnTenantAdmin() throws Exception {
        assertTenantAdminGated("getKnowledgeBases", int.class, Long.class);
        assertTenantAdminGated("assignKnowledgeBaseOwner", long.class, Long.class);
    }

    @Test
    void testNoOwnerListsEveryKnowledgeBaseInTheTenant() {
        embeddedKnowledgeBaseApiFacade.getKnowledgeBases(0, null);

        verify(knowledgeBaseService).getKnowledgeBases(eq(0), eq(Optional.empty()));
    }

    @Test
    void testAnOwnerListsWhatThatAccountWouldSee() {
        embeddedKnowledgeBaseApiFacade.getKnowledgeBases(0, 42L);

        verify(knowledgeBaseService).getKnowledgeBases(eq(0), eq(Optional.of(Owner.connectedUser(42L))));
    }

    @Test
    void testAssigningAnOwnerPassesAConnectedUser() {
        embeddedKnowledgeBaseApiFacade.assignKnowledgeBaseOwner(7L, 42L);

        verify(knowledgeBaseService).assignOwner(7L, Owner.connectedUser(42L));
    }

    @Test
    void testAssigningNoOwnerReturnsTheKnowledgeBaseToTheVendor() {
        embeddedKnowledgeBaseApiFacade.assignKnowledgeBaseOwner(7L, null);

        verify(knowledgeBaseService).assignOwner(7L, null);
    }

    private static void assertTenantAdminGated(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = EmbeddedKnowledgeBaseApiFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, methodName + " is not gated");
        assertEquals("isTenantAdmin()", preAuthorize.value());
    }
}
