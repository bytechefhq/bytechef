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

package com.bytechef.automation.search.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class AutomationSearchFacadeTest {

    private final SearchAssetProvider searchAssetProvider = mock(SearchAssetProvider.class);

    private final AutomationSearchFacadeImpl automationSearchFacade = new AutomationSearchFacadeImpl(
        List.of(searchAssetProvider));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSearchRefusesAConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of("external-user"));

        assertThatThrownBy(() -> automationSearchFacade.search("slack", 10))
            .isInstanceOf(AccessDeniedException.class);

        verify(searchAssetProvider, never()).search(anyString(), anyInt());
    }

    @Test
    void testSearchQueriesEveryProviderForAPlatformUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of()));

        doReturn(List.of()).when(searchAssetProvider)
            .search("slack", 10);

        assertThat(automationSearchFacade.search("slack", 10)).isEmpty();

        verify(searchAssetProvider).search("slack", 10);
    }
}
