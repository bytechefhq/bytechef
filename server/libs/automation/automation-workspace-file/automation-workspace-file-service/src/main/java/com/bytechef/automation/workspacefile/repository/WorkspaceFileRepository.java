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

package com.bytechef.automation.workspacefile.repository;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Ivica Cardic
 */
public interface WorkspaceFileRepository extends ListCrudRepository<WorkspaceFile, Long> {

    @Query("""
        SELECT wf.* FROM workspace_file wf
        JOIN workspace_workspace_file wwf ON wwf.workspace_file_id = wf.id
        WHERE wwf.workspace_id = :workspaceId
        ORDER BY wf.last_modified_date DESC
        """)
    List<WorkspaceFile> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT DISTINCT wf.* FROM workspace_file wf
        JOIN workspace_workspace_file wwf ON wwf.workspace_file_id = wf.id
        JOIN workspace_file_tag wft ON wft.workspace_file_id = wf.id
        WHERE wwf.workspace_id = :workspaceId AND wft.tag_id IN (:tagIds)
        ORDER BY wf.last_modified_date DESC
        """)
    List<WorkspaceFile> findAllByWorkspaceIdAndTagIdsIn(
        @Param("workspaceId") Long workspaceId, @Param("tagIds") List<Long> tagIds);

    Optional<WorkspaceFile> findFirstByName(String name);

    @Query("""
        SELECT COALESCE(SUM(wf.size_bytes), 0) FROM workspace_file wf
        JOIN workspace_workspace_file wwf ON wwf.workspace_file_id = wf.id
        WHERE wwf.workspace_id = :workspaceId
        """)
    long sumSizeBytesByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
