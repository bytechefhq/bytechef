/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.remote.client.service;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
    public long addWebhook(String baseName, String url, DataTableWebhookType type, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Webhook> listWebhooks(String baseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeWebhook(long id) {
        throw new UnsupportedOperationException();
    }
}
