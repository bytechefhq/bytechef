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

package com.bytechef.platform.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class KnowledgeBaseStorageServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private KnowledgeBaseStorageServiceImpl createService(long limit, long used) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getKnowledgeBase()
            .setMaxSizeBytes(limit);

        when(jdbcTemplate.queryForObject(eq(KnowledgeBaseStorageServiceImpl.USAGE_SQL), eq(Long.class)))
            .thenReturn(used);

        return new KnowledgeBaseStorageServiceImpl(applicationProperties, jdbcTemplate);
    }

    @Test
    void testGetUsageComputesPercentage() {
        KnowledgeBaseStorageUsage usage = createService(1_000L, 800L).getUsage();

        assertThat(usage.percentage()).isEqualTo(80.0);
        assertThat(usage.unlimited()).isFalse();
    }

    @Test
    void testCheckWithinLimitThrowsWhenIncomingExceeds() {
        assertThatThrownBy(() -> createService(1_000L, 900L).checkWithinLimit(200))
            .isInstanceOf(KnowledgeBaseStorageLimitExceededException.class);
    }

    @Test
    void testCheckWithinLimitPassesAtExactBoundary() {
        createService(1_000L, 900L).checkWithinLimit(100);
    }

    @Test
    void testCheckWithinLimitPassesWhenUnlimited() {
        createService(0L, 999_999L).checkWithinLimit(1_000_000);
    }
}
