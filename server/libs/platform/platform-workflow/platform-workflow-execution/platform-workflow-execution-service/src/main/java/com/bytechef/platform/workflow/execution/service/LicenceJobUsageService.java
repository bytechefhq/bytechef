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

import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.workflow.execution.exception.JobLimitExceededException;
import com.bytechef.platform.workflow.execution.repository.LicenceJobUsageRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
public class LicenceJobUsageService {

    private final Clock clock;
    private final LicenceJobUsageRepository licenceJobUsageRepository;
    private final ObjectProvider<LicenceManager> licenceManagerProvider;

    @SuppressFBWarnings("EI2")
    public LicenceJobUsageService(
        ObjectProvider<LicenceManager> licenceManagerProvider, LicenceJobUsageRepository licenceJobUsageRepository,
        Clock clock) {

        this.licenceManagerProvider = licenceManagerProvider;
        this.licenceJobUsageRepository = licenceJobUsageRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consumeOrThrow() {
        LicenceManager licenceManager = licenceManagerProvider.getIfAvailable();

        if (licenceManager == null) {
            return;
        }

        long allowed = licenceManager.getAllowedJobs();

        if (allowed < 0) {
            return;
        }

        String yearMonth = YearMonth.now(clock)
            .toString();

        licenceJobUsageRepository.insertIgnore(yearMonth);

        int updated = licenceJobUsageRepository.incrementIfBelow(yearMonth, allowed);

        if (updated == 0) {
            throw new JobLimitExceededException(allowed);
        }
    }

    @Transactional(readOnly = true)
    public long currentMonthUsage() {
        LicenceManager licenceManager = licenceManagerProvider.getIfAvailable();

        if (licenceManager == null) {
            return 0;
        }

        Long count = licenceJobUsageRepository.findCount(YearMonth.now(clock)
            .toString());

        return count == null ? 0 : count;
    }
}
