/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link ContextStoreVectorStoreMetadataService}. Verifies the per-call table-name validation guard,
 * per-call table-name resolution, the empty-input short-circuits, and the SQL shape produced by each helper.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class ContextStoreVectorStoreMetadataServiceTest {

    private static final String TABLE_NAME = "public.cs_vector_store";

    private JdbcTemplate jdbcTemplate;
    private ContextStoreVectorStoreMetadataService metadataService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);

        metadataService =
            new ContextStoreVectorStoreMetadataService(jdbcTemplate, mock(ObjectMapper.class), () -> TABLE_NAME);
    }

    @Test
    void testQueryRejectsUnsafeTableName() {
        ContextStoreVectorStoreMetadataService unsafeService = new ContextStoreVectorStoreMetadataService(
            jdbcTemplate, mock(ObjectMapper.class), () -> "cs_vector_store; DROP TABLE users");

        assertThatThrownBy(() -> unsafeService.findVectorStoreIdsByRecordIds(List.of(1L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid table name");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testTableNameResolvedFromSupplierOnEachCall() {
        AtomicInteger resolveCount = new AtomicInteger();

        ContextStoreVectorStoreMetadataService perTenantService = new ContextStoreVectorStoreMetadataService(
            jdbcTemplate, mock(ObjectMapper.class),
            () -> "tenant_" + resolveCount.incrementAndGet() + ".cs_vector_store");

        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(Object[].class)))
            .thenReturn(List.of());

        perTenantService.findVectorStoreIdsByRecordIds(List.of(1L));
        perTenantService.findVectorStoreIdsByRecordIds(List.of(2L));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, times(2)).queryForList(sqlCaptor.capture(), eq(String.class), any(Object[].class));

        List<String> capturedSql = sqlCaptor.getAllValues();

        assertThat(capturedSql.get(0)).contains("tenant_1.cs_vector_store");
        assertThat(capturedSql.get(1)).contains("tenant_2.cs_vector_store");
    }

    @Test
    void testFindExistingPayloadHashesByRecordIdsReturnsEmptyForNullCollection() {
        Map<Long, String> result = metadataService.findExistingPayloadHashesByRecordIds(null);

        assertThat(result).isEmpty();

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testFindExistingPayloadHashesByRecordIdsReturnsEmptyForEmptyCollection() {
        Map<Long, String> result = metadataService.findExistingPayloadHashesByRecordIds(List.of());

        assertThat(result).isEmpty();

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testFindExistingPayloadHashesByRecordIdsBuildsParameterizedQuery() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.getLong("record_id")).thenReturn(101L, 202L);
        when(resultSet.getString("payload_hash")).thenReturn("hash-1", "hash-2");

        doAnswer(invocation -> {
            RowMapper<?> rowMapper = invocation.getArgument(2, RowMapper.class);

            rowMapper.mapRow(resultSet, 0);
            rowMapper.mapRow(resultSet, 1);

            return null;
        }).when(jdbcTemplate)
            .query(anyString(), any(Object[].class), any(RowMapper.class));

        Map<Long, String> result = metadataService.findExistingPayloadHashesByRecordIds(List.of(101L, 202L));

        assertThat(result).containsEntry(101L, "hash-1")
            .containsEntry(202L, "hash-2");
    }

    @Test
    void testFindVectorStoreIdsByRecordIdsReturnsEmptyForEmptyInput() {
        List<String> result = metadataService.findVectorStoreIdsByRecordIds(List.of());

        assertThat(result).isEmpty();

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testFindVectorStoreIdsByRecordIdsDelegatesToJdbcTemplate() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(Object[].class)))
            .thenReturn(List.of("uuid-1", "uuid-2"));

        List<String> result = metadataService.findVectorStoreIdsByRecordIds(List.of(1L, 2L));

        assertThat(result).containsExactly("uuid-1", "uuid-2");
    }

    @Test
    void testFindVectorStoreIdsBySourceAndEntityDelegatesToJdbcTemplate() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(42L), eq("contacts")))
            .thenReturn(List.of("uuid-3"));

        List<String> result = metadataService.findVectorStoreIdsBySourceAndEntity(42L, "contacts");

        assertThat(result).containsExactly("uuid-3");

        verify(jdbcTemplate).queryForList(anyString(), eq(String.class), eq(42L), eq("contacts"));
    }
}
