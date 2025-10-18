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

package com.bytechef.platform.workflow.execution.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings(
    value = "SQL_INJECTION_SPRING_JDBC",
    justification = "Test class exercising parameterized query implementation; no actual SQL injection risk")
class CustomPrincipalJobRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<String> queryCaptor;

    @Captor
    private ArgumentCaptor<Object[]> argsCaptor;

    private CustomPrincipalJobRepositoryImpl customPrincipalJobRepository;

    @BeforeEach
    void beforeEach() {
        customPrincipalJobRepository = new CustomPrincipalJobRepositoryImpl(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFindAllJobIdsPaginationParameterized() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(10L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(1L, 2L, 3L));

        Page<Long> page = customPrincipalJobRepository.findAllJobIds(
            null, null, null, null, 1, List.of(), PageRequest.of(0, 3));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        String query = queryCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(query).contains("LIMIT ? OFFSET ?");
        assertThat(query).doesNotContain("LIMIT 3");
        assertThat(query).doesNotContain("OFFSET 0");

        assertThat(args[args.length - 2]).isEqualTo(3);
        assertThat(args[args.length - 1]).isEqualTo(0L);

        assertThat(page.getContent()).hasSize(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFindAllJobIdsSecondPageOffset() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(10L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(4L, 5L, 6L));

        customPrincipalJobRepository.findAllJobIds(
            null, null, null, null, 1, List.of(), PageRequest.of(1, 3));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        Object[] args = argsCaptor.getValue();

        assertThat(args[args.length - 2]).isEqualTo(3);
        assertThat(args[args.length - 1]).isEqualTo(3L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFindAllJobIdsWithStatusFilter() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(1L));

        customPrincipalJobRepository.findAllJobIds(
            2, null, null, null, 1, List.of(), PageRequest.of(0, 10));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        String query = queryCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(query).contains("AND status = ?");
        assertThat(args).contains(1, 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFindAllJobIdsWithDateFilters() {
        Instant now = Instant.now();

        Instant startDate = now.minusSeconds(3600);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(1L));

        customPrincipalJobRepository.findAllJobIds(
            null, startDate, now, null, 1, List.of(), PageRequest.of(0, 10));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        String query = queryCaptor.getValue();

        assertThat(query).contains("AND start_date >= ?");
        assertThat(query).contains("AND end_date <= ?");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFindAllJobIdsWithInstanceIds() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(1L));

        customPrincipalJobRepository.findAllJobIds(
            null, null, null, List.of(1L, 2L, 3L), 1, List.of(), PageRequest.of(0, 10));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        String query = queryCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(query).contains("AND principal_id IN(?,?,?)");
        assertThat(args).contains(1L, 2L, 3L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFindAllJobIdsWithWorkflowIds() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(1L));

        customPrincipalJobRepository.findAllJobIds(
            null, null, null, null, 1, List.of("workflow1", "workflow2"), PageRequest.of(0, 10));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        String query = queryCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(query).contains("AND workflow_id IN(?,?)");
        assertThat(args).contains("workflow1", "workflow2");
    }

    @Test
    void testFindAllJobIdsEmptyResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);

        Page<Long> page = customPrincipalJobRepository.findAllJobIds(
            null, null, null, null, 1, List.of(), PageRequest.of(0, 10));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFindAllJobIdsAllFiltersParameterized() {
        Instant now = Instant.now();

        Instant startDate = now.minusSeconds(3600);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(1L, 2L));

        customPrincipalJobRepository.findAllJobIds(
            1, startDate, now, List.of(10L, 20L), 2, List.of("wf1", "wf2"), PageRequest.of(2, 5));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        String query = queryCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(query).contains("WHERE type = ?");
        assertThat(query).contains("AND status = ?");
        assertThat(query).contains("AND start_date >= ?");
        assertThat(query).contains("AND end_date <= ?");
        assertThat(query).contains("AND principal_id IN(?,?)");
        assertThat(query).contains("AND workflow_id IN(?,?)");
        assertThat(query).contains("LIMIT ? OFFSET ?");

        assertThat(query).doesNotMatch(".*LIMIT \\d+.*");
        assertThat(query).doesNotMatch(".*OFFSET \\d+.*");

        assertThat(args[args.length - 2]).isEqualTo(5);
        assertThat(args[args.length - 1]).isEqualTo(10L);
    }
}
