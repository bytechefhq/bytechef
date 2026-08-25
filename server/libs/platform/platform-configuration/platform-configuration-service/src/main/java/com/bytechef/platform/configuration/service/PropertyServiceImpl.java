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

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.credential.store.exception.ReadOnlyCredentialStoreException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class PropertyServiceImpl implements PropertyService {

    private final List<CredentialStore> credentialStores;
    private final PropertyRepository propertyRepository;

    @SuppressFBWarnings("EI2")
    public PropertyServiceImpl(List<CredentialStore> credentialStores, PropertyRepository propertyRepository) {
        this.credentialStores = credentialStores;
        this.propertyRepository = propertyRepository;
    }

    @Override
    public void delete(String key, Property.Scope scope, Long scopeId) {
        delete(key, scope, scopeId, null);
    }

    @Override
    public void delete(String key, Property.Scope scope, Long scopeId, @Nullable Long environmentId) {
        findProperty(key, scope, scopeId, environmentId)
            .ifPresent(property -> {
                CredentialStore store = getStore(property.getCredentialStoreType());

                if (!store.isReadOnly()) {
                    store.deleteSecret(property);
                }
                // When the active store is read-only, the property row is deleted but the external secret is left
                // intact. The operator owns the vault lifecycle; documented behavior.

                propertyRepository.delete(property);
            });
    }

    @Override
    public Optional<Property> fetchProperty(String key, Property.Scope scope, @Nullable Long scopeId) {
        return fetchProperty(key, scope, scopeId, null);
    }

    @Override
    public Optional<Property> fetchProperty(
        String key, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId) {

        return findProperty(key, scope, scopeId, environmentId).map(this::populateValue);
    }

    @Override
    public Property getProperty(String key, Property.Scope scope, @Nullable Long scopeId) {
        return fetchProperty(key, scope, scopeId)
            .orElseThrow(() -> new IllegalArgumentException("Property not found: " + key));
    }

    @Override
    public Property getProperty(
        String key, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId) {

        return fetchProperty(key, scope, scopeId, environmentId)
            .orElseThrow(() -> new IllegalArgumentException("Property not found: " + key));
    }

    @Override
    public List<Property> getProperties(List<String> keys, Property.Scope scope, @Nullable Long scopeId) {
        return getProperties(keys, scope, scopeId, null);
    }

    @Override
    public List<Property> getProperties(
        List<String> keys, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId) {

        if (scopeId == null && environmentId == null) {
            return populateAll(propertyRepository.findAllByKeyInAndScope(keys, scope.ordinal()));
        } else if (scopeId == null) {
            return populateAll(
                propertyRepository.findAllByKeyInAndScopeAndEnvironment(keys, scope.ordinal(),
                    environmentId.intValue()));
        } else if (environmentId == null) {
            return populateAll(propertyRepository.findAllByKeyInAndScopeAndScopeId(keys, scope.ordinal(), scopeId));
        } else {
            return populateAll(
                propertyRepository.findAllByKeyInAndScopeAndScopeIdAndEnvironment(
                    keys, scope.ordinal(), scopeId, environmentId.intValue()));
        }
    }

    @Override
    public List<Property> getPropertiesByKeyPrefix(
        String keyPrefix, Property.Scope scope, @Nullable Long scopeId, Long environmentId) {

        if (scopeId == null) {
            return populateAll(
                propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdIsNullAndEnvironment(
                    keyPrefix, scope.ordinal(), environmentId.intValue()));
        }

        return populateAll(
            propertyRepository.findAllByKeyStartingWithAndScopeAndScopeIdAndEnvironment(
                keyPrefix, scope.ordinal(), scopeId, environmentId.intValue()));
    }

    @Override
    public void save(String key, Map<String, ?> value, Property.Scope scope, @Nullable Long scopeId) {
        save(key, value, scope, scopeId, null);
    }

    @Override
    public void save(
        String key, Map<String, ?> value, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId) {

        fetchProperty(key, scope, scopeId, environmentId)
            .ifPresentOrElse(property -> {
                storeValue(property, value);

                propertyRepository.save(property);
            }, () -> {
                Property property = new Property();

                property.setEnabled(true);

                if (environmentId != null) {
                    property.setEnvironment(environmentId.intValue());
                }

                property.setKey(key);
                property.setScope(scope);
                property.setScopeId(scopeId);

                storeValue(property, value);

                propertyRepository.save(property);
            });
    }

    @Override
    public void update(String key, boolean enabled, Property.Scope scope, @Nullable Long scopeId) {
        update(key, enabled, scope, scopeId, null);
    }

    @Override
    public void update(
        String key, boolean enabled, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId) {

        // Use the raw (no-populate) lookup so the inline value field is not loaded from the external
        // store before saving. For external rows (credentialStoreType != DATABASE) the inline column
        // must stay empty; calling fetchProperty here would write the secret back to the DB.
        findProperty(key, scope, scopeId, environmentId)
            .ifPresent(property -> {
                property.setEnabled(enabled);

                propertyRepository.save(property);
            });
    }

    private Optional<Property> findProperty(
        String key, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId) {

        if (scopeId == null && environmentId == null) {
            return propertyRepository.findByKeyAndScope(key, scope.ordinal());
        } else if (scopeId == null) {
            return propertyRepository.findByKeyAndScopeAndEnvironment(
                key, scope.ordinal(), environmentId.intValue());
        } else if (environmentId == null) {
            return propertyRepository.findByKeyAndScopeAndScopeId(key, scope.ordinal(), scopeId);
        } else {
            return propertyRepository.findByKeyAndScopeAndScopeIdAndEnvironment(
                key, scope.ordinal(), scopeId, environmentId.intValue());
        }
    }

    private CredentialStore resolveTargetStore() {
        return credentialStores.stream()
            .filter(store -> store.getType() != CredentialStoreType.DATABASE)
            .findFirst()
            .orElseGet(() -> getStore(CredentialStoreType.DATABASE));
    }

    private CredentialStore getStore(CredentialStoreType type) {
        return credentialStores.stream()
            .filter(store -> store.getType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                ("No CredentialStore registered for type %s. Configure bytechef.credential-store.external.provider" +
                    " or migrate this row to DATABASE.").formatted(type)));
    }

    private List<Property> populateAll(List<Property> properties) {
        properties.forEach(this::populateValue);

        return properties;
    }

    private Property populateValue(Property property) {
        CredentialStore store = getStore(property.getCredentialStoreType());

        property.setValue(store.getSecret(property));

        return property;
    }

    private void storeValue(Property property, Map<String, ?> value) {
        CredentialStore target = resolveTargetStore();

        if (target.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(target.getType());
        }

        CredentialStore current = getStore(property.getCredentialStoreType());

        if (current.getType() != target.getType() && !current.isReadOnly()) {
            current.deleteSecret(property);
        }

        property.setCredentialStoreType(target.getType());
        property.setCredentialRef(null);

        target.storeSecret(property, value);
    }
}
