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

package com.bytechef.platform.component.owner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.owner.Owner;
import com.bytechef.platform.owner.OwnerResolver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class OwnerResolutionTest {

    @Test
    void testNoResolverMeansNoOwner() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, emptyProvider());

        assertEquals(Optional.empty(), owner);
    }

    @Test
    void testJobRunResolvesThroughTheJobPrincipal() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.isEditorEnvironment()).thenReturn(false);
        when(actionContextAware.getJobPrincipalId()).thenReturn(77L);
        when(actionContextAware.getPlatformType()).thenReturn(PlatformType.EMBEDDED);

        OwnerResolver ownerResolver = mock(OwnerResolver.class);

        when(ownerResolver.resolveJobPrincipal(77L, PlatformType.EMBEDDED))
            .thenReturn(Optional.of(Owner.connectedUser(1055L)));

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, providerOf(ownerResolver));

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);
    }

    @Test
    void testEditorRunResolvesFromTheSecurityPrincipal() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.isEditorEnvironment()).thenReturn(true);

        OwnerResolver ownerResolver = mock(OwnerResolver.class);

        when(ownerResolver.resolveCurrentPrincipal()).thenReturn(Optional.of(Owner.connectedUser(1055L)));

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, providerOf(ownerResolver));

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);

        verify(actionContextAware, never()).getJobPrincipalId();
    }

    @Test
    void testEditorRunNeverFallsBackToTheJobPrincipal() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.isEditorEnvironment()).thenReturn(true);

        OwnerResolver ownerResolver = mock(OwnerResolver.class);

        when(ownerResolver.resolveCurrentPrincipal()).thenReturn(Optional.empty());

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, providerOf(ownerResolver));

        assertEquals(Optional.empty(), owner);

        verify(actionContextAware, never()).getJobPrincipalId();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<OwnerResolver> emptyProvider() {
        ObjectProvider<OwnerResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(null);

        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<OwnerResolver> providerOf(OwnerResolver ownerResolver) {
        ObjectProvider<OwnerResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(ownerResolver);

        return objectProvider;
    }
}
