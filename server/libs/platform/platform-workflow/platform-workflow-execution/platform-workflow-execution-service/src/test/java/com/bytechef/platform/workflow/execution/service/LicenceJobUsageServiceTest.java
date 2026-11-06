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

package com.bytechef.platform.workflow.execution.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.workflow.execution.exception.JobLimitExceededException;
import com.bytechef.platform.workflow.execution.repository.LicenceJobUsageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
public class LicenceJobUsageServiceTest {

    private LicenceManager licenceManager;
    private ObjectProvider<LicenceManager> licenceManagerProvider;
    private LicenceJobUsageRepository licenceJobUsageRepository;
    private LicenceJobUsageService licenceJobUsageService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        licenceManager = mock(LicenceManager.class);
        licenceManagerProvider = mock(ObjectProvider.class);
        licenceJobUsageRepository = mock(LicenceJobUsageRepository.class);

        Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

        when(licenceManagerProvider.getIfAvailable()).thenReturn(licenceManager);

        licenceJobUsageService = new LicenceJobUsageService(licenceManagerProvider, licenceJobUsageRepository, clock);
    }

    @Test
    void testConsumeUnlimitedSkipsRepository() {
        when(licenceManager.getAllowedJobs()).thenReturn(-1L);

        licenceJobUsageService.consumeOrThrow();

        verifyNoInteractions(licenceJobUsageRepository);
    }

    @Test
    void testConsumeUnderLimitIncrements() {
        when(licenceManager.getAllowedJobs()).thenReturn(100L);
        when(licenceJobUsageRepository.incrementIfBelow("2026-06", 100L)).thenReturn(1);

        licenceJobUsageService.consumeOrThrow();

        verify(licenceJobUsageRepository).insertIgnore("2026-06");
        verify(licenceJobUsageRepository).incrementIfBelow("2026-06", 100L);
    }

    @Test
    void testConsumeAtLimitThrows() {
        when(licenceManager.getAllowedJobs()).thenReturn(100L);
        when(licenceJobUsageRepository.incrementIfBelow("2026-06", 100L)).thenReturn(0);

        assertThatThrownBy(() -> licenceJobUsageService.consumeOrThrow())
            .isInstanceOf(JobLimitExceededException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNoLicenceManagerBeanSkipsMetering() {
        ObjectProvider<LicenceManager> emptyProvider = mock(ObjectProvider.class);

        when(emptyProvider.getIfAvailable()).thenReturn(null);

        Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);
        LicenceJobUsageService serviceWithoutBean =
            new LicenceJobUsageService(emptyProvider, licenceJobUsageRepository, clock);

        assertThatCode(() -> serviceWithoutBean.consumeOrThrow()).doesNotThrowAnyException();

        verifyNoInteractions(licenceJobUsageRepository);
    }
}
