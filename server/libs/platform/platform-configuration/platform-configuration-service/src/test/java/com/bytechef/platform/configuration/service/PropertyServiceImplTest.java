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

package com.bytechef.platform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.credential.store.exception.ReadOnlyCredentialStoreException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PropertyServiceImplTest {

    private final CredentialStore databaseStore = stubStore(CredentialStoreType.DATABASE, false);
    private final PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);

    @Test
    void testSaveRoutesToExternalStoreWhenRegistered() {
        CredentialStore externalStore = stubStore(CredentialStoreType.AWS_SECRETS_MANAGER, false);
        PropertyServiceImpl propertyService = new PropertyServiceImpl(
            List.of(databaseStore, externalStore), propertyRepository);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.empty());
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        propertyService.save("k", Map.of("apiKey", "v"), Property.Scope.PLATFORM, null);

        verify(externalStore).storeSecret(any(CredentialSecret.class), any());
    }

    @Test
    void testSaveFallsBackToDatabaseWhenNoExternal() {
        PropertyServiceImpl propertyService = new PropertyServiceImpl(List.of(databaseStore), propertyRepository);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.empty());
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        propertyService.save("k", Map.of("apiKey", "v"), Property.Scope.PLATFORM, null);

        verify(databaseStore).storeSecret(any(CredentialSecret.class), any());
    }

    @Test
    void testSaveOnReadOnlyExternalStoreThrows() {
        CredentialStore externalStore = stubStore(CredentialStoreType.AWS_SECRETS_MANAGER, true);
        PropertyServiceImpl propertyService = new PropertyServiceImpl(
            List.of(databaseStore, externalStore), propertyRepository);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.save("k", Map.of("apiKey", "v"), Property.Scope.PLATFORM, null))
            .isInstanceOf(ReadOnlyCredentialStoreException.class);
    }

    @Test
    void testSaveMigratesDatabaseRowToExternalAndDeletesFromDatabase() {
        CredentialStore externalStore = stubStore(CredentialStoreType.AWS_SECRETS_MANAGER, false);
        PropertyServiceImpl propertyService = new PropertyServiceImpl(
            List.of(databaseStore, externalStore), propertyRepository);

        Property existingProperty = new Property();

        existingProperty.setKey("k");
        existingProperty.setScope(Property.Scope.PLATFORM);
        existingProperty.setEnabled(true);
        existingProperty.setCredentialStoreType(CredentialStoreType.DATABASE);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.of(existingProperty));
        doReturn(Map.of("apiKey", "old")).when(databaseStore)
            .getSecret(any());
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        propertyService.save("k", Map.of("apiKey", "new"), Property.Scope.PLATFORM, null);

        verify(databaseStore).deleteSecret(any(CredentialSecret.class));
        verify(externalStore).storeSecret(any(CredentialSecret.class), any());
    }

    @Test
    void testUpdateEnabledFlagDoesNotWriteExternalSecretToInlineColumn() {
        CredentialStore externalStore = stubStore(CredentialStoreType.AWS_SECRETS_MANAGER, false);
        PropertyServiceImpl propertyService = new PropertyServiceImpl(
            List.of(databaseStore, externalStore), propertyRepository);

        Property existingProperty = new Property();

        existingProperty.setKey("k");
        existingProperty.setScope(Property.Scope.PLATFORM);
        existingProperty.setEnabled(true);
        existingProperty.setCredentialStoreType(CredentialStoreType.AWS_SECRETS_MANAGER);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.of(existingProperty));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        propertyService.update("k", false, Property.Scope.PLATFORM, null);

        ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);

        verify(propertyRepository).save(captor.capture());

        Property saved = captor.getValue();

        // The external store must not be read — the inline column must not be populated before saving.
        verify(externalStore, never()).getSecret(any());
        // The store type must be preserved as-is (no migration side-effect from update).
        assertThat(saved.getCredentialStoreType()).isEqualTo(CredentialStoreType.AWS_SECRETS_MANAGER);
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    void testFetchPropertyDispatchesReadToRowsOwnStore() {
        CredentialStore externalStore = stubStore(CredentialStoreType.AWS_SECRETS_MANAGER, false);
        PropertyServiceImpl propertyService = new PropertyServiceImpl(
            List.of(databaseStore, externalStore), propertyRepository);

        Property existingProperty = new Property();

        existingProperty.setKey("k");
        existingProperty.setScope(Property.Scope.PLATFORM);
        existingProperty.setEnabled(true);
        existingProperty.setCredentialStoreType(CredentialStoreType.AWS_SECRETS_MANAGER);

        when(propertyRepository.findByKeyAndScope("k", Property.Scope.PLATFORM.ordinal()))
            .thenReturn(Optional.of(existingProperty));
        doReturn(Map.of("k", "v")).when(externalStore)
            .getSecret(any());

        propertyService.fetchProperty("k", Property.Scope.PLATFORM, null);

        verify(externalStore).getSecret(any(CredentialSecret.class));
        verify(databaseStore, never()).getSecret(any());
    }

    @Test
    void testGetPropertiesByKeyPrefixDispatchesToScopeIdAndEnvironmentFinderAndPopulatesValues() {
        PropertyServiceImpl propertyService = new PropertyServiceImpl(List.of(databaseStore), propertyRepository);

        Property property = new Property();

        property.setKey("variable.API_URL");
        property.setScope(Property.Scope.WORKSPACE);
        property.setScopeId(7L);
        property.setEnabled(true);
        property.setCredentialStoreType(CredentialStoreType.DATABASE);

        when(propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdAndEnvironment(
            "variable.", Property.Scope.WORKSPACE.ordinal(), 7L, 0))
                .thenReturn(List.of(property));
        doReturn(Map.of("value", "https://api")).when(databaseStore)
            .getSecret(any());

        List<Property> properties = propertyService.getPropertiesByKeyPrefix(
            "variable.", Property.Scope.WORKSPACE, 7L, 0L);

        assertThat(properties).hasSize(1);
        assertThat(properties.getFirst()
            .get("value")).isEqualTo("https://api");
    }

    @Test
    void testGetPropertiesByKeyPrefixWithNullScopeIdUsesScopeIdIsNullFinder() {
        PropertyServiceImpl propertyService = new PropertyServiceImpl(List.of(databaseStore), propertyRepository);

        when(propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdIsNullAndEnvironment(
            "variable.", Property.Scope.EMBEDDED.ordinal(), 2))
                .thenReturn(List.of());

        List<Property> properties = propertyService.getPropertiesByKeyPrefix(
            "variable.", Property.Scope.EMBEDDED, null, 2L);

        assertThat(properties).isEmpty();
        verify(propertyRepository).findAllByKeyStartingWithAndScopeAndScopeIdIsNullAndEnvironment(
            "variable.", Property.Scope.EMBEDDED.ordinal(), 2);
    }

    private static CredentialStore stubStore(CredentialStoreType type, boolean readOnly) {
        CredentialStore store = Mockito.mock(CredentialStore.class);

        when(store.getType()).thenReturn(type);
        when(store.isReadOnly()).thenReturn(readOnly);
        doReturn(Map.of()).when(store)
            .getSecret(any());

        return store;
    }
}
