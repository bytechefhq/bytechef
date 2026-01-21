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

package com.bytechef.automation.data.table.configuration.service;

import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.config.DataTableIntTestConfiguration;
import com.bytechef.automation.data.table.configuration.repository.DataTableRepository;
import com.bytechef.automation.data.table.configuration.repository.WorkspaceDataTableRepository;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class DataTableTagServiceIntTest {

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableTagService dataTableTagService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TagService tagService;

    @Autowired
    private WorkspaceDataTableRepository workspaceDataTableRepository;

    @BeforeEach
    public void beforeEach() {
        cleanupTables();
        reset(tagService);
    }

    @AfterEach
    public void afterEach() {
        cleanupTables();
    }

    private void cleanupTables() {
        workspaceDataTableRepository.deleteAll();
        dataTableRepository.deleteAll();

        jdbcTemplate.execute("DELETE FROM tag");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_orders\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_products\"");
    }

    private void insertTag(long id, String name) {
        jdbcTemplate.update(
            "INSERT INTO tag (id, name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, ?, NOW(), 'system', NOW(), 'system', 0)",
            id, name);
    }

    @Test
    public void testGetAllTagsReturnsEmptyWhenNoTables() {
        List<Tag> tags = dataTableTagService.getAllTags();

        assertThat(tags).isEmpty();
    }

    @Test
    public void testGetAllTagsReturnsEmptyWhenNoTagsAssigned() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        List<Tag> tags = dataTableTagService.getAllTags();

        assertThat(tags).isEmpty();
    }

    @Test
    public void testGetAllTagsReturnsTags() {
        insertTag(1L, "important");
        insertTag(2L, "urgent");

        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long tableId = dataTableService.getIdByBaseName("orders");

        Tag tag1 = new Tag(1L, "important");
        Tag tag2 = new Tag(2L, "urgent");

        when(tagService.save(anyList())).thenReturn(List.of(tag1, tag2));
        when(tagService.getTags(anyList())).thenReturn(List.of(tag1, tag2));

        dataTableTagService.updateTags(tableId, List.of(tag1, tag2));

        List<Tag> tags = dataTableTagService.getAllTags();

        assertThat(tags).hasSize(2);
        assertThat(tags).extracting(Tag::getName)
            .containsExactlyInAnyOrder("important", "urgent");
    }

    @Test
    public void testGetTagsByTableNameReturnsEmptyWhenNoTables() {
        Map<String, List<Tag>> tagsByTableName = dataTableTagService.getTagsByTableName();

        assertThat(tagsByTableName).isEmpty();
    }

    @Test
    public void testGetTagsByTableNameReturnsTags() {
        insertTag(1L, "important");

        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        dataTableService.createTable(
            "products",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long ordersId = dataTableService.getIdByBaseName("orders");

        Tag tag1 = new Tag(1L, "important");

        when(tagService.save(anyList())).thenReturn(List.of(tag1));
        when(tagService.getTags(List.of(1L))).thenReturn(List.of(tag1));
        when(tagService.getTags(List.of())).thenReturn(List.of());

        dataTableTagService.updateTags(ordersId, List.of(tag1));

        Map<String, List<Tag>> tagsByTableName = dataTableTagService.getTagsByTableName();

        assertThat(tagsByTableName).hasSize(2);
        assertThat(tagsByTableName.get("orders")).hasSize(1);
        assertThat(tagsByTableName.get("orders")
            .getFirst()
            .getName()).isEqualTo("important");
        assertThat(tagsByTableName.get("products")).isEmpty();
    }

    @Test
    public void testUpdateTagsWithNewTags() {
        insertTag(1L, "newtag");

        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long tableId = dataTableService.getIdByBaseName("orders");

        Tag newTag = new Tag("newtag");
        Tag savedTag = new Tag(1L, "newtag");

        when(tagService.save(anyList())).thenReturn(List.of(savedTag));
        when(tagService.getTags(List.of(1L))).thenReturn(List.of(savedTag));

        dataTableTagService.updateTags(tableId, List.of(newTag));

        List<Tag> tags = dataTableTagService.getAllTags();

        assertThat(tags).hasSize(1);
        assertThat(tags.getFirst()
            .getName()).isEqualTo("newtag");
    }

    @Test
    public void testUpdateTagsWithExistingTags() {
        insertTag(1L, "existing");

        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long tableId = dataTableService.getIdByBaseName("orders");

        Tag existingTag = new Tag(1L, "existing");

        when(tagService.getTags(List.of(1L))).thenReturn(List.of(existingTag));

        dataTableTagService.updateTags(tableId, List.of(existingTag));

        List<Tag> tags = dataTableTagService.getAllTags();

        assertThat(tags).hasSize(1);
        assertThat(tags.getFirst()
            .getName()).isEqualTo("existing");
    }

    @Test
    public void testUpdateTagsWithEmptyList() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long tableId = dataTableService.getIdByBaseName("orders");

        dataTableTagService.updateTags(tableId, List.of());

        List<Tag> tags = dataTableTagService.getAllTags();

        assertThat(tags).isEmpty();
    }

    @Test
    public void testUpdateTagsWithNull() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long tableId = dataTableService.getIdByBaseName("orders");

        dataTableTagService.updateTags(tableId, null);

        List<Tag> tags = dataTableTagService.getAllTags();

        assertThat(tags).isEmpty();
    }
}
