/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.data.table.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.data.table.configuration.service.DataTableService;
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
class EmbeddedDataTableApiFacadeTest {

    @Mock
    private DataTableService dataTableService;

    @InjectMocks
    private EmbeddedDataTableApiFacadeImpl embeddedDataTableApiFacade;

    /**
     * Not a tautology. The failure this catches is a later refactor moving the gate onto the controller, where it
     * compiles, reads as protected, and silently stops applying.
     */
    @Test
    void testEveryFacadeMethodIsGatedOnTenantAdmin() throws Exception {
        assertTenantAdminGated("getDataTables", long.class, Long.class);
        assertTenantAdminGated("assignDataTableOwner", long.class, Long.class);
    }

    @Test
    void testNoOwnerListsEveryTableInTheTenant() {
        embeddedDataTableApiFacade.getDataTables(0, null);

        verify(dataTableService).listTables(eq(0L), eq(Optional.empty()));
    }

    @Test
    void testAnOwnerListsWhatThatAccountWouldSee() {
        embeddedDataTableApiFacade.getDataTables(0, 42L);

        verify(dataTableService).listTables(eq(0L), eq(Optional.of(Owner.connectedUser(42L))));
    }

    @Test
    void testAssigningAnOwnerPassesAConnectedUser() {
        embeddedDataTableApiFacade.assignDataTableOwner(7L, 42L);

        verify(dataTableService).assignOwner(7L, Owner.connectedUser(42L));
    }

    @Test
    void testAssigningNoOwnerReturnsTheTableToTheVendor() {
        embeddedDataTableApiFacade.assignDataTableOwner(7L, null);

        verify(dataTableService).assignOwner(7L, null);
    }

    private static void assertTenantAdminGated(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = EmbeddedDataTableApiFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, methodName + " is not gated");
        assertEquals("isTenantAdmin()", preAuthorize.value());
    }
}
