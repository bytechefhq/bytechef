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

package com.bytechef.platform.workflow.execution.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class LicenceJobUsageRepositoryIntTest {

    private static final String YEAR_MONTH = "2026-06";

    @Autowired
    private LicenceJobUsageRepository licenceJobUsageRepository;

    @AfterEach
    void afterEach() {
        licenceJobUsageRepository.deleteAll();
    }

    @Test
    void testFindCountReturnsNullWithoutARow() {
        assertThat(licenceJobUsageRepository.findCount(YEAR_MONTH)).isNull();
    }

    @Test
    void testInsertIgnoreCreatesAZeroRowOnlyOnce() {
        licenceJobUsageRepository.insertIgnore(YEAR_MONTH);

        assertThat(licenceJobUsageRepository.incrementIfBelow(YEAR_MONTH, 5)).isEqualTo(1);

        licenceJobUsageRepository.insertIgnore(YEAR_MONTH);

        assertThat(licenceJobUsageRepository.findCount(YEAR_MONTH)).isEqualTo(1L);
    }

    @Test
    void testIncrementIfBelowStopsAtTheAllowedLimit() {
        licenceJobUsageRepository.insertIgnore(YEAR_MONTH);

        assertThat(licenceJobUsageRepository.incrementIfBelow(YEAR_MONTH, 2)).isEqualTo(1);
        assertThat(licenceJobUsageRepository.incrementIfBelow(YEAR_MONTH, 2)).isEqualTo(1);
        assertThat(licenceJobUsageRepository.incrementIfBelow(YEAR_MONTH, 2)).isZero();

        assertThat(licenceJobUsageRepository.findCount(YEAR_MONTH)).isEqualTo(2L);
    }
}
