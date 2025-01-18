/*
 * Copyright 2023-present ByteChef Inc.
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
import org.springframework.lang.NonNull;
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
    public void delete(@NonNull String key) {
        propertyRepository.findByKey(key)
            .ifPresent(propertyRepository::delete);
    }

    @Override
    public Optional<Property> fetchProperty(@NonNull String key) {
        return propertyRepository.findByKey(key);
    }

    @Override
    public Property getProperty(@NonNull String key) {
        return OptionalUtils.get(propertyRepository.findByKey(key));
    }

    @Override
    public List<Property> getProperties(@NonNull List<String> keys) {
        return propertyRepository.findAllByKeyIn(keys);
    }

    @Override
    public void save(@NonNull String key, @NonNull Map<String, ?> value) {
        propertyRepository.findByKey(key)
            .ifPresentOrElse(properties -> {
                properties.setValue(value);

                propertyRepository.save(properties);
            }, () -> {
                Property property = new Property();

                property.setEnabled(true);
                property.setKey(key);
                property.setValue(value);

                propertyRepository.save(property);
            });
    }

    @Override
    public void update(@NonNull String key, boolean enabled) {
        propertyRepository.findByKey(key)
            .ifPresent(properties -> {
                properties.setEnabled(enabled);

                propertyRepository.save(properties);
            });
    }
}
