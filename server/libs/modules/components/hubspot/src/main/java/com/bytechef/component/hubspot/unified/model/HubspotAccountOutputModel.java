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

package com.bytechef.component.hubspot.unified.model;

import com.bytechef.component.definition.unified.crm.model.ProviderAccountOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
// CHECKSTYLE:OFF
public record HubspotAccountOutputModel(
    String id, HubspotAccountProperties properties, String createdAt, String updatedAt, boolean archived)
    implements ProviderAccountOutputModel {

    public static class HubspotAccountProperties extends HubspotAccountInputModel {

        private HubspotAccountProperties() {
        }

        public HubspotAccountProperties(
            String city, String name, String phone, String domain, String industry, String state, String country,
            String address, String numberofemployees, String zip, String hubspot_owner_id,
            HashMap<String, Object> customFields) {

            super(
                city, name, phone, domain, industry, state, country, address, numberofemployees, zip, hubspot_owner_id,
                customFields);
        }
    }
}
