/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.client.service;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemotePropertyServiceClient implements PropertyService {

    @Override
    public void delete(@NonNull String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Property> fetchProperty(@NonNull String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(@NonNull String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Property> getProperties(@NonNull List<String> keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(@NonNull String key, @NonNull Map<String, ?> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(@NonNull String key, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}
