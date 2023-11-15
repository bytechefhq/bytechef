/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.configuration.filesystem;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.EncodingUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates modified workflows for the existing projects on the filesystem.
 *
 * @author Ivica Cardic
 */
public class FilesystemWorkflowWatcher {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemWorkflowWatcher.class);

    private final String locationPattern;
    private final List<String> dirNames = new ArrayList<>();
    private final WorkflowService workflowService;
    private final Map<WatchKey, Path> watchKeyPathMap = new HashMap<>();
    private final TaskExecutor taskExecutor;
    private final WatchService watchService;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public FilesystemWorkflowWatcher(
        String locationPattern, TaskExecutor taskExecutor, WorkflowService workflowService) throws IOException {

        this.locationPattern = locationPattern;
        this.taskExecutor = taskExecutor;
        this.workflowService = workflowService;

        FileSystem fileSystem = FileSystems.getDefault();

        this.watchService = fileSystem.newWatchService();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReadyEvent() throws IOException {
        registerWatch(locationPattern);
        taskExecutor.execute(this::run);
    }

    private void registerWatch(String locationPattern) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resolver.getResources("file:" + locationPattern);

        for (Resource resource : resources) {
            registerWatch(resource);
        }
    }

    private void registerWatch(Resource resource) throws IOException {
        File file = resource.getFile();

        String dirName = file.getParent();

        if (!dirNames.contains(dirName)) {
            dirNames.add(dirName);

            Path path = Paths.get(dirName);

            watchKeyPathMap.put(path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY), path);
        }
    }

    private void run() {
        WatchKey key;

        try {
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    updateWorkflow(event, key);
                }

                key.reset();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void updateWorkflow(WatchEvent<?> event, WatchKey key) {
        Path path = watchKeyPathMap.get(key);

        Object context = event.context();

        String workflowFilename = context.toString();

        if (logger.isDebugEnabled()) {
            logger.debug("Event kind: {}, file affected: {}", event.kind(), path.resolve(workflowFilename));
        }

        String workflowId = EncodingUtils.encodeBase64ToString(
            workflowFilename.substring(0, workflowFilename.lastIndexOf('.')));

        workflowService.refreshCache(workflowId);
    }
}
