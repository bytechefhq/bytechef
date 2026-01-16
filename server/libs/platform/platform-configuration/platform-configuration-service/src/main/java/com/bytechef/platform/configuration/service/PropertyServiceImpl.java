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

import com.bytechef.commons.util.OptionalUtils;
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
        if (scopeId == null) {
            propertyRepository.findByKeyAndScope(key, scope.ordinal())
                .ifPresent(propertyRepository::delete);
        } else {
            propertyRepository.findByKeyAndScopeAndScopeId(key, scope.ordinal(), scopeId)
                .ifPresent(propertyRepository::delete);
        }
    }

    @Override
    public Optional<Property> fetchProperty(String key, Property.Scope scope, @Nullable Long scopeId) {
        if (scopeId == null) {
            return propertyRepository.findByKeyAndScope(key, scope.ordinal());
        } else {
            return propertyRepository.findByKeyAndScopeAndScopeId(key, scope.ordinal(), scopeId);
        }
    }

    @Override
    public Property getProperty(String key, Property.Scope scope, @Nullable Long scopeId) {
        return OptionalUtils.get(fetchProperty(key, scope, scopeId));
    }

    @Override
    public List<Property> getProperties(List<String> keys, Property.Scope scope, @Nullable Long scopeId) {
        if (scopeId == null) {
            return propertyRepository.findAllByKeyInAndScope(keys, scope.ordinal());
        } else {
            return propertyRepository.findAllByKeyInAndScopeAndScopeId(keys, scope.ordinal(), scopeId);
        }
    }

    @Override
    public void save(String key, Map<String, ?> value, Property.Scope scope, @Nullable Long scopeId) {
        fetchProperty(key, scope, scopeId)
            .ifPresentOrElse(property -> {
                property.setValue(value);

                propertyRepository.save(property);
            }, () -> {
                Property property = new Property();

                property.setEnabled(true);
                property.setKey(key);
                property.setScope(scope);
                property.setScopeId(scopeId);
                property.setValue(value);

                propertyRepository.save(property);
            });
    }

    @Override
    public void update(String key, boolean enabled, Property.Scope scope, @Nullable Long scopeId) {
        fetchProperty(key, scope, scopeId)
            .ifPresent(properties -> {
                properties.setEnabled(enabled);

                propertyRepository.save(properties);
            });
    }
}
