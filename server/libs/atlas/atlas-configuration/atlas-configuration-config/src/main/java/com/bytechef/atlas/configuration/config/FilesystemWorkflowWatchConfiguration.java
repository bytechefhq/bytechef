
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.configuration.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "workflow.repository.filesystem.enabled", havingValue = "true")
public class FilesystemWorkflowWatchConfiguration {

    private final List<String> directories = new ArrayList<>();
    private final WorkflowService workflowService;
    private final WatchService watchService;

    public FilesystemWorkflowWatchConfiguration(
        @Value("${bytechef.workflow.repository.filesystem.location-pattern}") String locationPattern,
        TaskExecutor taskExecutor, WorkflowService workflowService) throws IOException {

        this.workflowService = workflowService;

        FileSystem fileSystem = FileSystems.getDefault();

        this.watchService = fileSystem.newWatchService();

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resolver.getResources("file:" + locationPattern);

        for (Resource resource : resources) {
            File file = resource.getFile();

            String directory = file.getParent();

            if (!directories.contains(directory)) {
                directories.add(directory);

                Path path = Paths.get(directory);

                path.register(
                    watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            }
        }

        taskExecutor.execute(this::run);
    }

    private void run() {
        WatchKey key;

        try {
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");
                }

                key.reset();
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
