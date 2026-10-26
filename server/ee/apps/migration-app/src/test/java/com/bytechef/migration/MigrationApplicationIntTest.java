/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ClassUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = MigrationApplication.class)
class MigrationApplicationIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testChangelogsOfEveryModuleRun() {
        List<String> filenames = jdbcTemplate.queryForList("SELECT filename FROM databasechangelog", String.class);

        assertThat(filenames).anyMatch(filename -> filename.contains("platform/user/"));
        assertThat(filenames).anyMatch(filename -> filename.contains("atlas/execution/"));
        assertThat(filenames).anyMatch(filename -> filename.contains("automation/configuration/"));
    }

    @Test
    void testMonoOnlyChangesetRuns() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'api_key'", Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void testNoWebServerOnClasspath() {
        assertThat(ClassUtils.isPresent("org.springframework.boot.web.server.WebServer", null)).isFalse();
    }
}
