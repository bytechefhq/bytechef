/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Pins the table-name sanitisation contract: canonical shape, injection-attempt neutralisation, dot collapsing,
 * disambiguation-prefix preservation under truncation. Includes the {@code c<contextStoreId>} segment added with the
 * multi-env Context Store work — DEV/STAGING/PROD stores of the same logical entity get physically distinct table
 * names.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ClickHouseTableNameSanitizerTest {

    @Test
    void testHappyPathProducesCanonicalShape() {
        assertThat(ClickHouseTableNameSanitizer.tableNameFor(1L, 5L, 42L, "Companies"))
            .isEqualTo("cs_w1_c5_s42_companies");
    }

    @Test
    void testSqlInjectionAttemptIsNeutralised() {
        String name = ClickHouseTableNameSanitizer.tableNameFor(1L, 1L, 1L, "x'); DROP TABLE users;--");

        assertThat(name).matches(ClickHouseTableNameSanitizer.VALID_TABLE_NAME_REGEX);
        assertThat(name).doesNotContain("'", "(", ")", ";", "-", " ");
    }

    @Test
    void testDottedPathsAreCollapsedToUnderscores() {
        assertThat(ClickHouseTableNameSanitizer.tableNameFor(2L, 4L, 3L, "company.name"))
            .isEqualTo("cs_w2_c4_s3_company_name");
    }

    @Test
    void testEntityNameWithOnlyForbiddenCharsThrows() {
        assertThatThrownBy(() -> ClickHouseTableNameSanitizer.tableNameFor(1L, 1L, 1L, "!!!"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reduces to empty");
    }

    @Test
    void testLongEntityNameIsTruncatedNotPrefix() {
        String veryLong = "a".repeat(500);

        String name = ClickHouseTableNameSanitizer.tableNameFor(7L, 8L, 9L, veryLong);

        assertThat(name).hasSize(ClickHouseTableNameSanitizer.MAX_TABLE_NAME_LENGTH);
        assertThat(name).startsWith("cs_w7_c8_s9_");
        assertThat(name).matches(ClickHouseTableNameSanitizer.VALID_TABLE_NAME_REGEX);
    }

    @Test
    void testCaseIsNormalisedToLower() {
        assertThat(ClickHouseTableNameSanitizer.tableNameFor(1L, 1L, 1L, "MixedCASEEntity"))
            .isEqualTo("cs_w1_c1_s1_mixedcaseentity");
    }

    @Test
    void testDifferentContextStoreIdsYieldDistinctTableNames() {
        String devTable = ClickHouseTableNameSanitizer.tableNameFor(1L, 5L, 42L, "Companies");
        String prodTable = ClickHouseTableNameSanitizer.tableNameFor(1L, 6L, 42L, "Companies");

        assertThat(devTable).isNotEqualTo(prodTable);
        assertThat(devTable).contains("_c5_");
        assertThat(prodTable).contains("_c6_");
    }
}
