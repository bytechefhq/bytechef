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

package com.bytechef.platform.data.storage;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
public final class DataStorageScopeDeleter {

    private static final Logger log = LoggerFactory.getLogger(DataStorageScopeDeleter.class);

    private DataStorageScopeDeleter() {
    }

    public static void deleteAfterCommit(
        @Nullable DataStorage dataStorage, PlatformTransactionManager transactionManager,
        List<ScopeData> scopeDataList) {

        if (dataStorage == null || scopeDataList.isEmpty()) {
            return;
        }

        List<ScopeData> scopeDataListCopy = List.copyOf(scopeDataList);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        if (!TransactionSynchronizationManager.isSynchronizationActive() ||
            !TransactionSynchronizationManager.isActualTransactionActive()) {

            delete(dataStorage, transactionTemplate, scopeDataListCopy);

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                delete(dataStorage, transactionTemplate, scopeDataListCopy);
            }
        });
    }

    private static void delete(
        DataStorage dataStorage, TransactionTemplate transactionTemplate, List<ScopeData> scopeDataList) {

        for (ScopeData scopeData : scopeDataList) {
            try {
                transactionTemplate.executeWithoutResult(transactionStatus -> dataStorage.deleteScopeData(
                    scopeData.scope(), scopeData.scopeId(), scopeData.type()));
            } catch (UnsupportedOperationException exception) {
                log.debug(
                    "Scope-wide Data Storage delete is unsupported by this provider; skipping {}", scopeData);
            } catch (RuntimeException exception) {
                log.warn("Failed to delete Data Storage values of {}", scopeData, exception);
            }
        }
    }

    public record ScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
    }
}
