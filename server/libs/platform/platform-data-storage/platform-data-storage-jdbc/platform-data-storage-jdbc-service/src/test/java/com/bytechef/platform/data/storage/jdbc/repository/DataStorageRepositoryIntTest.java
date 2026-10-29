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

package com.bytechef.platform.data.storage.jdbc.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.config.DataStorageIntTestConfiguration;
import com.bytechef.platform.data.storage.jdbc.domain.DataEntry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = DataStorageIntTestConfiguration.class)
public class DataStorageRepositoryIntTest {

    @Autowired
    private DataStorageRepository dataStorageRepository;

    @AfterEach
    public void afterEach() {
        dataStorageRepository.deleteAll();
    }

    @Test
    public void testDeleteAllByScopeAndScopeIdDeletesOnlyTheGivenScopeAndScopeId() {
        DataEntry deletedFirstDataEntry = dataStorageRepository.save(
            newDataEntry(DataStorageScope.WORKFLOW, "workflow-1", "first"));
        DataEntry deletedSecondDataEntry = dataStorageRepository.save(
            newDataEntry(DataStorageScope.WORKFLOW, "workflow-1", "second"));
        DataEntry otherScopeIdDataEntry = dataStorageRepository.save(
            newDataEntry(DataStorageScope.WORKFLOW, "workflow-2", "first"));
        DataEntry otherScopeDataEntry = dataStorageRepository.save(
            newDataEntry(DataStorageScope.PRINCIPAL, "workflow-1", "first"));

        dataStorageRepository.deleteAllByScopeAndScopeId(DataStorageScope.WORKFLOW.ordinal(), "workflow-1");

        List<DataEntry> remainingDataEntries = dataStorageRepository.findAll();

        assertThat(remainingDataEntries).containsExactlyInAnyOrder(otherScopeIdDataEntry, otherScopeDataEntry);
        assertThat(remainingDataEntries).doesNotContain(deletedFirstDataEntry, deletedSecondDataEntry);
    }

    private static DataEntry newDataEntry(DataStorageScope scope, String scopeId, String key) {
        return new DataEntry("component", scope, scopeId, key, "value", 0, PlatformType.AUTOMATION);
    }
}
