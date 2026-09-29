/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.crm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.ee.embedded.unified.facade.UnifiedApiFacade;
import com.bytechef.ee.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.CreateUpdateAccountModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.ListAccountsPageableParameterModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CrmAccountApiControllerTest {

    private static final String ACCOUNT_ID = "account-1";
    private static final String ENVIRONMENT = "production";
    private static final String EXTERNAL_USER_ID = "alice";
    private static final long INSTANCE_ID = 7L;

    private final UnifiedApiFacade unifiedApiFacade = mock(UnifiedApiFacade.class);

    private final CrmAccountApiController crmAccountApiController =
        new CrmAccountApiController(mock(ConversionService.class), unifiedApiFacade);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCrmAccountActsAsTheCallingConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(EXTERNAL_USER_ID));

        crmAccountApiController.getAccount(ACCOUNT_ID, INSTANCE_ID, ENVIRONMENT, false);
        crmAccountApiController.updateAccount(ACCOUNT_ID, new CreateUpdateAccountModel(), INSTANCE_ID, ENVIRONMENT);

        verify(unifiedApiFacade).get(
            eq(EXTERNAL_USER_ID), eq(ACCOUNT_ID), any(), eq(INSTANCE_ID), eq(Environment.PRODUCTION), any());
        verify(unifiedApiFacade).update(
            eq(EXTERNAL_USER_ID), eq(ACCOUNT_ID), any(), any(), eq(INSTANCE_ID), eq(Environment.PRODUCTION), any());
    }

    @Test
    void testCrmAccountCreatesAndListsAsTheCallingConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(EXTERNAL_USER_ID));

        doReturn(mock(CursorPageSlice.class)).when(unifiedApiFacade)
            .getPage(any(), any(), any(), any(), any(), any());

        crmAccountApiController.createAccount(new CreateUpdateAccountModel(), INSTANCE_ID, ENVIRONMENT);
        crmAccountApiController.listAccounts(
            INSTANCE_ID, ENVIRONMENT, false, new ListAccountsPageableParameterModel());

        verify(unifiedApiFacade).create(
            eq(EXTERNAL_USER_ID), any(), any(), eq(INSTANCE_ID), eq(Environment.PRODUCTION), any());
        verify(unifiedApiFacade).getPage(
            eq(EXTERNAL_USER_ID), any(), any(), eq(INSTANCE_ID), eq(Environment.PRODUCTION), any());
    }

    @Test
    void testAPlatformSessionWhoseLoginIsAnExternalIdIsRefused() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(EXTERNAL_USER_ID, "", List.of()));

        assertThatThrownBy(() -> crmAccountApiController.getAccount(ACCOUNT_ID, INSTANCE_ID, ENVIRONMENT, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> crmAccountApiController.updateAccount(
                ACCOUNT_ID, new CreateUpdateAccountModel(), INSTANCE_ID, ENVIRONMENT))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> crmAccountApiController.createAccount(new CreateUpdateAccountModel(), INSTANCE_ID, ENVIRONMENT))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> crmAccountApiController.listAccounts(
                INSTANCE_ID, ENVIRONMENT, false, new ListAccountsPageableParameterModel()))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(unifiedApiFacade);
    }
}
