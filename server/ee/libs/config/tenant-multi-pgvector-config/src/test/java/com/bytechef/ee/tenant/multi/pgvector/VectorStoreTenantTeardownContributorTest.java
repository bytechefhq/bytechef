/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.pgvector;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class VectorStoreTenantTeardownContributorTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final VectorStoreTenantTeardownContributor contributor = new VectorStoreTenantTeardownContributor(
        jdbcTemplate);

    @Test
    void testVectorStoreSchemaDropped() {
        contributor.afterSchemaDrop("000001");

        verify(jdbcTemplate).execute("DROP SCHEMA IF EXISTS bytechef_vectorstore_000001 CASCADE");
    }

    @Test
    void testRejectsTenantIdWithSqlCharacters() {
        assertThatThrownBy(() -> contributor.afterSchemaDrop("1; DROP"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(jdbcTemplate);
    }
}
