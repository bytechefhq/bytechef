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
import com.bytechef.platform.configuration.domain.Property.Scope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface PropertyService {

    void delete(String key, Scope scope, @Nullable Long scopeId);

    Optional<Property> fetchProperty(String key, Scope scope, @Nullable Long scopeId);

    Property getProperty(String key, Scope scope, @Nullable Long scopeId);

    List<Property> getProperties(List<String> keys, Scope scope, @Nullable Long scopeId);

    void save(String key, Map<String, ?> value, Scope scope, @Nullable Long scopeId);

    void update(String key, boolean enabled, Scope scope, @Nullable Long scopeId);
}
