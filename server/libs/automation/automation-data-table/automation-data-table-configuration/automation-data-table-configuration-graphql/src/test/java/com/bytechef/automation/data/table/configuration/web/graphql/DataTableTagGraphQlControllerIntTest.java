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

package com.bytechef.automation.data.table.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.configuration.service.DataTableTagService;
import com.bytechef.automation.data.table.configuration.web.graphql.config.DataTableGraphQlConfigurationSharedMocks;
import com.bytechef.automation.data.table.configuration.web.graphql.config.DataTableGraphQlTestConfiguration;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    DataTableGraphQlTestConfiguration.class,
    DataTableTagGraphQlController.class
})
@GraphQlTest(
    controllers = DataTableTagGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@DataTableGraphQlConfigurationSharedMocks
public class DataTableTagGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableTagService dataTableTagService;

    @Test
    void testGetDataTableTags() {
        // Given
        Tag tag1 = new Tag(1L, "important");
        Tag tag2 = new Tag(2L, "urgent");

        when(dataTableTagService.getAllTags()).thenReturn(List.of(tag1, tag2));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableTags {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("dataTableTags")
            .entityList(Object.class)
            .hasSize(2)
            .path("dataTableTags[0].name")
            .entity(String.class)
            .isEqualTo("important")
            .path("dataTableTags[1].name")
            .entity(String.class)
            .isEqualTo("urgent");
    }

    @Test
    void testGetDataTableTagsEmpty() {
        // Given
        when(dataTableTagService.getAllTags()).thenReturn(List.of());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableTags {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("dataTableTags")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testGetDataTableTagsByTable() {
        // Given
        Tag tag1 = new Tag(1L, "important");
        Tag tag2 = new Tag(2L, "urgent");

        when(dataTableTagService.getTagsByTableName()).thenReturn(Map.of(
            "orders", List.of(tag1, tag2),
            "products", List.of(tag1)));
        when(dataTableService.getIdByBaseName("orders")).thenReturn(10L);
        when(dataTableService.getIdByBaseName("products")).thenReturn(20L);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableTagsByTable {
                        tableId
                        tags {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("dataTableTagsByTable")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testGetDataTableTagsByTableEmpty() {
        // Given
        when(dataTableTagService.getTagsByTableName()).thenReturn(Map.of());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableTagsByTable {
                        tableId
                        tags {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("dataTableTagsByTable")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testUpdateDataTableTags() {
        // Given
        doNothing().when(dataTableTagService)
            .updateTags(eq(10L), any());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateDataTableTags(input: {
                        tableId: "10",
                        tags: [
                            { id: "1", name: "important" },
                            { name: "new-tag" }
                        ]
                    })
                }
                """)
            .execute()
            .path("updateDataTableTags")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableTagService).updateTags(eq(10L), any());
    }

    @Test
    void testUpdateDataTableTagsWithEmptyTags() {
        // Given
        doNothing().when(dataTableTagService)
            .updateTags(eq(10L), any());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateDataTableTags(input: {
                        tableId: "10",
                        tags: []
                    })
                }
                """)
            .execute()
            .path("updateDataTableTags")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableTagService).updateTags(eq(10L), any());
    }

    @Test
    void testUpdateDataTableTagsWithNullTags() {
        // Given
        doNothing().when(dataTableTagService)
            .updateTags(eq(10L), any());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateDataTableTags(input: {
                        tableId: "10"
                    })
                }
                """)
            .execute()
            .path("updateDataTableTags")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableTagService).updateTags(eq(10L), any());
    }
}
