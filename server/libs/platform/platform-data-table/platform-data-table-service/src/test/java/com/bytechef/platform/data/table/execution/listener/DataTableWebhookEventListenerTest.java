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

package com.bytechef.platform.data.table.execution.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ivica Cardic
 */
@SpringJUnitConfig(DataTableWebhookEventListenerTest.DataTableWebhookEventListenerTestConfiguration.class)
class DataTableWebhookEventListenerTest {

    private static final DataTableRef DATA_TABLE_REF = new DataTableRef(7L, 0);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private DataTableWebhookService dataTableWebhookService;

    @BeforeEach
    void beforeEach() {
        reset(dataTableWebhookService);
    }

    @AfterEach
    void afterEach() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }

        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void testEventOfARolledBackTransactionIsNotDelivered() {
        beginTransaction();

        applicationEventPublisher.publishEvent(createDataTableWebhookEvent());

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verifyNoInteractions(dataTableWebhookService);
    }

    @Test
    void testEventOfACommittedTransactionIsDeliveredAfterTheCommit() {
        beginTransaction();

        applicationEventPublisher.publishEvent(createDataTableWebhookEvent());

        verifyNoInteractions(dataTableWebhookService);

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(dataTableWebhookService).listWebhooks(DATA_TABLE_REF);
    }

    @Test
    void testEventPublishedOutsideATransactionIsDelivered() {
        applicationEventPublisher.publishEvent(createDataTableWebhookEvent());

        verify(dataTableWebhookService).listWebhooks(DATA_TABLE_REF);
    }

    private static void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private static void completeTransaction(int status) {
        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            for (TransactionSynchronization transactionSynchronization : transactionSynchronizations) {
                transactionSynchronization.afterCommit();
            }
        }

        for (TransactionSynchronization transactionSynchronization : transactionSynchronizations) {
            transactionSynchronization.afterCompletion(status);
        }
    }

    private static DataTableWebhookEvent createDataTableWebhookEvent() {
        return new DataTableWebhookEvent(
            DATA_TABLE_REF, DataTableWebhookType.RECORD_CREATED, Map.of("id", 1L, "values", Map.of()));
    }

    @EnableTransactionManagement
    static class DataTableWebhookEventListenerTestConfiguration {

        @Bean
        DataTableWebhookService dataTableWebhookService() {
            return mock(DataTableWebhookService.class);
        }

        @Bean
        DataTableWebhookEventListener dataTableWebhookEventListener(DataTableWebhookService dataTableWebhookService) {
            return new DataTableWebhookEventListener(
                mock(DataTableService.class), dataTableWebhookService, mock(RestTemplate.class));
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }
    }
}
