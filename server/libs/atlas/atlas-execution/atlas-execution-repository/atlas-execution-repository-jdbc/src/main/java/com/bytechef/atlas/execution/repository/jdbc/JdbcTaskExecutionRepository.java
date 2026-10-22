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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface JdbcTaskExecutionRepository
    extends ListPagingAndSortingRepository<TaskExecution, Long>, ListCrudRepository<TaskExecution, Long>,
    TaskExecutionRepository {

    @Override
    @Query("SELECT COUNT(*) FROM task_execution WHERE job_id = :jobId")
    long countByJobId(@Param("jobId") long jobId);

    @Override
    @Query("""
        SELECT COUNT(*) FROM task_execution
        WHERE parent_id = :parentId AND task_number = :taskNumber
        AND (created_date < :createdDate OR (created_date = :createdDate AND id <= :id))
        """)
    long countByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual(
        @Param("parentId") long parentId, @Param("taskNumber") int taskNumber,
        @Param("createdDate") Instant createdDate, @Param("id") long id);

    @Override
    void deleteById(long id);

    @Override
    default List<TaskExecution> findAllByIdIn(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return selectAllByIdIn(ids);
    }

    @Query("SELECT * FROM task_execution WHERE id IN (:ids)")
    List<TaskExecution> selectAllByIdIn(@Param("ids") List<Long> ids);

    @Override
    @Query("SELECT * FROM task_execution WHERE job_id = :jobId AND error IS NOT NULL ORDER BY created_date ASC, id ASC")
    List<TaskExecution> findAllByJobIdAndErrorNotNullOrderByCreatedDate(@Param("jobId") long jobId);

    @Override
    @Query("SELECT * FROM task_execution WHERE job_id = :jobId AND parent_id IS NULL ORDER BY created_date ASC, id ASC")
    List<TaskExecution> findAllByJobIdAndParentIdIsNullOrderByCreatedDate(@Param("jobId") long jobId);

    @Override
    List<TaskExecution> findAllByJobIdOrderByCreatedDate(long jobId);

    @Override
    List<TaskExecution> findAllByJobIdOrderByTaskNumber(long jobId);

    @Override
    List<TaskExecution> findAllByParentIdOrderByTaskNumber(long parentId);

    @Override
    @Query("""
        SELECT * FROM task_execution WHERE parent_id = :parentId
        ORDER BY created_date ASC, id ASC LIMIT :limit OFFSET :offset
        """)
    List<TaskExecution> findAllByParentIdOrderByCreatedDate(
        @Param("parentId") long parentId, @Param("limit") int limit, @Param("offset") long offset);

    @Override
    @Query("""
        SELECT id FROM task_execution WHERE parent_id = :parentId AND task_number = :taskNumber
        ORDER BY created_date ASC, id ASC
        """)
    List<Long> findAllIdsByParentIdAndTaskNumberOrderByCreatedDate(
        @Param("parentId") long parentId, @Param("taskNumber") int taskNumber);

    @Override
    @Query("SELECT id FROM task_execution WHERE parent_id = :parentId ORDER BY created_date ASC, id ASC")
    List<Long> findAllIdsByParentIdOrderByCreatedDate(@Param("parentId") long parentId);

    @Override
    default List<Long> findAllParentIdsByParentIdIn(List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }

        return selectAllParentIdsByParentIdIn(parentIds);
    }

    @Override
    default List<Long> findAllParentIdsByParentIdInAndTaskNumber(List<Long> parentIds, int taskNumber) {
        if (parentIds.isEmpty()) {
            return List.of();
        }

        return selectAllParentIdsByParentIdInAndTaskNumber(parentIds, taskNumber);
    }

    @Query("SELECT parent_id FROM task_execution WHERE parent_id IN (:parentIds)")
    List<Long> selectAllParentIdsByParentIdIn(@Param("parentIds") List<Long> parentIds);

    @Query("SELECT parent_id FROM task_execution WHERE parent_id IN (:parentIds) AND task_number = :taskNumber")
    List<Long> selectAllParentIdsByParentIdInAndTaskNumber(
        @Param("parentIds") List<Long> parentIds, @Param("taskNumber") int taskNumber);

    @Override
    @Query("SELECT * FROM task_execution WHERE id = :id FOR UPDATE")
    Optional<TaskExecution> findByIdForUpdate(@Param("id") long id);

    @Override
    @Query("SELECT * FROM task_execution WHERE task_execution.job_id = :jobId ORDER BY task_execution.created_date DESC LIMIT 1")
    Optional<TaskExecution> findLastByJobId(@Param("jobId") long jobId);

    TaskExecution save(TaskExecution taskExecution);
}
