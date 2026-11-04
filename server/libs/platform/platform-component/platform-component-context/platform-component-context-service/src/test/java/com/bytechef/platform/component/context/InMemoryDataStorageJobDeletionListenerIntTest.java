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

package com.bytechef.platform.component.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.coordinator.event.DeleteJobEvent;
import com.bytechef.cache.SharedCacheSupport;
import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class InMemoryDataStorageJobDeletionListenerIntTest {

    private InMemoryDataStorage inMemoryDataStorage;
    private InMemoryDataStorageJobDeletionListener inMemoryDataStorageJobDeletionListener;

    @BeforeEach
    void beforeEach() {
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        CacheConfiguration cacheConfiguration = new CacheConfiguration(
            new StandardEnvironment(), staticListableBeanFactory.getBeanProvider(SharedCacheSupport.class));

        CacheManager cacheManager = cacheConfiguration.cacheManager();

        inMemoryDataStorage = new InMemoryDataStorage(String.valueOf(UUID.randomUUID()), cacheManager);
        inMemoryDataStorageJobDeletionListener = new InMemoryDataStorageJobDeletionListener(cacheManager);
    }

    @Test
    void testEvictsCurrentExecutionValuesOfDeletedEditorJob() {
        inMemoryDataStorage.put("a", DataStorageScope.CURRENT_EXECUTION, "5", "key", "value");

        inMemoryDataStorageJobDeletionListener.onDeleteJob(
            new DeleteJobEvent(5, Map.of(MetadataConstants.EDITOR_ENVIRONMENT, true)));

        assertThat(inMemoryDataStorage.fetch("a", DataStorageScope.CURRENT_EXECUTION, "5", "key")).isEmpty();
    }

    @Test
    void testIgnoresNonEditorJobs() {
        inMemoryDataStorage.put("a", DataStorageScope.CURRENT_EXECUTION, "6", "key", "value");

        inMemoryDataStorageJobDeletionListener.onDeleteJob(new DeleteJobEvent(6, Map.of()));

        assertThat(inMemoryDataStorage.<String>fetch("a", DataStorageScope.CURRENT_EXECUTION, "6", "key"))
            .contains("value");
    }
}
