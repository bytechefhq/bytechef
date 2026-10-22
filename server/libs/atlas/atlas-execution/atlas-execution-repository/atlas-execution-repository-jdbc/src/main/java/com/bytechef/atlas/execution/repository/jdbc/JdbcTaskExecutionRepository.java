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
import java.util.Map;
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
    default Map<Long, Long> countAllByParentIdIn(List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }

        return selectCountsByParentIdIn(parentIds);
    }

    @Override
    default Map<Long, Long> countAllByParentIdInAndTaskNumber(List<Long> parentIds, int taskNumber) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }

        return selectCountsByParentIdInAndTaskNumber(parentIds, taskNumber);
    }

    @Override
    default long countLatestAttemptsByJobId(long jobId, List<Long> supersededIds) {
        if (supersededIds.isEmpty()) {
            return selectLatestAttemptCountByJobId(jobId);
        }

        return selectLatestAttemptCountByJobIdAndSupersededIds(jobId, supersededIds);
    }

    @Query("""
        WITH RECURSIVE retried_task_execution AS (
            SELECT id FROM task_execution
            WHERE job_id = :jobId AND status = 3 AND error IS NOT NULL AND retry_attempts < max_retries
            UNION
            SELECT task_execution.id FROM task_execution
            JOIN retried_task_execution ON task_execution.parent_id = retried_task_execution.id
        )
        SELECT (SELECT COUNT(*) FROM task_execution WHERE job_id = :jobId)
            - (SELECT COUNT(*) FROM retried_task_execution)
        """)
    long selectLatestAttemptCountByJobId(@Param("jobId") long jobId);

    @Query("""
        WITH RECURSIVE retried_task_execution AS (
            SELECT id FROM task_execution
            WHERE job_id = :jobId AND status = 3 AND error IS NOT NULL AND retry_attempts < max_retries
            UNION
            SELECT id FROM task_execution WHERE job_id = :jobId AND id IN (:supersededIds)
            UNION
            SELECT task_execution.id FROM task_execution
            JOIN retried_task_execution ON task_execution.parent_id = retried_task_execution.id
        )
        SELECT (SELECT COUNT(*) FROM task_execution WHERE job_id = :jobId)
            - (SELECT COUNT(*) FROM retried_task_execution)
        """)
    long selectLatestAttemptCountByJobIdAndSupersededIds(
        @Param("jobId") long jobId, @Param("supersededIds") List<Long> supersededIds);

    @Override
    @Query("""
        SELECT COUNT(*) FROM task_execution
        WHERE parent_id = :parentId
        AND (created_date < :createdDate OR (created_date = :createdDate AND id <= :id))
        """)
    long countByParentIdAndCreatedDateBeforeOrEqual(
        @Param("parentId") long parentId, @Param("createdDate") Instant createdDate, @Param("id") long id);

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
    @Query("SELECT * FROM task_execution WHERE job_id = :jobId AND parent_id IS NULL ORDER BY created_date ASC, id ASC")
    List<TaskExecution> findAllByJobIdAndParentIdIsNullOrderByCreatedDate(@Param("jobId") long jobId);

    @Override
    @Query("""
        WITH RECURSIVE retried_task_execution AS (
            SELECT id FROM task_execution
            WHERE job_id = :jobId AND status = 3 AND error IS NOT NULL AND retry_attempts < max_retries
            UNION
            SELECT task_execution.id FROM task_execution
            JOIN retried_task_execution ON task_execution.parent_id = retried_task_execution.id
        )
        SELECT * FROM task_execution
        WHERE job_id = :jobId AND error IS NOT NULL
        AND id NOT IN (SELECT id FROM retried_task_execution)
        AND NOT EXISTS (SELECT 1 FROM task_execution child WHERE child.parent_id = task_execution.id)
        ORDER BY created_date ASC, id ASC LIMIT :limit OFFSET :offset
        """)
    List<TaskExecution> findAllLatestAttemptFailedLeavesByJobIdOrderByCreatedDate(
        @Param("jobId") long jobId, @Param("limit") int limit, @Param("offset") long offset);

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
        SELECT * FROM task_execution WHERE parent_id = :parentId
        AND (created_date > :fromCreatedDate OR (created_date = :fromCreatedDate AND id >= :fromId))
        AND (created_date < :toCreatedDate OR (created_date = :toCreatedDate AND id < :toId))
        ORDER BY created_date ASC, id ASC
        """)
    List<TaskExecution> findAllByParentIdAndCreatedDateBetweenOrderByCreatedDate(
        @Param("parentId") long parentId, @Param("fromCreatedDate") Instant fromCreatedDate,
        @Param("fromId") long fromId, @Param("toCreatedDate") Instant toCreatedDate, @Param("toId") long toId);

    @Override
    @Query("""
        SELECT * FROM task_execution WHERE parent_id = :parentId
        AND (created_date > :fromCreatedDate OR (created_date = :fromCreatedDate AND id >= :fromId))
        ORDER BY created_date ASC, id ASC
        """)
    List<TaskExecution> findAllByParentIdAndCreatedDateFromOrderByCreatedDate(
        @Param("parentId") long parentId, @Param("fromCreatedDate") Instant fromCreatedDate,
        @Param("fromId") long fromId);

    @Override
    @Query("""
        SELECT * FROM task_execution WHERE parent_id = :parentId AND task_number = :taskNumber
        ORDER BY created_date ASC, id ASC LIMIT :limit OFFSET :offset
        """)
    List<TaskExecution> findAllByParentIdAndTaskNumberOrderByCreatedDate(
        @Param("parentId") long parentId, @Param("taskNumber") int taskNumber, @Param("limit") int limit,
        @Param("offset") long offset);

    @Override
    default List<Long> findAllIdsByIdInAndFirstChildTaskNumberNot(List<Long> ids, int taskNumber) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return selectAllIdsByIdInAndFirstChildTaskNumberNot(ids, taskNumber);
    }

    @Query("""
        SELECT parent.id FROM task_execution parent WHERE parent.id IN (:ids)
        AND (
            SELECT child.task_number FROM task_execution child WHERE child.parent_id = parent.id
            ORDER BY child.created_date ASC, child.id ASC LIMIT 1
        ) <> :taskNumber
        """)
    List<Long> selectAllIdsByIdInAndFirstChildTaskNumberNot(
        @Param("ids") List<Long> ids, @Param("taskNumber") int taskNumber);

    @Override
    @Query("SELECT id FROM task_execution WHERE parent_id = :parentId ORDER BY created_date ASC, id ASC")
    List<Long> findAllIdsByParentIdOrderByCreatedDate(@Param("parentId") long parentId);

    @Query(
        value = """
            SELECT parent_id, COUNT(*) AS child_count FROM task_execution
            WHERE parent_id IN (:parentIds) GROUP BY parent_id
            """,
        resultSetExtractorClass = ParentIdCountsResultSetExtractor.class)
    Map<Long, Long> selectCountsByParentIdIn(@Param("parentIds") List<Long> parentIds);

    @Query(
        value = """
            SELECT parent_id, COUNT(*) AS child_count FROM task_execution
            WHERE parent_id IN (:parentIds) AND task_number = :taskNumber GROUP BY parent_id
            """,
        resultSetExtractorClass = ParentIdCountsResultSetExtractor.class)
    Map<Long, Long> selectCountsByParentIdInAndTaskNumber(
        @Param("parentIds") List<Long> parentIds, @Param("taskNumber") int taskNumber);

    @Override
    @Query("SELECT * FROM task_execution WHERE id = :id FOR UPDATE")
    Optional<TaskExecution> findByIdForUpdate(@Param("id") long id);

    @Override
    @Query("SELECT * FROM task_execution WHERE task_execution.job_id = :jobId ORDER BY task_execution.created_date DESC LIMIT 1")
    Optional<TaskExecution> findLastByJobId(@Param("jobId") long jobId);

    @Override
    @Query("""
        SELECT * FROM task_execution WHERE job_id = :jobId AND start_date IS NOT NULL
        ORDER BY start_date DESC, id DESC LIMIT 1
        """)
    Optional<TaskExecution> findLastStartedByJobId(@Param("jobId") long jobId);

    TaskExecution save(TaskExecution taskExecution);
}
