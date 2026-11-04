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

package com.bytechef.platform.data.storage.jdbc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter.ScopeData;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.config.DataStorageIntTestConfiguration;
import com.bytechef.platform.data.storage.jdbc.config.JdbcDataStorageConfiguration;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Import({
    JdbcDataStorageConfiguration.class, PostgreSQLContainerConfiguration.class
})
@SpringBootTest(
    classes = DataStorageIntTestConfiguration.class,
    properties = {
        "bytechef.data-storage.provider=jdbc", "spring.datasource.hikari.auto-commit=false"
    })
class JdbcDataStorageServiceAfterCommitIntTest {

    private static final String COMPONENT_NAME = "dataStorage";
    private static final long ENVIRONMENT_ID = 0;

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private DataStorageRepository dataStorageRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void afterEach() {
        dataStorageRepository.deleteAll();
    }

    @Test
    void testDeleteScopeDataAfterCommitPersistsWithoutAutoCommit() {
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.PRINCIPAL, "5", "key", "value", ENVIRONMENT_ID, PlatformType.AUTOMATION);
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, "6", "key", "value", ENVIRONMENT_ID, PlatformType.AUTOMATION);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> DataStorageScopeDeleter.deleteAfterCommit(
            dataStorage, transactionManager,
            List.of(
                new ScopeData(DataStorageScope.PRINCIPAL, "5", PlatformType.AUTOMATION),
                new ScopeData(DataStorageScope.WORKFLOW, "6", PlatformType.AUTOMATION))));

        assertThat(dataStorageRepository.count()).isZero();
    }

    @Test
    void testDeleteScopeDataInARolledBackTransactionKeepsTheValues() {
        dataStorage.put(
            COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION, "7", "key", "value", ENVIRONMENT_ID,
            PlatformType.AUTOMATION);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            dataStorage.deleteScopeData(DataStorageScope.CURRENT_EXECUTION, "7", PlatformType.AUTOMATION);

            transactionStatus.setRollbackOnly();
        });

        assertThat(dataStorageRepository.count()).isEqualTo(1);
    }
}
