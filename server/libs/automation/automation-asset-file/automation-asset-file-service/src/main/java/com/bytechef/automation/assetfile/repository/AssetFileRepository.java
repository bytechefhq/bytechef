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

package com.bytechef.automation.assetfile.repository;

import com.bytechef.automation.assetfile.domain.AssetFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Ivica Cardic
 */
public interface AssetFileRepository extends ListCrudRepository<AssetFile, Long> {

    @Query("""
        SELECT * FROM asset_file
        WHERE workspace_id = :workspaceId AND environment = :environment
        ORDER BY last_modified_date DESC
        """)
    List<AssetFile> findAllByWorkspaceIdAndEnvironment(
        @Param("workspaceId") Long workspaceId, @Param("environment") int environment);

    @Query("""
        SELECT DISTINCT af.* FROM asset_file af
        JOIN asset_file_tag aft ON aft.asset_file_id = af.id
        WHERE af.workspace_id = :workspaceId AND af.environment = :environment AND aft.tag_id IN (:tagIds)
        ORDER BY af.last_modified_date DESC
        """)
    List<AssetFile> findAllByWorkspaceIdAndEnvironmentAndTagIdsIn(
        @Param("workspaceId") Long workspaceId, @Param("environment") int environment,
        @Param("tagIds") List<Long> tagIds);

    Optional<AssetFile> findFirstByName(String name);

    @Query("""
        SELECT COALESCE(SUM(size_bytes), 0) FROM asset_file
        WHERE workspace_id = :workspaceId AND environment = :environment
        """)
    long sumSizeBytesByWorkspaceIdAndEnvironment(
        @Param("workspaceId") Long workspaceId, @Param("environment") int environment);
}
