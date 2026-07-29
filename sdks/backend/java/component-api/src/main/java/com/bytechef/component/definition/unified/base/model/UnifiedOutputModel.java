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

package com.bytechef.component.definition.unified.base.model;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Marker interface for unified output models.
 *
 * @author Ivica Cardic
 */
public interface UnifiedOutputModel extends UnifiedInputModel {

    /**
     * Returns the ByteChef-assigned unified identifier of this resource.
     *
     * @return the unified identifier
     */
    String getId();

    /**
     * Returns the identifier of this resource as assigned by the remote provider.
     *
     * @return the provider-assigned identifier
     */
    String getRemoteId();

    /**
     * Returns the raw, provider-specific representation of this resource as returned by the remote API, keyed by field
     * name.
     *
     * @return the original remote data as a map of field name to value
     */
    Map<String, ?> getRemoteData();

    /**
     * Returns the timestamp at which this resource was created on the remote provider.
     *
     * @return the creation date
     */
    OffsetDateTime getCreatedDate();

    /**
     * Returns the timestamp at which this resource was last modified on the remote provider.
     *
     * @return the last modification date
     */
    OffsetDateTime getLastModifiedDate();
}
