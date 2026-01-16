/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.service;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemotePropertyServiceClient implements PropertyService {

    @Override
    public void delete(String key, Property.Scope scope, Long scopeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Property> fetchProperty(String key, Property.Scope scope, @Nullable Long scopeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(String key, Property.Scope scope, @Nullable Long scopeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Property> getProperties(List<String> keys, Property.Scope scope, @Nullable Long scopeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(String key, Map<String, ?> value, Property.Scope scope, @Nullable Long scopeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(String key, boolean enabled, Property.Scope scope, @Nullable Long scopeID) {
        throw new UnsupportedOperationException();
    }
}
