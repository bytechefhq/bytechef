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

import com.bytechef.automation.data.table.configuration.domain.DataTable;
import com.bytechef.automation.data.table.configuration.domain.DataTableWebhook;
import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.repository.DataTableRepository;
import com.bytechef.automation.data.table.configuration.repository.DataTableWebhookRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link DataTableWebhookService} that provides functionality for managing webhooks associated
 * with data tables. This service interacts with the data table and webhook repositories to facilitate the addition,
 * retrieval, and removal of webhooks.
 *
 * @author Ivica Cardic
 */
@Service
public class DataTableWebhookServiceImpl implements DataTableWebhookService {

    private final DataTableRepository dataTableRepository;
    private final DataTableWebhookRepository webhookRepository;

    @SuppressFBWarnings("EI")
    public DataTableWebhookServiceImpl(
        DataTableRepository dataTableRepository, DataTableWebhookRepository webhookRepository) {

        this.dataTableRepository = dataTableRepository;
        this.webhookRepository = webhookRepository;
    }

    @Override
    public long addWebhook(String baseName, String url, DataTableWebhookType type, long environmentId) {
        Optional<DataTable> dataTableOptional = dataTableRepository.findByName(baseName);

        DataTable table = dataTableOptional.orElseThrow(
            () -> new IllegalArgumentException("Table not found: " + baseName));

        DataTableWebhook dataTableWebhook = new DataTableWebhook();

        dataTableWebhook.setDataTableId(table.getId());
        dataTableWebhook.setType(type);
        dataTableWebhook.setUrl(url);
        dataTableWebhook.setEnvironment(Environment.values()[(int) environmentId]);

        DataTableWebhook savedDataTableHook = webhookRepository.save(dataTableWebhook);

        return savedDataTableHook.getId();
    }

    @Override
    public List<Webhook> listWebhooks(String baseName, long environmentId) {
        Optional<DataTable> dataTableOptional = dataTableRepository.findByName(baseName);

        if (dataTableOptional.isEmpty()) {
            return List.of();
        }

        DataTable dataTable = dataTableOptional.get();

        Environment environment = Environment.values()[(int) environmentId];

        List<DataTableWebhook> dataTableWebhooks = webhookRepository.findByDataTableIdAndEnvironment(
            dataTable.getId(), environment.ordinal());

        List<Webhook> webhooks = new ArrayList<>(dataTableWebhooks.size());

        for (DataTableWebhook dataTableWebhook : dataTableWebhooks) {
            webhooks.add(new Webhook(
                dataTableWebhook.getId(), dataTableWebhook.getDataTableId(), dataTableWebhook.getUrl(),
                dataTableWebhook.getType(), dataTableWebhook.getEnvironment()
                    .ordinal()));
        }

        return webhooks;
    }

    @Override
    public void removeWebhook(long id) {
        webhookRepository.deleteById(id);
    }
}
