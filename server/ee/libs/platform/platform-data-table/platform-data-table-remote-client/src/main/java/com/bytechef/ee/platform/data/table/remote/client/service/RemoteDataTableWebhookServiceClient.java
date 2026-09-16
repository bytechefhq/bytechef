/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.DataTableRef;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteDataTableWebhookServiceClient implements DataTableWebhookService {

    @Override
    public long addWebhook(DataTableRef dataTableRef, String url, DataTableWebhookType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Webhook> listWebhooks(DataTableRef dataTableRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Webhook> listWebhooks(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeWebhook(long id) {
        throw new UnsupportedOperationException();
    }
}
