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

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhook;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.repository.DataTableWebhookRepository;
import com.bytechef.platform.data.table.domain.DataTableRef;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link DataTableWebhookService} that provides functionality for managing webhooks associated
 * with data tables. This service interacts with the data table and webhook repositories to facilitate the addition,
 * retrieval, and removal of webhooks.
 *
 * <p>
 * Registration and delivery both key on the ref's registry id, which is what makes them meet: whatever table a ref
 * addresses at registration time is the table an event on that same ref is delivered to.
 *
 * @author Ivica Cardic
 */
@Service
public class DataTableWebhookServiceImpl implements DataTableWebhookService {

    private final DataTableWebhookRepository webhookRepository;

    @SuppressFBWarnings("EI")
    public DataTableWebhookServiceImpl(DataTableWebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Override
    public long addWebhook(DataTableRef dataTableRef, String url, DataTableWebhookType type) {
        DataTableWebhook dataTableWebhook = new DataTableWebhook();

        dataTableWebhook.setDataTableId(dataTableRef.dataTableId());
        dataTableWebhook.setType(type);
        dataTableWebhook.setUrl(url);
        dataTableWebhook.setEnvironment(Environment.values()[(int) dataTableRef.environmentId()]);

        DataTableWebhook savedDataTableHook = webhookRepository.save(dataTableWebhook);

        return savedDataTableHook.getId();
    }

    @Override
    public List<Webhook> listWebhooks(DataTableRef dataTableRef) {
        return findWebhooks(dataTableRef.dataTableId(), dataTableRef.environmentId());
    }

    @Override
    public List<Webhook> listWebhooks(long dataTableId, long environmentId) {
        return findWebhooks(dataTableId, environmentId);
    }

    @Override
    public void removeWebhook(long id) {
        webhookRepository.deleteById(id);
    }

    private List<Webhook> findWebhooks(long dataTableId, long environmentId) {
        Environment environment = Environment.values()[(int) environmentId];

        List<DataTableWebhook> dataTableWebhooks =
            webhookRepository.findByDataTableIdAndEnvironment(dataTableId, environment.ordinal());

        List<Webhook> webhooks = new ArrayList<>(dataTableWebhooks.size());

        for (DataTableWebhook dataTableWebhook : dataTableWebhooks) {
            webhooks.add(new Webhook(
                dataTableWebhook.getId(), dataTableWebhook.getDataTableId(), dataTableWebhook.getUrl(),
                dataTableWebhook.getType(), dataTableWebhook.getEnvironment()
                    .ordinal()));
        }

        return webhooks;
    }
}
