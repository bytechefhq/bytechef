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

package com.bytechef.automation.datasync.event;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.listener.ProjectContentContributor;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.facade.DataSyncFacade;
import com.bytechef.automation.datasync.service.DataSyncService;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Carries a project's data syncs through project duplicate, export/import and git sync. Each data sync travels as one
 * {@code data-syncs/<data-sync-name>.json} file holding {@link DataSyncFacade#exportDataSync(long)}'s document, which
 * never carries a connection.
 *
 * <p>
 * Duplicating copies the data syncs in full through {@link DataSyncFacade#copyProjectDataSyncs}, connections included,
 * since the copy stays in the same workspace. Import goes through {@link DataSyncFacade#importDataSync}, which creates
 * every element without a connection. A git pull updates the project data sync a file names — matched by name, then by
 * title — through {@link DataSyncFacade#updateDataSyncFromExport}, and imports a file no data sync matches; data syncs
 * without a file are left alone, as workflows missing from the repository are.
 * </p>
 *
 * <p>
 * {@code @Lazy} because {@code ProjectFacadeImpl} takes its contributors by constructor while
 * {@code DataSyncFacadeImpl} depends on the project services, which would otherwise form a construction cycle.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class DataSyncProjectContentContributor implements ProjectContentContributor {

    static final String CONTENT_DIRECTORY = "data-syncs/";

    private static final String FILE_EXTENSION = ".json";

    private final DataSyncFacade dataSyncFacade;
    private final DataSyncService dataSyncService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DataSyncProjectContentContributor(
        @Lazy DataSyncFacade dataSyncFacade, @Lazy DataSyncService dataSyncService,
        @Lazy ProjectService projectService) {

        this.dataSyncFacade = dataSyncFacade;
        this.dataSyncService = dataSyncService;
        this.projectService = projectService;
    }

    @Override
    public String getContentDirectory() {
        return CONTENT_DIRECTORY;
    }

    @Override
    public void onProjectDuplicated(long sourceProjectId, long duplicateProjectId) {
        if (dataSyncService.getProjectDataSyncs(sourceProjectId)
            .isEmpty()) {

            return;
        }

        dataSyncFacade.copyProjectDataSyncs(sourceProjectId, duplicateProjectId, getWorkspaceId(duplicateProjectId));
    }

    @Override
    public Map<String, byte[]> exportProjectContent(long projectId) {
        Map<String, byte[]> files = new LinkedHashMap<>();

        for (DataSync dataSync : dataSyncService.getProjectDataSyncs(projectId)) {
            String json = dataSyncFacade.exportDataSync(dataSync.getId());

            files.put(CONTENT_DIRECTORY + dataSync.getName() + FILE_EXTENSION, json.getBytes(StandardCharsets.UTF_8));
        }

        return files;
    }

    @Override
    public void importProjectContent(long projectId, long workspaceId, Map<String, byte[]> files) {
        for (String json : getDataSyncFiles(files).values()) {
            dataSyncFacade.importDataSync(workspaceId, json, projectId);
        }
    }

    @Override
    public void pullProjectContent(long projectId, Map<String, byte[]> files) {
        Map<String, String> dataSyncFiles = getDataSyncFiles(files);

        if (dataSyncFiles.isEmpty()) {
            return;
        }

        List<DataSync> unmatchedDataSyncs = new ArrayList<>(dataSyncService.getProjectDataSyncs(projectId));

        for (Map.Entry<String, String> entry : dataSyncFiles.entrySet()) {
            String json = entry.getValue();

            Optional<DataSync> matchedDataSync = findDataSync(unmatchedDataSyncs, entry.getKey(), json);

            if (matchedDataSync.isPresent()) {
                DataSync dataSync = matchedDataSync.get();

                unmatchedDataSyncs.remove(dataSync);

                dataSyncFacade.updateDataSyncFromExport(dataSync.getId(), json);
            } else {
                dataSyncFacade.importDataSync(getWorkspaceId(projectId), json, projectId);
            }
        }
    }

    /**
     * The data sync a pulled file stands for: the one whose name the file is named after, else — for a file whose data
     * sync got a suffixed name because the exported one was taken elsewhere in the workspace — the only one with its
     * title.
     */
    private static Optional<DataSync> findDataSync(List<DataSync> dataSyncs, String dataSyncName, String json) {
        Optional<DataSync> namedDataSync = dataSyncs.stream()
            .filter(dataSync -> Objects.equals(dataSync.getName(), dataSyncName))
            .findFirst();

        if (namedDataSync.isPresent()) {
            return namedDataSync;
        }

        Object title = readTitle(json);

        List<DataSync> titledDataSyncs = dataSyncs.stream()
            .filter(dataSync -> Objects.equals(dataSync.getTitle(), title))
            .toList();

        return titledDataSyncs.size() == 1 ? Optional.of(titledDataSyncs.getFirst()) : Optional.empty();
    }

    /**
     * The file's title, or {@code null} for a file that is not a JSON object — such a file matches no data sync by
     * title, and the import that follows rejects it with its own message.
     */
    private static Object readTitle(String json) {
        try {
            return JsonUtils.readMap(json)
                .get("title");
        } catch (RuntimeException runtimeException) {
            return null;
        }
    }

    /**
     * The data sync files among {@code files}, keyed by data sync name in name order: top-level {@code .json} entries
     * of the data syncs directory only.
     */
    private static Map<String, String> getDataSyncFiles(Map<String, byte[]> files) {
        Map<String, String> dataSyncFiles = new TreeMap<>();

        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String path = entry.getKey();

            if (!path.startsWith(CONTENT_DIRECTORY) || !path.endsWith(FILE_EXTENSION)) {
                continue;
            }

            String dataSyncName = path.substring(
                CONTENT_DIRECTORY.length(), path.length() - FILE_EXTENSION.length());

            if (dataSyncName.isEmpty() || dataSyncName.contains("/")) {
                continue;
            }

            dataSyncFiles.put(dataSyncName, new String(entry.getValue(), StandardCharsets.UTF_8));
        }

        return dataSyncFiles;
    }

    private long getWorkspaceId(long projectId) {
        Project project = projectService.getProject(projectId);

        return Objects.requireNonNull(project.getWorkspaceId(), "workspaceId");
    }
}
