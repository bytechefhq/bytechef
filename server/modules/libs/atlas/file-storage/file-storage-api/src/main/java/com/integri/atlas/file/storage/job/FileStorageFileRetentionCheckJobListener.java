/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.file.storage.job;

import com.integri.atlas.file.storage.FileStorageService;
import java.util.concurrent.TimeUnit;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.spring.annotations.Recurring;

/**
 * @author Ivica Cardic
 */
public class FileStorageFileRetentionCheckJobListener {

    private final FileStorageService fileStorageService;

    public FileStorageFileRetentionCheckJobListener(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Recurring(id = "file-retention-check-job", cron = "*/15 * * * *")
    @Job(name = "File retention check job")
    public void onRecurringInterval() {
        fileStorageService.deleteFiles(TimeUnit.HOURS.toMillis(6));
    }
}
