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

import com.bytechef.component.definition.unified.crm.model.ProviderContactOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
// CHECKSTYLE:OFF
public record HubspotContactOutputModel(
    String id, HubspotContactProperties properties, String createdAt, String updatedAt, String archived)
    implements ProviderContactOutputModel {

    public static class HubspotContactProperties extends HubspotContactInputModel {

        private HubspotContactProperties() {
        }

        public HubspotContactProperties(
            String firstname, String lastname, String email, String phone, String city, String country, String zip,
            String state, String addres, String mobilephone, String hubspot_owner_id, String associatedcompanyid,
            String fax, String jobtitle, Map<String, Object> customFields) {

            super(
                firstname, lastname, email, phone, city, country, zip, state, addres, mobilephone, hubspot_owner_id,
                associatedcompanyid, fax, jobtitle, customFields);
        }
    }
}
