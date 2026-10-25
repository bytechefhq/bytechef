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

package com.bytechef.platform.data.table.configuration.service;

import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import java.util.List;

/**
 * Service for managing webhooks associated with data tables. Webhooks enable external systems to receive notifications
 * about specific events occurring within a data table.
 *
 * <p>
 * A registration binds to one data table, so registration and delivery both key on the table's registry row rather than
 * on its base name.
 *
 * @author Ivica Cardic
 */
public interface DataTableWebhookService {

    /**
     * Adds a new webhook for the table {@code dataTableRef} addresses, enabling notifications for external systems when
     * certain events occur on it.
     *
     * <p>
     * Takes the resolved table rather than a base name, so a registration lands on the same table the registering run
     * reads and writes, and the environment comes out of the ref for the same reason.
     *
     * @param dataTableRef The table the webhook is registered against, as resolution settled it.
     * @param url          The URL that will receive the webhook notifications.
     * @param type         The type of event for which the webhook should trigger (e.g., record_created, record_deleted,
     *                     record_updated).
     * @return The unique identifier of the newly created webhook.
     */
    long addWebhook(DataTableRef dataTableRef, String url, DataTableWebhookType type);

    /**
     * The registrations of the table {@code dataTableRef} addresses, in that ref's environment.
     *
     * <p>
     * Resolving the ref back to its registry row is what makes delivery meet registration: {@link #addWebhook} resolves
     * the identical way.
     *
     * @param dataTableRef The table whose webhooks should be listed.
     * @return Its registrations, or an empty list when the ref addresses no registry row.
     */
    List<Webhook> listWebhooks(DataTableRef dataTableRef);

    /**
     * Every registration of one registry row in one environment. The management listing -- the console shows a table's
     * registrations, and a console caller is already authorized against the table.
     *
     * @param dataTableId   The registry id of the data table.
     * @param environmentId The environment ID for which to list webhooks.
     * @return A list of webhooks associated with the specified data table.
     */
    List<Webhook> listWebhooks(long dataTableId, long environmentId);

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
