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

    private final PropertyRepository propertyRepository;

    public PropertyServiceImpl(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public void delete(String key, Property.Scope scope, Long scopeId) {
        delete(key, scope, scopeId, null);
    }

    @Override
    public void delete(String key, Property.Scope scope, Long scopeId, @Nullable Long environmentId) {
        fetchProperty(key, scope, scopeId, environmentId)
            .ifPresent(propertyRepository::delete);
    }

    @Override
    public Optional<Property> fetchProperty(String key, Property.Scope scope, @Nullable Long scopeId) {
        return fetchProperty(key, scope, scopeId, null);
    }

    @Override
    public Optional<Property> fetchProperty(
        String key, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId) {

        if (scopeId == null && environmentId == null) {
            return propertyRepository.findByKeyAndScope(key, scope.ordinal());
        } else if (scopeId == null) {
            return propertyRepository.findByKeyAndScopeAndEnvironment(key, scope.ordinal(), environmentId.intValue());
        } else if (environmentId == null) {
            return propertyRepository.findByKeyAndScopeAndScopeId(key, scope.ordinal(), scopeId);
        } else {
            return propertyRepository.findByKeyAndScopeAndScopeIdAndEnvironment(
                key, scope.ordinal(), scopeId, environmentId.intValue());
        }
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
            return propertyRepository.findAllByKeyInAndScope(keys, scope.ordinal());
        } else if (scopeId == null) {
            return propertyRepository.findAllByKeyInAndScopeAndEnvironment(
                keys, scope.ordinal(), environmentId.intValue());
        } else if (environmentId == null) {
            return propertyRepository.findAllByKeyInAndScopeAndScopeId(keys, scope.ordinal(), scopeId);
        } else {
            return propertyRepository.findAllByKeyInAndScopeAndScopeIdAndEnvironment(
                keys, scope.ordinal(), scopeId, environmentId.intValue());
        }
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
                property.setValue(value);

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
                property.setValue(value);

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

        fetchProperty(key, scope, scopeId, environmentId)
            .ifPresent(property -> {
                property.setEnabled(enabled);

                propertyRepository.save(property);
            });
    }
}
