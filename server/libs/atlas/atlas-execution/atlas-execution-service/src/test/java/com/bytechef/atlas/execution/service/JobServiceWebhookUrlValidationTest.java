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

package com.bytechef.atlas.execution.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.commons.util.UrlValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class JobServiceWebhookUrlValidationTest {

    private final JobRepository jobRepository = Mockito.mock(JobRepository.class);

    @Test
    void testRejectsPrivateWebhookUrlWhenEnabled() {
        JobServiceImpl jobService = new JobServiceImpl(jobRepository, true, Set.of());

        assertThatThrownBy(() -> jobService.validateWebhookUrl("http://169.254.169.254/"))
            .isInstanceOf(UrlValidationException.class);
    }

    @Test
    void testAllowsPublicWebhookUrlWhenEnabled() {
        JobServiceImpl jobService = new JobServiceImpl(jobRepository, true, Set.of());

        assertThatCode(() -> jobService.validateWebhookUrl("https://1.1.1.1/hook")).doesNotThrowAnyException();
    }

    @Test
    void testSkipsValidationWhenDisabled() {
        JobServiceImpl jobService = new JobServiceImpl(jobRepository, false, Set.of());

        assertThatCode(() -> jobService.validateWebhookUrl("http://169.254.169.254/")).doesNotThrowAnyException();
    }
}
