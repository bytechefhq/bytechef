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

package com.bytechef.automation.data.table.configuration.service;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import java.util.List;

/**
 * Service for managing webhooks associated with data tables. Webhooks enable external systems to receive notifications
 * about specific events occurring within a data table.
 *
 * @author Ivica Cardic
 */
public interface DataTableWebhookService {

    /**
     * Adds a new webhook for the specified data table, enabling notifications for external systems when certain events
     * occur on the data table.
     *
     * @param baseName      The name of the data table to which the webhook will be associated.
     * @param url           The URL that will receive the webhook notifications.
     * @param type          The type of event for which the webhook should trigger (e.g., record_created,
     *                      record_deleted, record_updated).
     * @param environmentId The environment ID in which the webhook should be triggered.
     * @return The unique identifier of the newly created webhook.
     */
    long addWebhook(String baseName, String url, DataTableWebhookType type, long environmentId);

    /**
     * Retrieves a list of webhooks associated with the specified data table. Webhooks provide external systems with
     * notifications about certain events occurring within the data table.
     *
     * @param baseName      The name of the data table for which the webhooks should be listed.
     * @param environmentId The environment ID for which to list webhooks.
     * @return A list of webhooks associated with the specified data table.
     */
    List<Webhook> listWebhooks(String baseName, long environmentId);

    /**
     * Removes an existing webhook identified by its unique identifier. This action stops notifications from being sent
     * to the associated URL.
     *
     * @param id The unique identifier of the webhook to be removed.
     */
    void removeWebhook(long id);

    record Webhook(long id, long dataTableId, String url, DataTableWebhookType type, long environmentId) {
    }
}
