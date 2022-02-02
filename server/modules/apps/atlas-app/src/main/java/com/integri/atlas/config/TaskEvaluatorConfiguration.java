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

package com.integri.atlas.config;

import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.core.task.evaluator.spel.TempDir;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.spel.StoreFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Ivica Cardic
 */
@Configuration
public class TaskEvaluatorConfiguration {

    @Bean
    TaskEvaluator taskEvaluator(Environment environment, FileStorageService fileStorageService) {
        return SpelTaskEvaluator
            .builder()
            .environment(environment)
            .methodExecutor("storeFile", new StoreFile(fileStorageService))
            .methodExecutor("tempDir", new TempDir())
            .build();
    }
}
