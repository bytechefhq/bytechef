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

package com.bytechef.component.definition.unified.ticketing.model;

import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Normalized, provider-agnostic output model for a ticketing contact. It extends {@link ContactUnifiedInputModel} with
 * the read-only fields returned by a provider (identifiers, timestamps and the raw provider payload), and is produced
 * by a {@code ProviderContactMapper} from a provider-native output model.
 *
 * @author Ivica Cardic
 */
public class ContactUnifiedOutputModel extends ContactUnifiedInputModel implements UnifiedOutputModel {

    /**
     * Returns the ByteChef unified identifier of the contact.
     *
     * @return the unified contact identifier
     */
    @Override
    public String getId() {
        return "";
    }

    /**
     * Returns the contact identifier as assigned by the remote provider.
     *
     * @return the provider-assigned contact identifier
     */
    @Override
    public String getRemoteId() {
        return "";
    }

    /**
     * Returns the raw, unmodified data as returned by the remote provider for this contact.
     *
     * @return a map of the provider's raw contact data; empty when no remote data is retained
     */
    @Override
    public Map<String, ?> getRemoteData() {
        return Map.of();
    }

    /**
     * Returns the date and time at which the contact was created at the provider.
     *
     * @return the contact creation timestamp, or {@code null} when unknown
     */
    @Override
    public OffsetDateTime getCreatedDate() {
        return null;
    }

    /**
     * Returns the date and time at which the contact was last modified at the provider.
     *
     * @return the contact last-modified timestamp, or {@code null} when unknown
     */
    @Override
    public OffsetDateTime getLastModifiedDate() {
        return null;
    }
}
